/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;

/**
 * Holds a Chambers of Xeric unique back until its reward chest is claimed.
 * <p>Chambers announces a unique, in the chat and in the collection log, while the team is still
 * walking to the chest. Every other raid sends its collection log only once the chest is open, so
 * only Chambers needs this. Without it the popup and its sound give away the drop that the walk to
 * the chest exists to reveal.
 */
@Singleton
class CoxRewardGate
{
	@Inject
	Client client;
	@Inject
	CelebrationConfig config;

	/** The Chambers unique table, as the collection log words each item. */
	private static final Set<String> UNIQUES =
		Set.of("dexterous prayer scroll", "arcane prayer scroll", "twisted buckler", "dragon hunter crossbow",
			   "dinh's bulwark", "ancestral hat", "ancestral robe top", "ancestral robe bottom", "dragon claws",
			   "elder maul", "kodai insignia", "twisted bow");

	private final List<MessageNode> censored = new ArrayList<>();
	private final List<String> originals = new ArrayList<>();
	private boolean awaitingChest;

	/** @return whether the option is on and {@code name} is one of the Chambers uniques. */
	boolean covers(String name)
	{
		return config.hideCoxRewards() && name != null && UNIQUES.contains(name.trim().toLowerCase(Locale.ROOT));
	}

	/** @return whether this celebration waits for the chest instead of being presented now. */
	boolean holds(Celebration c)
	{
		return awaitingChest && c != null && covers(c.name);
	}

	/**
	 * Starts holding the drop back, and blanks the announcement where there is one to blank. The
	 * hold does not depend on the chat line: a missing or already censored node must never let the
	 * popup through early.
	 */
	void censor(MessageNode node)
	{
		awaitingChest = true;
		if (node == null || censored.contains(node))
		{
			return;
		}
		censored.add(node);
		originals.add(node.getValue());
		node.setValue("New item added to your collection log: ???");
		client.refreshChat();
	}

	/** The reward interface is the moment the player learns what they got. */
	void noteChestOpened()
	{
		awaitingChest = false;
		reveal();
	}

	private void reveal()
	{
		if (censored.isEmpty())
		{
			return;
		}
		for (int i = 0; i < censored.size(); i++)
		{
			censored.get(i).setValue(originals.get(i));
		}
		censored.clear();
		originals.clear();
		client.refreshChat();
	}

	/**
	 * Logging out clears the chat itself, so nothing is restored: the held celebration is dropped
	 * with the rest of the session, exactly as a queued reward already is.
	 */
	void reset()
	{
		censored.clear();
		originals.clear();
		awaitingChest = false;
	}
}
