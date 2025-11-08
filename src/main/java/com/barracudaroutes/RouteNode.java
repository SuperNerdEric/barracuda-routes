package com.barracudaroutes;

/**
 * Base interface for route nodes
 */
public interface RouteNode
{
    /**
     * Get the type of this node
     * @return "point" or "lapDivider"
     */
    String getType();
}

