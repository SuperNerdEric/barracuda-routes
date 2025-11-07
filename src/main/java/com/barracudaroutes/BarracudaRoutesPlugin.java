package com.barracudaroutes;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

@PluginDescriptor(
		name = "Barracuda Routes",
		description = "Create and share Barracuda Trials routes",
		tags = {"barracuda", "routes", "trials", "path"}
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

	private BarracudaRoutesPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		RouteManager.initDefaultRoutes();
		panel = new BarracudaRoutesPanel(this, colorPickerManager);
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
	}

	public Client getClient()
	{
		return client;
	}
}
