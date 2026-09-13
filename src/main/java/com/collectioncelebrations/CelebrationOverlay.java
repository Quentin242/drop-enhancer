/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE.
 * Layout, font rendering and separate icon animation adapted from SnakeSteak's
 * Collection Log Popup Enhanced; copyright (c) 2026 SnakeSteak.
 * See licenses/enhanced.txt. All frame artwork below is original Java2D geometry. */
package com.collectioncelebrations;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;

@Singleton
class CelebrationOverlay extends Overlay
{
	static final int WIDTH = 480, HEIGHT = 234;
	static final long SLIDE_MS = 550, ICON_MS = 400, FADE_MS = 400;
	@Inject
	Client client;
	@Inject
	ItemManager items;
	@Inject
	CelebrationConfig config;
	@Inject
	net.runelite.client.plugins.grounditems.GroundItemsConfig groundItemsConfig;
	private Celebration current;
	private long remaining, lifetime, lastFrame;
	private boolean held;

	CelebrationOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);
		setMovable(false);
	}
	void clearPreview()
	{
		if (current != null && current.previewTier != null)
		{
			clear();
		}
	}
	boolean idle()
	{
		return current == null;
	}
	boolean showing(Celebration c)
	{
		return current == c;
	}
	void show(Celebration c, long now)
	{
		current = c;
		lifetime = Math.max(1, Math.min(20, config.displaySeconds())) * 1000L + SLIDE_MS + ICON_MS + FADE_MS;
		remaining = lifetime;
		lastFrame = now;
		held = false;
	}
	void clear()
	{
		current = null;
		remaining = 0;
		held = false;
	}
	void advance(long now, boolean hold)
	{
		if (current != null && !held && !hold)
		{
			remaining -= Math.max(0, now - lastFrame);
		}
		lastFrame = now;
		held = hold;
		if (remaining <= 0)
		{
			current = null;
		}
	}
	static double slideProgress(long elapsed)
	{
		double t = Math.max(0, Math.min(1, elapsed / (double)SLIDE_MS));
		return 1 - Math.pow(1 - t, 3);
	}
	@Override
	public Dimension render(Graphics2D original)
	{
		if (current == null || held || (!config.showPopups() && current.previewTier == null))
		{
			return null;
		}
		Graphics2D g = (Graphics2D)original.create();
		try
		{
			double scale = Math.max(.6, Math.min(2, config.popupScale() / 100.0));
			scale = Math.min(
				scale, Math.min((client.getViewportWidth() - 24) / (double)WIDTH, (client.getViewportHeight() - 24) / (double)HEIGHT));
			if (scale <= 0)
			{
				return null;
			}
			int width = (int)Math.round(WIDTH * scale), height = (int)Math.ceil(HEIGHT * scale);
			int left = client.getViewportXOffset(), top = client.getViewportYOffset();
			int x = left + (client.getViewportWidth() - width) / 2;
			int targetY = top + 8;
			long elapsed = lifetime - remaining;
			int y = (int)Math.round(top - height - 12 + (targetY - top + height + 12) * slideProgress(elapsed));
			g.clipRect(left, top, client.getViewportWidth(), client.getViewportHeight());
			g.setComposite(AlphaComposite.SrcOver.derive((float)Math.max(0, Math.min(1, remaining / (double)FADE_MS))));
			// Draw geometry and fonts at the actual target size, never stretch a popup bitmap.
			drawPanel(g, x, y, width, current, TierStyle.color(current.tier, config, groundItemsConfig),
					  config.showItemIcon() && current.itemId >= 0 ? items.getImage(current.itemId) : null, config.showTotal(),
					  config.showKc(), config.showValue(), config.showWiki(), config, elapsed);
		}
		finally
		{
			g.dispose();
		}
		return null;
	}
	static void drawPanel(Graphics2D g, int x, int y, int width, Celebration c, Color accent, BufferedImage icon)
	{
		drawPanel(g, x, y, width, c, accent, icon, true, true, true, true, new CelebrationConfig() {});
	}
	static void drawPanel(Graphics2D g, int x, int y, int width, Celebration c, Color accent, BufferedImage icon, boolean totalVisible,
						  boolean kcVisible, boolean valueVisible, boolean wikiVisible, CelebrationConfig config)
	{
		drawPanel(g, x, y, width, c, accent, icon, totalVisible, kcVisible, valueVisible, wikiVisible, config, 1200);
	}

	static void drawPanel(Graphics2D original, int x, int y, int width, Celebration c, Color accent, BufferedImage icon,
						  boolean totalVisible, boolean kcVisible, boolean valueVisible, boolean wikiVisible, CelebrationConfig config,
						  long elapsed)
	{
		Graphics2D g = (Graphics2D)original.create();
		try
		{
			g.translate(x, y);
			Layout l = new Layout(width / (double)WIDTH, config);
			boolean crisp = config.textRenderMode() == TextRenderMode.CRISP;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
							   crisp ? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
							   crisp ? RenderingHints.VALUE_FRACTIONALMETRICS_OFF : RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
							   crisp ? RenderingHints.VALUE_STROKE_DEFAULT : RenderingHints.VALUE_STROKE_PURE);
			if (accent == null)
			{
				accent = new Color(221, 190, 125);
			}
			float brightness = Math.max(10, Math.min(60, config.backgroundDarkness())) / 100f;
			Color tint = new Color(Math.round(accent.getRed() * brightness), Math.round(accent.getGreen() * brightness),
								   Math.round(accent.getBlue() * brightness));
			Shape frame = frame(l, 0);
			if (c.newSlot && config.unlockEffects())
			{
				// Gold is a separate halo OUTSIDE the unmodified rarity frame.
				for (int i = 18; i >= 2; i -= 2)
				{
					g.setStroke(new BasicStroke(l.f(i)));
					g.setColor(new Color(255, 204, 92, 24));
					g.draw(frame);
				}
			}
			g.setPaint(new GradientPaint(0, 0, tint, 0, l.p(176), new Color(14, 17, 23)));
			g.fill(frame);
			g.setColor(accent);
			g.setStroke(new BasicStroke(l.f(2)));
			g.draw(frame);
			g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 85));
			g.setStroke(new BasicStroke(l.f(1)));
			g.draw(frame(l, 5));
			// Decorative top diamond and tier label belong to the rarity, not the unlock effect.
			g.setColor(accent);
			g.fillPolygon(new int[] {l.p(240), l.p(244), l.p(240), l.p(236)}, new int[] {l.p(3), l.p(7), l.p(11), l.p(7)}, 4);
			if (c.newSlot && config.unlockEffects())
			{
				// Keep the gold visible inside the viewport even when attached to its top edge.
				drawUnlockEffect(g, l, elapsed);
			}
			String caption = heading(c);
			l.center(g, caption, 240, 29, 18, c.newSlot ? new Color(0xFFE5A1) : config.colourCaption(), 332);
			String name = c.name + (c.dropQuantity > 1 ? " ×" + c.dropQuantity : "");
			l.center(g, name, 240, 66, 24, config.colourItemName(), 432);
			l.center(g, c.source == null ? "Source unavailable" : c.source, 240, 85, 13, config.colourStatLabel(), 420);
			g.setColor(new Color(0, 0, 0, 120));
			g.fillRect(l.p(24), l.p(96), l.p(432), l.p(3));
			if (config.showProgressBar() && c.progressMax > 0)
			{
				g.setColor(accent);
				g.fillRect(l.p(24), l.p(96), l.p(432 * Math.max(0, Math.min(1.0, c.progressCount / (double)c.progressMax))),
						   Math.max(1, l.p(3)));
			}
			PopupStat[] choices = {config.stat1(), config.stat2(), config.stat3(), config.stat4()};
			for (int i = 0; i < choices.length; i++)
			{
				String label = "", value = "—";
				PopupStat choice = choices[i];
				if (choice == null || choice == PopupStat.NONE)
				{
					continue;
				}
				if (choice == PopupStat.DROP_RATE && c.dropRateText == null)
				{
					choice = PopupStat.VALUE;
				}
				switch (choice)
				{
				case COLLECTION_COUNT:
					label = c.confirmedTotal != null ? "COLLECTED" : c.lastSyncedTotal != null ? "LAST SYNCED" : "TEMPORARY";
					if (totalVisible)
					{
						value = String.valueOf(c.confirmedTotal != null	   ? c.confirmedTotal
											   : c.lastSyncedTotal != null ? c.lastSyncedTotal
																		   : c.provisionalTotal);
					}
					break;
				case KILL_COUNT:
					if (!kcVisible || c.kc == null || c.kc.isBlank())
					{
						continue;
					}
					value = c.kc.replaceFirst("^[^:]+: *", "");
					if (value.isBlank())
					{
						continue;
					}
					label = "KILL COUNT";
					break;
				case VALUE:
					label = "ITEM VALUE";
					if (valueVisible)
					{
						value = c.untradeable ? "Untradeable" : String.format(java.util.Locale.ROOT, "%,d gp", c.value);
					}
					break;
				case WIKI_COMPLETION:
					label = "WIKI COMP.";
					if (wikiVisible && c.wikiCompletion != null)
					{
						value = String.format(java.util.Locale.ROOT, "%.2f%%", c.wikiCompletion);
					}
					break;
				case DROP_RATE:
					label = "DROP RATE";
					value = c.dropRateText;
					break;
				default:
					break;
				}
				// Two stacked stats on each side leave the central item medallion clear.
				int cx = i % 2 == 0 ? 105 : 375, baseline = i < 2 ? 113 : 150;
				l.center(g, label, cx, baseline, 12, config.colourStatLabel(), 156);
				l.center(g, value, cx, baseline + 18, 18, config.colourStatValue(), 156);
			}
			if (config.tierEffects() && elapsed >= SLIDE_MS)
			{
				drawTierEffect(g, l, c.tier, accent, elapsed, config.showItemIcon());
			}
			if (config.showItemIcon() && elapsed >= SLIDE_MS)
			{
				double raw = Math.max(0, Math.min(1, (elapsed - SLIDE_MS) / (double)ICON_MS));
				double t = raw - 1, pop = .4 + .6 * (1 + 2.9 * t * t * t + 1.9 * t * t);
				int radius = l.p(39 * pop), cx = l.p(240), cy = l.p(171);
				g.setColor(new Color(16, 20, 28));
				g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
				g.setStroke(new BasicStroke(l.f(2)));
				g.setColor(accent);
				g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
				g.setStroke(new BasicStroke(l.f(1)));
				g.drawOval(cx - radius + l.p(5), cy - radius + l.p(5), radius * 2 - l.p(10), radius * 2 - l.p(10));
				if (icon != null)
				{
					double spriteScale = l.scale * 58 * pop / Math.max(icon.getWidth(), icon.getHeight());
					int iw = (int)Math.round(icon.getWidth() * spriteScale), ih = (int)Math.round(icon.getHeight() * spriteScale);
					// Match Enhanced: crisp game pixel art, independently scaled from the text.
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					g.drawImage(icon, cx - iw / 2, cy - ih / 2, iw, ih, null);
				}
				else
				{
					l.center(g, "…", 240, 177, 24, accent, 50);
				}
			}
			l.center(g, c.tier == null ? "" : c.tier.toString(), 240, 117, 14, accent, 100);
		}
		finally
		{
			g.dispose();
		}
	}
	private static Shape frame(Layout l, int inset)
	{
		Path2D p = new Path2D.Double();
		p.moveTo(l.p(14 + inset), l.p(inset));
		p.lineTo(l.p(466 - inset), l.p(inset));
		p.lineTo(l.p(480 - inset), l.p(14 + inset));
		p.lineTo(l.p(480 - inset), l.p(162 - inset));
		p.lineTo(l.p(466 - inset), l.p(176 - inset));
		p.lineTo(l.p(14 + inset), l.p(176 - inset));
		p.lineTo(l.p(inset), l.p(162 - inset));
		p.lineTo(l.p(inset), l.p(14 + inset));
		p.closePath();
		return p;
	}
	static String heading(Celebration c)
	{
		return c.newSlot		? (c.previewTier != null ? "TEST · NEW COLLECTION LOG" : "NEW COLLECTION LOG")
		: c.previewTier != null ? "TEST"
								: "";
	}
	private static void drawUnlockEffect(Graphics2D g, Layout l, long elapsed)
	{
		double pulse = .5 + .5 * Math.sin(elapsed / 400.0);
		// Persistent gold title ribbon, with a slow shimmer rather than a barely visible outer line.
		g.setPaint(new GradientPaint(l.p(68), 0, new Color(171, 110, 22, 85), l.p(240), 0, new Color(244, 190, 58, 115), true));
		g.fillRoundRect(l.p(68), l.p(10), l.p(344), l.p(26), l.p(8), l.p(8));
		for (int side = 0; side < 2; side++)
		{
			int x = side == 0 ? 10 : 470;
			for (int i = 0; i < 4; i++)
			{
				double phase = ((elapsed + i * 370L) % 1700) / 1700.0;
				double y = 160 - phase * 111;
				double xx = x + (side == 0 ? 1 : -1) * (4 + Math.sin(phase * Math.PI) * 10);
				star(g, l, xx, y, 2 + 3 * Math.sin(phase * Math.PI), new Color(255, 227, 151, (int)(230 * Math.sin(phase * Math.PI))));
			}
		}
		star(g, l, 56, 26, 6 + 2 * pulse, new Color(255, 223, 128));
		star(g, l, 424, 26, 6 + 2 * pulse, new Color(255, 223, 128));
	}
	private static void drawTierEffect(Graphics2D g, Layout l, PreviewTier tier, Color colour, long elapsed, boolean iconVisible)
	{
		int strength = tier == PreviewTier.PET ? 3 : tier == PreviewTier.VERY_RARE ? 2 : tier == PreviewTier.RARE ? 1 : 0;
		if (strength == 0)
		{
			return;
		}
		// This effect belongs to the rarity, so it also appears on repeats.
		if (iconVisible)
		{
			double rotation = elapsed / 850.0;
			int cx = l.p(240), cy = l.p(171), r = l.p(46);
			g.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 155));
			g.setStroke(new BasicStroke(l.f(1.5)));
			for (int i = 0; i < strength + 1; i++)
			{
				double angle = rotation + i * Math.PI * 2 / (strength + 1);
				g.drawArc(cx - r, cy - r, r * 2, r * 2, (int)Math.toDegrees(-angle), 38);
				star(g, l, 240 + Math.cos(angle) * 46, 171 + Math.sin(angle) * 46, 3, colour);
			}
			if (strength > 1)
			{
				for (int i = 0; i < 12; i++)
				{
					double angle = i * Math.PI * 2 / 12 + rotation / 4;
					double ray = 49 + (strength == 3 ? 8 : 4) * (.5 + .5 * Math.sin(rotation * 2 + i));
					g.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 100));
					g.drawLine(l.p(240 + Math.cos(angle) * 43), l.p(171 + Math.sin(angle) * 43), l.p(240 + Math.cos(angle) * ray),
							   l.p(171 + Math.sin(angle) * ray));
				}
			}
		}
		if (strength >= 1)
		{
			for (int i = 0; i < strength * 4; i++)
			{
				double phase = ((elapsed + i * 271L) % 2400) / 2400.0;
				double x = i % 2 == 0 ? 30 + (i % 3) * 9 : 450 - (i % 3) * 9;
				star(g, l, x, 176 - phase * 150, 1.5 + 1.5 * Math.sin(phase * Math.PI),
					 new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), (int)(170 * Math.sin(phase * Math.PI))));
			}
		}
	}
	private static void star(Graphics2D g, Layout l, double x, double y, double radius, Color colour)
	{
		int cx = l.p(x), cy = l.p(y), r = Math.max(1, l.p(radius));
		g.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha() / 5));
		g.fillOval(cx - r * 2, cy - r * 2, r * 4, r * 4);
		g.setColor(colour);
		g.fillPolygon(new int[] {cx, cx + Math.max(1, r / 3), cx + r, cx + Math.max(1, r / 3), cx, cx - Math.max(1, r / 3), cx - r,
								 cx - Math.max(1, r / 3)},
					  new int[] {cy - r, cy - Math.max(1, r / 3), cy, cy + Math.max(1, r / 3), cy + r, cy + Math.max(1, r / 3), cy,
								 cy - Math.max(1, r / 3)},
					  8);
	}
	private static final class Layout
	{
		final double scale;
		final CelebrationConfig config;
		Layout(double scale, CelebrationConfig config)
		{
			this.scale = scale;
			this.config = config;
		}
		int p(double value)
		{
			return (int)Math.round(value * scale);
		}
		float f(double value)
		{
			return (float)Math.max(.5, value * scale);
		}
		void center(Graphics2D g, String value, int x, int y, int size, Color colour, int maxWidth)
		{
			Font base = FontManager.getRunescapeBoldFont();
			g.setFont(base.deriveFont(f(size)));
			int width = p(maxWidth);
			while (g.getFontMetrics().stringWidth(value) > width && size > 12)
			{
				g.setFont(base.deriveFont(f(--size)));
			}
			if (g.getFontMetrics().stringWidth(value) > width)
			{
				while (!value.isEmpty() && g.getFontMetrics().stringWidth(value + "…") > width)
				{
					value = value.substring(0, value.length() - 1);
				}
				value += "…";
			}
			int left = p(x) - g.getFontMetrics().stringWidth(value) / 2;
			if (config.textRenderMode() == TextRenderMode.SMOOTH_OUTLINED)
			{
				g.setColor(Color.BLACK);
				g.drawString(value, left + Math.max(1, p(1)), p(y) + Math.max(1, p(1)));
			}
			g.setColor(colour);
			g.drawString(value, left, p(y));
		}
	}
}
