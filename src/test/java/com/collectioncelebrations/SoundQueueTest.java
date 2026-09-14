/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.nio.file.Files;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import javax.sound.sampled.Clip;
import static org.mockito.Mockito.*;

public class SoundQueueTest
{
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();
	private SoundQueue sounds;
	private Clip audio;
	@Before
	public void setup() throws Exception
	{
		sounds = spy(new SoundQueue());
		audio = mock(Clip.class);
		doReturn(audio).when(sounds).openClip(any(java.io.File.class));
		sounds.gate = mock(CaseGate.class);
		sounds.config = mock(CelebrationConfig.class);
		sounds.runeliteDirectory = temporary.getRoot();
		java.nio.file.Path dir = temporary.getRoot().toPath().resolve("collection-celebrations/sounds");
		Files.createDirectories(dir);
		Files.write(dir.resolve("test.wav"), new byte[] {0});
		// Clip creation is mocked: no audio device is opened and no actual sound is played.
		sounds.start();
	}
	@After
	public void cleanup()
	{
		sounds.stop();
	}
	@Test
	public void rewardSoundWaitsAndPlaysOnlyOnce() throws Exception
	{
		when(sounds.gate.blocked()).thenReturn(true);
		sounds.offer("test.wav", 80, 0, true);
		sounds.tick(Long.MAX_VALUE);
		verifyNoInteractions(audio);
		when(sounds.gate.blocked()).thenReturn(false);
		sounds.tick(Long.MAX_VALUE);
		sounds.tick(Long.MAX_VALUE);
		verify(audio, timeout(1000).times(1)).start();
	}
	@Test
	public void logoutDropsQueuedAudio() throws Exception
	{
		sounds.offer("test.wav", 80, 10000, true);
		sounds.reset();
		sounds.tick(Long.MAX_VALUE);
		verifyNoInteractions(audio);
	}
	@Test
	public void zeroVolumeNeverStartsPlayback() throws Exception
	{
		sounds.offer("test.wav", 0, 0, false);
		sounds.tick(Long.MAX_VALUE);
		verifyNoInteractions(audio);
	}

	@Test
	public void replacingPreviewClosesOldClipWithoutOpeningRealAudio() throws Exception
	{
		javax.sound.sampled.Clip first = mock(javax.sound.sampled.Clip.class);
		javax.sound.sampled.Clip second = mock(javax.sound.sampled.Clip.class);
		doReturn(first, second).when(sounds).openClip(any(java.io.File.class));
		sounds.playPreview("test.wav", 80);
		verify(first, timeout(1000)).start();
		sounds.cancelPreview();
		sounds.playPreview("test.wav", 80);
		verify(second, timeout(1000)).start();
		verify(first).close();
		verify(second, never()).close();
		sounds.stop();
		verify(second, timeout(1000)).close();
	}

	@Test
	public void oldShutdownCannotCloseNewSessionsPreview() throws Exception
	{
		javax.sound.sampled.Clip first = mock(javax.sound.sampled.Clip.class);
		javax.sound.sampled.Clip second = mock(javax.sound.sampled.Clip.class);
		java.util.concurrent.CountDownLatch closing = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch drained = new java.util.concurrent.CountDownLatch(1);
		doAnswer(call -> {
			closing.countDown();
			release.await(2, java.util.concurrent.TimeUnit.SECONDS);
			return null;
		})
			.when(first)
			.stop();
		doReturn(first, second).when(sounds).openClip(any(java.io.File.class));
		doAnswer(call -> {
			drained.countDown();
			return null;
		})
			.when(first)
			.close();
		try
		{
			sounds.playPreview("test.wav", 80);
			verify(first, timeout(1000)).start();
			sounds.stop();
			org.junit.Assert.assertTrue(closing.await(1, java.util.concurrent.TimeUnit.SECONDS));
			sounds.start();
			sounds.playPreview("test.wav", 80);
			verify(second, timeout(1000)).start();
			release.countDown();
			org.junit.Assert.assertTrue(drained.await(1, java.util.concurrent.TimeUnit.SECONDS));
			verify(second, never()).close();
		}
		finally
		{
			release.countDown();
		}
	}

	@Test
	public void lateUnlockCancelsOnlyItsValueSound() throws Exception
	{
		sounds.offerValue("test.wav", 50, "Mole claw");
		sounds.offerValue("test.wav", 50, "Coins");
		sounds.cancelValue("MOLE CLAW");
		sounds.tick(Long.MAX_VALUE);
		verify(audio, timeout(1000).times(1)).start();
	}
	@Test
	public void valueAudioCanBeCancelledBeforeNextRender()
	{
		sounds.offerValue("test.wav", 50, "Mole claw");
		verify(sounds, never()).playNow(anyString(), anyInt());
		sounds.cancelValue("Mole claw");
		sounds.tick(Long.MAX_VALUE);
		verify(sounds, never()).playNow(anyString(), anyInt());
	}
	@Test
	public void ordinaryValueAudioIsImmediatelyEligibleButCasesStillHold()
	{
		when(sounds.gate.delayMillis()).thenReturn(1200L);
		sounds.offerValue("test.wav", 50, "Coins");
		long after = System.currentTimeMillis();
		when(sounds.gate.blocked()).thenReturn(true);
		sounds.tick(after);
		verify(sounds, never()).playNow(anyString(), anyInt());
		when(sounds.gate.blocked()).thenReturn(false);
		sounds.tick(after);
		verify(sounds).playNow("test.wav", 50);
	}
	@Test
	public void replacingPreviewClosesBothTierAndUnlockJingle() throws Exception
	{
		javax.sound.sampled.Clip tier = mock(javax.sound.sampled.Clip.class);
		javax.sound.sampled.Clip jingle = mock(javax.sound.sampled.Clip.class);
		doReturn(tier, jingle).when(sounds).openClip(any(java.io.File.class));
		sounds.playPreview("test.wav", 80, "test.wav", 60);
		verify(tier, timeout(1000)).start();
		verify(jingle, timeout(1000)).start();
		sounds.cancelPreview();
		verify(tier, timeout(1000)).close();
		verify(jingle, timeout(1000)).close();
	}

	@Test
	public void queuedAudioWaitsForClipToFinishAndResetStopsIt() throws Exception
	{
		when(audio.isRunning()).thenReturn(true);
		sounds.playNow("test.wav", 80);
		verify(audio, timeout(1000)).start();
		org.junit.Assert.assertTrue(sounds.busy());
		sounds.offerValue("test.wav", 50, "Coins");
		sounds.tick(Long.MAX_VALUE);
		verify(audio, times(1)).start();
		sounds.reset();
		verify(audio, timeout(1000)).close();
		org.junit.Assert.assertFalse(sounds.busy());
	}

	@Test
	public void stuckRunningFlagCannotHoldQueuePastActualWavDuration() throws Exception
	{
		doReturn(1000L).when(sounds).monotonicMillis();
		when(audio.getMicrosecondLength()).thenReturn(500000L);
		when(audio.isRunning()).thenReturn(true);
		sounds.playNow("test.wav", 80);
		verify(audio, timeout(1000)).start();
		org.junit.Assert.assertTrue(sounds.busy());
		doReturn(1800L).when(sounds).monotonicMillis();
		org.junit.Assert.assertFalse(sounds.busy());
		verify(audio, timeout(1000)).close();
	}

	@Test
	public void stalledDeviceOpenCannotFreezePopupQueue() throws Exception
	{
		java.util.concurrent.CompletableFuture<Void> entered = new java.util.concurrent.CompletableFuture<>();
		java.util.concurrent.CompletableFuture<Void> release = new java.util.concurrent.CompletableFuture<>();
		doReturn(1000L).when(sounds).monotonicMillis();
		doAnswer(call -> {
			entered.complete(null);
			release.get(2, java.util.concurrent.TimeUnit.SECONDS);
			return audio;
		})
			.when(sounds)
			.openClip(any(java.io.File.class));
		try
		{
			sounds.playNow("test.wav", 80);
			entered.get(1, java.util.concurrent.TimeUnit.SECONDS);
			doReturn(7000L).when(sounds).monotonicMillis();
			org.junit.Assert.assertFalse(sounds.busy());
			release.complete(null);
			verify(audio, never()).start();
		}
		finally
		{
			release.complete(null);
		}
	}
	@Test
	public void delayedRunningFlagDoesNotCutOffTierWhenJingleStarts() throws Exception
	{
		Clip jingle = mock(Clip.class);
		when(audio.getFrameLength()).thenReturn(22050);
		when(audio.getMicrosecondLength()).thenReturn(500000L);
		doReturn(audio, jingle).when(sounds).openClip(any(java.io.File.class));
		sounds.playPreview("test.wav", 80, "test.wav", 50);
		verify(jingle, timeout(1000)).start();
		verify(audio, never()).close();
		org.junit.Assert.assertTrue(sounds.busy());
		verify(audio, never()).close();
	}

	@Test
	public void brokenStopStillClosesAllLayersAndAllowsNextPreview() throws Exception
	{
		Clip jingle = mock(Clip.class);
		Clip next = mock(Clip.class);
		when(audio.isRunning()).thenReturn(true);
		doThrow(new IllegalStateException("device disconnected")).when(audio).stop();
		doReturn(audio, jingle, next).when(sounds).openClip(any(java.io.File.class));
		sounds.playPreview("test.wav", 80, "test.wav", 50);
		verify(jingle, timeout(1000)).start();
		sounds.cancelPreview();
		sounds.playPreview("test.wav", 80);
		verify(next, timeout(1000)).start();
		verify(audio).close();
		verify(jingle).close();
	}

	@Test
	public void failedClipStartAndCloseDoNotBlockNextSound() throws Exception
	{
		Clip next = mock(Clip.class);
		doThrow(new IllegalStateException("device disconnected")).when(audio).start();
		doThrow(new IllegalStateException("already disconnected")).when(audio).close();
		doReturn(audio, next).when(sounds).openClip(any(java.io.File.class));
		sounds.playNow("test.wav", 80);
		sounds.playNow("test.wav", 80);
		verify(next, timeout(1000)).start();
		verify(audio).close();
	}

	@Test
	public void completedFramesReleaseNextSoundWithoutWaitingForDeadline() throws Exception
	{
		doReturn(1000L).when(sounds).monotonicMillis();
		when(audio.getFrameLength()).thenReturn(22050);
		when(audio.getLongFramePosition()).thenReturn(22050L);
		when(audio.getMicrosecondLength()).thenReturn(500000L);
		Clip next = mock(Clip.class);
		doReturn(audio, next).when(sounds).openClip(any(java.io.File.class));
		sounds.playNow("test.wav", 80);
		verify(audio, timeout(1000)).start();
		// Let the worker finish its pending-open accounting; the audio clock stays fixed.
		sounds.offerValue("test.wav", 50, "Coins");
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(1);
		while (sounds.busy() && System.nanoTime() < deadline)
		{
			Thread.yield();
		}
		org.junit.Assert.assertFalse(sounds.busy());
		sounds.tick(Long.MAX_VALUE);
		verify(next, timeout(1000)).start();
		verify(audio).close();
	}

	@Test
	public void burstDuringCaseHoldKeepsOnlyLatest128SoundsInOrder()
	{
		doNothing().when(sounds).playNow(anyString(), anyInt());
		when(sounds.gate.blocked()).thenReturn(true);
		for (int i = 0; i < 1000; i++)
		{
			sounds.offerValue("sound" + i + ".wav", 50, "Coins");
			sounds.tick(Long.MAX_VALUE);
		}
		verify(sounds, never()).playNow(anyString(), anyInt());
		when(sounds.gate.blocked()).thenReturn(false);
		for (int i = 0; i < 130; i++)
		{
			sounds.tick(Long.MAX_VALUE);
		}
		org.mockito.ArgumentCaptor<String> files = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(sounds, times(128)).playNow(files.capture(), eq(50));
		for (int i = 0; i < 128; i++)
		{
			org.junit.Assert.assertEquals("sound" + (872 + i) + ".wav", files.getAllValues().get(i));
		}
	}

	@Test
	public void cancellationWhileDriverReadsLengthCannotStartStalePreview() throws Exception
	{
		java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
		doAnswer(call -> {
			entered.countDown();
			if (!release.await(2, java.util.concurrent.TimeUnit.SECONDS))
			{
				throw new IllegalStateException("test timed out");
			}
			return 500000L;
		}).when(audio).getMicrosecondLength();
		try
		{
			sounds.playPreview("test.wav", 80);
			org.junit.Assert.assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
			sounds.cancelPreview();
			release.countDown();
			verify(audio, timeout(1000)).close();
			verify(audio, never()).start();
		}
		finally
		{
			release.countDown();
		}
	}

}
