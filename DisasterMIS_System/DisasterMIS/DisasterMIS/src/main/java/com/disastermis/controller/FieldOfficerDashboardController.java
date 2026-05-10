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

public class FieldOfficerDashboardController implements Initializable {

    @FXML private Label userNameLabel;

    // Team status
    @FXML private TableView<ObservableList<String>> teamTable;

    // Assignment history
    @FXML private TableView<ObservableList<String>> assignmentTable;

    // Activity logs
    @FXML private TableView<ObservableList<String>> activityTable;
    @FXML private TextField outcomeSummaryField;

    // Hospital capacity
    @FXML private TableView<ObservableList<String>> hospitalTable;

    // Patients
    @FXML private TableView<ObservableList<String>> patientTable;
    @FXML private TextField patFirstName, patLastName, patAge, patCondition;
    @FXML private ComboBox<String> patGender, admitHospital, admitReport;

    // Approval requests
    @FXML private TextArea requestRemarks;
    @FXML private ComboBox<String> requestType;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userNameLabel.setText("Welcome, " + SessionManager.getInstance().getFullName());
        setupAllTables();
        initCombos();
    }

    private void setupAllTables() {
        buildTable(teamTable,
            new String[]{"ID", "Team Name", "Type", "Status", "Capacity", "Contact", "Members", "Lat", "Lon"});
        populateTable(teamTable, RescueTeamDAO.getAllTeams());

        buildTable(assignmentTable,
            new String[]{"ID", "Team", "Type", "Disaster", "Severity", "Assigned At", "Completed", "Assigned By"});
        populateTable(assignmentTable, RescueTeamDAO.getAssignmentHistory());

        buildTable(activityTable,
            new String[]{"ID", "Team", "Start", "End", "Status", "Outcome"});
        populateTable(activityTable, RescueTeamDAO.getActivityLogs());

        buildTable(hospitalTable,
            new String[]{"ID", "Hospital", "City", "Total Beds", "Em.Capacity", "Current", "Available"});
        populateTable(hospitalTable, HospitalDAO.getHospitalCapacity());

        buildTable(patientTable,
            new String[]{"ID", "Name", "Age", "Gender", "Condition"});
        populateTable(patientTable, HospitalDAO.getAllPatients());
    }

    private void initCombos() {
        patGender.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));

        admitHospital.getItems().clear();
        for (String[] h : HospitalDAO.getHospitalCapacity())
            admitHospital.getItems().add(h[0] + " - " + h[1]);

        admitReport.getItems().clear();
        admitReport.getItems().add("0 - None");
        for (String[] r : EmergencyReportDAO.getActiveReports())
            admitReport.getItems().add(r[0] + " - " + r[1] + " (" + r[2] + ")");

        requestType.setItems(FXCollections.observableArrayList(
            "Resource Distribution", "Rescue Deployment", "Financial Approval"));
    }

    @FXML
    private void handleMarkActivityComplete() {
        ObservableList<String> row = activityTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertHelper.showError("Select", "Select an activity log to mark complete.");
            return;
        }
        String currentStatus = row.get(4);
        if ("Completed".equals(currentStatus)) {
            AlertHelper.showError("Already Done", "This activity is already completed.");
            return;
        }
        String summary = outcomeSummaryField.getText().trim();
        if (summary.isEmpty()) {
            AlertHelper.showError("Outcome Required", "Enter an outcome summary.");
            return;
        }
        int actId = Integer.parseInt(row.get(0));
        if (RescueTeamDAO.markActivityComplete(actId, summary)) {
            AlertHelper.showInfo("Success", "Activity marked complete. Team is now Available.");
            outcomeSummaryField.clear();
            populateTable(activityTable, RescueTeamDAO.getActivityLogs());
            populateTable(teamTable, RescueTeamDAO.getAllTeams());
            populateTable(assignmentTable, RescueTeamDAO.getAssignmentHistory());
        } else {
            AlertHelper.showError("Error", "Could not update activity. It may already be completed.");
        }
    }

    @FXML
    private void handleCreatePatient() {
        try {
            String fn   = patFirstName.getText().trim();
            String ln   = patLastName.getText().trim();
            String ageStr = patAge.getText().trim();
            String cond = patCondition.getText().trim();
            String gender = patGender.getValue();

            if (fn.isEmpty() || ln.isEmpty()) {
                AlertHelper.showError("Validation", "First and last name are required.");
                return;
            }
            if (gender == null) {
                AlertHelper.showError("Validation", "Please select gender.");
                return;
            }

            int age = 0;
            if (!ageStr.isEmpty()) age = Integer.parseInt(ageStr);

            if (HospitalDAO.createPatient(fn, ln, age, gender, cond)) {
                AlertHelper.showInfo("Success", "Patient record created successfully.");
                patFirstName.clear();
                patLastName.clear();
                patAge.clear();
                patCondition.clear();
                patGender.setValue(null);
                populateTable(patientTable, HospitalDAO.getAllPatients());
            } else {
                AlertHelper.showError("Error", "Failed to create patient record.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation", "Enter a valid numeric age.");
        }
    }

    @FXML
    private void handleAdmitPatient() {
        ObservableList<String> row = patientTable.getSelectionModel().getSelectedItem();
        String hospStr = admitHospital.getValue();

        if (row == null) {
            AlertHelper.showError("Select Patient", "Please select a patient from the table.");
            return;
        }
        if (hospStr == null) {
            AlertHelper.showError("Select Hospital", "Please select a hospital.");
            return;
        }

        int patientId  = Integer.parseInt(row.get(0));
        int hospitalId = Integer.parseInt(hospStr.split(" - ")[0]);
        Integer reportId = parseComboId(admitReport.getValue());
        int userId = SessionManager.getInstance().getUserId();

        if (HospitalDAO.admitPatient(patientId, hospitalId, reportId, userId)) {
            AlertHelper.showInfo("Success", "Patient admitted successfully.");
            populateTable(hospitalTable, HospitalDAO.getHospitalCapacity());
            initCombos(); // Refresh hospital list
        } else {
            AlertHelper.showError("Admission Failed", 
                "Patient is already admitted to a hospital or the selected hospital is at full capacity.");
        }
    }
    @FXML
    private void handleSubmitRequest() {
        String type    = requestType.getValue();
        String remarks = requestRemarks.getText().trim();

        if (type == null) {
            AlertHelper.showError("Validation", "Please select a request type.");
            return;
        }
        if (remarks.isEmpty()) {
            AlertHelper.showError("Validation", "Please enter remarks for the request.");
            return;
        }

        int userId = SessionManager.getInstance().getUserId();
        if (ApprovalDAO.createRequest(type, userId, remarks)) {
            AlertHelper.showInfo("Success", "Approval request submitted successfully.");
            requestRemarks.clear();
            requestType.setValue(null);
        } else {
            AlertHelper.showError("Error", "Failed to submit request.");
        }
    }

    @FXML
    private void handleRefresh() {
        setupAllTables();
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

    @SuppressWarnings("unchecked")
    private void buildTable(TableView<ObservableList<String>> table, String[] columns) {
        table.getColumns().clear();
        for (int i = 0; i < columns.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(columns[i]);
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    data.getValue().size() > idx ? data.getValue().get(idx) : ""));
            col.setPrefWidth(120);
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void populateTable(TableView<ObservableList<String>> table, List<String[]> data) {
        ObservableList<ObservableList<String>> items = FXCollections.observableArrayList();
        for (String[] row : data) items.add(FXCollections.observableArrayList(row));
        table.setItems(items);
    }
}
