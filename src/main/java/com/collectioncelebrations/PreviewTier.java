/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.awt.Color;
import net.runelite.api.gameval.ItemID;

public enum PreviewTier
{
	COMMON("Common", new Color(180, 180, 180), "common.wav", 7416, "Mole claw"),
	UNCOMMON("Uncommon", new Color(95, 195, 110), "uncommon.wav", 20220, "Holy blessing"),
	RARE("Rare", new Color(90, 155, 245), "rare.wav", ItemID.BANDOS_CHESTPLATE, "Bandos chestplate"),
	VERY_RARE("Very rare", new Color(190, 110, 240), "veryrare.wav", ItemID.TWISTED_BOW, "Twisted bow"),
	PET("Pet", new Color(240, 185, 65), "pet_sound.wav", ItemID.BANDOSPET, "Pet general graardor");

	final Color color;
	final String sound;
	final int itemId;
	final String itemName;
	private final String label;
	PreviewTier(String label, Color color, String sound, int itemId, String itemName)
	{
		this.label = label;
		this.color = color;
		this.sound = sound;
		this.itemId = itemId;
		this.itemName = itemName;
	}
	@Override
	public String toString()
	{
		return label;
	}
}
