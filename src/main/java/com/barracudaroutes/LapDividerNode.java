package com.barracudaroutes;

import java.awt.Color;

/**
 * Represents a lap divider in the route
 */
public class LapDividerNode implements RouteNode
{
    private int lapNumber;
    private Color color; // Color for this lap (null means use default)

    public LapDividerNode(int lapNumber)
    {
        this.lapNumber = lapNumber;
        this.color = null; // Default to null (use default color)
    }

    public LapDividerNode(int lapNumber, Color color)
    {
        this.lapNumber = lapNumber;
        this.color = color;
    }

    @Override
    public String getType()
    {
        return "lapDivider";
    }

    public int getLapNumber()
    {
        return lapNumber;
    }

    public void setLapNumber(int lapNumber)
    {
        this.lapNumber = lapNumber;
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }
}

