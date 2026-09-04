package com.acadscatchup.dao;

import com.acadscatchup.db.DBConnection;
import com.acadscatchup.model.Subject;
import com.acadscatchup.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Subject management.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class SubjectDAO {

    public static final String DEVELOPER = "F4TAL";

    /** Returns all subjects. */
    public List<Subject> getAllSubjects() {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT id, code, name FROM subjects ORDER BY code";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Subject(rs.getInt("id"), rs.getString("code"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("getAllSubjects error: " + e.getMessage());
        }
        return list;
    }

    /** Returns subjects enrolled by a specific student with professor information. */
    public List<Subject> getSubjectsByStudent(int studentId) {
        List<Subject> list = new ArrayList<>();
        String profConcat = DBConnection.formatGroupConcat("u.full_name", ", ");
        String sql = "SELECT s.id, s.code, s.name, " +
                     "(SELECT " + profConcat + " FROM users u " +
                     " JOIN professor_subjects ps ON u.id = ps.professor_id " +
                     " WHERE ps.subject_id = s.id) AS prof_name " +
                     "FROM subjects s " +
                     "WHERE s.id IN (" +
                     "    SELECT subject_id FROM enrollments WHERE student_id = ? " +
                     "    UNION " +
                     "    SELECT subject_id FROM missed_items WHERE student_id = ?" +
                     ") " +
                     "ORDER BY s.code";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String prof = rs.getString("prof_name");
                    list.add(new Subject(rs.getInt("id"), rs.getString("code"), rs.getString("name"), prof));
                }
            }
        } catch (SQLException e) {
            System.err.println("getSubjectsByStudent error: " + e.getMessage());
        }
        return list;
    }

    public boolean addSubject(Subject subject) {
        String sql = "INSERT INTO subjects (code, name) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, subject.getCode().trim().toUpperCase());
            ps.setString(2, subject.getName().trim());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int subjectId = rs.getInt(1);
                        String enrollSql = "INSERT IGNORE INTO enrollments (student_id, subject_id) " +
                                           "SELECT id, ? FROM users WHERE role = 'STUDENT'";
                        try (PreparedStatement eps = conn.prepareStatement(enrollSql)) {
                            eps.setInt(1, subjectId);
                            eps.executeUpdate();
                        }
                    }
                }
            }
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("addSubject error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSubject(int id) {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("deleteSubject error: " + e.getMessage());
            return false;
        }
    }

    /** Returns subjects assigned to a specific professor. */
    public List<Subject> getSubjectsByProfessor(int professorId) {
        List<Subject> list = new ArrayList<>();
        String sql = """
                SELECT s.id, s.code, s.name FROM subjects s
                JOIN professor_subjects ps ON s.id = ps.subject_id
                WHERE ps.professor_id = ?
                ORDER BY s.code
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, professorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Subject(rs.getInt("id"), rs.getString("code"), rs.getString("name")));
                }
            }
        } catch (SQLException e) {
            System.err.println("getSubjectsByProfessor error: " + e.getMessage());
        }
        return list;
    }

    public boolean assignProfessorSubject(int professorId, int subjectId) {
        String sql = "REPLACE INTO professor_subjects (professor_id, subject_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, professorId);
            ps.setInt(2, subjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("assignProfessorSubject error: " + e.getMessage());
            return false;
        }
    }

    public boolean assignProfessorSubjects(int professorId, List<Integer> subjectIds) {
        removeProfessorSubjects(professorId);
        if (subjectIds == null || subjectIds.isEmpty()) return true;
        String sql = "INSERT INTO professor_subjects (professor_id, subject_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int subId : subjectIds) {
                ps.setInt(1, professorId);
                ps.setInt(2, subId);
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            System.err.println("assignProfessorSubjects error: " + e.getMessage());
            return false;
        }
    }

    public boolean removeProfessorSubjects(int professorId) {
        String sql = "DELETE FROM professor_subjects WHERE professor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, professorId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("removeProfessorSubjects error: " + e.getMessage());
            return false;
        }
    }

    public List<com.acadscatchup.model.User> getStudentsBySubject(int subjectId) {
        List<com.acadscatchup.model.User> list = new ArrayList<>();
        String sql = """
                SELECT DISTINCT u.id, u.username, u.full_name, u.role, u.program, u.year_level
                FROM users u
                JOIN enrollments e ON u.id = e.student_id
                WHERE e.subject_id = ? AND u.role = 'STUDENT'
                ORDER BY u.full_name
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.acadscatchup.model.User u = new com.acadscatchup.model.User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setRole(rs.getString("role"));
                    u.setProgram(rs.getString("program"));
                    u.setYearLevel(rs.getInt("year_level"));
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            System.err.println("getStudentsBySubject error: " + e.getMessage());
        }
        return list;
    }

    public List<com.acadscatchup.model.User> getStudentsBySubjects(List<Integer> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) return new ArrayList<>();
        List<com.acadscatchup.model.User> list = new ArrayList<>();
        String placeholders = String.join(",", java.util.Collections.nCopies(subjectIds.size(), "?"));
        String sql = "SELECT DISTINCT u.id, u.username, u.full_name, u.role, u.program, u.year_level " +
                     "FROM users u JOIN enrollments e ON u.id = e.student_id " +
                     "WHERE e.subject_id IN (" + placeholders + ") AND u.role = 'STUDENT' ORDER BY u.full_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < subjectIds.size(); i++) {
                ps.setInt(i + 1, subjectIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.acadscatchup.model.User u = new com.acadscatchup.model.User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setRole(rs.getString("role"));
                    u.setProgram(rs.getString("program"));
                    u.setYearLevel(rs.getInt("year_level"));
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            System.err.println("getStudentsBySubjects error: " + e.getMessage());
        }
        return list;
    }

    public boolean enrollStudent(int studentId, int subjectId) {
        String sql = "REPLACE INTO enrollments (student_id, subject_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("enrollStudent error: " + e.getMessage());
            return false;
        }
    }

    /** Enrolls a single student into ALL active subjects. */
    public int enrollStudentInAllSubjects(int studentId) {
        String sql = DBConnection.isMySQL()
                ? "INSERT IGNORE INTO enrollments (student_id, subject_id) SELECT ?, id FROM subjects"
                : "INSERT OR IGNORE INTO enrollments (student_id, subject_id) SELECT ?, id FROM subjects";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("enrollStudentInAllSubjects error: " + e.getMessage());
            return 0;
        }
    }

    /** Enrolls multiple students into ALL active subjects. */
    public int enrollStudentsInAllSubjects(List<Integer> studentIds) {
        int count = 0;
        for (int id : studentIds) {
            count += enrollStudentInAllSubjects(id);
        }
        return count;
    }

    /** Unenrolls a single student from ALL subjects. */
    public int unenrollStudentFromAllSubjects(int studentId) {
        String sql = "DELETE FROM enrollments WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("unenrollStudentFromAllSubjects error: " + e.getMessage());
            return 0;
        }
    }

    /** Unenrolls multiple students from ALL subjects. */
    public int unenrollStudentsFromAllSubjects(List<Integer> studentIds) {
        int count = 0;
        for (int id : studentIds) {
            count += unenrollStudentFromAllSubjects(id);
        }
        return count;
    }

    public boolean unenrollStudent(int studentId, int subjectId) {
        String sql = "DELETE FROM enrollments WHERE student_id = ? AND subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("unenrollStudent error: " + e.getMessage());
            return false;
        }
    }

    public boolean isStudentEnrolled(int studentId, int subjectId) {
        String sql = "SELECT 1 FROM enrollments WHERE student_id = ? AND subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public String getProfessorsNamesBySubject(int subjectId) {
        String sql = "SELECT " + DBConnection.formatGroupConcat("u.full_name", ", ") + " AS profs " +
                     "FROM users u " +
                     "JOIN professor_subjects ps ON u.id = ps.professor_id " +
                     "WHERE ps.subject_id = ? AND u.role = 'PROFESSOR'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String profs = rs.getString("profs");
                    return (profs != null && !profs.isBlank()) ? profs : "Not Assigned";
                }
            }
        } catch (SQLException e) {
            System.err.println("getProfessorsNamesBySubject error: " + e.getMessage());
        }
        return "Not Assigned";
    }

    /** Returns all professors assigned to a specific subject (strictly excluding Admin). */
    public List<User> getProfessorsBySubject(int subjectId) {
        List<User> list = new ArrayList<>();
        String sql = """
                SELECT u.id, u.username, u.full_name, u.role, u.program, u.year_level
                FROM users u
                JOIN professor_subjects ps ON u.id = ps.professor_id
                WHERE ps.subject_id = ? AND u.role = 'PROFESSOR'
                ORDER BY u.full_name
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setRole(rs.getString("role"));
                    u.setProgram(rs.getString("program"));
                    u.setYearLevel(rs.getInt("year_level"));
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            System.err.println("getProfessorsBySubject error: " + e.getMessage());
        }
        return list;
    }

    public static class StudentEnrollmentStatus {
        private final com.acadscatchup.model.User user;
        private final boolean enrolled;
        private final String professorName;

        public StudentEnrollmentStatus(com.acadscatchup.model.User user, boolean enrolled, String professorName) {
            this.user = user;
            this.enrolled = enrolled;
            this.professorName = professorName;
        }

        public com.acadscatchup.model.User getUser() { return user; }
        public boolean isEnrolled() { return enrolled; }
        public String getProfessorName() { return professorName; }
    }

    public List<StudentEnrollmentStatus> getAllStudentsWithEnrollmentStatus(int subjectId, String search, int yearLevel) {
        String profName = getProfessorsNamesBySubject(subjectId);
        List<StudentEnrollmentStatus> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT u.id, u.username, u.full_name, u.role, u.program, u.year_level,
                       CASE WHEN e.student_id IS NOT NULL THEN 1 ELSE 0 END AS is_enrolled
                FROM users u
                LEFT JOIN enrollments e ON u.id = e.student_id AND e.subject_id = ?
                WHERE u.role = 'STUDENT'
                """);
        List<Object> params = new ArrayList<>();
        params.add(subjectId);

        if (yearLevel > 0) {
            sql.append(" AND u.year_level = ?");
            params.add(yearLevel);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.username) LIKE ? OR LOWER(u.program) LIKE ?)");
            String q = "%" + search.toLowerCase().trim() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }
        sql.append(" ORDER BY is_enrolled DESC, u.year_level, u.full_name");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.acadscatchup.model.User u = new com.acadscatchup.model.User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setRole(rs.getString("role"));
                    u.setProgram(rs.getString("program"));
                    u.setYearLevel(rs.getInt("year_level"));
                    boolean enrolled = rs.getInt("is_enrolled") == 1;
                    list.add(new StudentEnrollmentStatus(u, enrolled, profName));
                }
            }
        } catch (SQLException e) {
            System.err.println("getAllStudentsWithEnrollmentStatus error: " + e.getMessage());
        }
        return list;
    }

    public List<com.acadscatchup.model.User> getStudentsNotEnrolled(int subjectId, String search, int yearLevel) {
        List<com.acadscatchup.model.User> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT u.id, u.username, u.full_name, u.role, u.program, u.year_level
                FROM users u
                WHERE u.role = 'STUDENT'
                  AND u.id NOT IN (SELECT student_id FROM enrollments WHERE subject_id = ?)
                """);
        List<Object> params = new ArrayList<>();
        params.add(subjectId);

        if (yearLevel > 0) {
            sql.append(" AND u.year_level = ?");
            params.add(yearLevel);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.username) LIKE ? OR LOWER(u.program) LIKE ?)");
            String q = "%" + search.toLowerCase().trim() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }
        sql.append(" ORDER BY u.year_level, u.full_name");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.acadscatchup.model.User u = new com.acadscatchup.model.User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setRole(rs.getString("role"));
                    u.setProgram(rs.getString("program"));
                    u.setYearLevel(rs.getInt("year_level"));
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            System.err.println("getStudentsNotEnrolled error: " + e.getMessage());
        }
        return list;
    }
}
