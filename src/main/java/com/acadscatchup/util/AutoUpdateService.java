package com.acadscatchup.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 100% Pure Java Auto-Update Engine.
 * Periodically checks remote version manifest, downloads updated standalone JAR,
 * and performs safe hot-swap restart without requiring external installer tools.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AutoUpdateService {

    public static final String DEVELOPER = "F4TAL";
    public static final String CURRENT_VERSION = "1.0.1";
    public static final String DEFAULT_UPDATE_URL =
            "https://raw.githubusercontent.com/ZEKESAMP/AcadsCatchUp/main/version.json";

    public static class UpdateInfo {
        public final boolean isUpdateAvailable;
        public final String latestVersion;
        public final String releaseDate;
        public final String downloadUrl;
        public final String changelog;

        public UpdateInfo(boolean isUpdateAvailable, String latestVersion, String releaseDate, String downloadUrl, String changelog) {
            this.isUpdateAvailable = isUpdateAvailable;
            this.latestVersion = latestVersion;
            this.releaseDate = releaseDate;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
        }
    }

    /**
     * Check for updates asynchronously in a background daemon thread.
     */
    public static void checkForUpdatesAsync(Consumer<UpdateInfo> callback) {
        Thread t = new Thread(() -> {
            UpdateInfo info = checkUpdateSync();
            if (callback != null) {
                Platform.runLater(() -> callback.accept(info));
            }
        }, "AutoUpdate-Daemon");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Synchronously queries the version manifest.
     */
    public static UpdateInfo checkUpdateSync() {
        try {
            URL url = URI.create(getUpdateManifestUrl()).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "AcadsCatchUp/" + CURRENT_VERSION);

            int code = conn.getResponseCode();
            if (code != 200) {
                return new UpdateInfo(false, CURRENT_VERSION, "", "", "HTTP " + code);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String json = sb.toString();

                String remoteVersion = extractJsonField(json, "version");
                String releaseDate   = extractJsonField(json, "release_date");
                String downloadUrl   = extractJsonField(json, "download_url");
                String notes         = extractJsonField(json, "notes");

                boolean available = isNewerVersion(remoteVersion, CURRENT_VERSION);
                return new UpdateInfo(available, remoteVersion, releaseDate, downloadUrl, notes);
            }
        } catch (Exception e) {
            return new UpdateInfo(false, CURRENT_VERSION, "", "", e.getMessage());
        }
    }

    private static String getUpdateManifestUrl() {
        String custom = System.getProperty("acadscatchup.update.url");
        if (custom != null && !custom.isBlank()) return custom.trim();
        return DEFAULT_UPDATE_URL;
    }

    private static String extractJsonField(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    /**
     * Compares version strings (e.g. "1.0.1" vs "1.0.0").
     */
    public static boolean isNewerVersion(String remote, String current) {
        if (remote == null || remote.isBlank() || current == null) return false;
        String[] rParts = remote.trim().split("\\.");
        String[] cParts = current.trim().split("\\.");
        int len = Math.max(rParts.length, cParts.length);

        for (int i = 0; i < len; i++) {
            int r = i < rParts.length ? parseSafeInt(rParts[i]) : 0;
            int c = i < cParts.length ? parseSafeInt(cParts[i]) : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }

    private static int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Interactively checks for updates with user feedback dialogs.
     */
    public static void checkManual(Window owner) {
        VBox dialogBox = new VBox(14);
        dialogBox.setStyle("-fx-background-color: #1a1d2e; -fx-padding: 24; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

        Label title = new Label("🔄 Software Update Check");
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label statusLbl = new Label("Connecting to update server... Checking for latest release.");
        statusLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        dialogBox.getChildren().addAll(title, statusLbl, progressBar);

        Thread checkThread = new Thread(() -> {
            UpdateInfo info = checkUpdateSync();
            Platform.runLater(() -> {
                ModalOverlay.close(dialogBox);
                if (info.isUpdateAvailable) {
                    promptAndApplyUpdate(owner, info);
                } else {
                    CustomAlert.showInfo(owner, "Up to Date",
                            "You are running the latest version of AcadsCatchUp (v" + CURRENT_VERSION + ").\n\nNo updates are required at this time.");
                }
            });
        });
        checkThread.setDaemon(true);
        checkThread.start();

        Node anchor = (owner != null && owner.getScene() != null) ? owner.getScene().getRoot() : (AppTrayManager.getCurrentStage() != null && AppTrayManager.getCurrentStage().getScene() != null ? AppTrayManager.getCurrentStage().getScene().getRoot() : null);
        ModalOverlay.showAndWait(anchor, dialogBox, 420, 180);
    }

    /**
     * Prompts the user to update and downloads the new JAR.
     */
    public static void promptAndApplyUpdate(Window owner, UpdateInfo info) {
        VBox root = new VBox(16);
        root.setStyle("-fx-background-color: #1a1d2e; -fx-padding: 24; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

        Label title = new Label("🎉 New Update Available: v" + info.latestVersion);
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label currentLbl = new Label("Installed Version: v" + CURRENT_VERSION + "  ➔  Latest Version: v" + info.latestVersion);
        currentLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12.5px; -fx-font-weight: bold;");

        Label notesTitle = new Label("Release Notes & Changes:");
        notesTitle.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label notes = new Label(info.changelog != null && !info.changelog.isBlank() ? info.changelog : "Performance enhancements, bug fixes, and security updates.");
        notes.setWrapText(true);
        notes.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-background-color: #121520; -fx-padding: 10; -fx-background-radius: 6;");

        ProgressBar progress = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setVisible(false);
        progress.setManaged(false);

        Label progressStatus = new Label("");
        progressStatus.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");
        progressStatus.setVisible(false);
        progressStatus.setManaged(false);

        HBox buttons = new HBox(12);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnLater = new Button("Later");
        btnLater.getStyleClass().add("btn-ghost");
        btnLater.setOnAction(e -> ModalOverlay.close(root));

        Button btnUpdate = new Button("⬇ Update & Restart Now");
        btnUpdate.getStyleClass().add("btn-primary");

        buttons.getChildren().addAll(btnLater, btnUpdate);
        root.getChildren().addAll(title, currentLbl, notesTitle, notes, progress, progressStatus, buttons);

        btnUpdate.setOnAction(e -> {
            btnLater.setDisable(true);
            btnUpdate.setDisable(true);
            progress.setVisible(true); progress.setManaged(true);
            progressStatus.setVisible(true); progressStatus.setManaged(true);
            progressStatus.setText("Downloading update package...");

            new Thread(() -> {
                boolean downloaded = downloadUpdateFile(info.downloadUrl, p -> {
                    Platform.runLater(() -> progress.setProgress(p));
                });

                Platform.runLater(() -> {
                    if (downloaded) {
                        progressStatus.setText("Download complete! Applying update and restarting...");
                        progressStatus.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");
                        executeRestartWithNewJar();
                    } else {
                        progressStatus.setText("❌ Download failed. Please try again later or visit the GitHub repository.");
                        progressStatus.setStyle("-fx-text-fill: #f87171;");
                        btnLater.setDisable(false);
                    }
                });
            }, "UpdateDownloader-Thread").start();
        });

        Node anchor = (owner != null && owner.getScene() != null) ? owner.getScene().getRoot() : (AppTrayManager.getCurrentStage() != null && AppTrayManager.getCurrentStage().getScene() != null ? AppTrayManager.getCurrentStage().getScene().getRoot() : null);
        ModalOverlay.showAndWait(anchor, root, 520, 360);
    }

    private static boolean downloadUpdateFile(String downloadUrl, Consumer<Double> progressConsumer) {
        if (downloadUrl == null || downloadUrl.isBlank()) return false;
        try {
            URL url = URI.create(downloadUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "AcadsCatchUp-Updater/" + CURRENT_VERSION);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307 || code == 308) {
                String newUrl = conn.getHeaderField("Location");
                return downloadUpdateFile(newUrl, progressConsumer);
            }

            if (code != 200) return false;

            long totalBytes = conn.getContentLengthLong();
            File currentJar = getCurrentJarFile();
            File targetNewJar = new File(currentJar.getParentFile(), "AcadsCatchUp.jar.new");

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(targetNewJar))) {

                byte[] buffer = new byte[8192];
                long readSoFar = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    readSoFar += read;
                    if (totalBytes > 0 && progressConsumer != null) {
                        progressConsumer.accept((double) readSoFar / totalBytes);
                    }
                }
            }

            // Verify file size
            return targetNewJar.exists() && targetNewJar.length() > 500000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static File getCurrentJarFile() {
        try {
            String path = AutoUpdateService.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File f = new File(path);
            if (f.isFile() && f.getName().endsWith(".jar")) {
                return f;
            }
        } catch (Exception ignored) {}
        // Fallback to local execution directory
        return new File("dist/AcadsCatchUp.jar").getAbsoluteFile();
    }

    private static void executeRestartWithNewJar() {
        File currentJar = getCurrentJarFile();
        File newJar = new File(currentJar.getParentFile(), "AcadsCatchUp.jar.new");
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

        try {
            if (isWindows) {
                File exeSibling = new File(currentJar.getParentFile(), "AcadsCatchUp.exe");
                String startCmd = (exeSibling.exists() && exeSibling.length() > 10000)
                        ? String.format("start \"\" \"%s\"", exeSibling.getAbsolutePath())
                        : String.format("start javaw -jar \"%s\"", currentJar.getAbsolutePath());

                String cmd = String.format(
                        "cmd.exe /c timeout /t 2 > nul & copy /y \"%s\" \"%s\" & del /f /q \"%s\" & %s",
                        newJar.getAbsolutePath(), currentJar.getAbsolutePath(), newJar.getAbsolutePath(), startCmd
                );
                new ProcessBuilder("cmd.exe", "/c", cmd).start();
            } else {
                String cmd = String.format(
                        "sleep 2 && cp -f \"%s\" \"%s\" && rm -f \"%s\" && java -jar \"%s\" &",
                        newJar.getAbsolutePath(), currentJar.getAbsolutePath(), newJar.getAbsolutePath(), currentJar.getAbsolutePath()
                );
                new ProcessBuilder("sh", "-c", cmd).start();
            }
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
