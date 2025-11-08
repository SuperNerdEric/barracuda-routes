package com.barracudaroutes.ui;

import javax.inject.Inject;

import com.barracudaroutes.BarracudaRoutesConfig;
import com.barracudaroutes.managers.RouteManager;
import com.barracudaroutes.managers.RouteVisibilityManager;
import com.barracudaroutes.model.routenodes.LapDividerNode;
import com.barracudaroutes.model.routenodes.PointNode;
import com.barracudaroutes.model.Route;
import com.barracudaroutes.model.routenodes.RouteNode;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Perspective;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class RouteOverlay extends Overlay
{
    private final Client client;
    private final RouteVisibilityManager visibilityManager;
    private final RouteManager routeManager;
    private final BarracudaRoutesConfig config;

    @Inject
    public RouteOverlay(Client client, RouteVisibilityManager visibilityManager, RouteManager routeManager, BarracudaRoutesConfig config)
    {
        this.client = client;
        this.visibilityManager = visibilityManager;
        this.routeManager = routeManager;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    // Default color scheme for laps (repeating)
    private static final Color[] DEFAULT_LAP_COLORS = {
        Color.RED,      // Lap 1
        Color.BLUE,     // Lap 2
        Color.GREEN,    // Lap 3
        Color.WHITE,    // Lap 4
        Color.BLACK,    // Lap 5
        new Color(128, 0, 128), // Lap 6 - Purple
        new Color(255, 165, 0)  // Lap 7 - Orange
    };
    
    private Color getLapColor(Route route, int lap)
    {
        // Check for custom color first
        Color customColor = route.getLapColor(lap);
        if (customColor != null)
        {
            return customColor;
        }
        
        // Use default color scheme (1-indexed, cycles through)
        int colorIndex = (lap - 1) % DEFAULT_LAP_COLORS.length;
        return DEFAULT_LAP_COLORS[colorIndex];
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        Route active = routeManager.getActiveRoute();
        if (active == null)
        {
            return null;
        }
        List<RouteNode> routeNodes = active.getRoute();
        if (routeNodes.isEmpty())
        {
            return null;
        }
        
        // Get visible tile indices from visibility manager
        Set<Integer> visibleIndices = visibilityManager.getVisibleTileIndices();

        Stroke originalStroke = g.getStroke();
        float lineWidth = Math.min(10f, Math.max(1f, config.routeLineWidth()));
        g.setStroke(new BasicStroke(lineWidth));
        int lineOpacityPercent = Math.max(0, Math.min(100, config.routeLineOpacity()));
        float opacityScale = lineOpacityPercent / 100f;
        Point prev = null;
        int currentLap = 1;
        
        for (int i = 0; i < routeNodes.size(); i++)
        {
            RouteNode node = routeNodes.get(i);
            
            if (node instanceof LapDividerNode)
            {
                // Update current lap
                currentLap = ((LapDividerNode) node).getLapNumber();
                prev = null; // Break the line at lap dividers
                continue;
            }
            else if (node instanceof PointNode)
            {
                // Skip if tile is not visible
                if (!visibleIndices.contains(i))
                {
                    prev = null; // Break the line when skipping tiles
                    continue;
                }
                
                PointNode point = (PointNode) node;
                WorldPoint wp = new WorldPoint(point.getX(), point.getY(), point.getPlane());
                LocalPoint lp = LocalPoint.fromWorld(client, wp.getX(), wp.getY());
                if (lp == null)
                {
                    prev = null;
                    continue;
                }
                net.runelite.api.Point canvas = Perspective.localToCanvas(client, lp, client.getPlane());
                if (canvas == null)
                {
                    prev = null;
                    continue;
                }
                
                // Set color for this point's lap (used for the line segment from prev to this point)
                Color lapColor = getLapColor(active, currentLap);
                int combinedAlpha = Math.round(lapColor.getAlpha() * opacityScale);
                Color lineColor = new Color(lapColor.getRed(), lapColor.getGreen(), lapColor.getBlue(), combinedAlpha);
                g.setColor(lineColor);
                
                if (prev != null)
                {
                    g.drawLine(prev.getX(), prev.getY(), canvas.getX(), canvas.getY());
                }
                prev = canvas;
            }
        }
        
        // Highlight selected tile if one is selected
        PointNode selectedTile = routeManager.getSelectedTile();
        if (selectedTile != null)
        {
            WorldPoint wp = new WorldPoint(selectedTile.getX(), selectedTile.getY(), selectedTile.getPlane());
            LocalPoint lp = LocalPoint.fromWorld(client, wp.getX(), wp.getY());
            if (lp != null)
            {
                // Get the polygon representing the entire tile
                Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
                if (tilePoly != null)
                {
                    // Draw filled tile highlight
                    g.setColor(new Color(255, 255, 0, 100)); // Yellow with transparency
                    g.fillPolygon(tilePoly);
                    
                    // Draw tile border
                    g.setColor(new Color(255, 255, 0, 255)); // Solid yellow
                    g.setStroke(new BasicStroke(2.0f));
                    g.drawPolygon(tilePoly);
                }
            }
        }
        g.setStroke(originalStroke);
        return null;
    }
}
