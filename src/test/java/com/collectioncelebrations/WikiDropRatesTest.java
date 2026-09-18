package com.collectioncelebrations;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Test;
import static org.junit.Assert.*;

public class WikiDropRatesTest
{
	static JsonObject row(String source, String item, String rarity)
	{
		JsonObject drop = new JsonObject();
		drop.addProperty("Dropped from", source);
		drop.addProperty("Rarity", rarity);
		drop.addProperty("Rolls", 1);
		JsonObject row = new JsonObject();
		row.addProperty("item_name", item);
		row.addProperty("drop_json", drop.toString());
		return row;
	}

	private WikiDropRates rates(JsonObject... rows) throws Exception
	{
		JsonArray page = new JsonArray();
		for (JsonObject row : rows) { page.add(row); }
		WikiDropRates result = new WikiDropRates();
		result.addPage(page, new Gson());
		return result;
	}

	@Test
	public void rateBelongsToTheActualSourceAndSurvivesCacheRoundTrip() throws Exception
	{
		WikiDropRates data = rates(row("Boss A", "Example", "1/512"), row("Boss B", "Example", "1/1,024"));
		data = WikiDropRates.fromJson(data.toJson(new Gson()));
		assertEquals("1/512", data.find("BOSS A", "example"));
		assertEquals("1/1024", data.find("Boss B", "Example"));
		assertNull(data.find("Other boss", "Example"));
		assertEquals("Varies", data.find(null, "Example"));
		assertEquals("Varies", data.find("Preview only", "Example"));
	}

	@Test
	public void variantsOfOneSourceBecomeARangeButSeparateRowsStayVariable() throws Exception
	{
		WikiDropRates data = rates(row("Boss#Normal", "Example", "1/512"), row("Boss#Hard", "Example", "1/256"),
			row("Boss C", "Example", "1/100"), row("Boss C", "Example", "1/200"));
		// The game reports "Boss" without the variant, so the rate is bounded rather than unknown.
		assertEquals("1/256 – 1/512", data.find("Boss", "Example"));
		assertEquals("1/256", data.find("Boss#Hard", "Example"));
		// Two rows filed under one key are separate drops, not the ends of a range.
		assertEquals("Varies", data.find("Boss C", "Example"));

		assertEquals("1/256 – 1/1024", rates(row("Boss#A", "Example", "1/512"), row("Boss#B", "Example", "1/256"),
			row("Boss#C", "Example", "1/1,024")).find("Boss", "Example"));
		// A range drops fractional denominators, and gives up entirely when it cannot stay short.
		assertEquals("1/10,517 – 1/19,796", rates(row("Boss#A", "Example", "1/19,796.13"),
			row("Boss#B", "Example", "1/10,516.69")).find("Boss", "Example"));
		assertEquals("Varies", rates(row("Boss#A", "Example", "1/123,456,789"),
			row("Boss#B", "Example", "1/987,654,321")).find("Boss", "Example"));
		// A guaranteed variant carries no rate, so it neither widens nor breaks the range.
		assertEquals("1/512", rates(row("Boss#A", "Example", "1/512"), row("Boss#B", "Example", "Always"))
			.find("Boss", "Example"));
		assertEquals("Varies", rates(row("Boss#A", "Example", "1/512"), row("Boss#B", "Example", "varies"))
			.find("Boss", "Example"));
	}

	@Test
	public void anAlternativeRarityKeepsTheOrdinaryRate()
	{
		// The wiki's "Alt Rarity" is the conditional alternative; "Rarity" beside it is the normal rate.
		JsonObject drop = new JsonObject();
		drop.addProperty("Rarity", "1/1,638.4");
		drop.addProperty("Alt Rarity", "1/832");
		assertEquals("1/1638.4", WikiDropRates.rateText(drop));
	}

	@Test
	public void thePreviewAlwaysGetsARealSourceWhenOneExists() throws Exception
	{
		WikiDropRates data = rates(row("Chaos Fanatic", "Pet chaos elemental", "1/1,000"),
			row("Boss#One", "Gull (pet)", "1/1,000"), row("Boss#Two", "Gull (pet)", "1/1,000"));
		assertEquals("Chaos Fanatic", data.anySource("Pet chaos elemental", 0));
		// Variant keys are ignored: the base name is what a loot event reports.
		assertEquals("Boss", data.anySource("Gull", 0));
		assertNull(data.anySource("Absent", 0));

		// Several sources still yield a concrete one, so a test popup never falls back to "Varies".
		WikiDropRates shared = rates(row("Chaos Elemental", "Pet chaos elemental", "1/300"),
			row("Chaos Fanatic", "Pet chaos elemental", "1/1,000"));
		java.util.Set<String> picked = new java.util.HashSet<>();
		for (int pick = -3; pick < 4; pick++)
		{
			String source = shared.anySource("Pet chaos elemental", pick);
			picked.add(source);
			assertNotNull(source);
			assertNotEquals("Varies", shared.find(source, "Pet chaos elemental"));
		}
		assertEquals(java.util.Set.of("Chaos Elemental", "Chaos Fanatic"), picked);
	}

	@Test
	public void matchingVariantRatesAndPetSlotAliasesAreUsable() throws Exception
	{
		WikiDropRates data = rates(row("Boss#One", "Gull (pet)", "1/1,000"), row("Boss#Two", "Gull (pet)", "1/1,000"));
		assertEquals("1/1000", data.find("Boss", "Gull"));
		assertEquals("1/1000", data.find(null, "Gull"));
	}

	@Test
	public void unparsableRatesAndSlotAliasesNeverDisplaceARealRate() throws Exception
	{
		// A dash in the map would be returned as a rate and mask the local dataset behind it.
		WikiDropRates data = rates(row("Boss", "Example", "1/512"), row("Boss", "Example", "Common"));
		assertEquals("1/512", data.find("Boss", "Example"));
		assertNull(rates(row("Boss", "Example", "Common")).find("Boss", "Example"));
		assertTrue(rates(row("Boss", "Example", "Common")).isEmpty());

		// "Gull" is both a real drop and the slot alias of "Gull (pet)"; the real item wins either way.
		assertEquals("1/100", rates(row("Boss", "Gull (pet)", "1/1,000"), row("Boss", "Gull", "1/100"))
			.find("Boss", "Gull"));
		assertEquals("1/100", rates(row("Boss", "Gull", "1/100"), row("Boss", "Gull (pet)", "1/1,000"))
			.find("Boss", "Gull"));
	}

	@Test
	public void preservesFractionsRollsAndApproximationWithoutInventingPerKillOdds()
	{
		JsonObject drop = new JsonObject();
		drop.addProperty("Rarity", "2/5,461.33");
		drop.addProperty("Rolls", 3);
		drop.addProperty("Approx", true);
		assertEquals("~3 × 2/5461.33", WikiDropRates.rateText(drop));
		drop.addProperty("Rarity", "varies");
		assertEquals("Varies", WikiDropRates.rateText(drop));
		drop.addProperty("Rarity", "2/5,461.33");
		drop.addProperty("Rarity Notes", "Only with a task");
		assertEquals("Conditional", WikiDropRates.rateText(drop));
	}

	@Test
	public void thousandsSeparatorsOnlyAppearFromFiveDigits()
	{
		assertEquals("1/999", rarity("1/999"));
		assertEquals("1/1000", rarity("1/1,000"));
		assertEquals("1/9999", rarity("1/9,999"));
		assertEquals("1/10,000", rarity("1/10,000"));
		assertEquals("1/20,000", rarity("1/20,000"));
		assertEquals("1/313,168", rarity("1/313,168"));
		// The threshold applies per number, and decimals survive it.
		assertEquals("2/5461.33", rarity("2/5,461.33"));
		assertEquals("2/249,262.5", rarity("2/249,262.5"));
	}

	private String rarity(String value)
	{
		JsonObject drop = new JsonObject();
		drop.addProperty("Rarity", value);
		return WikiDropRates.rateText(drop);
	}

	@Test
	public void missingQualitativeAndInvalidRatesNeverBecomeNumbers()
	{
		// A guaranteed drop is not a rate either: "Always" and 1/1 are hidden rather than displayed.
		for (String rarity : new String[] {"", "Unknown", "Common", "1/0", "2/1", "0/100", "1/(2*128)", "NaN",
			"Always", "1/1", "5/5"})
		{
			JsonObject drop = new JsonObject();
			drop.addProperty("Rarity", rarity);
			assertEquals(rarity, "—", WikiDropRates.rateText(drop));
		}
	}

	@Test
	public void fetchPaginatesPublicDataAndKeepsAllPages() throws Exception
	{
		List<Request> requests = new ArrayList<>();
		WikiCompletionClient client = new WikiCompletionClient();
		client.gson = new Gson();
		client.http = new OkHttpClient.Builder().addInterceptor(chain -> {
			Request request = chain.request();
			requests.add(request);
			JsonArray rows = new JsonArray();
			if (requests.size() == 1)
			{
				for (int i = 0; i < 1000; i++) { rows.add(row("Boss A", "Example", "1/512")); }
			}
			// A short page is a server-side cap, not the end: only an empty page stops the loop.
			else if (requests.size() == 2) { rows.add(row("Boss B", "Other", "1/100")); }
			JsonObject response = new JsonObject();
			response.add("bucket", rows);
			return response(request, response.toString());
		}).build();
		WikiDropRates data = client.fetchDropRates();
		assertEquals("1/512", data.find("Boss A", "Example"));
		assertEquals("1/100", data.find("Boss B", "Other"));
		assertEquals(3, requests.size());
		int[] offsets = {0, 1000, 1001};
		for (int i = 0; i < requests.size(); i++)
		{
			Request request = requests.get(i);
			assertEquals("GET", request.method());
			assertEquals("oldschool.runescape.wiki", request.url().host());
			assertEquals("bucket", request.url().queryParameter("action"));
			assertEquals("bucket('dropsline').select('page_name_sub','item_name','drop_json')"
				+ ".orderBy('drop_json','asc').limit(1000).offset(" + offsets[i] + ").run()",
				request.url().queryParameter("query"));
			assertNull(request.body());
		}
	}

	@Test
	public void aMalformedRowIsSkippedInsteadOfDiscardingThePage() throws Exception
	{
		JsonObject noSource = row("", "Example", "1/512");
		JsonObject broken = new JsonObject();
		broken.addProperty("item_name", "Example");
		broken.addProperty("drop_json", "not json at all");
		JsonObject fallback = row("", "Fallback item", "1/64");
		fallback.addProperty("page_name_sub", "Boss C");
		WikiDropRates data = rates(noSource, broken, row("Boss A", "Example", "1/512"), fallback);
		assertEquals("A good row after a bad one must survive", "1/512", data.find("Boss A", "Example"));
		// page_name_sub names the source when the row itself omits it.
		assertEquals("1/64", data.find("Boss C", "Fallback item"));
		// The source-less rows created no entry of their own, under any key.
		assertEquals("Boss A", data.anySource("Example", 0));
		assertEquals("Boss A", data.anySource("Example", 1));
	}

	@Test
	public void theRowLimitKeepsWhatWasAlreadyRead() throws Exception
	{
		java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
		WikiCompletionClient client = new WikiCompletionClient();
		client.gson = new Gson();
		client.http = new OkHttpClient.Builder().addInterceptor(chain -> {
			calls.incrementAndGet();
			JsonArray rows = new JsonArray();
			// Never a short page, so the loop can only end at the row limit.
			for (int i = 0; i < 1000; i++) { rows.add(row("Boss A", "Example", "1/512")); }
			JsonObject response = new JsonObject();
			response.add("bucket", rows);
			return response(chain.request(), response.toString());
		}).build();
		WikiDropRates data = client.fetchDropRates();
		assertEquals("1/512", data.find("Boss A", "Example"));
		assertEquals(120, calls.get());
	}

	@Test
	public void rejectsErrorsEmptyDataAndMalformedRows() throws Exception
	{
		for (String body : new String[] {"{\"error\":\"Unavailable\",\"bucket\":[]}", "{\"bucket\":[]}",
			"{\"bucket\":[{\"item_name\":\"Example\",\"drop_json\":\"{}\"}]}"})
		{
			WikiCompletionClient client = new WikiCompletionClient();
			client.gson = new Gson();
			client.http = new OkHttpClient.Builder().addInterceptor(chain -> response(chain.request(), body)).build();
			try { client.fetchDropRates(); fail("Invalid data accepted"); }
			catch (IOException expected) { /* Keep previous data. */ }
		}
	}

	static okhttp3.Response response(Request request, String body)
	{
		return new okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1)
			.code(200).message("OK")
			.body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), body)).build();
	}
}
