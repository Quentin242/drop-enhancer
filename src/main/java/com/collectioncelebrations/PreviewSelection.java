/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

public enum PreviewSelection
{
	OFF(null),
	COMMON(PreviewTier.COMMON),
	UNCOMMON(PreviewTier.UNCOMMON),
	RARE(PreviewTier.RARE),
	VERY_RARE(PreviewTier.VERY_RARE),
	PET(PreviewTier.PET);
	final PreviewTier tier;
	PreviewSelection(PreviewTier tier)
	{
		this.tier = tier;
	}
	@Override
	public String toString()
	{
		return tier == null ? "Off" : tier.toString();
	}
}
