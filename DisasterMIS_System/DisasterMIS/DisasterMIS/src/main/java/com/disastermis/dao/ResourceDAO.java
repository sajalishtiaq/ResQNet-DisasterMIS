package com.disastermis.dao;

import com.disastermis.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResourceDAO {

    public static List<String[]> getInventorySummary() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT WarehouseName, City, ResourceName, ResourceType, Unit, " +
                "QuantityAvailable, QuantityDispatched, QuantityConsumed, " +
                "LowStockThreshold, StockStatus, " +
                "CONVERT(VARCHAR(20), LastUpdated, 120) AS LastUpdated " +
                "FROM vw_inventory_summary ORDER BY StockStatus DESC, WarehouseName");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("WarehouseName"),
                    rs.getString("City"),
                    rs.getString("ResourceName"),
                    rs.getString("ResourceType"),
                    rs.getString("Unit"),
                    rs.getString("QuantityAvailable"),
                    rs.getString("QuantityDispatched"),
                    rs.getString("QuantityConsumed"),
                    rs.getString("LowStockThreshold"),
                    rs.getString("StockStatus"),
                    rs.getString("LastUpdated")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getLowStockItems() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT WarehouseName, ResourceName, ResourceType, Unit, " +
                "QuantityAvailable, LowStockThreshold " +
                "FROM vw_inventory_summary WHERE StockStatus='LOW STOCK' " +
                "ORDER BY QuantityAvailable ASC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("WarehouseName"),
                    rs.getString("ResourceName"),
                    rs.getString("ResourceType"),
                    rs.getString("Unit"),
                    rs.getString("QuantityAvailable"),
                    rs.getString("LowStockThreshold")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ==================== FIXED: Add New Resource ====================
    public static boolean addResource(String name, String type, String unit, int threshold) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Insert into RESOURCE table
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO RESOURCE (ResourceName, ResourceType, Unit, LowStockThreshold) " +
                "VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, unit);
            ps.setInt(4, threshold);
            
            boolean ok = ps.executeUpdate() > 0;
            
            if (ok) {
                ResultSet rs = ps.getGeneratedKeys();
                int newResourceId = 0;
                if (rs.next()) newResourceId = rs.getInt(1);
                rs.close();
                ps.close();

                // Automatically create inventory entry for ALL warehouses with 0 quantity
                if (newResourceId > 0) {
                    PreparedStatement psInv = conn.prepareStatement(
                        "INSERT INTO INVENTORY (WarehouseID, ResourceID, QuantityAvailable) " +
                        "SELECT WarehouseID, ?, 0 FROM WAREHOUSE");
                    psInv.setInt(1, newResourceId);
                    psInv.executeUpdate();
                    psInv.close();
                }
            }
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== FIXED: Restock (Add to existing) ====================
    public static boolean updateInventory(int warehouseId, int resourceId, int quantity) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            PreparedStatement ps = conn.prepareStatement(
                "MERGE INTO INVENTORY AS target " +
                "USING (SELECT ? AS WarehouseID, ? AS ResourceID) AS source " +
                "ON target.WarehouseID = source.WarehouseID " +
                "AND target.ResourceID = source.ResourceID " +
                "WHEN MATCHED THEN " +
                "    UPDATE SET " +
                "        QuantityAvailable = QuantityAvailable + ?, " +
                "        LastUpdated = GETDATE() " +
                "WHEN NOT MATCHED THEN " +
                "    INSERT (WarehouseID, ResourceID, QuantityAvailable, LastUpdated) " +
                "    VALUES (source.WarehouseID, source.ResourceID, ?, GETDATE());");
            
            ps.setInt(1, warehouseId);
            ps.setInt(2, resourceId);
            ps.setInt(3, quantity);
            ps.setInt(4, quantity);
            
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Other methods remain the same...
    public static boolean allocateResource(int resourceId, int warehouseId,
                                           Integer eventId, int qty,
                                           int allocatedBy, Integer requestId,
                                           Integer reportId) {
        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement psCheck = conn.prepareStatement(
                "SELECT QuantityAvailable FROM INVENTORY " +
                "WHERE WarehouseID = ? AND ResourceID = ?");
            psCheck.setInt(1, warehouseId);
            psCheck.setInt(2, resourceId);
            ResultSet rs = psCheck.executeQuery();
            int available = 0;
            if (rs.next()) available = rs.getInt("QuantityAvailable");
            rs.close();
            psCheck.close();

            if (available < qty) {
                return false;
            }

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO RESOURCEALLOCATION " +
                "(QuantityAllocated, ResourceID, WarehouseID, EventID, AllocatedBy, RequestID, ReportID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, qty);
            ps.setInt(2, resourceId);
            ps.setInt(3, warehouseId);
            if (eventId   != null) ps.setInt(4, eventId);   else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, allocatedBy);
            if (requestId != null) ps.setInt(6, requestId); else ps.setNull(6, Types.INTEGER);
            if (reportId  != null) ps.setInt(7, reportId);  else ps.setNull(7, Types.INTEGER);
            boolean ok = ps.executeUpdate() > 0;
            ps.close();
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String[]> getAllWarehouses() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT WarehouseID, Name FROM WAREHOUSE ORDER BY Name");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("WarehouseID"),
                    rs.getString("Name")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getAllResources() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT ResourceID, ResourceName, ResourceType, Unit, LowStockThreshold " +
                "FROM RESOURCE ORDER BY ResourceType, ResourceName");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("ResourceID"),
                    rs.getString("ResourceName"),
                    rs.getString("ResourceType"),
                    rs.getString("Unit"),
                    rs.getString("LowStockThreshold")
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> getAllocationHistory() {
        List<String[]> list = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT ra.AllocationID, r.ResourceName, w.Name AS Warehouse, " +
                "ra.QuantityAllocated, " +
                "CONVERT(VARCHAR(20), ra.AllocatedAt, 120) AS AllocatedAt, " +
                "ISNULL(e.EventName, '-') AS EventName, " +
                "ISNULL(u.FullName, '-') AS AllocatedBy " +
                "FROM RESOURCEALLOCATION ra " +
                "JOIN RESOURCE r ON r.ResourceID = ra.ResourceID " +
                "JOIN WAREHOUSE w ON w.WarehouseID = ra.WarehouseID " +
                "LEFT JOIN DISASTEREVENT e ON e.EventID = ra.EventID " +
                "LEFT JOIN [USER] u ON u.UserID = ra.AllocatedBy " +
                "ORDER BY ra.AllocatedAt DESC");
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("AllocationID"),
                    rs.getString("ResourceName"),
                    rs.getString("Warehouse"),
                    rs.getString("QuantityAllocated"),
                    rs.getString("AllocatedAt"),
                    rs.getString("EventName"),
                    rs.getString("AllocatedBy")
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