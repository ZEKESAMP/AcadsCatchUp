package com.acadscatchup.util;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Utility for display resolution detection, guaranteed full-screen launch,
 * and automatic centering on restore across all application windows.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class WindowUtil {

    public static final String DEVELOPER = "F4TAL";

    private static final String TARGET_W_KEY = "RESTORE_TARGET_WIDTH";
    private static final String TARGET_H_KEY = "RESTORE_TARGET_HEIGHT";
    private static final String LISTENER_KEY = "RESTORE_LISTENER_ATTACHED";

    /**
     * Detects the primary screen resolution and usable workspace bounds
     * (accounting for Windows taskbar and system insets).
     */
    public static Rectangle2D getScreenWorkArea() {
        return Screen.getPrimary().getVisualBounds();
    }

    /**
     * Immediately expands the stage to 100% full screen based on detected screen resolution.
     */
    public static void setFullScreen(Stage stage) {
        if (stage == null) return;
        Rectangle2D bounds = getScreenWorkArea();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true);
    }

    /**
     * Makes the stage full-screen / maximized by default, and installs
     * an automatic resolution detector listener so that whenever the user clicks
     * the "Restore" / unmaximize square button, the window immediately snaps to
     * the exact center of the screen based on their display resolution.
     */
    public static void initFullScreenWithCentering(Stage stage, double preferredRestoreW, double preferredRestoreH) {
        if (stage == null) return;

        stage.getProperties().put(TARGET_W_KEY, preferredRestoreW);
        stage.getProperties().put(TARGET_H_KEY, preferredRestoreH);

        // Force 100% full screen immediately
        setFullScreen(stage);

        // Install restore listener once per Stage
        if (stage.getProperties().get(LISTENER_KEY) == null) {
            stage.getProperties().put(LISTENER_KEY, Boolean.TRUE);

            stage.maximizedProperty().addListener((obs, wasMax, isNowMax) -> {
                if (!isNowMax) {
                    // User unmaximized / restored the window: center directly on screen!
                    Platform.runLater(() -> {
                        Rectangle2D bounds = getScreenWorkArea();
                        Double prefW = (Double) stage.getProperties().get(TARGET_W_KEY);
                        Double prefH = (Double) stage.getProperties().get(TARGET_H_KEY);
                        double w = prefW != null ? prefW : bounds.getWidth() * 0.85;
                        double h = prefH != null ? prefH : bounds.getHeight() * 0.85;

                        // Ensure dimensions fit comfortably on screen
                        w = Math.min(w, bounds.getWidth() * 0.95);
                        h = Math.min(h, bounds.getHeight() * 0.92);

                        stage.setWidth(w);
                        stage.setHeight(h);

                        double x = bounds.getMinX() + (bounds.getWidth() - w) / 2.0;
                        double y = bounds.getMinY() + (bounds.getHeight() - h) / 2.0;

                        stage.setX(Math.max(bounds.getMinX(), x));
                        stage.setY(Math.max(bounds.getMinY(), y));
                    });
                }
            });
        }

        // Re-apply full screen asynchronously to ensure complete fullscreen coverage
        Platform.runLater(() -> setFullScreen(stage));
    }

    /**
     * Intelligently centers a modal dialog on the screen based on detected resolution.
     */
    public static void centerModalDialog(Stage dialog, double preferredWidth, double preferredHeight) {
        if (dialog == null) return;
        Rectangle2D bounds = getScreenWorkArea();
        double w = Math.min(preferredWidth, bounds.getWidth() * 0.95);
        double h = Math.min(preferredHeight, bounds.getHeight() * 0.92);
        dialog.setWidth(w);
        dialog.setHeight(h);
        double x = bounds.getMinX() + (bounds.getWidth() - w) / 2.0;
        double y = bounds.getMinY() + (bounds.getHeight() - h) / 2.0;
        dialog.setX(Math.max(bounds.getMinX(), x));
        dialog.setY(Math.max(bounds.getMinY(), y));
    }

    /**
     * Centers a modal dialog directly inside its parent/owner window.
     * If owner is not valid, falls back to centering on screen.
     */
    public static void centerOnOwner(Stage dialog, Window owner, double preferredWidth, double preferredHeight) {
        if (dialog == null) return;
        Rectangle2D screenBounds = getScreenWorkArea();
        double w = Math.min(preferredWidth, screenBounds.getWidth() * 0.95);
        double h = Math.min(preferredHeight, screenBounds.getHeight() * 0.92);
        dialog.setWidth(w);
        dialog.setHeight(h);

        if (owner != null && owner.isShowing() && owner.getWidth() > 150 && owner.getHeight() > 150) {
            double x = owner.getX() + (owner.getWidth() - w) / 2.0;
            double y = owner.getY() + (owner.getHeight() - h) / 2.0;
            // Clamp to screen bounds
            x = Math.max(screenBounds.getMinX(), Math.min(x, screenBounds.getMaxX() - w));
            y = Math.max(screenBounds.getMinY(), Math.min(y, screenBounds.getMaxY() - h));
            dialog.setX(x);
            dialog.setY(y);
        } else {
            centerModalDialog(dialog, preferredWidth, preferredHeight);
        }
    }

    /**
     * Connects and binds a modal dialog to its owner window so that:
     * 1. The dialog centers directly on top of the owner window.
     * 2. When the owner moves, the dialog moves with it.
     * 3. When the owner is minimized, the dialog is minimized/hidden with it.
     * 4. When the owner is restored/unminimized, the dialog is restored with it.
     * 5. When the owner is focused or Alt-Tabbed, the dialog stays in front.
     */
    public static void setupModalDialog(Stage dialog, Window owner, double preferredWidth, double preferredHeight) {
        if (dialog == null) return;
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(Modality.WINDOW_MODAL);

        if (owner == null) {
            owner = AppTrayManager.getCurrentStage();
        }
        if (owner != null) {
            dialog.initOwner(owner);
        }

        try {
            dialog.getIcons().add(new Image(
                    WindowUtil.class.getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")));
        } catch (Exception ignored) {}

        centerOnOwner(dialog, owner, preferredWidth, preferredHeight);

        // Re-center on shown once layout is fully realized
        final Window finalOwner = owner;
        dialog.setOnShown(e -> Platform.runLater(() -> centerOnOwner(dialog, finalOwner, preferredWidth, preferredHeight)));

        if (owner instanceof Stage ownerStage) {
            final double[] offset = new double[2];
            final boolean[] updatingFromOwner = new boolean[]{false};

            // Relative offset tracking
            Runnable calcOffset = () -> {
                if (!updatingFromOwner[0] && dialog.isShowing() && !ownerStage.isIconified()) {
                    offset[0] = dialog.getX() - ownerStage.getX();
                    offset[1] = dialog.getY() - ownerStage.getY();
                }
            };

            ChangeListener<Number> dialogPosListener = (obs, oldVal, newVal) -> calcOffset.run();
            dialog.xProperty().addListener(dialogPosListener);
            dialog.yProperty().addListener(dialogPosListener);

            // Follow owner window when moved or resized
            ChangeListener<Number> ownerMoveListener = (obs, oldVal, newVal) -> {
                if (dialog.isShowing() && !ownerStage.isIconified()) {
                    Platform.runLater(() -> {
                        if (dialog.isShowing() && !ownerStage.isIconified()) {
                            updatingFromOwner[0] = true;
                            try {
                                if (offset[0] == 0 && offset[1] == 0) {
                                    centerOnOwner(dialog, ownerStage, dialog.getWidth(), dialog.getHeight());
                                    offset[0] = dialog.getX() - ownerStage.getX();
                                    offset[1] = dialog.getY() - ownerStage.getY();
                                } else {
                                    Rectangle2D screenBounds = getScreenWorkArea();
                                    double targetX = ownerStage.getX() + offset[0];
                                    double targetY = ownerStage.getY() + offset[1];
                                    targetX = Math.max(screenBounds.getMinX(), Math.min(targetX, screenBounds.getMaxX() - dialog.getWidth()));
                                    targetY = Math.max(screenBounds.getMinY(), Math.min(targetY, screenBounds.getMaxY() - dialog.getHeight()));
                                    dialog.setX(targetX);
                                    dialog.setY(targetY);
                                }
                            } finally {
                                updatingFromOwner[0] = false;
                            }
                        }
                    });
                }
            };
            ownerStage.xProperty().addListener(ownerMoveListener);
            ownerStage.yProperty().addListener(ownerMoveListener);
            ownerStage.widthProperty().addListener(ownerMoveListener);
            ownerStage.heightProperty().addListener(ownerMoveListener);

            // Synchronize minimization and restoration
            ChangeListener<Boolean> iconifiedListener = (obs, wasIconified, isNowIconified) -> {
                if (isNowIconified) {
                    if (dialog.isShowing()) {
                        dialog.getProperties().put("WAS_SHOWING_WHEN_MINIMIZED", Boolean.TRUE);
                        Platform.runLater(dialog::hide);
                    }
                } else {
                    if (Boolean.TRUE.equals(dialog.getProperties().get("WAS_SHOWING_WHEN_MINIMIZED"))) {
                        dialog.getProperties().remove("WAS_SHOWING_WHEN_MINIMIZED");
                        Platform.runLater(() -> {
                            if (!dialog.isShowing()) {
                                centerOnOwner(dialog, ownerStage, preferredWidth, preferredHeight);
                                dialog.show();
                                dialog.toFront();
                            }
                        });
                    }
                }
            };
            ownerStage.iconifiedProperty().addListener(iconifiedListener);

            // Keep in front on focus / Alt-Tab
            ChangeListener<Boolean> focusListener = (obs, wasFocused, isNowFocused) -> {
                if (isNowFocused && dialog.isShowing()) {
                    Platform.runLater(dialog::toFront);
                }
            };
            ownerStage.focusedProperty().addListener(focusListener);

            // Safe cleanup when dialog closes (using showingProperty to guarantee execution)
            dialog.showingProperty().addListener(new ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean wasShowing, Boolean isNowShowing) {
                    if (!isNowShowing) {
                        ownerStage.xProperty().removeListener(ownerMoveListener);
                        ownerStage.yProperty().removeListener(ownerMoveListener);
                        ownerStage.widthProperty().removeListener(ownerMoveListener);
                        ownerStage.heightProperty().removeListener(ownerMoveListener);
                        ownerStage.iconifiedProperty().removeListener(iconifiedListener);
                        ownerStage.focusedProperty().removeListener(focusListener);
                        dialog.xProperty().removeListener(dialogPosListener);
                        dialog.yProperty().removeListener(dialogPosListener);
                        dialog.showingProperty().removeListener(this);
                    }
                }
            });
        }
    }
}
