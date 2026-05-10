package com.disastermis.dao;

import com.disastermis.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {

    public static List<String[]> getRecentAuditTrail(int limit) {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP(?) al.LogID, al.ActionType, al.TableAffected, " +
                "al.OldValue, al.NewValue, " +
                "CONVERT(VARCHAR(23), al.LogTimestamp, 121) AS LogTimestamp, " +
                "ISNULL(u.FullName, " +
                "  CASE " +
                "    WHEN al.ActionType LIKE 'TEAM%'       THEN 'System (Team Trigger)' " +
                "    WHEN al.ActionType LIKE 'ALLOCATION%' THEN 'System (Inventory Trigger)' " +
                "    WHEN al.ActionType LIKE 'LOW_STOCK%'  THEN 'System (Stock Trigger)' " +
                "    WHEN al.ActionType LIKE 'FINANCIAL%'  THEN 'System (Finance Trigger)' " +
                "    WHEN al.ActionType LIKE 'APPROVAL%'   THEN 'System (Approval Trigger)' " +
                "    WHEN al.ActionType LIKE 'PATIENT%'    THEN 'System (Admission Trigger)' " +
                "    ELSE 'System (Trigger)' " +
                "  END) AS PerformedBy " +
                "FROM AUDITLOG al " +
                "LEFT JOIN [USER] u ON u.UserID = al.UserID " +
                "ORDER BY al.LogTimestamp DESC");
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("LogID"),
                    nullSafe(rs.getString("ActionType")),
                    nullSafe(rs.getString("TableAffected")),
                    nullSafe(rs.getString("OldValue")),
                    nullSafe(rs.getString("NewValue")),
                    nullSafe(rs.getString("LogTimestamp")),
                    rs.getString("PerformedBy")
                });
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getMISStats() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT DisasterType, SeverityLevel, COUNT(*) AS TotalReports " +
                "FROM EMERGENCYREPORT GROUP BY DisasterType, SeverityLevel " +
                "ORDER BY TotalReports DESC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("DisasterType"),
                    rs.getString("SeverityLevel"),
                    rs.getString("TotalReports")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getMonthlyTransactions() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT FORMAT(TransactionDate,'yyyy-MM') AS Month, " +
                "TransactionType, COUNT(*) AS TxnCount, SUM(Amount) AS TotalAmount " +
                "FROM FINANCIALTRANSACTION WHERE Status='Approved' " +
                "GROUP BY FORMAT(TransactionDate,'yyyy-MM'), TransactionType " +
                "ORDER BY Month");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("Month"),
                    rs.getString("TransactionType"),
                    rs.getString("TxnCount"),
                    rs.getString("TotalAmount")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}