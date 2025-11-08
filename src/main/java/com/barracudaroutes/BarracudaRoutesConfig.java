package com.barracudaroutes;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("barracudaroutes")
public interface BarracudaRoutesConfig extends Config
{
	@ConfigSection(
		position = 0,
		name = "Route visibility",
		description = "Settings for automatic route tile visibility"
	)
	String routeVisibilitySection = "routeVisibilitySection";

	@ConfigItem(
		keyName = "hideDistance",
		name = "Hide Distance",
		description = "Distance in tiles at which route tiles will hide when the player is near them",
		section = routeVisibilitySection,
		position = 1
	)
	default int hideDistance()
	{
		return 5;
	}
	
	@ConfigItem(
		keyName = "hideDelayTicks",
		name = "Hide Delay (Ticks)",
		description = "Number of game ticks to wait before hiding a tile after the player is near it (1 tick = 0.6 seconds)",
		section = routeVisibilitySection,
		position = 2
	)
	default int hideDelayTicks()
	{
		return 5;
	}
	
	@ConfigItem(
		keyName = "maxVisibleTiles",
		name = "Max Visible Tiles",
		description = "Maximum number of route tiles visible at a time",
		section = routeVisibilitySection,
		position = 3
	)
	default int maxVisibleTiles()
	{
		return 30;
	}
}
