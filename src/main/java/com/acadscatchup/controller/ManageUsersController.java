package com.acadscatchup.controller;

import com.acadscatchup.dao.UserDAO;
import com.acadscatchup.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.acadscatchup.util.EmailService;
import com.acadscatchup.util.WindowUtil;
import com.acadscatchup.util.CustomAlert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for the Manage Users dialog.
 * Professors and Admins can add, edit, and delete student/professor accounts.
 * Students get extra profile fields: Program and Year Level.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class ManageUsersController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private HBox                     headerBar;
    @FXML private TableView<User>          usersTable;
    @FXML private TableColumn<User,Boolean>colSelect;
    @FXML private TableColumn<User,String> colUsername;
    @FXML private TableColumn<User,String> colFullName;
    @FXML private TableColumn<User,String> colEmail;
    @FXML private TableColumn<User,String> colRole;
    @FXML private TableColumn<User,String> colProgram;
    @FXML private TableColumn<User,String> colYear;
    @FXML private TableColumn<User,Void>   colActions;
    @FXML private Button                   btnAddUser;
    @FXML private Button                   btnDeleteSelected;
    @FXML private Button                   btnBulkDeleteStudents;
    @FXML private Label                    selectionCountLabel;

    private final Set<Integer> selectedUserIds = new HashSet<>();
    private final CheckBox headerSelectAll = new CheckBox();

    private static final List<String> PROGRAMS = List.of(
            "BSIT", "BSCS", "BSIS", "BSECE", "BSCpE",
            "BSEE", "BSMath", "BSEntrep", "BSED", "BSBA", "Other");

    private static final List<String> YEAR_LEVELS = List.of(
            "1st Year", "2nd Year", "3rd Year", "4th Year");

    private final UserDAO userDAO = new UserDAO();
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

        if (colSelect != null) {
            colSelect.setGraphic(headerSelectAll);
            headerSelectAll.setOnAction(e -> {
                if (headerSelectAll.isSelected()) {
                    handleSelectAll();
                } else {
                    handleDeselectAll();
                }
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
                        return;
                    }
                    User u = getTableView().getItems().get(getIndex());
                    boolean isProtected = "ADMIN".equalsIgnoreCase(u.getRole()) || "PROFESSOR".equalsIgnoreCase(u.getRole());
                    if (isProtected) {
                        cb.setDisable(true);
                        cb.setSelected(false);
                        cb.setOpacity(0.3);
                    } else {
                        cb.setDisable(false);
                        cb.setSelected(selectedUserIds.contains(u.getId()));
                        cb.setOpacity(1.0);
                    }
                    setGraphic(cb);
                }
            });
        }

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        if (colEmail != null) {
            colEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue().getEmail() != null && !d.getValue().getEmail().isBlank() ? d.getValue().getEmail() : "Pending Verification"
            ));
            colEmail.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String email, boolean empty) {
                    super.updateItem(email, empty);
                    if (empty || email == null) {
                        setText(null);
                        setStyle("");
                    } else if (email.equals("Pending Verification")) {
                        setText(email);
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-style: italic; -fx-font-size: 11px;");
                    } else {
                        setText(email);
                        setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                    }
                }
            });
        }

        // Program / Dedicated Subject
        colProgram.setText("Program / Subject");
        colProgram.setCellValueFactory(d -> {
            User u = d.getValue();
            if (u.isProfessor() && u.getAssignedSubject() != null && !u.getAssignedSubject().isBlank()) {
                return new javafx.beans.property.SimpleStringProperty(u.getAssignedSubject());
            }
            return new javafx.beans.property.SimpleStringProperty(u.getProgram() != null && !u.getProgram().isBlank() ? u.getProgram() : "—");
        });

        // Year display
        colYear.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setText(null); return; }
                User u = getTableView().getItems().get(getIndex());
                if (u.isProfessor()) {
                    setText(u.getYearLevel() > 0 ? u.getYearDisplay() : "All Years");
                } else if (u.isStudent()) {
                    setText(u.getYearLevel() > 0 ? u.getYearDisplay() : "—");
                } else {
                    setText("—");
                }
            }
        });

        // Role badge
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(role);
                badge.getStyleClass().add(switch (role) {
                    case "PROFESSOR" -> "badge-graded";
                    case "ADMIN"     -> "badge-pending";
                    default          -> "badge-submitted";
                });
                setGraphic(badge); setText(null);
            }
        });

        // Actions: Edit + Delete
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑 Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-cursor: hand; -fx-text-fill: #e0e7ff; -fx-background-color: #24294a; -fx-border-color: #3e477a; -fx-border-radius: 6; -fx-background-radius: 6;");
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-cursor: hand; -fx-text-fill: #fca5a5; -fx-background-color: rgba(220,38,38,0.25); -fx-border-color: rgba(239,68,68,0.6); -fx-border-radius: 6; -fx-background-radius: 6;");
                editBtn.setOnAction(e -> onEdit(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                if (u == null) { setGraphic(null); return; }
                boolean cannotDelete = "ADMIN".equalsIgnoreCase(u.getRole()) || "F4TAL".equalsIgnoreCase(u.getUsername());
                deleteBtn.setDisable(cannotDelete);
                if (cannotDelete) {
                    deleteBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-opacity: 0.35; -fx-text-fill: #94a3b8; -fx-background-color: rgba(40,45,65,0.3); -fx-border-color: #3b4267; -fx-border-radius: 6; -fx-background-radius: 6;");
                } else {
                    deleteBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-cursor: hand; -fx-text-fill: #fca5a5; -fx-background-color: rgba(220,38,38,0.25); -fx-border-color: rgba(239,68,68,0.6); -fx-border-radius: 6; -fx-background-radius: 6;");
                }
                setGraphic(box);
            }
        });

        refreshTable();
    }

    private void refreshTable() {
        selectedUserIds.clear();
        usersTable.setItems(FXCollections.observableArrayList(userDAO.getAllUsers()));
        updateSelectionState();
    }

    @FXML
    private void handleSelectAll() {
        for (User u : usersTable.getItems()) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole()) && !"F4TAL".equalsIgnoreCase(u.getUsername())) {
                selectedUserIds.add(u.getId());
            }
        }
        updateSelectionState();
        usersTable.refresh();
    }

    @FXML
    private void handleDeselectAll() {
        selectedUserIds.clear();
        updateSelectionState();
        usersTable.refresh();
    }

    private void updateSelectionState() {
        int count = selectedUserIds.size();
        if (selectionCountLabel != null) {
            selectionCountLabel.setText(count + " account" + (count == 1 ? "" : "s") + " selected");
        }
        if (btnDeleteSelected != null) {
            btnDeleteSelected.setText(com.acadscatchup.util.OSCompat.label("🗑 ") + "Delete Selected (" + count + ")");
            btnDeleteSelected.setDisable(count == 0);
        }
        long eligibleCount = usersTable.getItems().stream()
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole()) && !"F4TAL".equalsIgnoreCase(u.getUsername()))
                .count();
        headerSelectAll.setSelected(eligibleCount > 0 && count == eligibleCount);
    }

    @FXML
    private void handleDeleteSelected() {
        if (selectedUserIds.isEmpty()) return;
        javafx.stage.Window owner = usersTable.getScene().getWindow();
        int count = selectedUserIds.size();

        boolean confirmed = com.acadscatchup.util.CustomAlert.showConfirmation(owner,
                "Delete Selected Accounts",
                "Are you sure you want to permanently delete " + count + " selected account" + (count == 1 ? "" : "s") + "?\n\nAll associated courses, enrollments, missed tasks, and submissions will also be deleted.\nThis action cannot be undone.");

        if (!confirmed) return;

        int deletedCount = 0;
        for (int id : new ArrayList<>(selectedUserIds)) {
            if (userDAO.deleteUser(id)) {
                deletedCount++;
            }
        }

        selectedUserIds.clear();
        refreshTable();
        com.acadscatchup.util.CustomAlert.showInfo(owner,
                "Accounts Deleted",
                "Successfully deleted " + deletedCount + " user account" + (deletedCount == 1 ? "" : "s") + ".");
    }

    @FXML
    private void handleBulkDeleteStudents() {
        javafx.stage.Window owner = (usersTable.getScene() != null) ? usersTable.getScene().getWindow() : null;
        List<User> students = usersTable.getItems().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .toList();

        if (students.isEmpty()) {
            com.acadscatchup.util.CustomAlert.showInfo(owner,
                    "No Student Accounts",
                    "There are currently no student accounts registered in the system to delete.");
            return;
        }

        int count = students.size();
        boolean confirmed = com.acadscatchup.util.CustomAlert.showConfirmation(owner,
                "Bulk Delete All Students",
                "Are you sure you want to permanently delete ALL " + count + " student account" + (count == 1 ? "" : "s") + "?\n\n"
                + "• All " + count + " student accounts will be deleted.\n"
                + "• All associated student enrollments, deficiencies, and submitted files will be purged.\n"
                + "• Administrator and Professor accounts will NOT be affected.\n\n"
                + "This bulk deletion cannot be undone!");

        if (!confirmed) return;

        int deletedCount = 0;
        for (User s : students) {
            if (userDAO.deleteUser(s.getId())) {
                deletedCount++;
            }
        }

        selectedUserIds.clear();
        refreshTable();
        com.acadscatchup.util.CustomAlert.showInfo(owner,
                "Bulk Deletion Complete",
                "Successfully deleted " + deletedCount + " student account" + (deletedCount == 1 ? "" : "s") + ".");
    }

    @FXML private void onAddUser() { showUserDialog(null); }

    private void onEdit(User user)   { showUserDialog(user); }

    private void onDelete(User user) {
        javafx.stage.Window owner = usersTable.getScene().getWindow();
        if ("ADMIN".equalsIgnoreCase(user.getRole()) || "F4TAL".equalsIgnoreCase(user.getUsername())) {
            com.acadscatchup.util.CustomAlert.showWarning(owner,
                    "Action Not Allowed",
                    "Permission Denied: Administrator accounts cannot be deleted.");
            return;
        }

        boolean confirmed = com.acadscatchup.util.CustomAlert.showConfirmation(owner,
                "Confirm Delete",
                "Delete " + user.getRole().toLowerCase() + " account \"" + user.getUsername() + "\"?\nAll associated courses and records will also be deleted.");
        if (confirmed) {
            userDAO.deleteUser(user.getId());
            refreshTable();
        }
    }

    /** Shared Add / Edit dialog — Styled to match FAQ and AcadsCatchUp design system */
    private void showUserDialog(User existing) {
        boolean isEdit = existing != null;
        boolean isStudent = !isEdit || "STUDENT".equals(existing.getRole());
        boolean isCurrentAdmin = com.acadscatchup.util.Session.getCurrentUser() != null
                && com.acadscatchup.util.Session.getCurrentUser().isAdmin();

        // ── Root container ──────────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle("-fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-background-color: #0f1117;");
        try {
            root.getStylesheets().add(getClass().getResource("/com/acadscatchup/css/style.css").toExternalForm());
        } catch (Exception ignored) {}

        // ── Header (FAQ style) ──────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-header");
        header.setStyle("-fx-padding: 12 18; -fx-background-color: #121520; -fx-border-color: #2d3255; -fx-border-width: 0 0 1 0;");

        javafx.scene.image.ImageView icon = new javafx.scene.image.ImageView();
        try {
            icon.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")));
            icon.setFitWidth(30);
            icon.setFitHeight(30);
            icon.setPreserveRatio(true);
        } catch (Exception ignored) {}

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(isEdit ? "Edit Account — " + existing.getUsername() : "Add New Account");
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subLabel = new Label("Configure account credentials, roles & permissions");
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(titleLabel, subLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(icon, titleBox, spacer);

        // ── Fields ──────────────────────────────────────────────────
        TextField     tfUsername  = new TextField(isEdit ? existing.getUsername() : "");
        TextField     tfFullName  = new TextField(isEdit ? existing.getFullName()  : "");
        com.acadscatchup.util.PasswordToggleHelper.PasswordBox boxPassword =
                com.acadscatchup.util.PasswordToggleHelper.createPasswordBox(
                        isEdit ? "Leave blank to keep current" : "Set password",
                        null
                );

        List<String> roleOptions = isCurrentAdmin
                ? List.of("STUDENT", "PROFESSOR")
                : List.of("STUDENT");

        ComboBox<String> cbRole   = new ComboBox<>(FXCollections.observableArrayList(roleOptions));
        cbRole.setValue(isEdit ? (existing.isProfessor() ? "PROFESSOR" : "STUDENT") : "STUDENT");
        cbRole.setDisable(isEdit || !isCurrentAdmin); // role cannot change after creation, and only admin can assign professor

        ComboBox<String> cbProgram = new ComboBox<>(FXCollections.observableArrayList(PROGRAMS));
        ComboBox<String> cbYear    = new ComboBox<>(FXCollections.observableArrayList(YEAR_LEVELS));

        // Multi-subject checklist for professors
        com.acadscatchup.dao.SubjectDAO sDao = new com.acadscatchup.dao.SubjectDAO();
        List<com.acadscatchup.model.Subject> allSubs = sDao.getAllSubjects();
        VBox profSubjectsBox = new VBox(6);
        profSubjectsBox.setStyle("-fx-background-color: #121520; -fx-padding: 8 10; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-background-radius: 6;");
        ScrollPane profSubjectsScroll = new ScrollPane(profSubjectsBox);
        profSubjectsScroll.setFitToWidth(true);
        profSubjectsScroll.setPrefHeight(130);
        profSubjectsScroll.setStyle("-fx-background: #121520; -fx-border-color: transparent;");

        List<CheckBox> subjectCheckBoxes = new ArrayList<>();
        Set<Integer> existingAssignedIds = new HashSet<>();
        if (isEdit && existing.isProfessor()) {
            List<com.acadscatchup.model.Subject> assigned = sDao.getSubjectsByProfessor(existing.getId());
            for (com.acadscatchup.model.Subject s : assigned) {
                existingAssignedIds.add(s.getId());
            }
        }

        for (com.acadscatchup.model.Subject s : allSubs) {
            CheckBox cb = new CheckBox(s.getName() + " (" + s.getCode() + ")");
            cb.setUserData(s.getId());
            cb.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 11px;");
            if (existingAssignedIds.contains(s.getId())) {
                cb.setSelected(true);
            }
            subjectCheckBoxes.add(cb);
            profSubjectsBox.getChildren().add(cb);
        }

        // Pre-fill for existing user
        if (isEdit && existing.isStudent()) {
            if (existing.getProgram() != null) cbProgram.setValue(existing.getProgram());
            if (existing.getYearLevel() > 0)   cbYear.setValue(existing.getYearDisplay());
        } else {
            cbProgram.setValue("BSIT");
            cbYear.setValue("1st Year");
        }

        tfUsername.setPromptText("e.g. juan.dela");
        tfFullName.setPromptText("e.g. Juan Dela Cruz");

        // ── Layout ──────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20, 24, 16, 24));

        int row = 0;
        if (!isEdit) {
            grid.addRow(row++, styledLabel("Role:"), cbRole);
        }
        grid.addRow(row++, styledLabel("Username:"),      tfUsername);
        grid.addRow(row++, styledLabel("Full Name:"),     tfFullName);
        grid.addRow(row++, styledLabel("Password:"),      boxPassword);

        // Student-only fields
        Label programRow = styledLabel("Program:");
        Label yearRow    = styledLabel("Year Level:");
        grid.addRow(row++, programRow, cbProgram);
        grid.addRow(row++, yearRow,    cbYear);

        // Professor-only field (Multi-subject)
        Label profSubRow = styledLabel("Teaching Subjects:");
        grid.addRow(row++, profSubRow, profSubjectsScroll);

        Runnable updateVisibility = () -> {
            boolean showStudent = "STUDENT".equals(cbRole.getValue());
            boolean showProf    = "PROFESSOR".equals(cbRole.getValue());
            programRow.setVisible(showStudent); cbProgram.setVisible(showStudent);
            programRow.setManaged(showStudent); cbProgram.setManaged(showStudent);

            if (showStudent) {
                yearRow.setText("Year Level:");
                cbYear.setItems(FXCollections.observableArrayList(YEAR_LEVELS));
                if (cbYear.getValue() == null || !YEAR_LEVELS.contains(cbYear.getValue())) {
                    cbYear.setValue("1st Year");
                }
            } else if (showProf) {
                yearRow.setText("Teaching Year:");
                List<String> profYears = List.of("All Years", "1st Year", "2nd Year", "3rd Year", "4th Year");
                cbYear.setItems(FXCollections.observableArrayList(profYears));
                if (cbYear.getValue() == null || !profYears.contains(cbYear.getValue())) {
                    cbYear.setValue((existing != null && existing.getYearLevel() > 0) ? existing.getYearDisplay() : "All Years");
                }
            }

            yearRow.setVisible(showStudent || showProf);    cbYear.setVisible(showStudent || showProf);
            yearRow.setManaged(showStudent || showProf);    cbYear.setManaged(showStudent || showProf);

            profSubRow.setVisible(showProf);    profSubjectsScroll.setVisible(showProf);
            profSubRow.setManaged(showProf);    profSubjectsScroll.setManaged(showProf);
        };
        cbRole.valueProperty().addListener((obs, o, n) -> updateVisibility.run());

        // Initial visibility
        updateVisibility.run();

        // ── Footer (FAQ style) ──────────────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 12 24; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> com.acadscatchup.util.ModalOverlay.close(cancelBtn));

        Button saveBtn = new Button(isEdit ? "💾 Save Changes" : "＋ Add Account");
        saveBtn.getStyleClass().add("btn-primary");

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, grid, footer);

        saveBtn.setOnAction(e -> {
            // ── Validate ─────────────────────────────────────────────────
            String username = tfUsername.getText().trim();
            String fullName = tfFullName.getText().trim();
            String email    = (isEdit && existing.getEmail() != null) ? existing.getEmail() : "";
            String password = boxPassword.getText().trim();
            String roleVal  = isEdit ? existing.getRole() : cbRole.getValue();
            boolean studentRole = "STUDENT".equals(roleVal);
            boolean profRole    = "PROFESSOR".equals(roleVal);
            String programVal = studentRole ? cbProgram.getValue() : null;
            int    yearVal    = 0;
            if (studentRole) {
                yearVal = YEAR_LEVELS.indexOf(cbYear.getValue()) + 1;
            } else if (profRole) {
                String val = cbYear.getValue();
                if (val != null && !val.equalsIgnoreCase("All Years")) {
                    yearVal = YEAR_LEVELS.indexOf(val) + 1;
                } else {
                    yearVal = 0;
                }
            }

            if (username.isEmpty() || fullName.isEmpty()) {
                showError("Username and Full Name are required.");
                return;
            }
            if (!isEdit && password.isEmpty()) {
                showError("Password is required for new accounts.");
                return;
            }
            if (studentRole && cbProgram.getValue() == null) {
                showError("Please select a program for the student.");
                return;
            }

            int excludeId = isEdit ? existing.getId() : 0;
            if (userDAO.isUsernameTaken(username, excludeId)) {
                showError("The username \"" + username + "\" is already taken.");
                return;
            }

            List<Integer> selectedSubIds = new ArrayList<>();
            if (profRole) {
                for (CheckBox cb : subjectCheckBoxes) {
                    if (cb.isSelected()) {
                        selectedSubIds.add((Integer) cb.getUserData());
                    }
                }
                if (selectedSubIds.isEmpty()) {
                    showError("Please select at least one teaching subject for the professor.");
                    return;
                }
            }

            // ── Save ─────────────────────────────────────────────────────
            boolean ok;
            int targetUserId;
            if (isEdit) {
                ok = userDAO.updateUserFull(existing.getId(), username, fullName, email,
                        password.isEmpty() ? null : password, programVal, yearVal);
                targetUserId = existing.getId();
            } else {
                User newUser = new User(0, username, fullName, roleVal, email, programVal, yearVal);
                ok = userDAO.addUser(newUser, password);
                targetUserId = newUser.getId();
            }

            if (ok) {
                if (profRole) {
                    sDao.assignProfessorSubjects(targetUserId, selectedSubIds);
                }
                com.acadscatchup.util.ModalOverlay.close(saveBtn);
                refreshTable();
                String successMsg = isEdit
                        ? "Account for " + fullName + " has been updated successfully!"
                        : "Account for " + fullName + " has been created. They will verify their email on first login.";
                com.acadscatchup.util.CustomAlert.showInfo(usersTable.getScene().getWindow(),
                        isEdit ? "Account Updated" : "Account Created",
                        successMsg);
            } else {
                showError("Failed to save. Username or email may already be taken.");
            }
        });

        com.acadscatchup.util.ModalOverlay.showAndWait(usersTable, root, 490, 580);
    }

    private Label styledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #c9d1d9;");
        return l;
    }

    private void showError(String msg) {
        javafx.stage.Window owner = (usersTable.getScene() != null) ? usersTable.getScene().getWindow() : null;
        com.acadscatchup.util.CustomAlert.showError(owner, "Error", msg);
    }

    @FXML
    private void handleEmailSettings() {
        EmailService.ensureConfigLoaded();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1d2e; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #151825; -fx-padding: 16 20; -fx-background-radius: 12 12 0 0; -fx-border-color: #2d3255; -fx-border-width: 0 0 1 0;");
        Label icon = new Label("✉");
        icon.setStyle("-fx-font-size: 22px;");
        VBox titleBox = new VBox(2);
        Label title = new Label("Gmail OTP & 2FA Settings");
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label sub = new Label("Configure Google SMTP Relay for real OTP emails");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(icon, titleBox, spacer);

        // Content
        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 22, 14, 22));

        Label senderLbl = styledLabel("Sender Email Address (Brevo login or Gmail):");
        TextField tfSender = new TextField(EmailService.getSenderEmail());
        tfSender.setPromptText("e.g. yourname@gmail.com");

        Label apiKeyLbl = styledLabel("Brevo API Key (Option 2 — 300 Free Emails/Day):");
        TextField tfApiKey = new TextField(EmailService.getApiKey());
        tfApiKey.setPromptText("e.g. xkeysib-...");

        Label passLbl = styledLabel("Or Google App Password (Option 1):");
        com.acadscatchup.util.PasswordToggleHelper.PasswordBox pfAppPass =
                com.acadscatchup.util.PasswordToggleHelper.createPasswordBox(
                        "e.g. 16-letter App Password (if using direct Gmail)",
                        null
                );
        pfAppPass.setText(EmailService.getAppPassword());

        CheckBox cb2FA = new CheckBox("Require Two-Factor Authentication (2FA) on Login");
        cb2FA.setSelected(EmailService.is2FAEnabled());
        cb2FA.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");

        VBox hintBox = new VBox(4);
        hintBox.setStyle("-fx-background-color: #121520; -fx-padding: 10 12; -fx-border-color: #2d3255; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label h1 = new Label("ℹ Option 2: Brevo Setup (100% Free):");
        h1.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px;");
        Label h2 = new Label("1. Sign up free at brevo.com (no credit card)\n2. Go to SMTP & API ➔ Generate API Key\n3. Paste the xkeysib-... key above.");
        h2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        hintBox.getChildren().addAll(h1, h2);

        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-font-size: 12px;");
        statusLbl.setVisible(false); statusLbl.setManaged(false);

        content.getChildren().addAll(senderLbl, tfSender, apiKeyLbl, tfApiKey, passLbl, pfAppPass, cb2FA, hintBox, statusLbl);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 12 20; -fx-background-radius: 0 0 12 12; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button testBtn = new Button("⚡ Test Connection");
        testBtn.getStyleClass().add("btn-secondary");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> com.acadscatchup.util.ModalOverlay.close(cancelBtn));

        Button saveBtn = new Button("💾 Save Settings");
        saveBtn.getStyleClass().add("btn-primary");

        testBtn.setOnAction(e -> {
            String sEmail  = tfSender.getText().trim();
            String sApiKey = tfApiKey.getText().trim();
            String sPass   = pfAppPass.getText().trim();

            if (sApiKey.isEmpty() && sPass.isEmpty()) {
                statusLbl.setText("Please enter either a Brevo API Key or Google App Password to test.");
                statusLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                statusLbl.setVisible(true); statusLbl.setManaged(true);
                return;
            }

            testBtn.setDisable(true);
            statusLbl.setText("Testing connection and sending verification OTP...");
            statusLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px;");
            statusLbl.setVisible(true); statusLbl.setManaged(true);

            new Thread(() -> {
                try {
                    if (!sApiKey.isEmpty()) {
                        EmailService.testBrevoConnection(sApiKey, sEmail, sEmail);
                        javafx.application.Platform.runLater(() -> {
                            testBtn.setDisable(false);
                            statusLbl.setText("✔ Brevo connected! Test OTP email sent to " + sEmail);
                            statusLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: bold;");
                        });
                    } else {
                        EmailService.testSmtpConnection(sEmail, sPass, sEmail);
                        javafx.application.Platform.runLater(() -> {
                            testBtn.setDisable(false);
                            statusLbl.setText("✔ Gmail connected! Test OTP email sent to " + sEmail);
                            statusLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: bold;");
                        });
                    }
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        testBtn.setDisable(false);
                        statusLbl.setText("✖ Test failed: " + ex.getMessage());
                        statusLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                    });
                }
            }).start();
        });

        saveBtn.setOnAction(e -> {
            String sEmail  = tfSender.getText().trim();
            String sApiKey = tfApiKey.getText().trim();
            String sPass   = pfAppPass.getText().trim();
            boolean is2fa  = cb2FA.isSelected();

            boolean ok = EmailService.saveConfig(sEmail, sPass, sApiKey, is2fa);
            if (ok) {
                com.acadscatchup.util.ModalOverlay.close(saveBtn);
                CustomAlert.showInfo(usersTable.getScene().getWindow(), "Settings Saved", "Email and Security settings successfully saved to cloud database!");
            } else {
                statusLbl.setText("Failed to save settings to database.");
                statusLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                statusLbl.setVisible(true); statusLbl.setManaged(true);
            }
        });

        footer.getChildren().addAll(testBtn, cancelBtn, saveBtn);
        root.getChildren().addAll(header, content, footer);

        com.acadscatchup.util.ModalOverlay.showAndWait(usersTable, root, 480, 480);
    }

    @FXML
    private void onClose() {
        com.acadscatchup.util.ModalOverlay.close(usersTable);
    }
}
