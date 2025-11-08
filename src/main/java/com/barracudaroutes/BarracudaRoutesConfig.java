package com.barracudaroutes;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("barracudaroutes")
public interface BarracudaRoutesConfig extends Config
{
	@ConfigSection(
		position = 0,
		name = "Route visibility",
		description = "Settings for automatic route tile visibility"
	)
	String routeVisibilitySection = "routeVisibilitySection";

	@ConfigSection(
		position = 1,
		name = "Route appearance",
		description = "Visual settings for route lines"
	)
	String routeAppearanceSection = "routeAppearanceSection";

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

	@Range(
		min = 1,
		max = 10
	)
	@ConfigItem(
		keyName = "routeLineWidth",
		name = "Route Line Width",
		description = "Pixel width of the route lines in the overlay",
		section = routeAppearanceSection,
		position = 1
	)
	default int routeLineWidth()
	{
		return 1;
	}

	@Range(
		max = 100
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "routeLineOpacity",
		name = "Route Line Opacity",
		description = "Opacity of the route lines",
		section = routeAppearanceSection,
		position = 2
	)
	default int routeLineOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "showRouteDirectionArrows",
		name = "Show Direction Arrows",
		description = "Draw arrows on every other tile to highlight the route direction",
		section = routeAppearanceSection,
		position = 3
	)
	default boolean showRouteDirectionArrows()
	{
		return false;
	}
}
