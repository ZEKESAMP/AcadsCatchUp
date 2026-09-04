package com.acadscatchup.dao;

import com.acadscatchup.db.DBConnection;
import com.acadscatchup.model.HelpReport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Help & Bug Reports submitted to Admin (F4TAL).
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class HelpReportDAO {

    public static final String DEVELOPER = "F4TAL";

    public boolean submitReport(int userId, String userName, String userRole, String title, String message) {
        String sql = "INSERT INTO help_reports (user_id, user_name, user_role, title, message) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, userName);
            ps.setString(3, userRole);
            ps.setString(4, title);
            ps.setString(5, message);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error submitting help report: " + e.getMessage());
            return false;
        }
    }

    public List<HelpReport> getAllReports() {
        List<HelpReport> list = new ArrayList<>();
        String sql = "SELECT id, user_id, user_name, user_role, title, message, created_at, status FROM help_reports ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new HelpReport(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("user_name"),
                        rs.getString("user_role"),
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getString("created_at"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching help reports: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE help_reports SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating report status: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteReport(int id) {
        String sql = "DELETE FROM help_reports WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting report: " + e.getMessage());
            return false;
        }
    }

    public int getOpenCount() {
        String sql = "SELECT COUNT(*) FROM help_reports WHERE status = 'OPEN'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error fetching open reports count: " + e.getMessage());
        }
        return 0;
    }
}
