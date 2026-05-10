package com.disastermis.controller;

import com.disastermis.dao.UserDAO;
import com.disastermis.util.AlertHelper;
import com.disastermis.util.SceneManager;
import com.disastermis.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            AlertHelper.showError("Login Failed", "Please enter both email and password.");
            return;
        }

        try {
            // Returns [UserID, FullName, Email, RoleID, RoleName] or null
            String[] user = UserDAO.authenticate(email, pass);
            if (user != null) {
                int    userId   = Integer.parseInt(user[0]);
                String fullName = user[1];
                String userEmail= user[2];
                int    roleId   = Integer.parseInt(user[3]);
                String roleName = user[4];

                SessionManager.getInstance().login(userId, fullName, userEmail, roleId, roleName);
                redirectByRole(roleName);
            } else {
                AlertHelper.showError("Login Failed", "Invalid email or password.");
            }
        } catch (Exception e) {
            AlertHelper.showError("Connection Error", "Cannot connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void redirectByRole(String role) {
        switch (role) {
            case "Administrator"      -> SceneManager.switchTo("AdminDashboard.fxml",        "Administrator Dashboard");
            case "Emergency Operator" -> SceneManager.switchTo("OperatorDashboard.fxml",     "Emergency Operator Dashboard");
            case "Field Officer"      -> SceneManager.switchTo("FieldOfficerDashboard.fxml", "Field Officer Dashboard");
            case "Warehouse Manager"  -> SceneManager.switchTo("WarehouseDashboard.fxml",    "Warehouse Manager Dashboard");
            case "Finance Officer"    -> SceneManager.switchTo("FinanceDashboard.fxml",      "Finance Officer Dashboard");
            default                   -> AlertHelper.showError("Error", "Unknown role: " + role);
        }
    }
}
