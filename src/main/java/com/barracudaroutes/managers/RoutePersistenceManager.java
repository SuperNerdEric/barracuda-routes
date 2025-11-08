package com.barracudaroutes.managers;

import com.barracudaroutes.model.routenodes.LapDividerNode;
import com.barracudaroutes.model.routenodes.PointNode;
import com.barracudaroutes.model.Route;
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

import net.runelite.client.RuneLite;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages persistence of routes to JSON files in the barracuda-routes directory.
 * Routes are organized by trial in separate subdirectories.
 */
@Slf4j
@Singleton
public class RoutePersistenceManager
{
    private static final String ROUTES_DIR = "barracuda-routes";
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
    public RoutePersistenceManager()
    {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeAdapter(Color.class, new ColorAdapter());
        builder.registerTypeAdapter(RouteNode.class, new RouteNodeAdapter());
        this.gson = builder.create();
    }
    
    /**
     * Get the base routes directory (inside RuneLite directory)
     */
    private Path getRoutesDir()
    {
        return RuneLite.RUNELITE_DIR.toPath().resolve(ROUTES_DIR);
    }
    
    /**
     * Get the directory for a specific trial
     */
    private Path getTrialDir(String trialName)
    {
        return getRoutesDir().resolve(sanitizeFileName(trialName));
    }
    
    /**
     * Sanitize a string to be used as a file or directory name
     * Replaces spaces and special characters with underscores
     */
    private String sanitizeFileName(String name)
    {
        if (name == null || name.isEmpty())
        {
            return "unknown";
        }
        // Replace spaces and special characters with underscores
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * Generate a filename for a route: <Route_Name>_<UUID>.json
     */
    private String generateFileName(String routeName, UUID uuid)
    {
        String sanitized = sanitizeFileName(routeName);
        return sanitized + "_" + uuid.toString() + ".json";
    }
    
    /**
     * Load all routes from the barracuda-routes directory
     */
    public List<Route> loadAllRoutes()
    {
        List<Route> routes = new ArrayList<>();
        Path routesDir = getRoutesDir();
        
        if (!Files.exists(routesDir))
        {
            log.debug("Routes directory does not exist: {}", routesDir);
            return routes;
        }
        
        try
        {
            // Iterate through trial directories
            Files.list(routesDir).forEach(trialPath -> {
                if (Files.isDirectory(trialPath))
                {
                    try
                    {
                        // Load all JSON files in this trial directory
                        Files.list(trialPath)
                                .filter(path -> path.toString().endsWith(".json"))
                                .forEach(jsonPath -> {
                                    try
                                    {
                                        Route route = loadRouteFromFile(jsonPath.toFile());
                                        if (route != null)
                                        {
                                            routes.add(route);
                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        log.error("Failed to load route from file: {}", jsonPath, e);
                                    }
                                });
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to list files in trial directory: {}", trialPath, e);
                    }
                }
            });
        }
        catch (IOException e)
        {
            log.error("Failed to load routes from directory: {}", routesDir, e);
        }
        
        log.info("Loaded {} routes from {}", routes.size(), routesDir);
        return routes;
    }
    
    /**
     * Load a single route from a JSON file
     */
    private Route loadRouteFromFile(File file)
    {
        try (FileReader reader = new FileReader(file))
        {
            Route route = gson.fromJson(reader, Route.class);
            
            // Extract UUID from filename if not already set
            if (route.getFileUuid() == null)
            {
                String fileName = file.getName();
                // Extract UUID from filename: <Route_Name>_<UUID>.json
                int lastUnderscore = fileName.lastIndexOf('_');
                int lastDot = fileName.lastIndexOf('.');
                if (lastUnderscore > 0 && lastDot > lastUnderscore)
                {
                    String uuidStr = fileName.substring(lastUnderscore + 1, lastDot);
                    try
                    {
                        UUID uuid = UUID.fromString(uuidStr);
                        route.setFileUuid(uuid);
                    }
                    catch (IllegalArgumentException e)
                    {
                        log.warn("Could not parse UUID from filename: {}", fileName);
                    }
                }
            }
            
            return route;
        }
        catch (Exception e)
        {
            log.error("Failed to load route from file: {}", file, e);
            return null;
        }
    }
    
    /**
     * Save a route to a JSON file
     * Only saves if the route has a name set
     */
    public void saveRoute(Route route)
    {
        if (route == null)
        {
            log.warn("Attempted to save null route");
            return;
        }
        
        String routeName = route.getName();
        if (routeName == null || routeName.trim().isEmpty())
        {
            log.debug("Skipping save for route without name");
            return;
        }
        
        String trialName = route.getTrialName();
        if (trialName == null || trialName.isEmpty())
        {
            trialName = "The Tempor Tantrum"; // Default trial
        }
        
        // Ensure UUID is set
        if (route.getFileUuid() == null)
        {
            route.setFileUuid(UUID.randomUUID());
        }
        
        Path trialDir = getTrialDir(trialName);
        String fileName = generateFileName(route.getName(), route.getFileUuid());
        Path filePath = trialDir.resolve(fileName);
        
        try
        {
            // Create trial directory if it doesn't exist
            Files.createDirectories(trialDir);
            
            // Write route to JSON file
            try (FileWriter writer = new FileWriter(filePath.toFile()))
            {
                gson.toJson(route, writer);
            }
            
            log.debug("Saved route to: {}", filePath);
        }
        catch (IOException e)
        {
            log.error("Failed to save route to file: {}", filePath, e);
        }
    }
    
    /**
     * Delete a route's JSON file
     * Only deletes if the route has a name set
     */
    public void deleteRoute(Route route)
    {
        if (route == null)
        {
            log.warn("Attempted to delete null route");
            return;
        }
        
        String routeName = route.getName();
        if (routeName == null || routeName.trim().isEmpty())
        {
            log.debug("Skipping delete for route without name");
            return;
        }
        
        // If route has no UUID, it was never saved, so nothing to delete
        if (route.getFileUuid() == null)
        {
            log.debug("Route has no UUID, skipping delete");
            return;
        }
        
        String trialName = route.getTrialName();
        if (trialName == null || trialName.isEmpty())
        {
            trialName = "The Tempor Tantrum"; // Default trial
        }
        
        Path trialDir = getTrialDir(trialName);
        String fileName = generateFileName(routeName, route.getFileUuid());
        Path filePath = trialDir.resolve(fileName);
        
        try
        {
            if (Files.exists(filePath))
            {
                Files.delete(filePath);
                log.debug("Deleted route file: {}", filePath);
            }
        }
        catch (IOException e)
        {
            log.error("Failed to delete route file: {}", filePath, e);
        }
    }
}

