package com.disastermis.controller;

import com.disastermis.dao.*;
import com.disastermis.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class WarehouseDashboardController implements Initializable {

    @FXML private Label userNameLabel;
    @FXML private Label lowStockCountLabel;

    // Inventory
    @FXML private TableView<ObservableList<String>> inventoryTable;

    // Low stock
    @FXML private TableView<ObservableList<String>> lowStockTable;

    // Allocations
    @FXML private TableView<ObservableList<String>> allocationTable;

    // Allocate resource form
    @FXML private ComboBox<String> allocResourceBox, allocWarehouseBox, allocEventBox;
    @FXML private TextField allocQtyField;

    // Restock form
    @FXML private ComboBox<String> restockResourceBox, restockWarehouseBox;
    @FXML private TextField restockQtyField;

    // Add Resource form
    @FXML private TextField resNameField, resUnitField, resThresholdField;
    @FXML private ComboBox<String> resTypeBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userNameLabel.setText("Welcome, " + SessionManager.getInstance().getFullName());
        setupTables();
        initCombos();
        refreshLowStockCount();
    }

    private void setupTables() {
        // === Full Inventory Summary ===
        buildTable(inventoryTable, new String[]{
            "Warehouse", "City", "Resource", "Type", "Unit",
            "Available", "Dispatched", "Consumed", "Threshold", "Status", "Updated"}, 
            new double[]{140, 90, 160, 100, 60, 85, 85, 85, 80, 90, 140});

        populateTable(inventoryTable, ResourceDAO.getInventorySummary());

        // === Low Stock ===
        buildTable(lowStockTable, new String[]{
            "Warehouse", "Resource", "Type", "Unit", "Available", "Threshold"}, 
            new double[]{140, 160, 100, 70, 90, 90});

        populateTable(lowStockTable, ResourceDAO.getLowStockItems());

        // === Allocation History ===
        buildTable(allocationTable, new String[]{
            "ID", "Resource", "Warehouse", "Qty", "Allocated At", "Event", "By"}, 
            new double[]{50, 160, 140, 60, 140, 140, 120});

        populateTable(allocationTable, ResourceDAO.getAllocationHistory());
    }

    private void initCombos() {
        allocResourceBox.getItems().clear();
        restockResourceBox.getItems().clear();
        for (String[] r : ResourceDAO.getAllResources()) {
            String item = r[0] + " - " + r[1] + " (" + r[2] + ")";
            allocResourceBox.getItems().add(item);
            restockResourceBox.getItems().add(item);
        }

        allocWarehouseBox.getItems().clear();
        restockWarehouseBox.getItems().clear();
        for (String[] w : ResourceDAO.getAllWarehouses()) {
            String item = w[0] + " - " + w[1];
            allocWarehouseBox.getItems().add(item);
            restockWarehouseBox.getItems().add(item);
        }

        allocEventBox.getItems().clear();
        allocEventBox.getItems().add("0 - None");
        for (String[] e : EmergencyReportDAO.getDisasterEvents())
            allocEventBox.getItems().add(e[0] + " - " + e[1]);

        resTypeBox.setItems(FXCollections.observableArrayList(
            "Food", "Water", "Medicine", "Shelter", "Equipment", "Other"));
    }

    private void refreshLowStockCount() {
        int count = ResourceDAO.getLowStockItems().size();
        lowStockCountLabel.setText(count + " LOW STOCK ITEMS");
    }

    // ==================== ACTION HANDLERS ====================

    @FXML
    private void handleAllocate() {
        try {
            String resStr   = allocResourceBox.getValue();
            String wareStr  = allocWarehouseBox.getValue();
            String qtyStr   = allocQtyField.getText().trim();

            if (resStr == null || wareStr == null || qtyStr.isEmpty()) {
                AlertHelper.showError("Validation", "Select resource, warehouse and enter quantity.");
                return;
            }

            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) {
                AlertHelper.showError("Validation", "Quantity must be greater than zero.");
                return;
            }

            int resourceId  = Integer.parseInt(resStr.split(" - ")[0]);
            int warehouseId = Integer.parseInt(wareStr.split(" - ")[0]);
            Integer eventId = parseComboId(allocEventBox.getValue());
            int userId      = SessionManager.getInstance().getUserId();

            if (ResourceDAO.allocateResource(resourceId, warehouseId, eventId, qty, userId, null, null)) {
                AlertHelper.showInfo("Success", "Resource allocated successfully.");
                clearAllocateForm();
                refreshAllTables();
            } else {
                AlertHelper.showError("Error", "Allocation failed. Insufficient stock.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation", "Enter a valid numeric quantity.");
        }
    }

    @FXML
    private void handleRestock() {
        try {
            String resStr  = restockResourceBox.getValue();
            String wareStr = restockWarehouseBox.getValue();
            String qtyStr  = restockQtyField.getText().trim();

            if (resStr == null || wareStr == null || qtyStr.isEmpty()) {
                AlertHelper.showError("Validation", "Select resource, warehouse and enter quantity.");
                return;
            }

            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) {
                AlertHelper.showError("Validation", "Quantity must be greater than zero.");
                return;
            }

            int resourceId  = Integer.parseInt(resStr.split(" - ")[0]);
            int warehouseId = Integer.parseInt(wareStr.split(" - ")[0]);

            if (ResourceDAO.updateInventory(warehouseId, resourceId, qty)) {
                AlertHelper.showInfo("Success", "Inventory restocked successfully.");
                restockQtyField.clear();
                restockResourceBox.setValue(null);
                restockWarehouseBox.setValue(null);
                refreshAllTables();
            } else {
                AlertHelper.showError("Error", "Failed to restock inventory.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation", "Enter a valid numeric quantity.");
        }
    }

    @FXML
    private void handleAddResource() {
        String name   = resNameField.getText().trim();
        String unit   = resUnitField.getText().trim();
        String type   = resTypeBox.getValue();
        String thrStr = resThresholdField.getText().trim();

        if (name.isEmpty() || unit.isEmpty() || type == null || thrStr.isEmpty()) {
            AlertHelper.showError("Validation", "Fill all resource fields.");
            return;
        }

        try {
            int threshold = Integer.parseInt(thrStr);
            if (threshold < 0) {
                AlertHelper.showError("Validation", "Threshold cannot be negative.");
                return;
            }
            if (ResourceDAO.addResource(name, type, unit, threshold)) {
                AlertHelper.showInfo("Success", "Resource added successfully.");
                resNameField.clear();
                resUnitField.clear();
                resThresholdField.clear();
                resTypeBox.setValue(null);
                initCombos();
                refreshAllTables();
            } else {
                AlertHelper.showError("Error", "Failed to add resource.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation", "Enter a valid numeric threshold.");
        }
    }

    private void clearAllocateForm() {
        allocQtyField.clear();
        allocResourceBox.setValue(null);
        allocWarehouseBox.setValue(null);
        allocEventBox.setValue(null);
    }

    private void refreshAllTables() {
        populateTable(inventoryTable, ResourceDAO.getInventorySummary());
        populateTable(lowStockTable, ResourceDAO.getLowStockItems());
        populateTable(allocationTable, ResourceDAO.getAllocationHistory());
        refreshLowStockCount();
    }

    @FXML
    private void handleRefresh() {
        refreshAllTables();
        initCombos();
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.switchTo("Login.fxml", "Login");
    }

    private Integer parseComboId(String val) {
        if (val == null) return null;
        int id = Integer.parseInt(val.split(" - ")[0]);
        return id == 0 ? null : id;
    }

    // ==================== TABLE BUILDER (FIXED) ====================

    @SuppressWarnings("unchecked")
    private void buildTable(TableView<ObservableList<String>> table, String[] columns, double[] widths) {
        table.getColumns().clear();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);  // ← Fixed

        for (int i = 0; i < columns.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(columns[i]);
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    data.getValue().size() > idx ? data.getValue().get(idx) : ""));
            col.setPrefWidth(widths[i]);
            col.setMinWidth(50);
            table.getColumns().add(col);
        }
    }

    private void populateTable(TableView<ObservableList<String>> table, List<String[]> data) {
        ObservableList<ObservableList<String>> items = FXCollections.observableArrayList();
        for (String[] row : data) {
            items.add(FXCollections.observableArrayList(row));
        }
        table.setItems(items);
    }
}