/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

/** Bounded, non-blocking FIFO. Eligibility is checked before any presentation/audio side effect. */
final class RevealQueue<T>
{
	private final Deque<T> queue = new ArrayDeque<>();
	void add(T item)
	{
		if (queue.size() == 64)
		{
			queue.removeFirst();
		}
		queue.addLast(item);
	}
	T poll(Predicate<T> ready)
	{
		return !queue.isEmpty() && ready.test(queue.peekFirst()) ? queue.removeFirst() : null;
	}
	T poll(Predicate<T> ready, java.util.Comparator<T> priority)
	{
		T best = null;
		for (T candidate : queue)
		{
			if (ready.test(candidate) && (best == null || priority.compare(candidate, best) > 0))
			{
				best = candidate;
			}
		}
		if (best != null)
		{
			queue.removeFirstOccurrence(best);
		}
		return best;
	}
	void clear()
	{
		queue.clear();
	}
	int size()
	{
		return queue.size();
	}
	Iterable<T> items()
	{
		return queue;
	}
}
