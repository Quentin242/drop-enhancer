/* Copyright (c) 2026 maiz. Ranking formula adapted from Collection Log Popup
 * Enhanced, copyright (c) 2026 SnakeSteak. BSD-2-Clause; see licenses/enhanced.txt. */
package com.collectioncelebrations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

/** Wiki population statistics never establish the player's own membership. */
@Singleton
class WikiRarity
{
	@Inject
	Gson gson;
	@Inject
	WikiCompletionClient remote;
	@Inject
	CelebrationConfig config;
	@Inject
	RarityResolver resolver;
	@Inject
	DropRateResolver dropRates;
	private volatile Map<Integer, Entry> entries = Map.of();
	private ExecutorService loader;
	private volatile int generation;
	Path dataDirectory = RuneLite.RUNELITE_DIR.toPath().resolve("collection-celebrations/data");
	static final class Entry
	{
		final int id;
		final String name;
		final Double completion;
		final boolean pet;
		final List<String> tabs;
		Entry(int id, String name, Double completion, boolean pet, List<String> tabs)
		{
			this.id = id;
			this.name = name;
			this.completion = completion;
			this.pet = pet;
			this.tabs = tabs;
		}
	}
	synchronized void start()
	{
		int session = ++generation;
		loader = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "collection-wiki-data");
			t.setDaemon(true);
			return t;
		});
		loader.execute(() -> {
			Path folder = dataDirectory;
			try (Reader reader = Files.newBufferedReader(folder.resolve("collection-log.json"), StandardCharsets.UTF_8))
			{
				Map<Integer, Entry> loaded = parse(gson.fromJson(reader, JsonObject.class));
				installCurrent(loaded, session);
			}
			catch (Exception ignored)
			{ /* Missing/unreadable data is displayed as unavailable. */
			}
			try (Reader reader = Files.newBufferedReader(folder.resolve("drop-rates.json"), StandardCharsets.UTF_8))
			{
				Map<String, Map<String, Double>> loaded = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				gson.fromJson(reader, JsonObject.class).entrySet().forEach(source -> {
					Map<String, Double> values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
					source.getValue().getAsJsonObject().entrySet().forEach(item -> {
						double p = item.getValue().getAsDouble();
						if (p > 0 && p <= 1)
						{
							values.put(item.getKey(), p);
						}
					});
					loaded.put(source.getKey(), values);
				});
				if (generation == session)
				{
					dropRates.reload(loaded);
				}
			}
			catch (Exception ignored)
			{
			}
			if (generation == session && config.refreshWikiData())
			{
				refreshCompletion(folder, session);
			}
		});
	}
	synchronized void stop()
	{
		generation++;
		if (loader != null)
		{
			loader.shutdownNow();
			loader = null;
		}
		remote.cancel();
	}
	synchronized void refreshSetting()
	{
		if (loader != null)
		{
			stop();
			start();
		}
	}

	private void refreshCompletion(Path folder, int session)
	{
		Path cache = folder.resolve("wiki-completion-cache.json");
		long now = System.currentTimeMillis();
		try (Reader reader = Files.newBufferedReader(cache, StandardCharsets.UTF_8))
		{
			JsonObject saved = gson.fromJson(reader, JsonObject.class);
			Map<Integer, Entry> loaded = parse(saved.getAsJsonObject("items"));
			if (loaded.isEmpty())
			{
				throw new IllegalArgumentException("Empty cache");
			}
			if (!installCurrent(loaded, session))
			{
				return;
			}
			long age = now - saved.get("fetchedAt").getAsLong();
			if (age >= 0 && age < 86400000L)
			{
				return;
			}
		}
		catch (Exception ignored)
		{ /* A stale/local dataset stays usable while refreshing. */
		}
		if (generation != session)
		{
			return;
		}
		try
		{
			JsonObject data = remote.fetch();
			Map<Integer, Entry> loaded = parse(data);
			if (!installCurrent(loaded, session))
			{
				return;
			}
			JsonObject saved = new JsonObject();
			saved.addProperty("fetchedAt", now);
			saved.addProperty("attribution", "OSRS Wiki contributors: Module:Collection_log/data.json and completion.json");
			saved.addProperty("source", "https://oldschool.runescape.wiki/w/Module:Collection_log");
			saved.addProperty("license", "https://creativecommons.org/licenses/by-nc-sa/3.0/");
			saved.addProperty("changes", "Joined item list and completion percentages by item ID");
			saved.add("items", data);
			Files.createDirectories(folder);
			Path temporary = Files.createTempFile(folder, "wiki-completion-", ".tmp");
			try
			{
				Files.writeString(temporary, gson.toJson(saved), StandardCharsets.UTF_8);
				synchronized (this)
				{
					if (generation == session)
					{
						Files.move(temporary, cache, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
			finally
			{
				Files.deleteIfExists(temporary);
			}
		}
		catch (Exception failure)
		{
			org.slf4j.LoggerFactory.getLogger(WikiRarity.class)
				.debug("Wiki completion refresh unavailable; keeping existing data", failure);
		}
	}

	static Map<Integer, Entry> parse(JsonObject root)
	{
		Map<Integer, Entry> result = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> row : root.entrySet())
		{
			JsonObject obj = row.getValue().getAsJsonObject();
			JsonElement itemName = obj.get("name");
			// Wiki snapshots can contain unnamed placeholders. One placeholder must
			// not prevent every valid collection item from being recognized.
			if (itemName == null || itemName.isJsonNull() || !itemName.isJsonPrimitive() ||
				!itemName.getAsJsonPrimitive().isString() || itemName.getAsString().isBlank())
			{
				continue;
			}
			int id = Integer.parseInt(row.getKey());
			Double comp = obj.has("comp") && !obj.get("comp").isJsonNull() ? obj.get("comp").getAsDouble() : null;
			if (id < 0 || (comp != null && (!Double.isFinite(comp) || comp < 0 || comp > 100)))
			{
				continue;
			}
			boolean pet = false;
			List<String> tabs = new ArrayList<>();
			if (obj.has("tabs"))
			{
				for (JsonElement tab : obj.getAsJsonArray("tabs"))
				{
					tabs.add(tab.getAsString());
					pet |= "All Pets".equalsIgnoreCase(tab.getAsString());
				}
			}
			result.put(id, new Entry(id, itemName.getAsString(), comp, pet, List.copyOf(tabs)));
		}
		return Collections.unmodifiableMap(result);
	}
	private synchronized boolean installCurrent(Map<Integer, Entry> loaded, int session)
	{
		if (generation != session)
		{
			return false;
		}
		install(loaded);
		return true;
	}

	private void install(Map<Integer, Entry> loaded)
	{
		Map<String, RarityResolver.CompletionEntry> raw = new LinkedHashMap<>();
		loaded.forEach((id, e) -> {
			RarityResolver.CompletionEntry value = new RarityResolver.CompletionEntry();
			value.name = e.name;
			value.comp = e.completion;
			value.tabs = e.tabs;
			raw.put(String.valueOf(id), value);
		});
		resolver.reload(raw);
		entries = loaded;
	}
	Entry entry(int id, String name)
	{
		Entry e = entries.get(id);
		if (e != null)
		{
			return e;
		}
		Integer mapped = resolver.datasetIdForName(name);
		return mapped == null ? null : entries.get(mapped);
	}
	RarityResult resolve(int id, String name)
	{
		return resolver.resolve(id, name);
	}

	String dropRateText(String source, String name)
	{
		Double exact = source == null ? null : dropRates.dropProbability(source, name);
		if (exact != null)
		{
			return probabilityText(exact);
		}
		List<DropRateResolver.SourceRate> matches = dropRates.dropRatesByItemName(name);
		if (matches.size() == 1)
		{
			return probabilityText(matches.get(0).getProbability());
		}
		if (matches.size() == 2)
		{
			return probabilityText(matches.get(0).getProbability()) + " / " + probabilityText(matches.get(1).getProbability());
		}
		return null;
	}
	private static String probabilityText(double value)
	{
		return String.format(Locale.ROOT, "1/%,.0f", 1 / value);
	}

	Entry example(PreviewTier tier)
	{
		Integer id = resolver.randomItemIdForTier(tier);
		return id == null ? null : entries.get(id);
	}
}
