/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import org.junit.Test;
import static org.junit.Assert.*;

public class CollectionLedgerTest
{
	@Test
	public void unknownIsNotZeroOrARepeat()
	{
		CollectionLedger l = new CollectionLedger();
		assertNull(l.get(100));
		assertFalse(l.obtained(100));
	}
	@Test
	public void oldSnapshotCannotMasqueradeAsCurrentDropTotal()
	{
		CollectionLedger l = new CollectionLedger();
		l.observe(100, 1, 1);
		assertTrue(l.obtained(100));
		assertNull(l.confirmedAfter(100, 2));
		l.observe(100, 2, 3);
		assertEquals(Integer.valueOf(2), l.confirmedAfter(100, 2));
	}
	@Test
	public void refreshReplacesRatherThanAddsQuantities()
	{
		CollectionLedger l = new CollectionLedger();
		l.observe(100, 7, 1);
		l.observe(100, 7, 2);
		assertEquals(7, l.get(100).quantity);
		l.observe(100, 0, 3);
		assertFalse(l.obtained(100));
	}
	@Test
	public void resetPreventsCrossAccountCounts()
	{
		CollectionLedger l = new CollectionLedger();
		l.observe(100, 20, 1);
		l.clear();
		assertNull(l.get(100));
	}
	@Test
	public void invalidObservationsDoNotOverwriteValidData()
	{
		CollectionLedger l = new CollectionLedger();
		l.observe(100, 2, 1);
		l.observe(100, -1, 2);
		assertEquals(2, l.get(100).quantity);
	}
}
