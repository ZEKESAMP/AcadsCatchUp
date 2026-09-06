package com.acadscatchup.controller;

import com.acadscatchup.dao.MissedItemDAO;
import com.acadscatchup.dao.SubjectDAO;
import com.acadscatchup.dao.UserDAO;
import com.acadscatchup.model.MissedItem;
import com.acadscatchup.model.Subject;
import com.acadscatchup.model.User;
import com.acadscatchup.util.CSVExporter;
import com.acadscatchup.util.Session;
import com.acadscatchup.util.LiveSyncService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Professor dashboard controller — full CRUD + filters + CSV export.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class ProfDashboardController {

    public static final String DEVELOPER = "F4TAL";

    // ── Top bar ─────────────────────────────────────────────────────────
    @FXML private HBox   topBar;
    @FXML private Label  appTitleLabel;
    @FXML private Label  roleBadge;
    @FXML private Label  profNameLabel;
    @FXML private Label  profSyncStatusLabel;
    @FXML private Button adminInboxBtn;
    @FXML private Button profInboxBtn;
    @FXML private Button helpBtn;
    @FXML private Button updatesBtn;
    @FXML private Button settingsBtn;
    @FXML private Button profLogoutBtn;

    // ── Center Scroll & Stats ─────────────────────────────────────────────
    @FXML private ScrollPane dashboardScrollPane;
    @FXML private HBox       statsRow;

    // ── Sidebar ──────────────────────────────────────────────────────────
    @FXML private Button     enrollStudentBtn;
    @FXML private Button     addStudentBtn;
    @FXML private ComboBox<String> studentScopeCombo;
    @FXML private TextField  studentSearchField;
    @FXML private ListView<User> studentListView;
    @FXML private ComboBox<String>  yearFilterCombo;
    @FXML private ComboBox<String>  statusFilterCombo;
    @FXML private ComboBox<String>  subjectFilterCombo;
    @FXML private ComboBox<String>  typeFilterCombo;

    // ── Stats cards ──────────────────────────────────────────────────────
    @FXML private VBox  statTotal;
    @FXML private VBox  statPending;
    @FXML private VBox  statSubmitted;
    @FXML private VBox  statGraded;
    @FXML private Label totalCount;
    @FXML private Label pendingCount;
    @FXML private Label submittedCount;
    @FXML private Label gradedCount;

    // ── Action bar ───────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Button    manageUsersBtn;
    @FXML private Separator manageUsersSep;
    @FXML private Button    addSubjectBtn;
    @FXML private Separator adminSep;

    // ── Table ────────────────────────────────────────────────────────────
    @FXML private TableView<MissedItem>          itemsTable;
    @FXML private TableColumn<MissedItem,String> colStudent;
    @FXML private TableColumn<MissedItem,String> colSubject;
    @FXML private TableColumn<MissedItem,String> colType;
    @FXML private TableColumn<MissedItem,String> colItemName;
    @FXML private TableColumn<MissedItem,String> colDateMissed;
    @FXML private TableColumn<MissedItem,String> colDeadline;
    @FXML private TableColumn<MissedItem,String> colStatus;
    @FXML private TableColumn<MissedItem,String> colNotes;

    private final MissedItemDAO missedItemDAO = new MissedItemDAO();
    private final UserDAO       userDAO       = new UserDAO();
    private final SubjectDAO    subjectDAO    = new SubjectDAO();
    private final com.acadscatchup.dao.InboxDAO inboxDAO = new com.acadscatchup.dao.InboxDAO();

    private ObservableList<MissedItem> tableData = FXCollections.observableArrayList();
    private List<User>    allStudents;
    private List<Subject> allSubjects;
    private List<Subject> myDedicatedSubjects = new ArrayList<>();
    private com.acadscatchup.util.LoadingOverlay loadingOverlay;
    private User lastClickedStudent = null;

    // ── Initialization ───────────────────────────────────────────────────
    @FXML
    private void initialize() {
        boolean isAdmin = Session.getCurrentUser() != null && Session.getCurrentUser().isAdmin();
        String profFullName = com.acadscatchup.util.OSCompat.label("👤 ") + (Session.getCurrentUser() != null ? Session.getCurrentUser().getFullName() : "") + 
                              (isAdmin ? " [ADMIN]" : "");
        profNameLabel.setText(profFullName);
        profNameLabel.setTooltip(new Tooltip(profFullName));

        // Modern auto-scaling responsiveness (compact mode on resize)
        com.acadscatchup.util.ResponsiveLayoutUtil.installProfResponsiveLayout(
                dashboardScrollPane,
                topBar,
                appTitleLabel,
                roleBadge,
                profSyncStatusLabel,
                adminInboxBtn,
                profInboxBtn,
                helpBtn,
                settingsBtn,
                profNameLabel,
                profLogoutBtn,
                statsRow
        );

        // Only ADMIN (F4TAL) can see and use Add Subject, Manage Users, and Bug Reports Inbox
        if (addSubjectBtn != null) {
            addSubjectBtn.setVisible(isAdmin);
            addSubjectBtn.setManaged(isAdmin);
        }
        if (adminSep != null) {
            adminSep.setVisible(isAdmin);
            adminSep.setManaged(isAdmin);
        }
        if (manageUsersBtn != null) {
            manageUsersBtn.setVisible(isAdmin);
            manageUsersBtn.setManaged(isAdmin);
        }
        if (manageUsersSep != null) {
            manageUsersSep.setVisible(isAdmin);
            manageUsersSep.setManaged(isAdmin);
        }
        if (addStudentBtn != null) {
            addStudentBtn.setVisible(isAdmin);
            addStudentBtn.setManaged(isAdmin);
        }
        if (adminInboxBtn != null) {
            adminInboxBtn.setVisible(isAdmin);
            adminInboxBtn.setManaged(isAdmin);
            if (isAdmin) {
                int openCount = new com.acadscatchup.dao.HelpReportDAO().getOpenCount();
                adminInboxBtn.setText(com.acadscatchup.util.OSCompat.label("📥 ") + "Bug Reports (" + openCount + ")");
            }
        }

        refreshProfInboxBadge();

        if (helpBtn != null) {
            // Professors see Help button to report to Admin (Admin doesn't need to report to himself)
            helpBtn.setVisible(!isAdmin);
            helpBtn.setManaged(!isAdmin);
        }

        if (Session.getCurrentUser() != null && Session.getCurrentUser().isProfessor()) {
            myDedicatedSubjects = subjectDAO.getSubjectsByProfessor(Session.getCurrentUser().getId());
        }

        setupStudentScope();
        setupFilters();
        setupStatCards();
        setupTable();
        loadStudentList();
        loadAllSubjectsIntoFilter();
        subjectFilterCombo.valueProperty().addListener((obs, o, n) -> loadItems());
        loadItems();

        // Filter items when a student is selected in the sidebar
        studentListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> loadItems());

        // Toggle selection off when clicking an already-selected student
        studentListView.setOnMouseClicked(event -> {
            User current = studentListView.getSelectionModel().getSelectedItem();
            if (current != null && current.equals(lastClickedStudent)) {
                studentListView.getSelectionModel().clearSelection();
                lastClickedStudent = null;
                loadItems();
            } else {
                lastClickedStudent = current;
            }
        });

        // Live search on Enter
        studentSearchField.textProperty().addListener((obs, o, n) -> filterStudentList(n));

        // Live real-time background sync engine (checks cloud fingerprint every 3.5s)
        User curr = Session.getCurrentUser();
        if (curr != null) {
            liveSyncService = LiveSyncService.forProfessor(curr.getId(), isAdmin, new LiveSyncService.SyncListener() {
                @Override
                public void onDataChanged() {
                    loadItemsAsync(false);
                    queryProfInboxInBackground();
                    if (isAdmin) {
                        refreshAdminInboxBadge();
                    }
                }

                @Override
                public void onStatusChanged(LiveSyncService.SyncStatus status) {
                    updateProfSyncBadge(status);
                }
            });
            liveSyncService.start();
        }

        com.acadscatchup.util.UpdateSplash.checkAndBadgeUpdatesButton(updatesBtn);

        // Register custom FAQ-style close confirmation dialog
        javafx.application.Platform.runLater(() -> {
            if (profNameLabel.getScene() != null && profNameLabel.getScene().getWindow() instanceof Stage s) {
                s.setOnCloseRequest(e -> {
                    e.consume();
                    com.acadscatchup.util.AppTrayManager.handleCloseRequest(s);
                });
            }
            // Cross-platform emoji patching for Linux
            if (profNameLabel.getScene() != null) {
                com.acadscatchup.util.OSCompat.patchEmojis(profNameLabel.getScene().getRoot());
            }
        });
    }

    private void setupStudentScope() {
        if (studentScopeCombo == null) return;
        boolean isAdmin = Session.getCurrentUser() != null && Session.getCurrentUser().isAdmin();

        ObservableList<String> scopes = FXCollections.observableArrayList();
        scopes.add("OLLC STUDENTS");

        List<Subject> targetSubjects = isAdmin ? subjectDAO.getAllSubjects() : myDedicatedSubjects;
        if (targetSubjects != null) {
            for (Subject s : targetSubjects) {
                String entry = s.getCode() + " - " + s.getName();
                if (!scopes.contains(entry)) {
                    scopes.add(entry);
                }
            }
        }

        studentScopeCombo.setItems(scopes);
        if (!isAdmin && !myDedicatedSubjects.isEmpty()) {
            Subject first = myDedicatedSubjects.get(0);
            studentScopeCombo.setValue(first.getCode() + " - " + first.getName());
        } else {
            studentScopeCombo.setValue("OLLC STUDENTS");
        }

        studentScopeCombo.valueProperty().addListener((obs, o, n) -> {
            loadStudentList();
            loadItems();
        });
    }

    private void setupFilters() {
        if (yearFilterCombo != null) {
            yearFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "1st Year", "2nd Year", "3rd Year", "4th Year"));
            User curr = Session.getCurrentUser();
            if (curr != null && curr.isProfessor() && curr.getYearLevel() > 0) {
                yearFilterCombo.setValue(curr.getYearDisplay());
            } else {
                yearFilterCombo.setValue("ALL");
            }
            yearFilterCombo.valueProperty().addListener((obs, o, n) -> {
                loadStudentList();
                loadItems();
            });
        }

        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "ALL", "PENDING", "SUBMITTED", "GRADED"));
        statusFilterCombo.setValue("ALL");
        statusFilterCombo.valueProperty().addListener((obs, o, n) -> loadItems());

        typeFilterCombo.setItems(FXCollections.observableArrayList(
                "ALL", "ACTIVITY", "QUIZ", "EXAM", "ASSIGNMENT"));
        typeFilterCombo.setValue("ALL");
        typeFilterCombo.valueProperty().addListener((obs, o, n) -> loadItems());
    }

    private void setupStatCards() {
        if (statTotal != null) {
            statTotal.setCursor(Cursor.HAND);
            Tooltip.install(statTotal, new Tooltip("Click to filter by ALL items"));
            statTotal.setOnMouseClicked(e -> {
                if (statusFilterCombo != null) statusFilterCombo.setValue("ALL");
            });
        }
        if (statPending != null) {
            statPending.setCursor(Cursor.HAND);
            Tooltip.install(statPending, new Tooltip("Click to filter by PENDING items"));
            statPending.setOnMouseClicked(e -> {
                if (statusFilterCombo != null) statusFilterCombo.setValue("PENDING");
            });
        }
        if (statSubmitted != null) {
            statSubmitted.setCursor(Cursor.HAND);
            Tooltip.install(statSubmitted, new Tooltip("Click to filter by SUBMITTED items"));
            statSubmitted.setOnMouseClicked(e -> {
                if (statusFilterCombo != null) statusFilterCombo.setValue("SUBMITTED");
            });
        }
        if (statGraded != null) {
            statGraded.setCursor(Cursor.HAND);
            Tooltip.install(statGraded, new Tooltip("Click to filter by GRADED items"));
            statGraded.setOnMouseClicked(e -> {
                if (statusFilterCombo != null) statusFilterCombo.setValue("GRADED");
            });
        }
    }

    private void setupTable() {
        colStudent   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentName()));
        colSubject   .setCellValueFactory(d -> {
            String code = d.getValue().getSubjectCode();
            String name = d.getValue().getSubjectName();
            if (name != null && !name.isBlank()) {
                return new SimpleStringProperty(code + " - " + name);
            }
            return new SimpleStringProperty(code != null ? code : "");
        });
        colType      .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemType()));
        colItemName  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        colDateMissed.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDateMissed() != null ? d.getValue().getDateMissed().toString() : ""));
        colDeadline  .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDeadline() != null ? d.getValue().getDeadline().toString() : ""));
        colNotes     .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNotes() != null ? d.getValue().getNotes() : ""));

        // Status column with colored badge label
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

        // Row styling by status
        itemsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(MissedItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-pending","row-submitted","row-graded","row-overdue");
                if (item != null && !empty) {
                    if (item.isOverdue())                    getStyleClass().add("row-overdue");
                    else if ("SUBMITTED".equals(item.getStatus())) getStyleClass().add("row-submitted");
                    else if ("GRADED".equals(item.getStatus()))    getStyleClass().add("row-graded");
                    else                                           getStyleClass().add("row-pending");
                }
            }
        });

        itemsTable.setItems(tableData);
    }

    private void loadStudentList() {
        String scopeVal = studentScopeCombo != null ? studentScopeCombo.getValue() : null;

        List<User> source;
        if (scopeVal != null && !scopeVal.equals("OLLC STUDENTS") && !scopeVal.isBlank()) {
            String code = scopeVal.contains(" - ") ? scopeVal.split(" - ")[0].trim() : scopeVal.trim();
            Optional<Subject> targetSub = subjectDAO.getAllSubjects().stream()
                    .filter(s -> s.getCode().equalsIgnoreCase(code))
                    .findFirst();

            if (targetSub.isPresent()) {
                source = subjectDAO.getStudentsBySubject(targetSub.get().getId());
            } else {
                source = userDAO.getAllStudents();
            }
        } else {
            source = userDAO.getAllStudents();
        }

        // Filter by Year Level if set
        String yearVal = (yearFilterCombo != null) ? yearFilterCombo.getValue() : "ALL";
        if (yearVal != null && !"ALL".equalsIgnoreCase(yearVal)) {
            allStudents = source.stream()
                    .filter(u -> yearVal.equalsIgnoreCase(u.getYearDisplay()))
                    .toList();
        } else {
            allStudents = source;
        }

        // Student list displays only actual registered students; top combo controls filter scope
        lastClickedStudent = null;
        studentListView.setItems(FXCollections.observableArrayList(allStudents));
        studentListView.getSelectionModel().clearSelection();
    }

    private void loadAllSubjectsIntoFilter() {
        allSubjects = subjectDAO.getAllSubjects();
        boolean isAdmin = Session.getCurrentUser() != null && Session.getCurrentUser().isAdmin();

        ObservableList<String> codes = FXCollections.observableArrayList("ALL");
        if (!isAdmin && !myDedicatedSubjects.isEmpty()) {
            myDedicatedSubjects.forEach(s -> codes.add(s.getCode()));
            subjectFilterCombo.setItems(codes);
            if (myDedicatedSubjects.size() == 1) {
                subjectFilterCombo.setValue(myDedicatedSubjects.get(0).getCode());
            } else {
                subjectFilterCombo.setValue("ALL");
            }
        } else {
            allSubjects.forEach(s -> codes.add(s.getCode()));
            subjectFilterCombo.setItems(codes);
            subjectFilterCombo.setValue("ALL");
        }
    }

    private void filterStudentList(String query) {
        if (query == null || query.isBlank()) {
            loadStudentList();
            return;
        }
        String q = query.toLowerCase().trim();
        ObservableList<User> filtered = FXCollections.observableArrayList();
        allStudents.stream()
                .filter(u -> u.getFullName().toLowerCase().contains(q)
                          || u.getUsername().toLowerCase().contains(q)
                          || (u.getProgram() != null && u.getProgram().toLowerCase().contains(q)))
                .forEach(filtered::add);
        lastClickedStudent = null;
        studentListView.setItems(filtered);
        studentListView.getSelectionModel().clearSelection();
    }

    // ── Data Loading ─────────────────────────────────────────────────────
    private void loadItems() {
        loadItemsAsync(false);
    }

    private void loadItemsAsync(boolean showOverlay) {
        String status  = statusFilterCombo.getValue();
        String subject = subjectFilterCombo.getValue();
        String type    = typeFilterCombo.getValue();
        String search  = searchField != null ? searchField.getText() : null;

        User selected = studentListView.getSelectionModel().getSelectedItem();
        final int selectedId = (selected != null) ? selected.getId() : 0;

        boolean isAdmin = Session.getCurrentUser() != null && Session.getCurrentUser().isAdmin();
        List<Subject> capturedDedicatedSubjects = new ArrayList<>(myDedicatedSubjects);
        String yearVal = (yearFilterCombo != null) ? yearFilterCombo.getValue() : "ALL";
        List<User> capturedStudents = allStudents != null ? new ArrayList<>(allStudents) : new ArrayList<>();

        // Create overlay if first time
        if (showOverlay && loadingOverlay == null && profNameLabel.getScene() != null) {
            javafx.scene.Parent sceneRoot = profNameLabel.getScene().getRoot();
            if (sceneRoot instanceof javafx.scene.layout.Pane p) {
                loadingOverlay = new com.acadscatchup.util.LoadingOverlay(p);
            }
        }

        if (showOverlay && loadingOverlay != null) {
            loadingOverlay.show("Loading data...");
        }

        new Thread(() -> {
            try {
                List<MissedItem> items;
                if (selectedId == 0) {
                    items = missedItemDAO.getAll(status, subject, type, search);
                } else {
                    items = missedItemDAO.getByStudent(selectedId, status, subject);
                }

                // Scope to professor's assigned subjects if not admin
                if (!isAdmin && !capturedDedicatedSubjects.isEmpty()) {
                    Set<String> myCodes = capturedDedicatedSubjects.stream().map(Subject::getCode).collect(java.util.stream.Collectors.toSet());
                    items = items.stream().filter(i -> myCodes.contains(i.getSubjectCode())).toList();
                }

                // Scope to student scope subject if set and subject filter is ALL
                String scopeVal = studentScopeCombo != null ? studentScopeCombo.getValue() : null;
                if (scopeVal != null && scopeVal.contains(" - ") && (subject == null || "ALL".equalsIgnoreCase(subject))) {
                    String scopeCode = scopeVal.split(" - ")[0].trim();
                    items = items.stream().filter(i -> scopeCode.equalsIgnoreCase(i.getSubjectCode())).toList();
                }

                // Filter by Year Level if specified
                if (yearVal != null && !"ALL".equalsIgnoreCase(yearVal)) {
                    Set<Integer> studentIdsInYear = capturedStudents.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
                    items = items.stream().filter(i -> studentIdsInYear.contains(i.getStudentId())).toList();
                }

                final List<MissedItem> finalItems = items;
                javafx.application.Platform.runLater(() -> {
                    tableData.setAll(finalItems);
                    updateStats(finalItems);
                    if (loadingOverlay != null) loadingOverlay.hide();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (loadingOverlay != null) loadingOverlay.hide();
                });
                System.err.println("[LoadItems] " + e.getMessage());
            }
        }, "LoadItems-Worker").start();
    }

    private void updateStats(List<MissedItem> items) {
        long pending   = items.stream().filter(i -> "PENDING".equals(i.getStatus())).count();
        long submitted = items.stream().filter(i -> "SUBMITTED".equals(i.getStatus())).count();
        long graded    = items.stream().filter(i -> "GRADED".equals(i.getStatus())).count();

        totalCount    .setText(String.valueOf(items.size()));
        pendingCount  .setText(String.valueOf(pending));
        submittedCount.setText(String.valueOf(submitted));
        gradedCount   .setText(String.valueOf(graded));
    }

    // ── Action Handlers ──────────────────────────────────────────────────
    @FXML private void applyFilters()  { loadItemsAsync(true); }

    @FXML private void clearFilters() {
        if (yearFilterCombo != null) {
            User curr = Session.getCurrentUser();
            if (curr != null && curr.isProfessor() && curr.getYearLevel() > 0) {
                yearFilterCombo.setValue(curr.getYearDisplay());
            } else {
                yearFilterCombo.setValue("ALL");
            }
        }
        statusFilterCombo.setValue("ALL");
        if (myDedicatedSubjects.size() == 1) {
            subjectFilterCombo.setValue(myDedicatedSubjects.get(0).getCode());
        } else {
            subjectFilterCombo.setValue("ALL");
        }
        typeFilterCombo.setValue("ALL");
        searchField.clear();
        loadStudentList();
        studentListView.getSelectionModel().select(0);
        loadItemsAsync(true);
    }

    @FXML
    private void handleOpenEnrollDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/acadscatchup/fxml/enroll_student_dialog.fxml"));
            Parent root = loader.load();

            EnrollStudentController ctrl = loader.getController();
            boolean isAdmin = Session.getCurrentUser() != null && Session.getCurrentUser().isAdmin();
            if (!isAdmin && !myDedicatedSubjects.isEmpty()) {
                ctrl.setSubjects(myDedicatedSubjects, myDedicatedSubjects.get(0));
            } else {
                ctrl.setSubjects(allSubjects, (!myDedicatedSubjects.isEmpty() ? myDedicatedSubjects.get(0) : (allSubjects.isEmpty() ? null : allSubjects.get(0))));
            }

            com.acadscatchup.util.ModalOverlay.showAndWait(itemsTable, root, 920, 600);

            if (ctrl.isEnrolledSuccessfully()) {
                loadStudentList();
                loadItems();
                if (liveSyncService != null) {
                    liveSyncService.triggerImmediateSync();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddItem() {
        openAddEditDialog(null);
    }

    @FXML
    private void handleEditItem() {
        MissedItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an item to edit.");
            return;
        }
        openAddEditDialog(selected);
    }

    @FXML
    private void handleDeleteItem() {
        MissedItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an item to delete.");
            return;
        }

        boolean confirmed = com.acadscatchup.util.CustomAlert.showConfirmation(
                profNameLabel.getScene().getWindow(),
                "Confirm Deletion",
                "Delete \"" + selected.getItemName() + "\" for " + selected.getStudentName() + "?");

        if (confirmed) {
            javafx.scene.Parent sceneRoot = profNameLabel.getScene().getRoot();
            if (sceneRoot instanceof javafx.scene.layout.Pane p) {
                com.acadscatchup.util.LoadingOverlay.runWithOverlay(p, "Deleting...", () -> {
                    if (missedItemDAO.delete(selected.getId())) {
                        javafx.application.Platform.runLater(() -> loadItemsAsync(false));
                        if (liveSyncService != null) {
                            liveSyncService.triggerImmediateSync();
                        }
                    } else {
                        javafx.application.Platform.runLater(() ->
                                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete the item."));
                    }
                }, null);
            }
        }
    }

    @FXML
    private void handleMarkSubmitted() {
        MissedItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "No Selection", "Select an item first."); return; }
        javafx.scene.Parent sceneRoot = profNameLabel.getScene().getRoot();
        if (sceneRoot instanceof javafx.scene.layout.Pane p) {
            com.acadscatchup.util.LoadingOverlay.runWithOverlay(p, "Updating status...", () -> {
                if (missedItemDAO.updateStatus(selected.getId(), "SUBMITTED")) {
                    javafx.application.Platform.runLater(() -> loadItemsAsync(false));
                    if (liveSyncService != null) {
                        liveSyncService.triggerImmediateSync();
                    }
                }
            }, null);
        }
    }

    @FXML
    private void handleMarkGraded() {
        MissedItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "No Selection", "Select an item first."); return; }
        javafx.scene.Parent sceneRoot = profNameLabel.getScene().getRoot();
        if (sceneRoot instanceof javafx.scene.layout.Pane p) {
            com.acadscatchup.util.LoadingOverlay.runWithOverlay(p, "Grading item...", () -> {
                if (missedItemDAO.updateStatus(selected.getId(), "GRADED")) {
                    User prof = Session.getCurrentUser();
                    int profId = (prof != null) ? prof.getId() : 0;
                    String profName = (prof != null) ? prof.getFullName() : "Your Professor";
                    inboxDAO.sendGradedNotice(selected.getStudentId(), selected.getStudentName(), profId, profName, selected);
                    javafx.application.Platform.runLater(() -> {
                        loadItemsAsync(false);
                        if (liveSyncService != null) {
                            liveSyncService.triggerImmediateSync();
                        }
                        showAlert(Alert.AlertType.INFORMATION, "Graded Successfully",
                                "\"" + selected.getItemName() + "\" marked as GRADED and the student was notified in their Inbox!");
                    });
                }
            }, null);
        }
    }

    @FXML
    private void handleExportCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save CSV Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("acadscatchup_report.csv");
        File file = fc.showSaveDialog(itemsTable.getScene().getWindow());
        if (file != null) {
            boolean ok = CSVExporter.export(tableData, file.getAbsolutePath());
            showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                    ok ? "Export Successful" : "Export Failed",
                    ok ? "Report saved to: " + file.getAbsolutePath() : "Could not save CSV file.");
        }
    }

    @FXML
    private void handleManageUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/acadscatchup/fxml/manage_users.fxml"));
            javafx.scene.Parent root = loader.load();
            com.acadscatchup.util.ModalOverlay.showAndWait(profNameLabel, root, 960, 600);
            // Refresh student list after managing users
            loadStudentList();
            if (liveSyncService != null) {
                liveSyncService.triggerImmediateSync();
            }
        } catch (java.io.IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Manage Users: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddSubject() {
        if (Session.getCurrentUser() == null || !Session.getCurrentUser().isAdmin()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only administrators can add subjects.");
            return;
        }


        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/add_subject_dialog.fxml"));
            Parent root = loader.load();
            AddSubjectController ctrl = loader.getController();

            com.acadscatchup.util.ModalOverlay.showAndWait(profNameLabel, root, 480, 290);

            if (ctrl.isSubjectAdded()) {
                loadAllSubjectsIntoFilter();
                if (liveSyncService != null) {
                    liveSyncService.triggerImmediateSync();
                }
            }
        } catch (java.io.IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Add Subject dialog: " + e.getMessage());
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
            Stage stage = (Stage) profNameLabel.getScene().getWindow();
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

    // ── Dialog ───────────────────────────────────────────────────────────
    private void openAddEditDialog(MissedItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/acadscatchup/fxml/add_edit_item.fxml"));
            Parent root = loader.load();

            AddEditItemController ctrl = loader.getController();
            ctrl.setItem(item);

            boolean isAdmin = Session.getCurrentUser() != null && Session.getCurrentUser().isAdmin();
            List<Subject> availableSubjects = (!isAdmin && !myDedicatedSubjects.isEmpty()) ? myDedicatedSubjects : allSubjects;
            ctrl.setSubjects(availableSubjects);

            // Determine active subject to pre-select based on sidebar selection or filter
            String activeCode = null;
            String scopeVal = studentScopeCombo != null ? studentScopeCombo.getValue() : null;
            if (scopeVal != null && scopeVal.contains(" - ")) {
                activeCode = scopeVal.split(" - ")[0].trim();
            } else if (subjectFilterCombo != null && !"ALL".equalsIgnoreCase(subjectFilterCombo.getValue())) {
                activeCode = subjectFilterCombo.getValue();
            }

            if (item != null) {
                ctrl.preselectByIds(item.getStudentId(), item.getSubjectId());
            } else {
                if (activeCode != null) {
                    String finalCode = activeCode;
                    availableSubjects.stream()
                            .filter(s -> s.getCode().equalsIgnoreCase(finalCode))
                            .findFirst()
                            .ifPresent(ctrl::preselectSubject);
                }
                User selectedStudent = studentListView.getSelectionModel().getSelectedItem();
                if (selectedStudent != null && selectedStudent.getId() > 0) {
                    ctrl.preselectStudent(selectedStudent.getId());
                }
            }

            com.acadscatchup.util.ModalOverlay.showAndWait(itemsTable, root, 580, 650);

            loadItems(); // Refresh after dialog closes
            if (liveSyncService != null) {
                liveSyncService.triggerImmediateSync();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        javafx.stage.Window owner = (profNameLabel.getScene() != null) ? profNameLabel.getScene().getWindow() : null;
        if (type == Alert.AlertType.ERROR) {
            com.acadscatchup.util.CustomAlert.showError(owner, title, msg);
        } else if (type == Alert.AlertType.WARNING) {
            com.acadscatchup.util.CustomAlert.showWarning(owner, title, msg);
        } else {
            com.acadscatchup.util.CustomAlert.showInfo(owner, title, msg);
        }
    }

    @FXML
    private void handleOpenAdminInbox() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/admin_inbox.fxml"));
            Parent root = loader.load();
            com.acadscatchup.util.ModalOverlay.showAndWait(profNameLabel, root, 860, 640);

            // Refresh open reports badge count
            if (adminInboxBtn != null) {
                int openCount = new com.acadscatchup.dao.HelpReportDAO().getOpenCount();
                adminInboxBtn.setText(com.acadscatchup.util.OSCompat.label("📥 ") + "Bug Reports (" + openCount + ")");
            }
            if (liveSyncService != null) {
                liveSyncService.triggerImmediateSync();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Inbox: " + e.getMessage());
        }
    }

    private boolean notifiedProfSession = false;
    private int lastSeenProfUnread = -1;
    private LiveSyncService liveSyncService = null;

    private void updateProfSyncBadge(LiveSyncService.SyncStatus status) {
        if (profSyncStatusLabel == null) return;
        profSyncStatusLabel.setText(status.label);
        profSyncStatusLabel.setStyle(
                "-fx-text-fill: " + status.textColor + "; " +
                "-fx-background-color: " + status.bgColor + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 3 8;"
        );
    }

    private void refreshAdminInboxBadge() {
        new Thread(() -> {
            try {
                int openCount = new com.acadscatchup.dao.HelpReportDAO().getOpenCount();
                javafx.application.Platform.runLater(() -> {
                    if (adminInboxBtn != null) {
                        adminInboxBtn.setText(com.acadscatchup.util.OSCompat.label("📥 ") + "Bug Reports (" + openCount + ")");
                    }
                });
            } catch (Exception ignored) {}
        }, "Admin-Inbox-Refresh").start();
    }

    private void refreshProfInboxBadge() {
        new Thread(this::queryProfInboxInBackground, "Prof-Inbox-Refresh").start();
    }

    private void queryProfInboxInBackground() {
        try {
            User curr = Session.getCurrentUser();
            if (curr == null || profInboxBtn == null) return;

            // 1. Fast lightweight COUNT query to minimize cloud RU consumption
            int unread = inboxDAO.getUnreadCount(curr.getId());

            // 2. Only fetch full message objects if an alert needs to be shown
            com.acadscatchup.model.InboxMessage alertMessage = null;
            if (unread > 0 && (!notifiedProfSession || (lastSeenProfUnread != -1 && unread > lastSeenProfUnread))) {
                List<com.acadscatchup.model.InboxMessage> msgs = inboxDAO.getMessagesForRecipient(curr.getId());
                for (com.acadscatchup.model.InboxMessage m : msgs) {
                    if (!m.isRead()) {
                        alertMessage = m;
                        break;
                    }
                }
            }

            final int finalUnread = unread;
            final com.acadscatchup.model.InboxMessage finalAlert = alertMessage;
            javafx.application.Platform.runLater(() -> processProfInboxUpdate(curr, finalUnread, finalAlert));
        } catch (Exception e) {
            System.err.println("[ProfInbox] " + e.getMessage());
        }
    }

    private void processProfInboxUpdate(User curr, int unread, com.acadscatchup.model.InboxMessage alertMessage) {
        if (profInboxBtn == null) return;
        profInboxBtn.setText(com.acadscatchup.util.OSCompat.label("📬 ") + "Submissions (" + unread + ")");
        if (unread > 0) {
            profInboxBtn.setStyle("-fx-background-color: rgba(59,130,246,0.3); -fx-text-fill: #93c5fd; -fx-font-weight: bold;");
            boolean shouldNotify = (!notifiedProfSession && alertMessage != null) ||
                    (lastSeenProfUnread != -1 && unread > lastSeenProfUnread && alertMessage != null);

            if (shouldNotify && alertMessage != null) {
                notifiedProfSession = true;
                boolean isUpdate = "UPDATE".equalsIgnoreCase(alertMessage.getMsgType())
                        || "SYSTEM_UPDATE".equalsIgnoreCase(alertMessage.getMsgType())
                        || (alertMessage.getTitle() != null && (alertMessage.getTitle().toLowerCase().contains("update") || alertMessage.getTitle().toLowerCase().contains("what's new")));
                boolean isResolved = "REPORT_RESOLVED".equalsIgnoreCase(alertMessage.getMsgType())
                        || (alertMessage.getTitle() != null && alertMessage.getTitle().toLowerCase().contains("resolved"));

                String toastTitle = isUpdate
                        ? "AcadsCatchUp • What's New Update 🚀"
                        : (isResolved ? "AcadsCatchUp • Bug Report Resolved ✔" : "AcadsCatchUp! Student Submissions");
                String toastBody = isUpdate
                        ? ("Hi " + curr.getFullName() + "! " + alertMessage.getTitle() + " has arrived in your Inbox. Check to view release notes!")
                        : (isResolved
                        ? ("Hi " + curr.getFullName() + "! Your reported issue has been addressed and marked as RESOLVED by the System Administrator.")
                        : ("You have " + unread + " student deficiency submission(s) awaiting your review."));

                com.acadscatchup.util.WindowsNotificationUtil.showNotification(
                        toastTitle,
                        toastBody,
                        java.awt.TrayIcon.MessageType.INFO
                );
            }
        } else {
            profInboxBtn.setStyle("");
        }
        lastSeenProfUnread = unread;
    }

    @FXML
    private void handleOpenProfInbox() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/user_inbox.fxml"));
            Parent root = loader.load();
            com.acadscatchup.util.ModalOverlay.showAndWait(profNameLabel, root, 840, 600);

            refreshProfInboxBadge();
            loadItems();
            if (liveSyncService != null) {
                liveSyncService.triggerImmediateSync();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Submissions Inbox: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenHelpReport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/help_report_dialog.fxml"));
            Parent root = loader.load();
            com.acadscatchup.util.ModalOverlay.showAndWait(profNameLabel, root, 560, 520);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Help Report: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenUpdates() {
        com.acadscatchup.util.UpdatesDialog.show(profNameLabel.getScene().getWindow());
    }

    @FXML
    private void handleOpenAccountSettings() {
        com.acadscatchup.util.AccountSettingsDialog.show(profNameLabel.getScene().getWindow());
    }
}
