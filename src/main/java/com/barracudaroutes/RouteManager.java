package com.barracudaroutes;

import java.util.ArrayList;
import java.util.List;

public class RouteManager
{
    private static final List<Route> routes = new ArrayList<>();
    private static Route active = null;

    public static void initDefaultRoutes()
    {
        Route r1 = new Route("Prepackaged: Example loop");
        r1.addPoint(new RoutePoint(3001, 3230, 0));
        r1.addPoint(new RoutePoint(3003, 3230, 0));
        r1.addPoint(new RoutePoint(3003, 3232, 0));
        r1.addPoint(new RoutePoint(3001, 3232, 0));
        routes.add(r1);
    }

    public static List<Route> getAllRoutes()
    {
        return new ArrayList<>(routes);
    }

    public static void addRoute(Route r)
    {
        routes.add(r);
    }

    public static void removeRoute(Route r)
    {
        routes.remove(r);
        if (active == r)
        {
            active = null;
        }
    }

    public static Route getActiveRoute()
    {
        return active;
    }

    public static void setActiveRoute(Route r)
    {
        active = r;
    }
}
