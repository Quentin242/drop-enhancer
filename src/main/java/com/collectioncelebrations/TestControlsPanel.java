/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;

/** Small action panel only; RuneLite's native cog owns all persistent settings. */
final class TestControlsPanel extends PluginPanel
{
	TestControlsPanel(BiConsumer<String, PreviewTier> action, net.runelite.client.config.ConfigManager manager)
	{
		setLayout(new BorderLayout());
		JPanel controls = new JPanel(new GridLayout(0, 1, 0, 6));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		controls.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
		JLabel title = new JLabel("Drop Enhancer tests");
		title.setForeground(Color.WHITE);
		controls.add(title);
		JComboBox<PreviewTier> tier = new JComboBox<>(PreviewTier.values());
		controls.add(tier);
		for (String label : new String[] {"Test selected", "Add to queue", "Test all tiers", "Next", "Stop"})
		{
			JButton button = new JButton(label);
			button.setName(label);
			button.addActionListener(e -> action.accept(label, (PreviewTier)tier.getSelectedItem()));
			controls.add(button);
		}
		JLabel note = new JLabel("<html>Log in to test.<br>Highest rarity goes first.<br>New unlock/drop: plugin cog → Test popup.</html>");
		note.setForeground(Color.LIGHT_GRAY);
		controls.add(note);
		JLabel soundsLabel = new JLabel("Sound file");
		soundsLabel.setForeground(Color.WHITE);
		controls.add(soundsLabel);
		JComboBox<SoundOption> sound = new JComboBox<>(SOUND_OPTIONS);
		controls.add(sound);
		JLabel selected = new JLabel();
		selected.setForeground(Color.LIGHT_GRAY);
		controls.add(selected);
		Runnable refresh = () ->
		{
			SoundOption option = (SoundOption)sound.getSelectedItem();
			String value = manager.getConfiguration("collection-celebrations", option.key);
			selected.setText(value == null ? "Default sound" : value);
			selected.setToolTipText(value);
		};
		sound.addActionListener(e -> refresh.run());
		refresh.run();
		JButton choose = new JButton("Choose WAV…");
		controls.add(choose);
		choose.addActionListener(event -> {
			java.nio.file.Path folder = net.runelite.client.RuneLite.RUNELITE_DIR.toPath().resolve("collection-celebrations/sounds");
			JFileChooser chooser = new JFileChooser(folder.toFile());
			chooser.setDialogTitle("Choose Drop Enhancer sound");
			chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("WAV audio", "wav"));
			chooser.setAcceptAllFileFilterUsed(false);
			if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			{
				return;
			}
			java.nio.file.Path source = chooser.getSelectedFile().toPath();
			SoundOption target = (SoundOption)sound.getSelectedItem();
			choose.setEnabled(false);
			new SwingWorker<String, Void>() {
				@Override
				protected String doInBackground() throws Exception
				{
					return LocalSoundFiles.select(source, folder);
				}
				@Override
				protected void done()
				{
					try
					{
						manager.setConfiguration("collection-celebrations", target.key, get());
						refresh.run();
					}
					catch (Exception error)
					{
						JOptionPane.showMessageDialog(TestControlsPanel.this,
													  "Unable to select this WAV: " +
														  (error.getCause() == null ? error.getMessage() : error.getCause().getMessage()),
													  "Sound selection", JOptionPane.ERROR_MESSAGE);
					}
					finally
					{
						choose.setEnabled(true);
					}
				}
			}.execute();
		});
		add(controls, BorderLayout.NORTH);
	}
	private static final SoundOption[] SOUND_OPTIONS = {new SoundOption("fileCommon", "Low / Common"),
														new SoundOption("fileUncommon", "Medium / Uncommon"),
														new SoundOption("fileRare", "High / Rare"),
														new SoundOption("fileVeryRare", "Highest / Very rare"),
														new SoundOption("filePet", "Pet"),
														new SoundOption("highlightedFile", "Highlighted items (custom)"),
														new SoundOption("unlockFile", "New collection log jingle")};

	private static final class SoundOption
	{
		final String key, label;
		SoundOption(String key, String label)
		{
			this.key = key;
			this.label = label;
		}
		@Override
		public String toString()
		{
			return label;
		}
	}

	static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setColor(new Color(0xFF66B2));
			g.fillPolygon(new int[] {4, 13, 4}, new int[] {2, 8, 14}, 3);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}
}
