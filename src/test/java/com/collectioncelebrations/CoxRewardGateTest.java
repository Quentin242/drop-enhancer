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
	private static final String PLAIN = "New item added to your collection log: Twisted bow";

	private CoxRewardGate gate()
	{
		CoxRewardGate gate = new CoxRewardGate();
		gate.client = mock(Client.class);
		gate.config = mock(CelebrationConfig.class, CALLS_REAL_METHODS);
		when(gate.config.hideCoxRewards()).thenReturn(true);
		inside(gate, true);
		return gate;
	}

	private void inside(CoxRewardGate gate, boolean raiding)
	{
		when(gate.client.getVarbitValue(net.runelite.api.gameval.VarbitID.RAIDS_CLIENT_INDUNGEON))
			.thenReturn(raiding ? 1 : 0);
		gate.tick();
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
		assertTrue(gate.hide(PLAIN, node));
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
		gate.hide(PLAIN, node);
		when(node.getValue()).thenReturn(HIDDEN);
		gate.hide(PLAIN, node);
		gate.noteChestOpened();
		// Censoring twice must not store "???" as the text to restore.
		verify(node, times(1)).setValue(LINE);
		verify(node, never()).setValue(argThat(v -> v != null && v.equals(HIDDEN) && false));
	}

	@Test
	public void everyWayARaidNamesAUniqueIsHidden()
	{
		// A team mate's unlock, broadcast to the clan.
		CoxRewardGate gate = gate();
		MessageNode mate = mock(MessageNode.class);
		when(mate.getValue()).thenReturn("Zezima received a new collection log item: Twisted bow.");
		assertTrue(gate.hide("Zezima received a new collection log item: Twisted bow.", mate));
		verify(mate).setValue("Zezima received a new collection log item: ???");

		// Your own unique you already owned, so there is no collection log line at all.
		gate = gate();
		MessageNode repeat = mock(MessageNode.class);
		when(repeat.getValue()).thenReturn("Valuable drop: Elder maul (1,234,567 coins)");
		assertTrue(gate.hide("Valuable drop: Elder maul (1,234,567 coins)", repeat));
		verify(repeat).setValue("Valuable drop: ???");
		assertTrue("A repeat unique must be held too", gate.holds(drop("Elder maul")));

		// The raid's own broadcast when a team mate gets the purple.
		gate = gate();
		MessageNode raid = mock(MessageNode.class);
		when(raid.getValue()).thenReturn("Zezima received special loot from a raid: Kodai insignia.");
		assertTrue(gate.hide("Zezima received special loot from a raid: Kodai insignia.", raid));
		verify(raid).setValue("Zezima received special loot from a raid: ???");

		// Anything else in the same shape is left alone.
		gate = gate();
		MessageNode whip = mock(MessageNode.class);
		assertFalse(gate.hide("Valuable drop: Abyssal whip (2,000,000 coins)", whip));
		verify(whip, never()).setValue(anyString());
		assertFalse(gate.holds(drop("Abyssal whip")));
	}

	@Test
	public void aHoldSurvivesALineThatCannotBeEdited()
	{
		CoxRewardGate gate = gate();
		MessageNode tagged = mock(MessageNode.class);
		// Tags split the prefix, so the text cannot be rewritten safely.
		when(tagged.getValue()).thenReturn("<col=ff>Valuable<col=00> drop: Twisted bow");
		assertTrue(gate.hide("Valuable drop: Twisted bow", tagged));
		verify(tagged, never()).setValue(anyString());
		assertTrue("The popup must still wait for the chest", gate.holds(drop("Twisted bow")));
	}

	@Test
	public void aBroadcastFromOutsideARaidIsLeftAlone()
	{
		CoxRewardGate gate = gate();
		inside(gate, false);
		MessageNode mate = mock(MessageNode.class);
		String line = "Zezima received special loot from a raid: Kodai insignia.";
		// You are fishing somewhere; a clanmate's purple is none of this plugin's business, and there
		// is no chest of yours left to clear the hold with.
		assertFalse(gate.hide(line, mate));
		verify(mate, never()).setValue(anyString());
		assertFalse(gate.holds(drop("Kodai insignia")));
		gate.arm("Kodai insignia");
		assertFalse(gate.holds(drop("Kodai insignia")));
	}

	@Test
	public void aChestAlreadyOpenedIsNotReArmedUntilTheNextRaid()
	{
		CoxRewardGate gate = gate();
		gate.arm("Twisted bow");
		assertTrue(gate.holds(drop("Twisted bow")));
		gate.noteChestOpened();
		// A collection log line that lands after the chest must not strand the drop again.
		gate.arm("Twisted bow");
		assertFalse(gate.holds(drop("Twisted bow")));
		// Leaving and entering again is a new raid, so a new unique is held once more.
		inside(gate, false);
		inside(gate, true);
		gate.arm("Twisted bow");
		assertTrue(gate.holds(drop("Twisted bow")));
	}

	@Test
	public void leavingTheRaidReleasesWhateverWasStillHeld()
	{
		CoxRewardGate gate = gate();
		MessageNode node = mock(MessageNode.class);
		when(node.getValue()).thenReturn(LINE);
		gate.hide(PLAIN, node);
		inside(gate, false);
		verify(node).setValue(LINE);
		assertFalse(gate.holds(drop("Twisted bow")));
	}

	@Test
	public void resetDropsTheHeldStateWithTheRestOfTheSession()
	{
		CoxRewardGate gate = gate();
		MessageNode node = mock(MessageNode.class);
		when(node.getValue()).thenReturn(LINE);
		gate.hide(PLAIN, node);
		gate.reset();
		assertFalse(gate.holds(drop("Twisted bow")));
		gate.noteChestOpened();
		// Logging out clears the chat itself, so no line is put back afterwards.
		verify(node, never()).setValue(LINE);
	}
}
