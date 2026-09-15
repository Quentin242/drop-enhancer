package com.collectioncelebrations;

import java.nio.file.*;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LocalSoundFilesTest
{
	@Rule
	public TemporaryFolder temp = new TemporaryFolder();
	@Test
	public void selectingImportsWithoutOverwritingAndReusesLocalFile() throws Exception
	{
		Path source = temp.getRoot().toPath().resolve("reward.wav"), folder = temp.getRoot().toPath().resolve("sounds");
		try (AudioInputStream audio =
				 new AudioInputStream(new ByteArrayInputStream(new byte[800]), new AudioFormat(8000, 8, 1, false, false), 800))
		{
			AudioSystem.write(audio, AudioFileFormat.Type.WAVE, source.toFile());
		}
		assertEquals("reward.wav", LocalSoundFiles.select(source, folder));
		assertEquals("reward (1).wav", LocalSoundFiles.select(source, folder));
		assertEquals("reward.wav", LocalSoundFiles.select(folder.resolve("reward.wav"), folder));
		assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(folder.resolve("reward.wav")));
	}
	@Test
	public void fakeWavCannotBeImported() throws Exception
	{
		Path source = temp.getRoot().toPath().resolve("bad.wav");
		Files.write(source, new byte[] {1, 2, 3});
		try
		{
			LocalSoundFiles.select(source, temp.getRoot().toPath().resolve("sounds"));
			fail();
		}
		catch (java.io.IOException expected)
		{
		}
	}
}
