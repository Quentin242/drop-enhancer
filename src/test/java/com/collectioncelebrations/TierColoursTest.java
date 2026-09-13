package com.collectioncelebrations;
import java.awt.Color;
import net.runelite.client.plugins.grounditems.GroundItemsConfig;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
public class TierColoursTest
{
	@Test
	public void followsLiveGroundItemsPaletteWithSeparatePetColour()
	{
		GroundItemsConfig ground = mock(GroundItemsConfig.class);
		when(ground.lowValueColor()).thenReturn(Color.BLUE);
		when(ground.mediumValueColor()).thenReturn(Color.GREEN);
		when(ground.highValueColor()).thenReturn(Color.ORANGE);
		when(ground.insaneValueColor()).thenReturn(Color.PINK);
		CelebrationConfig config = new CelebrationConfig() {};
		assertEquals(Color.BLUE, TierStyle.color(PreviewTier.COMMON, config, ground));
		assertEquals(Color.GREEN, TierStyle.color(PreviewTier.UNCOMMON, config, ground));
		assertEquals(Color.ORANGE, TierStyle.color(PreviewTier.RARE, config, ground));
		assertEquals(Color.PINK, TierStyle.color(PreviewTier.VERY_RARE, config, ground));
		when(ground.insaneValueColor()).thenReturn(new Color(100, 20, 60, 0));
		assertEquals(new Color(100, 20, 60), TierStyle.color(PreviewTier.VERY_RARE, config, ground));
		assertEquals(new Color(0x64FFE0), TierStyle.color(PreviewTier.PET, config, ground));
	}
	@Test
	public void customPaletteRemainsAvailable()
	{
		CelebrationConfig config = new CelebrationConfig() {
			@Override
			public boolean followGroundItemColours()
			{
				return false;
			}
			@Override
			public Color colourPet()
			{
				return Color.RED;
			}
		};
		assertEquals(Color.RED, TierStyle.color(PreviewTier.PET, config, mock(GroundItemsConfig.class)));
	}
}
