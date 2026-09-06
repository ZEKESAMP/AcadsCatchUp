package com.acadscatchup.util;

import com.acadscatchup.Main;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discord-Style Startup Update Splash Screen for AcadsCatchUp.
 *
 * Algorithm:
 * 1. Check if there is a new update from version manifest on GitHub.
 * 2. If there is a new .jar, download it to the user's Downloads folder with live progress.
 * 3. Find the "AcadsCatchUp-Portable" folder and copy/replace the old .jar with the new .jar.
 * 4. After replacing the old .jar, NO restart is needed — seamlessly proceed to the Login phase.
 * 5. If there is no update, inform the user "No update" and immediately proceed to Login.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class UpdateSplash {

    public static final String DEVELOPER = "F4TAL";

    public static final String CURRENT_VERSION = "1.0.9";
    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/ZEKESAMP/AcadsCatchUp/main/version.json";

    private static double xOffset = 0;
    private static double yOffset = 0;

    /**
     * Displays the Discord-style splash screen and triggers the startup update check.
     */
    public static void showAndCheck(Stage primaryStage) {
        Stage splashStage = new Stage(StageStyle.TRANSPARENT);
        splashStage.setTitle("AcadsCatchUp");

        try {
            splashStage.getIcons().add(new Image(
                    UpdateSplash.class.getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")));
        } catch (Exception ignored) {}

        // ── Card Root ────────────────────────────────────────────────────────
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24, 28, 22, 28));
        card.setPrefSize(350, 290);
        card.setStyle(
                "-fx-background-color: #1e1f22; " +
                "-fx-background-radius: 16; " +
                "-fx-border-color: #2b2d31; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.65), 24, 0, 0, 6);"
        );

        // Allow dragging the frameless window
        card.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        card.setOnMouseDragged(e -> {
            splashStage.setX(e.getScreenX() - xOffset);
            splashStage.setY(e.getScreenY() - yOffset);
        });

        // ── Logo ─────────────────────────────────────────────────────────────
        ImageView logoView = new ImageView();
        try {
            Image img = new Image(UpdateSplash.class.getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png"), 64, 64, true, true);
            logoView.setImage(img);
        } catch (Exception ignored) {}
        logoView.setFitWidth(60);
        logoView.setFitHeight(60);

        // ── Brand Text ───────────────────────────────────────────────────────
        Label titleLabel = new Label("AcadsCatchUp");
        titleLabel.setStyle("-fx-text-fill: #f2f3f5; -fx-font-size: 19px; -fx-font-weight: 800; -fx-font-family: 'Segoe UI', sans-serif;");

        Label subtitleLabel = new Label("ACADEMIC DEFICIENCY TRACKING • v" + CURRENT_VERSION);
        subtitleLabel.setStyle("-fx-text-fill: #949ba4; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 0.8px;");

        VBox brandBox = new VBox(3, titleLabel, subtitleLabel);
        brandBox.setAlignment(Pos.CENTER);

        // ── Status & Progress Indicator ──────────────────────────────────────
        Label statusLabel = new Label("Checking for updates...");
        statusLabel.setStyle("-fx-text-fill: #dbdee1; -fx-font-size: 12.5px; -fx-font-weight: 600;");

        ProgressBar progressBar = new ProgressBar(-1); // Indeterminate pulse
        progressBar.setPrefWidth(280);
        progressBar.setPrefHeight(7);
        progressBar.setStyle("-fx-accent: #5865f2;"); // Discord Blurple accent

        Label detailLabel = new Label("Connecting to server...");
        detailLabel.setStyle("-fx-text-fill: #949ba4; -fx-font-size: 11px;");

        VBox progressBox = new VBox(6, statusLabel, progressBar, detailLabel);
        progressBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(logoView, brandBox, progressBox);

        Scene scene = new Scene(card);
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();

        // ── Background Update Check Thread ───────────────────────────────────
        Thread checkerThread = new Thread(() -> {
            try {
                // Short initial delay for smooth entrance animation
                Thread.sleep(300);

                RemoteUpdateInfo updateInfo = fetchLatestUpdate();
                if (updateInfo != null && updateInfo.version != null) {
                    System.out.println("[UpdateSplash] Current: v" + CURRENT_VERSION + " | Remote: v" + updateInfo.version);

                    if (isNewerVersion(updateInfo.version, CURRENT_VERSION)) {
                        // ── Update Available ──
                        Platform.runLater(() -> {
                            statusLabel.setText("⬇ Update found: v" + updateInfo.version);
                            detailLabel.setText("Downloading to Downloads folder...");
                            progressBar.setProgress(0);
                        });

                        downloadAndApplyUpdate(updateInfo.downloadUrl, updateInfo.version, progressBar, statusLabel, detailLabel, splashStage, primaryStage);
                        return;
                    } else {
                        // ── No Update (Already Up to Date) ──
                        Platform.runLater(() -> {
                            statusLabel.setText("✔ v" + CURRENT_VERSION + " is up to date");
                            statusLabel.setStyle("-fx-text-fill: #23a55a; -fx-font-size: 13px; -fx-font-weight: 700;");
                            progressBar.setProgress(1.0);
                            progressBar.setStyle("-fx-accent: #23a55a;");
                            detailLabel.setText("Launching AcadsCatchUp...");
                        });
                        Thread.sleep(600);
                    }
                } else {
                    handleOfflineMode(statusLabel, detailLabel);
                }
            } catch (Exception e) {
                handleOfflineMode(statusLabel, detailLabel);
            }

            // Launch Main Login Screen
            Platform.runLater(() -> {
                splashStage.close();
                try {
                    Main.showLoginScreen(primaryStage);
                } catch (Exception ex) {
                    System.err.println("[UpdateSplash] Error launching main window: " + ex.getMessage());
                }
            });
        }, "Discord-Splash-Checker");

        checkerThread.setDaemon(true);
        checkerThread.start();
    }

    private static void handleOfflineMode(Label statusLabel, Label detailLabel) {
        try {
            Platform.runLater(() -> {
                statusLabel.setText("⚠️ Offline — Skipping update check");
                statusLabel.setStyle("-fx-text-fill: #f0a500; -fx-font-size: 12.5px; -fx-font-weight: 700;");
                detailLabel.setText("No internet detected • Starting v" + CURRENT_VERSION + "...");
            });
            Thread.sleep(700);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Downloads the updated JAR file to a staging file, verifies integrity,
     * and executes a detached restart handoff to replace active application files cleanly.
     */
    private static void downloadAndApplyUpdate(String downloadUrl, String newVersion,
                                              ProgressBar progressBar, Label statusLabel, Label detailLabel,
                                              Stage splashStage, Stage primaryStage) {
        try {
            File portableDir = findPortableDirectory();
            if (portableDir == null || !portableDir.exists()) {
                portableDir = new File(".").getAbsoluteFile();
            }

            File stagingJar = new File(portableDir, "update_staging.jar");

            HttpURLConnection conn = openFollowRedirects(downloadUrl);
            int contentLength = conn.getContentLength();

            try (InputStream in = new BufferedInputStream(conn.getInputStream(), 32768);
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(stagingJar), 32768)) {

                byte[] buffer = new byte[32768];
                int bytesRead;
                long totalRead = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    if (contentLength > 0) {
                        double progress = (double) totalRead / contentLength;
                        long finalTotalRead = totalRead;
                        Platform.runLater(() -> {
                            progressBar.setProgress(progress);
                            double mbRead = finalTotalRead / (1024.0 * 1024.0);
                            double mbTotal = contentLength / (1024.0 * 1024.0);
                            detailLabel.setText(String.format("Downloading: %.1f MB / %.1f MB (%.0f%%)", mbRead, mbTotal, progress * 100));
                        });
                    }
                }
            }

            // ── Integrity Guard: reject partial/corrupt downloads ──────────────
            if ((contentLength > 0 && stagingJar.length() < contentLength * 0.95) || stagingJar.length() < 1000000) {
                System.err.println("[UpdateSplash] Downloaded JAR size mismatch — expected ~" + contentLength + " bytes, got " + stagingJar.length() + ". Aborting update.");
                if (stagingJar.exists()) stagingJar.delete();
                Platform.runLater(() -> {
                    statusLabel.setText("⚠️ Download incomplete — skipping update");
                    statusLabel.setStyle("-fx-text-fill: #f0a500; -fx-font-size: 12.5px; -fx-font-weight: 700;");
                    detailLabel.setText("File may be corrupted. Launching current version...");
                });
                Thread.sleep(1000);
                Platform.runLater(() -> {
                    splashStage.close();
                    try { Main.showLoginScreen(primaryStage); } catch (Exception ex) {
                        System.err.println("[UpdateSplash] Error launching main login: " + ex.getMessage());
                    }
                });
                return;
            }

            Platform.runLater(() -> {
                statusLabel.setText("✔ Update downloaded!");
                statusLabel.setStyle("-fx-text-fill: #23a55a; -fx-font-size: 13px; -fx-font-weight: bold;");
                detailLabel.setText("Restarting AcadsCatchUp to apply v" + newVersion + "...");
                progressBar.setProgress(1.0);
                progressBar.setStyle("-fx-accent: #23a55a;");
            });

            Thread.sleep(800);
            executeDetachedHandoff(stagingJar, portableDir);

        } catch (Exception e) {
            System.err.println("[UpdateSplash] Download/update failed: " + e.getMessage());
            Platform.runLater(() -> {
                statusLabel.setText("No update (Download failed)");
                detailLabel.setText("Launching current version...");
            });
            try { Thread.sleep(700); } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                splashStage.close();
                try {
                    Main.showLoginScreen(primaryStage);
                } catch (Exception ex) {
                    System.err.println("[UpdateSplash] Error launching main login: " + ex.getMessage());
                }
            });
        }
    }

    /**
     * Checks whether a directory is a valid AcadsCatchUp-Portable folder
     * using file anchor detection rather than strict directory naming.
     */
    public static boolean isPortableRoot(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return false;
        boolean hasAppDir = new File(dir, "app").isDirectory() && (
                new File(dir, "app/AcadsCatchUp.cfg").exists() ||
                new File(dir, "app/AcadsCatchUp.jar").exists() ||
                new File(dir, "app/acadscatchup-app.jar").exists());
        boolean hasRuntime = new File(dir, "runtime").isDirectory();
        boolean hasExe = new File(dir, "AcadsCatchUp.exe").isFile();

        // If it has app directory with configs/jars or runtime, it is definitively the portable root
        if (hasAppDir || hasRuntime) {
            return true;
        }

        // If it has both the exe and jar and portable indicator
        return hasExe && new File(dir, "AcadsCatchUp.jar").isFile() && dir.getName().toLowerCase().contains("portable");
    }

    /**
     * Locates the AcadsCatchUp-Portable folder by checking the execution environment,
     * runtime properties, and common filesystem locations.
     */
    public static File findPortableDirectory() {
        // 1. Check java.home (in jpackage, java.home is portableDir/runtime)
        try {
            String javaHome = System.getProperty("java.home");
            if (javaHome != null && !javaHome.isEmpty()) {
                File cur = new File(javaHome);
                while (cur != null) {
                    if (isPortableRoot(cur)) {
                        System.out.println("[UpdateSplash] Found portable directory via java.home: " + cur.getAbsolutePath());
                        return cur;
                    }
                    cur = cur.getParentFile();
                }
            }
        } catch (Exception ignored) {}

        // 2. Check running location from CodeSource JAR
        try {
            File codeSourceJar = new File(UpdateSplash.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File cur = codeSourceJar.getParentFile();
            while (cur != null) {
                File childPortable = new File(cur, "AcadsCatchUp-Portable");
                if (isPortableRoot(childPortable)) {
                    System.out.println("[UpdateSplash] Found portable directory in CodeSource child: " + childPortable.getAbsolutePath());
                    return childPortable;
                }
                if (isPortableRoot(cur)) {
                    System.out.println("[UpdateSplash] Found portable directory via CodeSource: " + cur.getAbsolutePath());
                    return cur;
                }
                cur = cur.getParentFile();
            }
        } catch (Exception ignored) {}

        // 3. Check current working directory
        try {
            File cwd = new File(".").getAbsoluteFile();
            File cur = cwd;
            while (cur != null) {
                File childPortable = new File(cur, "AcadsCatchUp-Portable");
                if (isPortableRoot(childPortable)) {
                    System.out.println("[UpdateSplash] Found portable directory in cwd child: " + childPortable.getAbsolutePath());
                    return childPortable;
                }
                File distChild = new File(cur, "dist/AcadsCatchUp-Portable");
                if (isPortableRoot(distChild)) {
                    System.out.println("[UpdateSplash] Found portable directory in cwd dist child: " + distChild.getAbsolutePath());
                    return distChild;
                }
                if (isPortableRoot(cur)) {
                    System.out.println("[UpdateSplash] Found portable directory via cwd: " + cur.getAbsolutePath());
                    return cur;
                }
                cur = cur.getParentFile();
            }
        } catch (Exception ignored) {}

        // 4. Search common base directories and their immediate subfolders
        String userHome = System.getProperty("user.home");
        String localAppData = System.getenv("LOCALAPPDATA");
        File[] baseCandidates = new File[] {
            new File("."),
            new File("./dist/AcadsCatchUp-Portable"),
            new File("./dist"),
            new File(userHome, "Downloads"),
            new File(userHome, "Desktop"),
            new File(userHome, "Documents"),
            new File(userHome, "Documents/AcadsCatchUp"),
            new File(userHome, "Documents/AcadsCatchUp/dist"),
            new File(userHome, "OneDrive/Desktop"),
            new File(userHome, "OneDrive/Documents"),
            localAppData != null ? new File(localAppData) : null
        };

        for (File base : baseCandidates) {
            if (base == null || !base.exists() || !base.isDirectory()) continue;

            if (isPortableRoot(base)) {
                System.out.println("[UpdateSplash] Found portable directory at base: " + base.getAbsolutePath());
                return base;
            }

            // Search immediate child directories
            File[] children = base.listFiles(File::isDirectory);
            if (children != null) {
                // First check children with "AcadsCatchUp" in name
                for (File child : children) {
                    if (child.getName().toLowerCase().contains("acadscatchup") && isPortableRoot(child)) {
                        System.out.println("[UpdateSplash] Found portable directory in child: " + child.getAbsolutePath());
                        return child;
                    }
                }
                // Then check any child that is a portable root
                for (File child : children) {
                    if (isPortableRoot(child)) {
                        System.out.println("[UpdateSplash] Found portable directory in child: " + child.getAbsolutePath());
                        return child;
                    }
                }
            }
        }

        System.err.println("[UpdateSplash] Warning: Could not locate portable directory via any strategy.");
        return null;
    }

    /**
     * Copies the downloaded JAR to AcadsCatchUp-Portable root and app/ directory.
     * Returns true if all applicable targets were successfully copied directly,
     * or false if any target could not be replaced directly (e.g. JVM file-lock on Windows).
     */
    public static boolean copyJarToPortable(File sourceJar, File portableDir) {
        if (sourceJar == null || !sourceJar.exists() || portableDir == null || !portableDir.exists()) {
            return false;
        }

        boolean allSuccess = true;

        // 1. Replace root AcadsCatchUp.jar
        File rootJar = new File(portableDir, "AcadsCatchUp.jar");
        if (!safeCopy(sourceJar, rootJar)) {
            allSuccess = false;
        }

        // 2. Replace app/AcadsCatchUp.jar and app/acadscatchup-app.jar if app directory exists
        File appDir = new File(portableDir, "app");
        if (appDir.exists() && appDir.isDirectory()) {
            File appJar1 = new File(appDir, "AcadsCatchUp.jar");
            if (!safeCopy(sourceJar, appJar1)) {
                allSuccess = false;
            }

            File appJar2 = new File(appDir, "acadscatchup-app.jar");
            if (appJar2.exists()) {
                if (!safeCopy(sourceJar, appJar2)) {
                    allSuccess = false;
                }
            }
        }

        return allSuccess;
    }

    private static boolean safeCopy(File source, File destination) {
        try {
            java.nio.file.Files.copy(
                    source.toPath(),
                    destination.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            System.out.println("[UpdateSplash] Replaced: " + destination.getAbsolutePath());
            return true;
        } catch (Exception e) {
            System.err.println("[UpdateSplash] Files.copy failed for " + destination.getAbsolutePath() + ": " + e.getMessage());
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(destination)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                System.out.println("[UpdateSplash] Stream copy succeeded for: " + destination.getAbsolutePath());
                return true;
            } catch (Exception streamEx) {
                System.err.println("[UpdateSplash] Error copying to " + destination.getAbsolutePath() + ": " + streamEx.getMessage());
                return false;
            }
        }
    }

    /**
     * Opens an HttpURLConnection, following HTTP and HTTPS redirects (301, 302, 303, 307, 308)
     * across different hosts and protocols (e.g. GitHub Releases to AWS S3/Azure Blob).
     */
    private static HttpURLConnection openFollowRedirects(String initialUrl) throws Exception {
        String currentUrl = initialUrl;
        int redirects = 0;
        while (redirects < 7) {
            HttpURLConnection conn = (HttpURLConnection) new URI(currentUrl).toURL().openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(90000);
            conn.setRequestProperty("User-Agent", "AcadsCatchUp-Client/" + CURRENT_VERSION);
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setInstanceFollowRedirects(false);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                    || status == 307
                    || status == 308) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null && !newUrl.isEmpty()) {
                    currentUrl = newUrl;
                    redirects++;
                    continue;
                }
            }
            return conn;
        }
        throw new IOException("Too many redirects connecting to: " + initialUrl);
    }

    /**
     * Executes a verified detached batch script on Windows to bypass active JVM file locks.
     * The script terminates the running JVM process by PID, synchronizes all target JARs in portableDir
     * and app/ subfolder with retry verification, copies to Downloads, relaunches AcadsCatchUp.exe, and self-deletes.
     */
    public static void executeDetachedHandoff(File sourceJar, File portableDir) {
        try {
            if (portableDir == null || !portableDir.exists()) {
                portableDir = findPortableDirectory();
            }
            if (portableDir == null || !portableDir.exists()) {
                portableDir = new File(".").getAbsoluteFile();
            }

            long currentPid = ProcessHandle.current().pid();
            File batFile = new File(portableDir, "update_handoff.bat");
            String batContent =
                    "@echo off\r\n" +
                    "chcp 65001 >nul 2>&1\r\n" +
                    "setlocal enabledelayedexpansion\r\n" +
                    "set \"SRC=%~1\"\r\n" +
                    "set \"DEST=%~dp0\"\r\n" +
                    "set \"OLD_PID=%~2\"\r\n" +
                    "\r\n" +
                    ":: 1. Wait for parent Java process to exit and release file handles\r\n" +
                    "if not \"%OLD_PID%\"==\"\" (\r\n" +
                    "    set /a pcount=0\r\n" +
                    "    :wait_pid_loop\r\n" +
                    "    tasklist /fi \"PID eq %OLD_PID%\" 2>nul | findstr /i \"%OLD_PID%\" >nul\r\n" +
                    "    if !ERRORLEVEL! equ 0 (\r\n" +
                    "        set /a pcount+=1\r\n" +
                    "        if !pcount! geq 6 (\r\n" +
                    "            taskkill /F /PID %OLD_PID% /T >nul 2>&1\r\n" +
                    "        ) else (\r\n" +
                    "            ping 127.0.0.1 -n 2 >nul 2>&1\r\n" +
                    "            goto wait_pid_loop\r\n" +
                    "        )\r\n" +
                    "    )\r\n" +
                    ")\r\n" +
                    ":: Extra buffer to ensure OS releases all file handles\r\n" +
                    "ping 127.0.0.1 -n 2 >nul 2>&1\r\n" +
                    "\r\n" +
                    ":: 2. Synchronize new JAR across all target locations with retry loop\r\n" +
                    "set /a ccount=0\r\n" +
                    ":copy_retry\r\n" +
                    "set /a ccount+=1\r\n" +
                    "set \"FAIL=0\"\r\n" +
                    "\r\n" +
                    "copy /y \"%SRC%\" \"%DEST%AcadsCatchUp.jar\" >nul 2>&1\r\n" +
                    "if !ERRORLEVEL! neq 0 set \"FAIL=1\"\r\n" +
                    "\r\n" +
                    "if exist \"%DEST%app\" (\r\n" +
                    "    copy /y \"%SRC%\" \"%DEST%app\\AcadsCatchUp.jar\" >nul 2>&1\r\n" +
                    "    if !ERRORLEVEL! neq 0 set \"FAIL=1\"\r\n" +
                    "    copy /y \"%SRC%\" \"%DEST%app\\acadscatchup-app.jar\" >nul 2>&1\r\n" +
                    "    if !ERRORLEVEL! neq 0 set \"FAIL=1\"\r\n" +
                    ")\r\n" +
                    "\r\n" +
                    "if \"!FAIL!\"==\"1\" (\r\n" +
                    "    if !ccount! lss 10 (\r\n" +
                    "        ping 127.0.0.1 -n 2 >nul 2>&1\r\n" +
                    "        goto copy_retry\r\n" +
                    "    )\r\n" +
                    ")\r\n" +
                    "\r\n" +
                    ":: 3. Also synchronize to user's Downloads folder if it exists\r\n" +
                    "if exist \"%USERPROFILE%\\Downloads\" (\r\n" +
                    "    copy /y \"%SRC%\" \"%USERPROFILE%\\Downloads\\AcadsCatchUp.jar\" >nul 2>&1\r\n" +
                    ")\r\n" +
                    "\r\n" +
                    ":: 4. Clean up temporary staging file\r\n" +
                    "if exist \"%SRC%\" (\r\n" +
                    "    echo \"%SRC%\" | findstr /i \"update_staging\" >nul 2>&1\r\n" +
                    "    if !ERRORLEVEL! equ 0 del /q /f \"%SRC%\" >nul 2>&1\r\n" +
                    ")\r\n" +
                    "\r\n" +
                    ":: 5. Relaunch application\r\n" +
                    "if exist \"%DEST%AcadsCatchUp.exe\" (\r\n" +
                    "    start \"\" \"%DEST%AcadsCatchUp.exe\"\r\n" +
                    ") else if exist \"%DEST%runtime\\bin\\javaw.exe\" (\r\n" +
                    "    start \"\" \"%DEST%runtime\\bin\\javaw.exe\" -jar \"%DEST%AcadsCatchUp.jar\"\r\n" +
                    ") else (\r\n" +
                    "    start \"\" javaw -jar \"%DEST%AcadsCatchUp.jar\"\r\n" +
                    ")\r\n" +
                    "\r\n" +
                    ":: 6. Self-delete this script\r\n" +
                    "(goto) 2>nul & del \"%~f0\"\r\n";

            try (FileWriter writer = new FileWriter(batFile)) {
                writer.write(batContent);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c", "start", "\"AcadsCatchUp-Update\"", "/min",
                    batFile.getAbsolutePath(),
                    sourceJar.getAbsolutePath(),
                    String.valueOf(currentPid)
            );
            pb.directory(portableDir);
            pb.start();

            System.out.println("[UpdateSplash] Detached update script launched. Shutting down JVM to release file locks.");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("[UpdateSplash] Error executing detached handoff: " + e.getMessage());
        }
    }

    private static class RemoteUpdateInfo {
        final String version;
        final String downloadUrl;

        RemoteUpdateInfo(String version, String downloadUrl) {
            this.version = version;
            this.downloadUrl = downloadUrl;
        }
    }

    /**
     * Fetches latest update info by checking direct GitHub Releases API first,
     * falling back to version.json with anti-cache query headers.
     */
    private static RemoteUpdateInfo fetchLatestUpdate() {
        // 1. Direct GitHub Releases API (real-time, zero CDN caching)
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI("https://api.github.com/repos/ZEKESAMP/AcadsCatchUp/releases/latest").toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "AcadsCatchUp-Client/" + CURRENT_VERSION);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }
                String json = sb.toString();
                String tagName = extractJsonField(json, "tag_name");
                if (tagName != null) {
                    String version = tagName.replace("v", "").trim();
                    // Extract .jar download url from release assets
                    Pattern p = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");
                    Matcher m = p.matcher(json);
                    String downloadUrl = null;
                    while (m.find()) {
                        String url = m.group(1);
                        if (url.endsWith("AcadsCatchUp.jar")) {
                            downloadUrl = url;
                            break;
                        } else if (downloadUrl == null) {
                            downloadUrl = url;
                        }
                    }
                    if (downloadUrl == null) {
                        downloadUrl = "https://github.com/ZEKESAMP/AcadsCatchUp/releases/download/" + tagName + "/AcadsCatchUp.jar";
                    }
                    return new RemoteUpdateInfo(version, downloadUrl);
                }
            }
        } catch (Exception e) {
            System.err.println("[UpdateSplash] GitHub Releases API check failed: " + e.getMessage());
        }

        // 2. Fallback to raw version.json with anti-cache timestamp
        try {
            String noCacheUrl = VERSION_URL + "?nocache=" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URI(noCacheUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "AcadsCatchUp-Client/" + CURRENT_VERSION);
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            conn.setRequestProperty("Pragma", "no-cache");

            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }
                String json = sb.toString();
                String remoteVersion = extractJsonField(json, "version");
                String downloadUrl = extractJsonField(json, "download_url");
                if (remoteVersion != null) {
                    return new RemoteUpdateInfo(remoteVersion, downloadUrl);
                }
            }
        } catch (Exception e) {
            System.err.println("[UpdateSplash] version.json check failed: " + e.getMessage());
        }

        return null;
    }

    /**
     * Manual update check method for Account Settings and Updates dialog.
     */
    public static void checkManual(Window owner) {
        new Thread(() -> {
            try {
                RemoteUpdateInfo updateInfo = fetchLatestUpdate();

                if (updateInfo != null && updateInfo.version != null) {
                    if (isNewerVersion(updateInfo.version, CURRENT_VERSION)) {
                        Platform.runLater(() -> {
                            boolean confirm = CustomAlert.showConfirmation(
                                    owner,
                                    "Update Available",
                                    "A new version of AcadsCatchUp is available!\n\n" +
                                    "Current Version: v" + CURRENT_VERSION + "\n" +
                                    "Latest Version:  v" + updateInfo.version + "\n\n" +
                                    "Download and apply update now?\n(AcadsCatchUp will restart automatically to apply the update)"
                            );
                            if (confirm) {
                                Stage[] dlStage = new Stage[1];
                                ProgressBar dlBar = new ProgressBar(0);
                                Label dlStatus = new Label("Downloading AcadsCatchUp v" + updateInfo.version + "...");
                                Label dlDetail = new Label("Connecting to server...");

                                dlBar.setMaxWidth(Double.MAX_VALUE);
                                dlBar.setPrefHeight(10);
                                dlBar.setStyle("-fx-accent: #3b82f6;");

                                dlStatus.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold;");
                                dlDetail.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                                VBox box = new VBox(12, dlStatus, dlBar, dlDetail);
                                box.setPadding(new Insets(24, 28, 24, 28));
                                box.setStyle("-fx-background-color: #1a1d2e; -fx-background-radius: 12; " +
                                             "-fx-border-color: #3b82f6; -fx-border-width: 1.5; -fx-border-radius: 12;");
                                box.setPrefWidth(380);

                                Scene scene = new Scene(box);
                                scene.setFill(Color.TRANSPARENT);
                                Stage stage = new Stage();
                                stage.initStyle(StageStyle.TRANSPARENT);
                                if (owner != null) stage.initOwner(owner);
                                stage.initModality(Modality.APPLICATION_MODAL);
                                stage.setScene(scene);
                                stage.centerOnScreen();
                                stage.show();
                                dlStage[0] = stage;

                                new Thread(() -> {
                                    try {
                                        File portable = findPortableDirectory();
                                        if (portable == null || !portable.exists()) {
                                            portable = new File(".").getAbsoluteFile();
                                        }
                                        File downloadedJar = new File(portable, "update_staging.jar");

                                        HttpURLConnection dlConn = openFollowRedirects(updateInfo.downloadUrl);
                                        int totalBytes = dlConn.getContentLength();

                                        if (totalBytes > 0) {
                                            Platform.runLater(() -> dlBar.setProgress(0));
                                        }

                                        try (InputStream in = new BufferedInputStream(dlConn.getInputStream(), 32768);
                                             OutputStream out = new BufferedOutputStream(new FileOutputStream(downloadedJar), 32768)) {
                                            byte[] buf = new byte[32768];
                                            int len;
                                            long downloaded = 0;
                                            while ((len = in.read(buf)) != -1) {
                                                out.write(buf, 0, len);
                                                downloaded += len;
                                                long finalDownloaded = downloaded;
                                                if (totalBytes > 0) {
                                                    double pct = (double) downloaded / totalBytes;
                                                    Platform.runLater(() -> {
                                                        dlBar.setProgress(pct);
                                                        dlDetail.setText(String.format("%.1f MB / %.1f MB  (%.0f%%)",
                                                                finalDownloaded / 1048576.0, totalBytes / 1048576.0, pct * 100));
                                                    });
                                                }
                                            }
                                        }

                                        // ── Integrity guard ─────────────────────────────────────
                                        if ((totalBytes > 0 && downloadedJar.length() < totalBytes * 0.95) || downloadedJar.length() < 1000000) {
                                            if (downloadedJar.exists()) downloadedJar.delete();
                                            Platform.runLater(() -> {
                                                if (dlStage[0] != null) dlStage[0].close();
                                                CustomAlert.showWarning(owner, "Download Incomplete",
                                                        "⚠️ The download appears incomplete or corrupted.\nExpected ~" +
                                                        String.format("%.1f", totalBytes / 1048576.0) + " MB, got " +
                                                        String.format("%.1f", downloadedJar.length() / 1048576.0) + " MB.\n\nPlease try again.");
                                            });
                                            return;
                                        }

                                        Platform.runLater(() -> {
                                            dlStatus.setText("✔ Download complete!");
                                            dlStatus.setStyle("-fx-text-fill: #23a55a; -fx-font-size: 13px; -fx-font-weight: bold;");
                                            dlBar.setProgress(1.0);
                                            dlBar.setStyle("-fx-accent: #23a55a;");
                                            dlDetail.setText("Restarting AcadsCatchUp to apply update...");
                                        });
                                        Thread.sleep(800);

                                        final File finalPortable = portable;
                                        Platform.runLater(() -> {
                                            if (dlStage[0] != null) dlStage[0].close();
                                            executeDetachedHandoff(downloadedJar, finalPortable);
                                        });
                                    } catch (Exception ex) {
                                        Platform.runLater(() -> {
                                            if (dlStage[0] != null) dlStage[0].close();
                                            CustomAlert.showWarning(owner, "Update Failed", "Failed to download update: " + ex.getMessage());
                                        });
                                    }
                                }, "Manual-Update-Downloader").start();
                            }
                        });
                    } else {
                        Platform.runLater(() -> CustomAlert.showInfo(
                                owner,
                                "✔ Up to Date",
                                "You are already using the latest version of AcadsCatchUp.\n\nInstalled: v" + CURRENT_VERSION
                        ));
                    }
                } else {
                    Platform.runLater(() -> CustomAlert.showWarning(
                            owner,
                            "Server Unavailable",
                            "Could not reach update server. Please check your internet connection."
                    ));
                }
            } catch (Exception e) {
                Platform.runLater(() -> CustomAlert.showWarning(
                        owner,
                        "Check Failed",
                        "Unable to check for updates: " + e.getMessage()
                ));
            }
        }, "Manual-Update-Checker").start();
    }

    private static String extractJsonField(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static boolean isNewerVersion(String remote, String current) {
        String[] rParts = remote.replace("v", "").split("\\.");
        String[] cParts = current.replace("v", "").split("\\.");

        int length = Math.max(rParts.length, cParts.length);
        for (int i = 0; i < length; i++) {
            int r = i < rParts.length ? Integer.parseInt(rParts[i].replaceAll("\\D+", "")) : 0;
            int c = i < cParts.length ? Integer.parseInt(cParts[i].replaceAll("\\D+", "")) : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }

    /**
     * Silently checks GitHub in a background daemon thread.
     * If a newer version is found, updates the given updatesBtn with a yellow
     * "🔄 Updates (New!)" badge so the user knows to click it.
     * Call this from dashboard initialize() methods.
     *
     * @param updatesBtn the Button to badge if an update is available
     */
    public static void checkAndBadgeUpdatesButton(javafx.scene.control.Button updatesBtn) {
        if (updatesBtn == null) return;
        Thread t = new Thread(() -> {
            try {
                RemoteUpdateInfo info = fetchLatestUpdate();
                if (info != null && info.version != null && isNewerVersion(info.version, CURRENT_VERSION)) {
                    Platform.runLater(() -> {
                        updatesBtn.setText("\uD83D\uDD04 Updates (New!)");
                        updatesBtn.setStyle(
                                "-fx-background-color: rgba(240, 165, 0, 0.25); " +
                                "-fx-text-fill: #fbbf24; " +
                                "-fx-font-weight: bold; " +
                                "-fx-border-color: #f0a500; " +
                                "-fx-border-radius: 6; " +
                                "-fx-border-width: 1; " +
                                "-fx-background-radius: 6;"
                        );
                        updatesBtn.setTooltip(new javafx.scene.control.Tooltip(
                                "AcadsCatchUp v" + info.version + " is available! Click to update."
                        ));
                        System.out.println("[UpdateSplash] Badge: newer version v" + info.version + " detected.");
                    });
                }
            } catch (Exception e) {
                // Silent fail — badge is optional, never block dashboard startup
                System.err.println("[UpdateSplash] Badge check error: " + e.getMessage());
            }
        }, "UpdateBadge-Check");
        t.setDaemon(true);
        t.start();
    }
}
