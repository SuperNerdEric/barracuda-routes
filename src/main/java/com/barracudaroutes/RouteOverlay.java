package com.barracudaroutes;

import javax.inject.Inject;

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

    @Inject
    public RouteOverlay(Client client, RouteVisibilityManager visibilityManager)
    {
        this.client = client;
        this.visibilityManager = visibilityManager;
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
        Route active = RouteManager.getActiveRoute();
        if (active == null)
        {
            return null;
        }
        List<RoutePoint> pts = active.getPoints();
        if (pts.isEmpty())
        {
            return null;
        }
        
        // Get visible tile indices from visibility manager
        Set<Integer> visibleIndices = visibilityManager.getVisibleTileIndices();
        
        g.setStroke(new BasicStroke(3.0f));
        Point prev = null;
        
        for (int i = 0; i < pts.size(); i++)
        {
            // Skip if tile is not visible
            if (!visibleIndices.contains(i))
            {
                prev = null; // Break the line when skipping tiles
                continue;
            }
            
            RoutePoint rp = pts.get(i);
            WorldPoint wp = new WorldPoint(rp.getX(), rp.getY(), rp.getPlane());
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
            
            int currentLap = rp.getLap();
            
            // Set color for this point's lap (used for the line segment from prev to this point)
            Color lapColor = getLapColor(active, currentLap);
            g.setColor(lapColor);
            
            if (prev != null)
            {
                g.drawLine(prev.getX(), prev.getY(), canvas.getX(), canvas.getY());
            }
            prev = canvas;
        }
        
        // Highlight selected tile if one is selected
        RoutePoint selectedTile = RouteManager.getSelectedTile();
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
        
        return null;
    }
}
