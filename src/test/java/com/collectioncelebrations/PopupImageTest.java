/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
public class PopupImageTest
{
	@Test
	public void unlockAddsOuterGoldButPreservesRarityBorder()
	{
		Celebration c = new Celebration("Mole claw", 7416, 1, "Giant Mole", true, 0, 0, 0);
		c.tier = PreviewTier.COMMON;
		java.awt.Color rarity = new java.awt.Color(0x45B9D7);
		BufferedImage unlock = panel(c, rarity);
		c.newSlot = false;
		BufferedImage repeat = panel(c, rarity);
		// Same left border colour, different pixels outside it where the gold ornament sits.
		assertEquals(rarity.getRGB(), unlock.getRGB(10, 60));
		assertEquals(unlock.getRGB(10, 60), repeat.getRGB(10, 60));
		assertTrue((unlock.getRGB(5, 60) >>> 24) > 0);
		assertEquals(0, repeat.getRGB(5, 60) >>> 24);
	}
	private BufferedImage panel(Celebration c, java.awt.Color rarity)
	{
		BufferedImage image = new BufferedImage(510, 240, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			CelebrationOverlay.drawPanel(g, 10, 10, 480, c, rarity, null);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}
	@Test
	public void animationEntersFromAboveAndPausesDuringCaseHold()
	{
		CelebrationOverlay overlay = new CelebrationOverlay();
		overlay.client = mock(Client.class);
		overlay.items = mock(ItemManager.class);
		overlay.config = new CelebrationConfig() {};
		when(overlay.client.getViewportWidth()).thenReturn(800);
		when(overlay.client.getViewportHeight()).thenReturn(600);
		Celebration c = new Celebration("Mole claw", -1, 1, "Giant Mole", true, 0, 0, 0);
		c.tier = PreviewTier.COMMON;
		overlay.show(c, 0);
		assertEquals(0, pixels(overlay));
		overlay.advance(550, false);
		assertTrue(pixels(overlay) > 1000);
		overlay.advance(600, true);
		assertEquals(0, pixels(overlay));
		overlay.advance(100000, true);
		assertFalse(overlay.idle());
		overlay.advance(100001, false);
		assertTrue(pixels(overlay) > 1000);
		overlay.advance(110000, false);
		assertTrue(overlay.idle());
	}
	private int pixels(CelebrationOverlay overlay)
	{
		BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			overlay.render(g);
		}
		finally
		{
			g.dispose();
		}
		int count = 0;
		for (int y = 0; y < 600; y++)
		{
			for (int x = 0; x < 800; x++)
			{
				if ((image.getRGB(x, y) >>> 24) > 0)
				{
					count++;
				}
			}
		}
		return count;
	}
	@Test
	public void previewRequestsActualItemSpriteAndCanBeReplaced()
	{
		CelebrationOverlay overlay = new CelebrationOverlay();
		overlay.client = mock(Client.class);
		overlay.items = mock(ItemManager.class);
		overlay.config = new CelebrationConfig() {};
		when(overlay.client.getViewportWidth()).thenReturn(800);
		when(overlay.client.getViewportHeight()).thenReturn(600);
		Celebration preview = new Celebration("Bandos chestplate", 11832, 1, "Preview", true, 0, 0, 0);
		preview.previewTier = PreviewTier.RARE;
		preview.tier = PreviewTier.RARE;
		overlay.show(preview, 0);
		Graphics2D g = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			overlay.render(g);
		}
		finally
		{
			g.dispose();
		}
		verify(overlay.items).getImage(11832);
		overlay.clearPreview();
		assertTrue(overlay.idle());
		preview.previewTier = null;
		overlay.show(preview, 0);
		overlay.clearPreview();
		assertFalse(overlay.idle());
	}
	@Test
	public void onlyNewUnlockHasCollectionHeading()
	{
		Celebration c = new Celebration("Mole claw", 7416, 1, "Giant Mole", true, 0, 0, 0);
		assertEquals("NEW COLLECTION LOG", CelebrationOverlay.heading(c));
		c.newSlot = false;
		assertEquals("", CelebrationOverlay.heading(c));
		c.previewTier = PreviewTier.COMMON;
		assertEquals("TEST", CelebrationOverlay.heading(c));
	}
	@Test
	public void highTierEffectsMoveAndCanBeDisabled()
	{
		Celebration c = new Celebration("Twisted bow", -1, 1, "Chambers of Xeric", false, 0, 0, 0);
		c.tier = PreviewTier.RARE;
		CelebrationConfig on = new CelebrationConfig() {};
		CelebrationConfig off = new CelebrationConfig() {
			@Override
			public boolean tierEffects()
			{
				return false;
			}
		};
		assertFalse(java.util.Arrays.equals(effectPixels(c, on, 1200), effectPixels(c, on, 1700)));
		assertArrayEquals(effectPixels(c, off, 1200), effectPixels(c, off, 1700));
	}
	private int[] effectPixels(Celebration c, CelebrationConfig config, long elapsed)
	{
		BufferedImage image = new BufferedImage(510, 260, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			CelebrationOverlay.drawPanel(g, 10, 10, 480, c, java.awt.Color.PINK, null, true, true, true, true, config, elapsed);
		}
		finally
		{
			g.dispose();
		}
		return image.getRGB(0, 0, 510, 260, null, 0, 510);
	}
	@Test
	public void fixedTopPaddingKeepsGoldBannerVisible()
	{
		CelebrationOverlay overlay = new CelebrationOverlay();
		overlay.client = mock(Client.class);
		overlay.items = mock(ItemManager.class);
		overlay.config = new CelebrationConfig() {};
		when(overlay.client.getViewportWidth()).thenReturn(800);
		when(overlay.client.getViewportHeight()).thenReturn(600);
		Celebration c = new Celebration("Mole claw", -1, 1, "Giant Mole", true, 0, 0, 0);
		c.tier = PreviewTier.COMMON;
		overlay.show(c, 0);
		overlay.advance(1200, false);
		BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			overlay.render(g);
		}
		finally
		{
			g.dispose();
		}
		assertTrue("Only the faint halo may reach the top margin", (image.getRGB(300, 0) >>> 24) < (image.getRGB(300, 8) >>> 24));
		assertTrue((image.getRGB(300, 8) >>> 24) > 0);
		java.awt.Color banner = new java.awt.Color(image.getRGB(300, 24), true);
		assertTrue("Visible gold ribbon inside the screen", banner.getRed() > banner.getBlue() + 30);
	}
}
