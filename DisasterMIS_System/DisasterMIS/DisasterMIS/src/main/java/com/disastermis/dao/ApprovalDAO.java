package com.disastermis.dao;

import com.disastermis.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApprovalDAO {

    /**
     * Returns ALL pending items for Admin (ApprovalRequests + Pending Financial Transactions)
     */
    public static List<String[]> getAllApprovals() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();

            // Regular Approval Requests
            String sql = """
                SELECT RequestID AS ID, 
                       RequestType AS Type, 
                       RequestedAt, 
                       Status, 
                       Remarks, 
                       ru.FullName AS RequestedBy, 
                       ISNULL(au.FullName, '-') AS ApprovedBy,
                       'ApprovalRequest' AS Source
                FROM APPROVALREQUEST ar
                JOIN [USER] ru ON ru.UserID = ar.RequestedBy
                LEFT JOIN [USER] au ON au.UserID = ar.ApprovedBy
                WHERE ar.Status = 'Pending'

                UNION ALL

                -- Pending Financial Transactions
                SELECT TransactionID AS ID,
                       TransactionType AS Type,
                       TransactionDate AS RequestedAt,
                       Status,
                       Description AS Remarks,
                       ISNULL(u.FullName, 'Finance Officer') AS RequestedBy,
                       '-' AS ApprovedBy,
                       'FinancialTransaction' AS Source
                FROM FINANCIALTRANSACTION t
                LEFT JOIN [USER] u ON u.UserID = t.ApprovedBy
                WHERE t.Status = 'Pending'
                ORDER BY RequestedAt DESC
                """;

            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("ID"),
                    rs.getString("Type"),
                    rs.getString("RequestedAt"),
                    rs.getString("Status"),
                    nullSafe(rs.getString("Remarks")),
                    rs.getString("RequestedBy"),
                    rs.getString("ApprovedBy")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean processApproval(int requestId, int approverId,
                                          String decision, String remarks) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE APPROVALREQUEST " +
                "SET Status = ?, ApprovedBy = ?, DecisionAt = GETDATE(), Remarks = ? " +
                "WHERE RequestID = ? AND Status = 'Pending'");
            ps.setString(1, decision);
            ps.setInt(2, approverId);
            ps.setString(3, remarks != null ? remarks : "");
            ps.setInt(4, requestId);
            int rowsAffected = ps.executeUpdate();
            ps.close();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean createRequest(String type, int requestedBy, String remarks) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO APPROVALREQUEST (RequestType, RequestedBy, Remarks) VALUES (?,?,?)");
            ps.setString(1, type);
            ps.setInt(2, requestedBy);
            ps.setString(3, remarks);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}