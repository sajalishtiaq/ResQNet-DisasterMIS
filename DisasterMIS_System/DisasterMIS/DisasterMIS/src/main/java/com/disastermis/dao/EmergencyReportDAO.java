package com.disastermis.dao;

import com.disastermis.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmergencyReportDAO {

    /**
     * Returns active/pending reports for Operator dashboard.
     * Columns: [0]=ReportID [1]=DisasterType [2]=SeverityLevel [3]=TimeOfReport
     *          [4]=Status [5]=Description [6]=ReportedBy [7]=EventName
     */
    public static List<String[]> getActiveReports() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT ReportID, DisasterType, SeverityLevel, TimeOfReport, Status, " +
                "Description, ReportedBy, ContactNumber, EventName " +
                "FROM vw_active_emergency_reports ORDER BY " +
                "CASE SeverityLevel WHEN 'Critical' THEN 1 WHEN 'High' THEN 2 " +
                "WHEN 'Medium' THEN 3 ELSE 4 END");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("ReportID"),
                    rs.getString("DisasterType"),
                    rs.getString("SeverityLevel"),
                    rs.getString("TimeOfReport"),
                    rs.getString("Status"),
                    nullSafe(rs.getString("Description")),
                    nullSafe(rs.getString("ReportedBy")),
                    nullSafe(rs.getString("EventName"))
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getAllReports() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT r.ReportID, r.DisasterType, r.SeverityLevel, r.TimeOfReport, " +
                "r.Status, r.Description, " +
                "ISNULL(c.FirstName + ' ' + c.LastName, 'Unknown') AS ReportedBy, " +
                "ISNULL(e.EventName, '-') AS EventName " +
                "FROM EMERGENCYREPORT r " +
                "LEFT JOIN CITIZEN c ON c.CitizenID = r.CitizenID " +
                "LEFT JOIN DISASTEREVENT e ON e.EventID = r.EventID " +
                "ORDER BY r.TimeOfReport DESC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("ReportID"),
                    rs.getString("DisasterType"),
                    rs.getString("SeverityLevel"),
                    rs.getString("TimeOfReport"),
                    rs.getString("Status"),
                    nullSafe(rs.getString("Description")),
                    rs.getString("ReportedBy"),
                    rs.getString("EventName")
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
     * Returns true if an active (Pending or In Progress) report already exists
     * for the same location (within ~500m) and same disaster type.
     * Coordinate tolerance: 0.005 degrees ≈ 500 metres — tight enough to catch
     * accidental re-submission, loose enough to allow genuinely nearby but
     * distinct incidents of a different type.
     */
    public static boolean isDuplicateReport(double lat, double lon, String disasterType) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM EMERGENCYREPORT " +
                "WHERE DisasterType = ? " +
                "  AND Status NOT IN ('Resolved') " +
                "  AND ABS(Latitude  - ?) < 0.005 " +
                "  AND ABS(Longitude - ?) < 0.005");
            ps.setString(1, disasterType);
            ps.setDouble(2, lat);
            ps.setDouble(3, lon);
            ResultSet rs = ps.executeQuery();
            boolean dup = rs.next() && rs.getInt(1) > 0;
            rs.close();
            ps.close();
            return dup;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean submitReport(double lat, double lon, String disasterType,
                                        String severity, String description,
                                        Integer citizenId, Integer eventId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO EMERGENCYREPORT " +
                "(Latitude, Longitude, DisasterType, SeverityLevel, Description, CitizenID, EventID) " +
                "VALUES (?,?,?,?,?,?,?)");
            ps.setDouble(1, lat);
            ps.setDouble(2, lon);
            ps.setString(3, disasterType);
            ps.setString(4, severity);
            ps.setString(5, description);
            if (citizenId != null) ps.setInt(6, citizenId); else ps.setNull(6, Types.INTEGER);
            if (eventId   != null) ps.setInt(7, eventId);   else ps.setNull(7, Types.INTEGER);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateStatus(int reportId, String newStatus) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE EMERGENCYREPORT SET Status = ? WHERE ReportID = ?");
            ps.setString(1, newStatus);
            ps.setInt(2, reportId);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** For dashboard summary counts: [Pending, InProgress, TeamsAvail, PendingApprovals, Critical] */
    public static int[] getDashboardCounts() {
        int[] counts = {0, 0, 0, 0, 0};
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT " +
                "(SELECT COUNT(*) FROM EMERGENCYREPORT WHERE Status='Pending') AS Pending," +
                "(SELECT COUNT(*) FROM EMERGENCYREPORT WHERE Status='In Progress') AS InProgress," +
                "(SELECT COUNT(*) FROM RESCUETEAM WHERE AvailabilityStatus='Available') AS TeamsAvail," +
                "(SELECT COUNT(*) FROM APPROVALREQUEST WHERE Status='Pending') AS Approvals," +
                "(SELECT COUNT(*) FROM EMERGENCYREPORT WHERE SeverityLevel='Critical' AND Status NOT IN ('Resolved')) AS Critical");
            if (rs.next()) {
                counts[0] = rs.getInt("Pending");
                counts[1] = rs.getInt("InProgress");
                counts[2] = rs.getInt("TeamsAvail");
                counts[3] = rs.getInt("Approvals");
                counts[4] = rs.getInt("Critical");
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }

    public static List<String[]> getDisasterEvents() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            // Show all events (Active and Resolved) so users can link reports to any event
            ResultSet rs = st.executeQuery(
                "SELECT EventID, EventName FROM DISASTEREVENT ORDER BY EventName");
            while (rs.next()) {
                list.add(new String[]{ rs.getString("EventID"), rs.getString("EventName") });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getCitizens() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT CitizenID, FirstName+' '+LastName AS Name FROM CITIZEN ORDER BY FirstName");
            while (rs.next()) {
                list.add(new String[]{ rs.getString("CitizenID"), rs.getString("Name") });
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