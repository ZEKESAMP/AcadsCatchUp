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

    public static final String CURRENT_VERSION = "1.0.3";
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

                HttpURLConnection conn = (HttpURLConnection) new URI(VERSION_URL).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2500); // 2.5s connect timeout
                conn.setReadTimeout(2500);    // 2.5s read timeout
                conn.setRequestProperty("User-Agent", "AcadsCatchUp-Client/" + CURRENT_VERSION);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    String json = sb.toString();
                    String remoteVersion = extractJsonField(json, "version");
                    String downloadUrl = extractJsonField(json, "download_url");

                    if (remoteVersion != null && isNewerVersion(remoteVersion, CURRENT_VERSION)) {
                        // ── Update Available ──
                        Platform.runLater(() -> {
                            statusLabel.setText("⬇ Update found: v" + remoteVersion);
                            detailLabel.setText("Downloading to Downloads folder...");
                            progressBar.setProgress(0);
                        });

                        downloadAndApplyUpdate(downloadUrl, remoteVersion, progressBar, statusLabel, detailLabel, splashStage, primaryStage);
                        return;
                    } else {
                        // ── No Update ──
                        Platform.runLater(() -> {
                            statusLabel.setText("No update");
                            statusLabel.setStyle("-fx-text-fill: #949ba4; -fx-font-size: 13px; -fx-font-weight: 600;");
                            progressBar.setProgress(1.0);
                            progressBar.setStyle("-fx-accent: #5865f2;");
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
                statusLabel.setText("No update");
                statusLabel.setStyle("-fx-text-fill: #949ba4; -fx-font-size: 13px; -fx-font-weight: 600;");
                detailLabel.setText("Offline mode • Launching AcadsCatchUp...");
            });
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Downloads the updated JAR file to Downloads folder, locates AcadsCatchUp-Portable,
     * replaces the existing JAR(s), and immediately transitions to the Login screen without restarting.
     */
    private static void downloadAndApplyUpdate(String downloadUrl, String newVersion,
                                              ProgressBar progressBar, Label statusLabel, Label detailLabel,
                                              Stage splashStage, Stage primaryStage) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI(downloadUrl).toURL().openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "AcadsCatchUp-Client/" + CURRENT_VERSION);

            int contentLength = conn.getContentLength();

            // 1. Target in user's Downloads folder
            File downloadsDir = new File(System.getProperty("user.home"), "Downloads");
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File downloadedJar = new File(downloadsDir, "AcadsCatchUp.jar");

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(downloadedJar))) {

                byte[] buffer = new byte[8192];
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

            Platform.runLater(() -> {
                statusLabel.setText("Applying update to AcadsCatchUp-Portable...");
                detailLabel.setText("Replacing old .jar file...");
            });

            // 2. Find AcadsCatchUp-Portable folder and copy/replace
            File portableDir = findPortableDirectory();
            if (portableDir != null && portableDir.exists()) {
                copyJarToPortable(downloadedJar, portableDir);
            }

            // 3. User feedback: Update applied!
            Platform.runLater(() -> {
                statusLabel.setText("✔ Update applied!");
                statusLabel.setStyle("-fx-text-fill: #23a55a; -fx-font-size: 13px; -fx-font-weight: bold;");
                detailLabel.setText("Opening AcadsCatchUp...");
                progressBar.setProgress(1.0);
                progressBar.setStyle("-fx-accent: #23a55a;");
            });

            Thread.sleep(700);

        } catch (Exception e) {
            System.err.println("[UpdateSplash] Download/update failed: " + e.getMessage());
            Platform.runLater(() -> {
                statusLabel.setText("No update (Download failed)");
                detailLabel.setText("Launching current version...");
            });
            try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        }

        // 4. No restart needed — go automatically to Login Phase
        Platform.runLater(() -> {
            splashStage.close();
            try {
                Main.showLoginScreen(primaryStage);
            } catch (Exception ex) {
                System.err.println("[UpdateSplash] Error launching main login: " + ex.getMessage());
            }
        });
    }

    /**
     * Locates the AcadsCatchUp-Portable folder by checking the execution environment
     * and common filesystem locations.
     */
    public static File findPortableDirectory() {
        // 1. Check running location from CodeSource
        try {
            File codeSourceJar = new File(UpdateSplash.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File current = codeSourceJar.getParentFile();
            while (current != null) {
                if (current.getName().equalsIgnoreCase("AcadsCatchUp-Portable")
                        || current.getName().equalsIgnoreCase("AcadsCatchUp")) {
                    return current;
                }
                current = current.getParentFile();
            }

            File parent = codeSourceJar.getParentFile();
            if (parent != null) {
                if (parent.getName().equalsIgnoreCase("app")) {
                    File potential = parent.getParentFile();
                    if (potential != null && (potential.getName().equalsIgnoreCase("AcadsCatchUp-Portable")
                            || potential.getName().equalsIgnoreCase("AcadsCatchUp")
                            || new File(potential, "AcadsCatchUp.exe").exists()
                            || new File(potential, "AcadsCatchUp.jar").exists())) {
                        return potential;
                    }
                }
                if (new File(parent, "AcadsCatchUp.exe").exists() || new File(parent, "AcadsCatchUp.jar").exists()) {
                    return parent;
                }
            }
        } catch (Exception ignored) {}

        // 2. Search common locations
        String userHome = System.getProperty("user.home");
        String localAppData = System.getenv("LOCALAPPDATA");
        File[] candidates = new File[] {
            new File("."),
            new File("./AcadsCatchUp-Portable"),
            new File("./AcadsCatchUp"),
            new File("./dist/AcadsCatchUp-Portable"),
            new File("./dist/AcadsCatchUp"),
            new File(userHome, "Downloads/AcadsCatchUp-Portable"),
            new File(userHome, "Downloads/AcadsCatchUp"),
            new File(userHome, "Desktop/AcadsCatchUp-Portable"),
            new File(userHome, "Desktop/AcadsCatchUp"),
            new File(userHome, "Documents/AcadsCatchUp-Portable"),
            new File(userHome, "Documents/AcadsCatchUp"),
            new File(userHome, "Documents/AcadsCatchUp/dist/AcadsCatchUp-Portable"),
            localAppData != null ? new File(localAppData, "AcadsCatchUp") : null,
            localAppData != null ? new File(localAppData, "AcadsCatchUp-Portable") : null
        };

        for (File candidate : candidates) {
            if (candidate != null && candidate.exists() && candidate.isDirectory()) {
                if (candidate.getName().equalsIgnoreCase("AcadsCatchUp-Portable")
                        || candidate.getName().equalsIgnoreCase("AcadsCatchUp")
                        || new File(candidate, "AcadsCatchUp.exe").exists()
                        || new File(candidate, "AcadsCatchUp.jar").exists()
                        || new File(candidate, "app").exists()) {
                    return candidate;
                }
            }
        }

        return null;
    }

    /**
     * Copies the downloaded JAR to AcadsCatchUp-Portable root and app/ directory.
     */
    public static void copyJarToPortable(File sourceJar, File portableDir) {
        if (sourceJar == null || !sourceJar.exists() || portableDir == null || !portableDir.exists()) {
            return;
        }

        // Replace root AcadsCatchUp.jar
        File rootJar = new File(portableDir, "AcadsCatchUp.jar");
        safeCopy(sourceJar, rootJar);

        // Replace app/AcadsCatchUp.jar and app/acadscatchup-app.jar if app directory exists
        File appDir = new File(portableDir, "app");
        if (appDir.exists() && appDir.isDirectory()) {
            File appJar1 = new File(appDir, "AcadsCatchUp.jar");
            safeCopy(sourceJar, appJar1);

            File appJar2 = new File(appDir, "acadscatchup-app.jar");
            if (appJar2.exists()) {
                safeCopy(sourceJar, appJar2);
            }
        }
    }

    private static void safeCopy(File source, File destination) {
        try {
            java.nio.file.Files.copy(
                    source.toPath(),
                    destination.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            System.out.println("[UpdateSplash] Replaced: " + destination.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[UpdateSplash] Warning: Files.copy failed for " + destination.getAbsolutePath() + ": " + e.getMessage());
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(destination)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                System.out.println("[UpdateSplash] Stream copy succeeded for: " + destination.getAbsolutePath());
            } catch (Exception streamEx) {
                System.err.println("[UpdateSplash] Error copying to " + destination.getAbsolutePath() + ": " + streamEx.getMessage());
            }
        }
    }

    /**
     * Manual update check method for Account Settings dialog.
     */
    public static void checkManual(Window owner) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URI(VERSION_URL).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                    }
                    String json = sb.toString();
                    String remoteVersion = extractJsonField(json, "version");
                    String downloadUrl = extractJsonField(json, "download_url");

                    if (remoteVersion != null && isNewerVersion(remoteVersion, CURRENT_VERSION)) {
                        Platform.runLater(() -> {
                            boolean confirm = CustomAlert.showConfirmation(
                                    owner,
                                    "Update Available",
                                    "A new version of AcadsCatchUp is available!\n\n" +
                                    "Current Version: v" + CURRENT_VERSION + "\n" +
                                    "Latest Version:  v" + remoteVersion + "\n\n" +
                                    "Download to Downloads and apply to AcadsCatchUp-Portable now?\n(No restart required)"
                            );
                            if (confirm) {
                                new Thread(() -> {
                                    try {
                                        File downloadsDir = new File(System.getProperty("user.home"), "Downloads");
                                        if (!downloadsDir.exists()) downloadsDir.mkdirs();
                                        File downloadedJar = new File(downloadsDir, "AcadsCatchUp.jar");

                                        HttpURLConnection dlConn = (HttpURLConnection) new URI(downloadUrl).toURL().openConnection();
                                        dlConn.setConnectTimeout(8000);
                                        dlConn.setReadTimeout(20000);
                                        try (InputStream in = new BufferedInputStream(dlConn.getInputStream());
                                             OutputStream out = new BufferedOutputStream(new FileOutputStream(downloadedJar))) {
                                            byte[] buf = new byte[8192];
                                            int len;
                                            while ((len = in.read(buf)) != -1) {
                                                out.write(buf, 0, len);
                                            }
                                        }

                                        File portable = findPortableDirectory();
                                        if (portable != null && portable.exists()) {
                                            copyJarToPortable(downloadedJar, portable);
                                        }

                                        Platform.runLater(() -> CustomAlert.showInfo(
                                                owner,
                                                "Update Applied",
                                                "✔ Update v" + remoteVersion + " downloaded and applied to AcadsCatchUp-Portable!\nNo restart needed."
                                        ));
                                    } catch (Exception ex) {
                                        Platform.runLater(() -> CustomAlert.showWarning(
                                                owner,
                                                "Update Failed",
                                                "Failed to download update: " + ex.getMessage()
                                        ));
                                    }
                                }, "Manual-Update-Downloader").start();
                            }
                        });
                    } else {
                        Platform.runLater(() -> CustomAlert.showInfo(
                                owner,
                                "No update",
                                "No update • You are already using the latest version of AcadsCatchUp (v" + CURRENT_VERSION + ")."
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
}
