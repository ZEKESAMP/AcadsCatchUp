package com.acadscatchup.installer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 100% Pure Java Multi-Step Windows Setup & Installation Wizard for AcadsCatchUp.
 * Matches the classic Inno Setup / Git Setup aesthetic and step-by-step workflow:
 *  - Step 0: Information / License Agreement (GNU General Public License v3)
 *  - Step 1: Select Destination Location (%LOCALAPPDATA%\Programs\AcadsCatchUp)
 *  - Step 2: Select Additional Tasks (Desktop & Start Menu shortcuts)
 *  - Step 3: Ready to Install (Configuration summary)
 *  - Step 4: Installing (Component extraction, shortcut creation, registry entry)
 *  - Step 5: Completing Setup (Finished page with Launch checkbox)
 *
 * Fully integrated with Windows 10 Control Panel ("Apps & features") and native uninstaller.
 * Free of SFX packer heuristics that trigger YARA false-positives.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AcadsCatchUpInstaller extends Application {

    public static final String DEVELOPER = "F4TAL";
    public static final String APP_NAME = "AcadsCatchUp";
    public static final String VERSION = "1.0.0";
    public static final String PUBLISHER = "F4TAL (Stevenson James G. Gastanes)";
    public static final String PROJECT_URL = "https://github.com/ZEKESAMP/AcadsCatchUp";

    private static final String BTN_STYLE =
            "-fx-background-color: #e1e1e1, #f0f0f0; " +
            "-fx-background-insets: 0, 1; " +
            "-fx-background-radius: 2; " +
            "-fx-border-color: #adadad; " +
            "-fx-border-radius: 2; " +
            "-fx-text-fill: #000000; " +
            "-fx-font-family: 'Segoe UI', sans-serif; " +
            "-fx-font-size: 11.5px; " +
            "-fx-min-width: 75px; " +
            "-fx-pref-width: 75px; " +
            "-fx-min-height: 23px; " +
            "-fx-pref-height: 23px; " +
            "-fx-cursor: hand;";

    private static final String BTN_HOVER_STYLE =
            "-fx-background-color: #e5f1fb, #fdfdfd; " +
            "-fx-background-insets: 0, 1; " +
            "-fx-background-radius: 2; " +
            "-fx-border-color: #0078d7; " +
            "-fx-border-radius: 2; " +
            "-fx-text-fill: #000000; " +
            "-fx-font-family: 'Segoe UI', sans-serif; " +
            "-fx-font-size: 11.5px; " +
            "-fx-min-width: 75px; " +
            "-fx-pref-width: 75px; " +
            "-fx-min-height: 23px; " +
            "-fx-pref-height: 23px; " +
            "-fx-cursor: hand;";

    // Wizard navigation state
    private int currentStep = 0;
    private final Node[] pages = new Node[6];

    // Header UI
    private HBox headerPane;
    private Label headerTitleLbl;
    private Label headerSubtitleLbl;
    private ImageView headerLogoView;

    // Body Pages UI
    private StackPane bodyContainer;
    private TextField targetDirField;
    private CheckBox cbDesktopShortcut;
    private CheckBox cbStartMenuShortcut;
    private CheckBox cbLaunchAfter;
    private TextArea summaryArea;
    private ProgressBar progressBar;
    private Label statusLabel;

    // Footer UI
    private Hyperlink footerLink;
    private Button btnBack;
    private Button btnNext;
    private Button btnCancel;

    public static void main(String[] args) {
        for (String arg : args) {
            if ("--silent".equalsIgnoreCase(arg) || "-s".equalsIgnoreCase(arg)) {
                runSilentInstall();
                return;
            }
        }
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle(APP_NAME + " " + VERSION + " Setup");

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")));
        } catch (Exception ignored) {}

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f0f0f0; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // ── 1. Top Header Banner ──────────────────────────────────────────────
        headerPane = new HBox(12);
        headerPane.setAlignment(Pos.CENTER_LEFT);
        headerPane.setStyle("-fx-background-color: #ffffff; -fx-padding: 8 16 8 16; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0; -fx-min-height: 58; -fx-pref-height: 58;");

        VBox headerTextBox = new VBox(2);
        headerTitleLbl = new Label("Information");
        headerTitleLbl.setStyle("-fx-text-fill: #000000; -fx-font-size: 12px; -fx-font-weight: bold;");

        headerSubtitleLbl = new Label("Please read the following important information before continuing.");
        headerSubtitleLbl.setStyle("-fx-text-fill: #333333; -fx-font-size: 11px; -fx-padding: 0 0 0 16;");
        headerTextBox.getChildren().addAll(headerTitleLbl, headerSubtitleLbl);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        headerLogoView = new ImageView();
        try {
            headerLogoView.setImage(new Image(getClass().getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")));
            headerLogoView.setFitWidth(40);
            headerLogoView.setFitHeight(40);
            headerLogoView.setPreserveRatio(true);
        } catch (Exception ignored) {}

        headerPane.getChildren().addAll(headerTextBox, headerSpacer, headerLogoView);

        // ── 2. Body Container & Pages ─────────────────────────────────────────
        bodyContainer = new StackPane();
        bodyContainer.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 12 16 10 16;");
        VBox.setVgrow(bodyContainer, Priority.ALWAYS);

        initWizardPages(stage);

        // ── 3. Bottom Footer ──────────────────────────────────────────────────
        VBox footerContainer = new VBox(0);
        Separator footerSep = new Separator();
        footerSep.setStyle("-fx-background-color: #d0d0d0;");

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10 14 10 14;");

        footerLink = new Hyperlink(PROJECT_URL);
        footerLink.setStyle("-fx-text-fill: #0066cc; -fx-font-size: 11px; -fx-padding: 2 0; -fx-border-color: transparent;");
        footerLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(PROJECT_URL));
                }
            } catch (Exception ignored) {}
        });

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        btnBack = createWindowsButton("< Back");
        btnBack.setOnAction(e -> handleBack());

        btnNext = createWindowsButton("Next >");
        btnNext.setOnAction(e -> handleNext(stage));

        btnCancel = createWindowsButton("Cancel");
        btnCancel.setOnAction(e -> stage.close());

        footer.getChildren().addAll(footerLink, footerSpacer, btnBack, btnNext, btnCancel);
        footerContainer.getChildren().addAll(footerSep, footer);

        root.getChildren().addAll(headerPane, bodyContainer, footerContainer);

        // Set initial step 0
        goToStep(0);

        stage.setScene(new Scene(root, 520, 395));
        stage.setResizable(false);
        stage.show();
    }

    private Button createWindowsButton(String text) {
        Button b = new Button(text);
        b.setStyle(BTN_STYLE);
        b.setOnMouseEntered(e -> {
            if (!b.isDisable()) b.setStyle(BTN_HOVER_STYLE);
        });
        b.setOnMouseExited(e -> {
            if (!b.isDisable()) b.setStyle(BTN_STYLE);
        });
        return b;
    }

    private void initWizardPages(Stage stage) {
        // ── Step 0: Information / License Agreement ───────────────────────────
        VBox p0 = new VBox(8);
        p0.setAlignment(Pos.TOP_LEFT);

        Label p0Notice = new Label("When you are ready to continue with Setup, click Next.");
        p0Notice.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px;");

        TextArea licenseArea = new TextArea(loadLicenseText());
        licenseArea.setEditable(false);
        licenseArea.setWrapText(true);
        licenseArea.setStyle("-fx-control-inner-background: #ffffff; -fx-border-color: #7a7a7a; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 10.5px;");
        VBox.setVgrow(licenseArea, Priority.ALWAYS);

        p0.getChildren().addAll(p0Notice, licenseArea);
        pages[0] = p0;

        // ── Step 1: Select Destination Location ───────────────────────────────
        VBox p1 = new VBox(12);
        p1.setAlignment(Pos.TOP_LEFT);

        Label p1Desc1 = new Label("Setup will install " + APP_NAME + " into the following folder.");
        p1Desc1.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px;");

        Label p1Desc2 = new Label("To continue, click Next. If you would like to select a different folder, click Browse.");
        p1Desc2.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px;");

        HBox dirRow = new HBox(8);
        dirRow.setAlignment(Pos.CENTER_LEFT);
        targetDirField = new TextField(getDefaultInstallDir().getAbsolutePath());
        targetDirField.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-border-color: #7a7a7a; -fx-padding: 3 6; -fx-font-size: 11.5px;");
        HBox.setHgrow(targetDirField, Priority.ALWAYS);

        Button btnBrowse = createWindowsButton("Browse...");
        btnBrowse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Installation Folder");
            File f = dc.showDialog(stage);
            if (f != null) {
                targetDirField.setText(new File(f, APP_NAME).getAbsolutePath());
            }
        });
        dirRow.getChildren().addAll(targetDirField, btnBrowse);

        Region p1Spacer = new Region();
        VBox.setVgrow(p1Spacer, Priority.ALWAYS);

        Label p1Req = new Label("At least 35.0 MB of free disk space is required.");
        p1Req.setStyle("-fx-text-fill: #333333; -fx-font-size: 11px;");

        p1.getChildren().addAll(p1Desc1, p1Desc2, dirRow, p1Spacer, p1Req);
        pages[1] = p1;

        // ── Step 2: Select Additional Tasks ───────────────────────────────────
        VBox p2 = new VBox(12);
        p2.setAlignment(Pos.TOP_LEFT);

        Label p2Desc = new Label("Select the additional tasks you would like Setup to perform while installing " + APP_NAME + ", then click Next.");
        p2Desc.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px;");

        Label p2Group = new Label("Additional shortcuts:");
        p2Group.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px; -fx-font-weight: bold;");

        cbDesktopShortcut = new CheckBox("Create a desktop shortcut");
        cbDesktopShortcut.setSelected(true);
        cbDesktopShortcut.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px;");

        cbStartMenuShortcut = new CheckBox("Create a Start Menu shortcut");
        cbStartMenuShortcut.setSelected(true);
        cbStartMenuShortcut.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px;");

        p2.getChildren().addAll(p2Desc, p2Group, cbDesktopShortcut, cbStartMenuShortcut);
        pages[2] = p2;

        // ── Step 3: Ready to Install ──────────────────────────────────────────
        VBox p3 = new VBox(8);
        p3.setAlignment(Pos.TOP_LEFT);

        Label p3Desc = new Label("Click Install to continue with the installation, or click Back if you want to review or change any settings.");
        p3Desc.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px;");

        summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setStyle("-fx-control-inner-background: #ffffff; -fx-border-color: #7a7a7a; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 11.5px;");
        VBox.setVgrow(summaryArea, Priority.ALWAYS);

        p3.getChildren().addAll(p3Desc, summaryArea);
        pages[3] = p3;

        // ── Step 4: Installing ────────────────────────────────────────────────
        VBox p4 = new VBox(12);
        p4.setAlignment(Pos.CENTER_LEFT);

        Label p4Title = new Label("Installing");
        p4Title.setStyle("-fx-text-fill: #000000; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label p4Desc = new Label("Please wait while Setup installs " + APP_NAME + " on your computer.");
        p4Desc.setStyle("-fx-text-fill: #333333; -fx-font-size: 11.5px;");

        statusLabel = new Label("Extracting files...");
        statusLabel.setStyle("-fx-text-fill: #000000; -fx-font-size: 11px;");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #0078d7;");

        p4.getChildren().addAll(p4Title, p4Desc, statusLabel, progressBar);
        pages[4] = p4;

        // ── Step 5: Completing Setup ──────────────────────────────────────────
        HBox p5 = new HBox(16);
        p5.setAlignment(Pos.CENTER_LEFT);

        ImageView p5Logo = new ImageView();
        try {
            p5Logo.setImage(new Image(getClass().getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")));
            p5Logo.setFitWidth(64);
            p5Logo.setFitHeight(64);
            p5Logo.setPreserveRatio(true);
        } catch (Exception ignored) {}

        VBox p5Text = new VBox(14);
        p5Text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(p5Text, Priority.ALWAYS);

        Label p5Title = new Label("Completing the " + APP_NAME + " Setup Wizard");
        p5Title.setStyle("-fx-text-fill: #000000; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label p5Desc = new Label("Setup has finished installing " + APP_NAME + " on your computer. The application may be launched by selecting the installed shortcuts.");
        p5Desc.setWrapText(true);
        p5Desc.setStyle("-fx-text-fill: #333333; -fx-font-size: 11.5px; -fx-line-spacing: 2;");

        cbLaunchAfter = new CheckBox("Launch " + APP_NAME);
        cbLaunchAfter.setSelected(true);
        cbLaunchAfter.setStyle("-fx-text-fill: #000000; -fx-font-size: 11.5px;");

        p5Text.getChildren().addAll(p5Title, p5Desc, cbLaunchAfter);
        p5.getChildren().addAll(p5Logo, p5Text);
        pages[5] = p5;
    }

    private void goToStep(int step) {
        currentStep = step;
        bodyContainer.getChildren().clear();
        bodyContainer.getChildren().add(pages[step]);

        switch (step) {
            case 0 -> { // Information / License
                headerPane.setVisible(true);
                headerPane.setManaged(true);
                headerTitleLbl.setText("Information");
                headerSubtitleLbl.setText("Please read the following important information before continuing.");
                btnBack.setDisable(true);
                btnNext.setText("Next >");
                btnNext.setDisable(false);
                btnCancel.setVisible(true);
            }
            case 1 -> { // Select Destination
                headerPane.setVisible(true);
                headerPane.setManaged(true);
                headerTitleLbl.setText("Select Destination Location");
                headerSubtitleLbl.setText("Where should " + APP_NAME + " be installed?");
                btnBack.setDisable(false);
                btnNext.setText("Next >");
                btnNext.setDisable(false);
                btnCancel.setVisible(true);
            }
            case 2 -> { // Select Additional Tasks
                headerPane.setVisible(true);
                headerPane.setManaged(true);
                headerTitleLbl.setText("Select Additional Tasks");
                headerSubtitleLbl.setText("Which additional tasks should be performed?");
                btnBack.setDisable(false);
                btnNext.setText("Next >");
                btnNext.setDisable(false);
                btnCancel.setVisible(true);
            }
            case 3 -> { // Ready to Install
                headerPane.setVisible(true);
                headerPane.setManaged(true);
                headerTitleLbl.setText("Ready to Install");
                headerSubtitleLbl.setText("Setup is now ready to begin installing " + APP_NAME + " on your computer.");
                updateSummaryText();
                btnBack.setDisable(false);
                btnNext.setText("Install");
                btnNext.setDisable(false);
                btnCancel.setVisible(true);
            }
            case 4 -> { // Installing
                headerPane.setVisible(true);
                headerPane.setManaged(true);
                headerTitleLbl.setText("Installing");
                headerSubtitleLbl.setText("Please wait while Setup installs " + APP_NAME + " on your computer.");
                btnBack.setDisable(true);
                btnNext.setDisable(true);
                btnCancel.setDisable(true);
            }
            case 5 -> { // Finished
                headerPane.setVisible(false);
                headerPane.setManaged(false);
                btnBack.setVisible(false);
                btnCancel.setVisible(false);
                btnNext.setText("Finish");
                btnNext.setDisable(false);
            }
        }
    }

    private void handleBack() {
        if (currentStep > 0 && currentStep != 4 && currentStep != 5) {
            goToStep(currentStep - 1);
        }
    }

    private void handleNext(Stage stage) {
        if (currentStep == 0) {
            goToStep(1);
        } else if (currentStep == 1) {
            String dir = targetDirField.getText().trim();
            if (dir.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Please specify an installation directory.", ButtonType.OK);
                a.initOwner(stage);
                a.showAndWait();
                return;
            }
            goToStep(2);
        } else if (currentStep == 2) {
            goToStep(3);
        } else if (currentStep == 3) {
            goToStep(4);
            doInstall(stage);
        } else if (currentStep == 5) {
            stage.close();
            if (cbLaunchAfter.isSelected()) {
                launchInstalledApp(new File(targetDirField.getText().trim()));
            }
        }
    }

    private void updateSummaryText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Destination location:\n");
        sb.append("      ").append(targetDirField.getText().trim()).append("\n\n");
        sb.append("Additional tasks:\n");
        sb.append("      Additional shortcuts:\n");
        if (cbDesktopShortcut.isSelected()) {
            sb.append("            Create a desktop shortcut\n");
        }
        if (cbStartMenuShortcut.isSelected()) {
            sb.append("            Create a Start Menu shortcut\n");
        }
        summaryArea.setText(sb.toString());
    }

    private void doInstall(Stage stage) {
        File installDir = new File(targetDirField.getText().trim());
        boolean makeDesktop = cbDesktopShortcut.isSelected();
        boolean makeStart = cbStartMenuShortcut.isSelected();

        new Thread(() -> {
            try {
                updateProgress(0.15, "Creating destination directory...");
                if (!installDir.exists()) {
                    installDir.mkdirs();
                }

                updateProgress(0.35, "Extracting application components (AcadsCatchUp.jar)...");
                File destJar = new File(installDir, "AcadsCatchUp.jar");
                copyApplicationJar(destJar);

                updateProgress(0.55, "Extracting icons and launcher components...");
                File iconFile = new File(installDir, "app_icon.ico");
                copyIconFile(iconFile);

                File vbsLauncher = new File(installDir, "Launch_AcadsCatchUp.vbs");
                createVbsLauncher(vbsLauncher);

                File uninstallScript = new File(installDir, "uninstall.bat");
                createUninstallScript(uninstallScript);

                updateProgress(0.75, "Creating Windows shortcuts...");
                if (makeDesktop) {
                    createDesktopShortcut(installDir);
                }
                if (makeStart) {
                    createStartMenuShortcut(installDir);
                }

                updateProgress(0.90, "Registering in Windows Control Panel...");
                registerInWindowsControlPanel(installDir, destJar.length());

                updateProgress(1.0, "Finishing installation...");
                Thread.sleep(400);

                Platform.runLater(() -> goToStep(5));

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Installation failed: " + ex.getMessage(), ButtonType.OK);
                    err.initOwner(stage);
                    err.showAndWait();
                    btnCancel.setDisable(false);
                    goToStep(3);
                });
            }
        }, "Installer-Worker").start();
    }

    private void updateProgress(double p, String msg) {
        Platform.runLater(() -> {
            progressBar.setProgress(p);
            statusLabel.setText(msg);
        });
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
    }

    private String loadLicenseText() {
        // 1. Try classpath /LICENSE
        try (InputStream in = getClass().getResourceAsStream("/LICENSE")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        // 2. Try filesystem relative LICENSE
        File licFile = new File("LICENSE");
        if (licFile.exists()) {
            try {
                return Files.readString(licFile.toPath());
            } catch (Exception ignored) {}
        }

        // 3. Fallback standard GPL v3 header
        return "GNU GENERAL PUBLIC LICENSE\n" +
               "Version 3, 29 June 2007\n\n" +
               "Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>\n" +
               "Everyone is permitted to copy and distribute verbatim copies of this license document, but changing it is not allowed.\n\n" +
               "Preamble\n\n" +
               "The GNU General Public License is a free, copyleft license for software and other kinds of works.\n\n" +
               "Author: F4TAL (Stevenson James G. Gastanes)\n" +
               "Project: AcadsCatchUp - Academic Deficiency Tracking System\n" +
               "Repository: " + PROJECT_URL + "\n";
    }

    public static File getDefaultInstallDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return new File(localAppData, "Programs\\" + APP_NAME);
        }
        return new File(System.getProperty("user.home"), "AppData\\Local\\Programs\\" + APP_NAME);
    }

    public static void copyApplicationJar(File dest) throws IOException {
        // 1. Check current code source JAR
        try {
            File src = new File(AcadsCatchUpInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            if (src.isFile() && src.getName().endsWith(".jar") && src.length() > 500000) {
                if (src.getName().equalsIgnoreCase("AcadsCatchUp.jar")) {
                    Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
                if (src.getParentFile() != null) {
                    File sibling = new File(src.getParentFile(), "AcadsCatchUp.jar");
                    if (sibling.exists() && sibling.length() > 500000) {
                        Files.copy(sibling.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        return;
                    }
                }
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        } catch (Exception ignored) {}

        // 2. Check embedded resource payload
        try (InputStream in = AcadsCatchUpInstaller.class.getResourceAsStream("/payload/AcadsCatchUp.jar")) {
            if (in != null) {
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        } catch (Exception ignored) {}

        // 3. Fallback: search relative dist/ folder
        File distJar = new File("dist/AcadsCatchUp.jar");
        if (distJar.exists()) {
            Files.copy(distJar.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        File currentJar = new File("AcadsCatchUp.jar");
        if (currentJar.exists()) {
            Files.copy(currentJar.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        throw new FileNotFoundException("AcadsCatchUp.jar could not be located for installation.");
    }

    public static void copyIconFile(File dest) throws IOException {
        try {
            File src = new File(AcadsCatchUpInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            if (src.getParentFile() != null) {
                File siblingIcon = new File(src.getParentFile(), "app_icon.ico");
                if (siblingIcon.exists()) {
                    Files.copy(siblingIcon.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        } catch (Exception ignored) {}

        File distIcon = new File("dist/app_icon.ico");
        if (distIcon.exists()) {
            Files.copy(distIcon.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        try (InputStream in = AcadsCatchUpInstaller.class.getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")) {
            if (in != null) {
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static void createVbsLauncher(File vbsFile) throws IOException {
        String vbs = "Set WshShell = CreateObject(\"WScript.Shell\")\r\n" +
                     "WshShell.Run \"javaw -jar \"\"\" & Replace(WScript.ScriptFullName, \"Launch_AcadsCatchUp.vbs\", \"AcadsCatchUp.jar\") & \"\"\"\", 0, False\r\n";
        Files.writeString(vbsFile.toPath(), vbs);
    }

    public static void createUninstallScript(File batFile) throws IOException {
        String bat = "@echo off\r\n" +
                     "start javaw -jar \"%~dp0AcadsCatchUp.jar\" --uninstall %*\r\n";
        Files.writeString(batFile.toPath(), bat);
    }

    public static void createDesktopShortcut(File installDir) {
        File target = new File(installDir, "Launch_AcadsCatchUp.vbs");
        File icon = new File(installDir, "app_icon.ico");
        String desktop = System.getProperty("user.home") + "\\Desktop\\AcadsCatchUp.lnk";
        createWindowsShortcut(desktop, target.getAbsolutePath(), icon.getAbsolutePath(), installDir.getAbsolutePath(), "AcadsCatchUp Academic Tracker");
    }

    public static void createStartMenuShortcut(File installDir) {
        String appData = System.getenv("APPDATA");
        if (appData == null) return;
        File startFolder = new File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\" + APP_NAME);
        if (!startFolder.exists()) startFolder.mkdirs();

        File target = new File(installDir, "Launch_AcadsCatchUp.vbs");
        File icon = new File(installDir, "app_icon.ico");
        String shortcut = new File(startFolder, APP_NAME + ".lnk").getAbsolutePath();
        createWindowsShortcut(shortcut, target.getAbsolutePath(), icon.getAbsolutePath(), installDir.getAbsolutePath(), "AcadsCatchUp Academic Tracker");
    }

    public static void createWindowsShortcut(String shortcutPath, String targetPath, String iconPath, String workDir, String description) {
        try {
            String psCmd = String.format(
                    "$ws = New-Object -ComObject WScript.Shell; " +
                    "$s = $ws.CreateShortcut('%s'); " +
                    "$s.TargetPath = '%s'; " +
                    "$s.IconLocation = '%s,0'; " +
                    "$s.WorkingDirectory = '%s'; " +
                    "$s.Description = '%s'; " +
                    "$s.Save()",
                    shortcutPath.replace("'", "''"),
                    targetPath.replace("'", "''"),
                    iconPath.replace("'", "''"),
                    workDir.replace("'", "''"),
                    description.replace("'", "''")
            );
            new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCmd).start().waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerInWindowsControlPanel(File installDir, long jarSizeBytes) {
        String key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\" + APP_NAME;
        String uninstallCmd = "cmd.exe /c \"\"" + installDir.getAbsolutePath() + "\\uninstall.bat\"\"";
        String quietUninstallCmd = "cmd.exe /c \"\"" + installDir.getAbsolutePath() + "\\uninstall.bat\" --silent\"";
        String iconPath = installDir.getAbsolutePath() + "\\app_icon.ico";
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sizeKb = jarSizeBytes / 1024;

        execRegAdd(key, "DisplayName", "REG_SZ", APP_NAME);
        execRegAdd(key, "DisplayVersion", "REG_SZ", VERSION);
        execRegAdd(key, "Publisher", "REG_SZ", PUBLISHER);
        execRegAdd(key, "DisplayIcon", "REG_SZ", iconPath);
        execRegAdd(key, "InstallLocation", "REG_SZ", installDir.getAbsolutePath());
        execRegAdd(key, "UninstallString", "REG_SZ", uninstallCmd);
        execRegAdd(key, "QuietUninstallString", "REG_SZ", quietUninstallCmd);
        execRegAdd(key, "EstimatedSize", "REG_DWORD", String.valueOf(sizeKb));
        execRegAdd(key, "InstallDate", "REG_SZ", today);
        execRegAdd(key, "HelpLink", "REG_SZ", PROJECT_URL);
        execRegAdd(key, "URLInfoAbout", "REG_SZ", PROJECT_URL);
        execRegAdd(key, "NoModify", "REG_DWORD", "1");
        execRegAdd(key, "NoRepair", "REG_DWORD", "1");
    }

    private static void execRegAdd(String key, String valueName, String type, String data) {
        try {
            new ProcessBuilder("reg.exe", "add", key, "/v", valueName, "/t", type, "/d", data, "/f")
                    .start()
                    .waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void launchInstalledApp(File installDir) {
        try {
            File vbs = new File(installDir, "Launch_AcadsCatchUp.vbs");
            if (vbs.exists()) {
                new ProcessBuilder("wscript.exe", vbs.getAbsolutePath()).start();
            } else {
                File jar = new File(installDir, "AcadsCatchUp.jar");
                new ProcessBuilder("javaw", "-jar", jar.getAbsolutePath()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runSilentInstall() {
        try {
            File installDir = getDefaultInstallDir();
            if (!installDir.exists()) installDir.mkdirs();

            File destJar = new File(installDir, "AcadsCatchUp.jar");
            copyApplicationJar(destJar);

            File iconFile = new File(installDir, "app_icon.ico");
            copyIconFile(iconFile);

            File vbsLauncher = new File(installDir, "Launch_AcadsCatchUp.vbs");
            createVbsLauncher(vbsLauncher);

            File uninstallScript = new File(installDir, "uninstall.bat");
            createUninstallScript(uninstallScript);

            createDesktopShortcut(installDir);
            createStartMenuShortcut(installDir);
            registerInWindowsControlPanel(installDir, destJar.length());

            System.out.println(APP_NAME + " successfully installed to: " + installDir.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Silent install error: " + e.getMessage());
            System.exit(1);
        }
    }
}
