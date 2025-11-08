package com.barracudaroutes;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Route
{
    private String name;
    private String description;
    private String trialName;
    private final List<RoutePoint> points = new ArrayList<>();
    private final Map<Integer, Color> lapColors = new HashMap<>(); // Custom colors per lap
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

    public List<RoutePoint> getPoints()
    {
        return points;
    }

    public void addPoint(RoutePoint p)
    {
        points.add(p);
    }

    public Map<Integer, Color> getLapColors()
    {
        return lapColors;
    }
    
    public void setLapColor(int lap, Color color)
    {
        if (color == null)
        {
            lapColors.remove(lap);
        }
        else
        {
            lapColors.put(lap, color);
        }
    }
    
    public Color getLapColor(int lap)
    {
        return lapColors.get(lap);
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
