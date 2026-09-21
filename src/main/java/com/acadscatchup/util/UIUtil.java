package com.acadscatchup.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;

/**
 * UIUtil — Centralized UI Helper & Presentation Utilities.
 * Standardizes common UI flows across dashboards:
 *  - Safe, synchronized logout procedure
 *  - Reusable status badge table cell factory
 *  - Standardized live synchronization status badge styling
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class UIUtil {

    public static final String DEVELOPER = "F4TAL";

    private UIUtil() {}

    /**
     * Executes a clean, thread-safe user logout:
     * 1. Shuts down the background LiveSync daemon if running.
     * 2. Clears user session and credential tokens.
     * 3. Transitions the current stage back to the centralized Login screen.
     * 4. Centers the stage, installs tray close handlers, and applies default window constraints.
     *
     * @param stage           The active dashboard Stage to transition
     * @param liveSyncService The background LiveSyncService instance to terminate
     */
    public static void performLogout(Stage stage, LiveSyncService liveSyncService) {
        if (liveSyncService != null) {
            liveSyncService.shutdown();
        }

        Session.clear();

        try {
            FXMLLoader loader = new FXMLLoader(UIUtil.class.getResource("/com/acadscatchup/fxml/login.fxml"));
            Parent root = loader.load();

            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.setMinWidth(480);
                stage.setMinHeight(580);
                stage.setTitle("AcadsCatchUp — Login");
                WindowUtil.initFullScreenWithCentering(stage, 540, 720);
                AppTrayManager.setCurrentStage(stage);
                stage.setOnCloseRequest(e -> {
                    e.consume();
                    AppTrayManager.handleCloseRequest(stage);
                });
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("[UIUtil] Failed to navigate to login screen: " + e.getMessage());
            if (stage != null) {
                CustomAlert.showError(stage, "Logout Error", "Could not return to login screen: " + e.getMessage());
            }
        }
    }

    /**
     * Creates a standardized, color-coded status badge TableCell factory
     * for academic deficiency items (PENDING, SUBMITTED, GRADED, RESOLVED, REJECTED).
     */
    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> createStatusCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(status);
                    String baseStyle = "-fx-alignment: CENTER; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; ";
                    switch (status.toUpperCase()) {
                        case "PENDING" -> setStyle(baseStyle + "-fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.15);");
                        case "SUBMITTED" -> setStyle(baseStyle + "-fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.15);");
                        case "GRADED" -> setStyle(baseStyle + "-fx-text-fill: #34d399; -fx-background-color: rgba(52, 211, 153, 0.15);");
                        case "RESOLVED" -> setStyle(baseStyle + "-fx-text-fill: #10b981; -fx-background-color: rgba(16, 185, 129, 0.15);");
                        case "REJECTED" -> setStyle(baseStyle + "-fx-text-fill: #ef4444; -fx-background-color: rgba(239, 68, 68, 0.15);");
                        default -> setStyle(baseStyle + "-fx-text-fill: #94a3b8; -fx-background-color: rgba(148, 163, 184, 0.12);");
                    }
                }
            }
        };
    }

    /**
     * Updates a live sync badge Label with appropriate color coding and status label text.
     */
    public static void updateSyncBadge(Label syncStatusLabel, LiveSyncService.SyncStatus status) {
        if (syncStatusLabel == null || status == null) return;
        Platform.runLater(() -> {
            syncStatusLabel.setText(status.label);
            syncStatusLabel.setStyle(
                    "-fx-background-color: " + status.bgColor + ";" +
                    "-fx-text-fill: " + status.textColor + ";" +
                    "-fx-background-radius: 12;" +
                    "-fx-padding: 3 10 3 10;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;"
            );
        });
    }
}
