package com.acadscatchup.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 100% Pure Java & CSS In-App Floating Toast Notification.
 * Renders sleek notifications directly inside the JavaFX dashboard.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class InAppNotification {

    public static final String DEVELOPER = "F4TAL";

    public static void show(Pane container, String title, String message, boolean isWarning) {
        if (container == null) return;

        VBox toast = new VBox(4);
        toast.setMaxWidth(420);
        toast.setStyle(
                "-fx-background-color: " + (isWarning ? "rgba(30, 27, 75, 0.95)" : "rgba(15, 23, 42, 0.95)") + "; " +
                "-fx-border-color: " + (isWarning ? "#f59e0b" : "#3b82f6") + "; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 12 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 14, 0, 0, 4);"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(isWarning ? "⚠️" : "🔔");
        iconLbl.setStyle("-fx-font-size: 14px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + (isWarning ? "#fbbf24" : "#60a5fa") + ";"
        );

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; " +
                "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0 4;"
        );

        header.getChildren().addAll(iconLbl, titleLbl, spacer, closeBtn);

        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #e2e8f0; -fx-padding: 2 0 0 22;");

        toast.getChildren().addAll(header, msgLbl);

        closeBtn.setOnAction(e -> container.getChildren().remove(toast));

        container.getChildren().add(toast);

        // Fade in, wait 6 seconds, fade out and remove
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition stay = new PauseTransition(Duration.seconds(6));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> container.getChildren().remove(toast));

        SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
        seq.play();
    }
}
