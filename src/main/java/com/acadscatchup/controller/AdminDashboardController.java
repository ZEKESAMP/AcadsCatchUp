package com.acadscatchup.controller;

import com.acadscatchup.dao.HelpReportDAO;
import com.acadscatchup.dao.SubjectDAO;
import com.acadscatchup.dao.UserDAO;
import com.acadscatchup.model.Subject;
import com.acadscatchup.model.User;
import com.acadscatchup.util.CustomAlert;
import com.acadscatchup.util.EmailService;
import com.acadscatchup.util.LiveSyncService;
import com.acadscatchup.util.ModalOverlay;
import com.acadscatchup.util.PasswordToggleHelper;
import com.acadscatchup.util.Session;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Dedicated Controller for Administrator Dashboard.
 * Provides comprehensive control over Students, Professors, Subjects,
 * Enrollments, Bug Reports, and System Configurations.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AdminDashboardController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private HBox topBar;
    @FXML private Label appTitleLabel;
    @FXML private Label roleBadge;
    @FXML private Label syncStatusLabel;
    @FXML private Button adminInboxBtn;
    @FXML private Button smtpConfigBtn;
    @FXML private Button settingsBtn;
    @FXML private Label adminNameLabel;
    @FXML private Button adminLogoutBtn;

    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> programFilterCombo;
    @FXML private ComboBox<String> yearFilterCombo;

    @FXML private ScrollPane dashboardScrollPane;
    @FXML private HBox statsRow;
    @FXML private Label totalUsersCount;
    @FXML private Label studentsCount;
    @FXML private Label professorsCount;
    @FXML private Label subjectsCount;
    @FXML private Label openReportsCount;

    @FXML private TextField searchField;
    @FXML private Label selectionCountLabel;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Boolean> colSelect;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, User> colEmail;
    @FXML private TableColumn<User, String> colProgram;
    @FXML private TableColumn<User, String> colYear;
    @FXML private TableColumn<User, Void> colActions;

    private final UserDAO userDAO = new UserDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final HelpReportDAO reportDAO = new HelpReportDAO();

    private final ObservableList<User> masterUsersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    private final Set<Integer> selectedUserIds = new HashSet<>();
    private final CheckBox headerSelectAll = new CheckBox();

    private final Map<Integer, String> profSubjectsMap = new HashMap<>();
    private LiveSyncService liveSyncService = null;

    private static final List<String> PROGRAMS = List.of(
            "ALL", "BSIT", "BSCS", "BSIS", "BSECE", "BSCpE",
            "BSEE", "BSMath", "BSEntrep", "BSED", "BSBA", "Other");

    private static final List<String> YEAR_LEVELS = List.of(
            "ALL", "1st Year", "2nd Year", "3rd Year", "4th Year");

    @FXML
    public void initialize() {
        User curr = Session.getCurrentUser();
        String adminName = curr != null ? curr.getFullName() : "Administrator";
        adminNameLabel.setText(com.acadscatchup.util.OSCompat.label("👤 ") + adminName);
        adminNameLabel.setTooltip(new Tooltip(adminName + " (Master Administrator)"));

        setupFilters();
        setupTable();
        loadData();

        // Responsive auto-scaling layout manager
        com.acadscatchup.util.ResponsiveLayoutUtil.installAdminResponsiveLayout(
                dashboardScrollPane,
                topBar,
                appTitleLabel,
                roleBadge,
                syncStatusLabel,
                adminInboxBtn,
                smtpConfigBtn,
                settingsBtn,
                adminNameLabel,
                adminLogoutBtn,
                statsRow
        );

        // Initialize real-time LiveSync engine
        if (curr != null) {
            liveSyncService = LiveSyncService.forAdmin(curr.getId(), new LiveSyncService.SyncListener() {
                @Override
                public void onDataChanged() {
                    Platform.runLater(() -> loadDataSilently());
                }

                @Override
                public void onStatusChanged(LiveSyncService.SyncStatus status) {
                    Platform.runLater(() -> updateSyncBadge(status));
                }
            });
            liveSyncService.start();
        }

        // Register window close request
        Platform.runLater(() -> {
            if (adminNameLabel.getScene() != null && adminNameLabel.getScene().getWindow() instanceof Stage s) {
                s.setOnCloseRequest(e -> {
                    e.consume();
                    com.acadscatchup.util.AppTrayManager.handleCloseRequest(s);
                });
            }
        });
    }

    private void updateSyncBadge(LiveSyncService.SyncStatus status) {
        if (syncStatusLabel == null) return;
        syncStatusLabel.setText(status.label);
        syncStatusLabel.setStyle(
                "-fx-text-fill: " + status.textColor + "; " +
                "-fx-background-color: " + status.bgColor + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 3 8;"
        );
    }

    private void setupFilters() {
        roleFilterCombo.setItems(FXCollections.observableArrayList("ALL", "STUDENT", "PROFESSOR", "ADMIN"));
        roleFilterCombo.setValue("ALL");
        roleFilterCombo.setOnAction(e -> applyFilters());

        programFilterCombo.setItems(FXCollections.observableArrayList(PROGRAMS));
        programFilterCombo.setValue("ALL");
        programFilterCombo.setOnAction(e -> applyFilters());

        yearFilterCombo.setItems(FXCollections.observableArrayList(YEAR_LEVELS));
        yearFilterCombo.setValue("ALL");
        yearFilterCombo.setOnAction(e -> applyFilters());
    }

    private void setupTable() {
        filteredUsers = new FilteredList<>(masterUsersList, p -> true);
        usersTable.setItems(filteredUsers);

        // Header Select All checkbox
        colSelect.setGraphic(headerSelectAll);
        headerSelectAll.setOnAction(e -> {
            if (headerSelectAll.isSelected()) {
                for (User u : filteredUsers) {
                    selectedUserIds.add(u.getId());
                }
            } else {
                selectedUserIds.clear();
            }
            usersTable.refresh();
            updateSelectionState();
        });

        colSelect.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        User u = getTableView().getItems().get(getIndex());
                        if (cb.isSelected()) {
                            selectedUserIds.add(u.getId());
                        } else {
                            selectedUserIds.remove(u.getId());
                        }
                        updateSelectionState();
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    cb.setSelected(selectedUserIds.contains(u.getId()));
                    setGraphic(cb);
                }
            }
        });

        // Role column with color-coded badge
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(role);
                    switch (role.toUpperCase()) {
                        case "ADMIN" -> badge.getStyleClass().add("role-badge-admin");
                        case "PROFESSOR" -> badge.getStyleClass().add("role-badge-prof");
                        default -> badge.getStyleClass().add("role-badge-student");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colFullName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));

        // Email with verified indicator
        colEmail.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEmail.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String email = user.getEmail();
                    if (email == null || email.isBlank()) {
                        Label unlinked = new Label("No Gmail Linked");
                        unlinked.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic; -fx-font-size: 11px;");
                        setGraphic(unlinked);
                    } else {
                        HBox box = new HBox(6);
                        box.setAlignment(Pos.CENTER_LEFT);
                        Label emailLbl = new Label(email);
                        emailLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");

                        Label verifyBadge = new Label(user.isVerified() ? "✔ Verified" : "⚠️ Unverified");
                        verifyBadge.getStyleClass().add(user.isVerified() ? "verified-badge" : "unverified-badge");
                        box.getChildren().addAll(emailLbl, verifyBadge);
                        setGraphic(box);
                    }
                    setText(null);
                }
            }
        });

        // Program / Teaching Subjects
        colProgram.setCellValueFactory(d -> {
            User u = d.getValue();
            if (u.isProfessor()) {
                String subs = profSubjectsMap.getOrDefault(u.getId(), "No Subjects Assigned");
                return new SimpleStringProperty(subs);
            } else if (u.isStudent()) {
                return new SimpleStringProperty(u.getProgram() != null ? u.getProgram() : "-");
            } else {
                return new SimpleStringProperty("Full Access");
            }
        });

        colYear.setCellValueFactory(d -> {
            User u = d.getValue();
            if (u.isStudent()) {
                return new SimpleStringProperty(u.getYearLevel() > 0 ? u.getYearDisplay() : "-");
            } else if (u.isProfessor()) {
                return new SimpleStringProperty(u.getYearLevel() > 0 ? u.getYearDisplay() : "All Years");
            }
            return new SimpleStringProperty("-");
        });

        // Actions: Edit and Delete buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDel = new Button("🗑");
            private final HBox pane = new HBox(6, btnEdit, btnDel);
            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-background-color: rgba(99,102,241,0.2); -fx-text-fill: #a5b4fc; -fx-font-size: 11px; -fx-padding: 3 7; -fx-cursor: hand; -fx-background-radius: 4;");
                btnDel.setStyle("-fx-background-color: rgba(239,68,68,0.2); -fx-text-fill: #f87171; -fx-font-size: 11px; -fx-padding: 3 7; -fx-cursor: hand; -fx-background-radius: 4;");

                btnEdit.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    openUserDialog(u);
                });
                btnDel.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    confirmDeleteUser(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    // Protect master administrator and self
                    boolean isSelfOrMaster = "F4TAL".equalsIgnoreCase(u.getUsername()) ||
                            (Session.getCurrentUser() != null && Session.getCurrentUser().getId() == u.getId());
                    btnDel.setDisable(isSelfOrMaster);
                    setGraphic(pane);
                }
            }
        });
    }

    private void updateSelectionState() {
        int count = selectedUserIds.size();
        if (selectionCountLabel != null) {
            selectionCountLabel.setText(count > 0 ? count + " account(s) selected" : "");
        }
        if (headerSelectAll != null && filteredUsers != null) {
            headerSelectAll.setSelected(!filteredUsers.isEmpty() && selectedUserIds.containsAll(
                    filteredUsers.stream().map(User::getId).toList()
            ));
        }
    }

    @FXML
    private void handleSearchKey() {
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String role = roleFilterCombo != null ? roleFilterCombo.getValue() : "ALL";
        String program = programFilterCombo != null ? programFilterCombo.getValue() : "ALL";
        String year = yearFilterCombo != null ? yearFilterCombo.getValue() : "ALL";

        filteredUsers.setPredicate(u -> {
            if (u == null) return false;

            // Role filter
            if (role != null && !"ALL".equalsIgnoreCase(role)) {
                if (!role.equalsIgnoreCase(u.getRole())) return false;
            }

            // Program filter
            if (program != null && !"ALL".equalsIgnoreCase(program)) {
                if (u.isStudent() && (u.getProgram() == null || !u.getProgram().equalsIgnoreCase(program))) {
                    return false;
                }
            }

            // Year filter
            if (year != null && !"ALL".equalsIgnoreCase(year)) {
                if (u.isStudent() && (u.getYearDisplay() == null || !u.getYearDisplay().equalsIgnoreCase(year))) {
                    return false;
                }
            }

            // Text search
            if (!query.isEmpty()) {
                boolean matchUser = u.getUsername() != null && u.getUsername().toLowerCase().contains(query);
                boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(query);
                boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(query);
                boolean matchProg = u.getProgram() != null && u.getProgram().toLowerCase().contains(query);
                String profSubs = profSubjectsMap.getOrDefault(u.getId(), "").toLowerCase();
                return matchUser || matchName || matchEmail || matchProg || profSubs.contains(query);
            }

            return true;
        });

        updateSelectionState();
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) searchField.clear();
        if (roleFilterCombo != null) roleFilterCombo.setValue("ALL");
        if (programFilterCombo != null) programFilterCombo.setValue("ALL");
        if (yearFilterCombo != null) yearFilterCombo.setValue("ALL");
        applyFilters();
    }

    private void loadData() {
        new Thread(() -> {
            List<User> users = userDAO.getAllUsers();
            List<Subject> subjects = subjectDAO.getAllSubjects();
            int openReports = reportDAO.getOpenCount();

            // Cache professor subjects
            Map<Integer, String> newProfMap = new HashMap<>();
            for (User u : users) {
                if (u.isProfessor()) {
                    List<Subject> assigned = subjectDAO.getSubjectsByProfessor(u.getId());
                    if (!assigned.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < assigned.size(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(assigned.get(i).getCode());
                        }
                        newProfMap.put(u.getId(), sb.toString());
                    } else {
                        newProfMap.put(u.getId(), "None");
                    }
                }
            }

            // Count roles
            int total = users.size();
            int students = 0;
            int professors = 0;
            for (User u : users) {
                if (u.isStudent()) students++;
                else if (u.isProfessor()) professors++;
            }

            final int finalTotal = total;
            final int finalStudents = students;
            final int finalProfessors = professors;
            final int finalSubjects = subjects.size();
            final int finalReports = openReports;

            Platform.runLater(() -> {
                profSubjectsMap.clear();
                profSubjectsMap.putAll(newProfMap);
                masterUsersList.setAll(users);
                applyFilters();

                totalUsersCount.setText(String.valueOf(finalTotal));
                studentsCount.setText(String.valueOf(finalStudents));
                professorsCount.setText(String.valueOf(finalProfessors));
                subjectsCount.setText(String.valueOf(finalSubjects));
                openReportsCount.setText(String.valueOf(finalReports));

                if (adminInboxBtn != null) {
                    adminInboxBtn.setText(com.acadscatchup.util.OSCompat.label("📥 ") + "Bug Reports (" + finalReports + ")");
                }
            });
        }, "AdminDashboard-DataLoader").start();
    }

    private void loadDataSilently() {
        loadData();
    }

    // ── Add / Edit User (Strictly NO Gmail Editing) ──────────────────────────

    @FXML
    private void handleAddUser() {
        openUserDialog(null);
    }

    @FXML
    private void handleEditUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            CustomAlert.showWarning(usersTable.getScene().getWindow(), "No Selection", "Please select a user account to edit.");
            return;
        }
        openUserDialog(selected);
    }

    private void openUserDialog(User existing) {
        boolean isEdit = existing != null;

        VBox root = new VBox(0);
        root.setStyle("-fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-background-color: #0f1117;");
        try {
            root.getStylesheets().add(getClass().getResource("/com/acadscatchup/css/style.css").toExternalForm());
        } catch (Exception ignored) {}

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 14 20; -fx-background-color: #121520; -fx-border-color: #2d3255; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label(isEdit ? "Edit Account — " + existing.getUsername() : "Create New Account");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subLabel = new Label(isEdit ? "Update account credentials and subject assignments" : "Add student or professor to the system");
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        VBox titleBox = new VBox(2, titleLabel, subLabel);
        header.getChildren().addAll(titleBox);

        // Fields
        TextField tfUsername = new TextField(isEdit ? existing.getUsername() : "");
        TextField tfFullName = new TextField(isEdit ? existing.getFullName() : "");
        PasswordToggleHelper.PasswordBox boxPassword =
                PasswordToggleHelper.createPasswordBox(isEdit ? "Leave blank to keep current" : "Set initial password", null);

        ComboBox<String> cbRole = new ComboBox<>(FXCollections.observableArrayList("STUDENT", "PROFESSOR"));
        cbRole.setValue(isEdit ? (existing.isProfessor() ? "PROFESSOR" : "STUDENT") : "STUDENT");
        cbRole.setDisable(isEdit);

        ComboBox<String> cbProgram = new ComboBox<>(FXCollections.observableArrayList(PROGRAMS.subList(1, PROGRAMS.size())));
        ComboBox<String> cbYear = new ComboBox<>(FXCollections.observableArrayList(YEAR_LEVELS.subList(1, YEAR_LEVELS.size())));

        // Teaching subjects checklist for professors
        List<Subject> allSubs = subjectDAO.getAllSubjects();
        VBox profSubjectsBox = new VBox(6);
        profSubjectsBox.setStyle("-fx-background-color: #121520; -fx-padding: 8 10; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-background-radius: 6;");
        ScrollPane profSubjectsScroll = new ScrollPane(profSubjectsBox);
        profSubjectsScroll.setFitToWidth(true);
        profSubjectsScroll.setPrefHeight(130);
        profSubjectsScroll.setStyle("-fx-background: #121520; -fx-border-color: transparent;");

        List<CheckBox> subjectCheckBoxes = new ArrayList<>();
        Set<Integer> existingAssignedIds = new HashSet<>();
        if (isEdit && existing.isProfessor()) {
            List<Subject> assigned = subjectDAO.getSubjectsByProfessor(existing.getId());
            for (Subject s : assigned) existingAssignedIds.add(s.getId());
        }

        for (Subject s : allSubs) {
            CheckBox cb = new CheckBox(s.getName() + " (" + s.getCode() + ")");
            cb.setUserData(s.getId());
            cb.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 11px;");
            if (existingAssignedIds.contains(s.getId())) cb.setSelected(true);
            subjectCheckBoxes.add(cb);
            profSubjectsBox.getChildren().add(cb);
        }

        if (isEdit && existing.isStudent()) {
            if (existing.getProgram() != null) cbProgram.setValue(existing.getProgram());
            if (existing.getYearLevel() > 0) cbYear.setValue(existing.getYearDisplay());
        } else {
            cbProgram.setValue("BSIT");
            cbYear.setValue("1st Year");
        }

        tfUsername.setPromptText("e.g. juan.dela");
        tfFullName.setPromptText("e.g. Juan Dela Cruz");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20, 24, 16, 24));

        int row = 0;
        if (!isEdit) {
            grid.addRow(row++, styledLabel("Account Role:"), cbRole);
        }
        grid.addRow(row++, styledLabel("Username:"), tfUsername);
        grid.addRow(row++, styledLabel("Full Name:"), tfFullName);
        grid.addRow(row++, styledLabel("Password:"), boxPassword);

        Label programRow = styledLabel("Program:");
        Label yearRow = styledLabel("Year Level:");
        grid.addRow(row++, programRow, cbProgram);
        grid.addRow(row++, yearRow, cbYear);

        Label profSubRow = styledLabel("Teaching Subjects:");
        grid.addRow(row++, profSubRow, profSubjectsScroll);

        Runnable updateVis = () -> {
            boolean isStudent = "STUDENT".equals(cbRole.getValue());
            boolean isProf = "PROFESSOR".equals(cbRole.getValue());
            programRow.setVisible(isStudent); cbProgram.setVisible(isStudent);
            programRow.setManaged(isStudent); cbProgram.setManaged(isStudent);

            if (isStudent) {
                yearRow.setText("Year Level:");
                cbYear.setItems(FXCollections.observableArrayList(YEAR_LEVELS.subList(1, YEAR_LEVELS.size())));
                if (cbYear.getValue() == null) cbYear.setValue("1st Year");
            } else if (isProf) {
                yearRow.setText("Teaching Year:");
                List<String> profYears = List.of("All Years", "1st Year", "2nd Year", "3rd Year", "4th Year");
                cbYear.setItems(FXCollections.observableArrayList(profYears));
                if (cbYear.getValue() == null) cbYear.setValue("All Years");
            }

            yearRow.setVisible(isStudent || isProf); cbYear.setVisible(isStudent || isProf);
            yearRow.setManaged(isStudent || isProf); cbYear.setManaged(isStudent || isProf);
            profSubRow.setVisible(isProf); profSubjectsScroll.setVisible(isProf);
            profSubRow.setManaged(isProf); profSubjectsScroll.setManaged(isProf);
        };
        cbRole.valueProperty().addListener((obs, o, n) -> updateVis.run());
        updateVis.run();

        // Footer
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 12 24; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> ModalOverlay.close(cancelBtn));

        Button saveBtn = new Button(isEdit ? "💾 Save Changes" : "＋ Create Account");
        saveBtn.getStyleClass().add("btn-primary");

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, grid, footer);

        saveBtn.setOnAction(e -> {
            String username = tfUsername.getText().trim();
            String fullName = tfFullName.getText().trim();
            String password = boxPassword.getText().trim();
            String roleVal = isEdit ? existing.getRole() : cbRole.getValue();
            boolean isStudent = "STUDENT".equals(roleVal);
            boolean isProf = "PROFESSOR".equals(roleVal);
            String programVal = isStudent ? cbProgram.getValue() : null;
            int yearVal = 0;
            if (isStudent) {
                yearVal = YEAR_LEVELS.indexOf(cbYear.getValue());
            } else if (isProf) {
                String val = cbYear.getValue();
                yearVal = (val != null && !val.equalsIgnoreCase("All Years")) ? YEAR_LEVELS.indexOf(val) : 0;
            }

            if (username.isEmpty() || fullName.isEmpty()) {
                CustomAlert.showError(root.getScene().getWindow(), "Validation Error", "Username and Full Name are required.");
                return;
            }
            if (!isEdit && password.isEmpty()) {
                CustomAlert.showError(root.getScene().getWindow(), "Validation Error", "Password is required for new accounts.");
                return;
            }

            int excludeId = isEdit ? existing.getId() : 0;
            if (userDAO.isUsernameTaken(username, excludeId)) {
                CustomAlert.showError(root.getScene().getWindow(), "Validation Error", "The username \"" + username + "\" is already taken.");
                return;
            }

            List<Integer> selectedSubIds = new ArrayList<>();
            if (isProf) {
                for (CheckBox cb : subjectCheckBoxes) {
                    if (cb.isSelected()) selectedSubIds.add((Integer) cb.getUserData());
                }
                if (selectedSubIds.isEmpty()) {
                    CustomAlert.showError(root.getScene().getWindow(), "Validation Error", "Please assign at least one teaching subject for the professor.");
                    return;
                }
            }

            boolean ok;
            int targetUserId;
            if (isEdit) {
                // Preserve verified Gmail address intact
                String preservedEmail = existing.getEmail() != null ? existing.getEmail() : "";
                ok = userDAO.updateUserFull(existing.getId(), username, fullName, preservedEmail,
                        password.isEmpty() ? null : password, programVal, yearVal);
                targetUserId = existing.getId();
            } else {
                User newUser = new User(0, username, fullName, roleVal, "", programVal, yearVal);
                ok = userDAO.addUser(newUser, password);
                targetUserId = newUser.getId();
            }

            if (ok) {
                if (isProf && targetUserId > 0) {
                    subjectDAO.assignProfessorSubjects(targetUserId, selectedSubIds);
                }
                ModalOverlay.close(saveBtn);
                loadData();
                if (liveSyncService != null) liveSyncService.triggerImmediateSync();
                CustomAlert.showInfo(usersTable.getScene().getWindow(), "Success",
                        isEdit ? "Account updated successfully!" : "Account created successfully!");
            } else {
                CustomAlert.showError(root.getScene().getWindow(), "Database Error", "Failed to save account.");
            }
        });

        ModalOverlay.showAndWait(usersTable, root, 560, 520);
    }

    private Label styledLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold;");
        return lbl;
    }

    // ── Delete User ──────────────────────────────────────────────────────────

    @FXML
    private void handleDeleteUser() {
        if (selectedUserIds.isEmpty()) {
            User selected = usersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                confirmDeleteUser(selected);
            } else {
                CustomAlert.showWarning(usersTable.getScene().getWindow(), "No Selection", "Please select account(s) to delete.");
            }
            return;
        }

        boolean confirm = CustomAlert.showConfirmation(usersTable.getScene().getWindow(), "Delete Selected Accounts",
                "Are you sure you want to permanently delete " + selectedUserIds.size() + " selected user account(s)?\n\nThis action cannot be undone.");
        if (!confirm) return;

        int deletedCount = 0;
        User currentAdmin = Session.getCurrentUser();
        for (int id : new ArrayList<>(selectedUserIds)) {
            User target = masterUsersList.stream().filter(u -> u.getId() == id).findFirst().orElse(null);
            if (target != null) {
                if ("F4TAL".equalsIgnoreCase(target.getUsername()) || (currentAdmin != null && currentAdmin.getId() == id)) {
                    continue; // Safeguard master admin and self
                }
                if (userDAO.deleteUser(id)) {
                    deletedCount++;
                }
            }
        }
        selectedUserIds.clear();
        loadData();
        if (liveSyncService != null) liveSyncService.triggerImmediateSync();
        CustomAlert.showInfo(usersTable.getScene().getWindow(), "Deletion Complete", deletedCount + " user account(s) deleted.");
    }

    private void confirmDeleteUser(User target) {
        if (target == null) return;
        if ("F4TAL".equalsIgnoreCase(target.getUsername())) {
            CustomAlert.showError(usersTable.getScene().getWindow(), "Action Prohibited", "The master developer account (F4TAL) cannot be deleted.");
            return;
        }
        if (Session.getCurrentUser() != null && Session.getCurrentUser().getId() == target.getId()) {
            CustomAlert.showError(usersTable.getScene().getWindow(), "Action Prohibited", "You cannot delete your own active administrator account.");
            return;
        }

        boolean confirm = CustomAlert.showConfirmation(usersTable.getScene().getWindow(), "Confirm Deletion",
                "Are you sure you want to permanently delete \"" + target.getFullName() + "\" (" + target.getUsername() + ")?\nRole: " + target.getRole());
        if (!confirm) return;

        if (userDAO.deleteUser(target.getId())) {
            selectedUserIds.remove(target.getId());
            loadData();
            if (liveSyncService != null) liveSyncService.triggerImmediateSync();
            CustomAlert.showInfo(usersTable.getScene().getWindow(), "Account Deleted", "User account removed successfully.");
        } else {
            CustomAlert.showError(usersTable.getScene().getWindow(), "Deletion Failed", "Could not delete user account.");
        }
    }

    // ── Academic Controls Dialogs ───────────────────────────────────────────

    @FXML
    private void handleEnrollStudent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/enroll_student_dialog.fxml"));
            Parent root = loader.load();
            EnrollStudentController ctrl = loader.getController();

            List<Subject> subs = subjectDAO.getAllSubjects();
            ctrl.setSubjects(subs, subs.isEmpty() ? null : subs.get(0));

            ModalOverlay.showAndWait(usersTable, root, 920, 600);

            if (ctrl.isEnrolledSuccessfully()) {
                loadData();
                if (liveSyncService != null) liveSyncService.triggerImmediateSync();
            }
        } catch (IOException e) {
            CustomAlert.showError(usersTable.getScene().getWindow(), "Error", "Could not open Enrollment dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddSubject() {
        handleManageSubjects();
    }

    @FXML
    private void handleManageSubjects() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/add_subject_dialog.fxml"));
            Parent root = loader.load();
            AddSubjectController ctrl = loader.getController();

            ModalOverlay.showAndWait(usersTable, root, 480, 290);

            if (ctrl.isSubjectAdded()) {
                loadData();
                if (liveSyncService != null) liveSyncService.triggerImmediateSync();
            }
        } catch (IOException e) {
            CustomAlert.showError(usersTable.getScene().getWindow(), "Error", "Could not open Add Subject dialog: " + e.getMessage());
        }
    }

    // ── System Tools ─────────────────────────────────────────────────────────

    @FXML
    private void handleOpenBugReports() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/admin_inbox.fxml"));
            Parent root = loader.load();
            ModalOverlay.showAndWait(usersTable, root, 860, 640);

            loadData();
            if (liveSyncService != null) liveSyncService.triggerImmediateSync();
        } catch (IOException e) {
            CustomAlert.showError(usersTable.getScene().getWindow(), "Error", "Could not open Bug Reports: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenSmtpConfig() {
        EmailService.ensureConfigLoaded();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1d2e; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #151825; -fx-padding: 16 20; -fx-background-radius: 12 12 0 0; -fx-border-color: #2d3255; -fx-border-width: 0 0 1 0;");

        Label title = new Label("📧 SMTP & Gmail 2FA Security Configuration");
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold;");
        header.getChildren().add(title);

        VBox body = new VBox(14);
        body.setStyle("-fx-padding: 20;");

        Label desc = new Label("Configure the transactional email engine for OTP codes and bug report notifications.");
        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        TextField tfSender = new TextField(EmailService.getSenderEmail());
        tfSender.setPromptText("acadscatchup.system@gmail.com");

        TextField tfApiKey = new TextField(EmailService.getApiKey());
        tfApiKey.setPromptText("Brevo API Key (xkeysib-...)");

        PasswordField pfAppPass = new PasswordField();
        pfAppPass.setPromptText("Google 16-character App Password");
        pfAppPass.setText(EmailService.getAppPassword());

        CheckBox cb2FA = new CheckBox("Enforce Two-Factor Authentication (2FA) for Verified Users");
        cb2FA.setSelected(EmailService.is2FAEnabled());
        cb2FA.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");

        Label statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 12px;");

        HBox testBtns = new HBox(10);
        Button btnTestBrevo = new Button("Test Brevo API");
        btnTestBrevo.getStyleClass().add("btn-secondary");
        Button btnTestSmtp = new Button("Test Gmail SMTP");
        btnTestSmtp.getStyleClass().add("btn-secondary");
        testBtns.getChildren().addAll(btnTestBrevo, btnTestSmtp);

        btnTestBrevo.setOnAction(e -> {
            statusLbl.setText("Testing Brevo connection...");
            statusLbl.setStyle("-fx-text-fill: #93c5fd;");
            new Thread(() -> {
                try {
                    EmailService.testBrevoConnection(tfApiKey.getText().trim(), tfSender.getText().trim(), tfSender.getText().trim());
                    Platform.runLater(() -> {
                        statusLbl.setText("✔ Brevo API connected successfully!");
                        statusLbl.setStyle("-fx-text-fill: #34d399;");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLbl.setText("❌ Brevo test failed: " + ex.getMessage());
                        statusLbl.setStyle("-fx-text-fill: #f87171;");
                    });
                }
            }).start();
        });

        btnTestSmtp.setOnAction(e -> {
            statusLbl.setText("Testing Gmail SMTP connection...");
            statusLbl.setStyle("-fx-text-fill: #93c5fd;");
            new Thread(() -> {
                try {
                    EmailService.testSmtpConnection(tfSender.getText().trim(), pfAppPass.getText().trim(), tfSender.getText().trim());
                    Platform.runLater(() -> {
                        statusLbl.setText("✔ Gmail SMTP authenticated successfully!");
                        statusLbl.setStyle("-fx-text-fill: #34d399;");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLbl.setText("❌ Gmail SMTP test failed: " + ex.getMessage());
                        statusLbl.setStyle("-fx-text-fill: #f87171;");
                    });
                }
            }).start();
        });

        body.getChildren().addAll(
                desc,
                styledLabel("Sender Email Address:"), tfSender,
                styledLabel("Brevo API Key (Cloud Gateway):"), tfApiKey,
                styledLabel("Gmail SMTP App Password (Fallback):"), pfAppPass,
                cb2FA, testBtns, statusLbl
        );

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 14 20; -fx-background-radius: 0 0 12 12; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn-ghost");
        btnCancel.setOnAction(e -> ModalOverlay.close(btnCancel));

        Button btnSave = new Button("Save Configuration");
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setOnAction(e -> {
            boolean ok = EmailService.saveConfig(tfSender.getText().trim(), pfAppPass.getText().trim(), tfApiKey.getText().trim(), cb2FA.isSelected());
            if (ok) {
                ModalOverlay.close(btnSave);
                CustomAlert.showInfo(usersTable.getScene().getWindow(), "Configuration Saved", "Email and security settings updated successfully.");
            } else {
                CustomAlert.showError(usersTable.getScene().getWindow(), "Save Failed", "Could not persist email configuration.");
            }
        });

        footer.getChildren().addAll(btnCancel, btnSave);
        root.getChildren().addAll(header, body, footer);

        ModalOverlay.showAndWait(usersTable, root, 520, 560);
    }

    @FXML
    private void handleOpenAccountSettings() {
        com.acadscatchup.util.AccountSettingsDialog.show(usersTable.getScene().getWindow());
    }

    @FXML
    private void handleExportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Users Directory to CSV");
        fc.setInitialFileName("AcadsCatchUp_Users_Directory.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fc.showSaveDialog(usersTable.getScene().getWindow());
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("ID,Role,Username,FullName,Email,Verified,ProgramOrSubjects,YearLevel\n");
            for (User u : filteredUsers) {
                String progOrSubs = u.isProfessor()
                        ? profSubjectsMap.getOrDefault(u.getId(), "None")
                        : (u.getProgram() != null ? u.getProgram() : "");
                fw.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\"\n",
                        u.getId(),
                        escapeCsv(u.getRole()),
                        escapeCsv(u.getUsername()),
                        escapeCsv(u.getFullName()),
                        escapeCsv(u.getEmail()),
                        u.isVerified() ? "TRUE" : "FALSE",
                        escapeCsv(progOrSubs),
                        escapeCsv(u.isStudent() ? u.getYearDisplay() : "-")
                ));
            }
            CustomAlert.showInfo(usersTable.getScene().getWindow(), "Export Complete", "Exported " + filteredUsers.size() + " accounts to " + file.getName());
        } catch (IOException e) {
            CustomAlert.showError(usersTable.getScene().getWindow(), "Export Error", "Could not export CSV: " + e.getMessage());
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
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
            Stage stage = (Stage) adminNameLabel.getScene().getWindow();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
