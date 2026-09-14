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
	private MenuEntry included;
	private MenuEntry parent;
	private Menu menu;
	private Menu submenu;
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
		menu = mock(Menu.class);
		submenu = mock(Menu.class);
		parent = mock(MenuEntry.class, RETURNS_SELF);
		included = mock(MenuEntry.class, RETURNS_SELF);
		when(plugin.client.getMenu()).thenReturn(menu);
		when(menu.createMenuEntry(-1)).thenReturn(parent);
		when(parent.createSubMenu()).thenReturn(submenu);
		when(submenu.createMenuEntry(-1)).thenReturn(included, added);
		when(plugin.items.canonicalize(123)).thenReturn(123);
		ItemComposition definition = mock(ItemComposition.class);
		when(definition.getName()).thenReturn("Mystic hat");
		when(plugin.items.getItemComposition(123)).thenReturn(definition);
	}
	@Test
	public void onlyExplicitShiftGroundMenuClickWritesSettings()
	{
		plugin.onMenuEntryAdded(new MenuEntryAdded(original));
		verify(menu, times(1)).createMenuEntry(-1);
		verify(parent).setOption("Drop Enhancer");
		verify(parent).setType(MenuAction.RUNELITE);
		verify(included).setOption("Include in popups");
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
		verifyNoInteractions(menu);
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
	@Test
	public void inclusionClickRemovesExactExclusionButPreservesWildcard()
	{
		when(plugin.config.excludedPopupItems()).thenReturn("Mystic hat, Mystic *");
		plugin.onMenuEntryAdded(new MenuEntryAdded(original));
		verifyNoInteractions(plugin.configManager);
		ArgumentCaptor<Consumer<MenuEntry>> callback = ArgumentCaptor.forClass(Consumer.class);
		verify(included).onClick(callback.capture());
		callback.getValue().accept(included);
		verify(plugin.configManager).setConfiguration("collection-celebrations", "includedPopupItems", "Mystic hat");
		verify(plugin.configManager).setConfiguration("collection-celebrations", "excludedPopupItems", "Mystic *");
	}

	@Test
	public void inclusionRemovalPreservesOtherRules()
	{
		when(plugin.config.includedPopupItems()).thenReturn("MYSTIC HAT, Rune *, mystic hat");
		plugin.onMenuEntryAdded(new MenuEntryAdded(original));
		verify(included).setOption("Remove popup inclusion");
		plugin.setPopupIncluded("Mystic hat", false);
		verify(plugin.configManager).setConfiguration("collection-celebrations", "includedPopupItems", "Rune *");
		verify(plugin.configManager, never()).setConfiguration(eq("collection-celebrations"), eq("excludedPopupItems"), anyString());
	}

}
