package com.barracudaroutes;

import javax.inject.Inject;

import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import com.barracudaroutes.Route;
import net.runelite.api.Perspective;

import java.awt.*;
import java.util.List;

public class RouteOverlay extends Overlay
{
    private final Client client;

    @Inject
    public RouteOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
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
        if (pts.size() < 2)
        {
            return null;
        }
        g.setStroke(new BasicStroke(3.0f));
        g.setColor(Color.BLUE);
        Point prev = null;
        for (RoutePoint rp : pts)
        {
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
            if (prev != null)
            {
                g.drawLine(prev.getX(), prev.getY(), canvas.getX(), canvas.getY());
            }
            prev = canvas;
        }
        return null;
    }
}
