package com.acadscatchup.util;

import javafx.application.Platform;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Discord-style System Tray Manager for AcadsCatchUp.
 * Provides permanent Windows taskbar tray icon with right-click context menu (Open / Exit)
 * and click-to-restore behavior.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AppTrayManager {

    public static final String DEVELOPER = "F4TAL";

    private static TrayIcon trayIcon = null;
    private static Stage currentStage = null;
    private static boolean initialized = false;
    private static boolean minimizeNoticeShown = false;

    /**
     * Listener that intercepts window minimization:
     * Hides the window from the screen and taskbar ("closes the tab")
     * and minimizes it into the system tray.
     */
    private static final javafx.beans.value.ChangeListener<Boolean> MINIMIZE_TO_TRAY_LISTENER =
            (obs, wasIconified, isIconified) -> {
                if (isIconified && currentStage != null && OSCompat.isSystemTraySupported()) {
                    Platform.runLater(() -> {
                        if (currentStage != null) {
                            currentStage.setIconified(false);
                            currentStage.hide(); // Closes the taskbar tab and hides the window
                            if (!minimizeNoticeShown) {
                                displayMessage(
                                        "AcadsCatchUp Minimized to Tray",
                                        "AcadsCatchUp is running in the background. Click this tray icon to reopen.",
                                        TrayIcon.MessageType.INFO
                                );
                                minimizeNoticeShown = true;
                            }
                        }
                    });
                }
            };

    /**
     * Updates the reference to the currently active application window
     * and hooks the minimize-to-tray listener.
     */
    public static void setCurrentStage(Stage stage) {
        if (currentStage != null && currentStage != stage) {
            currentStage.iconifiedProperty().removeListener(MINIMIZE_TO_TRAY_LISTENER);
        }
        currentStage = stage;
        if (currentStage != null) {
            currentStage.iconifiedProperty().removeListener(MINIMIZE_TO_TRAY_LISTENER);
            currentStage.iconifiedProperty().addListener(MINIMIZE_TO_TRAY_LISTENER);
        }
    }

    /**
     * Gets the currently active application window stage.
     */
    public static Stage getCurrentStage() {
        return currentStage;
    }

    /**
     * Initializes the system tray icon with Discord-style menu.
     */
    public static synchronized void initTray(Stage stage) {
        setCurrentStage(stage);
        if (initialized || !OSCompat.isSystemTraySupported()) return;

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Load app logo
            Image image = null;
            try (InputStream is = AppTrayManager.class.getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")) {
                if (is != null) {
                    image = ImageIO.read(is);
                }
            } catch (Exception ignored) {}

            if (image == null) {
                image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            }

            // Right-click Popup Menu (Discord-style)
            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Open AcadsCatchUp");
            Font boldFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
            showItem.setFont(boldFont);
            showItem.addActionListener(e -> showApplication());

            MenuItem exitItem = new MenuItem("Exit AcadsCatchUp");
            exitItem.addActionListener(e -> exitApplication());

            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "AcadsCatchUp", popup);
            trayIcon.setImageAutoSize(true);

            // Strictly LEFT-click to open application window.
            // DO NOT use addActionListener() because on Windows AWT it also triggers on right-click/popup dismissal.
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && !e.isPopupTrigger()) {
                        showApplication();
                    }
                }
            });

            tray.add(trayIcon);
            initialized = true;

            // Keep JavaFX application alive even if window is minimized or hidden
            Platform.setImplicitExit(false);

        } catch (Exception e) {
            System.err.println("[F4TAL] Tray initialization error: " + e.getMessage());
        }
    }

    /**
     * Restores and brings the main window to the front.
     */
    public static void showApplication() {
        Platform.runLater(() -> {
            if (currentStage != null) {
                if (!currentStage.isShowing()) {
                    currentStage.show();
                }
                if (currentStage.isIconified()) {
                    currentStage.setIconified(false);
                }
                currentStage.toFront();
                currentStage.requestFocus();
            }
        });
    }

    /**
     * Completely shuts down the application and removes the tray icon.
     */
    public static void exitApplication() {
        if (trayIcon != null && OSCompat.isSystemTraySupported()) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception ignored) {}
        }
        Platform.exit();
        System.exit(0);
    }

    /**
     * Handles window close request (X button) with FAQ-styled modal dialog.
     * Offers choice to minimize to system tray, exit completely, or cancel.
     */
    public static void handleCloseRequest(Stage stage) {
        if (stage == null) {
            exitApplication();
            return;
        }
        CustomAlert.ExitChoice choice = CustomAlert.showExitDialog(stage);
        if (choice == CustomAlert.ExitChoice.MINIMIZE_TO_TRAY) {
            currentStage = stage;
            stage.hide(); // Hides the window and removes the tab from the Windows taskbar
            displayMessage(
                    "AcadsCatchUp Minimized to Tray",
                    "AcadsCatchUp is running in the background. Click or right-click this icon to reopen.",
                    TrayIcon.MessageType.INFO
            );
        } else if (choice == CustomAlert.ExitChoice.EXIT_PROGRAM) {
            exitApplication();
        }
        // If CANCEL, do nothing (window stays open)
    }

    /**
     * Displays a native OS balloon/toast notification from the tray icon.
     */
    public static void displayMessage(String title, String message, TrayIcon.MessageType type) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, type);
        }
    }

    public static TrayIcon getTrayIcon() {
        return trayIcon;
    }
}
