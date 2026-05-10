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

public class AdminDashboardController implements Initializable {

    // Stats cards
    @FXML private Label pendingReportsLabel;
    @FXML private Label activeReportsLabel;
    @FXML private Label teamsAvailLabel;
    @FXML private Label pendingApprovalsLabel;
    @FXML private Label criticalLabel;
    @FXML private Label userNameLabel;

    // Users Tab
    @FXML private TableView<ObservableList<String>> usersTable;
    @FXML private TextField newUserName, newUserEmail, newUserPhone, newUserPassword;
    @FXML private ComboBox<String> newUserRole;

    // Approvals Tab
    @FXML private TableView<ObservableList<String>> approvalsTable;
    @FXML private TextField approvalRemarks;

    // Audit Tab
    @FXML private TableView<ObservableList<String>> auditTable;

    // Reports Tab
    @FXML private TableView<ObservableList<String>> reportsTable;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userNameLabel.setText("Welcome, " + SessionManager.getInstance().getFullName());
        loadStats();
        setupUsersTable();
        setupApprovalsTable();
        setupAuditTable();
        setupReportsTable();
        loadRoles();
    }

    private void loadStats() {
        try {
            int[] c = EmergencyReportDAO.getDashboardCounts();
            pendingReportsLabel.setText(String.valueOf(c[0]));
            activeReportsLabel.setText(String.valueOf(c[1]));
            teamsAvailLabel.setText(String.valueOf(c[2]));
            pendingApprovalsLabel.setText(String.valueOf(c[3]));
            criticalLabel.setText(String.valueOf(c[4]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUsersTable() {
        String[] cols = {"ID", "Full Name", "Email", "Phone", "Status", "Role"};
        buildTable(usersTable, cols);
        refreshUsers();
    }

    private void refreshUsers() {
        populateTable(usersTable, UserDAO.getAllUsers());
    }

    private void setupApprovalsTable() {
        String[] cols = {"ID", "Type", "Requested At", "Status", "Remarks", "Requested By", "Approved By"};
        buildTable(approvalsTable, cols);
        refreshApprovals();
    }

    private void refreshApprovals() {
        List<String[]> data = ApprovalDAO.getAllApprovals();  // This already includes pending transactions
        populateTable(approvalsTable, data);
    }

    private void setupAuditTable() {
        String[] cols = {"LogID", "Action", "Table", "Old Value", "New Value", "Timestamp", "Performed By"};
        buildTable(auditTable, cols);
        refreshAudit();
    }

    private void refreshAudit() {
        List<String[]> data = AuditDAO.getRecentAuditTrail(100);
        populateTable(auditTable, data);
    }

    private void setupReportsTable() {
        String[] cols = {"ID", "Disaster Type", "Severity", "Time", "Status", "Description", "Reported By", "Event"};
        buildTable(reportsTable, cols);
        populateTable(reportsTable, EmergencyReportDAO.getAllReports());
    }

    private void loadRoles() {
        newUserRole.getItems().clear();
        List<String[]> roles = UserDAO.getAllRoles();
        for (String[] r : roles) newUserRole.getItems().add(r[0] + " - " + r[1]);
    }

    @FXML
    private void handleCreateUser() {
        // ... (existing code remains same)
        String name    = newUserName.getText().trim();
        String email   = newUserEmail.getText().trim();
        String phone   = newUserPhone.getText().trim();
        String pass    = newUserPassword.getText();
        String roleStr = newUserRole.getValue();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || roleStr == null) {
            AlertHelper.showError("Validation", "Please fill all required fields.");
            return;
        }

        int roleId = Integer.parseInt(roleStr.split(" - ")[0]);
        if (UserDAO.createUser(name, email, pass, phone, roleId)) {
            AlertHelper.showInfo("Success", "User created successfully.");
            newUserName.clear(); newUserEmail.clear(); newUserPhone.clear(); newUserPassword.clear();
            newUserRole.setValue(null);
            refreshUsers();
        } else {
            AlertHelper.showError("Error", "Failed to create user.");
        }
    }

    // ==================== APPROVAL HANDLERS (UPDATED) ====================
    @FXML
    private void handleApprove() {
        ObservableList<String> row = approvalsTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertHelper.showError("Select", "Please select a request to approve.");
            return;
        }
        String status = row.get(3);
        if (!"Pending".equals(status)) {
            AlertHelper.showError("Already Processed", "This is already " + status + ".");
            return;
        }

        int id = Integer.parseInt(row.get(0));
        String type = row.get(1);           // "Donation", "Expense", etc. or RequestType
        String remarks = approvalRemarks.getText().trim();

        boolean success;
        if ("Donation".equals(type) || "Expense".equals(type) || "Procurement".equals(type)) {
            success = FinanceDAO.approveTransaction(id, SessionManager.getInstance().getUserId(), remarks);
        } else {
            success = ApprovalDAO.processApproval(id, SessionManager.getInstance().getUserId(), "Approved", remarks);
        }

        if (success) {
            AlertHelper.showInfo("Success", "Approved successfully!");
            approvalRemarks.clear();
            refreshApprovals();
            loadStats();
        } else {
            AlertHelper.showError("Error", "Failed to approve.");
        }
    }

    @FXML
    private void handleReject() {
        ObservableList<String> row = approvalsTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertHelper.showError("Select", "Please select a request.");
            return;
        }
        String status = row.get(3);
        if (!"Pending".equals(status)) {
            AlertHelper.showError("Already Processed", "This is already " + status + ".");
            return;
        }

        String remarks = approvalRemarks.getText().trim();
        if (remarks.isEmpty()) {
            AlertHelper.showError("Remarks Required", "Please provide rejection remarks.");
            return;
        }

        int id = Integer.parseInt(row.get(0));
        String type = row.get(1);

        boolean success;
        if ("Donation".equals(type) || "Expense".equals(type) || "Procurement".equals(type)) {
            success = FinanceDAO.rejectTransaction(id, SessionManager.getInstance().getUserId(), remarks);
        } else {
            success = ApprovalDAO.processApproval(id, SessionManager.getInstance().getUserId(), "Rejected", remarks);
        }

        if (success) {
            AlertHelper.showInfo("Success", "Rejected successfully.");
            approvalRemarks.clear();
            refreshApprovals();
            loadStats();
        } else {
            AlertHelper.showError("Error", "Failed to reject.");
        }
    }

    @FXML private void handleRefreshAudit() { refreshAudit(); }
    @FXML private void handleRefreshStats()  { 
        loadStats(); 
        refreshApprovals(); 
        setupReportsTable(); 
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.switchTo("Login.fxml", "Login");
    }

    // Generic Table Helpers
    @SuppressWarnings("unchecked")
    private void buildTable(TableView<ObservableList<String>> table, String[] columns) {
        table.getColumns().clear();
        for (int i = 0; i < columns.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(columns[i]);
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    data.getValue().size() > idx ? data.getValue().get(idx) : ""));
            col.setPrefWidth(130);
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void populateTable(TableView<ObservableList<String>> table, List<String[]> data) {
        ObservableList<ObservableList<String>> items = FXCollections.observableArrayList();
        for (String[] row : data) {
            items.add(FXCollections.observableArrayList(row));
        }
        table.setItems(items);
    }
}