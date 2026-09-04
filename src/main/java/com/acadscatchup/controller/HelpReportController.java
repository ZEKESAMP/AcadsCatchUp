package com.acadscatchup.controller;

import com.acadscatchup.dao.HelpReportDAO;
import com.acadscatchup.model.User;
import com.acadscatchup.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controller for Help & Bug Report submission by Students and Professors.
 * Reports are sent to Admin (F4TAL)'s inbox.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class HelpReportController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private javafx.scene.layout.HBox headerBar;
    @FXML private Label reporterNameLabel;
    @FXML private Label reporterRoleLabel;
    @FXML private Label reportDateLabel;
    @FXML private TextField titleField;
    @FXML private TextArea messageArea;
    @FXML private Label statusLabel;
    @FXML private Button submitButton;

    private final HelpReportDAO helpReportDAO = new HelpReportDAO();
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
            reporterNameLabel.setText(user.getFullName());
            reporterRoleLabel.setText("PROFESSOR".equalsIgnoreCase(user.getRole()) ? "Professor" : "Student");
        } else {
            reporterNameLabel.setText("Anonymous");
            reporterRoleLabel.setText("Student");
        }

        reportDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    @FXML
    private void handleSubmit() {
        statusLabel.setText("");
        String title = titleField.getText().trim();
        String message = messageArea.getText().trim();

        if (title.isEmpty()) {
            statusLabel.setText("Please enter an issue subject or summary.");
            return;
        }

        if (message.isEmpty() || message.length() < 10) {
            statusLabel.setText("Please write a detailed explanation (at least 10 characters).");
            return;
        }

        User user = Session.getCurrentUser();
        int userId = user != null ? user.getId() : 1;
        String userName = user != null ? user.getFullName() : "Unknown User";
        String userRole = user != null ? user.getRole() : "STUDENT";

        submitButton.setDisable(true);
        javafx.scene.Parent root = submitButton.getScene().getRoot();
        if (root instanceof javafx.scene.layout.Pane p) {
            com.acadscatchup.util.LoadingOverlay.runWithResult(p, "Sending report...",
                    () -> helpReportDAO.submitReport(userId, userName, userRole, title, message),
                    success -> {
                        submitButton.setDisable(false);
                        if (success) {
                            com.acadscatchup.util.CustomAlert.showInfo(titleField.getScene().getWindow(),
                                    "Report Sent Successfully",
                                    "Your report has been sent directly to the System Administrator's Inbox!\n\nThank you for helping improve AcadsCatchUp.");
                            closeDialog();
                        } else {
                            statusLabel.setText("Failed to submit report. Please try again.");
                        }
                    });
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        com.acadscatchup.util.ModalOverlay.close(submitButton);
    }
}
