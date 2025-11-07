package com.barracudaroutes;

import java.util.ArrayList;
import java.util.List;

public class Route
{
    private final String name;
    private final List<RoutePoint> points = new ArrayList<>();

    public Route(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public List<RoutePoint> getPoints()
    {
        return points;
    }

    public void addPoint(RoutePoint p)
    {
        points.add(p);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
