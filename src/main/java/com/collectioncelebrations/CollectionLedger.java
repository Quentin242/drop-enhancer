/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.util.HashMap;
import java.util.Map;

/** Only authoritative collection-log observations enter this ledger. Never increment on loot. */
final class CollectionLedger
{
	static final class Entry
	{
		final int quantity;
		final long sequence;
		Entry(int quantity, long sequence)
		{
			this.quantity = quantity;
			this.sequence = sequence;
		}
	}
	private final Map<Integer, Entry> items = new HashMap<>();
	void observe(int itemId, int quantity, long sequence)
	{
		if (itemId >= 0 && quantity >= 0)
		{
			items.put(itemId, new Entry(quantity, sequence));
		}
	}
	Entry get(int itemId)
	{
		return items.get(itemId);
	}
	boolean obtained(int itemId)
	{
		Entry e = get(itemId);
		return e != null && e.quantity > 0;
	}
	Integer confirmedAfter(int itemId, long sequence)
	{
		Entry e = get(itemId);
		return e != null && e.sequence > sequence ? e.quantity : null;
	}
	void clear()
	{
		items.clear();
	}
}
