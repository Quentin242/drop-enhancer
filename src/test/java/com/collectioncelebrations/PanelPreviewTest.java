/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

/** Offscreen Java2D only: this does not load a game client or simulate game content. */
public class PanelPreviewTest
{
	@Test
	public void renderOriginalPanelsWithoutGameAssets() throws Exception
	{
		BufferedImage image = new BufferedImage(1040, 1640, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.scale(2, 2);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(14, 15, 21));
		g.fillRect(0, 0, 1040, 1640);
		g.setColor(Color.LIGHT_GRAY);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		g.drawString("Drop Enhancer · offline layout preview", 40, 30);
		Celebration c = new Celebration("Bandos chestplate", -1, 1, "General Graardor", true, 0, 0, 0);
		c.tier = PreviewTier.RARE;
		c.wikiCompletion = 15.6;
		c.dropRateText = "1/381";
		c.value = 27000000;
		c.confirmedTotal = 1;
		c.kc = "Kills: 347";
		CelebrationOverlay.drawPanel(g, 20, 55, 480, c, new Color(0xFF9600), null);
		c.tier = PreviewTier.VERY_RARE;
		c.newSlot = false;
		c.confirmedTotal = 2;
		c.kc = "Kills: 512";
		CelebrationOverlay.drawPanel(g, 20, 305, 480, c, new Color(0xFF66B2), null);
		c = new Celebration("Pet general graardor", -1, 1, "General Graardor", false, 0, 0, 0);
		c.tier = PreviewTier.PET;
		c.confirmedTotal = 2;
		c.kc = "Kills: 512";
		CelebrationOverlay.drawPanel(g, 20, 555, 480, c, new Color(0x64FFE0), null);
		g.dispose();
		File out = new File("build/preview.png");
		out.getParentFile().mkdirs();
		ImageIO.write(image, "png", out);
		assertTrue(out.length() > 1000);
	}
}
