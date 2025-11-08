package com.barracudaroutes;

import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

/**
 * Manages automatic hiding of route tiles when the player is near them.
 * Uses a queue-based approach where tiles are processed sequentially.
 * Features:
 * - Hides tiles when player is within 5 tiles (with 5 tick delay)
 * - Only shows next 30 visible tiles at a time
 * - Tiles are processed sequentially in queue order
 */
@Singleton
public class RouteVisibilityManager
{
    private final Client client;
    private final RouteManager routeManager;
    private final ConfigManager configManager;
    
    // Configuration
    private boolean enabled = true;
    
    // Queue-based state
    private Route currentRoute = null;
    private final Queue<ManagedTile> tileQueue = new LinkedList<>();
    
    /**
     * Represents a tile in the visibility queue with all its state
     */
    private static class ManagedTile
    {
        final int index; // Tile index in the route
        int ticksRemaining; // Ticks remaining until hide (-1 if not pending hide, 0 if hidden)
        boolean isHidden; // Whether this tile has been hidden
        
        ManagedTile(int index)
        {
            this.index = index;
            this.ticksRemaining = -1; // Not pending hide
            this.isHidden = false;
        }
        
        /**
         * Check if this tile is pending hide (countdown active)
         */
        boolean isPendingHide()
        {
            return ticksRemaining > 0;
        }
        
        /**
         * Check if this tile is visible (not hidden - pending hide tiles are still visible during countdown)
         */
        boolean isVisible()
        {
            return !isHidden;
        }
        
        /**
         * Mark this tile as pending hide with countdown
         */
        void markPendingHide(int delayTicks)
        {
            ticksRemaining = delayTicks;
        }
        
        /**
         * Decrement countdown and hide if it reaches 0
         */
        boolean decrementAndCheckHide()
        {
            if (ticksRemaining > 0)
            {
                ticksRemaining--;
                if (ticksRemaining == 0)
                {
                    isHidden = true;
                    return true; // Just became hidden
                }
            }
            return false; // Not hidden yet
        }
    }
    
    @Inject
    public RouteVisibilityManager(Client client, RouteManager routeManager, ConfigManager configManager)
    {
        this.client = client;
        this.routeManager = routeManager;
        this.configManager = configManager;
    }
    
    private BarracudaRoutesConfig getConfig()
    {
        return configManager.getConfig(BarracudaRoutesConfig.class);
    }
    
    /**
     * Enable or disable the visibility manager
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        if (!enabled)
        {
            // Reset state when disabled
            reset();
        }
    }
    
    /**
     * Check if the visibility manager is enabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }
    
    /**
     * Update the visibility state based on current player position
     * Should be called regularly (e.g., on game tick)
     */
    public void update()
    {
        if (!enabled)
        {
            return;
        }
        
        // Don't hide tiles when in edit mode
        if (routeManager.isInEditMode())
        {
            return;
        }
        
        Route activeRoute = routeManager.getActiveRoute();
        
        // Reset if route changed (different route object)
        if (activeRoute != currentRoute && activeRoute != null)
        {
            reset();
            currentRoute = activeRoute;
            initializeQueue(activeRoute);
        }
        
        // Update currentRoute reference if it's null but we have an active route
        if (currentRoute == null && activeRoute != null)
        {
            currentRoute = activeRoute;
            initializeQueue(activeRoute);
        }
        
        if (activeRoute == null || activeRoute.getPoints().isEmpty())
        {
            return;
        }
        
        // Get current player position
        if (client.getLocalPlayer() == null)
        {
            return;
        }
        
        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        if (playerPos == null)
        {
            return;
        }
        
        // Decrement countdowns for pending hide tiles
        decrementPendingHides();
        
        // Process queue - check next tile that isn't pending hide
        processQueue(playerPos);
        
        // Check if we've reached the end - if so, reset queue back to full
        if (isQueueAtEnd())
        {
            resetQueue();
        }
    }
    
    /**
     * Initialize the queue with the first MAX_VISIBLE_TILES tiles
     */
    private void initializeQueue(Route route)
    {
        tileQueue.clear();
        
        if (route == null || route.getPoints().isEmpty())
        {
            return;
        }
        
        List<RoutePoint> points = route.getPoints();
        int tilesToAdd = Math.min(getConfig().maxVisibleTiles(), points.size());
        
        for (int i = 0; i < tilesToAdd; i++)
        {
            tileQueue.add(new ManagedTile(i));
        }
    }
    
    /**
     * Reset the queue back to full (first MAX_VISIBLE_TILES tiles)
     */
    private void resetQueue()
    {
        if (currentRoute == null || currentRoute.getPoints().isEmpty())
        {
            return;
        }
        
        initializeQueue(currentRoute);
    }
    
    /**
     * Check if we've reached the end of the queue (all tiles are hidden or beyond max visible)
     */
    private boolean isQueueAtEnd()
    {
        if (currentRoute == null || currentRoute.getPoints().isEmpty())
        {
            return false;
        }
        
        List<RoutePoint> points = currentRoute.getPoints();
        
        // Check if all tiles in queue are hidden
        boolean allHidden = true;
        for (ManagedTile tile : tileQueue)
        {
            if (!tile.isHidden)
            {
                allHidden = false;
                break;
            }
        }
        
        if (!allHidden)
        {
            return false;
        }
        
        // Check if we've processed all tiles
        int maxIndex = -1;
        for (ManagedTile tile : tileQueue)
        {
            if (tile.index > maxIndex)
            {
                maxIndex = tile.index;
            }
        }
        
        return maxIndex >= points.size() - 1;
    }
    
    /**
     * Decrement countdowns for pending hide tiles and hide when countdown reaches 0
     */
    private void decrementPendingHides()
    {
        for (ManagedTile tile : tileQueue)
        {
            tile.decrementAndCheckHide();
        }
    }
    
    /**
     * Process the queue - check the next tile that isn't pending hide
     */
    private void processQueue(WorldPoint playerPos)
    {
        if (currentRoute == null || currentRoute.getPoints().isEmpty())
        {
            return;
        }
        
        List<RoutePoint> points = currentRoute.getPoints();
        
        // Find the next tile in queue that isn't pending hide and isn't hidden
        ManagedTile nextTile = null;
        Iterator<ManagedTile> queueIterator = tileQueue.iterator();
        
        while (queueIterator.hasNext())
        {
            ManagedTile tile = queueIterator.next();
            
            // Skip if hidden
            if (tile.isHidden)
            {
                continue;
            }
            
            // Skip if already pending hide
            if (tile.isPendingHide())
            {
                continue;
            }
            
            // Found the next tile to check
            nextTile = tile;
            break;
        }
        
        if (nextTile == null)
        {
            // No tiles to check in queue, try to add more if we haven't reached the end
            int maxIndex = getMaxQueueIndex();
            if (tileQueue.isEmpty() || maxIndex < points.size() - 1)
            {
                // Add next tile to queue
                int nextIndex = maxIndex + 1;
                if (nextIndex < points.size())
                {
                    ManagedTile newTile = new ManagedTile(nextIndex);
                    tileQueue.add(newTile);
                    nextTile = newTile;
                }
            }
        }
        
        if (nextTile == null)
        {
            return;
        }
        
        // Check distance to player
        RoutePoint point = points.get(nextTile.index);
        int distance = getDistance(playerPos, point);
        
        if (distance <= getConfig().hideDistance())
        {
            int delayTicks = getConfig().hideDelayTicks();
            if (delayTicks <= 0)
            {
                // No delay - hide immediately
                nextTile.isHidden = true;
            }
            else
            {
                // Player is near this tile, mark it as pending hide with countdown
                nextTile.markPendingHide(delayTicks);
            }
        }
    }
    
    /**
     * Get the maximum index currently in the queue
     */
    private int getMaxQueueIndex()
    {
        int max = -1;
        for (ManagedTile tile : tileQueue)
        {
            if (tile.index > max)
            {
                max = tile.index;
            }
        }
        return max;
    }
    
    /**
     * Check if a tile at the given index should be visible
     */
    public boolean isTileVisible(int index)
    {
        if (!enabled || currentRoute == null)
        {
            return true; // Show all tiles when disabled or no route
        }
        
        List<RoutePoint> points = currentRoute.getPoints();
        if (index < 0 || index >= points.size())
        {
            return false;
        }
        
        // Find the tile in the queue
        for (ManagedTile tile : tileQueue)
        {
            if (tile.index == index)
            {
                return tile.isVisible();
            }
        }
        
        // Tile not in queue - check if it's beyond max visible
        int firstVisibleIndex = findFirstVisibleIndex(points);
        if (index < firstVisibleIndex)
        {
            return false; // This tile should be hidden (previous tiles were hidden)
        }
        
        if (index >= firstVisibleIndex + getConfig().maxVisibleTiles())
        {
            return false; // Beyond max visible tiles
        }
        
        return true; // Should be visible but not in queue yet
    }
    
    /**
     * Get the list of visible tile indices
     */
    public Set<Integer> getVisibleTileIndices()
    {
        Route activeRoute = routeManager.getActiveRoute();
        
        // If no route, return empty set
        if (activeRoute == null)
        {
            return new HashSet<>();
        }
        
        // Always use the current active route to get the latest points
        List<RoutePoint> points = activeRoute.getPoints();
        
        if (!enabled)
        {
            // Return all indices when disabled
            Set<Integer> allIndices = new HashSet<>();
            for (int i = 0; i < points.size(); i++)
            {
                allIndices.add(i);
            }
            return allIndices;
        }
        
        // In edit mode, show all tiles
        if (routeManager.isInEditMode())
        {
            Set<Integer> allIndices = new HashSet<>();
            for (int i = 0; i < points.size(); i++)
            {
                allIndices.add(i);
            }
            return allIndices;
        }
        
        // Use currentRoute for visibility logic, but ensure it's up to date
        if (currentRoute == null)
        {
            currentRoute = activeRoute;
            initializeQueue(activeRoute);
        }
        
        Set<Integer> visible = new HashSet<>();
        List<RoutePoint> currentRoutePoints = currentRoute.getPoints();
        int firstVisibleIndex = findFirstVisibleIndex(currentRoutePoints);
        
        for (int i = firstVisibleIndex; i < currentRoutePoints.size() && i < firstVisibleIndex + getConfig().maxVisibleTiles(); i++)
        {
            // Check if tile is visible by looking in queue
            boolean found = false;
            for (ManagedTile tile : tileQueue)
            {
                if (tile.index == i)
                {
                    if (tile.isVisible())
                    {
                        visible.add(i);
                    }
                    found = true;
                    break;
                }
            }
            
            // If not in queue yet, it should be visible
            if (!found)
            {
                visible.add(i);
            }
        }
        
        return visible;
    }
    
    /**
     * Reset all state (called when route changes or feature is disabled)
     */
    public void reset()
    {
        tileQueue.clear();
        currentRoute = null;
    }
    
    /**
     * Find the first visible tile index (first tile that isn't hidden)
     */
    private int findFirstVisibleIndex(List<RoutePoint> points)
    {
        for (int i = 0; i < points.size(); i++)
        {
            // Check if this tile is in the queue and visible
            boolean isVisible = true;
            for (ManagedTile tile : tileQueue)
            {
                if (tile.index == i)
                {
                    isVisible = tile.isVisible();
                    break;
                }
            }
            
            if (isVisible)
            {
                return i;
            }
        }
        // All tiles are hidden
        return points.size();
    }
    
    /**
     * Calculate distance between player position and route point
     */
    private int getDistance(WorldPoint playerPos, RoutePoint point)
    {
        if (playerPos.getPlane() != point.getPlane())
        {
            return Integer.MAX_VALUE; // Different plane = very far
        }
        
        int dx = Math.abs(playerPos.getX() - point.getX());
        int dy = Math.abs(playerPos.getY() - point.getY());
        
        // Use Chebyshev distance (max of dx and dy) for tile distance
        return Math.max(dx, dy);
    }
}
