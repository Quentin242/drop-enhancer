/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.io.File;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class SoundQueue
{
	private static final Logger log = LoggerFactory.getLogger(SoundQueue.class);
	@Inject
	CelebrationConfig config;
	@Inject
	CaseGate gate;
	private final List<Request> waiting = new ArrayList<>();
	private ExecutorService worker;
	private volatile int generation;
	private volatile int previewGeneration;
	// Each worker owns its clips, including cleanup after a quick disable/re-enable.
	private static final class AudioPlayback
	{
		final List<javax.sound.sampled.Clip> clips = new java.util.concurrent.CopyOnWriteArrayList<>();
		final java.util.Map<javax.sound.sampled.Clip, Long> deadlines = new java.util.concurrent.ConcurrentHashMap<>();
		void close()
		{
			for (javax.sound.sampled.Clip clip : clips)
			{
				closeClip(clip);
			}
			clips.clear();
			deadlines.clear();
		}
	}
	private static void closeClip(javax.sound.sampled.Clip clip)
	{
		try
		{
			clip.stop();
		}
		catch (RuntimeException e)
		{
			log.debug("Unable to stop audio clip", e);
		}
		try
		{
			clip.close();
		}
		catch (RuntimeException e)
		{
			log.debug("Unable to close audio clip", e);
		}
	}

	private AudioPlayback preview;
	private AudioPlayback rewards;
	private volatile long openingSince;
	long monotonicMillis()
	{
		return System.nanoTime() / 1000000;
	}
	private java.util.concurrent.atomic.AtomicInteger pendingAudio = new java.util.concurrent.atomic.AtomicInteger();
	boolean busy()
	{
		if (worker != null && pendingAudio.get() > 0 && monotonicMillis() - openingSince > 5000)
		{
			// A blocked audio driver must not freeze visual notifications or accumulate new work.
			generation++;
			previewGeneration++;
			worker.shutdownNow();
			worker = null;
			log.debug("Audio device did not respond; audio suspended until plugin restart");
		}
		return worker != null && (pendingAudio.get() > 0 || playing(preview) || playing(rewards));
	}
	private boolean playing(AudioPlayback playback)
	{
		if (playback == null)
		{
			return false;
		}
		boolean active = false;
		for (javax.sound.sampled.Clip clip : playback.clips)
		{
			long deadline = playback.deadlines.getOrDefault(clip, Long.MAX_VALUE);
			// start() can return before the driver sets isRunning(). An unfinished
			// frame position keeps that clip alive, bounded by the WAV deadline.
			if (monotonicMillis() < deadline && (clip.isRunning() || clip.getLongFramePosition() < clip.getFrameLength()))
			{
				active = true;
			}
			else if (playback.clips.remove(clip))
			{
				playback.deadlines.remove(clip);
				if (worker != null)
				{
					worker.execute(() -> closeClip(clip));
				}
			}
		}
		return active;
	}
	File runeliteDirectory = RuneLite.RUNELITE_DIR;
	private static final class Request
	{
		final String name;
		final int volume;
		final long due;
		final boolean reward;
		String valueItemName;
		Request(String name, int volume, long due, boolean reward)
		{
			this.name = name;
			this.volume = volume;
			this.due = due;
			this.reward = reward;
		}
	}
	void start()
	{
		pendingAudio = new java.util.concurrent.atomic.AtomicInteger();
		preview = new AudioPlayback();
		rewards = new AudioPlayback();
		worker = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "collection-celebrations-audio");
			t.setDaemon(true);
			return t;
		});
	}
	void reset()
	{
		generation++;
		waiting.clear();
		cancelPreview();
		if (worker != null)
		{
			AudioPlayback old = rewards;
			worker.execute(old::close);
		}
	}
	void stop()
	{
		reset();
		if (worker != null)
		{
			worker.shutdown();
			worker = null;
		}
	}
	static boolean validName(String name)
	{
		return name != null && name.trim().matches("[A-Za-z0-9_ .()-]+\\.wav") && !name.contains("..");
	}
	private File file(String name)
	{
		return new File(new File(runeliteDirectory, "collection-celebrations/sounds"), name.trim());
	}
	void offer(String name, int volume, long delay, boolean reward)
	{
		if (volume <= 0)
		{
			return;
		}
		if (waiting.size() == 128)
		{
			waiting.remove(0);
		}
		waiting.add(new Request(name, volume, System.currentTimeMillis() + Math.max(delay, reward ? gate.delayMillis() : 0), reward));
	}
	void offerValue(String name, int volume, String itemName)
	{
		if (volume <= 0)
		{
			return;
		}
		if (waiting.size() == 128)
		{
			waiting.remove(0);
		}
		// Item identity is already known. Release at the next render callback; cases still gate playback.
		Request request = new Request(name, volume, System.currentTimeMillis(), true);
		request.valueItemName = itemName.toLowerCase(java.util.Locale.ROOT);
		waiting.add(request);
	}
	void cancelValue(String itemName)
	{
		String key = itemName.toLowerCase(java.util.Locale.ROOT);
		waiting.removeIf(r -> key.equals(r.valueItemName));
	}
	void tick(long now)
	{
		Iterator<Request> it = waiting.iterator();
		while (it.hasNext())
		{
			Request r = it.next();
			if (!busy() && now >= r.due && (!r.reward || !gate.blocked()))
			{
				it.remove();
				playNow(r.name, r.volume);
				break;
			}
		}
	}
	void cancelPreview()
	{
		previewGeneration++;
		if (worker != null)
		{
			AudioPlayback playback = preview;
			worker.execute(playback::close);
		}
	}
	javax.sound.sampled.Clip openClip(File file) throws Exception
	{
		javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
		try (javax.sound.sampled.AudioInputStream input = SoundResources.open(file))
		{
			clip.open(input);
			return clip;
		}
		catch (Exception e)
		{
			clip.close();
			throw e;
		}
	}
	void playPreview(String name, int volume)
	{
		playPreview(name, volume, null, 0);
	}
	void playPreview(String name, int volume, String unlockName, int unlockVolume)
	{
		if (worker == null)
		{
			return;
		}
		int token = previewGeneration;
		AudioPlayback playback = preview;
		java.util.concurrent.atomic.AtomicInteger counter = pendingAudio;
		if (counter.getAndIncrement() == 0)
		{
			openingSince = monotonicMillis();
		}
		worker.execute(() -> {
			try
			{
				if (token != previewGeneration)
				{
					return;
				}
				playback.close();
				openLayer(playback, name, volume, token, true);
				openLayer(playback, unlockName, unlockVolume, token, true);
			}
			finally
			{
				counter.decrementAndGet();
			}
		});
	}
	private void openLayer(AudioPlayback playback, String name, int volume, int token, boolean test)
	{
		if (token != (test ? previewGeneration : generation) || volume <= 0 || !validName(name))
		{
			return;
		}
		try
		{
			javax.sound.sampled.Clip clip = openClip(file(name));
			try
			{
				if (token != (test ? previewGeneration : generation))
				{
					clip.close();
					return;
				}
				if (clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN))
				{
					javax.sound.sampled.FloatControl gain =
						(javax.sound.sampled.FloatControl)clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
					gain.setValue(
						Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), 20f * (float)Math.log10(Math.min(100, volume) / 100f))));
				}
				long lengthMillis = Math.max(1, clip.getMicrosecondLength() / 1000);
				// Device calls above may block while Stop/logout invalidates this request.
				if (token != (test ? previewGeneration : generation))
				{
					closeClip(clip);
					return;
				}
				playback.deadlines.put(clip, monotonicMillis() + lengthMillis + 250);
				playback.clips.add(clip);
				clip.start();
			}
			catch (Exception e)
			{
				playback.clips.remove(clip);
				playback.deadlines.remove(clip);
				closeClip(clip);
				throw e;
			}
		}
		catch (Exception e)
		{
			log.debug("Unable to play sound {}", name, e);
		}
	}

	void playNow(String name, int volume)
	{
		if (worker == null || volume <= 0 || !validName(name))
		{
			return;
		}
		int session = generation;
		AudioPlayback playback = rewards;
		java.util.concurrent.atomic.AtomicInteger counter = pendingAudio;
		if (counter.getAndIncrement() == 0)
		{
			openingSince = monotonicMillis();
		}
		worker.execute(() -> {
			try
			{
				openLayer(playback, name, volume, session, false);
			}
			finally
			{
				counter.decrementAndGet();
			}
		});
	}
}
