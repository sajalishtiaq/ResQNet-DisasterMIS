package com.disastermis.dao;

import com.disastermis.db.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinanceDAO {

    public static List<String[]> getFinancialSummary() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT TransactionID, TransactionType, Amount, " +
                "CONVERT(VARCHAR(20), TransactionDate, 120) AS TransactionDate, " +
                "Status, Description, " +
                "ISNULL(DonorName, '-') AS DonorName, " +
                "ISNULL(SupplierName, '-') AS SupplierName, " +
                "ISNULL(EventName, '-') AS EventName, " +
                "ISNULL(ApprovedByUser, '-') AS ApprovedByUser " +
                "FROM vw_financial_summary ORDER BY TransactionDate DESC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("TransactionID"),
                    rs.getString("TransactionType"),
                    rs.getString("Amount"),
                    rs.getString("TransactionDate"),
                    rs.getString("Status"),
                    nullSafe(rs.getString("Description")),
                    rs.getString("DonorName"),
                    rs.getString("SupplierName"),
                    rs.getString("EventName"),
                    rs.getString("ApprovedByUser")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getBudgetAnalysis() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT EventID, EventName, DisasterType, Status, " +
                "TotalBudget, TotalDonations, TotalExpenses, RemainingBudget " +
                "FROM vw_event_budget_analysis ORDER BY TotalExpenses DESC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("EventID"),
                    rs.getString("EventName"),
                    rs.getString("DisasterType"),
                    rs.getString("Status"),
                    nullSafe(rs.getString("TotalBudget")),
                    nullSafe(rs.getString("TotalDonations")),
                    nullSafe(rs.getString("TotalExpenses")),
                    nullSafe(rs.getString("RemainingBudget"))
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean recordTransaction(BigDecimal amount, String type,
                                            String description, Integer donorId,
                                            Integer supplierId, Integer eventId,
                                            Integer resourceId, Integer approvedBy,
                                            Integer requestId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String status = (approvedBy != null) ? "Approved" : "Pending";

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO FINANCIALTRANSACTION " +
                "(Amount, TransactionType, Description, Status, ApprovedBy, " +
                "DonorID, SupplierID, EventID, ResourceID, RequestID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setBigDecimal(1, amount);
            ps.setString(2, type);
            ps.setString(3, description);
            ps.setString(4, status);
            if (approvedBy != null) ps.setInt(5, approvedBy); else ps.setNull(5, Types.INTEGER);
            if (donorId    != null) ps.setInt(6, donorId);    else ps.setNull(6, Types.INTEGER);
            if (supplierId != null) ps.setInt(7, supplierId); else ps.setNull(7, Types.INTEGER);
            if (eventId    != null) ps.setInt(8, eventId);    else ps.setNull(8, Types.INTEGER);
            if (resourceId != null) ps.setInt(9, resourceId); else ps.setNull(9, Types.INTEGER);
            if (requestId  != null) ps.setInt(10, requestId); else ps.setNull(10, Types.INTEGER);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String[]> getDonors() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT DonorID, Name, DonorType FROM DONOR ORDER BY Name");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("DonorID"),
                    rs.getString("Name"),
                    rs.getString("DonorType")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getSuppliers() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT SupplierID, SupplierName FROM SUPPLIER ORDER BY SupplierName");
            while (rs.next()) {
                list.add(new String[]{ rs.getString("SupplierID"), rs.getString("SupplierName") });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static String[] getTotals() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT " +
                "ISNULL(SUM(CASE WHEN TransactionType='Donation' AND Status='Approved' THEN Amount ELSE 0 END),0) AS Donations," +
                "ISNULL(SUM(CASE WHEN TransactionType IN ('Expense','Procurement') AND Status='Approved' THEN Amount ELSE 0 END),0) AS Expenses " +
                "FROM FINANCIALTRANSACTION");
            if (rs.next()) {
                String d = rs.getString("Donations");
                String e = rs.getString("Expenses");
                rs.close();
                st.close();
                return new String[]{ d != null ? d : "0", e != null ? e : "0" };
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{"0","0"};
    }

    // ====================== NEW METHODS FOR APPROVAL ======================
    public static boolean approveTransaction(int transactionId, int approvedBy, String remarks) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE FINANCIALTRANSACTION " +
                "SET Status = 'Approved', ApprovedBy = ?, " +
                "Description = ISNULL(Description,'') + ' | Approved: ' + ? " +
                "WHERE TransactionID = ? AND Status = 'Pending'");
            ps.setInt(1, approvedBy);
            ps.setString(2, remarks != null ? remarks : "");
            ps.setInt(3, transactionId);
            int rows = ps.executeUpdate();
            ps.close();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean rejectTransaction(int transactionId, int approvedBy, String remarks) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE FINANCIALTRANSACTION " +
                "SET Status = 'Rejected', ApprovedBy = ?, " +
                "Description = ISNULL(Description,'') + ' | Rejected: ' + ? " +
                "WHERE TransactionID = ? AND Status = 'Pending'");
            ps.setInt(1, approvedBy);
            ps.setString(2, remarks);
            ps.setInt(3, transactionId);
            int rows = ps.executeUpdate();
            ps.close();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String nullSafe(String s) {
        return s != null ? s : "0";
    }
}