package com.acadscatchup.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Modern FAQ-styled modal alert dialog for AcadsCatchUp.
 * Directly owned by the parent window so it stays connected when minimized/restored.
 * Includes draggable header, close '✕' button, and keyboard Esc support.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class CustomAlert {

    public static final String DEVELOPER = "F4TAL";

    public enum Type {
        INFORMATION, WARNING, ERROR, CONFIRMATION
    }

    public enum ExitChoice {
        MINIMIZE_TO_TRAY,
        EXIT_PROGRAM,
        CANCEL
    }

    private static double xOffset = 0;
    private static double yOffset = 0;

    /**
     * Shows an Information message dialog styled like the FAQ modal.
     */
    public static void showInfo(Window owner, String title, String message) {
        showAlert(owner, Type.INFORMATION, title, message);
    }

    /**
     * Shows a Warning message dialog styled like the FAQ modal.
     */
    public static void showWarning(Window owner, String title, String message) {
        showAlert(owner, Type.WARNING, title, message);
    }

    /**
     * Shows an Error message dialog styled like the FAQ modal.
     */
    public static void showError(Window owner, String title, String message) {
        showAlert(owner, Type.ERROR, title, message);
    }

    /**
     * Shows a Confirmation dialog with Yes / Cancel buttons. Returns true if user clicked Yes.
     */
    public static boolean showConfirmation(Window owner, String title, String message) {
        final boolean[] result = new boolean[]{false};

        Stage dialog = new Stage();
        WindowUtil.setupModalDialog(dialog, owner, 460, 200);

        VBox root = new VBox(0);
        root.setStyle("-fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-background-color: #0f1117;");
        root.getStylesheets().add(CustomAlert.class.getResource("/com/acadscatchup/css/style.css").toExternalForm());

        // Header
        HBox header = buildHeader(dialog, title != null ? title : "Confirmation");

        // Body
        HBox body = new HBox(16);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setStyle("-fx-padding: 22 24 16 24;");

        Label iconLbl = new Label("❓");
        iconLbl.setStyle("-fx-font-size: 28px; -fx-padding: 0 4 0 0;");

        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #e2e8f0;");
        HBox.setHgrow(msgLbl, Priority.ALWAYS);

        body.getChildren().addAll(iconLbl, msgLbl);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 12 20; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> {
            result[0] = false;
            dialog.close();
        });

        Button yesBtn = new Button("Yes, Confirm");
        yesBtn.getStyleClass().add("btn-primary");
        yesBtn.setOnAction(e -> {
            result[0] = true;
            dialog.close();
        });

        footer.getChildren().addAll(cancelBtn, yesBtn);
        root.getChildren().addAll(header, body, footer);

        Node anchor = (owner != null && owner.getScene() != null) ? owner.getScene().getRoot() : (AppTrayManager.getCurrentStage() != null && AppTrayManager.getCurrentStage().getScene() != null ? AppTrayManager.getCurrentStage().getScene().getRoot() : null);
        if (anchor != null) {
            cancelBtn.setOnAction(e -> {
                result[0] = false;
                ModalOverlay.close(cancelBtn);
            });
            yesBtn.setOnAction(e -> {
                result[0] = true;
                ModalOverlay.close(yesBtn);
            });
            ModalOverlay.showAndWait(anchor, root, 460, 200);
            return result[0];
        }

        Scene scene = new Scene(root, 460, 200);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                result[0] = false;
                dialog.close();
            }
        });

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();

        return result[0];
    }

    /**
     * Shows a customized FAQ-styled exit modal asking whether to minimize to system tray or exit completely.
     */
    public static ExitChoice showExitDialog(Window owner) {
        final ExitChoice[] result = new ExitChoice[]{ExitChoice.CANCEL};

        Stage dialog = new Stage();
        WindowUtil.setupModalDialog(dialog, owner, 530, 220);

        VBox root = new VBox(0);
        root.setStyle("-fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-background-color: #0f1117;");
        root.getStylesheets().add(CustomAlert.class.getResource("/com/acadscatchup/css/style.css").toExternalForm());

        // Header with Draggable bar, icon, title, subtitle, and close button
        HBox header = buildHeader(dialog, "Exit AcadsCatchUp");

        // Body
        HBox body = new HBox(16);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setStyle("-fx-padding: 22 24 16 24;");

        Label iconLbl = new Label("🚪");
        iconLbl.setStyle("-fx-font-size: 32px; -fx-padding: 0 4 0 0;");

        VBox textBox = new VBox(6);
        Label mainLbl = new Label("What would you like to do?");
        mainLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label descLbl = new Label("Choose \"Minimize to Tray\" to keep AcadsCatchUp running in the background for notifications, or \"Exit Program\" to close completely.");
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        textBox.getChildren().addAll(mainLbl, descLbl);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        body.getChildren().addAll(iconLbl, textBox);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 14 20; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> {
            result[0] = ExitChoice.CANCEL;
            dialog.close();
        });

        Button trayBtn = new Button("📥 Minimize to Tray");
        trayBtn.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #60a5fa; -fx-border-color: #3b82f6; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 7 14; -fx-cursor: hand;");
        trayBtn.setOnAction(e -> {
            result[0] = ExitChoice.MINIMIZE_TO_TRAY;
            dialog.close();
        });

        Button exitBtn = new Button("❌ Exit Program");
        exitBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: #ffffff; -fx-border-color: #b91c1c; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 7 14; -fx-cursor: hand;");
        exitBtn.setOnAction(e -> {
            result[0] = ExitChoice.EXIT_PROGRAM;
            dialog.close();
        });

        footer.getChildren().addAll(cancelBtn, trayBtn, exitBtn);
        root.getChildren().addAll(header, body, footer);

        Node anchor = (owner != null && owner.getScene() != null) ? owner.getScene().getRoot() : (AppTrayManager.getCurrentStage() != null && AppTrayManager.getCurrentStage().getScene() != null ? AppTrayManager.getCurrentStage().getScene().getRoot() : null);
        if (anchor != null) {
            cancelBtn.setOnAction(e -> {
                result[0] = ExitChoice.CANCEL;
                ModalOverlay.close(cancelBtn);
            });
            trayBtn.setOnAction(e -> {
                result[0] = ExitChoice.MINIMIZE_TO_TRAY;
                ModalOverlay.close(trayBtn);
            });
            exitBtn.setOnAction(e -> {
                result[0] = ExitChoice.EXIT_PROGRAM;
                ModalOverlay.close(exitBtn);
            });
            ModalOverlay.showAndWait(anchor, root, 530, 220);
            return result[0];
        }

        Scene scene = new Scene(root, 530, 220);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                result[0] = ExitChoice.CANCEL;
                dialog.close();
            }
        });

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();

        return result[0];
    }

    private static void showAlert(Window owner, Type type, String title, String message) {
        Stage dialog = new Stage();
        WindowUtil.setupModalDialog(dialog, owner, 460, 200);

        VBox root = new VBox(0);
        root.setStyle("-fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-background-color: #0f1117;");
        root.getStylesheets().add(CustomAlert.class.getResource("/com/acadscatchup/css/style.css").toExternalForm());

        String defaultTitle = switch (type) {
            case INFORMATION -> "Information";
            case WARNING -> "Warning";
            case ERROR -> "Error";
            default -> "Message";
        };

        // Header
        HBox header = buildHeader(dialog, title != null ? title : defaultTitle);

        // Body
        HBox body = new HBox(16);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setStyle("-fx-padding: 22 24 16 24;");

        String iconEmoji = switch (type) {
            case INFORMATION -> "ℹ️";
            case WARNING -> "⚠️";
            case ERROR -> "❌";
            default -> "🔔";
        };

        Label iconLbl = new Label(iconEmoji);
        iconLbl.setStyle("-fx-font-size: 28px; -fx-padding: 0 4 0 0;");

        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #e2e8f0;");
        HBox.setHgrow(msgLbl, Priority.ALWAYS);

        body.getChildren().addAll(iconLbl, msgLbl);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 12 20; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setMinWidth(80);

        footer.getChildren().add(okBtn);
        root.getChildren().addAll(header, body, footer);

        Node anchorAlert = (owner != null && owner.getScene() != null) ? owner.getScene().getRoot() : (AppTrayManager.getCurrentStage() != null && AppTrayManager.getCurrentStage().getScene() != null ? AppTrayManager.getCurrentStage().getScene().getRoot() : null);
        if (anchorAlert != null) {
            okBtn.setOnAction(e -> ModalOverlay.close(okBtn));
            ModalOverlay.showAndWait(anchorAlert, root, 460, 200);
            return;
        }

        okBtn.setOnAction(e -> dialog.close());

        Scene scene = new Scene(root, 460, 200);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) {
                dialog.close();
            }
        });

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private static HBox buildHeader(Stage dialog, String title) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-header");
        header.setStyle("-fx-cursor: move; -fx-padding: 12 16;");

        // Dragging logic
        header.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        header.setOnMouseDragged(e -> {
            dialog.setX(e.getScreenX() - xOffset);
            dialog.setY(e.getScreenY() - yOffset);
        });

        try {
            ImageView iv = new ImageView(new Image(
                    CustomAlert.class.getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")));
            iv.setFitWidth(22);
            iv.setFitHeight(22);
            iv.setPreserveRatio(true);
            header.getChildren().add(iv);
        } catch (Exception ignored) {}

        VBox titleBox = new VBox(1);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label subLbl = new Label("AcadsCatchUp");
        subLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        titleBox.getChildren().addAll(titleLbl, subLbl);
        header.getChildren().add(titleBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        return header;
    }

}
