package com.barracudaroutes;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Route
{
    private String name;
    private String description;
    private String trialName;
    private final List<RouteNode> route = new ArrayList<>();
    private UUID fileUuid; // UUID used in filename for persistence

    public Route(String name, String description, String trialName)
    {
        this.name = name;
        this.description = description;
        this.trialName = trialName;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getTrialName()
    {
        return trialName;
    }

    public void setTrialName(String trialName)
    {
        this.trialName = trialName;
    }

    public List<RouteNode> getRoute()
    {
        return route;
    }

    public void addNode(RouteNode node)
    {
        route.add(node);
    }
    
    /**
     * Get all point nodes from the route (excluding lap dividers)
     */
    public List<PointNode> getPointNodes()
    {
        List<PointNode> points = new ArrayList<>();
        for (RouteNode node : route)
        {
            if (node instanceof PointNode)
            {
                points.add((PointNode) node);
            }
        }
        return points;
    }
    
    /**
     * Get the current lap number at a given index in the route
     */
    public int getLapAt(int index)
    {
        int currentLap = 1;
        for (int i = 0; i <= index && i < route.size(); i++)
        {
            RouteNode node = route.get(i);
            if (node instanceof LapDividerNode)
            {
                currentLap = ((LapDividerNode) node).getLapNumber();
            }
        }
        return currentLap;
    }
    
    /**
     * Get the color for a lap divider, or null if using default
     */
    public Color getLapColor(int lapNumber)
    {
        for (RouteNode node : route)
        {
            if (node instanceof LapDividerNode)
            {
                LapDividerNode lapDivider = (LapDividerNode) node;
                if (lapDivider.getLapNumber() == lapNumber)
                {
                    return lapDivider.getColor();
                }
            }
        }
        return null;
    }
    
    /**
     * Set the color for a lap divider
     */
    public void setLapColor(int lapNumber, Color color)
    {
        for (RouteNode node : route)
        {
            if (node instanceof LapDividerNode)
            {
                LapDividerNode lapDivider = (LapDividerNode) node;
                if (lapDivider.getLapNumber() == lapNumber)
                {
                    lapDivider.setColor(color);
                    return;
                }
            }
        }
    }
    
    public UUID getFileUuid()
    {
        return fileUuid;
    }
    
    public void setFileUuid(UUID fileUuid)
    {
        this.fileUuid = fileUuid;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
