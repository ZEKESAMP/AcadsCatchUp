package com.acadscatchup.controller;

import com.acadscatchup.dao.SubjectDAO;
import com.acadscatchup.model.Subject;
import com.acadscatchup.util.CustomAlert;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller for the FAQ-styled Add New Subject dialog.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AddSubjectController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private HBox headerBar;
    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private Label errorLabel;

    private final SubjectDAO subjectDAO = new SubjectDAO();
    private boolean subjectAdded = false;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        if (headerBar != null) {
            headerBar.setOnMousePressed(e -> {
                xOffset = e.getSceneX();
                yOffset = e.getSceneY();
            });
            headerBar.setOnMouseDragged(e -> {
                Stage stage = (Stage) headerBar.getScene().getWindow();
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            });
        }
    }

    @FXML
    private void handleSave() {
        String code = (codeField.getText() != null) ? codeField.getText().trim().toUpperCase() : "";
        String name = (nameField.getText() != null) ? nameField.getText().trim() : "";

        if (code.isEmpty() || name.isEmpty()) {
            errorLabel.setText("Both subject code and name are required.");
            return;
        }

        boolean ok = subjectDAO.addSubject(new Subject(0, code, name));
        if (ok) {
            subjectAdded = true;
            javafx.stage.Window owner = codeField.getScene() != null ? codeField.getScene().getWindow() : null;
            com.acadscatchup.util.ModalOverlay.close(codeField);
            CustomAlert.showInfo(owner, "Subject Added",
                    "Subject " + code + " — " + name + " has been added successfully!\nAll students have been enrolled.");
        } else {
            errorLabel.setText("Failed to add subject. Code may already exist.");
        }
    }

    @FXML
    private void handleCancel() {
        com.acadscatchup.util.ModalOverlay.close(codeField);
    }

    public boolean isSubjectAdded() {
        return subjectAdded;
    }
}
