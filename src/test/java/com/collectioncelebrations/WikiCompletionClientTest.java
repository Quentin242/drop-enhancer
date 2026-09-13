package com.collectioncelebrations;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class WikiCompletionClientTest
{
	@Test
	public void joinsByIdWithoutInventingMissingCompletionOrOwnership() throws Exception
	{
		JsonObject data = WikiCompletionClient.merge(
			new JsonParser()
				.parse("[{\"id\":1,\"name\":\"Example\",\"tabs\":[\"All Pets\"]},{\"id\":2,\"name\":\"New item\"}]")
				.getAsJsonArray(),
			new JsonParser().parse("{\"1\":12.5,\"999\":99}").getAsJsonObject());
		assertEquals(2, data.size());
		assertEquals(12.5, data.getAsJsonObject("1").get("comp").getAsDouble(), 0);
		assertFalse(data.getAsJsonObject("2").has("comp"));
		assertTrue(WikiRarity.parse(data).get(1).pet);
	}
	@Test(expected = java.io.IOException.class)
	public void rejectsInvalidPercentage() throws Exception
	{
		WikiCompletionClient.merge(new JsonParser().parse("[{\"id\":1,\"name\":\"Example\"}]").getAsJsonArray(),
								   new JsonParser().parse("{\"1\":101}").getAsJsonObject());
	}
	@Test(expected = java.io.IOException.class)
	public void rejectsEmptyItemResponse() throws Exception
	{
		WikiCompletionClient.merge(new com.google.gson.JsonArray(), new JsonObject());
	}

	@Test
	public void fetchUsesOnlyPublicGetRequests() throws Exception
	{
		java.util.List<okhttp3.Request> requests = new java.util.ArrayList<>();
		WikiCompletionClient client = new WikiCompletionClient();
		client.gson = new com.google.gson.Gson();
		client.http = new okhttp3.OkHttpClient.Builder()
						  .addInterceptor(chain -> {
							  okhttp3.Request request = chain.request();
							  requests.add(request);
							  String body =
								  request.url().encodedPath().endsWith("/data.json") ? "[{\"id\":1,\"name\":\"Example\"}]" : "{\"1\":50}";
							  return new okhttp3.Response.Builder()
								  .request(request)
								  .protocol(okhttp3.Protocol.HTTP_1_1)
								  .code(200)
								  .message("OK")
								  .body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), body))
								  .build();
						  })
						  .build();
		assertEquals(50, client.fetch().getAsJsonObject("1").get("comp").getAsInt());
		assertEquals(2, requests.size());
		for (okhttp3.Request request : requests)
		{
			assertEquals("oldschool.runescape.wiki", request.url().host());
			assertEquals("GET", request.method());
			assertEquals("action=raw", request.url().query());
			assertTrue(request.header("User-Agent").startsWith("DropEnhancer/"));
			assertNull(request.body());
		}
	}
	@Test
	public void missingAndNullCompletionStayUnknownWhileZeroIsPreserved() throws Exception
	{
		JsonObject result = WikiCompletionClient.merge(new JsonParser().parse(
			"[{\"id\":1,\"name\":\"Missing\"},{\"id\":2,\"name\":\"Null\"},{\"id\":3,\"name\":\"Zero\"}]").getAsJsonArray(),
			new JsonParser().parse("{\"2\":null,\"3\":0}").getAsJsonObject());
		assertFalse(result.getAsJsonObject("1").has("comp"));
		assertFalse(result.getAsJsonObject("2").has("comp"));
		assertEquals(0, result.getAsJsonObject("3").get("comp").getAsInt());
	}

	@Test(expected = java.io.IOException.class)
	public void unknownContentLengthStillEnforcesDownloadLimit() throws Exception
	{
		WikiCompletionClient client = new WikiCompletionClient();
		client.gson = new com.google.gson.Gson();
		client.http = new okhttp3.OkHttpClient.Builder().addInterceptor(chain -> {
			okhttp3.ResponseBody body = new okhttp3.ResponseBody()
			{
				private final okio.Buffer data = new okio.Buffer().write(new byte[2 * 1024 * 1024 + 1]);
				@Override public okhttp3.MediaType contentType() { return okhttp3.MediaType.parse("application/json"); }
				@Override public long contentLength() { return -1; }
				@Override public okio.BufferedSource source() { return data; }
			};
			return new okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
				.code(200).message("OK").body(body).build();
		}).build();
		client.fetch();
	}

}
