package com.barracudaroutes;

import net.runelite.client.ui.PluginPanel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BarracudaRoutesPanel extends PluginPanel
{
    private final BarracudaRoutesPlugin plugin;
    private final DefaultListModel<Route> routeListModel = new DefaultListModel<>();
    private final JList<Route> routeJList = new JList<>(routeListModel);
    private final JButton createButton = new JButton("Create route");
    private final JButton addTileButton = new JButton("Add tile (player)");
    private final JButton saveButton = new JButton("Finish & Save");
    private final JButton activateButton = new JButton("Activate selected");
    private final JButton deleteButton = new JButton("Delete selected");
    private final JLabel statusLabel = new JLabel("Ready");
    private boolean creating = false;
    private Route currentRoute = null;

    public BarracudaRoutesPanel(BarracudaRoutesPlugin plugin)
    {
        this.plugin = plugin;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setMaximumSize(new Dimension(300, 200));
        top.add(createButton);
        top.add(addTileButton);
        top.add(saveButton);
        top.add(activateButton);
        top.add(deleteButton);
        top.add(statusLabel);
        add(top);
        routeJList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value.getName()));
        add(new JScrollPane(routeJList));
        for (Route r : RouteManager.getAllRoutes())
        {
            routeListModel.addElement(r);
        }
        createButton.addActionListener(e -> onCreate());
        addTileButton.addActionListener(e -> onAddTile());
        saveButton.addActionListener(e -> onSave());
        activateButton.addActionListener(e -> onActivate());
        deleteButton.addActionListener(e -> onDelete());
        updateButtons();
    }

    private void onCreate()
    {
        creating = true;
        currentRoute = new Route("Route " + (RouteManager.getAllRoutes().size() + 1));
        statusLabel.setText("Creating: " + currentRoute.getName());
        updateButtons();
    }

    private void onAddTile()
    {
        if (!creating || currentRoute == null)
        {
            statusLabel.setText("Not in create mode");
            return;
        }
        if (plugin.getClient().getLocalPlayer() == null)
        {
            statusLabel.setText("Player not found");
            return;
        }
        int x = plugin.getClient().getLocalPlayer().getWorldLocation().getX();
        int y = plugin.getClient().getLocalPlayer().getWorldLocation().getY();
        int plane = plugin.getClient().getPlane();
        currentRoute.addPoint(new RoutePoint(x, y, plane));
        statusLabel.setText("Added tile (" + x + "," + y + "," + plane + ")");
    }

    private void onSave()
    {
        if (!creating || currentRoute == null)
        {
            statusLabel.setText("Nothing to save");
            return;
        }
        if (currentRoute.getPoints().isEmpty())
        {
            statusLabel.setText("Route has no points");
            return;
        }
        RouteManager.addRoute(currentRoute);
        routeListModel.addElement(currentRoute);
        creating = false;
        currentRoute = null;
        statusLabel.setText("Saved");
        updateButtons();
    }

    public void onActivate()
    {
        Route r = routeJList.getSelectedValue();
        if (r == null)
        {
            statusLabel.setText("Select a route first");
            return;
        }
        RouteManager.setActiveRoute(r);
        statusLabel.setText("Activated: " + r.getName());
    }

    private void onDelete()
    {
        Route r = routeJList.getSelectedValue();
        if (r == null)
        {
            statusLabel.setText("Select a route first");
            return;
        }
        RouteManager.removeRoute(r);
        routeListModel.removeElement(r);
        statusLabel.setText("Deleted: " + r.getName());
    }

    private void updateButtons()
    {
        addTileButton.setEnabled(creating);
        saveButton.setEnabled(creating);
    }
}
