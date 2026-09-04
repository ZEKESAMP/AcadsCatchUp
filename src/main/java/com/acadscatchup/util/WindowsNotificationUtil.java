package com.acadscatchup.util;

import com.acadscatchup.model.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * 100% Pure Java OS Notification Utility.
 * Uses Java Standard Library (java.desktop) SystemTray and TrayIcon.
 * Completely free of external scripts or non-Java processes.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class WindowsNotificationUtil {

    public static final String DEVELOPER = "F4TAL";

    private static TrayIcon trayIcon = null;
    private static boolean initialized = false;

    /**
     * Initializes the native SystemTray icon using pure Java.
     */
    public static synchronized void initTray() {
        if (initialized || !OSCompat.isSystemTraySupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = null;
            try (InputStream is = WindowsNotificationUtil.class.getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")) {
                if (is != null) image = ImageIO.read(is);
            } catch (Exception ignored) {}

            if (image == null) {
                image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            }

            trayIcon = new TrayIcon(image, "AcadsCatchUp");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            initialized = true;
        } catch (Exception e) {
            System.err.println("[F4TAL] SystemTray initialization: " + e.getMessage());
        }
    }

    /**
     * Displays a native OS notification using 100% Pure Java SystemTray.
     */
    public static void showNotification(String title, String message, TrayIcon.MessageType type) {
        new Thread(() -> {
            try {
                if (AppTrayManager.getTrayIcon() != null) {
                    AppTrayManager.displayMessage(title, message, type);
                    return;
                }
                if (!OSCompat.isSystemTraySupported()) return;
                initTray();
                if (trayIcon != null) {
                    trayIcon.displayMessage(title, message, type);
                }
            } catch (Exception e) {
                System.err.println("[F4TAL] Notification error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Triggers OS notification for a student regarding their missed items.
     */
    public static void notifyStudentDeficiencies(User student, int pendingCount, String nearestDeadline) {
        if (student == null) return;
        if (pendingCount > 0) {
            String deadLineInfo = (nearestDeadline != null && !nearestDeadline.isBlank())
                    ? " Next deadline: " + nearestDeadline + "."
                    : "";
            showNotification(
                    "AcadsCatchUp! • Deficiency Alert",
                    "Hi " + student.getFullName() + "! You have " + pendingCount + " pending missed activity/quiz." + deadLineInfo,
                    TrayIcon.MessageType.WARNING
            );
        } else {
            showNotification(
                    "AcadsCatchUp! • All Caught Up! 🎉",
                    "Great work, " + student.getFullName() + "! You have 0 pending missed activities.",
                    TrayIcon.MessageType.INFO
            );
        }
    }

    /**
     * Triggers OS notification for a professor regarding pending student submissions.
     */
    public static void notifyProfessorSubmissions(User prof, int pendingSubmissions) {
        if (prof == null || pendingSubmissions <= 0) return;
        showNotification(
                "AcadsCatchUp! • Student Submissions",
                "Hello " + prof.getFullName() + "! You have " + pendingSubmissions + " student deficiency submission(s) waiting in your Inbox.",
                TrayIcon.MessageType.INFO
        );
    }

    /**
     * Triggers OS notification for unread inbox messages.
     */
    public static void notifyUnreadInbox(int unreadCount) {
        if (unreadCount <= 0) return;
        showNotification(
                "AcadsCatchUp! • New Messages",
                "You have " + unreadCount + " unread message(s) in your AcadsCatchUp Inbox.",
                TrayIcon.MessageType.INFO
        );
    }
}
