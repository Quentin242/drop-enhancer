/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

final class LocalSoundFiles
{
	static String select(Path source, Path folder) throws Exception
	{
		if (!Files.isRegularFile(source) || Files.size(source) > 32L * 1024 * 1024)
		{
			throw new IOException("Select a WAV smaller than 32 MB");
		}
		if (!AudioFileFormat.Type.WAVE.equals(AudioSystem.getAudioFileFormat(source.toFile()).getType()))
		{
			throw new IOException("File is not WAV audio");
		}
		Files.createDirectories(folder);
		String name = source.getFileName().toString();
		if (Files.isSameFile(source.getParent(), folder) && SoundQueue.validName(name))
		{
			return name;
		}
		String base = name.replaceFirst("(?i)\\.wav$", "").replaceAll("[^A-Za-z0-9_ ()-]", "_");
		if (base.isBlank())
		{
			base = "sound";
		}
		for (int i = 0; i < 10000; i++)
		{
			String candidate = base + (i == 0 ? "" : " (" + i + ")") + ".wav";
			try
			{
				Files.copy(source, folder.resolve(candidate));
				return candidate;
			}
			catch (FileAlreadyExistsException collision)
			{ /* Preserve existing sounds. */
			}
		}
		throw new IOException("Too many files with this name");
	}
}
