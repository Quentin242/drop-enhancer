/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.config.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
public class ConfigVisibilityTest
{
	@Test
	public void everySettingsItemHasAVisibleSection()
	{
		ConfigManager manager = mock(ConfigManager.class, CALLS_REAL_METHODS);
		ConfigDescriptor descriptor = manager.getConfigDescriptor(new CelebrationConfig() {});
		Set<String> sections = descriptor.getSections().stream().map(ConfigSectionDescriptor::getKey).collect(Collectors.toSet());
		assertTrue(sections.contains("valueDrops"));
		assertTrue(sections.contains("clueDrops"));
		assertFalse(sections.contains("specWeapon"));
		assertEquals(6, sections.size());
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			assertTrue(item.getItem().keyName(), item.getItem().section().isEmpty() || sections.contains(item.getItem().section()));
		}
		assertTrue(descriptor.getItems().size() > 60);
	}
	@Test
	public void wikiNetworkAccessIsOptOutWithPrivacyWarning()
	{
		CelebrationConfig config = new CelebrationConfig() {};
		// On by default, so the popup has rates out of the box; the switch remains the way to stop it.
		assertTrue(config.refreshWikiData());
		ConfigManager manager = mock(ConfigManager.class, CALLS_REAL_METHODS);
		ConfigItem item = manager.getConfigDescriptor(config).getItems().stream()
			.map(ConfigItemDescriptor::getItem).filter(c -> c.keyName().equals("refreshWikiData"))
			.findFirst().orElseThrow();
		assertEquals("This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", item.warning());
	}

}
