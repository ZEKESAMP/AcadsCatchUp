package com.acadscatchup.controller;

import com.acadscatchup.dao.HelpReportDAO;
import com.acadscatchup.model.HelpReport;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for Admin Inbox where F4TAL manages submitted help and bug reports.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AdminInboxController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private javafx.scene.layout.HBox headerBar;
    @FXML private Label openCountBadge;
    @FXML private TableView<HelpReport> reportsTable;
    @FXML private TableColumn<HelpReport, Number> colId;
    @FXML private TableColumn<HelpReport, String> colUser;
    @FXML private TableColumn<HelpReport, String> colRole;
    @FXML private TableColumn<HelpReport, String> colTitle;
    @FXML private TableColumn<HelpReport, String> colDate;
    @FXML private TableColumn<HelpReport, String> colStatus;

    @FXML private Label detailSenderLabel;
    @FXML private TextArea detailMessageArea;
    @FXML private Button btnResolve;

    private final HelpReportDAO dao = new HelpReportDAO();
    private final ObservableList<HelpReport> tableData = FXCollections.observableArrayList();
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
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()));
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserName()));
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoleBadge()));
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));

        // Status column with colored badge
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status);
                if ("RESOLVED".equalsIgnoreCase(status)) {
                    badge.setStyle("-fx-background-color: rgba(16,185,129,0.2); -fx-text-fill: #34d399; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else {
                    badge.setStyle("-fx-background-color: rgba(239,68,68,0.2); -fx-text-fill: #f87171; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        // Listen for table selection to update message details
        reportsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                detailSenderLabel.setText("From: " + selected.getUserName() + " (" + selected.getRoleBadge() + ")  •  Sent: " + selected.getCreatedAt());
                detailMessageArea.setText(selected.getMessage());
                btnResolve.setText("RESOLVED".equalsIgnoreCase(selected.getStatus()) ? "↩ Reopen Report" : "✔ Mark as Resolved");
            } else {
                detailSenderLabel.setText("Select a report from the table above to view its contents.");
                detailMessageArea.clear();
                btnResolve.setText("✔ Mark as Resolved");
            }
        });

        reportsTable.setItems(tableData);
        loadReports();
    }

    private void loadReports() {
        List<HelpReport> list = dao.getAllReports();
        tableData.setAll(list);

        int openCount = dao.getOpenCount();
        openCountBadge.setText(openCount + " Open");
        if (openCount == 0) {
            openCountBadge.setStyle("-fx-background-color: rgba(16,185,129,0.2); -fx-text-fill: #34d399; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else {
            openCountBadge.setStyle("-fx-background-color: rgba(239,68,68,0.2); -fx-text-fill: #f87171; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 12px;");
        }

        if (!list.isEmpty() && reportsTable.getSelectionModel().getSelectedItem() == null) {
            reportsTable.getSelectionModel().select(0);
        }
    }

    @FXML
    private void handleToggleStatus() {
        HelpReport selected = reportsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String newStatus = "RESOLVED".equalsIgnoreCase(selected.getStatus()) ? "OPEN" : "RESOLVED";
        if (dao.updateStatus(selected.getId(), newStatus)) {
            selected.setStatus(newStatus);
            reportsTable.refresh();
            btnResolve.setText("RESOLVED".equalsIgnoreCase(newStatus) ? "↩ Reopen Report" : "✔ Mark as Resolved");
            openCountBadge.setText(dao.getOpenCount() + " Open");

            // Notify student or professor in their personal inbox
            if ("RESOLVED".equalsIgnoreCase(newStatus)) {
                new com.acadscatchup.dao.InboxDAO().sendMessage(
                        1,
                        "System Administrator",
                        "ADMIN",
                        selected.getUserId(),
                        selected.getUserName(),
                        "Bug Report Resolved: " + selected.getTitle(),
                        "Hello " + selected.getUserName() + ",\n\n" +
                        "Your reported issue has been addressed and marked as RESOLVED by the System Administrator.\n\n" +
                        "• Subject: " + selected.getTitle() + "\n" +
                        "• Reported on: " + selected.getCreatedAt() + "\n\n" +
                        "Thank you for helping keep AcadsCatchUp bug-free!",
                        null, null, null,
                        "REPORT_RESOLVED"
                );
            }
        }
    }

    @FXML
    private void handleDeleteReport() {
        HelpReport selected = reportsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean confirmed = com.acadscatchup.util.CustomAlert.showConfirmation(
                reportsTable.getScene().getWindow(),
                "Confirm Deletion",
                "Delete report #" + selected.getId() + " from " + selected.getUserName() + "?");
        if (confirmed) {
            if (dao.deleteReport(selected.getId())) {
                loadReports();
            }
        }
    }

    @FXML
    private void handleOpenPersonalInbox() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/acadscatchup/fxml/user_inbox.fxml"));
            javafx.scene.Parent root = loader.load();
            com.acadscatchup.util.ModalOverlay.showAndWait(reportsTable, root, 860, 640);
        } catch (java.io.IOException e) {
            com.acadscatchup.util.CustomAlert.showError(reportsTable.getScene().getWindow(), "Error", "Could not open Personal Inbox: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        com.acadscatchup.util.ModalOverlay.close(reportsTable);
    }
}
