package com.disastermis.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String SERVER   = "LALEYKA\\SQLEXPRESS";
    private static final String DATABASE = "DisasterMIS";
    private static final String USER     = "app_user";
    private static final String PASSWORD = "sajal1234567";

    private static final String URL =
            "jdbc:sqlserver://" + SERVER + ";"
          + "databaseName=" + DATABASE + ";"
          + "encrypt=true;"
          + "trustServerCertificate=true;"
          + "loginTimeout=10;";

    private static Connection connection = null;

    public static synchronized Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                if (connection != null) {
                    try { connection.close(); } catch (Exception ignored) {}
                }
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            // Try once more fresh
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
