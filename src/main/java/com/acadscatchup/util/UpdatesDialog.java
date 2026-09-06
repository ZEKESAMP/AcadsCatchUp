package com.acadscatchup.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Map;

/**
 * Updates & What's New Dialog for AcadsCatchUp.
 * Provides an interactive in-window modal displaying version changelogs,
 * release notes, and version switcher buttons.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class UpdatesDialog {

    public static final String DEVELOPER = "F4TAL";

    /**
     * Displays the Updates & What's New modal window.
     */
    public static void show(Window owner) {
        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: #1a1d2e; " +
                "-fx-background-radius: 14; " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 24, 0, 0, 8);"
        );

        // ── 1. HEADER ────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: #151825; " +
                "-fx-padding: 16 22; " +
                "-fx-background-radius: 14 14 0 0; " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-width: 0 0 1 0;"
        );

        Label iconLbl = new Label("🚀");
        iconLbl.setStyle("-fx-font-size: 26px;");

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Updates & What's New");
        titleLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16.5px; -fx-font-weight: 800; -fx-font-family: 'Segoe UI', sans-serif;");

        Label subTitleLbl = new Label("ACADSCATCHUP RELEASE NOTES & CHANGELOG • CURRENT: v" + UpdateSplash.CURRENT_VERSION);
        subTitleLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-letter-spacing: 0.6px;");
        titleBox.getChildren().addAll(titleLbl, subTitleLbl);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label currentBadge = new Label("🟢 v" + UpdateSplash.CURRENT_VERSION + " Installed");
        currentBadge.setStyle(
                "-fx-background-color: rgba(35, 165, 90, 0.15); " +
                "-fx-text-fill: #23a55a; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 4 10; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: #23a55a; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 1;"
        );

        header.getChildren().addAll(iconLbl, titleBox, headerSpacer, currentBadge);

        // ── 2. VERSION SELECTOR BAR (Buttons) ────────────────────────────────
        HBox versionBar = new HBox(8);
        versionBar.setAlignment(Pos.CENTER_LEFT);
        versionBar.setStyle(
                "-fx-background-color: #121520; " +
                "-fx-padding: 10 22; " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-width: 0 0 1 0;"
        );

        Label selectLbl = new Label("Select Version:");
        selectLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-font-weight: bold;");

        versionBar.getChildren().add(selectLbl);

        // Content Area
        VBox contentBox = new VBox(14);
        contentBox.setPadding(new Insets(18, 22, 18, 22));

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1a1d2e; -fx-border-color: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Version definitions
        String[] versions = new String[] { "v1.0.3", "v1.0.2", "v1.0.1", "v1.0.0" };
        Map<String, Button> versionButtons = new HashMap<>();

        for (String v : versions) {
            String labelText = v.equals("v1.0.3") ? "✨ v1.0.3 (Latest)" : v;
            Button vBtn = new Button(labelText);
            vBtn.setStyle(
                    "-fx-background-color: #242840; " +
                    "-fx-text-fill: #94a3b8; " +
                    "-fx-font-size: 11.5px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 6 12; " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-color: #3b4267; " +
                    "-fx-border-radius: 6; " +
                    "-fx-border-width: 1; " +
                    "-fx-cursor: hand;"
            );

            vBtn.setOnAction(e -> {
                // Reset all buttons style
                for (Map.Entry<String, Button> entry : versionButtons.entrySet()) {
                    entry.getValue().setStyle(
                            "-fx-background-color: #242840; " +
                            "-fx-text-fill: #94a3b8; " +
                            "-fx-font-size: 11.5px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12; " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #3b4267; " +
                            "-fx-border-radius: 6; " +
                            "-fx-border-width: 1; " +
                            "-fx-cursor: hand;"
                    );
                }
                // Highlight selected button
                vBtn.setStyle(
                        "-fx-background-color: #5865f2; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-font-size: 11.5px; " +
                        "-fx-font-weight: 800; " +
                        "-fx-padding: 6 12; " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-color: #7289da; " +
                        "-fx-border-radius: 6; " +
                        "-fx-border-width: 1; " +
                        "-fx-cursor: hand;"
                );

                renderVersionNotes(contentBox, v);
            });

            versionButtons.put(v, vBtn);
            versionBar.getChildren().add(vBtn);
        }

        // Default to v1.0.3
        Button defaultBtn = versionButtons.get("v1.0.3");
        if (defaultBtn != null) {
            defaultBtn.setStyle(
                    "-fx-background-color: #5865f2; " +
                    "-fx-text-fill: #ffffff; " +
                    "-fx-font-size: 11.5px; " +
                    "-fx-font-weight: 800; " +
                    "-fx-padding: 6 12; " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-color: #7289da; " +
                    "-fx-border-radius: 6; " +
                    "-fx-border-width: 1; " +
                    "-fx-cursor: hand;"
            );
            renderVersionNotes(contentBox, "v1.0.3");
        }

        // ── 3. FOOTER ────────────────────────────────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
                "-fx-background-color: #151825; " +
                "-fx-padding: 12 22; " +
                "-fx-background-radius: 0 0 14 14; " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-width: 1 0 0 0;"
        );

        Button btnCheckUpdate = new Button("🔄 Check for Updates");
        btnCheckUpdate.getStyleClass().add("btn-ghost");
        btnCheckUpdate.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 14;");
        btnCheckUpdate.setOnAction(e -> UpdateSplash.checkManual(owner));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button doneBtn = new Button("Close");
        doneBtn.getStyleClass().add("btn-primary");
        doneBtn.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 18;");
        doneBtn.setOnAction(e -> ModalOverlay.close(doneBtn));

        footer.getChildren().addAll(btnCheckUpdate, footerSpacer, doneBtn);

        root.getChildren().addAll(header, versionBar, scrollPane, footer);

        Node anchor = (owner != null && owner.getScene() != null) ? owner.getScene().getRoot() : null;
        ModalOverlay.showAndWait(anchor, root, 640, 600);
    }

    /**
     * Renders release notes for the selected version into the container.
     */
    private static void renderVersionNotes(VBox container, String version) {
        container.getChildren().clear();

        switch (version) {
            case "v1.0.3":
                renderV103(container);
                break;
            case "v1.0.2":
                renderV102(container);
                break;
            case "v1.0.1":
                renderV101(container);
                break;
            case "v1.0.0":
                renderV100(container);
                break;
            default:
                renderV103(container);
                break;
        }
    }

    private static void renderV103(VBox c) {
        // Banner Card
        VBox banner = createBannerCard(
                "🚀 AcadsCatchUp v1.0.3 — What's New",
                "Release Date: September 2026 • Build: v1.0.3-PROD-F4TAL",
                "CURRENT INSTALLED VERSION",
                "#23a55a"
        );

        // Feature Sections
        VBox features = createSectionCard("✨ Highlights & New Features", new String[]{
                "In-App Updates Hub: View what's new, release notes, and version history directly from Student, Professor, and Admin dashboards via the new 'Updates' button.",
                "Seamless Auto-Updater (Zero Restart): Downloads new .jar files directly into your Downloads folder, automatically updates AcadsCatchUp-Portable, and opens the Login phase instantly without restarting.",
                "Enrolled Subjects Active Filter Badge: Student dashboard enrolled subjects now feature an interactive '🎯 Selected in Filter' badge and cyan border glow when selected in the filter dropdown below.",
                "One-Click Manual Check: Check and apply new updates directly from the Updates modal or Account Settings without restarting your session."
        });

        VBox improvements = createSectionCard("⚡ Improvements & Optimization", new String[]{
                "Windows File-Lock Elimination: Streamlined the update hand-off so active processes are never forcibly terminated by external batch loops.",
                "Instant Offline Fallback: If no internet connection is detected, the app displays 'No update' and loads immediately with zero hang.",
                "Refined Dashboard Top Bar: 'Updates' button placed alongside 'Settings' across all role views for effortless navigation.",
                "DeveloperGuard 100% Compliant: Full signature integrity verified across all 49 project classes."
        });

        c.getChildren().addAll(banner, features, improvements);
    }

    private static void renderV102(VBox c) {
        VBox banner = createBannerCard(
                "📦 AcadsCatchUp v1.0.2 — Splash Updater & Portable Release",
                "Release Date: September 2026 • Build: v1.0.2-PROD-F4TAL",
                "ARCHIVED RELEASE",
                "#6366f1"
        );

        VBox features = createSectionCard("✨ Key Additions", new String[]{
                "Discord-Style Frameless Splash Screen: Modern startup splash with live download percentage and MB progress indicator.",
                "View-Only Enrolled Subjects: Made enrolled subject badges on Student Dashboard view-only to prevent unintended filter toggles.",
                "Standalone Portable Architecture: Integrated self-contained JRE packaging in AcadsCatchUp-Portable folder."
        });

        c.getChildren().addAll(banner, features);
    }

    private static void renderV101(VBox c) {
        VBox banner = createBannerCard(
                "🔧 AcadsCatchUp v1.0.1 — Cross-Platform OS & Cloud Sync",
                "Release Date: September 2026 • Build: v1.0.1-PROD-F4TAL",
                "ARCHIVED RELEASE",
                "#64748b"
        );

        VBox features = createSectionCard("✨ Key Additions", new String[]{
                "Cross-Platform OS Compatibility: Native emoji rendering on Windows and Segoe UI modern typography fallback.",
                "SQLite Cloud Live Sync: Real-time background sync indicators ('🟢 Live Sync') on all headers.",
                "Discord-Style System Tray: Minimizes to system tray with quick-action context menu."
        });

        c.getChildren().addAll(banner, features);
    }

    private static void renderV100(VBox c) {
        VBox banner = createBannerCard(
                "🎉 AcadsCatchUp v1.0.0 — Initial Official Release",
                "Release Date: August 2026 • Build: v1.0.0-PROD-F4TAL",
                "FOUNDATION RELEASE",
                "#64748b"
        );

        VBox features = createSectionCard("✨ Foundation Features", new String[]{
                "Role-Based Dashboards: Dedicated interfaces for Students, Professors, and System Administrators.",
                "Academic Deficiency Tracking: Complete workflow for tracking pending, submitted, and completed requirements.",
                "Direct File Submission: Upload assignments, files, and remarks directly with professor review queues.",
                "Secure Authentication: Role protection, password hashing, and registered Gmail OTP verification."
        });

        c.getChildren().addAll(banner, features);
    }

    private static VBox createBannerCard(String title, String subtitle, String badgeText, String badgeColor) {
        VBox banner = new VBox(6);
        banner.setStyle(
                "-fx-background-color: #121520; " +
                "-fx-padding: 14 18; " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10;"
        );

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: 800;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label badge = new Label(badgeText);
        badge.setStyle(
                "-fx-background-color: " + badgeColor + "22; " +
                "-fx-text-fill: " + badgeColor + "; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 3 8; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: " + badgeColor + "; " +
                "-fx-border-radius: 10; " +
                "-fx-border-width: 1;"
        );

        topRow.getChildren().addAll(titleLbl, sp, badge);

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        banner.getChildren().addAll(topRow, subLbl);
        return banner;
    }

    private static VBox createSectionCard(String heading, String[] bullets) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.4); " +
                "-fx-padding: 14 18; " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10;"
        );

        Label headingLbl = new Label(heading);
        headingLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox list = new VBox(6);
        for (String bullet : bullets) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.TOP_LEFT);

            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #60a5fa; -fx-font-weight: bold; -fx-font-size: 13px;");

            Label text = new Label(bullet);
            text.setWrapText(true);
            text.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-line-spacing: 2px;");
            HBox.setHgrow(text, Priority.ALWAYS);

            row.getChildren().addAll(dot, text);
            list.getChildren().add(row);
        }

        card.getChildren().addAll(headingLbl, list);
        return card;
    }
}
