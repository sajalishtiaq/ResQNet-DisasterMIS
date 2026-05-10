package com.disastermis.dao;

import com.disastermis.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RescueTeamDAO {

    public static List<String[]> getAllTeams() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT TeamID, TeamName, TeamType, AvailabilityStatus, " +
                "Capacity, ContactNumber, TotalMembers, Latitude, Longitude " +
                "FROM vw_team_status ORDER BY AvailabilityStatus, TeamName");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("TeamID"),
                    rs.getString("TeamName"),
                    rs.getString("TeamType"),
                    rs.getString("AvailabilityStatus"),
                    rs.getString("Capacity"),
                    nullSafe(rs.getString("ContactNumber")),
                    rs.getString("TotalMembers"),
                    nullSafe(rs.getString("Latitude")),
                    nullSafe(rs.getString("Longitude"))
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getAvailableTeams() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT TeamID, TeamName, TeamType FROM RESCUETEAM " +
                "WHERE AvailabilityStatus='Available' ORDER BY TeamName");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("TeamID"),
                    rs.getString("TeamName"),
                    rs.getString("TeamType")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 
     * Assigns a rescue team to a report directly via SQL.
     * Replaces stored procedure call — sp_assign_rescue_team uses SET XACT_ABORT ON + RAISERROR
     * which causes JDBC CallableStatement to throw even on success.
     * Replicates SP logic: checks availability, inserts assignment.
     */
    public static boolean assignTeam(int teamId, int reportId, int assignedBy) {
        try {
            Connection conn = DatabaseConnection.getConnection();

            // Check availability (mirrors SP's UPDLOCK check)
            PreparedStatement psCheck = conn.prepareStatement(
                "SELECT AvailabilityStatus FROM RESCUETEAM WHERE TeamID = ?");
            psCheck.setInt(1, teamId);
            ResultSet rs = psCheck.executeQuery();
            String status = null;
            if (rs.next()) status = rs.getString("AvailabilityStatus");
            rs.close();
            psCheck.close();

            if (!"Available".equals(status)) {
                return false; // Team not available
            }

            // Insert assignment — trigger trg_team_assignment_status fires and sets team to Assigned
            PreparedStatement psInsert = conn.prepareStatement(
                "INSERT INTO TEAMASSIGNMENT (TeamID, ReportID, AssignedBy) VALUES (?, ?, ?)");
            psInsert.setInt(1, teamId);
            psInsert.setInt(2, reportId);
            psInsert.setInt(3, assignedBy);
            boolean ok = psInsert.executeUpdate() > 0;
            psInsert.close();

            if (ok) {
                // Create activity log entry so Field Officers can mark it complete
                PreparedStatement psLog = conn.prepareStatement(
                    "IF NOT EXISTS (SELECT 1 FROM TEAMACTIVITYLOG WHERE TeamID = ? AND Status = 'In Progress') " +
                    "INSERT INTO TEAMACTIVITYLOG (TeamID, StartTime, Status) VALUES (?, GETDATE(), 'In Progress')");
                psLog.setInt(1, teamId);
                psLog.setInt(2, teamId);
                psLog.executeUpdate();
                psLog.close();
            }

            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String[]> getAssignmentHistory() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT ta.AssignmentID, rt.TeamName, rt.TeamType, " +
                "r.DisasterType, r.SeverityLevel, ta.AssignedAt, " +
                "ISNULL(CONVERT(VARCHAR(20),ta.CompletedAt,120),'-') AS CompletedAt, " +
                "ISNULL(u.FullName, '-') AS AssignedBy " +
                "FROM TEAMASSIGNMENT ta " +
                "JOIN RESCUETEAM rt ON rt.TeamID = ta.TeamID " +
                "JOIN EMERGENCYREPORT r ON r.ReportID = ta.ReportID " +
                "LEFT JOIN [USER] u ON u.UserID = ta.AssignedBy " +
                "ORDER BY ta.AssignedAt DESC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("AssignmentID"),
                    rs.getString("TeamName"),
                    rs.getString("TeamType"),
                    rs.getString("DisasterType"),
                    rs.getString("SeverityLevel"),
                    rs.getString("AssignedAt"),
                    rs.getString("CompletedAt"),
                    rs.getString("AssignedBy")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getActivityLogs() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT tal.ActivityID, rt.TeamName, " +
                "CONVERT(VARCHAR(20), tal.StartTime, 120) AS StartTime, " +
                "ISNULL(CONVERT(VARCHAR(20), tal.EndTime, 120), '-') AS EndTime, " +
                "tal.Status, ISNULL(tal.OutcomeSummary, '-') AS OutcomeSummary " +
                "FROM TEAMACTIVITYLOG tal " +
                "JOIN RESCUETEAM rt ON rt.TeamID = tal.TeamID " +
                "ORDER BY tal.StartTime DESC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("ActivityID"),
                    rs.getString("TeamName"),
                    rs.getString("StartTime"),
                    rs.getString("EndTime"),
                    rs.getString("Status"),
                    rs.getString("OutcomeSummary")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Marks activity as complete. The trg_team_activity_complete trigger
     * automatically sets the team back to Available.
     * Also updates TEAMASSIGNMENT CompletedAt for the team's active assignment.
     */
    public static boolean markActivityComplete(int activityId, String summary) {
        try {
            Connection conn = DatabaseConnection.getConnection();

            // Get teamId from activity log first
            PreparedStatement psGet = conn.prepareStatement(
                "SELECT TeamID FROM TEAMACTIVITYLOG WHERE ActivityID = ?");
            psGet.setInt(1, activityId);
            ResultSet rs = psGet.executeQuery();
            int teamId = -1;
            if (rs.next()) teamId = rs.getInt("TeamID");
            rs.close();
            psGet.close();

            if (teamId == -1) return false;

            // Update activity log — trigger fires and sets team Available
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE TEAMACTIVITYLOG SET Status='Completed', EndTime=GETDATE(), " +
                "OutcomeSummary=? WHERE ActivityID=? AND Status <> 'Completed'");
            ps.setString(1, summary);
            ps.setInt(2, activityId);
            int rows = ps.executeUpdate();
            ps.close();

            if (rows == 0) return false;

            // Also mark the latest open team assignment as completed
            PreparedStatement psAssign = conn.prepareStatement(
                "UPDATE TEAMASSIGNMENT SET CompletedAt = GETDATE() " +
                "WHERE TeamID = ? AND CompletedAt IS NULL");
            psAssign.setInt(1, teamId);
            psAssign.executeUpdate();
            psAssign.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean createTeam(String name, String type, String contact, int capacity) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO RESCUETEAM (TeamName,TeamType,ContactNumber,Capacity) VALUES (?,?,?,?)");
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, contact);
            ps.setInt(4, capacity);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String[]> getTeamMembers(int teamId) {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT tm.MemberID, tm.FirstName+' '+tm.LastName AS Name, tm.Designation, " +
                "tm.PhoneNumber, STRING_AGG(sp.Specialization,', ') AS Specs " +
                "FROM TEAMMEMBER tm " +
                "LEFT JOIN SPECIALIZATION sp ON sp.MemberID = tm.MemberID " +
                "WHERE tm.TeamID=? GROUP BY tm.MemberID, tm.FirstName, tm.LastName, " +
                "tm.Designation, tm.PhoneNumber ORDER BY tm.FirstName");
            ps.setInt(1, teamId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("MemberID"),
                    rs.getString("Name"),
                    nullSafe(rs.getString("Designation")),
                    nullSafe(rs.getString("PhoneNumber")),
                    nullSafe(rs.getString("Specs"))
                });
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
