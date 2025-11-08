package com.barracudaroutes;

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
    private final JButton createButton = new JButton("Create route");
    
    // Card layout for switching between main and edit panels
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JPanel mainPanel = new JPanel();
    private JList<Object> routesList;
    private DefaultListModel<Object> listModel;
    
    private static final String[] TRIAL_OPTIONS = {"The Tempor Tantrum", "Jubbly Jive", "Gwenith Glide"};
    private JButton editButton;
    private JButton deleteButton;
    private JPanel actionButtonsPanel;
    private RouteEditPanel editPanel;
    
    private static final ImageIcon EDIT_ICON;
    private static final ImageIcon DELETE_ICON;
    private static final ImageIcon BOAT_ICON;
    
    static
    {
        EDIT_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/edit.png"));
        DELETE_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/delete.png"));
        BOAT_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/boat.png"));
    }

    public BarracudaRoutesPanel(BarracudaRoutesPlugin plugin, net.runelite.client.ui.components.colorpicker.ColorPickerManager colorPickerManager)
    {
        this.plugin = plugin;
        this.colorPickerManager = colorPickerManager;
        setLayout(new BorderLayout());
        
        // Set up main panel
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        top.setAlignmentX(Component.LEFT_ALIGNMENT); // Explicitly align to left
        top.setMaximumSize(new Dimension(300, 200));
        
        // Create button panel with icon to the right
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(createButton);
        buttonPanel.add(Box.createHorizontalStrut(8)); // Add spacing between button and icon
        JLabel boatLabel = new JLabel(BOAT_ICON);
        buttonPanel.add(boatLabel);
        
        top.add(buttonPanel, BorderLayout.WEST);
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
        routesLabel.setFont(routesLabel.getFont().deriveFont(Font.BOLD));
        routesLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        routesHeaderPanel.add(routesLabel, BorderLayout.WEST);
        
        // Action buttons panel (edit/delete) - initially hidden
        actionButtonsPanel = new JPanel();
        actionButtonsPanel.setLayout(new BoxLayout(actionButtonsPanel, BoxLayout.X_AXIS));
        editButton = createButton(EDIT_ICON, "Edit route", () -> onEditSelected());
        deleteButton = createButton(DELETE_ICON, "Delete route", () -> onDeleteSelected());
        actionButtonsPanel.add(editButton);
        actionButtonsPanel.add(deleteButton);
        // Set fixed size to prevent layout shifts
        actionButtonsPanel.setPreferredSize(new Dimension(60, 24));
        actionButtonsPanel.setMaximumSize(new Dimension(60, 24));
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
                    onEditSelected();
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
        for (Route route : RouteManager.getAllRoutes())
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
            if (routesByTrial.containsKey(trialName))
            {
                for (Route route : routesByTrial.get(trialName))
                {
                    listModel.addElement(route);
                }
            }
        }
    }
    
    private void onCreate()
    {
        // Create a new route immediately
        Route newRoute = new Route("", "", "The Tempor Tantrum");
        RouteManager.addRoute(newRoute);
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
    }
    
    private void onDeleteSelected()
    {
        Object selected = routesList.getSelectedValue();
        if (selected instanceof Route)
        {
            onDelete((Route) selected);
        }
    }

    private void onDelete(Route route)
    {
        // Show confirmation dialog
        if (!isConfirmed("Are you sure you want to delete this route?", "Delete Route"))
        {
            return;
        }
        
        RouteManager.removeRoute(route);
        populateRoutesList();
        
        // Clear selection if the deleted route was selected
        if (RouteManager.getActiveRoute() == route)
        {
            RouteManager.setActiveRoute(null);
            routesList.clearSelection();
        }
    }
    
    private void updateSelection()
    {
        Object selected = routesList.getSelectedValue();
        if (selected instanceof Route)
        {
            RouteManager.setActiveRoute((Route) selected);
            actionButtonsPanel.setVisible(true);
        }
        else
        {
            actionButtonsPanel.setVisible(false);
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
            }
            else if (value instanceof Route)
            {
                // Route item
                Route route = (Route) value;
                label.setText("  " + route.getName()); // Indent routes under their trial
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
            }
            
            return label;
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
            cardPanel.remove(editPanel);
        }
        
        // Activate the route so it shows in the overlay
        RouteManager.setActiveRoute(route);
        
        // Set edit mode flag
        RouteManager.setInEditMode(true);
        
        // Select the route in the list
        int index = listModel.indexOf(route);
        if (index >= 0)
        {
            routesList.setSelectedIndex(index);
        }
        
        // Create new edit panel
        editPanel = new RouteEditPanel(plugin, route, isNew, this::showMainPanel, this::onEditSave, colorPickerManager);
        cardPanel.add(editPanel, "EDIT");
        cardLayout.show(cardPanel, "EDIT");
    }
    
    private void showMainPanel()
    {
        if (editPanel != null)
        {
            editPanel.cleanup();
            cardPanel.remove(editPanel);
            editPanel = null;
        }
        
        // Clear edit mode flag
        RouteManager.setInEditMode(false);
        
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
            if (!RouteManager.getAllRoutes().contains(editPanel.route))
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
