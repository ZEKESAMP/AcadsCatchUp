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
 * Displays an elegant frameless dark card with logo, checks GitHub for updates
 * asynchronously with a strict timeout, and seamlessly transitions to Login.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class UpdateSplash {

    public static final String DEVELOPER = "F4TAL";

    public static final String CURRENT_VERSION = "1.0.2";
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
                            detailLabel.setText("Preparing download...");
                            progressBar.setProgress(0);
                        });

                        downloadAndApplyUpdate(downloadUrl, remoteVersion, progressBar, statusLabel, detailLabel);
                        return;
                    } else {
                        // ── Up to Date ──
                        Platform.runLater(() -> {
                            statusLabel.setText("✔ You're up to date!");
                            statusLabel.setStyle("-fx-text-fill: #23a55a; -fx-font-size: 12.5px; -fx-font-weight: bold;");
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
                statusLabel.setText("Offline mode • Starting...");
                detailLabel.setText("No network connection detected.");
            });
            Thread.sleep(400);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Downloads the updated JAR file with live progress tracking and triggers hand-off restart.
     */
    private static void downloadAndApplyUpdate(String downloadUrl, String newVersion,
                                              ProgressBar progressBar, Label statusLabel, Label detailLabel) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI(downloadUrl).toURL().openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "AcadsCatchUp-Client/" + CURRENT_VERSION);

            int contentLength = conn.getContentLength();
            File tempJar = File.createTempFile("AcadsCatchUp-update-", ".jar");
            tempJar.deleteOnExit();

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(tempJar))) {

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
                statusLabel.setText("✔ Update ready! Restarting...");
                statusLabel.setStyle("-fx-text-fill: #23a55a; -fx-font-size: 12.5px; -fx-font-weight: bold;");
                detailLabel.setText("Applying v" + newVersion + "...");
                progressBar.setProgress(1.0);
            });

            Thread.sleep(800);
            applyUpdateAndRestart(tempJar);

        } catch (Exception e) {
            System.err.println("[UpdateSplash] Download failed: " + e.getMessage());
            Platform.runLater(() -> {
                statusLabel.setText("Download failed. Launching current version...");
                detailLabel.setText("Error: " + e.getMessage());
            });
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Executes detached hand-off script to safely overwrite the active JAR and restart.
     */
    private static void applyUpdateAndRestart(File tempJar) {
        try {
            String currentPath = UpdateSplash.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            if (currentPath.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("win")) {
                currentPath = currentPath.substring(1);
            }
            File currentJar = new File(currentPath);

            File baseDir = currentJar.getParentFile();
            File exeFile = new File(baseDir, "AcadsCatchUp.exe");

            String restartCmd;
            if (exeFile.exists()) {
                restartCmd = String.format(
                        "timeout /t 2 /nobreak >nul & move /y \"%s\" \"%s\" & start \"\" \"%s\"",
                        tempJar.getAbsolutePath(),
                        currentJar.getAbsolutePath(),
                        exeFile.getAbsolutePath()
                );
            } else {
                restartCmd = String.format(
                        "timeout /t 2 /nobreak >nul & move /y \"%s\" \"%s\" & start javaw -jar \"%s\"",
                        tempJar.getAbsolutePath(),
                        currentJar.getAbsolutePath(),
                        currentJar.getAbsolutePath()
                );
            }

            new ProcessBuilder("cmd.exe", "/c", restartCmd).start();
            System.exit(0);

        } catch (Exception e) {
            System.err.println("[UpdateSplash] Restart execution failed: " + e.getMessage());
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
                    String remoteVersion = extractJsonField(sb.toString(), "version");

                    if (remoteVersion != null && isNewerVersion(remoteVersion, CURRENT_VERSION)) {
                        Platform.runLater(() -> {
                            boolean confirm = CustomAlert.showConfirmation(
                                    owner,
                                    "Update Available",
                                    "A new version of AcadsCatchUp is available!\n\n" +
                                    "Current Version: v" + CURRENT_VERSION + "\n" +
                                    "Latest Version:  v" + remoteVersion + "\n\n" +
                                    "Would you like to restart and apply this update now?"
                            );
                            if (confirm) {
                                Stage splash = new Stage();
                                showAndCheck(splash);
                            }
                        });
                    } else {
                        Platform.runLater(() -> CustomAlert.showInfo(
                                owner,
                                "Up to Date",
                                "You are already using the latest version of AcadsCatchUp (v" + CURRENT_VERSION + ")."
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
