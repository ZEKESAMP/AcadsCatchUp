package com.acadscatchup.controller;

import com.acadscatchup.dao.InboxDAO;
import com.acadscatchup.dao.SubjectDAO;
import com.acadscatchup.model.Subject;
import com.acadscatchup.model.User;
import com.acadscatchup.util.CustomAlert;
import com.acadscatchup.util.Session;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.*;

/**
 * Controller for enrolling students into a subject.
 * Shows all students in the system with their enrollment status and professor.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class EnrollStudentController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private HBox headerBar;
    @FXML private ComboBox<Subject> subjectCombo;
    @FXML private Label profNameLabel;
    @FXML private ComboBox<String>  yearFilterCombo;
    @FXML private TextField searchField;
    @FXML private TableView<StudentRow> studentsTable;

    @FXML private TableColumn<StudentRow, Boolean> colSelect;
    @FXML private TableColumn<StudentRow, String>  colFullName;
    @FXML private TableColumn<StudentRow, String>  colProgram;
    @FXML private TableColumn<StudentRow, String>  colYear;
    @FXML private TableColumn<StudentRow, String>  colStatus;
    @FXML private TableColumn<StudentRow, String>  colProf;

    @FXML private Label selectedCountLabel;
    @FXML private Button btnEnroll;
    @FXML private Button btnUnenroll;
    @FXML private Button btnEnrollAllSubjects;
    @FXML private Button btnUnenrollAllSubjects;

    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final InboxDAO inboxDAO = new InboxDAO();
    private final ObservableList<StudentRow> tableRows = FXCollections.observableArrayList();

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean enrolledSuccessfully = false;

    public static class StudentRow {
        private final User user;
        private final BooleanProperty selected;
        private final boolean initialEnrolled;
        private final String professorName;

        public StudentRow(User user, boolean initiallyEnrolled, String profName) {
            this.user = user;
            this.initialEnrolled = initiallyEnrolled;
            this.selected = new SimpleBooleanProperty(initiallyEnrolled);
            this.professorName = profName;
        }

        public User getUser() { return user; }
        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { selected.set(val); }
        public boolean isInitiallyEnrolled() { return initialEnrolled; }
        public String getProfessorName() { return professorName; }
    }

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

        yearFilterCombo.setItems(FXCollections.observableArrayList(
                "ALL", "1st Year", "2nd Year", "3rd Year", "4th Year"));
        yearFilterCombo.setValue("ALL");

        subjectCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Subject s) {
                return s != null ? s.getName() + " (" + s.getCode() + ")" : "";
            }
            @Override public Subject fromString(String string) { return null; }
        });

        // Table setup
        colSelect.setCellValueFactory(d -> d.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);

        colFullName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUser().getFullName()));
        colProgram.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUser().getProgram() != null ? d.getValue().getUser().getProgram() : "—"));
        colYear.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUser().getYearDisplay()));

        // Status badge column
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isSelected() ? "Enrolled" : "Not Enrolled"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(val);
                if ("Enrolled".equalsIgnoreCase(val)) {
                    badge.getStyleClass().add("status-badge-graded");
                    badge.setStyle("-fx-font-size: 10.5px; -fx-padding: 2 8; -fx-background-radius: 4; -fx-min-width: -Infinity; -fx-text-overrun: clip;");
                } else {
                    badge.getStyleClass().add("status-badge-pending");
                    badge.setStyle("-fx-font-size: 10.5px; -fx-padding: 2 8; -fx-background-radius: 4; -fx-opacity: 0.85; -fx-min-width: -Infinity; -fx-text-overrun: clip;");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        colProf.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProfessorName()));

        studentsTable.setEditable(true);
        studentsTable.setItems(tableRows);

        // Listeners for instant filtering
        subjectCombo.valueProperty().addListener((obs, o, n) -> loadStudents());
        yearFilterCombo.valueProperty().addListener((obs, o, n) -> loadStudents());
        searchField.textProperty().addListener((obs, o, n) -> loadStudents());
    }

    public void setSubjects(List<Subject> subjects, Subject defaultSubject) {
        subjectCombo.setItems(FXCollections.observableArrayList(subjects));
        if (defaultSubject != null) {
            subjectCombo.setValue(defaultSubject);
        } else if (!subjects.isEmpty()) {
            subjectCombo.setValue(subjects.get(0));
        }
        loadStudents();
    }

    private void loadStudents() {
        Subject sub = subjectCombo.getValue();
        if (sub == null) {
            tableRows.clear();
            updateSelectedCount();
            if (profNameLabel != null) profNameLabel.setText("👤 None");
            return;
        }

        String profName = subjectDAO.getProfessorsNamesBySubject(sub.getId());
        if (profNameLabel != null) {
            profNameLabel.setText("👤 " + profName);
        }

        String yearStr = yearFilterCombo.getValue();
        int yearLevel = 0;
        if ("1st Year".equals(yearStr)) yearLevel = 1;
        else if ("2nd Year".equals(yearStr)) yearLevel = 2;
        else if ("3rd Year".equals(yearStr)) yearLevel = 3;
        else if ("4th Year".equals(yearStr)) yearLevel = 4;

        String query = searchField.getText();

        List<SubjectDAO.StudentEnrollmentStatus> list = subjectDAO.getAllStudentsWithEnrollmentStatus(sub.getId(), query, yearLevel);
        ObservableList<StudentRow> rows = FXCollections.observableArrayList();
        for (SubjectDAO.StudentEnrollmentStatus status : list) {
            StudentRow r = new StudentRow(status.getUser(), status.isEnrolled(), status.getProfessorName());
            r.selectedProperty().addListener((obs, o, n) -> {
                updateSelectedCount();
                studentsTable.refresh();
            });
            rows.add(r);
        }

        tableRows.setAll(rows);
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        long enrolledCount = tableRows.stream().filter(StudentRow::isSelected).count();
        selectedCountLabel.setText(enrolledCount + " of " + tableRows.size() + " students enrolled");
        btnEnroll.setDisable(tableRows.isEmpty());
    }

    @FXML
    private void handleSelectAll() {
        tableRows.forEach(r -> r.setSelected(true));
        studentsTable.refresh();
        updateSelectedCount();
    }

    @FXML
    private void handleDeselectAll() {
        tableRows.forEach(r -> r.setSelected(false));
        studentsTable.refresh();
        updateSelectedCount();
    }

    @FXML
    private void handleEnroll() {
        Subject sub = subjectCombo.getValue();
        if (sub == null) return;

        User currUser = Session.getCurrentUser();
        String senderName = currUser != null ? currUser.getFullName() : "Professor";
        String senderRole = currUser != null ? currUser.getRole() : "PROFESSOR";
        int senderId = currUser != null ? currUser.getId() : 0;

        int newlyEnrolled = 0;
        int newlyUnenrolled = 0;

        for (StudentRow r : tableRows) {
            boolean currentlySelected = r.isSelected();
            boolean wasEnrolled = r.isInitiallyEnrolled();

            if (currentlySelected && !wasEnrolled) {
                if (subjectDAO.enrollStudent(r.getUser().getId(), sub.getId())) {
                    newlyEnrolled++;

                    // Send official enrollment notification message to the student's personal inbox
                    String title = "🎓 Enrolled in " + sub.getCode() + " — " + sub.getName();
                    String body = "Hello " + r.getUser().getFullName() + ",\n\n"
                            + "You have been officially enrolled into the following subject:\n"
                            + "📖 " + sub.getCode() + " - " + sub.getName() + "\n"
                            + "👨‍🏫 Instructor: " + senderName + "\n\n"
                            + "You can now view this subject in your Enrolled Subjects section on your dashboard, check for any missed activities or quizzes, and submit requirements directly.\n\n"
                            + "— AcadsCatchUp Academic Management System";

                    inboxDAO.sendMessage(
                            senderId, senderName, senderRole,
                            r.getUser().getId(), r.getUser().getFullName(),
                            title, body,
                            null, null, sub.getCode(),
                            "ENROLLMENT"
                    );
                }
            } else if (!currentlySelected && wasEnrolled) {
                if (subjectDAO.unenrollStudent(r.getUser().getId(), sub.getId())) {
                    newlyUnenrolled++;
                }
            }
        }

        enrolledSuccessfully = true;
        Stage stage = (Stage) btnEnroll.getScene().getWindow();
        CustomAlert.showInfo(stage.getOwner(), "Enrollments Updated 🎉",
                "Enrollment updated for " + sub.getName() + " (" + sub.getCode() + ")!\n" +
                newlyEnrolled + " enrolled, " + newlyUnenrolled + " unenrolled.");

        stage.close();
    }

    @FXML
    private void handleEnrollAllSubjects() {
        List<StudentRow> selectedStudents = tableRows.stream().filter(StudentRow::isSelected).toList();
        if (selectedStudents.isEmpty()) {
            Stage stage = (Stage) btnEnroll.getScene().getWindow();
            CustomAlert.showWarning(stage.getOwner(), "No Students Selected", "Please select at least one student to enroll in all subjects.");
            return;
        }

        Stage stage = (Stage) btnEnroll.getScene().getWindow();
        boolean confirmed = CustomAlert.showConfirmation(stage.getOwner(),
                "Enroll to All Subjects",
                "Are you sure you want to enroll " + selectedStudents.size() + " student(s) into ALL subjects in the curriculum?");

        if (!confirmed) return;

        List<Integer> studentIds = selectedStudents.stream().map(r -> r.getUser().getId()).toList();
        int totalNew = subjectDAO.enrollStudentsInAllSubjects(studentIds);

        User currUser = Session.getCurrentUser();
        String senderName = currUser != null ? currUser.getFullName() : "Administrator / Professor";
        String senderRole = currUser != null ? currUser.getRole() : "PROFESSOR";
        int senderId = currUser != null ? currUser.getId() : 0;

        for (StudentRow r : selectedStudents) {
            String title = "🎓 Enrolled in All Curriculum Subjects";
            String body = "Hello " + r.getUser().getFullName() + ",\n\n"
                    + "You have been officially enrolled into all curriculum subjects by " + senderName + ".\n\n"
                    + "You can now view your enrolled subjects overview on your dashboard and track all academic deadlines.\n\n"
                    + "— AcadsCatchUp Academic Management System";

            inboxDAO.sendMessage(
                    senderId, senderName, senderRole,
                    r.getUser().getId(), r.getUser().getFullName(),
                    title, body,
                    null, null, "ALL",
                    "ENROLLMENT"
            );
        }

        enrolledSuccessfully = true;
        CustomAlert.showInfo(stage.getOwner(), "Enrolled in All Subjects 🎉",
                "Successfully enrolled " + selectedStudents.size() + " student(s) into ALL subjects!\n(" + totalNew + " enrollment records updated).");
        loadStudents();
    }

    @FXML
    private void handleUnenroll() {
        Subject sub = subjectCombo.getValue();
        if (sub == null) return;

        List<StudentRow> selectedStudents = tableRows.stream().filter(StudentRow::isSelected).toList();
        if (selectedStudents.isEmpty()) {
            Stage stage = (Stage) btnEnroll.getScene().getWindow();
            CustomAlert.showWarning(stage.getOwner(), "No Students Selected",
                    "Please select at least one student to unenroll from " + sub.getName() + " (" + sub.getCode() + ").");
            return;
        }

        Stage stage = (Stage) btnEnroll.getScene().getWindow();
        boolean confirmed = CustomAlert.showConfirmation(stage.getOwner(),
                "Unenroll from " + sub.getCode(),
                "Are you sure you want to unenroll " + selectedStudents.size() + " student(s) from " + sub.getName() + " (" + sub.getCode() + ")?");

        if (!confirmed) return;

        int newlyUnenrolled = 0;
        for (StudentRow r : selectedStudents) {
            if (subjectDAO.unenrollStudent(r.getUser().getId(), sub.getId())) {
                newlyUnenrolled++;
            }
        }

        enrolledSuccessfully = true;
        CustomAlert.showInfo(stage.getOwner(), "Unenrolled Successfully 🎉",
                "Successfully unenrolled " + newlyUnenrolled + " student(s) from " + sub.getName() + " (" + sub.getCode() + ").");
        loadStudents();
    }

    @FXML
    private void handleUnenrollAllSubjects() {
        List<StudentRow> selectedStudents = tableRows.stream().filter(StudentRow::isSelected).toList();
        if (selectedStudents.isEmpty()) {
            Stage stage = (Stage) btnEnroll.getScene().getWindow();
            CustomAlert.showWarning(stage.getOwner(), "No Students Selected", "Please select at least one student to unenroll from all subjects.");
            return;
        }

        Stage stage = (Stage) btnEnroll.getScene().getWindow();
        boolean confirmed = CustomAlert.showConfirmation(stage.getOwner(),
                "Unenroll from All Subjects",
                "Are you sure you want to unenroll " + selectedStudents.size() + " selected student(s) from ALL subjects in the curriculum?");

        if (!confirmed) return;

        List<Integer> studentIds = selectedStudents.stream().map(r -> r.getUser().getId()).toList();
        int totalRemoved = subjectDAO.unenrollStudentsFromAllSubjects(studentIds);

        enrolledSuccessfully = true;
        CustomAlert.showInfo(stage.getOwner(), "Unenrolled from All Subjects",
                "Successfully unenrolled " + selectedStudents.size() + " student(s) from ALL subjects!\n(" + totalRemoved + " enrollment records removed).");
        loadStudents();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    public boolean isEnrolledSuccessfully() {
        return enrolledSuccessfully;
    }

    private void closeDialog() {
        com.acadscatchup.util.ModalOverlay.close(btnEnroll);
    }
}
