/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import org.junit.Test;
import static org.junit.Assert.*;

public class RevealQueueTest
{
	@Test
	public void heldRewardsRemainInOrderAndReleaseOnce()
	{
		RevealQueue<String> q = new RevealQueue<>();
		q.add("first");
		q.add("second");
		assertNull(q.poll(x -> false));
		assertEquals(2, q.size());
		assertEquals("first", q.poll(x -> true));
		assertEquals("second", q.poll(x -> true));
		assertNull(q.poll(x -> true));
	}
	@Test
	public void boundedQueueSurvivesMissingRelease()
	{
		RevealQueue<Integer> q = new RevealQueue<>();
		for (int i = 0; i < 100; i++)
		{
			q.add(i);
		}
		assertEquals(64, q.size());
		assertEquals(Integer.valueOf(36), q.poll(x -> true));
		q.clear();
		assertNull(q.poll(x -> true));
	}
	@Test
	public void priorityOnlyReordersReadyItemsAndPreservesTies()
	{
		RevealQueue<Integer> queue = new RevealQueue<>();
		queue.add(1);
		queue.add(5);
		queue.add(3);
		assertEquals(Integer.valueOf(3), queue.poll(n -> n < 5, Integer::compare));
		assertEquals(Integer.valueOf(5), queue.poll(n -> true, Integer::compare));
		assertEquals(Integer.valueOf(1), queue.poll(n -> true, Integer::compare));
	}
}
