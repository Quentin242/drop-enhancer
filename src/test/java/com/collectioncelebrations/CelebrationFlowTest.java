/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.util.List;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CelebrationFlowTest
{
	private static final int ITEM = 11832;
	private static final String NAME = "Bandos chestplate";
	private static class TestPlugin extends CelebrationPlugin
	{
		long time = System.currentTimeMillis();
		@Override
		long now()
		{
			return time;
		}
	}
	private TestPlugin p;
	@Before
	public void setup()
	{
		p = new TestPlugin();
		p.client = mock(Client.class);
		p.items = mock(ItemManager.class);
		p.config = mock(CelebrationConfig.class, CALLS_REAL_METHODS);
		p.gate = mock(CaseGate.class);
		p.sounds = mock(SoundQueue.class);
		p.overlay = mock(CelebrationOverlay.class);
		p.wiki = mock(WikiRarity.class);
		p.custom = mock(CustomSoundEvents.class);
		p.kills = new KillCountTracker();
		when(p.client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(p.config.repeatDrops()).thenReturn(true);
		when(p.config.repeatCommon()).thenReturn(true);
		when(p.config.collectionAudio()).thenReturn(true);
		when(p.config.masterVolume()).thenReturn(70);
		when(p.config.fileCommon()).thenReturn("CollectionLog.wav");
		when(p.config.fileUncommon()).thenReturn("uncommon.wav");
		when(p.config.fileRare()).thenReturn("rare.wav");
		when(p.config.fileVeryRare()).thenReturn("veryrare.wav");
		when(p.config.filePet()).thenReturn("pet_sound.wav");
		when(p.config.volumeCommon()).thenReturn(100);
		when(p.config.volumeUncommon()).thenReturn(100);
		when(p.config.volumeRare()).thenReturn(100);
		when(p.config.volumeVeryRare()).thenReturn(100);
		when(p.config.volumePet()).thenReturn(100);
		when(p.overlay.idle()).thenReturn(true);
		when(p.gate.delayMillis()).thenReturn(1200L);
		ItemComposition item = mock(ItemComposition.class);
		when(item.getName()).thenReturn(NAME);
		when(p.items.canonicalize(ITEM)).thenReturn(ITEM);
		when(p.items.getItemComposition(ITEM)).thenReturn(item);
	}
	private void sync(int quantity)
	{
		ScriptPreFired e = new ScriptPreFired(4100);
		ScriptEvent script = mock(ScriptEvent.class);
		when(script.getArguments()).thenReturn(new Object[] {4100, ITEM, quantity});
		e.setScriptEvent(script);
		p.onScriptPreFired(e);
	}
	private void loot()
	{
		p.onLootReceived(new LootReceived("General Graardor", 624, LootRecordType.NPC, List.of(new ItemStack(ITEM, 1)), 1, null));
	}
	private void chat(String message)
	{
		ChatMessage e = new ChatMessage();
		e.setType(ChatMessageType.GAMEMESSAGE);
		e.setMessage(message);
		p.onChatMessage(e);
	}
	private Celebration release()
	{
		p.time += 1500;
		p.onBeforeRender(new BeforeRender());
		ArgumentCaptor<Celebration> c = ArgumentCaptor.forClass(Celebration.class);
		verify(p.overlay).show(c.capture(), anyLong());
		return c.getValue();
	}
	@Test
	public void noPopupOrCollectionSoundBeforeCaseRelease()
	{
		sync(1);
		loot();
		when(p.gate.blocked()).thenReturn(true);
		p.time += 50000;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		verify(p.sounds, never()).playNow(anyString(), anyInt());
		when(p.gate.blocked()).thenReturn(false);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay).show(any(), anyLong());
		verify(p.sounds).playNow("CollectionLog.wav", 70);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, times(1)).show(any(), anyLong());
	}
	@Test
	public void refreshedOfficialTotalAppearsOnRepeat()
	{
		sync(1);
		loot();
		sync(2);
		Celebration c = release();
		assertFalse(c.newSlot);
		assertEquals(Integer.valueOf(2), c.confirmedTotal);
	}
	@Test
	public void noFabricatedSecondCopyFromStaleSnapshot()
	{
		sync(1);
		loot();
		Celebration c = release();
		assertNull(c.confirmedTotal);
		assertEquals(Integer.valueOf(1), c.lastSyncedTotal);
	}
	@Test
	public void newUnlockAndLootProduceOnePopupInEitherOrder()
	{
		chat("New item added to your collection log: " + NAME);
		loot();
		Celebration c = release();
		assertTrue(c.newSlot);
		assertEquals("General Graardor", c.source);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, times(1)).show(any(), anyLong());
	}
	@Test
	public void lootBeforeFirstUnlockPreservesSourceAndIcon()
	{
		loot();
		chat("New item added to your collection log: " + NAME);
		Celebration c = release();
		assertTrue(c.newSlot);
		assertEquals(ITEM, c.itemId);
		assertEquals("General Graardor", c.source);
	}
	@Test
	public void viewingAnotherPlayersLogDoesNotSeedOurRepeats()
	{
		when(p.client.getVarbitValue(VarbitID.COLLECTION_POH_HOST_BOOK_OPEN)).thenReturn(1);
		sync(20);
		loot();
		p.time += 2000;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}
	@Test
	public void logoutClearsPendingAndOfficialBaseline()
	{
		sync(2);
		loot();
		GameStateChanged e = new GameStateChanged();
		e.setGameState(GameState.LOGIN_SCREEN);
		p.onGameStateChanged(e);
		loot();
		p.time += 2000;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		verify(p.sounds).reset();
	}
	@Test
	public void unsyncedUnlockUsesBinaryOneEvenWithoutItemId()
	{
		chat("New item added to your collection log: " + NAME);
		Celebration c = release();
		assertEquals(1, c.provisionalTotal);
		assertNull(c.confirmedTotal);
		assertNull(c.lastSyncedTotal);
	}
	@Test
	public void observedUnlockEnablesRepeatsWithoutInventingSecondCopy()
	{
		chat("New item added to your collection log: " + NAME);
		loot();
		release();
		clearInvocations(p.overlay);
		p.time += 3000;
		loot();
		Celebration c = release();
		assertFalse(c.newSlot);
		assertEquals(1, c.provisionalTotal);
		assertNull(c.confirmedTotal);
		assertNull(c.lastSyncedTotal);
	}
	@Test
	public void firstOfficialSyncReplacesBinaryPlaceholder()
	{
		chat("New item added to your collection log: " + NAME);
		sync(7);
		assertEquals(Integer.valueOf(7), release().confirmedTotal);
	}
	@Test
	public void resetClearsUnsyncedMembership()
	{
		loot();
		chat("New item added to your collection log: " + NAME);
		release();
		GameStateChanged e = new GameStateChanged();
		e.setGameState(GameState.HOPPING);
		p.onGameStateChanged(e);
		clearInvocations(p.overlay);
		loot();
		p.time += 2000;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}
	@Test
	public void officialZeroClearsTemporaryMembership()
	{
		loot();
		chat("New item added to your collection log: " + NAME);
		release();
		sync(0);
		clearInvocations(p.overlay);
		loot();
		p.time += 2000;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}

	@Test
	public void eachPreviewTierReleasesOnceWithSelectedSound()
	{
		for (PreviewTier tier : PreviewTier.values())
		{
			clearInvocations(p.overlay, p.sounds);
			p.queuePreview(tier);
			Celebration c = release();
			assertSame(tier, c.previewTier);
			verify(p.sounds).playPreview(tier == PreviewTier.COMMON ? "CollectionLog.wav" : tier.sound, 70, "custom-sounds-unlock.wav", 24);
			assertEquals(tier.itemId, c.itemId);
			p.onBeforeRender(new BeforeRender());
			verify(p.overlay, times(1)).show(any(), anyLong());
		}
	}
	@Test
	public void repeatPreviewHasNoGoldUnlockBorderAndDoesNotSeedMembership()
	{
		when(p.config.previewKind()).thenReturn(PreviewKind.REPEAT_DROP);
		p.queuePreview(PreviewTier.RARE);
		Celebration c = release();
		assertFalse(c.newSlot);
		assertEquals(Integer.valueOf(2), c.confirmedTotal);
		clearInvocations(p.overlay);
		loot();
		p.time += 2000;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}
	@Test
	public void previewWaitsForCaseAndDoesNotSeedCollectionMembership()
	{
		p.queuePreview(PreviewTier.RARE);
		when(p.gate.blocked()).thenReturn(true);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		verify(p.sounds, never()).playNow(anyString(), anyInt());
		when(p.gate.blocked()).thenReturn(false);
		release();
		verify(p.sounds).playPreview("rare.wav", 70, "custom-sounds-unlock.wav", 24);
		clearInvocations(p.overlay);
		loot();
		p.time += 2000;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}
	@Test
	public void previewClearsOnLogoutAndCannotQueueWhileLoggedOut()
	{
		p.queuePreview(PreviewTier.PET);
		GameStateChanged e = new GameStateChanged();
		e.setGameState(GameState.LOGIN_SCREEN);
		p.onGameStateChanged(e);
		when(p.client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		p.queuePreview(PreviewTier.COMMON);
		when(p.client.getGameState()).thenReturn(GameState.LOGGED_IN);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}

	@Test
	public void realRewardKeepsPriorityOverPreview()
	{
		sync(1);
		loot();
		p.queuePreview(PreviewTier.COMMON);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		assertNull(release().previewTier);
		clearInvocations(p.overlay);
		assertSame(PreviewTier.COMMON, release().previewTier);
	}

	@Test
	public void rapidTestsQueueWithoutCancellingCurrentPresentation()
	{
		p.queuePreview(PreviewTier.COMMON);
		assertSame(PreviewTier.COMMON, release().previewTier);
		when(p.overlay.idle()).thenReturn(false);
		p.queuePreview(PreviewTier.RARE);
		p.queuePreview(PreviewTier.PET);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, times(1)).show(any(), anyLong());
		verify(p.overlay, never()).clearPreview();
		verify(p.sounds, never()).cancelPreview();
		when(p.overlay.idle()).thenReturn(true);
		clearInvocations(p.overlay);
		assertSame(PreviewTier.PET, release().previewTier);
		clearInvocations(p.overlay);
		assertSame(PreviewTier.RARE, release().previewTier);
	}

	@Test
	public void nativePreviewOffCancelsTestsButKeepsRealReward()
	{
		sync(1);
		loot();
		when(p.config.previewSelection()).thenReturn(PreviewSelection.RARE);
		p.updatePreview();
		when(p.config.previewSelection()).thenReturn(PreviewSelection.OFF);
		p.updatePreview();
		assertNull(release().previewTier);
		clearInvocations(p.overlay);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}
	@Test
	public void nativePreviewChangeUsesNewestTierAndUnlockKind()
	{
		when(p.config.previewSelection()).thenReturn(PreviewSelection.COMMON);
		p.updatePreview();
		when(p.config.previewSelection()).thenReturn(PreviewSelection.PET);
		when(p.config.previewKind()).thenReturn(PreviewKind.REPEAT_DROP);
		p.updatePreview();
		Celebration c = release();
		assertSame(PreviewTier.PET, c.previewTier);
		assertFalse(c.newSlot);
	}

	@Test
	public void lateUnlockDoesNotReplayAnAlreadyPresentedReward()
	{
		sync(1);
		loot();
		Celebration shown = release();
		when(p.overlay.showing(shown)).thenReturn(true);
		chat("New item added to your collection log: " + NAME);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, times(1)).show(any(), anyLong());
		assertTrue("Upgrade visible notification without replaying tier audio", shown.newSlot);
		verify(p.sounds, times(1)).playNow("CollectionLog.wav", 70);
		verify(p.sounds).offer("custom-sounds-unlock.wav", 24, 0, true);
	}

	@Test
	public void sourceMatchedKcIsAttached()
	{
		sync(1);
		chat("Your General Graardor kill count is: 347.");
		loot();
		assertEquals("Kills: 347", release().kc);
	}

	@Test
	public void laterKillCannotOverwriteKcOfQueuedReward()
	{
		sync(1);
		chat("Your General Graardor kill count is: 347.");
		loot();
		p.time += 1000;
		chat("Your General Graardor kill count is: 348.");
		loot();
		assertEquals("Kills: 347", release().kc);
		clearInvocations(p.overlay);
		assertEquals("Kills: 348", release().kc);
	}
	@Test
	public void repeatRaritySwitchSuppressesOnlyRepeats()
	{
		when(p.config.repeatCommon()).thenReturn(false);
		sync(1);
		loot();
		p.time += 1500;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		verify(p.sounds, never()).playNow(anyString(), anyInt());
		// A late genuine unlock must still be able to promote the suppressed reward.
		chat("New item added to your collection log: " + NAME);
		assertTrue(release().newSlot);
	}
	@Test
	public void testsIgnoreRepeatRaritySwitches()
	{
		when(p.config.repeatCommon()).thenReturn(false);
		when(p.config.previewKind()).thenReturn(PreviewKind.REPEAT_DROP);
		p.queuePreview(PreviewTier.COMMON);
		assertFalse(release().newSlot);
	}
	@Test
	public void genuineUnlockCancelsValueAudioAndAddsJingle()
	{
		chat("New item added to your collection log: " + NAME);
		release();
		verify(p.sounds).cancelValue(NAME);
		verify(p.sounds).playNow("custom-sounds-unlock.wav", 24);
	}
	@Test
	public void repeatDoesNotAddUnlockJingle()
	{
		sync(1);
		loot();
		release();
		verify(p.sounds, never()).playNow(eq("custom-sounds-unlock.wav"), anyInt());
	}
	@Test
	public void knownCollectionDropShowsBeforeLogSyncWithoutClaimingOwnership()
	{
		when(p.wiki.entry(ITEM, NAME)).thenReturn(new WikiRarity.Entry(ITEM, NAME, 10.0, false, List.of()));
		loot();
		ArgumentCaptor<java.util.function.IntPredicate> predicate = ArgumentCaptor.forClass(java.util.function.IntPredicate.class);
		verify(p.custom).onLootReceived(any(LootReceived.class), predicate.capture());
		assertTrue(predicate.getValue().test(ITEM));
		p.time += 2500;
		p.onBeforeRender(new BeforeRender());
		ArgumentCaptor<Celebration> shown = ArgumentCaptor.forClass(Celebration.class);
		verify(p.overlay).show(shown.capture(), anyLong());
		assertFalse(shown.getValue().newSlot);
		assertNull(shown.getValue().confirmedTotal);
		assertNull(shown.getValue().lastSyncedTotal);
		assertEquals(0, shown.getValue().provisionalTotal);
	}
	@Test
	public void immediateKnownSlotSoundCanBecomeNewUnlockWithoutReplay()
	{
		when(p.gate.delayMillis()).thenReturn(0L);
		when(p.wiki.entry(ITEM, NAME)).thenReturn(new WikiRarity.Entry(ITEM, NAME, 10.0, false, List.of()));
		loot();
		p.onBeforeRender(new BeforeRender());
		verify(p.sounds).playNow("CollectionLog.wav", 70);
		ArgumentCaptor<Celebration> shown = ArgumentCaptor.forClass(Celebration.class);
		verify(p.overlay).show(shown.capture(), anyLong());
		when(p.overlay.showing(shown.getValue())).thenReturn(true);
		when(p.overlay.idle()).thenReturn(false);
		chat("New item added to your collection log: " + NAME);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay).show(any(), anyLong());
		verify(p.sounds, times(1)).playNow("CollectionLog.wav", 70);
		verify(p.sounds).offer("custom-sounds-unlock.wav", 24, 0, true);
		assertTrue(shown.getValue().newSlot);
	}
	@Test
	public void allTierTestQueuesHighestFirstAndStopClearsRemainder()
	{
		p.testAction("Test all tiers", PreviewTier.COMMON);
		assertSame(PreviewTier.PET, release().previewTier);
		clearInvocations(p.overlay);
		assertSame(PreviewTier.VERY_RARE, release().previewTier);
		p.testAction("Stop", PreviewTier.COMMON);
		clearInvocations(p.overlay);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}
	@Test
	public void addToQueueDoesNotReplaceCurrentPreview()
	{
		p.testAction("Add to queue", PreviewTier.RARE);
		verify(p.overlay, never()).clearPreview();
		verify(p.sounds, never()).cancelPreview();
		assertSame(PreviewTier.RARE, release().previewTier);
	}

	@Test
	public void nextCollectionWaitsForAudioEvenAfterPopupFinishes()
	{
		when(p.wiki.entry(ITEM, NAME)).thenReturn(new WikiRarity.Entry(ITEM, NAME, 10.0, false, List.of()));
		loot();
		p.time += 2500;
		when(p.sounds.busy()).thenReturn(true);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		when(p.sounds.busy()).thenReturn(false);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay).show(any(), anyLong());
	}
	@Test
	public void collectionTransmissionWithoutItemDefinitionPreservesQuantity()
	{
		when(p.items.getItemComposition(ITEM)).thenReturn(null);
		sync(7);
		ItemComposition restored = mock(ItemComposition.class);
		when(restored.getName()).thenReturn(NAME);
		when(p.items.getItemComposition(ITEM)).thenReturn(restored);
		loot();
		assertEquals(Integer.valueOf(7), release().lastSyncedTotal);
	}

	@Test
	public void identicalDropsDuringCaseHoldRemainTwoNotifications()
	{
		sync(1);
		when(p.gate.blocked()).thenReturn(true);
		loot();
		loot();
		chat("New item added to your collection log: " + NAME);
		p.time += 1500;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		when(p.gate.blocked()).thenReturn(false);
		p.onBeforeRender(new BeforeRender());
		p.onBeforeRender(new BeforeRender());
		p.onBeforeRender(new BeforeRender());
		ArgumentCaptor<Celebration> captures = ArgumentCaptor.forClass(Celebration.class);
		verify(p.overlay, times(2)).show(captures.capture(), anyLong());
		assertTrue(captures.getAllValues().get(0).newSlot);
		assertFalse(captures.getAllValues().get(1).newSlot);
	}

	@Test
	public void duplicateStacksCannotOverflowIntoNegativeQuantity()
	{
		sync(1);
		p.onLootReceived(new LootReceived("General Graardor", 624, LootRecordType.NPC,
			List.of(new ItemStack(ITEM, Integer.MAX_VALUE), new ItemStack(ITEM, 10)), 1, null));
		assertEquals(Integer.MAX_VALUE, release().dropQuantity);
	}

	@Test
	public void hoppingBetweenLootAndUnlockCannotReusePreviousWorldSource()
	{
		sync(3);
		loot();
		GameStateChanged hop = new GameStateChanged();
		hop.setGameState(GameState.HOPPING);
		p.onGameStateChanged(hop);
		chat("New item added to your collection log: " + NAME);
		Celebration notification = release();
		assertTrue(notification.newSlot);
		assertNull(notification.source);
		assertNull(notification.lastSyncedTotal);
		assertNull(notification.confirmedTotal);
		assertEquals(1, notification.provisionalTotal);
	}

	@Test
	public void excludedQueuedUnlockSuppressesPopupAndCollectionAudioButKeepsOwnership()
	{
		loot();
		chat("New item added to your collection log: " + NAME);
		when(p.config.excludedPopupItems()).thenReturn("bandos *");
		p.time += 1500;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		verify(p.sounds, never()).playNow(anyString(), anyInt());
		when(p.config.excludedPopupItems()).thenReturn("");
		p.time += 3000;
		loot();
		Celebration c = release();
		assertFalse(c.newSlot);
		assertEquals(1, c.provisionalTotal);
	}

	@Test
	public void quantityExclusionUsesReceivedStackAndNotOfficialTotal()
	{
		sync(50);
		when(p.config.excludedPopupItems()).thenReturn(NAME + " < 2");
		loot();
		p.time += 1500;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		p.onLootReceived(new LootReceived("General Graardor", 624, LootRecordType.NPC,
			List.of(new ItemStack(ITEM, 2)), 1, null));
		assertEquals(2, release().dropQuantity);
	}

	@Test
	public void explicitPreviewBypassesItemExclusion()
	{
		when(p.config.excludedPopupItems()).thenReturn("*");
		p.queuePreview(PreviewTier.COMMON);
		assertEquals(PreviewTier.COMMON, release().previewTier);
	}

	@Test
	public void lateUnlockJingleRespectsNewlyAddedExclusion()
	{
		sync(1);
		loot();
		Celebration shown = release();
		when(p.overlay.showing(shown)).thenReturn(true);
		when(p.config.excludedPopupItems()).thenReturn(NAME);
		clearInvocations(p.sounds);
		chat("New item added to your collection log: " + NAME);
		verify(p.sounds, never()).offer(anyString(), anyInt(), anyLong(), anyBoolean());
	}

	@Test
	public void unlockAccentFollowsQuietTierAndRespectsMute()
	{
		when(p.config.volumeUncommon()).thenReturn(20);
		when(p.config.unlockVolume()).thenReturn(100);
		assertEquals(4, p.unlockVolume(PreviewTier.UNCOMMON));
		when(p.config.volumeUncommon()).thenReturn(0);
		assertEquals(0, p.unlockVolume(PreviewTier.UNCOMMON));
		when(p.config.volumeUncommon()).thenReturn(100);
		when(p.config.unlockVolume()).thenReturn(0);
		assertEquals(0, p.unlockVolume(PreviewTier.UNCOMMON));
	}

	@Test
	public void readyCollectionPopupCannotBeStarvedByQueuedValueAudio()
	{
		sync(1);
		loot();
		doAnswer(call -> {
			when(p.sounds.busy()).thenReturn(true);
			return null;
		}).when(p.sounds).tick(anyLong());
		assertEquals(NAME, release().name);
		verify(p.sounds, never()).tick(anyLong());
	}

	@Test
	public void standaloneSoundsStillReleaseWhenNoPopupIsWaiting()
	{
		p.onBeforeRender(new BeforeRender());
		verify(p.sounds).tick(p.time);
	}

	@Test
	public void includedCommonRepeatOverridesDisabledTier()
	{
		RarityResult common = new RarityResult(RarityTier.COMMON, ITEM, 0, false, null, null, 0, 0, 0, 0, 0, 0);
		when(p.wiki.resolve(ITEM, NAME)).thenReturn(common);
		sync(2);
		when(p.config.repeatCommon()).thenReturn(false);
		when(p.config.includedPopupItems()).thenReturn(NAME);
		loot();
		Celebration c = release();
		assertFalse(c.newSlot);
		assertFalse(c.extraItem);
		assertEquals(PreviewTier.COMMON, c.tier);
		verify(p.sounds).playNow("CollectionLog.wav", 70);
	}

	@Test
	public void includedExtraItemOverridesGlobalRepeatWithoutClaimingOwnership()
	{
		when(p.config.repeatDrops()).thenReturn(false);
		when(p.config.includedPopupItems()).thenReturn("Bandos *");
		loot();
		Celebration c = release();
		assertTrue(c.extraItem);
		assertFalse(c.newSlot);
		assertNull(c.confirmedTotal);
		assertNull(c.lastSyncedTotal);
		assertEquals(0, c.provisionalTotal);
		ArgumentCaptor<java.util.function.IntPredicate> classifier = ArgumentCaptor.forClass(java.util.function.IntPredicate.class);
		verify(p.custom).onLootReceived(any(), classifier.capture());
		assertTrue(classifier.getValue().test(ITEM));
	}

	@Test
	public void exclusionsWinOverInclusions()
	{
		when(p.config.includedPopupItems()).thenReturn(NAME);
		when(p.config.excludedPopupItems()).thenReturn("Bandos *");
		loot();
		p.time += 1500;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
		verify(p.sounds, never()).playNow(anyString(), anyInt());
	}

	@Test
	public void removingInclusionWhileHeldSuppressesExtraItem()
	{
		when(p.config.includedPopupItems()).thenReturn(NAME);
		loot();
		when(p.config.includedPopupItems()).thenReturn("");
		p.time += 1500;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, never()).show(any(), anyLong());
	}

	@Test
	public void includedExtraItemCanBecomeGenuineUnlock()
	{
		when(p.config.includedPopupItems()).thenReturn(NAME);
		loot();
		chat("New item added to your collection log: " + NAME);
		Celebration c = release();
		assertTrue(c.newSlot);
		assertFalse(c.extraItem);
	}

	@Test
	public void inclusionQuantityUsesMergedReceivedStacks()
	{
		when(p.config.includedPopupItems()).thenReturn("Bandos * > 1");
		p.onLootReceived(new LootReceived("General Graardor", 624, LootRecordType.NPC,
			List.of(new ItemStack(ITEM, 1), new ItemStack(ITEM, 1)), 1, null));
		Celebration c = release();
		assertTrue(c.extraItem);
		assertEquals(2, c.dropQuantity);
		ArgumentCaptor<java.util.function.IntPredicate> classifier = ArgumentCaptor.forClass(java.util.function.IntPredicate.class);
		verify(p.custom).onLootReceived(any(), classifier.capture());
		assertTrue(classifier.getValue().test(ITEM));
	}

	private void queuedItem(int id, String name, RarityTier tier, int price, int quantity, boolean unlock)
	{
		ItemComposition item = mock(ItemComposition.class);
		when(item.getName()).thenReturn(name);
		when(item.isTradeable()).thenReturn(true);
		when(item.getHaPrice()).thenReturn(price / 2);
		when(p.items.canonicalize(id)).thenReturn(id);
		when(p.items.getItemComposition(id)).thenReturn(item);
		when(p.items.getItemPrice(id)).thenReturn(price);
		when(p.wiki.entry(id, name)).thenReturn(new WikiRarity.Entry(id, name, null, false, List.of()));
		when(p.wiki.resolve(id, name)).thenReturn(new RarityResult(tier, id, price, false, null, null, 0, 0, 0, 0, 0, price / 2));
		p.onLootReceived(new LootReceived("Chest", 1, LootRecordType.NPC, List.of(new ItemStack(id, quantity)), 1, null));
		if (unlock)
		{
			chat("New item added to your collection log: " + name);
		}
	}

	private void assertReleaseOrder(String... names)
	{
		for (String name : names)
		{
			clearInvocations(p.overlay);
			assertEquals(name, release().name);
		}
	}

	@Test
	public void newLogsAlwaysPrecedeRepeatsThenRarityThenValue()
	{
		queuedItem(101, "Expensive repeat", RarityTier.VERY_RARE, 1000000, 1, false);
		queuedItem(102, "Cheap new common", RarityTier.COMMON, 1, 1, true);
		queuedItem(103, "New rare", RarityTier.RARE, 10, 1, true);
		queuedItem(104, "Valuable new common", RarityTier.COMMON, 1000, 1, true);
		queuedItem(105, "Cheap repeat", RarityTier.COMMON, 1, 1, false);
		queuedItem(106, "Valuable repeat", RarityTier.COMMON, 1000, 1, false);
		assertReleaseOrder("New rare", "Valuable new common", "Cheap new common", "Expensive repeat", "Valuable repeat", "Cheap repeat");
	}

	@Test
	public void valueTieBreakerUsesReceivedStackAndKeepsEqualValuesInOrder()
	{
		queuedItem(101, "Single expensive", RarityTier.COMMON, 100, 1, true);
		queuedItem(102, "Large stack", RarityTier.COMMON, 60, 2, true);
		queuedItem(103, "Equal later stack", RarityTier.COMMON, 40, 3, true);
		assertReleaseOrder("Large stack", "Equal later stack", "Single expensive");
	}

	@Test
	public void valueTieBreakerFollowsConfiguredAlchMode()
	{
		when(p.config.valueMode()).thenReturn(ValueMode.HIGH_ALCH);
		queuedItem(101, "Higher GE", RarityTier.COMMON, 1000, 1, false);
		queuedItem(102, "Higher alch", RarityTier.COMMON, 10, 1, false);
		when(p.items.getItemComposition(102).getHaPrice()).thenReturn(2000);
		assertReleaseOrder("Higher alch", "Higher GE");
	}

	@Test
	public void newLogDoesNotInterruptVisibleRepeatAndWaitsForCaseHold()
	{
		queuedItem(101, "Visible repeat", RarityTier.COMMON, 1, 1, false);
		assertEquals("Visible repeat", release().name);
		when(p.overlay.idle()).thenReturn(false);
		queuedItem(102, "New rare", RarityTier.RARE, 100, 1, true);
		p.time += 1500;
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, times(1)).show(any(), anyLong());
		when(p.overlay.idle()).thenReturn(true);
		when(p.gate.blocked()).thenReturn(true);
		p.onBeforeRender(new BeforeRender());
		verify(p.overlay, times(1)).show(any(), anyLong());
		when(p.gate.blocked()).thenReturn(false);
		assertReleaseOrder("New rare");
	}

	@Test
	public void untradeableQueueTiesIgnoreAlchAndMappedPrices()
	{
		queuedItem(101, "First untradeable", RarityTier.RARE, 1, 1, false);
		queuedItem(102, "Later untradeable", RarityTier.RARE, 1000000, 1, false);
		when(p.items.getItemComposition(101).isTradeable()).thenReturn(false);
		when(p.items.getItemComposition(102).isTradeable()).thenReturn(false);
		assertReleaseOrder("First untradeable", "Later untradeable");
	}

}
