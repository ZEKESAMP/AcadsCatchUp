package com.acadscatchup.util;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Modern In-Window Modal Overlay System for AcadsCatchUp.
 * Renders dialogs, settings, forms, and alerts directly INSIDE the application window
 * (similar to Antigravity IDE, VS Code, and Discord), guaranteeing that popups are
 * 100% physically connected to the main window: they cannot float away, always move
 * and minimize together, and never desynchronize.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class ModalOverlay {

    public static final String DEVELOPER = "F4TAL";

    private static final String DISMISSER_KEY = "MODAL_OVERLAY_DISMISSER";

    /**
     * Displays a dialog node inside the main window scene as an in-app modal overlay,
     * pausing execution in a nested event loop until the dialog is closed (identical
     * to Stage.showAndWait() semantics, but fully embedded inside the parent window).
     */
    public static void showAndWait(Node anchorNode, Parent dialogContent, double preferredWidth, double preferredHeight) {
        if (dialogContent == null) return;

        // Auto-patch emojis for Linux compatibility across all modal dialogs
        OSCompat.patchEmojis(dialogContent);

        Scene scene = (anchorNode != null && anchorNode.getScene() != null)
                ? anchorNode.getScene()
                : (AppTrayManager.getCurrentStage() != null ? AppTrayManager.getCurrentStage().getScene() : null);

        if (scene == null) {
            System.err.println("[ModalOverlay] Error: Scene could not be resolved from anchorNode or AppTrayManager.");
            return;
        }

        StackPane stackRoot = ensureStackPaneRoot(scene);

        // ── Card container for dialog ──────────────────────────────────────
        StackPane card = new StackPane();
        card.setAlignment(Pos.CENTER);
        double maxW = Math.min(preferredWidth, Math.max(300, scene.getWidth() * 0.96));
        double maxH = Math.min(preferredHeight, Math.max(200, scene.getHeight() * 0.94));
        card.setMaxWidth(maxW);
        card.setMaxHeight(maxH);
        card.setPrefWidth(preferredWidth);
        card.setPrefHeight(preferredHeight);

        // Smoothly clip dialog content to card's rounded corners
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        dialogContent.setClip(clip);

        // Dynamically update card bounds if window size changes while modal is open
        javafx.beans.value.ChangeListener<Number> resizeListener = (obs, o, n) -> {
            double dynamicMaxW = Math.min(preferredWidth, Math.max(300, scene.getWidth() * 0.96));
            double dynamicMaxH = Math.min(preferredHeight, Math.max(200, scene.getHeight() * 0.94));
            card.setMaxWidth(dynamicMaxW);
            card.setMaxHeight(dynamicMaxH);
        };
        scene.widthProperty().addListener(resizeListener);
        scene.heightProperty().addListener(resizeListener);

        card.setStyle(
                "-fx-background-color: #0f1117; " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.78), 38, 0, 0, 10);"
        );
        card.getChildren().add(dialogContent);

        // ── Dark glass overlay backdrop ────────────────────────────────────
        StackPane overlayPane = new StackPane();
        overlayPane.setStyle("-fx-background-color: rgba(5, 7, 16, 0.76);");
        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.setPickOnBounds(true);
        overlayPane.getChildren().add(card);

        final Object loopKey = new Object();
        final boolean[] isClosed = new boolean[]{false};

        // Dismiss action with smooth exit animation
        Runnable dismissAction = () -> {
            if (isClosed[0]) return;
            isClosed[0] = true;

            scene.widthProperty().removeListener(resizeListener);
            scene.heightProperty().removeListener(resizeListener);

            FadeTransition ft = new FadeTransition(Duration.millis(120), overlayPane);
            ft.setToValue(0.0);
            ScaleTransition st = new ScaleTransition(Duration.millis(120), card);
            st.setToX(0.96);
            st.setToY(0.96);
            ParallelTransition pt = new ParallelTransition(ft, st);
            pt.setOnFinished(e -> {
                dialogContent.setClip(null);
                stackRoot.getChildren().remove(overlayPane);
                try {
                    Platform.exitNestedEventLoop(loopKey, null);
                } catch (Exception ignored) {}
            });
            pt.play();
        };

        // Attach dismisser to nodes for retrieval via close()
        dialogContent.getProperties().put(DISMISSER_KEY, dismissAction);
        card.getProperties().put(DISMISSER_KEY, dismissAction);
        overlayPane.getProperties().put(DISMISSER_KEY, dismissAction);

        // Clicking on the darkened backdrop outside the card dismisses the dialog
        overlayPane.setOnMouseClicked(event -> {
            if (event.getTarget() == overlayPane) {
                dismissAction.run();
                event.consume();
            }
        });

        // Pressing Escape dismisses the dialog
        overlayPane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dismissAction.run();
                event.consume();
            }
        });

        // Smooth entrance animation
        overlayPane.setOpacity(0.0);
        card.setScaleX(0.95);
        card.setScaleY(0.95);

        stackRoot.getChildren().add(overlayPane);

        FadeTransition ft = new FadeTransition(Duration.millis(160), overlayPane);
        ft.setToValue(1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
        st.setToX(1.0);
        st.setToY(1.0);
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();

        // Focus dialog card for immediate keyboard navigation / Esc handling
        card.setFocusTraversable(true);
        Platform.runLater(card::requestFocus);

        // Enter nested event loop: waits synchronously without freezing UI or thread
        Platform.enterNestedEventLoop(loopKey);
    }

    /**
     * Closes the active In-Window Modal Overlay that contains the given node.
     * Searches up the node parent hierarchy for the dismisser action.
     */
    public static void close(Node node) {
        if (node == null) return;

        Node cur = node;
        while (cur != null) {
            Object obj = cur.getProperties().get(DISMISSER_KEY);
            if (obj instanceof Runnable r) {
                r.run();
                return;
            }
            cur = cur.getParent();
        }

        // Fallback: If node was shown in a legacy Stage, close the stage
        if (node.getScene() != null && node.getScene().getWindow() instanceof Stage stage) {
            if (stage != AppTrayManager.getCurrentStage()) {
                stage.close();
            }
        }
    }

    /**
     * Checks if a node is part of an active modal overlay.
     */
    public static boolean isOverlayChild(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur.getProperties().containsKey(DISMISSER_KEY)) return true;
            cur = cur.getParent();
        }
        return false;
    }

    /**
     * Wraps the scene root in a StackPane if it isn't one already, allowing
     * overlay panes to layer over the existing view seamlessly.
     */
    private static StackPane ensureStackPaneRoot(Scene scene) {
        Parent root = scene.getRoot();
        if (root instanceof StackPane sp) {
            return sp;
        }

        StackPane wrapper = new StackPane();
        wrapper.getStylesheets().addAll(root.getStylesheets());
        wrapper.getStyleClass().addAll(root.getStyleClass());
        scene.setRoot(wrapper);
        wrapper.getChildren().add(root);
        return wrapper;
    }
}
