package com.collectioncelebrations;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

public class WikiLifecycleTest
{
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private WikiRarity create() throws Exception
	{
		WikiRarity wiki = new WikiRarity();
		wiki.dataDirectory = folder.newFolder().toPath();
		wiki.gson = new Gson();
		wiki.config = mock(CelebrationConfig.class, CALLS_REAL_METHODS);
		wiki.remote = new WikiCompletionClient();
		wiki.remote.gson = wiki.gson;
		wiki.resolver = mock(RarityResolver.class);
		wiki.dropRates = new DropRateResolver();
		return wiki;
	}

	private okhttp3.Response response(okhttp3.Interceptor.Chain chain)
	{
		String body = chain.request().url().encodedPath().endsWith("data.json") ? "[{\"id\":1,\"name\":\"Example\"}]" : "{\"1\":50}";
		return new okhttp3.Response.Builder()
			.request(chain.request())
			.protocol(okhttp3.Protocol.HTTP_1_1)
			.code(200)
			.message("OK")
			.body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), body))
			.build();
	}

	@Test
	public void enablingLoadsDataWithoutPluginRestart() throws Exception
	{
		WikiRarity wiki = create();
		wiki.remote.http = new okhttp3.OkHttpClient.Builder().addInterceptor(this::response).build();
		try
		{
			wiki.start();
			when(wiki.config.refreshWikiData()).thenReturn(true);
			wiki.refreshSetting();
			verify(wiki.resolver, timeout(2000).atLeastOnce()).reload(anyMap());
		}
		finally
		{
			wiki.stop();
		}
	}

	@Test
	public void disablingCancelsAndRejectsLateNetworkResult() throws Exception
	{
		WikiRarity wiki = create();
		CountDownLatch entered = new CountDownLatch(1);
		AtomicReference<Thread> worker = new AtomicReference<>();
		when(wiki.config.refreshWikiData()).thenReturn(true);
		AtomicReference<okhttp3.Call> request = new AtomicReference<>();
		wiki.remote.http = new okhttp3.OkHttpClient.Builder()
							   .addInterceptor(chain -> {
								   request.set(chain.call());
								   worker.set(Thread.currentThread());
								   entered.countDown();
								   try
								   {
									   new CountDownLatch(1).await(2, TimeUnit.SECONDS);
								   }
								   catch (InterruptedException expected)
								   {
									   Thread.currentThread().interrupt();
								   }
								   return response(chain);
							   })
							   .build();
		try
		{
			wiki.start();
			assertTrue(entered.await(2, TimeUnit.SECONDS));
			when(wiki.config.refreshWikiData()).thenReturn(false);
			wiki.refreshSetting();
			worker.get().join(2000);
			assertFalse(worker.get().isAlive());
			assertTrue(request.get().isCanceled());
			verify(wiki.resolver, never()).reload(anyMap());
			assertNull(wiki.entry(1, "Example"));
		}
		finally
		{
			wiki.stop();
		}
	}
}
