/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.awt.Color;

final class TierStyle
{
	static Color color(PreviewTier tier, CelebrationConfig c, net.runelite.client.plugins.grounditems.GroundItemsConfig ground)
	{
		if (!c.followGroundItemColours())
		{
			return color(tier, c);
		}
		if (tier == PreviewTier.PET)
		{
			return new Color(0x64FFE0);
		}
		if (ground == null)
		{
			return color(tier, c);
		}
		Color selected;
		switch (tier == null ? PreviewTier.COMMON : tier)
		{
		case UNCOMMON:
			selected = ground.mediumValueColor();
			break;
		case RARE:
			selected = ground.highValueColor();
			break;
		case VERY_RARE:
			selected = ground.insaneValueColor();
			break;
		default:
			selected = ground.lowValueColor();
		}
		// Ground Items permits alpha; transparent text colours should not erase a popup frame.
		return selected == null ? color(tier, c) : new Color(selected.getRGB() & 0xFFFFFF);
	}

	static Color color(PreviewTier tier, CelebrationConfig c)
	{
		if (tier == null)
		{
			return c.accent();
		}
		switch (tier)
		{
		case UNCOMMON:
			return c.colourUncommon();
		case RARE:
			return c.colourRare();
		case VERY_RARE:
			return c.colourVeryRare();
		case PET:
			return c.colourPet();
		default:
			return c.colourCommon();
		}
	}
	static boolean repeats(PreviewTier tier, CelebrationConfig c)
	{
		switch (tier == null ? PreviewTier.COMMON : tier)
		{
		case UNCOMMON:
			return c.repeatUncommon();
		case RARE:
			return c.repeatRare();
		case VERY_RARE:
			return c.repeatVeryRare();
		case PET:
			return c.repeatPet();
		default:
			return c.repeatCommon();
		}
	}
	static String file(PreviewTier tier, CelebrationConfig c)
	{
		switch (tier == null ? PreviewTier.COMMON : tier)
		{
		case UNCOMMON:
			return c.fileUncommon();
		case RARE:
			return c.fileRare();
		case VERY_RARE:
			return c.fileVeryRare();
		case PET:
			return c.filePet();
		default:
			return c.fileCommon();
		}
	}
	static int volume(PreviewTier tier, CelebrationConfig c)
	{
		int percentage;
		switch (tier == null ? PreviewTier.COMMON : tier)
		{
		case UNCOMMON:
			percentage = c.soundEnabledUncommon() ? c.volumeUncommon() : 0;
			break;
		case RARE:
			percentage = c.soundEnabledRare() ? c.volumeRare() : 0;
			break;
		case VERY_RARE:
			percentage = c.soundEnabledVeryRare() ? c.volumeVeryRare() : 0;
			break;
		case PET:
			percentage = c.soundEnabledPet() ? c.volumePet() : 0;
			break;
		default:
			percentage = c.soundEnabledCommon() ? c.volumeCommon() : 0;
		}
		return Math.max(0, Math.min(100, c.masterVolume())) * Math.max(0, Math.min(100, percentage)) / 100;
	}
}
