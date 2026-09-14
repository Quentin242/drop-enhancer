/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/** Local overrides take precedence; defaults are read directly from the plugin JAR. */
final class SoundResources
{
	private static final Map<String, String> LEGACY_DEFAULTS = Map.of(
		"botssouls-common.wav", "custom-sounds-common.wav",
		"botssouls-pet.wav", "custom-sounds-pet.wav",
		"botssouls-uncommon.wav", "custom-sounds-uncommon.wav",
		"botssouls-rare.wav", "custom-sounds-rare.wav",
		"botssouls-veryrare.wav", "custom-sounds-veryrare.wav",
		"botssouls-unlock.wav", "custom-sounds-unlock.wav");

	private SoundResources()
	{
	}

	static AudioInputStream open(File local) throws IOException, UnsupportedAudioFileException
	{
		if (local.exists())
		{
			return AudioSystem.getAudioInputStream(local);
		}
		String name = LEGACY_DEFAULTS.getOrDefault(local.getName(), local.getName());
		InputStream resource = SoundQueue.validName(name) ? SoundResources.class.getResourceAsStream("/sounds/" + name) : null;
		if (resource == null)
		{
			throw new FileNotFoundException("Sound is not installed: " + name);
		}
		BufferedInputStream buffered = new BufferedInputStream(resource);
		try
		{
			return AudioSystem.getAudioInputStream(buffered);
		}
		catch (IOException | UnsupportedAudioFileException | RuntimeException failure)
		{
			buffered.close();
			throw failure;
		}
	}
}
