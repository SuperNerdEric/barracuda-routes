package com.barracudaroutes;

import com.barracudaroutes.managers.RouteImportExportManager;
import com.barracudaroutes.managers.RouteManager;
import com.barracudaroutes.managers.RouteVisibilityManager;
import com.barracudaroutes.ui.BarracudaRoutesPanel;
import com.barracudaroutes.ui.RouteOverlay;
import com.google.inject.Provides;
import java.util.function.Consumer;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;


@PluginDescriptor(
		name = "Barracuda Routes",
		description = "Create and share Barracuda Trials routes",
		tags = {"barracuda", "trials", "sailing", "routes", "path", "race"}
)
public class BarracudaRoutesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RouteOverlay routeOverlay;

	@Inject
	private net.runelite.client.ui.components.colorpicker.ColorPickerManager colorPickerManager;

	@Inject
	private RouteVisibilityManager routeVisibilityManager;
	
	@Inject
	private RouteManager routeManager;
	
	@Inject
	private RouteImportExportManager routeImportExportManager;
	
	private BarracudaRoutesPanel panel;
	private NavigationButton navButton;
	private static final int MANUAL_TILE_MENU_IDENTIFIER = 0xBAAA;
	private static final String MANUAL_TILE_MENU_OPTION = "Add tile to route";
	private Consumer<WorldPoint> manualTileSelectionConsumer;
	
	@Provides
	BarracudaRoutesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BarracudaRoutesConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		// Load routes from disk
		routeManager.loadRoutes();
		panel = new BarracudaRoutesPanel(this, colorPickerManager, routeManager, routeImportExportManager);
		navButton = NavigationButton.builder()
				.tooltip("Barracuda Routes")
				.icon(ImageUtil.loadImageResource(getClass(), "/barracuda_icon.png"))
				.priority(7)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(routeOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(routeOverlay);
		clientToolbar.removeNavigation(navButton);
		panel = null;
		clearManualTileSelectionConsumer();
	}

	public Client getClient()
	{
		return client;
	}

	public void setManualTileSelectionConsumer(Consumer<WorldPoint> consumer)
	{
		manualTileSelectionConsumer = consumer;
	}

	public void clearManualTileSelectionConsumer()
	{
		manualTileSelectionConsumer = null;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		// Update route visibility manager on each game tick
		// Only works when not in edit mode (checked inside the manager)
		routeVisibilityManager.update();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!routeManager.isInEditMode() || manualTileSelectionConsumer == null)
		{
			return;
		}

		MenuEntry baseEntry = event.getMenuEntry();
		if (baseEntry == null)
		{
			return;
		}

		if (event.getType() != MenuAction.WALK.getId())
		{
			return;
		}

		// Avoid duplicating entries
		for (MenuEntry entry : client.getMenuEntries())
		{
			if (entry.getType() == MenuAction.RUNELITE && entry.getIdentifier() == MANUAL_TILE_MENU_IDENTIFIER)
			{
				return;
			}
		}

		client.createMenuEntry(0)
			.setOption(MANUAL_TILE_MENU_OPTION)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.setIdentifier(MANUAL_TILE_MENU_IDENTIFIER)
			.setParam0(baseEntry.getParam0())
			.setParam1(baseEntry.getParam1())
			.setDeprioritized(false)
			.setForceLeftClick(true)
			.onClick(this::handleManualTileMenuClick);
	}

	private void handleManualTileMenuClick(MenuEntry entry)
	{
		Consumer<WorldPoint> consumer = manualTileSelectionConsumer;
		if (!routeManager.isInEditMode() || consumer == null)
		{
			return;
		}

		WorldPoint worldPoint = null;

		Tile selectedTile = client.getSelectedSceneTile();
		if (selectedTile != null)
		{
			worldPoint = selectedTile.getWorldLocation();
		}

		if (worldPoint == null)
		{
			int sceneX = entry.getParam1();
			int sceneY = entry.getParam0();
			if (sceneX >= 0 && sceneY >= 0)
			{
				worldPoint = WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
			}
		}
		if (worldPoint == null)
		{
			return;
		}

		consumer.accept(worldPoint);
	}
}
