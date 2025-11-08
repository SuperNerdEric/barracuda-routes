package com.barracudaroutes;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("barracudaroutes")
public interface BarracudaRoutesConfig extends Config
{
	@ConfigItem(
		keyName = "hideDistance",
		name = "Hide Distance",
		description = "Distance in tiles at which route tiles will hide when the player is near them"
	)
	default int hideDistance()
	{
		return 5;
	}
	
	@ConfigItem(
		keyName = "hideDelayTicks",
		name = "Hide Delay (Ticks)",
		description = "Number of game ticks to wait before hiding a tile after the player is near it (1 tick = 0.6 seconds)"
	)
	default int hideDelayTicks()
	{
		return 5;
	}
	
	@ConfigItem(
		keyName = "maxVisibleTiles",
		name = "Max Visible Tiles",
		description = "Maximum number of route tiles visible at a time"
	)
	default int maxVisibleTiles()
	{
		return 30;
	}
}
