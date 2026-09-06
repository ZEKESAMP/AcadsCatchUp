package com.acadscatchup.dao;

import com.acadscatchup.db.DBConnection;
import com.acadscatchup.model.InboxMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for personal inbox and student deficiency submissions.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class InboxDAO {

    public static final String DEVELOPER = "F4TAL";

    public boolean sendMessage(int senderId, String senderName, String senderRole,
                               int recipientId, String recipientName,
                               String title, String message,
                               Integer itemId, String itemName, String subjectCode,
                               String msgType) {
        return sendMessage(senderId, senderName, senderRole, recipientId, recipientName,
                title, message, itemId, itemName, subjectCode, msgType, null, null, null);
    }

    public boolean sendMessage(int senderId, String senderName, String senderRole,
                               int recipientId, String recipientName,
                               String title, String message,
                               Integer itemId, String itemName, String subjectCode,
                               String msgType, String attachmentType, String attachmentName, String attachmentUrl) {
        String sql = """
            INSERT INTO inbox_messages
            (sender_id, sender_name, sender_role, recipient_id, recipient_name,
             title, message, item_id, item_name, subject_code, msg_type,
             attachment_type, attachment_name, attachment_url, is_read)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setString(2, senderName);
            ps.setString(3, senderRole);
            ps.setInt(4, recipientId);
            ps.setString(5, recipientName);
            ps.setString(6, title);
            ps.setString(7, message);
            if (itemId != null) ps.setInt(8, itemId); else ps.setNull(8, Types.INTEGER);
            ps.setString(9, itemName);
            ps.setString(10, subjectCode);
            ps.setString(11, msgType);
            ps.setString(12, attachmentType);
            ps.setString(13, attachmentName);
            ps.setString(14, attachmentUrl);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("sendMessage error: " + e.getMessage());
            return false;
        }
    }

    public boolean sendGradedNotice(int recipientId, String recipientName,
                                    int senderId, String senderName,
                                    com.acadscatchup.model.MissedItem item) {
        String title = "Activity Graded: " + item.getItemName();
        String body = "Hello " + recipientName + "!\n\n"
                + "Your submission for \"" + item.getItemName() + "\" (" + item.getSubjectCode() + ") "
                + "has been reviewed and officially marked as GRADED by " + senderName + ".\n\n"
                + "Great job staying caught up with your academics!";
        return sendMessage(senderId, senderName, "PROFESSOR",
                recipientId, recipientName,
                title, body,
                item.getId(), item.getItemName(), item.getSubjectCode(),
                "GRADED");
    }

    /**
     * Pushes an official deficiency notification to a student's inbox when a professor records a missed item.
     */
    public boolean sendMissedItemNotice(int recipientId, String recipientName,
                                        int senderId, String senderName, String senderRole,
                                        com.acadscatchup.model.MissedItem item) {
        String rawType = (item.getItemType() != null) ? item.getItemType().trim().toUpperCase() : "ACTIVITY";
        String displayType = switch (rawType) {
            case "QUIZ"       -> "Missed Quiz";
            case "EXAM"       -> "Missed Exam";
            case "ASSIGNMENT" -> "Missed Assignment";
            default           -> "Missed Activity";
        };

        String title = displayType + ": " + item.getItemName();
        String deadlineStr = (item.getDeadline() != null) ? item.getDeadline().toString() : "None set";
        String dateMissedStr = (item.getDateMissed() != null) ? item.getDateMissed().toString() : "N/A";
        String subCode = (item.getSubjectCode() != null && !item.getSubjectCode().isBlank()) ? item.getSubjectCode() : "";

        String body = "Hello " + recipientName + ",\n\n"
                + "A new " + displayType.toLowerCase() + " has been recorded on your record by " + senderName + " (" + senderRole + "):\n\n"
                + (subCode.isEmpty() ? "" : "• Subject: " + subCode + "\n")
                + "• Item: " + item.getItemName() + " (" + rawType + ")\n"
                + "• Date Missed: " + dateMissedStr + "\n"
                + "• Deadline: " + deadlineStr + "\n"
                + (item.getNotes() != null && !item.getNotes().isBlank() ? "• Instructions/Notes: " + item.getNotes() + "\n\n" : "\n")
                + "Please complete your activity and submit your make-up work on your dashboard to catch up with your academics.";

        return sendMessage(senderId, senderName, senderRole,
                recipientId, recipientName,
                title, body,
                item.getId(), item.getItemName(), subCode,
                displayType);
    }

    public List<InboxMessage> getMessagesForRecipient(int recipientId) {
        List<InboxMessage> list = new ArrayList<>();
        String sql = """
            SELECT id, sender_id, sender_name, sender_role, recipient_id, recipient_name,
                   title, message, item_id, item_name, subject_code, msg_type,
                   attachment_type, attachment_name, attachment_url, is_read, created_at
            FROM inbox_messages
            WHERE recipient_id = ?
            ORDER BY id DESC
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int iId = rs.getInt("item_id");
                    Integer itemId = rs.wasNull() ? null : iId;
                    String attType = null;
                    String attName = null;
                    String attUrl = null;
                    try {
                        attType = rs.getString("attachment_type");
                        attName = rs.getString("attachment_name");
                        attUrl = rs.getString("attachment_url");
                    } catch (SQLException ignored) {}
                    list.add(new InboxMessage(
                            rs.getInt("id"),
                            rs.getInt("sender_id"),
                            rs.getString("sender_name"),
                            rs.getString("sender_role"),
                            rs.getInt("recipient_id"),
                            rs.getString("recipient_name"),
                            rs.getString("title"),
                            rs.getString("message"),
                            itemId,
                            rs.getString("item_name"),
                            rs.getString("subject_code"),
                            rs.getString("msg_type"),
                            attType,
                            attName,
                            attUrl,
                            rs.getInt("is_read") == 1,
                            rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("getMessagesForRecipient error: " + e.getMessage());
        }
        return list;
    }

    public int getUnreadCount(int recipientId) {
        String sql = "SELECT COUNT(*) FROM inbox_messages WHERE recipient_id = ? AND is_read = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("getUnreadCount error: " + e.getMessage());
        }
        return 0;
    }

    public boolean markAsRead(int messageId) {
        String sql = "UPDATE inbox_messages SET is_read = 1 WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("markAsRead error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteMessage(int messageId) {
        String sql = "DELETE FROM inbox_messages WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("deleteMessage error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Broadcasts a System Update announcement to all registered users (Students, Professors, Admins)
     * if they haven't received it yet for the specified version.
     */
    public int broadcastSystemUpdateNotice(String version, String title, String message) {
        String matchTitle = "%v" + version + "%";
        String checkSql = "SELECT COUNT(*) FROM inbox_messages WHERE recipient_id = ? AND (title LIKE ? OR message LIKE ?)";
        String userSql = "SELECT id, full_name FROM users";
        int sentCount = 0;

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(userSql)) {

            List<int[]> userList = new ArrayList<>();
            List<String> nameList = new ArrayList<>();
            while (rs.next()) {
                userList.add(new int[]{ rs.getInt("id") });
                nameList.add(rs.getString("full_name"));
            }

            for (int i = 0; i < userList.size(); i++) {
                int uId = userList.get(i)[0];
                String uName = nameList.get(i);

                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setInt(1, uId);
                    checkPs.setString(2, matchTitle);
                    checkPs.setString(3, matchTitle);
                    try (ResultSet crs = checkPs.executeQuery()) {
                        if (crs.next() && crs.getInt(1) > 0) {
                            continue; // Already delivered to this user
                        }
                    }
                }

                String personalizedMsg = message.replace("{recipientName}", uName != null ? uName : "User");
                boolean ok = sendMessage(
                        0,
                        "AcadsCatchUp System",
                        "SYSTEM",
                        uId,
                        uName != null ? uName : "User",
                        title,
                        personalizedMsg,
                        null,
                        null,
                        null,
                        "UPDATE",
                        null,
                        null,
                        null
                );
                if (ok) sentCount++;
            }
        } catch (SQLException e) {
            System.err.println("[InboxDAO] broadcastSystemUpdateNotice error: " + e.getMessage());
        }
        return sentCount;
    }

    /**
     * Checks if the user has already received the update notice for the given version.
     */
    public boolean hasUserReceivedUpdate(int recipientId, String version) {
        String cleanVer = version != null ? version.replace("v", "").trim() : "";
        String matchTitle = "%v" + cleanVer + "%";
        String checkSql = "SELECT COUNT(*) FROM inbox_messages WHERE recipient_id = ? AND (title LIKE ? OR message LIKE ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, recipientId);
            checkPs.setString(2, matchTitle);
            checkPs.setString(3, matchTitle);
            try (ResultSet crs = checkPs.executeQuery()) {
                return crs.next() && crs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[InboxDAO] hasUserReceivedUpdate error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ensures that the specified user has received the What's New update announcement in their inbox.
     */
    public boolean sendUpdateNoticeIfNew(int recipientId, String recipientName, String version, String title, String message) {
        if (hasUserReceivedUpdate(recipientId, version)) {
            return false; // Already delivered
        }

        String personalizedMsg = message.replace("{recipientName}", recipientName != null ? recipientName : "User");
        return sendMessage(
                0,
                "AcadsCatchUp System",
                "SYSTEM",
                recipientId,
                recipientName != null ? recipientName : "User",
                title,
                personalizedMsg,
                null,
                null,
                null,
                "UPDATE",
                null,
                null,
                null
        );
    }
}
