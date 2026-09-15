/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Test;
import static org.junit.Assert.*;

public class WavDataTest
{
	private byte[] wav()
	{
		ByteBuffer b = ByteBuffer.allocate(844).order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(0x46464952).putInt(836).putInt(0x45564157);
		b.putInt(0x20746d66).putInt(16).putShort((short)1).putShort((short)1);
		b.putInt(8000).putInt(8000).putShort((short)1).putShort((short)8);
		b.putInt(0x61746164).putInt(800);
		return b.array();
	}

	@Test public void durationUsesSampleDataRatherThanContainerSize() throws Exception
	{
		byte[] bytes = wav();
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 844);
		WavData data = WavData.read(new ByteArrayInputStream(bytes));
		assertEquals(100, data.durationMillis);
		assertArrayEquals(bytes, data.bytes);
	}

	@Test(expected = IOException.class)
	public void truncatedDataIsRejected() throws Exception
	{
		WavData.read(new ByteArrayInputStream(java.util.Arrays.copyOf(wav(), 843)));
	}

	@Test(expected = IOException.class)
	public void hugeChunkCannotOverflowBounds() throws Exception
	{
		byte[] bytes = wav();
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(40, -1);
		WavData.read(new ByteArrayInputStream(bytes));
	}

	@Test(expected = IOException.class)
	public void invalidByteRateIsRejected() throws Exception
	{
		byte[] bytes = wav();
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(28, 0);
		WavData.read(new ByteArrayInputStream(bytes));
	}
}
