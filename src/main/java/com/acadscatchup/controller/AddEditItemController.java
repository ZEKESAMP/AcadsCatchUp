package com.acadscatchup.controller;

import com.acadscatchup.dao.MissedItemDAO;
import com.acadscatchup.model.MissedItem;
import com.acadscatchup.model.Subject;
import com.acadscatchup.model.User;
import com.acadscatchup.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for the Add/Edit Missed Item dialog.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AddEditItemController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private Label       dialogTitleLabel;
    @FXML private ComboBox<User>    studentCombo;
    @FXML private javafx.scene.layout.HBox headerBar;
    @FXML private ComboBox<Subject> subjectCombo;
    @FXML private ComboBox<String>  typeCombo;
    @FXML private ComboBox<String>  statusCombo;
    @FXML private TextField   itemNameField;
    @FXML private DatePicker  dateMissedPicker;
    @FXML private DatePicker  deadlinePicker;
    @FXML private TextArea    notesArea;
    @FXML private Label       errorLabel;
    @FXML private Button      saveButton;

    private MissedItem editingItem = null;
    private final MissedItemDAO dao = new MissedItemDAO();
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void initialize() {
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
        typeCombo.setItems(FXCollections.observableArrayList(
                "ACTIVITY", "QUIZ", "EXAM", "ASSIGNMENT"));
        typeCombo.setValue("ACTIVITY");

        statusCombo.setItems(FXCollections.observableArrayList(
                "PENDING", "SUBMITTED", "GRADED"));
        statusCombo.setValue("PENDING");

        subjectCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Subject s) {
                return s != null ? s.getName() + " (" + s.getCode() + ")" : "";
            }
            @Override public Subject fromString(String string) { return null; }
        });

        studentCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(User u) {
                if (u == null) return "";
                return u.getFullName() + (u.getProgram() != null ? " (" + u.getProgram() + " • " + u.getYearDisplay() + ")" : "");
            }
            @Override public User fromString(String string) { return null; }
        });

        // When professor changes subject, dynamically re-filter student list to students enrolled in that subject
        subjectCombo.valueProperty().addListener((obs, oldSub, newSub) -> {
            if (newSub != null) {
                List<User> enrolledStudents = new com.acadscatchup.dao.SubjectDAO().getStudentsBySubject(newSub.getId());
                User cur = studentCombo.getValue();
                studentCombo.setItems(FXCollections.observableArrayList(enrolledStudents));
                if (cur != null && enrolledStudents.stream().anyMatch(u -> u.getId() == cur.getId())) {
                    studentCombo.setValue(cur);
                } else if (!enrolledStudents.isEmpty()) {
                    studentCombo.setValue(enrolledStudents.get(0));
                } else {
                    studentCombo.setValue(null);
                }
            }
        });
    }

    /** Called by ProfDashboardController to pass data. */
    public void setStudents(List<User> students) {
        studentCombo.setItems(FXCollections.observableArrayList(students));
        if (!students.isEmpty() && studentCombo.getValue() == null) studentCombo.setValue(students.get(0));
    }

    public void setSubjects(List<Subject> subjects) {
        subjectCombo.setItems(FXCollections.observableArrayList(subjects));
        if (!subjects.isEmpty() && subjectCombo.getValue() == null) {
            subjectCombo.setValue(subjects.get(0));
        }
    }

    public void preselectSubject(Subject subject) {
        if (subject != null) {
            subjectCombo.getItems().stream()
                    .filter(s -> s.getId() == subject.getId())
                    .findFirst()
                    .ifPresent(subjectCombo::setValue);
        }
    }

    public void preselectStudent(int studentId) {
        studentCombo.getItems().stream()
                .filter(u -> u.getId() == studentId)
                .findFirst()
                .ifPresent(studentCombo::setValue);
    }

    /** When editing, pre-fill all fields from the existing item. */
    public void setItem(MissedItem item) {
        this.editingItem = item;
        if (item == null) {
            dialogTitleLabel.setText("Add Missed Item");
            saveButton.setText("＋ Add Missed Item");
            return;
        }

        dialogTitleLabel.setText("Edit Missed Item");
        saveButton.setText("💾 Save Changes");

        itemNameField   .setText(item.getItemName());
        dateMissedPicker.setValue(item.getDateMissed());
        deadlinePicker  .setValue(item.getDeadline());
        notesArea       .setText(item.getNotes());
        typeCombo       .setValue(item.getItemType());
        statusCombo     .setValue(item.getStatus());

        // Student and subject combos need to match by ID; wait until setStudents/setSubjects called
    }

    /** Pre-select by ID after combo items are loaded. */
    public void preselectByIds(int studentId, int subjectId) {
        subjectCombo.getItems().stream()
                .filter(s -> s.getId() == subjectId)
                .findFirst()
                .ifPresent(subjectCombo::setValue);
        studentCombo.getItems().stream()
                .filter(u -> u.getId() == studentId)
                .findFirst()
                .ifPresent(studentCombo::setValue);
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");

        // ── Validation ──────────────────────────────────────────────────
        if (studentCombo.getValue() == null) { errorLabel.setText("Please select a student."); return; }
        if (subjectCombo.getValue() == null)  { errorLabel.setText("Please select a subject."); return; }
        if (itemNameField.getText().isBlank()) { errorLabel.setText("Item name is required."); return; }
        if (dateMissedPicker.getValue() == null) { errorLabel.setText("Date Missed is required."); return; }
        if (deadlinePicker.getValue() != null && deadlinePicker.getValue().isBefore(dateMissedPicker.getValue())) {
            errorLabel.setText("Deadline cannot be earlier than Date Missed.");
            return;
        }

        // ── Build MissedItem ─────────────────────────────────────────────
        MissedItem item = (editingItem != null) ? editingItem : new MissedItem();
        item.setStudentId(studentCombo.getValue().getId());
        item.setSubjectId(subjectCombo.getValue().getId());
        item.setItemType(typeCombo.getValue());
        item.setItemName(itemNameField.getText().trim());
        item.setDateMissed(dateMissedPicker.getValue());
        item.setDeadline(deadlinePicker.getValue());
        item.setStatus(statusCombo.getValue());
        item.setNotes(notesArea.getText().isBlank() ? null : notesArea.getText().trim());
        item.setCreatedBy(Session.getCurrentUser() != null ? Session.getCurrentUser().getId() : 1);

        if (subjectCombo.getValue() != null) {
            item.setSubjectCode(subjectCombo.getValue().getCode());
            item.setSubjectName(subjectCombo.getValue().getName());
        }
        if (studentCombo.getValue() != null) {
            item.setStudentName(studentCombo.getValue().getFullName());
        }

        // ── Persist ──────────────────────────────────────────────────────
        boolean isNew = (editingItem == null);
        boolean success = isNew ? dao.insert(item) : dao.update(item);

        if (success) {
            if (isNew) {
                com.acadscatchup.dao.InboxDAO inboxDAO = new com.acadscatchup.dao.InboxDAO();
                com.acadscatchup.model.User currentUser = Session.getCurrentUser();
                int senderId = (currentUser != null) ? currentUser.getId() : 1;
                String senderName = (currentUser != null) ? currentUser.getFullName() : "Professor";
                String senderRole = (currentUser != null) ? currentUser.getRole() : "PROFESSOR";

                inboxDAO.sendMissedItemNotice(
                        studentCombo.getValue().getId(),
                        studentCombo.getValue().getFullName(),
                        senderId, senderName, senderRole,
                        item
                );
            }
            closeDialog();
        } else {
            errorLabel.setText("Failed to save. Please try again.");
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        com.acadscatchup.util.ModalOverlay.close(saveButton);
    }
}
