package com.barracudaroutes.ui;

import com.barracudaroutes.BarracudaRoutesPlugin;
import com.barracudaroutes.model.Route;
import com.barracudaroutes.managers.RouteManager;
import com.barracudaroutes.managers.RouteImportExportManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;

public class BarracudaRoutesPanel extends PluginPanel
{
    private final BarracudaRoutesPlugin plugin;
    private final net.runelite.client.ui.components.colorpicker.ColorPickerManager colorPickerManager;
    private final RouteManager routeManager;
    private final RouteImportExportManager importExportManager;
    private final JButton createButton = new JButton("Create route");
    
    // Card layout for switching between main and edit panels
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JPanel mainPanel = new JPanel();
    private JList<Object> routesList;
    private DefaultListModel<Object> listModel;
    
    private static final String[] TRIAL_OPTIONS = {"The Tempor Tantrum", "Jubbly Jive", "Gwenith Glide"};
    private JButton exportButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton importButton;
    private JPanel actionButtonsPanel;
    private RouteEditPanel editPanel;
    private JComponent editPanelComponent;
    
    private static final ImageIcon EDIT_ICON;
    private static final ImageIcon DELETE_ICON;
    private static final ImageIcon BOAT_ICON;
    private static final ImageIcon IMPORT_ICON;
    private static final ImageIcon EXPORT_ICON;
    
    static
    {
        EDIT_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/edit.png"));
        DELETE_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/delete.png"));
        BOAT_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/boat.png"));
        IMPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/import.png"));
        EXPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/export.png"));
    }

    public BarracudaRoutesPanel(BarracudaRoutesPlugin plugin, net.runelite.client.ui.components.colorpicker.ColorPickerManager colorPickerManager, RouteManager routeManager, RouteImportExportManager importExportManager)
    {
        super(false);
        this.setBorder(new EmptyBorder(6, 6, 6, 6));
        this.plugin = plugin;
        this.colorPickerManager = colorPickerManager;
        this.routeManager = routeManager;
        this.importExportManager = importExportManager;
        setLayout(new BorderLayout());
        
        // Set up main panel
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Title panel with import button on the right
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); // Constrain height to prevent layout shifts
        titlePanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 30));
        titlePanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        
        JLabel titleLabel = new JLabel("Barracuda Routes");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        // Import button on the right
        importButton = createButton(IMPORT_ICON, "Import route", () -> onImport());
        titlePanel.add(importButton, BorderLayout.EAST);

        mainPanel.add(titlePanel);
        
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        top.setAlignmentX(Component.LEFT_ALIGNMENT); // Explicitly align to left
        top.setMaximumSize(new Dimension(300, 200));
        
        // Add icon to the button
        createButton.setIcon(BOAT_ICON);
        createButton.setIconTextGap(8); // Add spacing between text and icon
        createButton.setBackground(new Color(0, 100, 0));

        top.add(createButton, BorderLayout.NORTH);
        top.setBorder(new EmptyBorder(0, 0, 8, 0)); // Add margin below the button
        // Set preferred size after adding components to prevent shifting when other components change
        top.setPreferredSize(new Dimension(300, top.getPreferredSize().height));
        mainPanel.add(top);
        
        // Routes list header with action buttons
        JPanel routesHeaderPanel = new JPanel(new BorderLayout());
        routesHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // Explicitly align to left
        routesHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); // Constrain height
        // Set preferred size to prevent width changes when buttons appear/disappear
        routesHeaderPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel routesLabel = new JLabel("Routes");
        routesLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        routesHeaderPanel.add(routesLabel, BorderLayout.WEST);
        
        // Action buttons panel (export/edit/delete) - initially hidden
        actionButtonsPanel = new JPanel();
        actionButtonsPanel.setLayout(new BoxLayout(actionButtonsPanel, BoxLayout.X_AXIS));
        exportButton = createButton(EXPORT_ICON, "Export route", () -> onExportSelected());
        editButton = createButton(EDIT_ICON, "Edit route", () -> onEditSelected());
        deleteButton = createButton(DELETE_ICON, "Delete route", () -> onDeleteSelected());
        actionButtonsPanel.add(exportButton);
        actionButtonsPanel.add(editButton);
        actionButtonsPanel.add(deleteButton);
        // Set fixed size to prevent layout shifts
        actionButtonsPanel.setPreferredSize(new Dimension(90, 24));
        actionButtonsPanel.setMaximumSize(new Dimension(90, 24));
        actionButtonsPanel.setVisible(false);
        routesHeaderPanel.add(actionButtonsPanel, BorderLayout.EAST);
        mainPanel.add(routesHeaderPanel);
        
        // Set up routes list
        listModel = new DefaultListModel<>();
        routesList = new JList<>(listModel);
        routesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        routesList.setCellRenderer(new RouteListCellRenderer());
        
        // Handle selection changes
        routesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelection();
            }
        });
        
        // Handle double-click to edit
        routesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object selected = routesList.getSelectedValue();
                    // Only allow editing routes, not empty markers or trial headers
                    if (selected instanceof Route) {
                        onEditSelected();
                    }
                }
            }
        });
        
        JScrollPane routeListScrollPane = new JScrollPane(routesList);
        routeListScrollPane.setBorder(new EmptyBorder(8, 0, 0, 0));
        routeListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        routeListScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        routeListScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT); // Explicitly align to left
        mainPanel.add(routeListScrollPane);
        
        // Populate routes list organized by trial
        populateRoutesList();
        
        createButton.addActionListener(e -> onCreate());
        
        // Set up card layout
        cardPanel.add(mainPanel, "MAIN");
        add(cardPanel, BorderLayout.CENTER);
        
        // Update selection for initial state
        updateSelection();
    }

    private void populateRoutesList()
    {
        listModel.clear();
        
        // Group routes by trial
        java.util.Map<String, java.util.List<Route>> routesByTrial = new java.util.HashMap<>();
        for (Route route : routeManager.getAllRoutes())
        {
            String trialName = route.getTrialName();
            if (trialName == null || trialName.isEmpty())
            {
                trialName = TRIAL_OPTIONS[0]; // Default to first trial
            }
            routesByTrial.computeIfAbsent(trialName, k -> new java.util.ArrayList<>()).add(route);
        }
        
        // Add sections for each trial in order
        for (String trialName : TRIAL_OPTIONS)
        {
            // Add trial header
            listModel.addElement(trialName);
            
            // Add routes for this trial (if any)
            if (routesByTrial.containsKey(trialName) && !routesByTrial.get(trialName).isEmpty())
            {
                for (Route route : routesByTrial.get(trialName))
                {
                    listModel.addElement(route);
                }
            }
            else
            {
                // Add "No routes yet" marker for empty trials
                listModel.addElement(new EmptyTrialMarker());
            }
        }
    }
    
    private void onCreate()
    {
        // Create a new route immediately
        Route newRoute = new Route("", "", "The Tempor Tantrum");
        routeManager.addRoute(newRoute);
        populateRoutesList();
        
        // Switch to edit panel
        startEditing(newRoute, true);
    }
    
    private void onEditSelected()
    {
        Object selected = routesList.getSelectedValue();
        if (selected instanceof Route)
        {
            startEditing((Route) selected);
        }
        // Ignore EmptyTrialMarker and String (trial headers)
    }
    
    private void onDeleteSelected()
    {
        Object selected = routesList.getSelectedValue();
        if (selected instanceof Route)
        {
            onDelete((Route) selected);
        }
        // Ignore EmptyTrialMarker and String (trial headers)
    }

    private void onDelete(Route route)
    {
        // Show confirmation dialog
        if (!isConfirmed("Are you sure you want to delete this route?", "Delete Route"))
        {
            return;
        }
        
        routeManager.removeRoute(route);
        populateRoutesList();
        
        // Clear selection if the deleted route was selected
        if (routeManager.getActiveRoute() == route)
        {
            routeManager.setActiveRoute(null);
            routesList.clearSelection();
        }
    }
    
    private void onExportSelected()
    {
        Object selected = routesList.getSelectedValue();
        if (selected instanceof Route)
        {
            onExport((Route) selected);
        }
    }
    
    private void onExport(Route route)
    {
        if (route == null)
        {
            return;
        }
        
        // Copy route to clipboard
        boolean success = importExportManager.exportRouteToClipboard(route);
        
        if (success)
        {
            JOptionPane.showMessageDialog(this,
                "Route data was copied to clipboard.",
                "Export Route Succeeded",
                JOptionPane.INFORMATION_MESSAGE);
        }
        else
        {
            JOptionPane.showMessageDialog(this,
                "Failed to export route.",
                "Export Route Failed",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void onImport()
    {
        try
        {
            // Show dialog to paste route JSON
            String json = JOptionPane.showInputDialog(this,
                "Enter route data",
                "Import New Route",
                JOptionPane.PLAIN_MESSAGE);
            
            // Cancel button was clicked
            if (json == null)
            {
                return;
            }
            
            Route importedRoute = importExportManager.importRouteFromJson(json);
            
            if (importedRoute != null)
            {
                // Check if route with same name already exists and rename if needed
                String originalName = importedRoute.getName();
                String newName = originalName;
                int counter = 1;
                while (true)
                {
                    final String nameToCheck = newName;
                    boolean nameExists = routeManager.getAllRoutes().stream().anyMatch(r -> r.getName().equals(nameToCheck));
                    if (!nameExists)
                    {
                        break;
                    }
                    newName = originalName + " (" + counter + ")";
                    counter++;
                }
                importedRoute.setName(newName);
                
                // Add the imported route
                routeManager.addRoute(importedRoute);
                populateRoutesList();
                
                JOptionPane.showMessageDialog(this,
                    "Route imported successfully: " + newName,
                    "Import Route Succeeded",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            else
            {
                JOptionPane.showMessageDialog(this,
                    "Invalid route data.",
                    "Import Route Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this,
                "Failed to import route: " + e.getMessage(),
                "Import Route Failed",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateSelection()
    {
        Object selected = routesList.getSelectedValue();
        if (selected instanceof Route)
        {
            routeManager.setActiveRoute((Route) selected);
            actionButtonsPanel.setVisible(true);
        }
        else
        {
            // Don't show action buttons for trial headers or empty markers
            actionButtonsPanel.setVisible(false);
            // Clear selection if it's not a route
            if (selected instanceof EmptyTrialMarker || selected instanceof String)
            {
                routesList.clearSelection();
            }
        }
    }
    
    // Custom cell renderer for the list
    private class RouteListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof String)
            {
                // Trial section header
                String trialName = (String) value;
                label.setText(trialName);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setForeground(JagexColors.DARK_ORANGE_INTERFACE_TEXT);
            }
            else if (value instanceof Route)
            {
                // Route item
                Route route = (Route) value;
                label.setText("  " + route.getName()); // Indent routes under their trial
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
            }
            else if (value instanceof EmptyTrialMarker)
            {
                // Empty trial marker
                label.setText("  No routes yet");
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
                label.setForeground(Color.GRAY);
                // Make it non-selectable by preventing selection styling
                if (isSelected)
                {
                    label.setBackground(list.getBackground());
                    label.setForeground(Color.GRAY);
                }
            }
            
            return label;
        }
    }
    
    /**
     * Marker class for empty trial sections - cannot be selected, edited, or deleted
     */
    private static class EmptyTrialMarker
    {
        @Override
        public String toString()
        {
            return "No routes yet";
        }
    }

    private void startEditing(Route route)
    {
        startEditing(route, false);
    }
    
    private void startEditing(Route route, boolean isNew)
    {
        // Clean up previous edit panel if it exists
        if (editPanel != null)
        {
            editPanel.cleanup();
            if (editPanelComponent != null)
            {
                cardPanel.remove(editPanelComponent);
                editPanelComponent = null;
            }
        }
        
        // Activate the route so it shows in the overlay
        routeManager.setActiveRoute(route);
        
        // Set edit mode flag
        routeManager.setInEditMode(true);
        
        // Select the route in the list
        int index = listModel.indexOf(route);
        if (index >= 0)
        {
            routesList.setSelectedIndex(index);
        }
        
        // Create new edit panel
        editPanel = new RouteEditPanel(plugin, route, isNew, this::showMainPanel, this::onEditSave, colorPickerManager, routeManager);
        editPanelComponent = editPanel.getWrappedPanel();
        cardPanel.add(editPanelComponent, "EDIT");
        cardLayout.show(cardPanel, "EDIT");
    }
    
    private void showMainPanel()
    {
        if (editPanel != null)
        {
            editPanel.cleanup();
            if (editPanelComponent != null)
            {
                cardPanel.remove(editPanelComponent);
                editPanelComponent = null;
            }
            editPanel = null;
        }
        
        // Clear edit mode flag
        routeManager.setInEditMode(false);
        
        updateSelection();
        cardLayout.show(cardPanel, "MAIN");
        // Revalidate and repaint to fix layout issues
        mainPanel.revalidate();
        mainPanel.repaint();
        cardPanel.revalidate();
        cardPanel.repaint();
    }
    
    private void onEditSave()
    {
        // Refresh list - need to repopulate since trial might have changed
        if (editPanel != null && editPanel.route != null)
        {
            // If route was removed (new route without name), remove from list
            if (!routeManager.getAllRoutes().contains(editPanel.route))
            {
                populateRoutesList();
            }
            else
            {
                // Repopulate to handle trial changes and name updates
                populateRoutesList();
                // Try to restore selection
                int index = listModel.indexOf(editPanel.route);
                if (index >= 0)
                {
                    routesList.setSelectedIndex(index);
                }
            }
        }
    }

    private JButton createButton(ImageIcon icon, String toolTipText, Runnable onClick)
    {
        JButton button = new JButton(icon);
        button.setPreferredSize(new Dimension(24, 24));
        SwingUtil.removeButtonDecorations(button);
        button.setToolTipText(toolTipText);
        button.addActionListener(e -> onClick.run());
        return button;
    }

    private boolean isConfirmed(final String message, final String title)
    {
        int confirm = JOptionPane.showConfirmDialog(this,
                message, title, JOptionPane.OK_CANCEL_OPTION);
        return confirm == JOptionPane.YES_OPTION;
    }
}
