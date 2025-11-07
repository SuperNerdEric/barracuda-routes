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
    private JList<Route> routesList;
    private DefaultListModel<Route> listModel;
    private JButton editButton;
    private JButton deleteButton;
    private JPanel actionButtonsPanel;
    private RouteEditPanel editPanel;
    
    private static final ImageIcon EDIT_ICON;
    private static final ImageIcon DELETE_ICON;
    
    static
    {
        EDIT_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/edit.png"));
        DELETE_ICON = new ImageIcon(ImageUtil.loadImageResource(BarracudaRoutesPanel.class, "/panel/delete.png"));
    }

    public BarracudaRoutesPanel(BarracudaRoutesPlugin plugin, net.runelite.client.ui.components.colorpicker.ColorPickerManager colorPickerManager)
    {
        this.plugin = plugin;
        this.colorPickerManager = colorPickerManager;
        setLayout(new BorderLayout());
        
        // Set up main panel
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setAlignmentX(Component.LEFT_ALIGNMENT); // Explicitly align to left
        top.setMaximumSize(new Dimension(300, 200));
        top.add(createButton);
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
        
        // Add existing routes
        for (Route r : RouteManager.getAllRoutes())
        {
            listModel.addElement(r);
        }
        
        createButton.addActionListener(e -> onCreate());
        
        // Set up card layout
        cardPanel.add(mainPanel, "MAIN");
        add(cardPanel, BorderLayout.CENTER);
        
        // Update selection for initial state
        updateSelection();
    }

    private void onCreate()
    {
        // Create a new route immediately
        Route newRoute = new Route("", "", "The Tempor Tantrum");
        RouteManager.addRoute(newRoute);
        listModel.addElement(newRoute);
        
        // Switch to edit panel
        startEditing(newRoute, true);
    }
    
    private void onEditSelected()
    {
        Route selected = routesList.getSelectedValue();
        if (selected != null)
        {
            startEditing(selected);
        }
    }
    
    private void onDeleteSelected()
    {
        Route selected = routesList.getSelectedValue();
        if (selected != null)
        {
            onDelete(selected);
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
        listModel.removeElement(route);
        
        // Clear selection if the deleted route was selected
        if (RouteManager.getActiveRoute() == route)
        {
            RouteManager.setActiveRoute(null);
            routesList.clearSelection();
        }
    }
    
    private void updateSelection()
    {
        Route selected = routesList.getSelectedValue();
        if (selected != null)
        {
            RouteManager.setActiveRoute(selected);
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
            
            if (value instanceof Route)
            {
                Route route = (Route) value;
                label.setText(route.getName());
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
        
        // Select the route in the list
        routesList.setSelectedValue(route, true);
        
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
        // Refresh list by updating the route
        if (editPanel != null && editPanel.route != null)
        {
            // If route was removed (new route without name), remove from list
            if (!RouteManager.getAllRoutes().contains(editPanel.route))
            {
                listModel.removeElement(editPanel.route);
            }
            else
            {
                // Update the list to reflect name changes
                int index = listModel.indexOf(editPanel.route);
                if (index >= 0)
                {
                    listModel.set(index, editPanel.route);
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
