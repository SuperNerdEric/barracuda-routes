package com.barracudaroutes;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class RouteEditPanel extends PluginPanel
{
    private final BarracudaRoutesPlugin plugin;
    final Route route;  // Made package-private for access from BarracudaRoutesPanel
    private final boolean isNewRoute;
    private final Runnable onBack;
    private final Runnable onSave;
    private final net.runelite.client.ui.components.colorpicker.ColorPickerManager colorPickerManager;
    private final RouteManager routeManager;
    
    private static final ImageIcon EDIT_ICON;
    private static final ImageIcon DELETE_ICON;
    private static final ImageIcon ARROW_BACK_ICON;
    
    static
    {
        EDIT_ICON = new ImageIcon(ImageUtil.loadImageResource(RouteEditPanel.class, "/panel/edit.png"));
        DELETE_ICON = new ImageIcon(ImageUtil.loadImageResource(RouteEditPanel.class, "/panel/delete.png"));
        ARROW_BACK_ICON = new ImageIcon(ImageUtil.loadImageResource(RouteEditPanel.class, "/panel/arrow_back.png"));
    }
    
    private final JButton backButton = new JButton(ARROW_BACK_ICON);
    private final JButton recordButton = new JButton("Record");
    private final JButton stopRecordButton = new JButton("Stop Recording");
    private final JButton addTileButton = new JButton("Add tile (player)");
    private final JButton newLapButton = new JButton("New Lap");
    
    private JTextField nameField;
    private JTextArea descriptionField;
    private JComboBox<String> trialComboBox;
    private JList<Object> tilesList;
    private DefaultListModel<Object> listModel;
    private JButton editButton;
    private JButton deleteButton;
    private JPanel actionButtonsPanel;
    private Integer selectedLap = null;
    
    private boolean recording = false;
    private Timer recordingTimer;
    private RoutePoint lastRecordedPoint = null;
    private int currentLap = 1;
    private final java.util.Set<Integer> emptyLaps = new java.util.HashSet<>();
    
    public RouteEditPanel(BarracudaRoutesPlugin plugin, Route route, boolean isNewRoute, Runnable onBack, Runnable onSave, net.runelite.client.ui.components.colorpicker.ColorPickerManager colorPickerManager, RouteManager routeManager)
    {
        this.plugin = plugin;
        this.route = route;
        this.isNewRoute = isNewRoute;
        this.onBack = onBack;
        this.onSave = onSave;
        this.colorPickerManager = colorPickerManager;
        this.routeManager = routeManager;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Top panel with back button on the right
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(backButton, BorderLayout.EAST);
        add(topPanel);
        
        // Style back button
        backButton.setPreferredSize(new Dimension(24, 24));
        SwingUtil.removeButtonDecorations(backButton);
        backButton.setToolTipText("Back");
        
        // Edit fields
        JPanel editPanel = createEditFields();
        editPanel.setBorder(BorderFactory.createCompoundBorder(
            editPanel.getBorder(),
            new EmptyBorder(0, 0, 8, 0)
        ));
        add(editPanel);
        
        // Buttons panel (record, stop recording, add tile, new lap) - aligned to the left
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.add(recordButton);
        buttonsPanel.add(stopRecordButton);
        buttonsPanel.add(addTileButton);
        buttonsPanel.add(newLapButton);
        
        // Wrap in a panel with BorderLayout to align to the left
        JPanel buttonsWrapper = new JPanel(new BorderLayout());
        buttonsWrapper.add(buttonsPanel, BorderLayout.WEST);
        add(buttonsWrapper);
        
        // Initially hide stop recording button
        stopRecordButton.setVisible(false);
        
        // Tiles list header with action buttons
        JPanel tilesHeaderPanel = new JPanel(new BorderLayout());
        JLabel tilesLabel = new JLabel("Tiles");
        tilesLabel.setFont(tilesLabel.getFont().deriveFont(Font.BOLD));
        tilesLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        tilesHeaderPanel.add(tilesLabel, BorderLayout.WEST);
        
        // Action buttons panel (edit/delete) - initially hidden
        actionButtonsPanel = new JPanel();
        actionButtonsPanel.setLayout(new BoxLayout(actionButtonsPanel, BoxLayout.X_AXIS));
        editButton = createButton(EDIT_ICON, "Edit", () -> onEditSelected());
        deleteButton = createButton(DELETE_ICON, "Delete", () -> onDeleteSelected());
        actionButtonsPanel.add(editButton);
        actionButtonsPanel.add(deleteButton);
        actionButtonsPanel.setVisible(false);
        tilesHeaderPanel.add(actionButtonsPanel, BorderLayout.EAST);
        add(tilesHeaderPanel);
        
        // Set up tiles list
        listModel = new DefaultListModel<>();
        tilesList = new JList<>(listModel);
        tilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tilesList.setCellRenderer(new TileListCellRenderer());
        tilesList.setDragEnabled(true);
        tilesList.setDropMode(DropMode.INSERT);
        tilesList.setTransferHandler(new TileListTransferHandler());
        
        // Handle selection changes
        tilesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelection();
            }
        });
        
        // Handle double-click to edit
        tilesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onEditSelected();
                }
            }
        });
        
        JScrollPane tilesScroll = new JScrollPane(tilesList);
        tilesScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 800));
        add(tilesScroll);
        add(Box.createVerticalGlue());
        
        // Populate fields
        populateFields();
        
        // Set up listeners
        backButton.addActionListener(e -> onBack());
        recordButton.addActionListener(e -> onRecord());
        stopRecordButton.addActionListener(e -> onStopRecord());
        addTileButton.addActionListener(e -> onAddTile());
        newLapButton.addActionListener(e -> onNewLap());
        
        updateButtons();
    }
    
    private JPanel createEditFields()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Details"));
        
        // Name field
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.add(new JLabel("Name:"));
        nameField = new JTextField(20);
        namePanel.add(nameField);
        panel.add(namePanel);
        
        // Description field
        JPanel descPanel = new JPanel();
        descPanel.setLayout(new BoxLayout(descPanel, BoxLayout.Y_AXIS));
        descPanel.add(new JLabel("Description (optional):"));
        descriptionField = new JTextArea(3, 20);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionField);
        descPanel.add(descScroll);
        panel.add(descPanel);
        
        // Trial Name dropdown
        JPanel trialPanel = new JPanel();
        trialPanel.setLayout(new BoxLayout(trialPanel, BoxLayout.X_AXIS));
        trialPanel.add(new JLabel("Trial:"));
        String[] trialOptions = {"The Tempor Tantrum", "Jubbly Jive", "Gwenith Glide"};
        trialComboBox = new JComboBox<>(trialOptions);
        trialPanel.add(trialComboBox);
        panel.add(trialPanel);
        
        return panel;
    }
    
    private void populateFields()
    {
        nameField.setText(route.getName());
        descriptionField.setText(route.getDescription() != null ? route.getDescription() : "");
        
        // Set trial combo box
        String trialName = route.getTrialName();
        String[] trialOptions = {"The Tempor Tantrum", "Jubbly Jive", "Gwenith Glide"};
        boolean found = false;
        for (String option : trialOptions)
        {
            if (option.equals(trialName))
            {
                trialComboBox.setSelectedItem(option);
                found = true;
                break;
            }
        }
        // If trial name doesn't match any option, default to first option
        if (!found && trialOptions.length > 0)
        {
            trialComboBox.setSelectedItem(trialOptions[0]);
        }
        
        // Initialize currentLap to the highest lap number, or 1 if no points
        currentLap = 1;
        for (RoutePoint point : route.getPoints())
        {
            if (point.getLap() > currentLap)
            {
                currentLap = point.getLap();
            }
            // Also ensure all points have a valid lap (default to 1 if lap is 0)
            if (point.getLap() == 0)
            {
                point.setLap(1);
            }
        }
        
        // Populate tiles list organized by lap
        populateTilesList();
    }
    
    private void populateTilesList()
    {
        listModel.clear();
        
        // Group points by lap
        java.util.Map<Integer, java.util.List<RoutePoint>> pointsByLap = new java.util.HashMap<>();
        for (RoutePoint point : route.getPoints())
        {
            int lap = point.getLap();
            pointsByLap.computeIfAbsent(lap, k -> new java.util.ArrayList<>()).add(point);
        }
        
        // Combine laps with tiles and empty laps
        java.util.Set<Integer> allLaps = new java.util.HashSet<>(pointsByLap.keySet());
        allLaps.addAll(emptyLaps);
        
        // Sort laps and add dividers and tiles
        java.util.List<Integer> laps = new java.util.ArrayList<>(allLaps);
        java.util.Collections.sort(laps);
        
        for (int lap : laps)
        {
            // Add lap divider (as Integer)
            listModel.addElement(lap);
            
            // Add tiles for this lap (if any)
            if (pointsByLap.containsKey(lap))
            {
                for (RoutePoint point : pointsByLap.get(lap))
                {
                    listModel.addElement(point);
                }
            }
        }
        
        // Remove empty laps that now have tiles
        emptyLaps.removeAll(pointsByLap.keySet());
        
        // Restore selection if possible
        restoreSelection();
    }
    
    private void updateSelection()
    {
        Object selected = tilesList.getSelectedValue();
        if (selected instanceof RoutePoint)
        {
            RoutePoint point = (RoutePoint) selected;
            routeManager.setSelectedTile(point);
            selectedLap = null;
            actionButtonsPanel.setVisible(true);
            editButton.setVisible(true);
            deleteButton.setVisible(true);
            editButton.setToolTipText("Edit tile");
            deleteButton.setToolTipText("Delete tile");
        }
        else if (selected instanceof Integer)
        {
            selectedLap = (Integer) selected;
            routeManager.setSelectedTile(null);
            actionButtonsPanel.setVisible(true);
            editButton.setVisible(true); // Can edit lap color
            deleteButton.setVisible(true);
            editButton.setToolTipText("Edit lap color");
            deleteButton.setToolTipText("Delete lap");
        }
        else
        {
            selectedLap = null;
            routeManager.setSelectedTile(null);
            actionButtonsPanel.setVisible(false);
        }
    }
    
    private void restoreSelection()
    {
        // Try to restore selection based on RouteManager or selectedLap
        RoutePoint selectedTile = routeManager.getSelectedTile();
        if (selectedTile != null)
        {
            for (int i = 0; i < listModel.size(); i++)
            {
                Object element = listModel.get(i);
                if (element == selectedTile)
                {
                    tilesList.setSelectedIndex(i);
                    return;
                }
            }
        }
        else if (selectedLap != null)
        {
            for (int i = 0; i < listModel.size(); i++)
            {
                Object element = listModel.get(i);
                if (element instanceof Integer && element.equals(selectedLap))
                {
                    tilesList.setSelectedIndex(i);
                    return;
                }
            }
        }
        tilesList.clearSelection();
    }
    
    // Custom cell renderer for the list
    private class TileListCellRenderer extends DefaultListCellRenderer
    {
        // Default color scheme for laps (repeating) - same as RouteOverlay
        private final Color[] DEFAULT_LAP_COLORS = {
            Color.RED,      // Lap 1
            Color.BLUE,     // Lap 2
            Color.GREEN,   // Lap 3
            Color.WHITE,   // Lap 4
            Color.BLACK,   // Lap 5
            new Color(128, 0, 128), // Lap 6 - Purple
            new Color(255, 165, 0)  // Lap 7 - Orange
        };
        
        private Color getLapColor(int lap)
        {
            // Check for custom color first
            Color customColor = route.getLapColor(lap);
            if (customColor != null)
            {
                return customColor;
            }
            
            // Use default color scheme (1-indexed, cycles through)
            int colorIndex = (lap - 1) % DEFAULT_LAP_COLORS.length;
            return DEFAULT_LAP_COLORS[colorIndex];
        }
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus)
        {
            if (value instanceof Integer)
            {
                // Lap divider - create a panel with label and color indicator
                int lap = (Integer) value;
                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(true);
                
                // Get the base label for styling
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText("Lap " + lap);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setBorder(new EmptyBorder(0, 0, 0, 8)); // Add spacing between text and color line
                
                // Create colored line indicator
                JPanel colorIndicator = new JPanel();
                colorIndicator.setPreferredSize(new Dimension(50, 5));
                colorIndicator.setMinimumSize(new Dimension(50, 5));
                colorIndicator.setMaximumSize(new Dimension(50, 5));
                colorIndicator.setOpaque(true);
                colorIndicator.setBackground(getLapColor(lap));
                
                // Set panel background based on selection
                if (isSelected)
                {
                    panel.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
                else
                {
                    panel.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
                
                panel.add(label, BorderLayout.WEST);
                panel.add(colorIndicator, BorderLayout.CENTER);
                
                return panel;
            }
            else if (value instanceof RoutePoint)
            {
                // Tile - use default renderer
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                RoutePoint point = (RoutePoint) value;
                label.setText("(" + point.getX() + ", " + point.getY() + ", " + point.getPlane() + ")");
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
                return label;
            }
            
            // Fallback
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
    
    private void onEditSelected()
    {
        Object selected = tilesList.getSelectedValue();
        if (selected instanceof RoutePoint)
        {
            onEditTile((RoutePoint) selected);
        }
        else if (selected instanceof Integer)
        {
            onEditLap((Integer) selected);
        }
    }
    
    private void onDeleteSelected()
    {
        Object selected = tilesList.getSelectedValue();
        if (selected instanceof RoutePoint)
        {
            onDeleteTile((RoutePoint) selected);
        }
        else if (selected instanceof Integer)
        {
            onDeleteLap((Integer) selected);
        }
    }
    
    private void saveChanges()
    {
        String name = nameField.getText().trim();
        if (name.isEmpty())
        {
            return;
        }
        
        String description = descriptionField.getText().trim();
        String trialName = (String) trialComboBox.getSelectedItem();
        
        // Update route
        route.setName(name);
        route.setDescription(description);
        route.setTrialName(trialName);
        
        // Save route to disk
        routeManager.updateRoute(route);
        
        onSave.run();
    }
    
    private void onRecord()
    {
        if (recording)
        {
            return;
        }
        
        // Show warning dialog about clearing existing tiles
        int result = JOptionPane.showConfirmDialog(
            this,
            "Warning: Recording will clear all existing tiles in this route. Do you want to continue?",
            "Clear Existing Tiles?",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result != JOptionPane.OK_OPTION)
        {
            return;
        }
        
        // Clear existing tiles and reset to lap 1
        route.getPoints().clear();
        emptyLaps.clear();
        currentLap = 1;
        populateTilesList();
        
        recording = true;
        lastRecordedPoint = null;
        updateButtons();
        
        // Show stop button, hide record button
        recordButton.setVisible(false);
        stopRecordButton.setVisible(true);
        
        // Start timer to record player position periodically
        recordingTimer = new Timer(100, e -> {
            if (!recording || route == null)
            {
                return;
            }
            if (plugin.getClient().getLocalPlayer() == null)
            {
                return;
            }
            int x = plugin.getClient().getLocalPlayer().getWorldLocation().getX();
            int y = plugin.getClient().getLocalPlayer().getWorldLocation().getY();
            int plane = plugin.getClient().getPlane();
            RoutePoint newPoint = new RoutePoint(x, y, plane, currentLap);
            
            // Only add if player moved (avoid duplicate points)
            if (lastRecordedPoint == null || 
                lastRecordedPoint.getX() != x || 
                lastRecordedPoint.getY() != y || 
                lastRecordedPoint.getPlane() != plane)
            {
                route.addPoint(newPoint);
                lastRecordedPoint = newPoint;
                // Remove from empty laps since we're adding a tile
                routeManager.updateRoute(route);
                emptyLaps.remove(currentLap);
                // Update tiles list
                populateTilesList();
            }
        });
        recordingTimer.start();
    }
    
    private void onStopRecord()
    {
        if (!recording)
        {
            return;
        }
        recording = false;
        if (recordingTimer != null)
        {
            recordingTimer.stop();
            recordingTimer = null;
        }
        updateButtons();
        
        // Show record button, hide stop button
        recordButton.setVisible(true);
        stopRecordButton.setVisible(false);
    }
    
    private void onAddTile()
    {
        if (plugin.getClient().getLocalPlayer() == null)
        {
            return;
        }
        int x = plugin.getClient().getLocalPlayer().getWorldLocation().getX();
        int y = plugin.getClient().getLocalPlayer().getWorldLocation().getY();
        int plane = plugin.getClient().getPlane();
        route.addPoint(new RoutePoint(x, y, plane, currentLap));
        // Remove from empty laps since we're adding a tile
        emptyLaps.remove(currentLap);
        routeManager.updateRoute(route);
        populateTilesList();
    }
    
    private void onNewLap()
    {
        // Find the highest lap number
        int maxLap = 1;
        for (RoutePoint point : route.getPoints())
        {
            if (point.getLap() > maxLap)
            {
                maxLap = point.getLap();
            }
        }
        if (!emptyLaps.isEmpty())
        {
            int maxEmptyLap = java.util.Collections.max(emptyLaps);
            if (maxEmptyLap > maxLap)
            {
                maxLap = maxEmptyLap;
            }
        }
        currentLap = maxLap + 1;
        emptyLaps.add(currentLap);
        populateTilesList();
    }
    
    private void onDeleteLap(int lap)
    {
        // Show confirmation dialog
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete Lap " + lap + " and all its tiles?",
            "Delete Lap",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result != JOptionPane.YES_OPTION)
        {
            return;
        }
        
        // Remove all tiles in this lap
        route.getPoints().removeIf(point -> point.getLap() == lap);
        // Remove from empty laps if it was empty
        emptyLaps.remove(lap);
        routeManager.setSelectedTile(null);
        selectedLap = null;
        
        // If currentLap was the deleted lap, reset to 1
        if (currentLap == lap)
        {
            currentLap = 1;
        }
        
        routeManager.updateRoute(route);
        populateTilesList();
    }
    
    private void onDeleteTile(RoutePoint point)
    {
        if (point == null)
        {
            return;
        }
        
        // Show confirmation dialog
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this tile?",
            "Delete Tile",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result != JOptionPane.YES_OPTION)
        {
            return;
        }
        
        route.getPoints().remove(point);
        routeManager.setSelectedTile(null);
        selectedLap = null;
        routeManager.updateRoute(route);
        populateTilesList();
    }
    
    private void onEditTile(RoutePoint point)
    {
        if (point == null)
        {
            return;
        }
        
        // Create dialog for editing tile coordinates
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = parentWindow instanceof Frame ? (Frame) parentWindow : null;
        JDialog dialog = new JDialog(parentFrame, "Edit Tile", true);
        dialog.setModal(true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // X field
        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("X:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JTextField xField = new JTextField(10);
        xField.setText(String.valueOf(point.getX()));
        dialog.add(xField, gbc);
        
        // Y field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        dialog.add(new JLabel("Y:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JTextField yField = new JTextField(10);
        yField.setText(String.valueOf(point.getY()));
        dialog.add(yField, gbc);
        
        // Plane field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        dialog.add(new JLabel("Plane:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JTextField planeField = new JTextField(10);
        planeField.setText(String.valueOf(point.getPlane()));
        dialog.add(planeField, gbc);
        
        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, gbc);
        
        // Button actions
        okButton.addActionListener(e -> {
            try
            {
                int x = Integer.parseInt(xField.getText().trim());
                int y = Integer.parseInt(yField.getText().trim());
                int plane = Integer.parseInt(planeField.getText().trim());
                
                point.setX(x);
                point.setY(y);
                point.setPlane(plane);
                
                // Update the list display
                populateTilesList();
                
                // Update selected tile if it's the one being edited
                if (routeManager.getSelectedTile() == point)
                {
                    routeManager.setSelectedTile(point);
                    restoreSelection();
                }
                
                dialog.dispose();
            }
            catch (NumberFormatException ex)
            {
                JOptionPane.showMessageDialog(dialog, "Invalid number format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame != null ? parentFrame : this);
        dialog.setVisible(true);
    }
    
    private void onEditLap(int lap)
    {
        // Get current color (custom or default)
        Color currentColor = route.getLapColor(lap);
        if (currentColor == null)
        {
            // Use default color for this lap
            Color[] defaultColors = {
                Color.RED,      // Lap 1
                Color.BLUE,     // Lap 2
                Color.GREEN,    // Lap 3
                Color.WHITE,    // Lap 4
                Color.BLACK,    // Lap 5
                new Color(128, 0, 128), // Lap 6 - Purple
                new Color(255, 165, 0)  // Lap 7 - Orange
            };
            int colorIndex = (lap - 1) % defaultColors.length;
            currentColor = defaultColors[colorIndex];
        }
        
        // Use RuneLite's color picker
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        net.runelite.client.ui.components.colorpicker.RuneliteColorPicker colorPicker = colorPickerManager.create(
            parentWindow != null ? parentWindow : this,
            currentColor,
            "Lap " + lap,
            true // alphaHidden - hide alpha channel
        );
        
        // Set the location relative to the panel
        if (parentWindow != null)
        {
            colorPicker.setLocation(parentWindow.getLocationOnScreen());
        }
        else
        {
            colorPicker.setLocation(this.getLocationOnScreen());
        }
        
           // Set callback for when color changes
           colorPicker.setOnColorChange((Color selectedColor) -> {
               if (selectedColor != null)
               {
                   route.setLapColor(lap, selectedColor);
                   routeManager.updateRoute(route);
               }
           });
        
        // Show the color picker
        colorPicker.setVisible(true);
    }
    
    private void updateButtons()
    {
        recordButton.setEnabled(!recording);
        stopRecordButton.setEnabled(recording);
    }
    
    // Transfer handler for drag and drop reordering
    private class TileListTransferHandler extends TransferHandler
    {
        @Override
        public int getSourceActions(JComponent c)
        {
            return MOVE;
        }
        
        @Override
        protected Transferable createTransferable(JComponent c)
        {
            JList<?> list = (JList<?>) c;
            Object selected = list.getSelectedValue();
            if (selected == null)
            {
                return null;
            }
            
            // Store the index and value for later use
            int index = list.getSelectedIndex();
            return new TileTransferable(selected, index);
        }
        
        @Override
        protected void exportDone(JComponent source, Transferable data, int action)
        {
            // The move has been completed in importData
        }
        
        @Override
        public boolean canImport(TransferSupport support)
        {
            return support.isDrop() && support.isDataFlavorSupported(TileTransferable.TILE_DATA_FLAVOR);
        }
        
        @Override
        public boolean importData(TransferSupport support)
        {
            if (!canImport(support))
            {
                return false;
            }
            
            try
            {
                TileTransferable transferable = (TileTransferable) support.getTransferable().getTransferData(TileTransferable.TILE_DATA_FLAVOR);
                Object draggedItem = transferable.getValue();
                
                JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
                int dropIndex = dropLocation.getIndex();
                
                // Determine target lap
                int targetLap = 1;
                
                // If dropping on an item, get its lap
                if (dropIndex < listModel.size() && dropIndex >= 0)
                {
                    Object targetItem = listModel.get(dropIndex);
                    if (targetItem instanceof Integer)
                    {
                        targetLap = (Integer) targetItem;
                    }
                    else if (targetItem instanceof RoutePoint)
                    {
                        targetLap = ((RoutePoint) targetItem).getLap();
                    }
                }
                // If dropping at the end, use the last item's lap or 1
                else if (listModel.size() > 0)
                {
                    Object lastItem = listModel.get(listModel.size() - 1);
                    if (lastItem instanceof Integer)
                    {
                        targetLap = (Integer) lastItem;
                    }
                    else if (lastItem instanceof RoutePoint)
                    {
                        targetLap = ((RoutePoint) lastItem).getLap();
                    }
                }
                
                // Handle dragging lap dividers
                if (draggedItem instanceof Integer)
                {
                    int draggedLap = (Integer) draggedItem;
                    
                    // Get all tiles in the dragged lap
                    java.util.List<RoutePoint> tilesInLap = new java.util.ArrayList<>();
                    for (RoutePoint point : route.getPoints())
                    {
                        if (point.getLap() == draggedLap)
                        {
                            tilesInLap.add(point);
                        }
                    }
                    
                    // Remove all tiles in the dragged lap
                    route.getPoints().removeAll(tilesInLap);
                    
                    // Update all tiles in the dragged lap to the target lap
                    for (RoutePoint point : tilesInLap)
                    {
                        point.setLap(targetLap);
                    }
                    
                    // Re-insert tiles
                    route.getPoints().addAll(tilesInLap);
                    
                    routeManager.updateRoute(route);
                    // Refresh the list
                    populateTilesList();
                    
                    return true;
                }
                // Handle dragging tiles
                else if (draggedItem instanceof RoutePoint)
                {
                    RoutePoint tile = (RoutePoint) draggedItem;
                    
                    // Update the tile's lap
                    tile.setLap(targetLap);
                    
                    // Remove from old position
                    route.getPoints().remove(tile);
                    
                    // Re-insert at the end of the target lap
                    int insertIndex = 0;
                    for (RoutePoint p : route.getPoints())
                    {
                        if (p.getLap() <= targetLap)
                        {
                            insertIndex++;
                        }
                        else
                        {
                            break;
                        }
                    }
                    route.getPoints().add(insertIndex, tile);
                    
                    routeManager.updateRoute(route);
                    // Refresh the list
                    populateTilesList();
                    
                    // Update selection
                    routeManager.setSelectedTile(tile);
                    restoreSelection();
                    
                    return true;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            
            return false;
        }
    }
    
    // Custom Transferable for list items
    private static class TileTransferable implements Transferable
    {
        private static final DataFlavor TILE_DATA_FLAVOR = new DataFlavor(Object.class, "Tile/Lap Item");
        private final Object value;
        
        public TileTransferable(Object value, int index)
        {
            this.value = value;
        }
        
        public Object getValue()
        {
            return value;
        }
        
        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[]{TILE_DATA_FLAVOR};
        }
        
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return TILE_DATA_FLAVOR.equals(flavor);
        }
        
        @Override
        public Object getTransferData(DataFlavor flavor) throws java.awt.datatransfer.UnsupportedFlavorException, java.io.IOException
        {
            if (!isDataFlavorSupported(flavor))
            {
                throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
            }
            return this;
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
    
    private void onBack()
    {
        // Stop recording if currently recording
        if (recording)
        {
            onStopRecord();
        }
        
        // If this is a new route and name is not set, remove it
        if (isNewRoute)
        {
            String name = nameField.getText().trim();
            if (name.isEmpty())
            {
                // Remove the route since it has no name
                routeManager.removeRoute(route);
                // Call onSave to update the list (remove from list)
                onSave.run();
                onBack.run();
                return;
            }
        }
        
        // Save changes before going back
        saveChanges();
        onBack.run();
    }
    
    public void cleanup()
    {
        if (recording && recordingTimer != null)
        {
            recordingTimer.stop();
            recordingTimer = null;
        }
        // Clear selected tile when leaving edit panel
        routeManager.setSelectedTile(null);
        selectedLap = null;
    }
}

