# Barracuda Routes

Create, manage, and follow routes for Barracuda Trials with automatic tile visibility and lap-based coloring.

## Features

### Following a Route
1. Select a route from the main panel
2. The route will appear on the overlay with colored lines
3. Tiles will automatically hide as you pass them

### Route Creation & Management
- **Create routes** with custom names and descriptions
- **Edit mode** - all tiles are always visible when editing a route
- **Record routes** while sailing - automatically captures your path
- **Manual tile placement** - add tiles at your current position
- **Multiple laps** - organize your route into separate laps
- **Drag and drop** - reorder tiles and lap dividers
- **Custom lap colors** - set custom colors for each lap or use the default color scheme

## Config Options

### Route Visibility
- **Hide Distance** - Distance in tiles at which route tiles will hide when the player is near them (default: 5)
- **Hide Delay (Ticks)** - Number of game ticks to wait before hiding a tile after the player is near it (default: 5, 1 tick = 0.6 seconds)
- **Max Visible Tiles** - Maximum number of route tiles visible at a time (default: 30)

## File Storage

Routes are saved as JSON files in:
```
.runelite/barracuda-routes/<TrialName>/<Route_Name>_<UUID>.json
```