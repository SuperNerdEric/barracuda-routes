package com.barracudaroutes;

public class RoutePoint
{
    private int x;
    private int y;
    private int plane;
    private int lap;

    public RoutePoint(int x, int y, int plane)
    {
        this(x, y, plane, 1);
    }

    public RoutePoint(int x, int y, int plane, int lap)
    {
        this.x = x;
        this.y = y;
        this.plane = plane;
        this.lap = lap;
    }

    public int getX()
    {
        return x;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getY()
    {
        return y;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public int getPlane()
    {
        return plane;
    }

    public void setPlane(int plane)
    {
        this.plane = plane;
    }

    public int getLap()
    {
        return lap;
    }

    public void setLap(int lap)
    {
        this.lap = lap;
    }
}