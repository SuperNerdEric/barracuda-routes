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

	private BarracudaRoutesPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		RouteManager.initDefaultRoutes();
		panel = new BarracudaRoutesPanel(this);
		BufferedImage icon = loadIcon();
		navButton = NavigationButton.builder()
				.tooltip("Barracuda Routes")
				.icon(icon)
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

	private BufferedImage loadIcon()
	{
		try (InputStream in = getClass().getResourceAsStream("/barracuda_icon.png"))
		{
			if (in != null)
			{
				return ImageIO.read(in);
			}
		}
		catch (IOException ignored)
		{
		}
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		return img;
	}

	public Client getClient()
	{
		return client;
	}
}
