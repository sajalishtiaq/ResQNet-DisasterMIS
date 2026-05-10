package com.disastermis.dao;

import com.disastermis.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HospitalDAO {

    public static List<String[]> getHospitalCapacity() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT HospitalID, HospitalName, City, TotalBeds, " +
                "EmergencyCapacity, CurrentAdmissions, BedsAvailable " +
                "FROM vw_hospital_capacity ORDER BY BedsAvailable ASC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("HospitalID"),
                    rs.getString("HospitalName"),
                    nullSafe(rs.getString("City")),
                    rs.getString("TotalBeds"),
                    rs.getString("EmergencyCapacity"),
                    rs.getString("CurrentAdmissions"),
                    rs.getString("BedsAvailable")
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
     * Admits a patient with duplicate prevention.
     * Checks:
     * 1. Hospital emergency capacity
     * 2. Patient does NOT already have an active admission
     */
    public static boolean admitPatient(int patientId, int hospitalId,
                                       Integer reportId, int assignedBy) {
        try {
            Connection conn = DatabaseConnection.getConnection();

            // === 1. Check if patient is already admitted somewhere ===
            PreparedStatement psPatientCheck = conn.prepareStatement(
                "SELECT COUNT(*) FROM PATIENTADMISSION " +
                "WHERE PatientID = ? AND DischargeTime IS NULL");
            psPatientCheck.setInt(1, patientId);
            ResultSet rsPatient = psPatientCheck.executeQuery();
            boolean alreadyAdmitted = false;
            if (rsPatient.next()) {
                alreadyAdmitted = rsPatient.getInt(1) > 0;
            }
            rsPatient.close();
            psPatientCheck.close();

            if (alreadyAdmitted) {
                return false; // Patient already has an active admission
            }

            // === 2. Check hospital capacity (mirrors original SP) ===
            PreparedStatement psCheck = conn.prepareStatement(
                "SELECT h.EmergencyCapacity, " +
                "COUNT(pa.AdmissionID) AS CurrentCount " +
                "FROM HOSPITAL h " +
                "LEFT JOIN PATIENTADMISSION pa " +
                "  ON pa.HospitalID = h.HospitalID AND pa.DischargeTime IS NULL " +
                "WHERE h.HospitalID = ? " +
                "GROUP BY h.EmergencyCapacity");
            psCheck.setInt(1, hospitalId);
            ResultSet rs = psCheck.executeQuery();
            int capacity = 0, current = 0;
            if (rs.next()) {
                capacity = rs.getInt("EmergencyCapacity");
                current  = rs.getInt("CurrentCount");
            }
            rs.close();
            psCheck.close();

            if (current >= capacity) {
                return false; // Hospital at capacity
            }

            // === 3. Insert admission ===
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO PATIENTADMISSION (PatientID, HospitalID, ReportID, AssignedBy) " +
                "VALUES (?, ?, ?, ?)");
            ps.setInt(1, patientId);
            ps.setInt(2, hospitalId);
            if (reportId != null) ps.setInt(3, reportId); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, assignedBy);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String[]> getActiveAdmissions() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT pa.AdmissionID, p.FirstName+' '+p.LastName AS PatientName, " +
                "p.Age, p.Gender, ISNULL(p.Condition,''), h.Name AS Hospital, " +
                "CONVERT(VARCHAR(20), pa.AdmissionTime, 120) AS AdmissionTime, " +
                "ISNULL(u.FullName, '-') AS AssignedBy " +
                "FROM PATIENTADMISSION pa " +
                "JOIN PATIENT p ON p.PatientID = pa.PatientID " +
                "JOIN HOSPITAL h ON h.HospitalID = pa.HospitalID " +
                "LEFT JOIN [USER] u ON u.UserID = pa.AssignedBy " +
                "WHERE pa.DischargeTime IS NULL ORDER BY pa.AdmissionTime DESC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("AdmissionID"),
                    rs.getString("PatientName"),
                    nullSafe(rs.getString("Age")),
                    nullSafe(rs.getString("Gender")),
                    nullSafe(rs.getString(5)),
                    rs.getString("Hospital"),
                    rs.getString("AdmissionTime"),
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

    public static List<String[]> getAllPatients() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT PatientID, FirstName+' '+LastName AS Name, " +
                "ISNULL(CAST(Age AS VARCHAR(10)),'-') AS Age, " +
                "ISNULL(Gender,'-') AS Gender, " +
                "ISNULL([Condition],'-') AS [Condition] " +
                "FROM PATIENT ORDER BY FirstName");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("PatientID"),
                    rs.getString("Name"),
                    rs.getString("Age"),
                    rs.getString("Gender"),
                    rs.getString("Condition")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean createPatient(String firstName, String lastName,
                                        int age, String gender, String condition) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO PATIENT (FirstName,LastName,Age,Gender,[Condition]) VALUES (?,?,?,?,?)");
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setInt(3, age);
            ps.setString(4, gender);
            ps.setString(5, condition != null && !condition.isEmpty() ? condition : null);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean dischargePatient(int admissionId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE PATIENTADMISSION SET DischargeTime=GETDATE() " +
                "WHERE AdmissionID=? AND DischargeTime IS NULL");
            ps.setInt(1, admissionId);
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