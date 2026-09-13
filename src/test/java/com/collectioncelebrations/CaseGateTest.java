/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.util.function.Predicate;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CaseGateTest
{
	private static class Gate extends CaseGate
	{
		long time = 1000;
		@Override
		long now()
		{
			return time;
		}
	}
	private Gate gate;
	private Overlay overlay;
	@Before
	public void setup()
	{
		gate = new Gate();
		gate.client = mock(Client.class);
		gate.overlays = mock(OverlayManager.class);
		overlay = mock(Overlay.class);
		when(gate.overlays.anyMatch(any())).thenAnswer(i -> ((Predicate<Overlay>)i.getArgument(0)).test(overlay));
	}
	@Test
	public void clueWaitsUntilRewardWidgetsAreRestored()
	{
		when(overlay.getName()).thenReturn("ClueCaseOverlay");
		gate.refresh();
		Widget w = mock(Widget.class);
		when(gate.client.getWidget(InterfaceID.TrailRewardscreen.ITEMS)).thenReturn(w);
		when(w.isSelfHidden()).thenReturn(true);
		assertTrue(gate.blocked());
		when(w.isSelfHidden()).thenReturn(false);
		assertFalse(gate.blocked());
	}
	@Test
	public void raidRewardCanHoldEvenIfHideChatAndNotificationAreOff()
	{
		when(overlay.getName()).thenReturn("LootCaseOpeningOverlay");
		gate.refresh();
		Widget w = mock(Widget.class);
		when(gate.client.getWidget(InterfaceID.ToaChests.UNIVERSE)).thenReturn(w);
		when(w.isSelfHidden()).thenReturn(true);
		assertTrue(gate.blocked());
		when(w.isSelfHidden()).thenReturn(false);
		assertFalse(gate.blocked());
	}
	@Test
	public void unrelatedNpcDoesNotGetTenSecondFallback()
	{
		when(overlay.getName()).thenReturn("LootCaseOpeningOverlay");
		gate.refresh();
		gate.noteReward("General Graardor");
		assertFalse(gate.blocked());
		gate.noteReward("Barrows");
		assertTrue(gate.blocked());
		gate.time += 11000;
		assertFalse(gate.blocked());
	}
	@Test
	public void normallyHiddenGlobalUiDoesNotHoldUnrelatedRewards()
	{
		when(overlay.getName()).thenReturn("LootCaseOpeningOverlay");
		gate.refresh();
		Widget w = mock(Widget.class);
		when(w.isSelfHidden()).thenReturn(true);
		when(gate.client.getWidget(InterfaceID.NotificationDisplay.UNIVERSE)).thenReturn(w);
		when(gate.client.getWidget(InterfaceID.Inventory.ITEMS)).thenReturn(w);
		gate.noteReward("General Graardor");
		assertFalse(gate.blocked());
		gate.noteReward("Barrows");
		gate.time += 11000;
		assertTrue(gate.blocked());
		gate.time += 120000;
		assertFalse(gate.blocked());
	}

	@Test
	public void removingCasePluginReleasesQueue()
	{
		when(overlay.getName()).thenReturn("LootCaseOpeningOverlay");
		gate.refresh();
		gate.noteReward("Barrows");
		assertTrue(gate.blocked());
		when(overlay.getName()).thenReturn("UnrelatedOverlay");
		gate.refresh();
		assertFalse(gate.blocked());
	}
}
