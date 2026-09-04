package com.acadscatchup.installer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

/**
 * 100% Pure Java Uninstaller for AcadsCatchUp.
 * Invoked by the Windows 10 Control Panel ("Apps & features" / "Add or Remove Programs").
 * Cleans up Desktop/Start Menu shortcuts, removes Registry entries, and deletes
 * the application installation directory.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class Uninstaller extends Application {

    public static final String DEVELOPER = "F4TAL";
    public static final String APP_NAME = "AcadsCatchUp";

    private static boolean isSilent = false;

    public static void main(String[] args) {
        for (String arg : args) {
            if ("--silent".equalsIgnoreCase(arg) || "-s".equalsIgnoreCase(arg)) {
                isSilent = true;
            }
        }

        if (isSilent) {
            performUninstall();
            System.exit(0);
        } else {
            launch(args);
        }
    }

    @Override
    public void start(Stage stage) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Uninstall AcadsCatchUp");
        confirmAlert.setHeaderText("Remove AcadsCatchUp");
        confirmAlert.setContentText("Are you sure you want to completely remove AcadsCatchUp and all of its components from your computer?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performUninstall();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("AcadsCatchUp Uninstalled");
            info.setHeaderText("Uninstallation Complete");
            info.setContentText("AcadsCatchUp was successfully removed from your computer.");
            info.showAndWait();
        }

        Platform.exit();
        System.exit(0);
    }

    public static void performUninstall() {
        try {
            // 1. Remove Desktop Shortcut
            String desktop = System.getProperty("user.home") + "\\Desktop\\" + APP_NAME + ".lnk";
            File desktopFile = new File(desktop);
            if (desktopFile.exists()) {
                desktopFile.delete();
            }

            // 2. Remove Start Menu Shortcut
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                File startMenuFolder = new File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\" + APP_NAME);
                if (startMenuFolder.exists()) {
                    deleteRecursively(startMenuFolder);
                }
            }

            // 3. Remove Windows Control Panel Registry Key
            String regKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\" + APP_NAME;
            new ProcessBuilder("reg.exe", "delete", regKey, "/f").start().waitFor();

            // 4. Determine installation directory
            File installDir = getInstallDir();

            // 5. Spawn background self-cleanup to delete files once JVM process terminates
            if (installDir != null && installDir.exists()) {
                String cleanCmd = String.format("cmd.exe /c timeout /t 2 > nul & rmdir /s /q \"%s\"", installDir.getAbsolutePath());
                new ProcessBuilder("cmd.exe", "/c", cleanCmd).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File getInstallDir() {
        try {
            String path = Uninstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(path);
            if (jarFile.isFile() && jarFile.getParentFile() != null) {
                return jarFile.getParentFile();
            }
        } catch (Exception ignored) {}

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            File def = new File(localAppData, "Programs\\" + APP_NAME);
            if (def.exists()) return def;
        }
        return null;
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
