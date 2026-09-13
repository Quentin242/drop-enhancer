/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.plugins.grounditems.GroundItemsConfig;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

/** Evaluates public list/value settings against the received loot stack, not private overlay state. */
final class GroundItemSoundFilter
{
	private static final Pattern QUANTITY = Pattern.compile("^(.*?)([<>])\\s*(\\d+)\\s*$");

	static boolean hidden(GroundItemsConfig config, String name, int quantity, int ge, int ha, boolean tradeable)
	{
		int highlight = match(config.getHighlightItems(), name, quantity);
		int hide = match(config.getHiddenItems(), name, quantity);
		if (highlight > 0 || hide > 0)
		{
			return hide > highlight;
		}
		return (ge > 0 || tradeable || !config.dontHideUntradeables()) && ge < config.getHideUnderValue() &&
			ha < config.getHideUnderValue();
	}

	// Exact rules outrank wildcard rules; highlight wins equal-priority matches.
	static int match(String csv, String name, int quantity)
	{
		int best = 0;
		if (csv == null)
		{
			return best;
		}
		for (String entry : Text.fromCSV(csv))
		{
			String pattern = entry.trim();
			Matcher rule = QUANTITY.matcher(pattern);
			if (rule.matches())
			{
				int limit;
				try
				{
					limit = Integer.parseInt(rule.group(3));
				}
				catch (NumberFormatException ex)
				{
					continue;
				}
				if (rule.group(2).equals("<") ? quantity >= limit : quantity <= limit)
				{
					continue;
				}
				pattern = rule.group(1).trim();
			}
			if (pattern.contains("*"))
			{
				if (WildcardMatcher.matches(pattern, name))
				{
					best = Math.max(best, 1);
				}
			}
			else if (pattern.equalsIgnoreCase(name))
			{
				return 2;
			}
		}
		return best;
	}
}
