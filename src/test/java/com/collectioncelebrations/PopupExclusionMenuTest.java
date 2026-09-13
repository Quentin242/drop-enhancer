/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.util.function.Consumer;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PopupExclusionMenuTest
{
	private CelebrationPlugin plugin;
	private MenuEntry original;
	private MenuEntry added;
	@Before
	public void setup()
	{
		plugin = new CelebrationPlugin();
		plugin.client = mock(Client.class);
		plugin.config = mock(CelebrationConfig.class, CALLS_REAL_METHODS);
		plugin.configManager = mock(ConfigManager.class);
		plugin.items = mock(ItemManager.class);
		original = mock(MenuEntry.class);
		added = mock(MenuEntry.class, RETURNS_SELF);
		when(original.getType()).thenReturn(MenuAction.EXAMINE_ITEM_GROUND);
		when(original.getIdentifier()).thenReturn(123);
		when(original.getTarget()).thenReturn("<col=ffffff>Mystic hat");
		when(plugin.client.isKeyPressed(KeyCode.KC_SHIFT)).thenReturn(true);
		when(plugin.client.createMenuEntry(-1)).thenReturn(added);
		when(plugin.items.canonicalize(123)).thenReturn(123);
		ItemComposition definition = mock(ItemComposition.class);
		when(definition.getName()).thenReturn("Mystic hat");
		when(plugin.items.getItemComposition(123)).thenReturn(definition);
	}
	@Test
	public void onlyExplicitShiftGroundMenuClickWritesSettings()
	{
		plugin.onMenuEntryAdded(new MenuEntryAdded(original));
		verify(added).setOption("Exclude from popups");
		verify(added).setType(MenuAction.RUNELITE);
		verifyNoInteractions(plugin.configManager);
		ArgumentCaptor<Consumer<MenuEntry>> callback = ArgumentCaptor.forClass(Consumer.class);
		verify(added).onClick(callback.capture());
		callback.getValue().accept(added);
		verify(plugin.configManager).setConfiguration("collection-celebrations", "excludedPopupItems", "Mystic hat");
	}
	@Test
	public void normalRightClickAndInventoryMenusAreUnaffected()
	{
		when(plugin.client.isKeyPressed(KeyCode.KC_SHIFT)).thenReturn(false);
		plugin.onMenuEntryAdded(new MenuEntryAdded(original));
		when(plugin.client.isKeyPressed(KeyCode.KC_SHIFT)).thenReturn(true);
		when(original.getType()).thenReturn(MenuAction.EXAMINE_ITEM);
		plugin.onMenuEntryAdded(new MenuEntryAdded(original));
		verify(plugin.client, never()).createMenuEntry(anyInt());
		verifyNoInteractions(plugin.configManager);
	}
	@Test
	public void removalPreservesOtherNamesAndWildcardRules()
	{
		when(plugin.config.excludedPopupItems()).thenReturn("Bones, MYSTIC HAT, Rune *, mystic hat");
		plugin.onMenuEntryAdded(new MenuEntryAdded(original));
		verify(added).setOption("Remove popup exclusion");
		plugin.setPopupExcluded("Mystic hat", false);
		ArgumentCaptor<String> csv = ArgumentCaptor.forClass(String.class);
		verify(plugin.configManager).setConfiguration(eq("collection-celebrations"), eq("excludedPopupItems"), csv.capture());
		assertEquals(java.util.List.of("Bones", "Rune *"), Text.fromCSV(csv.getValue().toString()));
	}
}
