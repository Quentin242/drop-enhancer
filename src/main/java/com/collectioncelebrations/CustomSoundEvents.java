/* Derived from Custom Sounds, copyright (c) 2023 daanbom.
 * BSD-2-Clause; full terms are in licenses/custom-sounds.txt. */
package com.collectioncelebrations;

import net.runelite.api.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.grounditems.GroundItemsConfig;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

@javax.inject.Singleton
public class CustomSoundEvents
{
	@Inject
	CelebrationConfig config;

	@Inject
	SoundQueue soundQueue;

	@Inject
	GroundItemsConfig groundItemsConfig;

	@Inject
	ItemManager itemManager;

	private static final String HIGHLIGHTED_SOUND_FILE = "highlighted_sound.wav";
	private static final String BEGINNER_CLUE_SOUND_FILE = "beginner_clue_sound.wav";
	private static final String EASY_CLUE_SOUND_FILE = "easy_clue_sound.wav";
	private static final String MEDIUM_CLUE_SOUND_FILE = "medium_clue_sound.wav";
	private static final String HARD_CLUE_SOUND_FILE = "hard_clue_sound.wav";
	private static final String ELITE_CLUE_SOUND_FILE = "elite_clue_sound.wav";
	private static final String MASTER_CLUE_SOUND_FILE = "master_clue_sound.wav";
	private static final String LOW_SOUND_FILE = "low_sound.wav";
	private static final String MEDIUM_SOUND_FILE = "medium_sound.wav";

	private static final String HIGH_SOUND_FILE = "high_sound.wav";
	private static final String HIGHEST_SOUND_FILE = "highest_sound.wav";
	private static final String DEFAULT_SOUND_FILE = "default_sound.wav";

	private List<String> highlightedItemsList = List.of();

	public void startUp()
	{
		updateHighlightedItemsList();
	}

	public void shutDown()
	{
		highlightedItemsList = List.of();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{

		if (configChanged.getGroup().equals("grounditems") && configChanged.getKey().equals("highlightedItems"))
		{
			updateHighlightedItemsList();
		}
	}

	public void onLootReceived(LootReceived lootReceived, java.util.function.IntPredicate collectionItem)
	{
		for (ItemStack stack : lootReceived.getItems())
		{
			handleItem(stack.getId(), stack.getQuantity(), collectionItem.test(stack.getId()));
		}
	}

	private void handleItem(int id, int quantity, boolean collectionItem)
	{
		final ItemComposition itemComposition = itemManager.getItemComposition(id);
		if (itemComposition == null || itemComposition.getName() == null || quantity <= 0)
		{
			return;
		}
		final String name = itemComposition.getName().toLowerCase(Locale.ROOT);

		if (config.highlightSound() && highlightedItemsList.contains(name))
		{
			playSound(HIGHLIGHTED_SOUND_FILE);
			return;
		}

		final int gePrice = (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, itemManager.getItemPrice(id)) * quantity);
		final int haPrice = (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, itemComposition.getHaPrice()) * quantity);
		final int value = getValueByMode(gePrice, haPrice);

		if (config.beginnerClueSound() && name.contains("clue scroll (beginner)"))
		{
			playSound(BEGINNER_CLUE_SOUND_FILE);
		}
		if (config.easyClueSound() && name.contains("clue scroll (easy)"))
		{
			playSound(EASY_CLUE_SOUND_FILE);
		}
		if (config.mediumClueSound() && name.contains("clue scroll (medium)"))
		{
			playSound(MEDIUM_CLUE_SOUND_FILE);
		}
		if (config.hardClueSound() && name.contains("clue scroll (hard)"))
		{
			playSound(HARD_CLUE_SOUND_FILE);
		}

		if (config.eliteClueSound() && name.contains("clue scroll (elite)"))
		{
			playSound(ELITE_CLUE_SOUND_FILE);
		}
		if (config.masterClueSound() && name.contains("clue scroll (master)"))
		{
			playSound(MASTER_CLUE_SOUND_FILE);
		}

		if (collectionItem)
		{
			return;
		}
		if (config.dropValueMode() == DropValueMode.GROUND_ITEMS &&
			GroundItemSoundFilter.hidden(groundItemsConfig, name, quantity, gePrice, haPrice, itemComposition.isTradeable()))
		{
			return;
		}

		if (config.dropValueMode() == DropValueMode.GROUND_ITEMS)
		{
			// Ground Items colours use strict > comparisons at each configured price boundary.
			if (value > groundItemsConfig.insaneValuePrice())
			{
				if (config.highestValueSound())
				{
					playValueSound(HIGHEST_SOUND_FILE, name);
				}
			}
			else if (value > groundItemsConfig.highValuePrice())
			{
				if (config.highValueSound())
				{
					playValueSound(HIGH_SOUND_FILE, name);
				}
			}
			else if (value > groundItemsConfig.mediumValuePrice())
			{
				if (config.mediumValueSound())
				{
					playValueSound(MEDIUM_SOUND_FILE, name);
				}
			}
			else if (value > groundItemsConfig.lowValuePrice())
			{
				if (config.lowValueSound())
				{
					playValueSound(LOW_SOUND_FILE, name);
				}
			}
			else if (config.defaultValueSound())
			{
				playValueSound(DEFAULT_SOUND_FILE, name);
			}
			return;
		}

		if (config.defaultValueSound() && value >= config.defaultStart() && value < config.defaultEnd())
		{
			playValueSound(DEFAULT_SOUND_FILE, name);
		}
		if (config.lowValueSound() && value >= config.lowStart() && value < config.lowEnd())
		{
			playValueSound(LOW_SOUND_FILE, name);
		}
		if (config.mediumValueSound() && value >= config.mediumStart() && value < config.mediumEnd())
		{
			playValueSound(MEDIUM_SOUND_FILE, name);
		}
		if (config.highValueSound() && value >= config.highStart() && value < config.highEnd())
		{
			playValueSound(HIGH_SOUND_FILE, name);
		}
		if (config.highestValueSound() && value >= config.highestStart() && value < config.highestEnd())
		{
			playValueSound(HIGHEST_SOUND_FILE, name);
		}
	}

	private void playValueSound(String f, String itemName)
	{
		soundQueue.offerValue(eventFile(f), eventVolume(f), itemName);
	}
	private void playSound(String f)
	{
		soundQueue.offer(eventFile(f), eventVolume(f), 0, true);
	}

	private PreviewTier eventTier(String file)
	{
		switch (file)
		{
		case "highest_sound.wav":
		case "elite_clue_sound.wav":
		case "master_clue_sound.wav":
			return PreviewTier.VERY_RARE;
		case "high_sound.wav":
		case "highlighted_sound.wav":
		case "hard_clue_sound.wav":
			return PreviewTier.RARE;
		case "medium_sound.wav":
		case "medium_clue_sound.wav":
			return PreviewTier.UNCOMMON;
		default:
			return PreviewTier.COMMON;
		}
	}

	private String eventFile(String file)
	{
		if (file.equals(HIGHLIGHTED_SOUND_FILE) && config.highlightedFile() != null && !config.highlightedFile().isBlank())
		{
			return config.highlightedFile();
		}
		return TierStyle.file(eventTier(file), config);
	}

	private int eventVolume(String file)
	{
		if (file.equals(HIGHLIGHTED_SOUND_FILE) && config.highlightedFile() != null && !config.highlightedFile().isBlank())
		{
			return Math.max(0, Math.min(100, config.highlightedVolume())) * Math.max(0, Math.min(100, config.masterVolume())) / 100;
		}
		return TierStyle.volume(eventTier(file), config);
	}

	private int getValueByMode(int gePrice, int haPrice)
	{
		DropValueMode mode = config.dropValueMode();
		if (mode == DropValueMode.GE)
		{
			return gePrice;
		}
		if (mode == DropValueMode.HIGH_ALCH)
		{
			return haPrice;
		}
		if (mode == DropValueMode.HIGHEST)
		{
			return Math.max(gePrice, haPrice);
		}
		if (groundItemsConfig.valueCalculationMode() == null)
		{
			return Math.max(gePrice, haPrice);
		}
		switch (groundItemsConfig.valueCalculationMode())
		{
		case GE:
			return gePrice;
		case HA:
			return haPrice;
		default: // Highest
			return Math.max(gePrice, haPrice);
		}
	}

	private void updateHighlightedItemsList()
	{
		highlightedItemsList = Text.fromCSV(groundItemsConfig.getHighlightItems().toLowerCase(Locale.ROOT));
	}
}
