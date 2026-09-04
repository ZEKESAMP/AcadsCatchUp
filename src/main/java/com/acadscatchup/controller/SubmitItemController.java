package com.acadscatchup.controller;

import com.acadscatchup.dao.InboxDAO;
import com.acadscatchup.dao.MissedItemDAO;
import com.acadscatchup.dao.SubjectDAO;
import com.acadscatchup.dao.UserDAO;
import com.acadscatchup.model.MissedItem;
import com.acadscatchup.model.User;
import com.acadscatchup.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for students to submit a deficiency directly to a dedicated professor.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class SubmitItemController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private HBox headerBar;
    @FXML private Label itemTypeBadge;
    @FXML private Label itemNameLabel;
    @FXML private Label itemSubjectLabel;
    @FXML private ComboBox<User> profCombo;
    @FXML private TextArea notesArea;
    @FXML private Label statusLabel;
    @FXML private Button submitBtn;

    @FXML private CheckBox chkLink;
    @FXML private CheckBox chkFile;
    @FXML private javafx.scene.layout.VBox linkInputBox;
    @FXML private TextField linkField;
    @FXML private javafx.scene.layout.VBox fileInputBox;
    @FXML private Button btnChooseFile;
    @FXML private Label selectedFileNameLabel;
    @FXML private Button btnClearFile;

    private java.io.File chosenFile = null;

    private MissedItem item;
    private boolean submitted = false;
    private final UserDAO userDAO = new UserDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final MissedItemDAO missedItemDAO = new MissedItemDAO();
    private final InboxDAO inboxDAO = new InboxDAO();

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

        if (chkLink != null) {
            chkLink.selectedProperty().addListener((obs, oldV, isSelected) -> {
                if (linkInputBox != null) {
                    linkInputBox.setVisible(isSelected);
                    linkInputBox.setManaged(isSelected);
                }
                statusLabel.setText("");
            });
        }

        if (chkFile != null) {
            chkFile.selectedProperty().addListener((obs, oldV, isSelected) -> {
                if (fileInputBox != null) {
                    fileInputBox.setVisible(isSelected);
                    fileInputBox.setManaged(isSelected);
                }
                statusLabel.setText("");
            });
        }

        loadProfessors(null);
    }

    private void loadProfessors(MissedItem currentItem) {
        List<User> professors = new ArrayList<>();
        if (currentItem != null && currentItem.getSubjectId() > 0) {
            professors = subjectDAO.getProfessorsBySubject(currentItem.getSubjectId());
        }

        // If no professor is specifically assigned to this subject, fallback to all faculty
        if (professors.isEmpty()) {
            professors = userDAO.getAllProfessors();
        }

        // Strictly exclude any Administrator account or non-professor role
        List<User> strictlyProfessors = professors.stream()
                .filter(u -> "PROFESSOR".equalsIgnoreCase(u.getRole()))
                .toList();

        profCombo.setItems(FXCollections.observableArrayList(strictlyProfessors));
        profCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(User u) {
                if (u == null) return "";
                return u.getFullName() + " [Professor]";
            }
            @Override public User fromString(String string) { return null; }
        });

        // Pre-select dedicated professor if matched by createdBy
        if (currentItem != null && currentItem.getCreatedBy() > 0) {
            for (User p : profCombo.getItems()) {
                if (p.getId() == currentItem.getCreatedBy()) {
                    profCombo.setValue(p);
                    return;
                }
            }
        }

        // Otherwise fallback to first professor in list
        if (!profCombo.getItems().isEmpty()) {
            profCombo.setValue(profCombo.getItems().get(0));
        } else {
            profCombo.setValue(null);
        }
    }

    public void setItem(MissedItem item) {
        this.item = item;
        if (item != null) {
            itemTypeBadge.setText(item.getItemType());
            itemNameLabel.setText(item.getItemName());
            itemSubjectLabel.setText(item.getSubjectCode() + " - " + item.getSubjectName());
            if (item.getNotes() != null && !item.getNotes().isBlank()) {
                notesArea.setText(item.getNotes());
            }

            loadProfessors(item);
        }
    }

    @FXML
    private void handleChooseFile() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choose Submission File (Any Type)");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("All Files (*.*)", "*.*"),
                new javafx.stage.FileChooser.ExtensionFilter("Documents (*.pdf, *.docx, *.txt, *.xlsx, *.pptx)", "*.pdf", "*.docx", "*.doc", "*.txt", "*.xlsx", "*.pptx"),
                new javafx.stage.FileChooser.ExtensionFilter("Images (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"),
                new javafx.stage.FileChooser.ExtensionFilter("Archives (*.zip, *.rar, *.7z)", "*.zip", "*.rar", "*.7z"),
                new javafx.stage.FileChooser.ExtensionFilter("Source Code (*.java, *.py, *.c, *.cpp, *.html, *.js)", "*.java", "*.py", "*.c", "*.cpp", "*.html", "*.js")
        );
        Stage stage = (Stage) submitBtn.getScene().getWindow();
        java.io.File file = chooser.showOpenDialog(stage);
        if (file != null) {
            long sizeBytes = file.length();
            if (sizeBytes > 15 * 1024 * 1024) { // 15MB max limit
                statusLabel.setText("File is too large (maximum size is 15MB).");
                return;
            }
            chosenFile = file;
            String sizeStr = (sizeBytes < 1024) ? sizeBytes + " B"
                    : (sizeBytes < 1024 * 1024) ? (sizeBytes / 1024) + " KB"
                    : String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
            selectedFileNameLabel.setText(file.getName() + " (" + sizeStr + ")");
            selectedFileNameLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11.5px;");
            btnClearFile.setVisible(true);
            btnClearFile.setManaged(true);
            statusLabel.setText("");
        }
    }

    @FXML
    private void handleClearFile() {
        chosenFile = null;
        selectedFileNameLabel.setText("No file chosen (Any format supported)");
        selectedFileNameLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");
        btnClearFile.setVisible(false);
        btnClearFile.setManaged(false);
    }

    @FXML
    private void handleSubmit() {
        statusLabel.setText("");
        User selectedProf = profCombo.getValue();
        if (selectedProf == null) {
            statusLabel.setText("Please select a dedicated professor to submit to.");
            return;
        }

        if (item == null) {
            statusLabel.setText("Item not found.");
            return;
        }

        User currentStudent = Session.getCurrentUser();
        int studentId = currentStudent != null ? currentStudent.getId() : item.getStudentId();
        String studentName = currentStudent != null ? currentStudent.getFullName() : item.getStudentName();

        String attType = null;
        String attName = null;
        String attUrl = null;

        boolean linkChecked = (chkLink != null && chkLink.isSelected());
        boolean fileChecked = (chkFile != null && chkFile.isSelected());

        String linkUrl = null;
        if (linkChecked) {
            String link = (linkField.getText() != null) ? linkField.getText().trim() : "";
            if (link.isEmpty()) {
                statusLabel.setText("Please provide a valid submission URL / link.");
                return;
            }
            linkUrl = link;
        }

        if (fileChecked) {
            if (chosenFile == null || !chosenFile.exists()) {
                statusLabel.setText("Please select a file to upload.");
                return;
            }
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(chosenFile.toPath());
                attType = "FILE";
                attName = chosenFile.getName();
                attUrl = java.util.Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                statusLabel.setText("Error reading file: " + e.getMessage());
                return;
            }
        } else if (linkChecked) {
            attType = "LINK";
            attName = linkUrl;
            attUrl = linkUrl;
        }

        String notes = notesArea.getText().trim();
        // If student attached both a file and a link, append the link to notes
        if (fileChecked && linkUrl != null && !linkUrl.isEmpty()) {
            if (!notes.isEmpty()) {
                notes = notes + "\n• Submission Link: " + linkUrl;
            } else {
                notes = "• Submission Link: " + linkUrl;
            }
        }

        if (attType == null && (notes.isEmpty() || notes.length() < 5)) {
            statusLabel.setText("Please provide submission notes or attach a link / file.");
            return;
        }

        // 1. Update item status to SUBMITTED in database
        item.setStatus("SUBMITTED");
        if (!notes.isEmpty()) {
            item.setNotes(notes);
        }
        item.setAttachmentType(attType);
        item.setAttachmentName(attName);
        item.setAttachmentUrl(attUrl);

        boolean ok = missedItemDAO.update(item);

        if (ok) {
            // 2. Send submission message directly to chosen dedicated professor's inbox
            String attNotice = "";
            if ("FILE".equals(attType)) {
                attNotice = "\n• Attached File: " + attName;
                if (linkUrl != null && !linkUrl.isEmpty()) {
                    attNotice += "\n• Attached Link: " + linkUrl;
                }
            } else if ("LINK".equals(attType)) {
                attNotice = "\n• Attached Link: " + attName;
            }

            String msg = "Student " + studentName + " has submitted deficiency item:\n" +
                         "• Subject: " + item.getSubjectCode() + " - " + item.getSubjectName() + "\n" +
                         "• Item: " + item.getItemName() + " (" + item.getItemType() + ")\n" +
                         "• Date Missed: " + item.getDateMissed() + attNotice + "\n\n" +
                         "Student Note:\n" + (notes.isEmpty() ? "(No additional notes provided)" : notes);

            inboxDAO.sendMessage(
                    studentId,
                    studentName,
                    "STUDENT",
                    selectedProf.getId(),
                    selectedProf.getFullName(),
                    "Deficiency Submission: " + item.getItemName() + " (" + item.getSubjectCode() + ")",
                    msg,
                    item.getId(),
                    item.getItemName(),
                    item.getSubjectCode(),
                    "SUBMISSION",
                    attType,
                    attName,
                    attUrl
            );

            submitted = true;
            closeDialog();
        } else {
            statusLabel.setText("Failed to save submission. Please try again.");
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    public boolean isSubmitted() {
        return submitted;
    }

    private void closeDialog() {
        com.acadscatchup.util.ModalOverlay.close(submitBtn);
    }
}
