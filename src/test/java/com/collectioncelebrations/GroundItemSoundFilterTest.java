package com.collectioncelebrations;

import net.runelite.client.plugins.grounditems.GroundItemsConfig;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GroundItemSoundFilterTest
{
	private final GroundItemsConfig config = mock(GroundItemsConfig.class, CALLS_REAL_METHODS);
	private boolean hidden(String name, int quantity, int ge, int ha, boolean tradeable)
	{
		return GroundItemSoundFilter.hidden(config, name, quantity, ge, ha, tradeable);
	}

	@Test
	public void namesWildcardsQuantityAndLiveChanges()
	{
		when(config.getHiddenItems()).thenReturn("Rune *, Coins < 100, Bones");
		assertTrue(hidden("rune sword", 1, 100000, 10000, true));
		assertTrue(hidden("COINS", 99, 99, 99, true));
		assertFalse(hidden("Coins", 100, 100, 100, true));
		assertTrue(hidden("bones", 1, 100, 100, true));
		when(config.getHiddenItems()).thenReturn("");
		assertFalse(hidden("bones", 1, 100, 100, true));
	}
	@Test
	public void exactRulesWinAndHighlightsBreakTies()
	{
		when(config.getHiddenItems()).thenReturn("Rune sword");
		when(config.getHighlightItems()).thenReturn("Rune *");
		assertTrue(hidden("Rune sword", 1, 100, 100, true));
		when(config.getHighlightItems()).thenReturn("Rune sword");
		assertFalse(hidden("Rune sword", 1, 100, 100, true));
		when(config.getHiddenItems()).thenReturn("Rune *");
		assertFalse(hidden("Rune sword", 1, 100, 100, true));
	}
	@Test
	public void thresholdRequiresBothPricesAndHonoursUntradeables()
	{
		when(config.getHideUnderValue()).thenReturn(100);
		assertTrue(hidden("Test", 1, 99, 99, true));
		assertFalse(hidden("Test", 1, 100, 99, true));
		assertFalse(hidden("Test", 1, 99, 100, true));
		assertFalse(hidden("Test", 1, 0, 0, false));
		when(config.dontHideUntradeables()).thenReturn(false);
		assertTrue(hidden("Test", 1, 0, 0, false));
		when(config.getHighlightItems()).thenReturn("Test");
		assertFalse(hidden("Test", 1, 0, 0, false));
	}
}
