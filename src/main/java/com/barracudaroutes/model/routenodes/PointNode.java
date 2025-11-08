package com.barracudaroutes.model.routenodes;

/**
 * Represents a point in the route
 */
public class PointNode implements RouteNode
{
    private int x;
    private int y;
    private int plane;

    public PointNode(int x, int y, int plane)
    {
        this.x = x;
        this.y = y;
        this.plane = plane;
    }

    @Override
    public String getType()
    {
        return "point";
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
}

