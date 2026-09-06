package com.acadscatchup.controller;

import com.acadscatchup.dao.InboxDAO;
import com.acadscatchup.dao.MissedItemDAO;
import com.acadscatchup.model.InboxMessage;
import com.acadscatchup.model.MissedItem;
import com.acadscatchup.model.User;
import com.acadscatchup.util.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for Student and Professor personal inbox.
 * Receives student submissions and bug report resolution notifications.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class UserInboxController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private HBox headerBar;
    @FXML private Label inboxTitleLabel;
    @FXML private Label unreadBadge;
    @FXML private TableView<InboxMessage> messagesTable;
    @FXML private TableColumn<InboxMessage, String> colType;
    @FXML private TableColumn<InboxMessage, String> colSender;
    @FXML private TableColumn<InboxMessage, String> colSubject;
    @FXML private TableColumn<InboxMessage, String> colDate;
    @FXML private TableColumn<InboxMessage, String> colStatus;

    @FXML private Label detailSenderLabel;
    @FXML private TextArea detailMessageArea;
    @FXML private Button btnGradeItem;
    @FXML private Button btnViewAttached;
    @FXML private Button btnMarkAllGraded;

    private final InboxDAO inboxDAO = new InboxDAO();
    private final MissedItemDAO missedItemDAO = new MissedItemDAO();
    private final ObservableList<InboxMessage> tableData = FXCollections.observableArrayList();

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        if (headerBar != null) {
            headerBar.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            headerBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) headerBar.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }

        User user = Session.getCurrentUser();
        if (user != null) {
            inboxTitleLabel.setText(user.getFullName() + "'s Inbox");
        }

        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeBadge()));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(type);
                String t = type.toLowerCase();
                if (t.contains("quiz")) {
                    badge.setStyle("-fx-background-color: rgba(245,158,11,0.22); -fx-text-fill: #fbbf24; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else if (t.contains("activity") || t.contains("assignment")) {
                    badge.setStyle("-fx-background-color: rgba(249,115,22,0.22); -fx-text-fill: #fb923c; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else if (t.contains("exam")) {
                    badge.setStyle("-fx-background-color: rgba(239,68,68,0.22); -fx-text-fill: #f87171; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else if (t.contains("graded")) {
                    badge.setStyle("-fx-background-color: rgba(34,197,94,0.22); -fx-text-fill: #4ade80; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else if (t.contains("submission")) {
                    badge.setStyle("-fx-background-color: rgba(59,130,246,0.22); -fx-text-fill: #60a5fa; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else if (t.contains("resolved") || t.contains("bug")) {
                    badge.setStyle("-fx-background-color: rgba(16,185,129,0.22); -fx-text-fill: #34d399; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else if (t.contains("update") || t.contains("what's new") || t.contains("system")) {
                    badge.setStyle("-fx-background-color: rgba(168,85,247,0.25); -fx-text-fill: #c084fc; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else {
                    badge.setStyle("-fx-background-color: rgba(148,163,184,0.22); -fx-text-fill: #94a3b8; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                }
                setGraphic(badge);
                setText(null);
            }
        });
        colSender.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSenderName()));
        colSubject.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));

        // Status badge: New vs Read
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isRead() ? "Read" : "NEW"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status);
                if ("NEW".equalsIgnoreCase(status)) {
                    badge.setStyle("-fx-background-color: rgba(59,130,246,0.25); -fx-text-fill: #60a5fa; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else {
                    badge.setStyle("-fx-background-color: rgba(148,163,184,0.15); -fx-text-fill: #94a3b8; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        // Listen for message selection
        messagesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                detailSenderLabel.setText("From: " + selected.getSenderName() + " (" + selected.getSenderRole() + ")  •  " + selected.getCreatedAt());
                detailMessageArea.setText(selected.getMessage());

                // Auto mark as read
                if (!selected.isRead()) {
                    inboxDAO.markAsRead(selected.getId());
                    selected.setRead(true);
                    messagesTable.refresh();
                    updateBadge();
                }

                // Show "Mark Item as Graded" button only for professors when item is attached
                boolean canGrade = (Session.getCurrentUser() != null && Session.getCurrentUser().isProfessor())
                                   && selected.getItemId() != null && selected.getItemId() > 0;
                btnGradeItem.setVisible(canGrade);
                btnGradeItem.setManaged(canGrade);
                btnGradeItem.setDisable(false);

                // Show "View Attached" button if message has an attached link or file
                boolean hasAtt = selected.hasAttachment();
                btnViewAttached.setVisible(hasAtt);
                btnViewAttached.setManaged(hasAtt);
                if (hasAtt) {
                    if ("FILE".equalsIgnoreCase(selected.getAttachmentType())) {
                        String fn = selected.getAttachmentName();
                        btnViewAttached.setText("📎 View Attached File" + (fn != null && !fn.isBlank() ? " (" + fn + ")" : ""));
                    } else {
                        btnViewAttached.setText("🔗 View Attached Link");
                    }
                }
            } else {
                detailSenderLabel.setText("Select a message above to read its content.");
                detailMessageArea.clear();
                btnGradeItem.setVisible(false);
                btnGradeItem.setManaged(false);
                btnViewAttached.setVisible(false);
                btnViewAttached.setManaged(false);
            }
        });

        btnGradeItem.setVisible(false);
        btnGradeItem.setManaged(false);
        btnViewAttached.setVisible(false);
        btnViewAttached.setManaged(false);

        boolean isProf = Session.getCurrentUser() != null && (Session.getCurrentUser().isProfessor() || Session.getCurrentUser().isAdmin());
        btnMarkAllGraded.setVisible(isProf);
        btnMarkAllGraded.setManaged(isProf);

        messagesTable.setItems(tableData);
        loadMessages();
    }

    private void loadMessages() {
        User user = Session.getCurrentUser();
        if (user == null) return;

        List<InboxMessage> list = inboxDAO.getMessagesForRecipient(user.getId());
        tableData.setAll(list);
        updateBadge();

        if (!list.isEmpty() && messagesTable.getSelectionModel().getSelectedItem() == null) {
            messagesTable.getSelectionModel().select(0);
        }
    }

    private void updateBadge() {
        User user = Session.getCurrentUser();
        if (user == null) return;
        int unread = inboxDAO.getUnreadCount(user.getId());
        unreadBadge.setText(unread + " Unread");
        if (unread > 0) {
            unreadBadge.setStyle("-fx-background-color: rgba(59,130,246,0.25); -fx-text-fill: #60a5fa; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else {
            unreadBadge.setStyle("-fx-background-color: rgba(148,163,184,0.15); -fx-text-fill: #94a3b8; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 12px;");
        }
    }

    @FXML
    private void handleMarkGraded() {
        InboxMessage selected = messagesTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getItemId() == null) return;

        int itemId = selected.getItemId();
        if (missedItemDAO.updateStatus(itemId, "GRADED")) {
            MissedItem item = missedItemDAO.getById(itemId);
            if (item != null) {
                User prof = Session.getCurrentUser();
                int profId = (prof != null) ? prof.getId() : 0;
                String profName = (prof != null) ? prof.getFullName() : "Your Professor";
                inboxDAO.sendGradedNotice(item.getStudentId(), item.getStudentName(), profId, profName, item);
            }

            com.acadscatchup.util.CustomAlert.showInfo(messagesTable.getScene().getWindow(),
                    "Graded Successfully",
                    "Deficiency item has been marked as GRADED and the student has been notified in their Inbox!");
            btnGradeItem.setDisable(true);
        } else {
            com.acadscatchup.util.CustomAlert.showError(messagesTable.getScene().getWindow(),
                    "Update Error", "Could not update item status.");
        }
    }

    @FXML
    private void handleViewAttached() {
        InboxMessage selected = messagesTable.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.hasAttachment()) return;

        String attType = selected.getAttachmentType();
        String attUrl = selected.getAttachmentUrl();
        String attName = selected.getAttachmentName();

        if ("FILE".equalsIgnoreCase(attType)) {
            try {
                byte[] data = java.util.Base64.getDecoder().decode(attUrl.trim());
                String filename = (attName != null && !attName.isBlank()) ? attName : ("attachment_" + selected.getId() + ".bin");
                java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"), "AcadsCatchUp");
                tempDir.mkdirs();
                java.io.File tempFile = new java.io.File(tempDir, filename);
                java.nio.file.Files.write(tempFile.toPath(), data);

                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(tempFile);
                } else {
                    com.acadscatchup.util.CustomAlert.showInfo(
                            messagesTable.getScene().getWindow(),
                            "Attached File Saved",
                            "File saved to:\n" + tempFile.getAbsolutePath()
                    );
                }
            } catch (Exception e) {
                com.acadscatchup.util.CustomAlert.showError(
                        messagesTable.getScene().getWindow(),
                        "Attachment Error",
                        "Could not open attached file: " + e.getMessage()
                );
            }
        } else {
            // LINK
            try {
                String url = (attUrl != null) ? attUrl.trim() : "";
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } else {
                    com.acadscatchup.util.CustomAlert.showInfo(
                            messagesTable.getScene().getWindow(),
                            "Attached Link",
                            "URL:\n" + url
                    );
                }
            } catch (Exception e) {
                com.acadscatchup.util.CustomAlert.showError(
                        messagesTable.getScene().getWindow(),
                        "Link Error",
                        "Could not open link in browser: " + e.getMessage()
                );
            }
        }
    }

    @FXML
    private void handleMarkAllGraded() {
        User prof = Session.getCurrentUser();
        if (prof == null) return;

        List<InboxMessage> subMsgs = tableData.stream()
                .filter(m -> m.getItemId() != null && m.getItemId() > 0)
                .toList();

        if (subMsgs.isEmpty()) {
            com.acadscatchup.util.CustomAlert.showInfo(
                    messagesTable.getScene().getWindow(),
                    "No Items to Grade",
                    "There are no student deficiency submissions found in your inbox."
            );
            return;
        }

        boolean confirmed = com.acadscatchup.util.CustomAlert.showConfirmation(
                messagesTable.getScene().getWindow(),
                "Mark All Items as Graded",
                "Are you sure you want to mark all " + subMsgs.size() + " submission item(s) in your inbox as GRADED?\n\nThis will update all items and notify each student in real time."
        );

        if (!confirmed) return;

        int updatedCount = 0;
        java.util.Set<Integer> processedItemIds = new java.util.HashSet<>();

        for (InboxMessage msg : subMsgs) {
            int itemId = msg.getItemId();
            if (processedItemIds.contains(itemId)) continue;
            processedItemIds.add(itemId);

            if (missedItemDAO.updateStatus(itemId, "GRADED")) {
                updatedCount++;
                MissedItem item = missedItemDAO.getById(itemId);
                if (item != null) {
                    inboxDAO.sendGradedNotice(item.getStudentId(), item.getStudentName(), prof.getId(), prof.getFullName(), item);
                }
            }
        }

        loadMessages();

        com.acadscatchup.util.CustomAlert.showInfo(
                messagesTable.getScene().getWindow(),
                "All Items Graded",
                "Successfully marked " + updatedCount + " deficiency item(s) as GRADED!\nAll students have been notified."
        );
    }

    @FXML
    private void handleDelete() {
        InboxMessage selected = messagesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean confirmed = com.acadscatchup.util.CustomAlert.showConfirmation(
                messagesTable.getScene().getWindow(),
                "Delete Message",
                "Are you sure you want to delete this message from your inbox?");
        if (confirmed) {
            if (inboxDAO.deleteMessage(selected.getId())) {
                loadMessages();
            }
        }
    }

    @FXML
    private void handleClose() {
        com.acadscatchup.util.ModalOverlay.close(messagesTable);
    }
}
