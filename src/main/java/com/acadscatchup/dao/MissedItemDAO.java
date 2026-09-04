package com.acadscatchup.dao;

import com.acadscatchup.db.DBConnection;
import com.acadscatchup.model.MissedItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Access Object for MissedItem CRUD and queries.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class MissedItemDAO {

    public static final String DEVELOPER = "F4TAL";

    // ── Base SELECT query ──────────────────────────────────────────────────
    private static final String BASE_SELECT = """
            SELECT mi.id, mi.student_id, u.full_name AS student_name,
                   mi.subject_id, s.code AS subject_code, s.name AS subject_name,
                   mi.item_type, mi.item_name, mi.date_missed, mi.deadline,
                   mi.status, mi.notes, mi.created_by,
                   COALESCE(u_prof.full_name, (
                       SELECT u_p.full_name FROM users u_p
                       JOIN professor_subjects ps ON u_p.id = ps.professor_id
                       WHERE ps.subject_id = mi.subject_id AND u_p.role = 'PROFESSOR'
                       LIMIT 1
                   ), 'Not Assigned') AS prof_name,
                   mi.attachment_type, mi.attachment_name, mi.attachment_url
            FROM missed_items mi
            JOIN users u    ON mi.student_id = u.id
            JOIN subjects s ON mi.subject_id = s.id
            LEFT JOIN users u_prof ON mi.created_by = u_prof.id AND u_prof.role = 'PROFESSOR'
            """;

    // ── Professor: get all missed items with optional filters ──────────────
    public List<MissedItem> getAll(String statusFilter, String subjectFilter, String typeFilter, String search) {
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equals(statusFilter)) {
            sql.append("AND mi.status = ? ");
            params.add(statusFilter);
        }
        if (subjectFilter != null && !subjectFilter.isBlank() && !"ALL".equals(subjectFilter)) {
            sql.append("AND s.code = ? ");
            params.add(subjectFilter);
        }
        if (typeFilter != null && !typeFilter.isBlank() && !"ALL".equals(typeFilter)) {
            sql.append("AND mi.item_type = ? ");
            params.add(typeFilter);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (u.full_name LIKE ? OR mi.item_name LIKE ?) ");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }
        sql.append("ORDER BY mi.date_missed DESC");

        return query(sql.toString(), params);
    }

    /** Find a single missed item by its ID. */
    public MissedItem getById(int id) {
        String sql = BASE_SELECT + " WHERE mi.id = ?";
        List<MissedItem> res = query(sql, List.of(id));
        return res.isEmpty() ? null : res.get(0);
    }

    // ── Student: get only their own missed items ───────────────────────────
    public List<MissedItem> getByStudent(int studentId, String statusFilter, String subjectFilter) {
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE mi.student_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(studentId);

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equals(statusFilter)) {
            sql.append("AND mi.status = ? ");
            params.add(statusFilter);
        }
        if (subjectFilter != null && !subjectFilter.isBlank() && !"ALL".equals(subjectFilter)) {
            sql.append("AND s.code = ? ");
            params.add(subjectFilter);
        }
        sql.append("ORDER BY mi.date_missed DESC");

        return query(sql.toString(), params);
    }

    /** Inserts a new missed item. */
    public boolean insert(MissedItem item) {
        String sql = """
                INSERT INTO missed_items
                (student_id, subject_id, item_type, item_name, date_missed, deadline, status, notes, created_by,
                 attachment_type, attachment_name, attachment_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, item.getStudentId());
            ps.setInt(2, item.getSubjectId());
            ps.setString(3, item.getItemType());
            ps.setString(4, item.getItemName());
            ps.setString(5, item.getDateMissed() != null ? item.getDateMissed().toString() : null);
            ps.setString(6, item.getDeadline()   != null ? item.getDeadline().toString()   : null);
            ps.setString(7, item.getStatus() != null ? item.getStatus() : "PENDING");
            ps.setString(8, item.getNotes());
            ps.setInt(9, item.getCreatedBy());
            ps.setString(10, item.getAttachmentType());
            ps.setString(11, item.getAttachmentName());
            ps.setString(12, item.getAttachmentUrl());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        item.setId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("insert error: " + e.getMessage());
            return false;
        }
    }

    /** Updates all fields of a missed item (professor). */
    public boolean update(MissedItem item) {
        String sql = """
                UPDATE missed_items SET
                  student_id = ?, subject_id = ?, item_type = ?, item_name = ?,
                  date_missed = ?, deadline = ?, status = ?, notes = ?,
                  attachment_type = ?, attachment_name = ?, attachment_url = ?
                WHERE id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, item.getStudentId());
            ps.setInt(2, item.getSubjectId());
            ps.setString(3, item.getItemType());
            ps.setString(4, item.getItemName());
            ps.setString(5, item.getDateMissed() != null ? item.getDateMissed().toString() : null);
            ps.setString(6, item.getDeadline()   != null ? item.getDeadline().toString()   : null);
            ps.setString(7, item.getStatus());
            ps.setString(8, item.getNotes());
            ps.setString(9, item.getAttachmentType());
            ps.setString(10, item.getAttachmentName());
            ps.setString(11, item.getAttachmentUrl());
            ps.setInt(12, item.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("update error: " + e.getMessage());
            return false;
        }
    }

    /** Student marks an item as SUBMITTED. Professor can also mark as GRADED. */
    public boolean updateStatus(int itemId, String newStatus) {
        String sql = "UPDATE missed_items SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateStatus error: " + e.getMessage());
            return false;
        }
    }

    /** Deletes a missed item by ID. */
    public boolean delete(int id) {
        String sql = "DELETE FROM missed_items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("delete error: " + e.getMessage());
            return false;
        }
    }

    /** Count stats for a student: [total, pending, submitted, graded]. */
    public int[] getStudentStats(int studentId) {
        String sql = """
                SELECT
                  COUNT(*) AS total,
                  SUM(status='PENDING')   AS pending,
                  SUM(status='SUBMITTED') AS submitted,
                  SUM(status='GRADED')    AS graded
                FROM missed_items WHERE student_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{
                            rs.getInt("total"),
                            rs.getInt("pending"),
                            rs.getInt("submitted"),
                            rs.getInt("graded")
                    };
                }
            }
        } catch (SQLException e) {
            System.err.println("getStudentStats error: " + e.getMessage());
        }
        return new int[]{0, 0, 0, 0};
    }

    // ── Internal helper ────────────────────────────────────────────────────
    private List<MissedItem> query(String sql, List<Object> params) {
        List<MissedItem> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("query error: " + e.getMessage());
        }
        return list;
    }

    private MissedItem mapItem(ResultSet rs) throws SQLException {
        MissedItem item = new MissedItem();
        item.setId(rs.getInt("id"));
        item.setStudentId(rs.getInt("student_id"));
        item.setStudentName(rs.getString("student_name"));
        item.setSubjectId(rs.getInt("subject_id"));
        item.setSubjectCode(rs.getString("subject_code"));
        item.setSubjectName(rs.getString("subject_name"));
        item.setItemType(rs.getString("item_type"));
        item.setItemName(rs.getString("item_name"));

        item.setDateMissed(parseDateSafely(rs.getString("date_missed")));
        item.setDeadline(parseDateSafely(rs.getString("deadline")));

        item.setStatus(rs.getString("status"));
        item.setNotes(rs.getString("notes"));
        item.setCreatedBy(rs.getInt("created_by"));
        try {
            item.setProfName(rs.getString("prof_name"));
            item.setAttachmentType(rs.getString("attachment_type"));
            item.setAttachmentName(rs.getString("attachment_name"));
            item.setAttachmentUrl(rs.getString("attachment_url"));
        } catch (SQLException ignored) {}
        return item;
    }

    private static java.time.LocalDate parseDateSafely(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            String trimmed = str.trim();
            if (trimmed.length() >= 10) trimmed = trimmed.substring(0, 10);
            return java.time.LocalDate.parse(trimmed);
        } catch (Exception e) {
            return null;
        }
    }
}
