package com.acadscatchup.util;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * Modern auto-scaling and responsive layout manager for AcadsCatchUp dashboards.
 * Dynamically scales UI elements, compacts buttons and badges, adjusts paddings,
 * and locks scroll viewports to prevent horizontal clipping when resizing or switching resolutions.
 *
 * Modeled after modern responsive IDEs (e.g. VS Code / Antigravity).
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class ResponsiveLayoutUtil {

    public static final String DEVELOPER = "F4TAL";

    // Breakpoint definitions
    public static final double BREAKPOINT_COMPACT = 860.0;
    public static final double BREAKPOINT_MEDIUM  = 1080.0;

    /**
     * Installs intelligent auto-scaling responsiveness for the Student Dashboard.
     */
    public static void installStudentResponsiveLayout(
            ScrollPane scrollPane,
            HBox topBar,
            Label appTitle,
            Label roleBadge,
            Label syncBadge,
            Button inboxBtn,
            Button helpBtn,
            Button settingsBtn,
            Label studentNameLabel,
            Button logoutBtn,
            HBox statsRow
    ) {
        if (scrollPane != null) {
            // Absolute horizontal lock: prevents the dashboard from ever scrolling off-screen to the left!
            scrollPane.setFitToWidth(true);
            scrollPane.setPannable(false);
            scrollPane.hvalueProperty().addListener((obs, oldV, newV) -> {
                if (newV.doubleValue() != 0) {
                    Platform.runLater(() -> scrollPane.setHvalue(0));
                }
            });
        }

        // Install tooltips so compact buttons remain 100% self-explanatory
        if (helpBtn != null && helpBtn.getTooltip() == null) {
            helpBtn.setTooltip(new Tooltip("Help / Bug Report"));
        }
        if (settingsBtn != null && settingsBtn.getTooltip() == null) {
            settingsBtn.setTooltip(new Tooltip("Account & Security Settings"));
        }
        if (logoutBtn != null && logoutBtn.getTooltip() == null) {
            logoutBtn.setTooltip(new Tooltip("Logout of AcadsCatchUp"));
        }

        // Hook into Scene width changes
        Platform.runLater(() -> {
            if (topBar == null || topBar.getScene() == null) return;
            Scene scene = topBar.getScene();

            ChangeListener<Number> widthListener = (obs, oldW, newW) -> {
                double w = newW.doubleValue();
                applyStudentBreakpoints(w, roleBadge, syncBadge, inboxBtn, helpBtn, settingsBtn, studentNameLabel, logoutBtn, topBar, statsRow);
            };

            scene.widthProperty().addListener(widthListener);
            // Apply immediately based on current width
            applyStudentBreakpoints(scene.getWidth(), roleBadge, syncBadge, inboxBtn, helpBtn, settingsBtn, studentNameLabel, logoutBtn, topBar, statsRow);
        });
    }

    private static void applyStudentBreakpoints(
            double w,
            Label roleBadge,
            Label syncBadge,
            Button inboxBtn,
            Button helpBtn,
            Button settingsBtn,
            Label studentNameLabel,
            Button logoutBtn,
            HBox topBar,
            HBox statsRow
    ) {
        if (w <= 0) return;

        if (w < BREAKPOINT_COMPACT) {
            // ── COMPACT MODE (< 860px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 8 12; -fx-spacing: 6;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(false);
                roleBadge.setManaged(false);
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢"));
                syncBadge.setTooltip(new Tooltip("Live Cloud Sync Active"));
            }
            if (helpBtn != null) {
                helpBtn.setText(OSCompat.label("💬"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText(OSCompat.label("🚪"));
            }
            if (studentNameLabel != null) {
                studentNameLabel.setMaxWidth(110);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 10 14 8 14; -fx-spacing: 8;");
            }
        } else if (w < BREAKPOINT_MEDIUM) {
            // ── MEDIUM MODE (860px - 1080px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 10 16; -fx-spacing: 10;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(true);
                roleBadge.setManaged(true);
                roleBadge.setText("Student");
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢 Sync"));
                syncBadge.setTooltip(new Tooltip("Live Real-Time Sync"));
            }
            if (helpBtn != null) {
                helpBtn.setText(OSCompat.label("💬 Help"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙ Settings"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText("Logout");
            }
            if (studentNameLabel != null) {
                studentNameLabel.setMaxWidth(150);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 14 18 12 18; -fx-spacing: 12;");
            }
        } else {
            // ── FULL WIDE MODE (>= 1080px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 12 22; -fx-spacing: 14;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(true);
                roleBadge.setManaged(true);
                roleBadge.setText("Student View");
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢 Live Sync"));
            }
            if (helpBtn != null) {
                helpBtn.setText(OSCompat.label("💬 Help / Bug Report"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙ Settings"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText("Logout");
            }
            if (studentNameLabel != null) {
                studentNameLabel.setMaxWidth(220);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 20 24 16 24; -fx-spacing: 16;");
            }
        }
    }

    /**
     * Installs intelligent auto-scaling responsiveness for the Professor / Admin Dashboard.
     */
    public static void installProfResponsiveLayout(
            ScrollPane scrollPane,
            HBox topBar,
            Label appTitle,
            Label roleBadge,
            Label syncBadge,
            Button adminInboxBtn,
            Button profInboxBtn,
            Button helpBtn,
            Button settingsBtn,
            Label profNameLabel,
            Button logoutBtn,
            HBox statsRow
    ) {
        if (scrollPane != null) {
            scrollPane.setFitToWidth(true);
            scrollPane.setPannable(false);
            scrollPane.hvalueProperty().addListener((obs, oldV, newV) -> {
                if (newV.doubleValue() != 0) {
                    Platform.runLater(() -> scrollPane.setHvalue(0));
                }
            });
        }

        if (helpBtn != null && helpBtn.getTooltip() == null) {
            helpBtn.setTooltip(new Tooltip("Help / Report Bug"));
        }
        if (settingsBtn != null && settingsBtn.getTooltip() == null) {
            settingsBtn.setTooltip(new Tooltip("Account & Security Settings"));
        }
        if (logoutBtn != null && logoutBtn.getTooltip() == null) {
            logoutBtn.setTooltip(new Tooltip("Logout of AcadsCatchUp"));
        }

        Platform.runLater(() -> {
            if (topBar == null || topBar.getScene() == null) return;
            Scene scene = topBar.getScene();

            ChangeListener<Number> widthListener = (obs, oldW, newW) -> {
                double w = newW.doubleValue();
                applyProfBreakpoints(w, roleBadge, syncBadge, adminInboxBtn, profInboxBtn, helpBtn, settingsBtn, profNameLabel, logoutBtn, topBar, statsRow);
            };

            scene.widthProperty().addListener(widthListener);
            applyProfBreakpoints(scene.getWidth(), roleBadge, syncBadge, adminInboxBtn, profInboxBtn, helpBtn, settingsBtn, profNameLabel, logoutBtn, topBar, statsRow);
        });
    }

    private static void applyProfBreakpoints(
            double w,
            Label roleBadge,
            Label syncBadge,
            Button adminInboxBtn,
            Button profInboxBtn,
            Button helpBtn,
            Button settingsBtn,
            Label profNameLabel,
            Button logoutBtn,
            HBox topBar,
            HBox statsRow
    ) {
        if (w <= 0) return;

        if (w < 960.0) {
            // ── COMPACT MODE (< 960px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 8 12; -fx-spacing: 6;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(false);
                roleBadge.setManaged(false);
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢"));
                syncBadge.setTooltip(new Tooltip("Live Cloud Sync Active"));
            }
            if (helpBtn != null) {
                helpBtn.setText(OSCompat.label("💬"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText(OSCompat.label("🚪"));
            }
            if (profNameLabel != null) {
                profNameLabel.setMaxWidth(110);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 10 14 8 14; -fx-spacing: 8;");
            }
        } else if (w < 1180.0) {
            // ── MEDIUM MODE (960px - 1180px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 10 16; -fx-spacing: 8;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(true);
                roleBadge.setManaged(true);
                roleBadge.setText("Professor");
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢 Sync"));
                syncBadge.setTooltip(new Tooltip("Live Real-Time Sync"));
            }
            if (helpBtn != null) {
                helpBtn.setText(OSCompat.label("💬 Help"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙ Settings"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText("Logout");
            }
            if (profNameLabel != null) {
                profNameLabel.setMaxWidth(150);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 14 18 12 18; -fx-spacing: 12;");
            }
        } else {
            // ── FULL WIDE MODE (>= 1180px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 12 22; -fx-spacing: 12;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(true);
                roleBadge.setManaged(true);
                roleBadge.setText("Professor Dashboard");
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢 Live Sync"));
            }
            if (helpBtn != null) {
                helpBtn.setText(OSCompat.label("💬 Help / Report Bug"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙ Settings"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText("Logout");
            }
            if (profNameLabel != null) {
                profNameLabel.setMaxWidth(200);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 20 24 16 24; -fx-spacing: 16;");
            }
        }
    }

    /**
     * Installs intelligent auto-scaling responsiveness for the Admin Dashboard.
     */
    public static void installAdminResponsiveLayout(
            ScrollPane scrollPane,
            HBox topBar,
            Label appTitle,
            Label roleBadge,
            Label syncBadge,
            Button adminInboxBtn,
            Button smtpConfigBtn,
            Button settingsBtn,
            Label adminNameLabel,
            Button logoutBtn,
            HBox statsRow
    ) {
        if (scrollPane != null) {
            scrollPane.setFitToWidth(true);
            scrollPane.setPannable(false);
            scrollPane.hvalueProperty().addListener((obs, oldV, newV) -> {
                if (newV.doubleValue() != 0) {
                    Platform.runLater(() -> scrollPane.setHvalue(0));
                }
            });
        }

        if (adminInboxBtn != null && adminInboxBtn.getTooltip() == null) {
            adminInboxBtn.setTooltip(new Tooltip("Bug Reports & User Issues"));
        }
        if (smtpConfigBtn != null && smtpConfigBtn.getTooltip() == null) {
            smtpConfigBtn.setTooltip(new Tooltip("SMTP & 2FA Configuration"));
        }
        if (settingsBtn != null && settingsBtn.getTooltip() == null) {
            settingsBtn.setTooltip(new Tooltip("Account & Security Settings"));
        }
        if (logoutBtn != null && logoutBtn.getTooltip() == null) {
            logoutBtn.setTooltip(new Tooltip("Logout of AcadsCatchUp"));
        }

        Platform.runLater(() -> {
            if (topBar == null || topBar.getScene() == null) return;
            Scene scene = topBar.getScene();

            ChangeListener<Number> widthListener = (obs, oldW, newW) -> {
                double w = newW.doubleValue();
                applyAdminBreakpoints(w, roleBadge, syncBadge, adminInboxBtn, smtpConfigBtn, settingsBtn, adminNameLabel, logoutBtn, topBar, statsRow);
            };

            scene.widthProperty().addListener(widthListener);
            applyAdminBreakpoints(scene.getWidth(), roleBadge, syncBadge, adminInboxBtn, smtpConfigBtn, settingsBtn, adminNameLabel, logoutBtn, topBar, statsRow);
        });
    }

    private static void applyAdminBreakpoints(
            double w,
            Label roleBadge,
            Label syncBadge,
            Button adminInboxBtn,
            Button smtpConfigBtn,
            Button settingsBtn,
            Label adminNameLabel,
            Button logoutBtn,
            HBox topBar,
            HBox statsRow
    ) {
        if (w <= 0) return;

        if (w < 960.0) {
            // ── COMPACT MODE (< 960px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 8 12; -fx-spacing: 6;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(false);
                roleBadge.setManaged(false);
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢"));
                syncBadge.setTooltip(new Tooltip("Live Cloud Sync Active"));
            }
            if (adminInboxBtn != null) {
                String t = adminInboxBtn.getText();
                int idx = t != null ? t.indexOf('(') : -1;
                adminInboxBtn.setText(idx != -1 ? OSCompat.label("📥 ") + t.substring(idx) : OSCompat.label("📥"));
            }
            if (smtpConfigBtn != null) {
                smtpConfigBtn.setText(OSCompat.label("📧"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText(OSCompat.label("🚪"));
            }
            if (adminNameLabel != null) {
                adminNameLabel.setMaxWidth(110);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 10 14 8 14; -fx-spacing: 8;");
            }
        } else if (w < 1180.0) {
            // ── MEDIUM MODE (960px - 1180px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 10 16; -fx-spacing: 8;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(true);
                roleBadge.setManaged(true);
                roleBadge.setText("Admin");
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢 Sync"));
                syncBadge.setTooltip(new Tooltip("Live Real-Time Sync"));
            }
            if (adminInboxBtn != null) {
                String t = adminInboxBtn.getText();
                int idx = t != null ? t.indexOf('(') : -1;
                adminInboxBtn.setText(idx != -1 ? OSCompat.label("📥 Reports ") + t.substring(idx) : OSCompat.label("📥 Reports"));
            }
            if (smtpConfigBtn != null) {
                smtpConfigBtn.setText(OSCompat.label("📧 SMTP"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙ Settings"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText("Logout");
            }
            if (adminNameLabel != null) {
                adminNameLabel.setMaxWidth(150);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 14 18 12 18; -fx-spacing: 12;");
            }
        } else {
            // ── FULL WIDE MODE (>= 1180px) ──
            if (topBar != null) {
                topBar.setStyle("-fx-padding: 12 22; -fx-spacing: 12;");
            }
            if (roleBadge != null) {
                roleBadge.setVisible(true);
                roleBadge.setManaged(true);
                roleBadge.setText("Admin Dashboard");
            }
            if (syncBadge != null) {
                syncBadge.setText(OSCompat.label("🟢 Live Sync"));
            }
            if (adminInboxBtn != null) {
                String t = adminInboxBtn.getText();
                int idx = t != null ? t.indexOf('(') : -1;
                adminInboxBtn.setText(idx != -1 ? OSCompat.label("📥 Bug Reports ") + t.substring(idx) : OSCompat.label("📥 Bug Reports (0)"));
            }
            if (smtpConfigBtn != null) {
                smtpConfigBtn.setText(OSCompat.label("📧 SMTP Config"));
            }
            if (settingsBtn != null) {
                settingsBtn.setText(OSCompat.label("⚙ Settings"));
            }
            if (logoutBtn != null) {
                logoutBtn.setText("Logout");
            }
            if (adminNameLabel != null) {
                adminNameLabel.setMaxWidth(200);
            }
            if (statsRow != null) {
                statsRow.setStyle("-fx-padding: 20 24 16 24; -fx-spacing: 16;");
            }
        }
    }
}
