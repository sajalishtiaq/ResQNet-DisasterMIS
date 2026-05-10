package com.disastermis.controller;

import com.disastermis.dao.*;
import com.disastermis.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class OperatorDashboardController implements Initializable {

    @FXML private Label userNameLabel;
    @FXML private Label pendingLabel, activeLabel, criticalLabel;

    @FXML private TableView<ObservableList<String>> reportsTable;

    @FXML private TextField latField, lonField, descField;
    @FXML private ComboBox<String> disasterTypeBox, severityBox, citizenBox, eventBox;

    @FXML private TableView<ObservableList<String>> teamTable;
    @FXML private ComboBox<String> teamAssignBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userNameLabel.setText("Welcome, " + SessionManager.getInstance().getFullName());
        loadStats();
        setupReportsTable();
        setupTeamTable();
        initFormCombos();
    }

    private void loadStats() {
        try {
            int[] c = EmergencyReportDAO.getDashboardCounts();
            pendingLabel.setText(String.valueOf(c[0]));
            activeLabel.setText(String.valueOf(c[1]));
            criticalLabel.setText(String.valueOf(c[4]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupReportsTable() {
        // Cols: ID, Disaster Type, Severity, Time, Status, Description, Reported By, Event
        String[] cols   = {"ID",  "Disaster Type", "Severity", "Time",  "Status", "Description", "Reported By", "Event"};
        double[] widths = { 45,    130,              85,         150,     95,        200,            130,           130};
        buildTable(reportsTable, cols, widths);
        refreshReports();
    }

    private void refreshReports() {
        populateTable(reportsTable, EmergencyReportDAO.getActiveReports());
    }

    private void setupTeamTable() {
        // Cols: ID, Team Name, Type, Status, Capacity, Contact, Members, Latitude, Longitude
        String[] cols   = {"ID",  "Team Name", "Type",  "Status",   "Capacity", "Contact",  "Members", "Latitude", "Longitude"};
        double[] widths = { 45,    170,          110,     110,         80,         130,         80,        110,        110};
        buildTable(teamTable, cols, widths);
        populateTable(teamTable, RescueTeamDAO.getAllTeams());
    }

    private void initFormCombos() {
        disasterTypeBox.setItems(FXCollections.observableArrayList(
            "Flood", "Earthquake", "Fire", "Landslide", "Heatwave", "Cyclone", "Other"));
        severityBox.setItems(FXCollections.observableArrayList("Critical", "High", "Medium", "Low"));

        citizenBox.getItems().clear();
        citizenBox.getItems().add("0 - None");
        for (String[] c : EmergencyReportDAO.getCitizens())
            citizenBox.getItems().add(c[0] + " - " + c[1]);

        eventBox.getItems().clear();
        eventBox.getItems().add("0 - None");
        for (String[] e : EmergencyReportDAO.getDisasterEvents())
            eventBox.getItems().add(e[0] + " - " + e[1]);

        teamAssignBox.getItems().clear();
        for (String[] t : RescueTeamDAO.getAvailableTeams())
            teamAssignBox.getItems().add(t[0] + " - " + t[1] + " (" + t[2] + ")");
    }

    @FXML
    private void handleSubmitReport() {
        try {
            String latStr = latField.getText().trim();
            String lonStr = lonField.getText().trim();
            if (latStr.isEmpty() || lonStr.isEmpty()) {
                AlertHelper.showError("Validation", "Latitude and longitude are required.");
                return;
            }
            double lat   = Double.parseDouble(latStr);
            double lon   = Double.parseDouble(lonStr);
            String dtype = disasterTypeBox.getValue();
            String sev   = severityBox.getValue();
            String desc  = descField.getText().trim();

            if (dtype == null || sev == null) {
                AlertHelper.showError("Validation", "Select disaster type and severity.");
                return;
            }

            Integer citizenId = parseComboId(citizenBox.getValue());
            Integer eventId   = parseComboId(eventBox.getValue());

            if (EmergencyReportDAO.submitReport(lat, lon, dtype, sev, desc, citizenId, eventId)) {
                AlertHelper.showInfo("Success", "Emergency report submitted successfully.");
                latField.clear(); lonField.clear(); descField.clear();
                disasterTypeBox.setValue(null); severityBox.setValue(null);
                citizenBox.setValue(null); eventBox.setValue(null);
                refreshReports();
                loadStats();
            } else {
                AlertHelper.showError("Error", "Failed to submit report.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation", "Enter valid numeric latitude and longitude.");
        }
    }

    @FXML
    private void handleAssignTeam() {
        ObservableList<String> reportRow = reportsTable.getSelectionModel().getSelectedItem();
        String teamStr = teamAssignBox.getValue();

        if (reportRow == null) {
            AlertHelper.showError("Select Report", "Please select a report from the Dashboard tab first.");
            return;
        }
        if (teamStr == null) {
            AlertHelper.showError("Select Team", "Please select an available team from the dropdown.");
            return;
        }
        if ("Resolved".equals(reportRow.get(4))) {
            AlertHelper.showError("Invalid", "Cannot assign a team to a resolved report.");
            return;
        }

        int reportId = Integer.parseInt(reportRow.get(0));
        int teamId   = Integer.parseInt(teamStr.split(" - ")[0]);
        int userId   = SessionManager.getInstance().getUserId();

        if (RescueTeamDAO.assignTeam(teamId, reportId, userId)) {
            EmergencyReportDAO.updateStatus(reportId, "In Progress");
            AlertHelper.showInfo("Success", "Team assigned. Report status updated to In Progress.");
            setupTeamTable();   // rebuild with correct column widths
            initFormCombos();
            refreshReports();
            loadStats();
        } else {
            AlertHelper.showError("Error", "Assignment failed. Team may no longer be available.");
        }
    }

    @FXML
    private void handleMarkResolved() {
        ObservableList<String> row = reportsTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertHelper.showError("Select", "Please select a report to mark as resolved.");
            return;
        }
        if ("Resolved".equals(row.get(4))) {
            AlertHelper.showError("Already Resolved", "This report is already resolved.");
            return;
        }
        int reportId = Integer.parseInt(row.get(0));
        if (EmergencyReportDAO.updateStatus(reportId, "Resolved")) {
            AlertHelper.showInfo("Success", "Report marked as Resolved.");
            refreshReports();
            loadStats();
        } else {
            AlertHelper.showError("Error", "Failed to update report status.");
        }
    }

    @FXML
    private void handleRefresh() {
        setupReportsTable();
        setupTeamTable();
        loadStats();
        initFormCombos();
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

    /**
     * Builds a TableView with explicit per-column widths.
     * UNCONSTRAINED_RESIZE_POLICY keeps each column at its defined width —
     * CONSTRAINED_RESIZE_POLICY was squishing all columns equally, hiding middle columns.
     */
    @SuppressWarnings("unchecked")
    private void buildTable(TableView<ObservableList<String>> table,
                            String[] columns, double[] widths) {
        table.getColumns().clear();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        for (int i = 0; i < columns.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(columns[i]);
            col.setCellValueFactory(data ->
                new SimpleStringProperty(
                    data.getValue().size() > idx ? data.getValue().get(idx) : ""));
            col.setPrefWidth(widths[i]);
            col.setMinWidth(40);
            table.getColumns().add(col);
        }
    }

    private void populateTable(TableView<ObservableList<String>> table, List<String[]> data) {
        ObservableList<ObservableList<String>> items = FXCollections.observableArrayList();
        for (String[] row : data) items.add(FXCollections.observableArrayList(row));
        table.setItems(items);
    }
}