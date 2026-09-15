/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Reads bounded WAV metadata for queue timing. RuneLite AudioPlayer decodes and plays it. */
final class WavData
{
	private static final int MAX_BYTES = 32 * 1024 * 1024;
	final byte[] bytes;
	final long durationMillis;

	private WavData(byte[] bytes, long durationMillis)
	{
		this.bytes = bytes;
		this.durationMillis = durationMillis;
	}

	static WavData read(InputStream input) throws IOException
	{
		byte[] bytes = input.readNBytes(MAX_BYTES + 1);
		if (bytes.length < 12 || bytes.length > MAX_BYTES)
		{
			throw new IOException("Select a valid WAV smaller than 32 MB");
		}
		ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		long end = Integer.toUnsignedLong(buffer.getInt(4)) + 8;
		if (buffer.getInt(0) != 0x46464952 || buffer.getInt(8) != 0x45564157 || end < 12)
		{
			throw new IOException("Invalid RIFF/WAVE header");
		}
		// Some otherwise playable exporters overstate RIFF size. Validate every actual
		// chunk below instead; truncated sample data is still rejected.
		end = Math.min(end, bytes.length);
		long byteRate = 0, dataSize = 0;
		for (long offset = 12; offset + 8 <= end;)
		{
			int pos = (int)offset;
			int tag = buffer.getInt(pos);
			long size = Integer.toUnsignedLong(buffer.getInt(pos + 4));
			if (offset + 8 + size > end)
			{
				throw new IOException("Truncated WAV chunk");
			}
			if (tag == 0x20746d66)
			{
				if (size < 16)
				{
					throw new IOException("Invalid WAV format chunk");
				}
				int format = Short.toUnsignedInt(buffer.getShort(pos + 8));
				if (format != 1 && format != 3 && format != 0xfffe)
				{
					throw new IOException("Select an uncompressed PCM WAV");
				}
				byteRate = Integer.toUnsignedLong(buffer.getInt(pos + 16));
				if (buffer.getShort(pos + 10) <= 0 || buffer.getInt(pos + 12) <= 0 || buffer.getShort(pos + 20) <= 0)
				{
					throw new IOException("Invalid WAV sample format");
				}
			}
			else if (tag == 0x61746164)
			{
				dataSize += size;
			}
			offset += 8 + size + (size & 1);
		}
		if (byteRate == 0 || dataSize == 0)
		{
			throw new IOException("WAV has no playable sample data");
		}
		return new WavData(bytes, Math.max(1, (dataSize * 1000 + byteRate - 1) / byteRate));
	}
}
