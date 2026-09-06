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
        currentBadge.setMinWidth(Region.USE_PREF_SIZE);
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

        // ── 2. VERSION SELECTOR BAR (FlowPane with wrap & fixed pref size) ──
        FlowPane versionBar = new FlowPane();
        versionBar.setAlignment(Pos.CENTER_LEFT);
        versionBar.setHgap(8);
        versionBar.setVgap(6);
        versionBar.setStyle(
                "-fx-background-color: #121520; " +
                "-fx-padding: 10 22; " +
                "-fx-border-color: #2d3255; " +
                "-fx-border-width: 0 0 1 0;"
        );

        Label selectLbl = new Label("Select Version:");
        selectLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        selectLbl.setMinWidth(Region.USE_PREF_SIZE);

        versionBar.getChildren().add(selectLbl);

        // Content Area
        VBox contentBox = new VBox(14);
        contentBox.setPadding(new Insets(18, 22, 18, 22));

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1a1d2e; -fx-border-color: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Version definitions
        String[] versions = new String[] { "v1.0.9", "v1.0.8", "v1.0.7", "v1.0.6", "v1.0.5", "v1.0.4", "v1.0.3", "v1.0.2", "v1.0.1", "v1.0.0" };
        final String[] currentSelectedVersion = new String[] { "v1.0.9" };
        Map<String, Button> versionButtons = new HashMap<>();

        for (String v : versions) {
            String labelText = v.equals("v1.0.9") ? "✨ v1.0.9 (Latest)" : v;
            Button vBtn = new Button(labelText);
            vBtn.setMinWidth(Region.USE_PREF_SIZE);
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
                currentSelectedVersion[0] = v;
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

        // Default to v1.0.9
        Button defaultBtn = versionButtons.get("v1.0.9");
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
            renderVersionNotes(contentBox, "v1.0.9");
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
        double screenW = (owner != null && owner.getScene() != null) ? owner.getScene().getWidth() : 800;
        double screenH = (owner != null && owner.getScene() != null) ? owner.getScene().getHeight() : 700;
        double modalW = Math.min(840, Math.max(620, screenW * 0.85));
        double modalH = Math.min(680, Math.max(520, screenH * 0.85));
        ModalOverlay.showAndWait(anchor, root, modalW, modalH);
    }

    /**
     * Renders release notes for the selected version into the container.
     */
    private static void renderVersionNotes(VBox container, String version) {
        container.getChildren().clear();

        switch (version) {
            case "v1.0.9":
                renderV109(container);
                break;
            case "v1.0.8":
                renderV108(container);
                break;
            case "v1.0.7":
                renderV107(container);
                break;
            case "v1.0.6":
                renderV106(container);
                break;
            case "v1.0.5":
                renderV105(container);
                break;
            case "v1.0.4":
                renderV104(container);
                break;
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
                renderV109(container);
                break;
        }
    }

    private static void renderV109(VBox c) {
        // Banner Card
        VBox banner = createBannerCard(
                "🚀 AcadsCatchUp v1.0.9 — Enrollment Alerts & Responsive Modal Polish",
                "Release Date: September 2026 • Build: v1.0.9-PROD-F4TAL",
                "CURRENT INSTALLED VERSION",
                "#23a55a"
        );

        // Feature Sections
        VBox features = createSectionCard("✨ Highlights & New Features", new String[]{
                "Student Enrollment Notifications: When an instructor or admin enrolls a student in a subject, the student immediately receives a live desktop tray notification and an official enrollment message delivered to their personal Inbox.",
                "Enrolled Subjects Background Sync: LiveSync automatically detects subject enrollments and refreshes the student's Enrolled Subjects overview chips in real time.",
                "Responsive Updates Modal: Completely redesigned the Updates & What's New dialog with dynamic resolution scaling and flexible wrapping version selector buttons.",
                "Streamlined Updates Hub: Removed obsolete manual inbox sending button in favor of a clean, dedicated 1-click update experience."
        });

        VBox improvements = createSectionCard("⚡ Improvements & Synergy", new String[]{
                "Adaptive Card Geometry: Dialog banners and badges now intelligently wrap and preserve fixed sizes across all display dimensions.",
                "Robust Offline Delivery: Enrollment notices persist in the database so students receiving enrollments while offline receive desktop alerts immediately upon login."
        });

        c.getChildren().addAll(banner, features, improvements);
    }

    private static void renderV108(VBox c) {
        // Banner Card
        VBox banner = createBannerCard(
                "🚀 AcadsCatchUp v1.0.8 — Student Reactive Search & Stat Card Synergy",
                "Release Date: September 2026 • Build: v1.0.8-PROD-F4TAL",
                "PREVIOUS RELEASE",
                "#64748b"
        );

        // Feature Sections
        VBox features = createSectionCard("✨ Highlights & New Features", new String[]{
                "Student Real-Time Reactive Search: Instant zero-latency search filtering directly on the Student Dashboard. Search through item names, subject codes, professors, deadlines, notes, and statuses as you type.",
                "Interactive Dashboard Stat Cards: Stat cards on both Student Dashboard and Professor Dashboard now feature hand cursors and 1-click status filtering (Total, Pending, Submitted, Graded).",
                "Student Double-Click Row Shortcut: Double-click any row in your deficiency checklist to immediately open the submission dialog.",
                "Export Deficiency Checklist to CSV: Students can now export their personalized missed item checklist to CSV for offline reference and submission records.",
                "Enhanced CSV Export with Professor Info: Exported deficiency CSV records now include the assigned Professor Name for comprehensive academic tracking.",
                "Seamless Windows Auto-Updater Overhaul: Completely revamped file-locking handoff with PID process termination and retry copy loop for 100% reliable in-app updates."
        });

        VBox improvements = createSectionCard("⚡ Improvements & Fixes", new String[]{
                "Zero-Latency FilteredList Architecture: In-memory reactive filtering eliminates database overhead on every keystroke.",
                "OpenCSV Export Robustness: Clean formatting, null-safe string mappings, and direct FileChooser integration.",
                "Cross-Platform UI Polish: Hand cursor tooltips and smooth responsive auto-scaling across diverse screen resolutions."
        });

        c.getChildren().addAll(banner, features, improvements);
    }

    private static void renderV107(VBox c) {
        // Banner Card
        VBox banner = createBannerCard(
                "🚀 AcadsCatchUp v1.0.7 — Admin Dashboard Fixes & Reactive Filtering",
                "Release Date: September 2026 • Build: v1.0.7-PROD-F4TAL",
                "PREVIOUS RELEASE",
                "#64748b"
        );

        // Feature Sections
        VBox features = createSectionCard("✨ Highlights & New Features", new String[]{
                "Admin Dashboard Filter Engine Overhaul: Fixed predicate logic where non-student accounts previously bypassed program and year filters. Selecting any program or year level now accurately isolates student accounts.",
                "Real-Time Reactive Search: Instant filtering as you type, paste, or clear text in the search box, with multi-attribute matching across username, full name, email, program, role, year level, and professor assigned subjects.",
                "Interactive Dashboard Stat Cards: Added hand pointer cursors and 1-click shortcut navigation directly from Total Users, Students, Professors, Subjects, and Reports cards.",
                "Table Row Double-Click & Single Checkbox Editing: Double-click any user row in the table to immediately open the Edit Account modal, or select a single user checkbox and click Edit directly.",
                "Header Select-All Polish: Deselects only visible filtered rows and disables automatically when results are empty."
        });

        VBox improvements = createSectionCard("⚡ Improvements & Synergy", new String[]{
                "Smart Filter Synergy: Coordinated Role, Program, and Year filters that disable irrelevant inputs when Admin or Professor roles are active.",
                "Seamless LiveSync Compatibility: Live updates and silent refresh preserve active user filters, selections, and search queries.",
                "Robust UI Synchronization: Dynamic stat label badge tooltips and responsive desktop layout adjustments across all resolutions."
        });

        c.getChildren().addAll(banner, features, improvements);
    }

    private static void renderV106(VBox c) {
        // Banner Card
        VBox banner = createBannerCard(
                "🚀 AcadsCatchUp v1.0.6 — Clean Settings & Centralized Updates",
                "Release Date: September 2026 • Build: v1.0.6-PROD-F4TAL",
                "PREVIOUS RELEASE",
                "#64748b"
        );

        // Feature Sections
        VBox features = createSectionCard("✨ Highlights & New Features", new String[]{
                "Streamlined Settings Experience: Removed redundant manual update check button from Account & Security Settings, cleanly centralizing all update operations inside the dedicated Updates Hub.",
                "Live Updates Version Badging: Automatic background checks alert you on the dashboard with an amber '🔄 Updates (New!)' badge when a new GitHub release is available.",
                "Live Download Progress Dialog: Manual updates now display real-time MB transfer counts, percentage completion, and status in a dedicated modal.",
                "Download Integrity Verification: Automated 95%+ size integrity check protects your application from partial or corrupt binary downloads.",
                "Admin Inbox & Update Notifications: System Administrators now receive update release notices and can open their personal Inbox with 1 click from Bug Reports."
        });

        VBox improvements = createSectionCard("⚡ Improvements & Optimization", new String[]{
                "Extended Network Timeouts: GitHub API timeout increased to 6.0s and download timeout to 90.0s for maximum stability on all network speeds.",
                "Duplicate Release Note Protection: Enhanced 'Send to My Inbox' guard prevents duplicate release notes from cluttering your personal inbox.",
                "Anti-Cache Manifest Retrieval: Direct-to-GitHub query parameters ensure zero CDN stale caching for instant release discovery."
        });

        c.getChildren().addAll(banner, features, improvements);
    }

    private static void renderV105(VBox c) {
        // Banner Card
        VBox banner = createBannerCard(
                "📦 AcadsCatchUp v1.0.5 — Inbox What's New & Release Delivery",
                "Release Date: September 2026 • Build: v1.0.5-PROD-F4TAL",
                "ARCHIVED RELEASE",
                "#6366f1"
        );

        // Feature Sections
        VBox features = createSectionCard("✨ Highlights & New Features", new String[]{
                "Inbox 'What's New' Notifications: Official update announcements and changelogs are now pushed straight to your personal Inbox, complete with unread count badges and desktop notifications.",
                "Anchor-Based Portable Detection: Smart directory resolver uses structural file anchors (AcadsCatchUp.exe, app/AcadsCatchUp.cfg, runtime/) to locate portable folders anywhere on your drives.",
                "Dual-Path Execution Sync: Synchronizes both root and internal app/ JARs (AcadsCatchUp.jar & acadscatchup-app.jar) to guarantee AcadsCatchUp.exe always loads the latest update.",
                "Resilient Direct-to-Login Updater: Sub-second background process release bypasses Windows JVM open-file locks and immediately opens your Login workspace with '--direct-login'."
        });

        VBox improvements = createSectionCard("⚡ Improvements & Optimization", new String[]{
                "On-Demand Inbox Delivery: Send any version's changelog directly to your personal Inbox using the new 'Send to My Inbox' button.",
                "Cross-Platform System Tray & Badging: Instant tray toast notifications and unread badges across Student and Professor dashboards.",
                "DeveloperGuard 100% Compliant: Full signature integrity verified across all project classes."
        });

        c.getChildren().addAll(banner, features, improvements);
    }

    private static void renderV104(VBox c) {
        // Banner Card
        VBox banner = createBannerCard(
                "📦 AcadsCatchUp v1.0.4 — Anchor Detection & Dual-Path Sync",
                "Release Date: September 2026 • Build: v1.0.4-PROD-F4TAL",
                "ARCHIVED RELEASE",
                "#6366f1"
        );

        // Feature Sections
        VBox features = createSectionCard("✨ Highlights & New Features", new String[]{
                "Anchor-Based Portable Folder Detection: Smart directory resolver inspects folder contents (AcadsCatchUp.exe, app/AcadsCatchUp.cfg, runtime/) to locate the portable environment accurately, even when renamed or moved.",
                "Dual-Path Execution Sync: Synchronizes both the root AcadsCatchUp.jar and app/AcadsCatchUp.jar / acadscatchup-app.jar to guarantee native jpackage launcher (AcadsCatchUp.exe) always loads the latest update.",
                "Direct-to-Login Handoff: Auto-updater transitions directly into the Login screen upon update without showing duplicate splash screens or restart dialogs.",
                "Resilient Windows File-Lock Handoff: Sub-second background handoff script safely replaces locked binaries on Windows without access-denied errors on active processes."
        });

        VBox improvements = createSectionCard("⚡ Improvements & Optimization", new String[]{
                "Zero-Restart User Experience: Seamlessly finishes binary synchronization and opens the login workspace directly.",
                "Multi-Drive Workspace Resolver: Searches local runtime, current directory, Downloads, Desktop, and OneDrive locations.",
                "DeveloperGuard 100% Compliant: Full signature integrity verified across all project classes."
        });

        c.getChildren().addAll(banner, features, improvements);
    }

    private static void renderV103(VBox c) {
        // Banner Card
        VBox banner = createBannerCard(
                "📦 AcadsCatchUp v1.0.3 — Updates Hub & Auto-Update Engine",
                "Release Date: September 2026 • Build: v1.0.3-PROD-F4TAL",
                "ARCHIVED RELEASE",
                "#6366f1"
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

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14.5px; -fx-font-weight: 800;");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label badge = new Label(badgeText);
        badge.setMinWidth(Region.USE_PREF_SIZE);
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

        topRow.getChildren().addAll(titleLbl, badge);

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        subLbl.setWrapText(true);

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
