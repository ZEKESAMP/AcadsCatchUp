package com.acadscatchup.controller;

import com.acadscatchup.dao.MissedItemDAO;
import com.acadscatchup.dao.SubjectDAO;
import com.acadscatchup.model.MissedItem;
import com.acadscatchup.model.Subject;
import com.acadscatchup.util.Session;
import com.acadscatchup.util.LiveSyncService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Student dashboard controller — view own items and mark as submitted.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class StudentDashboardController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private HBox topBar;
    @FXML private Label appTitleLabel;
    @FXML private Label roleBadge;
    @FXML private Label studentNameLabel;
    @FXML private Label syncStatusLabel;
    @FXML private Button inboxBtn;
    @FXML private Button helpBtn;
    @FXML private Button updatesBtn;
    @FXML private Button settingsBtn;
    @FXML private Button studentLogoutBtn;
    @FXML private ScrollPane dashboardScrollPane;
    @FXML private HBox statsRow;

    @FXML private VBox cardTotal;
    @FXML private VBox cardPending;
    @FXML private VBox cardSubmitted;
    @FXML private VBox cardGraded;

    @FXML private Label totalCount;
    @FXML private Label pendingCount;
    @FXML private Label submittedCount;
    @FXML private Label gradedCount;

    @FXML private Label enrolledCountBadge;
    @FXML private FlowPane enrolledSubjectsBox;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> subjectFilterCombo;
    @FXML private Button exportCsvBtn;

    private final Map<String, VBox> subjectCardMap = new HashMap<>();

    @FXML private TableView<MissedItem>          itemsTable;
    @FXML private TableColumn<MissedItem,String> colSubject;
    @FXML private TableColumn<MissedItem,String> colProfName;
    @FXML private TableColumn<MissedItem,String> colType;
    @FXML private TableColumn<MissedItem,String> colItemName;
    @FXML private TableColumn<MissedItem,String> colDateMissed;
    @FXML private TableColumn<MissedItem,String> colDeadline;
    @FXML private TableColumn<MissedItem,String> colStatus;
    @FXML private TableColumn<MissedItem,String> colNotes;

    private final MissedItemDAO missedItemDAO = new MissedItemDAO();
    private final SubjectDAO    subjectDAO    = new SubjectDAO();
    private final com.acadscatchup.dao.InboxDAO inboxDAO = new com.acadscatchup.dao.InboxDAO();

    private ObservableList<MissedItem> tableData = FXCollections.observableArrayList();
    private FilteredList<MissedItem> filteredData;
    private com.acadscatchup.util.LoadingOverlay loadingOverlay;
    private boolean notifiedThisSession = false;

    @FXML
    private void initialize() {
        com.acadscatchup.model.User curr = Session.getCurrentUser();
        String tag = (curr != null && curr.getProgram() != null && !curr.getProgram().isBlank())
                ? " (" + curr.getProgram() + (curr.getYearLevel() > 0 ? " • " + curr.getYearDisplay() : "") + ")"
                : "";
        String fullDisplayName = com.acadscatchup.util.OSCompat.label("\uD83D\uDC64 ") + (curr != null ? curr.getFullName() : "") + tag;
        studentNameLabel.setText(fullDisplayName);
        studentNameLabel.setTooltip(new Tooltip(fullDisplayName));

        // Modern auto-scaling responsiveness (compact mode on resize)
        com.acadscatchup.util.ResponsiveLayoutUtil.installStudentResponsiveLayout(
                dashboardScrollPane,
                topBar,
                appTitleLabel,
                roleBadge,
                syncStatusLabel,
                inboxBtn,
                helpBtn,
                settingsBtn,
                studentNameLabel,
                studentLogoutBtn,
                statsRow
        );

        refreshInboxBadge();
        setupStatCards();
        setupFilters();
        setupTable();
        loadItems();
        loadEnrolledSubjects();
        com.acadscatchup.util.UpdateSplash.checkAndBadgeUpdatesButton(updatesBtn);

        // Live real-time background sync engine (checks cloud fingerprint every 3.5s)
        if (curr != null) {
            liveSyncService = LiveSyncService.forStudent(curr.getId(), new LiveSyncService.SyncListener() {
                @Override
                public void onDataChanged() {
                    loadItemsAsync(false);
                    loadEnrolledSubjects();
                    queryInboxInBackground();
                }

                @Override
                public void onStatusChanged(LiveSyncService.SyncStatus status) {
                    updateSyncBadge(status);
                }
            });
            liveSyncService.start();
        }

        // Register custom FAQ-style close confirmation dialog
        javafx.application.Platform.runLater(() -> {
            if (studentNameLabel.getScene() != null && studentNameLabel.getScene().getWindow() instanceof Stage s) {
                s.setOnCloseRequest(e -> {
                    e.consume();
                    com.acadscatchup.util.AppTrayManager.handleCloseRequest(s);
                });
            }
            // Cross-platform emoji patching for Linux
            if (studentNameLabel.getScene() != null) {
                com.acadscatchup.util.OSCompat.patchEmojis(studentNameLabel.getScene().getRoot());
            }
        });
    }

    private int lastSeenUnreadCount = -1;
    private boolean notifiedOnLogin = false;
    private LiveSyncService liveSyncService = null;

    private void updateSyncBadge(LiveSyncService.SyncStatus status) {
        if (syncStatusLabel == null) return;
        syncStatusLabel.setText(status.label);
        syncStatusLabel.setStyle(
                "-fx-text-fill: " + status.textColor + "; " +
                "-fx-background-color: " + status.bgColor + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 3 8;"
        );
    }

    private void refreshInboxBadge() {
        new Thread(this::queryInboxInBackground, "Student-Inbox-Refresh").start();
    }

    private void queryInboxInBackground() {
        try {
            com.acadscatchup.model.User curr = Session.getCurrentUser();
            if (curr == null || inboxBtn == null) return;

            // 1. Fast lightweight COUNT query
            int unread = inboxDAO.getUnreadCount(curr.getId());

            // 2. Only fetch full messages if there are unread messages to inspect for notification alerts
            com.acadscatchup.model.InboxMessage alertMessage = null;
            if (unread > 0 && (!notifiedOnLogin || (lastSeenUnreadCount != -1 && unread > lastSeenUnreadCount))) {
                List<com.acadscatchup.model.InboxMessage> msgs = inboxDAO.getMessagesForRecipient(curr.getId());
                for (com.acadscatchup.model.InboxMessage m : msgs) {
                    if (!m.isRead()) {
                        String titleL = (m.getTitle() != null) ? m.getTitle().toLowerCase() : "";
                        String typeL = (m.getMsgType() != null) ? m.getMsgType().toLowerCase() : "";
                        if (titleL.contains("graded") || typeL.contains("graded") ||
                            titleL.contains("enroll") || typeL.contains("enroll") ||
                            titleL.startsWith("missed") || typeL.startsWith("missed") ||
                            typeL.contains("resolved") || titleL.contains("resolved") ||
                            typeL.contains("update") || titleL.contains("update") ||
                            titleL.contains("what's new")) {
                            alertMessage = m;
                            break;
                        }
                    }
                }
            }

            final int finalUnread = unread;
            final com.acadscatchup.model.InboxMessage finalAlert = alertMessage;
            javafx.application.Platform.runLater(() -> processInboxUpdate(curr, finalUnread, finalAlert));
        } catch (Exception e) {
            System.err.println("[StudentInbox] " + e.getMessage());
        }
    }

    private void processInboxUpdate(com.acadscatchup.model.User curr, int unread, com.acadscatchup.model.InboxMessage alertMessage) {
        if (inboxBtn == null) return;
        inboxBtn.setText(com.acadscatchup.util.OSCompat.label("📬 ") + "Inbox (" + unread + ")");
        if (unread > 0) {
            inboxBtn.setStyle("-fx-background-color: rgba(59,130,246,0.3); -fx-text-fill: #93c5fd; -fx-font-weight: bold;");
        } else {
            inboxBtn.setStyle("");
        }

        // Trigger notification on initial load if unread actionable item exists, or when a new one arrives
        boolean shouldNotify = (!notifiedOnLogin && alertMessage != null) ||
                (lastSeenUnreadCount != -1 && unread > lastSeenUnreadCount && alertMessage != null);

        if (shouldNotify && alertMessage != null) {
            notifiedOnLogin = true;
            final com.acadscatchup.model.InboxMessage notice = alertMessage;
            boolean isGraded = (notice.getTitle() != null && notice.getTitle().toLowerCase().contains("graded"))
                    || "GRADED".equalsIgnoreCase(notice.getMsgType());
            boolean isEnrolled = (notice.getTitle() != null && (notice.getTitle().toLowerCase().contains("enrolled") || notice.getTitle().toLowerCase().contains("enrollment")))
                    || "ENROLLMENT".equalsIgnoreCase(notice.getMsgType()) || "ENROLLED".equalsIgnoreCase(notice.getMsgType());
            boolean isResolved = (notice.getTitle() != null && notice.getTitle().toLowerCase().contains("resolved"))
                    || "REPORT_RESOLVED".equalsIgnoreCase(notice.getMsgType());
            boolean isUpdate = (notice.getTitle() != null && (notice.getTitle().toLowerCase().contains("update") || notice.getTitle().toLowerCase().contains("what's new")))
                    || "UPDATE".equalsIgnoreCase(notice.getMsgType()) || "SYSTEM_UPDATE".equalsIgnoreCase(notice.getMsgType());

            String toastTitle;
            String toastBody;

            if (isUpdate) {
                toastTitle = "AcadsCatchUp • What's New Update 🚀";
                toastBody = "Hi " + curr.getFullName() + "! " + notice.getTitle() + " has arrived in your Inbox. Click to view release notes!";
            } else if (isEnrolled) {
                toastTitle = "AcadsCatchUp • Subject Enrollment 🎓";
                toastBody = "Hi " + curr.getFullName() + "! You have been enrolled in " + (notice.getSubjectCode() != null && !notice.getSubjectCode().isBlank() ? notice.getSubjectCode() : "a subject") + " by " + (notice.getSenderName() != null ? notice.getSenderName() : "your instructor") + ". Check your Inbox & Enrolled Subjects!";
            } else if (isResolved) {
                toastTitle = "AcadsCatchUp • Bug Report Resolved ✔";
                toastBody = "Hi " + curr.getFullName() + "! Your reported issue has been addressed and marked as RESOLVED by the System Administrator.";
            } else if (isGraded) {
                toastTitle = "AcadsCatchUp • Submission Graded 🎉";
                toastBody = "Hi " + curr.getFullName() + "! " + notice.getTitle() + " has been marked as GRADED by your professor!";
            } else {
                toastTitle = "AcadsCatchUp • " + notice.getTypeBadge() + " ⚠️";
                toastBody = "Hi " + curr.getFullName() + "! New deficiency recorded: " + notice.getTitle() + ". Check your Inbox.";
            }

            com.acadscatchup.util.WindowsNotificationUtil.showNotification(
                    toastTitle,
                    toastBody,
                    (isResolved || isGraded || isUpdate || isEnrolled) ? java.awt.TrayIcon.MessageType.INFO : java.awt.TrayIcon.MessageType.WARNING
            );
            loadItems(); // Automatically refresh table to show new items or updated status!
            if (isEnrolled) {
                loadEnrolledSubjects();
            }
        }

        lastSeenUnreadCount = unread;
    }

    private void setupStatCards() {
        if (cardTotal != null) {
            cardTotal.setCursor(Cursor.HAND);
            Tooltip.install(cardTotal, new Tooltip("Click to filter by ALL items"));
            cardTotal.setOnMouseClicked(e -> {
                if (statusFilterCombo != null) statusFilterCombo.setValue("ALL");
            });
        }
        if (cardPending != null) {
            cardPending.setCursor(Cursor.HAND);
            Tooltip.install(cardPending, new Tooltip("Click to filter by PENDING items"));
            cardPending.setOnMouseClicked(e -> {
                if (statusFilterCombo != null) statusFilterCombo.setValue("PENDING");
            });
        }
        if (cardSubmitted != null) {
            cardSubmitted.setCursor(Cursor.HAND);
            Tooltip.install(cardSubmitted, new Tooltip("Click to filter by SUBMITTED items"));
            cardSubmitted.setOnMouseClicked(e -> {
                if (statusFilterCombo != null) statusFilterCombo.setValue("SUBMITTED");
            });
        }
        if (cardGraded != null) {
            cardGraded.setCursor(Cursor.HAND);
            Tooltip.install(cardGraded, new Tooltip("Click to filter by GRADED items"));
            cardGraded.setOnMouseClicked(e -> {
                if (statusFilterCombo != null) statusFilterCombo.setValue("GRADED");
            });
        }
    }

    private void setupFilters() {
        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "ALL", "PENDING", "SUBMITTED", "GRADED"));
        statusFilterCombo.setValue("ALL");

        // Load enrolled subjects (or all system subjects if none specifically mapped)
        int studentId = Session.getCurrentUser() != null ? Session.getCurrentUser().getId() : 0;
        List<Subject> subjects = subjectDAO.getSubjectsByStudent(studentId);
        if (subjects.isEmpty()) {
            subjects = subjectDAO.getAllSubjects();
        }
        ObservableList<String> codes = FXCollections.observableArrayList("ALL");
        subjects.forEach(s -> codes.add(s.getCode()));
        subjectFilterCombo.setItems(codes);
        subjectFilterCombo.setValue("ALL");

        // Dynamic instant filtering
        statusFilterCombo.valueProperty().addListener((obs, o, n) -> loadItems());
        subjectFilterCombo.valueProperty().addListener((obs, o, n) -> {
            updateSelectedSubjectCards(n);
            loadItems();
        });
    }

    private void setupTable() {
        colSubject   .setCellValueFactory(d -> {
            String code = d.getValue().getSubjectCode();
            String name = d.getValue().getSubjectName();
            if (name != null && !name.isBlank()) {
                return new SimpleStringProperty(code + " - " + name);
            }
            return new SimpleStringProperty(code != null ? code : "");
        });
        colProfName  .setCellValueFactory(d -> {
            String prof = d.getValue().getProfName();
            return new SimpleStringProperty((prof != null && !prof.isBlank()) ? prof : "Not Assigned");
        });
        colType      .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemType()));
        colItemName  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        colDateMissed.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDateMissed() != null ? d.getValue().getDateMissed().toString() : ""));
        colDeadline  .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDeadline() != null ? d.getValue().getDeadline().toString() : ""));
        colNotes     .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNotes() != null ? d.getValue().getNotes() : ""));

        // Status badge
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status);
                badge.getStyleClass().add(switch (status) {
                    case "SUBMITTED" -> "status-badge-submitted";
                    case "GRADED"    -> "status-badge-graded";
                    default          -> "status-badge-pending";
                });
                setGraphic(badge);
                setText(null);
            }
        });

        // Row styling + double-click row shortcut
        itemsTable.setRowFactory(tv -> {
            TableRow<MissedItem> row = new TableRow<>() {
                @Override
                protected void updateItem(MissedItem item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("row-pending","row-submitted","row-graded","row-overdue");
                    if (item != null && !empty) {
                        if (item.isOverdue())                              getStyleClass().add("row-overdue");
                        else if ("SUBMITTED".equals(item.getStatus()))     getStyleClass().add("row-submitted");
                        else if ("GRADED".equals(item.getStatus()))        getStyleClass().add("row-graded");
                        else                                               getStyleClass().add("row-pending");
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleMarkSubmitted();
                }
            });
            return row;
        });

        filteredData = new FilteredList<>(tableData, p -> true);
        SortedList<MissedItem> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(itemsTable.comparatorProperty());
        itemsTable.setItems(sortedData);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> updateSearchFilter(n));
        }
    }

    private void updateSearchFilter(String query) {
        if (filteredData == null) return;
        if (query == null || query.isBlank()) {
            filteredData.setPredicate(item -> true);
            return;
        }
        String q = query.trim().toLowerCase();
        filteredData.setPredicate(item -> {
            if (item == null) return false;
            String itemName = item.getItemName() != null ? item.getItemName().toLowerCase() : "";
            String subjCode = item.getSubjectCode() != null ? item.getSubjectCode().toLowerCase() : "";
            String subjName = item.getSubjectName() != null ? item.getSubjectName().toLowerCase() : "";
            String profName = item.getProfName() != null ? item.getProfName().toLowerCase() : "";
            String itemType = item.getItemType() != null ? item.getItemType().toLowerCase() : "";
            String status   = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
            String notes    = item.getNotes() != null ? item.getNotes().toLowerCase() : "";
            String dateMiss = item.getDateMissed() != null ? item.getDateMissed().toString().toLowerCase() : "";
            String deadline = item.getDeadline() != null ? item.getDeadline().toString().toLowerCase() : "";

            return itemName.contains(q)
                    || subjCode.contains(q)
                    || subjName.contains(q)
                    || profName.contains(q)
                    || itemType.contains(q)
                    || status.contains(q)
                    || notes.contains(q)
                    || dateMiss.contains(q)
                    || deadline.contains(q);
        });
    }

    private void loadItems() {
        loadItemsAsync(false);
    }

    private void loadItemsAsync(boolean showOverlay) {
        int studentId  = Session.getCurrentUser().getId();
        String status  = statusFilterCombo.getValue();
        String subject = subjectFilterCombo.getValue();

        // Create overlay if first time
        if (showOverlay && loadingOverlay == null && studentNameLabel.getScene() != null) {
            javafx.scene.Parent sceneRoot = studentNameLabel.getScene().getRoot();
            if (sceneRoot instanceof javafx.scene.layout.Pane p) {
                loadingOverlay = new com.acadscatchup.util.LoadingOverlay(p);
            }
        }
        if (showOverlay && loadingOverlay != null) {
            loadingOverlay.show("Loading data...");
        }

        new Thread(() -> {
            try {
                List<MissedItem> items = missedItemDAO.getByStudent(studentId, status, subject);
                int[] stats = missedItemDAO.getStudentStats(studentId);
                javafx.application.Platform.runLater(() -> {
                    tableData.setAll(items);
                    updateStatsFromCache(stats);
                    if (loadingOverlay != null) loadingOverlay.hide();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (loadingOverlay != null) loadingOverlay.hide();
                });
                System.err.println("[StudentLoadItems] " + e.getMessage());
            }
        }, "StudentLoadItems-Worker").start();
    }

    private void updateStatsFromCache(int[] stats) {
        totalCount    .setText(String.valueOf(stats[0]));
        pendingCount  .setText(String.valueOf(stats[1]));
        submittedCount.setText(String.valueOf(stats[2]));
        gradedCount   .setText(String.valueOf(stats[3]));

        if (!notifiedThisSession && Session.getCurrentUser() != null) {
            notifiedThisSession = true;
            LocalDate nearest = null;
            for (MissedItem mi : tableData) {
                if ("PENDING".equalsIgnoreCase(mi.getStatus()) && mi.getDeadline() != null) {
                    if (nearest == null || mi.getDeadline().isBefore(nearest)) {
                        nearest = mi.getDeadline();
                    }
                }
            }
            String nearestDeadlineStr = nearest != null ? nearest.toString() : null;
            com.acadscatchup.util.WindowsNotificationUtil.notifyStudentDeficiencies(
                    Session.getCurrentUser(), stats[1], nearestDeadlineStr);
        }
    }

    @FXML private void applyFilters() { loadItemsAsync(true); }

    @FXML private void clearFilters() {
        if (searchField != null) searchField.clear();
        statusFilterCombo.setValue("ALL");
        subjectFilterCombo.setValue("ALL");
        updateSelectedSubjectCards("ALL");
        loadItemsAsync(true);
    }

    @FXML
    private void handleExportCSV() {
        if (tableData.isEmpty()) {
            com.acadscatchup.util.CustomAlert.showWarning(studentNameLabel.getScene().getWindow(),
                    "Export CSV", "No missed items to export.");
            return;
        }
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export My Missed Items to CSV");
        String defaultName = "My_Missed_Items_" + java.time.LocalDate.now() + ".csv";
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        java.io.File file = fileChooser.showSaveDialog(studentNameLabel.getScene().getWindow());
        if (file != null) {
            boolean ok = com.acadscatchup.util.CSVExporter.export(tableData, file);
            if (ok) {
                com.acadscatchup.util.CustomAlert.showInfo(studentNameLabel.getScene().getWindow(),
                        "Export Successful", "Checklist exported successfully to:\n" + file.getAbsolutePath());
            } else {
                com.acadscatchup.util.CustomAlert.showError(studentNameLabel.getScene().getWindow(),
                        "Export Failed", "Failed to write CSV file. Please check file permissions.");
            }
        }
    }

    @FXML
    private void handleMarkSubmitted() {
        MissedItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            com.acadscatchup.util.CustomAlert.showWarning(studentNameLabel.getScene().getWindow(),
                    "Selection Required", "Please select an item to submit.");
            return;
        }
        if (!"PENDING".equals(selected.getStatus())) {
            com.acadscatchup.util.CustomAlert.showInfo(studentNameLabel.getScene().getWindow(),
                    "Already Processed", "This item is already " + selected.getStatus() + ".");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/submit_item_dialog.fxml"));
            Parent root = loader.load();
            SubmitItemController ctrl = loader.getController();
            ctrl.setItem(selected);

            com.acadscatchup.util.ModalOverlay.showAndWait(studentNameLabel, root, 560, 620);

            if (ctrl.isSubmitted()) {
                loadItems();
                loadEnrolledSubjects();
                refreshInboxBadge();
                if (liveSyncService != null) {
                    liveSyncService.triggerImmediateSync();
                }
                com.acadscatchup.util.CustomAlert.showInfo(studentNameLabel.getScene().getWindow(),
                        "Submission Sent",
                        "\"" + selected.getItemName() + "\" successfully submitted directly to your professor!");
            }
        } catch (IOException e) {
            com.acadscatchup.util.CustomAlert.showError(studentNameLabel.getScene().getWindow(),
                    "Dialog Error", "Could not open submission dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenInbox() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/user_inbox.fxml"));
            Parent root = loader.load();
            com.acadscatchup.util.ModalOverlay.showAndWait(studentNameLabel, root, 840, 600);

            refreshInboxBadge();
            loadItems();
            if (liveSyncService != null) {
                liveSyncService.triggerImmediateSync();
            }
        } catch (IOException e) {
            com.acadscatchup.util.CustomAlert.showError(studentNameLabel.getScene().getWindow(),
                    "Inbox Error", "Could not open Inbox: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        if (liveSyncService != null) {
            liveSyncService.shutdown();
            liveSyncService = null;
        }
        Session.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) studentNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMinWidth(480);
            stage.setMinHeight(580);
            stage.setTitle("AcadsCatchUp — Login");
            com.acadscatchup.util.WindowUtil.initFullScreenWithCentering(stage, 540, 720);
            com.acadscatchup.util.AppTrayManager.setCurrentStage(stage);
            stage.setOnCloseRequest(e -> {
                e.consume();
                com.acadscatchup.util.AppTrayManager.handleCloseRequest(stage);
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleOpenHelpReport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/help_report_dialog.fxml"));
            Parent root = loader.load();
            com.acadscatchup.util.ModalOverlay.showAndWait(studentNameLabel, root, 560, 520);
        } catch (IOException e) {
            com.acadscatchup.util.CustomAlert.showError(studentNameLabel.getScene().getWindow(),
                    "Report Error", "Could not open Help Report: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenUpdates() {
        com.acadscatchup.util.UpdatesDialog.show(studentNameLabel.getScene().getWindow());
    }

    @FXML
    private void handleOpenAccountSettings() {
        com.acadscatchup.util.AccountSettingsDialog.show(studentNameLabel.getScene().getWindow());
    }

    /**
     * Loads the enrolled subjects for the logged-in student, counts deficiencies per subject,
     * and displays them in the interactive Enrolled Subjects dashboard section.
     */
    private void loadEnrolledSubjects() {
        com.acadscatchup.model.User curr = Session.getCurrentUser();
        if (curr == null) return;
        int studentId = curr.getId();

        new Thread(() -> {
            try {
                List<Subject> subjects = subjectDAO.getSubjectsByStudent(studentId);
                List<MissedItem> allItems = missedItemDAO.getByStudent(studentId, "ALL", "ALL");

                // Count pending items per subject code
                Map<String, Integer> pendingMap = new HashMap<>();
                for (MissedItem item : allItems) {
                    if ("PENDING".equalsIgnoreCase(item.getStatus()) && item.getSubjectCode() != null) {
                        String codeKey = item.getSubjectCode().trim().toUpperCase();
                        pendingMap.put(codeKey, pendingMap.getOrDefault(codeKey, 0) + 1);
                    }
                }

                javafx.application.Platform.runLater(() -> {
                    renderEnrolledSubjects(subjects, pendingMap);
                    if (subjectFilterCombo != null) {
                        String currentVal = subjectFilterCombo.getValue();
                        ObservableList<String> codes = FXCollections.observableArrayList("ALL");
                        subjects.forEach(s -> codes.add(s.getCode()));
                        subjectFilterCombo.setItems(codes);
                        if (currentVal != null && codes.contains(currentVal)) {
                            subjectFilterCombo.setValue(currentVal);
                        } else {
                            subjectFilterCombo.setValue("ALL");
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("[StudentDashboard] loadEnrolledSubjects error: " + e.getMessage());
            }
        }, "LoadEnrolledSubjects-Worker").start();
    }

    private void renderEnrolledSubjects(List<Subject> subjects, Map<String, Integer> pendingMap) {
        if (enrolledSubjectsBox == null) return;
        enrolledSubjectsBox.getChildren().clear();
        subjectCardMap.clear();

        if (enrolledCountBadge != null) {
            int count = subjects.size();
            enrolledCountBadge.setText(count + (count == 1 ? " Subject" : " Subjects"));
        }

        if (subjects.isEmpty()) {
            HBox emptyBox = new HBox(8);
            emptyBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            emptyBox.getStyleClass().add("enrolled-empty-box");

            Label emptyLbl = new Label(com.acadscatchup.util.OSCompat.label("ℹ ") + "You are not currently enrolled in any subjects. Contact your instructor or administrator.");
            emptyLbl.getStyleClass().add("enrolled-empty-text");
            emptyBox.getChildren().add(emptyLbl);

            enrolledSubjectsBox.getChildren().add(emptyBox);
            return;
        }

        String currentFilter = subjectFilterCombo != null ? subjectFilterCombo.getValue() : "ALL";

        for (Subject s : subjects) {
            String code = s.getCode() != null ? s.getCode().trim().toUpperCase() : "";
            int pending = pendingMap.getOrDefault(code, 0);

            VBox card = new VBox(5);
            card.getStyleClass().add("subject-card-chip");

            // Top row: [📖 CODE] [Subject Name] (spacer) [Deficiency Badge]
            HBox topRow = new HBox(8);
            topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label codeLabel = new Label(com.acadscatchup.util.OSCompat.label("📖 ") + s.getCode());
            codeLabel.getStyleClass().add("subject-chip-code");

            Label nameLabel = new Label(s.getName() != null ? s.getName() : "");
            nameLabel.getStyleClass().add("subject-chip-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label badge = new Label(pending > 0 ? (com.acadscatchup.util.OSCompat.label("⚡ ") + pending + " Pending") : (com.acadscatchup.util.OSCompat.label("✔ ") + "Up to date"));
            badge.getStyleClass().add(pending > 0 ? "subject-chip-badge-pending" : "subject-chip-badge-clean");

            topRow.getChildren().addAll(codeLabel, nameLabel, spacer, badge);

            // Bottom row: [👨‍🏫 Professor Name] (spacer) [🎯 Filtered Badge]
            String profName = (s.getProfessorName() != null && !s.getProfessorName().isBlank())
                    ? s.getProfessorName()
                    : "No Assigned Professor";
            Label profLabel = new Label(com.acadscatchup.util.OSCompat.label("👨‍🏫 ") + profName);
            profLabel.getStyleClass().add("subject-chip-prof");

            Region bottomSpacer = new Region();
            HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

            Label filterIndicatorBadge = new Label("🎯 Selected in Filter");
            filterIndicatorBadge.getStyleClass().add("subject-chip-filter-badge");
            filterIndicatorBadge.setVisible(false);
            filterIndicatorBadge.setManaged(false);

            HBox bottomRow = new HBox(8);
            bottomRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            bottomRow.getChildren().addAll(profLabel, bottomSpacer, filterIndicatorBadge);

            card.getChildren().addAll(topRow, bottomRow);

            // View-only cursor
            card.setCursor(javafx.scene.Cursor.DEFAULT);

            // Informational Tooltip (View-only, no click-to-filter instruction)
            String baseTooltip = s.getCode() + " • " + s.getName() + "\nInstructor: " + profName +
                    "\nStatus: " + (pending > 0 ? (pending + " pending deficiencies") : "All requirements cleared");
            Tooltip tooltip = new Tooltip(baseTooltip);
            Tooltip.install(card, tooltip);

            card.getProperties().put("filterBadge", filterIndicatorBadge);
            card.getProperties().put("tooltip", tooltip);
            card.getProperties().put("baseTooltip", baseTooltip);

            // Active filter visual identifier if already filtered in ComboBox below
            if (s.getCode().equalsIgnoreCase(currentFilter)) {
                card.getStyleClass().add("subject-card-chip-active");
                filterIndicatorBadge.setVisible(true);
                filterIndicatorBadge.setManaged(true);
                tooltip.setText("🎯 Selected in filter below\n\n" + baseTooltip);
            }

            subjectCardMap.put(s.getCode().toUpperCase(), card);
            enrolledSubjectsBox.getChildren().add(card);
        }
    }

    private void updateSelectedSubjectCards(String selectedSubjectCode) {
        if (subjectCardMap.isEmpty()) return;
        for (Map.Entry<String, VBox> entry : subjectCardMap.entrySet()) {
            VBox card = entry.getValue();
            Label filterBadge = (Label) card.getProperties().get("filterBadge");
            Tooltip tooltip = (Tooltip) card.getProperties().get("tooltip");
            String baseTooltip = (String) card.getProperties().get("baseTooltip");

            if (entry.getKey().equalsIgnoreCase(selectedSubjectCode)) {
                if (!card.getStyleClass().contains("subject-card-chip-active")) {
                    card.getStyleClass().add("subject-card-chip-active");
                }
                if (filterBadge != null) {
                    filterBadge.setVisible(true);
                    filterBadge.setManaged(true);
                }
                if (tooltip != null && baseTooltip != null) {
                    tooltip.setText("🎯 Selected in filter below\n\n" + baseTooltip);
                }
            } else {
                card.getStyleClass().remove("subject-card-chip-active");
                if (filterBadge != null) {
                    filterBadge.setVisible(false);
                    filterBadge.setManaged(false);
                }
                if (tooltip != null && baseTooltip != null) {
                    tooltip.setText(baseTooltip);
                }
            }
        }
    }
}
