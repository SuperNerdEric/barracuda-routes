package com.barracudaroutes;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class RouteManager
{
    private final List<Route> routes = new ArrayList<>();
    private Route active = null;
    private PointNode selectedTile = null;
    private boolean inEditMode = false;
    
    private final RoutePersistenceManager persistenceManager;
    
    @Inject
    public RouteManager(RoutePersistenceManager persistenceManager)
    {
        this.persistenceManager = persistenceManager;
    }
    
    /**
     * Load all routes from disk
     */
    public void loadRoutes()
    {
        routes.clear();
        routes.addAll(persistenceManager.loadAllRoutes());
    }

    public List<Route> getAllRoutes()
    {
        return new ArrayList<>(routes);
    }

    public void addRoute(Route r)
    {
        routes.add(r);
        // Save to disk
        persistenceManager.saveRoute(r);
    }

    public void removeRoute(Route r)
    {
        routes.remove(r);
        if (active == r)
        {
            active = null;
        }
        // Delete from disk
        persistenceManager.deleteRoute(r);
    }
    
    /**
     * Update a route (save to disk)
     */
    public void updateRoute(Route r)
    {
        // Save to disk
        persistenceManager.saveRoute(r);
    }

    public Route getActiveRoute()
    {
        return active;
    }

    public void setActiveRoute(Route r)
    {
        active = r;
    }
    
    public PointNode getSelectedTile()
    {
        return selectedTile;
    }
    
    public void setSelectedTile(PointNode tile)
    {
        selectedTile = tile;
    }
    
    public boolean isInEditMode()
    {
        return inEditMode;
    }
    
    public void setInEditMode(boolean inEditMode)
    {
        this.inEditMode = inEditMode;
    }
}
