package com.disastermis.controller;

import com.disastermis.dao.*;
import com.disastermis.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FinanceDashboardController implements Initializable {

    @FXML private Label userNameLabel;
    @FXML private Label totalDonationsLabel, totalExpensesLabel;

    @FXML private TableView<ObservableList<String>> txnTable;
    @FXML private TableView<ObservableList<String>> budgetTable;

    @FXML private TextField amountField, descField;
    @FXML private ComboBox<String> txnTypeBox, donorBox, supplierBox, eventBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userNameLabel.setText("Welcome, " + SessionManager.getInstance().getFullName());
        setupTables();
        initCombos();
        loadTotals();
    }

    private void setupTables() {
        // Transactions Table
        buildTable(txnTable, new String[]{
            "ID", "Type", "Amount", "Date", "Status", "Description", 
            "Donor", "Supplier", "Event", "Approved By"}, 
            new double[]{50, 90, 100, 140, 90, 180, 130, 120, 130, 120});

        populateTable(txnTable, FinanceDAO.getFinancialSummary());

        // Budget Analysis Table
        buildTable(budgetTable, new String[]{
            "Event ID", "Event Name", "Disaster", "Status", 
            "Budget", "Donations", "Expenses", "Remaining"}, 
            new double[]{60, 160, 100, 90, 110, 110, 110, 110});

        populateTable(budgetTable, FinanceDAO.getBudgetAnalysis());
    }

    private void initCombos() {
        txnTypeBox.setItems(FXCollections.observableArrayList("Donation", "Expense", "Procurement"));

        donorBox.getItems().clear();
        donorBox.getItems().add("0 - None");
        for (String[] d : FinanceDAO.getDonors())
            donorBox.getItems().add(d[0] + " - " + d[1]);

        supplierBox.getItems().clear();
        supplierBox.getItems().add("0 - None");
        for (String[] s : FinanceDAO.getSuppliers())
            supplierBox.getItems().add(s[0] + " - " + s[1]);

        eventBox.getItems().clear();
        eventBox.getItems().add("0 - None");
        for (String[] e : EmergencyReportDAO.getDisasterEvents())
            eventBox.getItems().add(e[0] + " - " + e[1]);
    }

    private void loadTotals() {
        try {
            String[] totals = FinanceDAO.getTotals();
            totalDonationsLabel.setText("PKR " + formatAmount(totals[0]));
            totalExpensesLabel.setText("PKR " + formatAmount(totals[1]));
        } catch (Exception e) {
            totalDonationsLabel.setText("PKR 0.00");
            totalExpensesLabel.setText("PKR 0.00");
        }
    }

    @FXML
    private void handleRecordTransaction() {
        try {
            String amtStr = amountField.getText().trim();
            String type   = txnTypeBox.getValue();
            String desc   = descField.getText().trim();

            if (amtStr.isEmpty() || type == null || desc.isEmpty()) {
                AlertHelper.showError("Validation", "Please fill all required fields.");
                return;
            }

            BigDecimal amount = new BigDecimal(amtStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertHelper.showError("Validation", "Amount must be greater than zero.");
                return;
            }

            // === NEW: Prevent Expense larger than available funds ===
            if ("Expense".equals(type) || "Procurement".equals(type)) {
                String[] totals = FinanceDAO.getTotals();
                BigDecimal totalDonations = new BigDecimal(totals[0]);
                if (amount.compareTo(totalDonations) > 0) {
                    AlertHelper.showError("Budget Exceeded", 
                        "Expense amount (PKR " + amount + ") cannot be larger than Total Donations Received (PKR " + totalDonations + ").");
                    return;
                }
            }

            Integer donorId    = parseComboId(donorBox.getValue());
            Integer supplierId = parseComboId(supplierBox.getValue());
            Integer eventId    = parseComboId(eventBox.getValue());

            if (FinanceDAO.recordTransaction(amount, type, desc,
                    donorId, supplierId, eventId, null, null, null)) {
                
                AlertHelper.showInfo("Success", "Transaction recorded successfully as Pending.");
                
                amountField.clear();
                descField.clear();
                txnTypeBox.setValue(null);
                donorBox.setValue(null);
                supplierBox.setValue(null);
                eventBox.setValue(null);

                refreshAll();
            } else {
                AlertHelper.showError("Error", "Failed to record transaction.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation", "Enter a valid numeric amount (e.g., 50000.00).");
        }
    }

    private void refreshAll() {
        populateTable(txnTable, FinanceDAO.getFinancialSummary());
        populateTable(budgetTable, FinanceDAO.getBudgetAnalysis());
        loadTotals();
    }

    @FXML
    private void handleRefresh() {
        refreshAll();
        initCombos();
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.switchTo("Login.fxml", "Login");
    }

    private Integer parseComboId(String val) {
        if (val == null || val.startsWith("0 -")) return null;
        try {
            return Integer.parseInt(val.split(" - ")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatAmount(String raw) {
        try {
            return String.format("%,.2f", new BigDecimal(raw));
        } catch (Exception e) {
            return "0.00";
        }
    }

    // ==================== FIXED TABLE BUILDER ====================
    @SuppressWarnings("unchecked")
    private void buildTable(TableView<ObservableList<String>> table, String[] columns, double[] widths) {
        table.getColumns().clear();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY); // Better for dynamic content

        for (int i = 0; i < columns.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(columns[i]);
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    data.getValue().size() > idx ? data.getValue().get(idx) : ""));
            col.setPrefWidth(widths[i]);
            col.setMinWidth(60);
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