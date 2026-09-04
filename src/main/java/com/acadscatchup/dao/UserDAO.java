package com.acadscatchup.dao;

import com.acadscatchup.db.DBConnection;
import com.acadscatchup.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User authentication and management.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class UserDAO {
    public static final String DEVELOPER = "F4TAL";

    /**
     * Authenticates a user by username or email and password.
     * @return User object if credentials match, null otherwise.
     */
    public User login(String usernameOrEmail, String password) {
        String sql = "SELECT id, username, password, full_name, email, role, program, year_level, is_verified " +
                     "FROM users WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (com.acadscatchup.util.PasswordUtil.verify(password, storedPassword)) {
                        int userId = rs.getInt("id");
                        // Automatically upgrade legacy plaintext password to secure salted hash
                        if (com.acadscatchup.util.PasswordUtil.needsUpgrade(storedPassword)) {
                            upgradePassword(userId, password);
                        }
                        return mapUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return null;
    }

    private void upgradePassword(int userId, String plainPassword) {
        String hashed = com.acadscatchup.util.PasswordUtil.hash(plainPassword);
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashed);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("[Security] Transparently upgraded password for user ID " + userId + " to salted SHA-256");
        } catch (SQLException e) {
            System.err.println("Failed to upgrade password: " + e.getMessage());
        }
    }

    /** Returns all users with the STUDENT role. */
    public List<User> getAllStudents() {
        return queryList(
            "SELECT id, username, full_name, email, role, program, year_level, is_verified " +
            "FROM users WHERE role = 'STUDENT' ORDER BY full_name");
    }

    /** Returns all users with the PROFESSOR role (strictly excluding Admin). */
    public List<User> getAllProfessors() {
        return queryList(
            "SELECT u.id, u.username, u.full_name, u.email, u.role, u.program, u.year_level, u.is_verified, " +
            "(SELECT " + DBConnection.formatGroupConcat("s.code", ", ") + " FROM subjects s JOIN professor_subjects ps ON s.id = ps.subject_id WHERE ps.professor_id = u.id) AS assigned_subject " +
            "FROM users u WHERE u.role = 'PROFESSOR' ORDER BY u.full_name");
    }

    /** Returns ALL users (professors, students, admins) with assigned subjects. */
    public List<User> getAllUsers() {
        return queryList(
            "SELECT u.id, u.username, u.full_name, u.email, u.role, u.program, u.year_level, u.is_verified, " +
            "(SELECT " + DBConnection.formatGroupConcat("s.code", ", ") + " FROM subjects s JOIN professor_subjects ps ON s.id = ps.subject_id WHERE ps.professor_id = u.id) AS assigned_subject " +
            "FROM users u ORDER BY role, full_name");
    }

    /** Adds a new user (student or professor) with verified email. */
    public boolean addUser(User user, String password) {
        String sql = "INSERT INTO users (username, password, full_name, email, role, program, year_level, is_verified) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String hashedPassword = (password != null && !password.isBlank())
                ? com.acadscatchup.util.PasswordUtil.hash(password)
                : com.acadscatchup.util.PasswordUtil.hash("123456");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, hashedPassword);
            ps.setString(3, user.getFullName());
            ps.setString(4, (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail().trim() : null);
            ps.setString(5, user.getRole());
            ps.setString(6, user.getProgram());
            ps.setInt(7, user.getYearLevel());
            ps.setInt(8, user.isAdmin() ? 1 : 0);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
            }
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("addUser error: " + e.getMessage());
            return false;
        }
    }

    /** Updates username, full name, email, program, year level, and optionally password. */
    public boolean updateUserFull(int id, String username, String fullName, String email,
                                  String newPassword, String program, int yearLevel) {
        String sql = (newPassword != null && !newPassword.isBlank())
                ? "UPDATE users SET username=?, full_name=?, email=?, password=?, program=?, year_level=? WHERE id=?"
                : "UPDATE users SET username=?, full_name=?, email=?, program=?, year_level=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, (email != null && !email.isBlank()) ? email.trim() : null);
            if (newPassword != null && !newPassword.isBlank()) {
                ps.setString(4, com.acadscatchup.util.PasswordUtil.hash(newPassword));
                ps.setString(5, program);
                ps.setInt(6, yearLevel);
                ps.setInt(7, id);
            } else {
                ps.setString(4, program);
                ps.setInt(5, yearLevel);
                ps.setInt(6, id);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateUserFull error: " + e.getMessage());
            return false;
        }
    }

    /** Legacy updateUserFull for backward compatibility. */
    public boolean updateUserFull(int id, String username, String fullName,
                                  String newPassword, String program, int yearLevel) {
        return updateUserFull(id, username, fullName, null, newPassword, program, yearLevel);
    }

    /** Updates only the email for a user. */
    public boolean updateUserEmail(int id, String email) {
        String sql = "UPDATE users SET email = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (email != null && !email.isBlank()) ? email.trim() : null);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateUserEmail error: " + e.getMessage());
            return false;
        }
    }

    /** Verifies if the plainPassword matches the stored password for user id. */
    public boolean verifyUserPassword(int id, String plainPassword) {
        String sql = "SELECT password FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    return com.acadscatchup.util.PasswordUtil.verify(plainPassword, stored);
                }
            }
        } catch (SQLException e) {
            System.err.println("verifyUserPassword error: " + e.getMessage());
        }
        return false;
    }

    /** Updates the password for a user by id. */
    public boolean updatePassword(int id, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, com.acadscatchup.util.PasswordUtil.hash(newPassword));
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updatePassword error: " + e.getMessage());
            return false;
        }
    }

    /** Legacy updateUser kept for compatibility. */
    public boolean updateUser(int id, String fullName, String newPassword) {
        return updateUserFull(id, null, fullName, null, newPassword, null, 0);
    }

    /** Deletes a user by ID. Protects master admin (F4TAL) and faculty professors from deletion. */
    public boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ? AND username != 'F4TAL' AND role != 'ADMIN' AND role != 'PROFESSOR'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("deleteUser error: " + e.getMessage());
            return false;
        }
    }

    /** Checks if a username is already taken by another user. */
    public boolean isUsernameTaken(String username, int excludeId) {
        String sql = "SELECT id FROM users WHERE LOWER(username) = LOWER(?) AND id != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("isUsernameTaken error: " + e.getMessage());
            return false;
        }
    }

    /** Checks if an email is already registered to another user. */
    public boolean isEmailTaken(String email, int excludeId) {
        if (email == null || email.isBlank()) return false;
        String sql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?) AND id != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("isEmailTaken error: " + e.getMessage());
            return false;
        }
    }

    /** Finds a user by their registered email. */
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT u.id, u.username, u.full_name, u.email, u.role, u.program, u.year_level, u.is_verified, " +
                     "(SELECT " + DBConnection.formatGroupConcat("s.code", ", ") + " FROM subjects s JOIN professor_subjects ps ON s.id = ps.subject_id WHERE ps.professor_id = u.id) AS assigned_subject " +
                     "FROM users u WHERE LOWER(u.email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("findByEmail error: " + e.getMessage());
        }
        return null;
    }

    /** Finds a user by either their username or registered email. */
    public User findByUsernameOrEmail(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        String sql = "SELECT u.id, u.username, u.full_name, u.email, u.role, u.program, u.year_level, u.is_verified, " +
                     "(SELECT " + DBConnection.formatGroupConcat("s.code", ", ") + " FROM subjects s JOIN professor_subjects ps ON s.id = ps.subject_id WHERE ps.professor_id = u.id) AS assigned_subject " +
                     "FROM users u WHERE LOWER(u.username) = LOWER(?) OR LOWER(u.email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier.trim());
            ps.setString(2, identifier.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("findByUsernameOrEmail error: " + e.getMessage());
        }
        return null;
    }

    /** Marks a user account as email verified. */
    public boolean markUserVerified(int userId) {
        String sql = "UPDATE users SET is_verified = 1 WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("markUserVerified error: " + e.getMessage());
            return false;
        }
    }

    /** Updates a user's password using their verified email address. */
    public boolean updatePasswordByEmail(String email, String newPassword) {
        if (email == null || email.isBlank() || newPassword == null || newPassword.isBlank()) return false;
        String hashed = com.acadscatchup.util.PasswordUtil.hash(newPassword);
        String sql = "UPDATE users SET password = ? WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashed);
            ps.setString(2, email.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updatePasswordByEmail error: " + e.getMessage());
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<User> queryList(String sql) {
        List<User> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) {
            System.err.println("queryList error: " + e.getMessage());
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("role"),
                rs.getString("program"),
                rs.getInt("year_level")
        );
        try {
            u.setEmail(rs.getString("email"));
        } catch (SQLException ignored) {}
        try {
            u.setAssignedSubject(rs.getString("assigned_subject"));
        } catch (SQLException ignored) {}
        try {
            u.setVerified(rs.getInt("is_verified") == 1);
        } catch (SQLException ignored) {}
        return u;
    }

    public Integer getProfessorAssignedSubjectId(int professorId) {
        String sql = "SELECT subject_id FROM professor_subjects WHERE professor_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, professorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("subject_id");
            }
        } catch (SQLException ignored) {}
        return null;
    }
}
