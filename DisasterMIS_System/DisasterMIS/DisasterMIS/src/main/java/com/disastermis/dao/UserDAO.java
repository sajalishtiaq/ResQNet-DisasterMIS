package com.disastermis.dao;

import com.disastermis.db.DatabaseConnection;
import com.disastermis.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Returns a String array with user data if credentials match, null otherwise.
     * Fixed: never return a raw ResultSet (connection closes, RS becomes invalid).
     * Returns: [UserID, FullName, Email, RoleID, RoleName]
     */
    public static String[] authenticate(String email, String password) {
        try {
            String hash = PasswordUtil.sha256(password);
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT u.UserID, u.FullName, u.Email, u.RoleID, r.RoleName " +
                "FROM [USER] u JOIN ROLE r ON r.RoleID = u.RoleID " +
                "WHERE u.Email = ? AND u.PasswordHash = ? AND u.IsActive = 1");
            ps.setString(1, email);
            ps.setString(2, hash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{
                    rs.getString("UserID"),
                    rs.getString("FullName"),
                    rs.getString("Email"),
                    rs.getString("RoleID"),
                    rs.getString("RoleName")
                };
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String[]> getAllUsers() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT u.UserID, u.FullName, u.Email, u.PhoneNumber, u.IsActive, r.RoleName " +
                "FROM [USER] u JOIN ROLE r ON r.RoleID = u.RoleID ORDER BY u.UserID");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("UserID"),
                    rs.getString("FullName"),
                    rs.getString("Email"),
                    rs.getString("PhoneNumber") != null ? rs.getString("PhoneNumber") : "",
                    rs.getInt("IsActive") == 1 ? "Active" : "Inactive",
                    rs.getString("RoleName")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getAllRoles() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT RoleID, RoleName FROM ROLE ORDER BY RoleID");
            while (rs.next()) {
                list.add(new String[]{ rs.getString("RoleID"), rs.getString("RoleName") });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean createUser(String fullName, String email, String password,
                                     String phone, int roleId) {
        try {
            String hash = PasswordUtil.sha256(password);
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO [USER] (FullName, Email, PasswordHash, PhoneNumber, IsActive, RoleID) " +
                "VALUES (?,?,?,?,1,?)");
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, phone);
            ps.setInt(5, roleId);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean toggleUserActive(int userId, boolean activate) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE [USER] SET IsActive = ? WHERE UserID = ?");
            ps.setInt(1, activate ? 1 : 0);
            ps.setInt(2, userId);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
