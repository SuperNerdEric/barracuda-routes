package com.barracudaroutes.managers;

import com.barracudaroutes.model.Route;
import com.barracudaroutes.model.routenodes.LapDividerNode;
import com.barracudaroutes.model.routenodes.PointNode;
import com.barracudaroutes.model.routenodes.RouteNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.UUID;

/**
 * Manages import and export of routes to/from JSON format.
 * Handles clipboard and file-based import/export operations.
 */
@Slf4j
@Singleton
public class RouteImportExportManager
{
    private final Gson gson;
    
    // Custom serializer/deserializer for Color
    private static class ColorAdapter implements JsonSerializer<Color>, JsonDeserializer<Color>
    {
        @Override
        public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("r", src.getRed());
            obj.addProperty("g", src.getGreen());
            obj.addProperty("b", src.getBlue());
            obj.addProperty("a", src.getAlpha());
            return obj;
        }
        
        @Override
        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException
        {
            JsonObject obj = json.getAsJsonObject();
            int r = obj.get("r").getAsInt();
            int g = obj.get("g").getAsInt();
            int b = obj.get("b").getAsInt();
            int a = obj.has("a") ? obj.get("a").getAsInt() : 255;
            return new Color(r, g, b, a);
        }
    }
    
    // Custom serializer/deserializer for RouteNode
    private static class RouteNodeAdapter implements JsonSerializer<RouteNode>, JsonDeserializer<RouteNode>
    {
        private final ColorAdapter colorAdapter = new ColorAdapter();
        
        @Override
        public JsonElement serialize(RouteNode src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getType());
            
            if (src instanceof PointNode)
            {
                PointNode point = (PointNode) src;
                obj.addProperty("x", point.getX());
                obj.addProperty("y", point.getY());
                obj.addProperty("plane", point.getPlane());
            }
            else if (src instanceof LapDividerNode)
            {
                LapDividerNode lapDivider = (LapDividerNode) src;
                obj.addProperty("lapNumber", lapDivider.getLapNumber());
                if (lapDivider.getColor() != null)
                {
                    obj.add("color", colorAdapter.serialize(lapDivider.getColor(), Color.class, context));
                }
            }
            
            return obj;
        }
        
        @Override
        public RouteNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException
        {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();
            
            if ("point".equals(type))
            {
                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();
                int plane = obj.get("plane").getAsInt();
                return new PointNode(x, y, plane);
            }
            else if ("lapDivider".equals(type))
            {
                int lapNumber = obj.get("lapNumber").getAsInt();
                Color color = null;
                if (obj.has("color"))
                {
                    color = colorAdapter.deserialize(obj.get("color"), Color.class, context);
                }
                return new LapDividerNode(lapNumber, color);
            }
            
            throw new JsonParseException("Unknown route node type: " + type);
        }
    }
    
    @Inject
    public RouteImportExportManager()
    {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeAdapter(Color.class, new ColorAdapter());
        builder.registerTypeAdapter(RouteNode.class, new RouteNodeAdapter());
        this.gson = builder.create();
    }
    
    /**
     * Export a route to JSON string
     */
    public String exportRouteToJson(Route route)
    {
        if (route == null)
        {
            log.warn("Attempted to export null route");
            return null;
        }
        
        try
        {
            return gson.toJson(route);
        }
        catch (Exception e)
        {
            log.error("Failed to export route to JSON", e);
            return null;
        }
    }
    
    /**
     * Export a route to clipboard
     */
    public boolean exportRouteToClipboard(Route route)
    {
        String json = exportRouteToJson(route);
        if (json == null)
        {
            return false;
        }
        
        try
        {
            StringSelection contents = new StringSelection(json);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);
            return true;
        }
        catch (Exception e)
        {
            log.error("Failed to copy route to clipboard", e);
            return false;
        }
    }
    
    /**
     * Import a route from JSON string
     */
    public Route importRouteFromJson(String json)
    {
        if (json == null || json.trim().isEmpty())
        {
            log.warn("Attempted to import route from empty JSON");
            return null;
        }
        
        try
        {
            Route route = gson.fromJson(json, Route.class);
            
            // Validate the imported route
            if (route == null || route.getName() == null || route.getRoute() == null)
            {
                log.warn("Imported route is missing required fields");
                return null;
            }
            
            // Generate a new UUID for the imported route to avoid conflicts
            route.setFileUuid(UUID.randomUUID());
            
            return route;
        }
        catch (Exception e)
        {
            log.error("Failed to import route from JSON", e);
            return null;
        }
    }
    
    /**
     * Import a route from a file
     */
    public Route importRouteFromFile(File file)
    {
        if (file == null || !file.exists() || !file.isFile())
        {
            log.warn("Invalid file for route import: {}", file);
            return null;
        }
        
        try (FileReader reader = new FileReader(file))
        {
            Route route = gson.fromJson(reader, Route.class);
            
            // Validate the imported route
            if (route == null || route.getName() == null || route.getRoute() == null)
            {
                log.warn("Imported route from file is missing required fields");
                return null;
            }
            
            // Generate a new UUID for the imported route to avoid conflicts
            route.setFileUuid(UUID.randomUUID());
            
            return route;
        }
        catch (IOException e)
        {
            log.error("Failed to read route file: {}", file, e);
            return null;
        }
        catch (Exception e)
        {
            log.error("Failed to import route from file: {}", file, e);
            return null;
        }
    }
}

