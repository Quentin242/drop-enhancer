/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SoundResourcesTest
{

	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void freshInstallationCanDecodeEveryDefaultWithoutAnAudioDevice() throws Exception
	{
		CelebrationConfig config = new CelebrationConfig() {};
		String[] names = {config.fileCommon(), config.fileUncommon(), config.fileRare(), config.fileVeryRare(), config.filePet(),
			config.unlockFile()};
		for (String name : names)
		{
			try (AudioInputStream stream = SoundResources.open(new File(temporary.getRoot(), name)))
			{
				assertTrue(name, stream.getFrameLength() > 0);
				assertTrue(name, stream.getFormat().getSampleRate() > 0);
				assertTrue(name, stream.read(new byte[4096]) > 0);
			}
		}
	}

	@Test
	public void localOverrideWinsOverBundledDefault() throws Exception
	{
		File override = new File(temporary.getRoot(), "botssouls-common.wav");
		try (java.io.InputStream source = getClass().getResourceAsStream("/sounds/botssouls-pet.wav"))
		{
			Files.copy(source, override.toPath());
		}
		try (AudioInputStream local = SoundResources.open(override);
			 AudioInputStream pet = SoundResources.open(new File(temporary.getRoot(), "botssouls-pet.wav")))
		{
			assertEquals(pet.getFrameLength(), local.getFrameLength());
		}
	}

	@Test(expected = UnsupportedAudioFileException.class)
	public void invalidLocalOverrideDoesNotUnexpectedlyPlayDefault() throws Exception
	{
		File override = temporary.newFile("botssouls-common.wav");
		Files.writeString(override.toPath(), "invalid WAV");
		SoundResources.open(override);
	}

	@Test(expected = FileNotFoundException.class)
	public void unknownCustomNameHasNoFallback() throws Exception
	{
		SoundResources.open(new File(temporary.getRoot(), "missing-custom.wav"));
	}
	@Test
	public void savedLegacyDefaultsDecodeTheNewRecordings() throws Exception
	{
		for (String tier : new String[] {"common", "uncommon", "rare", "veryrare", "unlock"})
		{
			try (AudioInputStream legacy = SoundResources.open(new File(temporary.getRoot(), "botssouls-" + tier + ".wav"));
				AudioInputStream current = SoundResources.open(new File(temporary.getRoot(), "custom-sounds-" + tier + ".wav")))
			{
				assertEquals(current.getFormat().toString(), legacy.getFormat().toString());
				org.junit.Assert.assertArrayEquals(current.readAllBytes(), legacy.readAllBytes());
			}
		}
	}

	@Test
	public void newDefaultNamesStillAllowLocalOverrides() throws Exception
	{
		File override = new File(temporary.getRoot(), "custom-sounds-common.wav");
		try (java.io.InputStream source = getClass().getResourceAsStream("/sounds/botssouls-pet.wav"))
		{
			Files.copy(source, override.toPath());
		}
		try (AudioInputStream local = SoundResources.open(override);
			AudioInputStream pet = SoundResources.open(new File(temporary.getRoot(), "botssouls-pet.wav")))
		{
			org.junit.Assert.assertArrayEquals(pet.readAllBytes(), local.readAllBytes());
		}
	}

}
