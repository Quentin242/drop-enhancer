/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayManager;

@Singleton
class CaseGate
{
	@Inject
	Client client;
	@Inject
	OverlayManager overlays;
	private static final long MINIMUM_CASE_MILLIS = 10000;
	private boolean clue;
	private boolean loot;
	private long minimumUntil;
	private long indicatorUntil;
	private static final Set<String> LOOT_SOURCES =
		Set.of("corrupted hunllef", "crystalline hunllef", "theatre of blood", "chambers of xeric", "tombs of amascut", "lunar chest",
			   "barrows", "hallowed sepulchre grand coffin", "elven crystal chest", "chest (moon key)", "larran's big chest",
			   "zombie pirate's locker", "unsired", "doom of mokhaiotl");
	long now()
	{
		return System.currentTimeMillis();
	}
	void reset()
	{
		minimumUntil = 0;
		indicatorUntil = 0;
	}
	void noteReward(String source)
	{
		String name = source == null ? "" : source.toLowerCase(Locale.ROOT);
		if (((clue && name.startsWith("clue scroll")) || (loot && LOOT_SOURCES.contains(name))))
		{
			minimumUntil = Math.max(minimumUntil, now() + MINIMUM_CASE_MILLIS);
			indicatorUntil = now() + 120000;
		}
	}
	void noteDoomInterface()
	{
		if (loot)
		{
			minimumUntil = Math.max(minimumUntil, now() + MINIMUM_CASE_MILLIS);
			indicatorUntil = now() + 120000;
		}
	}

	void refresh()
	{
		// Identify case plugins by their public overlay names.
		clue = overlays.anyMatch(o -> "ClueCaseOverlay".equals(o.getName()));
		loot = overlays.anyMatch(o -> "LootCaseOpeningOverlay".equals(o.getName()));
	}
	boolean installed()
	{
		return (clue || loot);
	}
	long delayMillis()
	{
		return 0;
	}
	boolean blocked()
	{
		if (!installed())
		{
			return false;
		}
		if (now() < minimumUntil)
		{
			return true;
		}
		if (clue && (hidden(InterfaceID.TrailRewardscreen.UNIVERSE) || hidden(InterfaceID.TrailRewardscreen.ITEMS)))
		{
			return true;
		}
		boolean held =
			(now() < indicatorUntil && (hidden(InterfaceID.NotificationDisplay.UNIVERSE) ||
										(loot && (hidden(InterfaceID.Chatbox.UNIVERSE) || hidden(InterfaceID.Inventory.ITEMS))))) ||
			(loot && (hidden(InterfaceID.BarrowsReward.UNIVERSE) || hidden(InterfaceID.PmoonReward.UNIVERSAL) ||
					  hidden(InterfaceID.RaidsRewards.UNIVERSE) || hidden(InterfaceID.TobChests.UNIVERSE) ||
					  hidden(InterfaceID.ToaChests.UNIVERSE) || hidden(InterfaceID.DomEndLevelUi.UNIVERSE)));
		if (!held)
		{
			indicatorUntil = 0;
		}
		return held;
	}
	private boolean hidden(int component)
	{
		Widget widget = client.getWidget(component);
		return widget != null && widget.isSelfHidden();
	}
}
