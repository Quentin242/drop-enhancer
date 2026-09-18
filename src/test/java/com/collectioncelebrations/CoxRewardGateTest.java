package com.collectioncelebrations;

import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CoxRewardGateTest
{
	private static final String LINE = "New item added to your collection log: <col=ef1020>Twisted bow</col>";
	private static final String HIDDEN = "New item added to your collection log: ???";

	private CoxRewardGate gate()
	{
		CoxRewardGate gate = new CoxRewardGate();
		gate.client = mock(Client.class);
		gate.config = mock(CelebrationConfig.class, CALLS_REAL_METHODS);
		when(gate.config.hideCoxRewards()).thenReturn(true);
		return gate;
	}

	private Celebration drop(String name)
	{
		return new Celebration(name, -1, 1, "Chambers of Xeric", false, 0, 0, 0);
	}

	@Test
	public void onlyChambersUniquesAreCoveredAndOnlyWhenSwitchedOn()
	{
		CoxRewardGate gate = gate();
		assertTrue(gate.covers("Twisted bow"));
		assertTrue("Matching ignores case and padding", gate.covers("  ancestral robe top "));
		assertFalse(gate.covers("Abyssal whip"));
		// Other raids send their collection log only after the chest, so they are left alone.
		assertFalse(gate.covers("Scythe of Vitur (uncharged)"));
		assertFalse(gate.covers("Tumeken's guardian"));
		assertFalse(gate.covers(null));
		when(gate.config.hideCoxRewards()).thenReturn(false);
		assertFalse("Switched off it covers nothing at all", gate.covers("Twisted bow"));
	}

	@Test
	public void aUniqueWaitsForTheChestWhileOtherDropsGoThrough()
	{
		CoxRewardGate gate = gate();
		MessageNode node = mock(MessageNode.class);
		when(node.getValue()).thenReturn(LINE);
		Celebration bow = drop("Twisted bow"), whip = drop("Abyssal whip");

		assertFalse("Nothing is held before the raid announces a unique", gate.holds(bow));
		gate.censor(node);
		verify(node).setValue(HIDDEN);
		assertTrue(gate.holds(bow));
		assertFalse("An unrelated drop must not wait for the chest", gate.holds(whip));

		gate.noteChestOpened();
		verify(node).setValue(LINE);
		assertFalse("The chest is the reveal", gate.holds(bow));
	}

	@Test
	public void aRepeatedAnnouncementCannotLoseTheOriginalLine()
	{
		CoxRewardGate gate = gate();
		MessageNode node = mock(MessageNode.class);
		when(node.getValue()).thenReturn(LINE);
		gate.censor(node);
		when(node.getValue()).thenReturn(HIDDEN);
		gate.censor(node);
		gate.noteChestOpened();
		// Censoring twice must not store "???" as the text to restore.
		verify(node, times(1)).setValue(LINE);
		verify(node, never()).setValue(argThat(v -> v != null && v.equals(HIDDEN) && false));
	}

	@Test
	public void resetDropsTheHeldStateWithTheRestOfTheSession()
	{
		CoxRewardGate gate = gate();
		MessageNode node = mock(MessageNode.class);
		when(node.getValue()).thenReturn(LINE);
		gate.censor(node);
		gate.reset();
		assertFalse(gate.holds(drop("Twisted bow")));
		gate.noteChestOpened();
		// Logging out clears the chat itself, so no line is put back afterwards.
		verify(node, never()).setValue(LINE);
	}
}
