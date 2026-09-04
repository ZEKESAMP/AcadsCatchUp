package com.acadscatchup.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Full-screen semi-transparent loading overlay for AcadsCatchUp.
 * Displays a spinning arc indicator with customizable status text.
 * Prevents user interaction with the underlying UI while an operation is in progress.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class LoadingOverlay {

    public static final String DEVELOPER = "F4TAL";

    private final StackPane overlay;
    private final Label statusLabel;
    private final RotateTransition spinner;
    private Pane parentContainer;

    /**
     * Creates a new LoadingOverlay attached to the given parent pane.
     * If the parent is not a StackPane, the overlay will wrap the scene root
     * in a StackPane so it can layer on top of any layout.
     *
     * @param parent the Pane to overlay (can be BorderPane, VBox, etc.)
     */
    public LoadingOverlay(Pane parent) {
        this.parentContainer = parent;

        overlay = new StackPane();
        overlay.setStyle(
                "-fx-background-color: rgba(8, 10, 18, 0.82);"
        );
        overlay.setAlignment(Pos.CENTER);
        overlay.setMouseTransparent(false);
        overlay.setPickOnBounds(true); // block clicks through

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(320);
        card.setMaxHeight(180);
        card.setStyle(
                "-fx-background-color: rgba(20, 24, 40, 0.95); " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 14; " +
                "-fx-background-radius: 14; " +
                "-fx-padding: 32 40; " +
                "-fx-effect: dropshadow(gaussian, rgba(79, 70, 229, 0.25), 28, 0, 0, 6);"
        );

        // Spinning arc (pure JavaFX, no images)
        Arc arc = new Arc(0, 0, 22, 22, 45, 270);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web("#818cf8"));
        arc.setStrokeWidth(3.5);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);

        spinner = new RotateTransition(Duration.millis(900), arc);
        spinner.setByAngle(360);
        spinner.setCycleCount(RotateTransition.INDEFINITE);
        spinner.setInterpolator(javafx.animation.Interpolator.LINEAR);

        // Pulsing glow effect on the arc
        Timeline glowPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(arc.strokeProperty(), Color.web("#818cf8"))),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(arc.strokeProperty(), Color.web("#a78bfa"))),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(arc.strokeProperty(), Color.web("#818cf8")))
        );
        glowPulse.setCycleCount(Timeline.INDEFINITE);

        // Status text
        statusLabel = new Label("Loading...");
        statusLabel.setStyle(
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #c7d2fe; " +
                "-fx-font-family: 'Segoe UI', 'Inter', sans-serif;"
        );

        // Sub-label
        Label subLabel = new Label("Please wait");
        subLabel.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-text-fill: #64748b; " +
                "-fx-font-family: 'Segoe UI', sans-serif;"
        );

        card.getChildren().addAll(arc, statusLabel, subLabel);
        overlay.getChildren().add(card);

        // Auto-start animations when overlay becomes visible
        overlay.visibleProperty().addListener((obs, wasVis, isVis) -> {
            if (isVis) {
                spinner.play();
                glowPulse.play();
            } else {
                spinner.stop();
                glowPulse.stop();
            }
        });

        overlay.setVisible(false);
        overlay.setManaged(false);
    }

    /**
     * Ensures the parentContainer is a StackPane so the overlay can layer on top.
     * If the scene root is a BorderPane or other non-StackPane, wraps it in a StackPane.
     */
    private void ensureStackPaneRoot() {
        if (parentContainer instanceof StackPane) return;

        // Check if we can wrap the scene root
        if (parentContainer.getScene() != null) {
            javafx.scene.Parent sceneRoot = parentContainer.getScene().getRoot();
            if (sceneRoot instanceof StackPane sp) {
                parentContainer = sp;
                return;
            }
            // Wrap the existing scene root in a StackPane
            StackPane wrapper = new StackPane();
            wrapper.getStylesheets().addAll(sceneRoot.getStylesheets());
            wrapper.getStyleClass().addAll(sceneRoot.getStyleClass());
            parentContainer.getScene().setRoot(wrapper);
            wrapper.getChildren().add(sceneRoot);
            parentContainer = wrapper;
        }
    }

    /**
     * Shows the overlay with a custom status message.
     * Thread-safe: can be called from any thread.
     *
     * @param message the status text to display (e.g., "Logging in...")
     */
    public void show(String message) {
        Runnable action = () -> {
            ensureStackPaneRoot();
            statusLabel.setText(message != null ? message : "Loading...");
            if (!parentContainer.getChildren().contains(overlay)) {
                parentContainer.getChildren().add(overlay);
            }
            overlay.setVisible(true);
            overlay.setManaged(true);
            overlay.toFront();
        };
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * Updates the status label text without hiding/showing the overlay.
     *
     * @param message the new status message
     */
    public void updateMessage(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    /**
     * Hides and removes the overlay.
     * Thread-safe: can be called from any thread.
     */
    public void hide() {
        Runnable action = () -> {
            overlay.setVisible(false);
            overlay.setManaged(false);
            parentContainer.getChildren().remove(overlay);
        };
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * Returns whether the overlay is currently visible.
     */
    public boolean isShowing() {
        return overlay.isVisible();
    }

    // ── Convenience static methods ──────────────────────────────────────

    /**
     * Run a task on a background thread while showing a loading overlay on the given pane.
     * When the task completes, the overlay is hidden and onComplete runs on the FX thread.
     *
     * @param parent      the container pane to overlay
     * @param message     the loading message
     * @param task        the background work (runs on a new thread)
     * @param onComplete  callback on the FX thread after task finishes (may be null)
     */
    public static void runWithOverlay(Pane parent, String message, Runnable task, Runnable onComplete) {
        LoadingOverlay lo = new LoadingOverlay(parent);
        lo.show(message);
        new Thread(() -> {
            try {
                task.run();
            } finally {
                Platform.runLater(() -> {
                    lo.hide();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }
        }, "LoadingOverlay-Worker").start();
    }

    /**
     * Run a task that returns a result on a background thread with a loading overlay.
     *
     * @param parent     the container pane to overlay
     * @param message    the loading message
     * @param supplier   produces a result on a background thread
     * @param onResult   receives the result on the FX thread
     * @param <T>        result type
     */
    public static <T> void runWithResult(Pane parent, String message,
                                          java.util.function.Supplier<T> supplier,
                                          java.util.function.Consumer<T> onResult) {
        LoadingOverlay lo = new LoadingOverlay(parent);
        lo.show(message);
        new Thread(() -> {
            T result = supplier.get();
            Platform.runLater(() -> {
                lo.hide();
                if (onResult != null) {
                    onResult.accept(result);
                }
            });
        }, "LoadingOverlay-Worker").start();
    }
}
