package com.acadscatchup.util;

import com.acadscatchup.dao.InboxDAO;
import com.acadscatchup.model.User;

/**
 * Utility for formatting and delivering official "What's New" update announcements
 * to users' Inboxes.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class UpdateNoticeUtil {

    public static final String DEVELOPER = "F4TAL";

    private static final InboxDAO INBOX_DAO = new InboxDAO();

    /**
     * Returns the formatted release notes for the specified version.
     */
    public static String getWhatsNewText(String version) {
        if ("1.0.8".equals(version) || "v1.0.8".equalsIgnoreCase(version)) {
            return """
                Hello {recipientName}!

                Welcome to AcadsCatchUp v1.0.8! Here is what's new in this release:

                🔍 STUDENT REAL-TIME REACTIVE SEARCH
                • Introduced a powerful, zero-latency instant search bar on the Student Dashboard.
                • Type any keyword to instantly filter items by Subject Code, Subject Name, Professor Name, Item Name, Type, Status, Date Missed, Deadline, or Notes without reload delays.

                📊 INTERACTIVE DASHBOARD STAT CARDS
                • Total, Pending, Submitted, and Graded stat cards on both Student and Professor dashboards are now interactive!
                • Hover to see helpful tooltips and click any card to instantly filter your deficiency table by that status.

                ⚡ TABLE DOUBLE-CLICK ACTION SHORTCUT
                • Double-click any row in the Student deficiency table to quickly open the submission dialog directly.

                📥 STUDENT CHECKLIST EXPORT TO CSV
                • Students can now export their personalized deficiency checklist and missed item history directly to CSV for offline tracking or submission records.

                👨‍🏫 ENHANCED CSV EXPORT WITH PROFESSOR INFO
                • Exported CSV files now include the assigned Professor Name for every deficiency record across all roles.

                🚀 SEAMLESS HANDOFF AUTO-UPDATER
                • Completely rebuilt background update handoff engine with PID tracking and retry loop for 100% reliable in-app updates on Windows.

                Thank you for using AcadsCatchUp!
                — Engineered with care by F4TAL""";
        }

        if ("1.0.7".equals(version) || "v1.0.7".equalsIgnoreCase(version)) {
            return """
                Hello {recipientName}!

                Welcome to AcadsCatchUp v1.0.7! Here is what's new in this release:

                🛠️ ADMIN DASHBOARD FILTER ENGINE OVERHAUL
                • Fixed program and year level filtering logic where non-student accounts previously bypassed filter predicates.
                • Selecting a program (e.g. BSIT) or year level now cleanly isolates the exact matching student accounts.

                ⚡ REAL-TIME REACTIVE SEARCH FILTERING
                • Search box is now fully reactive with instant keystroke, clipboard paste, and text-clearing listeners.
                • Search queries now comprehensively match across username, full name, email, program, role, year level, and professor assigned subjects.

                🎯 SMART FILTER SYNERGY
                • Selecting the "ADMIN" or "PROFESSOR" role now intelligently coordinates with program and year filters, disabling irrelevant filter controls to prevent contradictory queries.

                🖱️ INTERACTIVE DASHBOARD STAT CARDS
                • Total Users, Students, Professors, Subjects, and Reports cards now feature pointer cursors and 1-click shortcut navigation:
                  - Click "Students" or "Professors" to instantly filter the directory.
                  - Click "Total Users" to reset all filters.
                  - Click "Subjects" or "Open Reports" to open academic and support tools directly.

                📋 TABLE SELECTION & DOUBLE-CLICK EDITING
                • Double-click any user row in the table to immediately open the Edit Account modal.
                • Selecting a single user's checkbox now smoothly resolves and opens edit mode when clicking "✏ Edit".
                • Header "Select All" unchecking now intelligently deselects only visible filtered items.

                Thank you for using AcadsCatchUp!
                — Engineered with care by F4TAL""";
        }

        if ("1.0.6".equals(version) || "v1.0.6".equalsIgnoreCase(version)) {
            return """
                Hello {recipientName}!

                Welcome to AcadsCatchUp v1.0.6! Here is what's new in this release:

                ⚙️ STREAMLINED SETTINGS & CENTRALIZED UPDATES
                • Removed redundant manual update check button from Account & Security Settings.
                • All update operations, version manifests, and changelogs are now cleanly centralized in the dedicated "Updates" header hub.

                ⚡ ENHANCED AUTO-UPDATE ENGINE & TIME-OUT RESILIENCE
                • Extended network connection timeouts to 6.0s and download timeouts to 90.0s for high reliability on slow or mobile connections.
                • Added instant anti-cache query parameters to ensure fresh version detection directly from GitHub Releases.

                📊 LIVE DOWNLOAD PROGRESS MODAL
                • Manual update downloads now feature a dedicated, dark-themed real-time progress dialog.
                • Watch live megabyte transfer counts, percentage completion, and status in real time.

                🛡️ DOWNLOAD INTEGRITY VERIFICATION GUARD
                • Added automated content-length validation (95%+ size check) after download.
                • Prevents partial, truncated, or corrupt files from ever replacing active installation binaries.

                🔄 DASHBOARD LIVE VERSION BADGING
                • Real-time daemon check highlights the "🔄 Updates (New!)" button with an amber badge whenever a newer GitHub release is published.

                📬 ADMIN INBOX & UPDATE NOTICE SUPPORT
                • System Administrators now receive update notifications and have direct 1-click access to their personal Inbox right from Bug Reports.

                Thank you for using AcadsCatchUp!
                — Engineered with care by F4TAL""";
        }

        if ("1.0.5".equals(version) || "v1.0.5".equalsIgnoreCase(version)) {
            return """
                Hello {recipientName}!

                Welcome to AcadsCatchUp v1.0.5! Here is what's new in this release:

                ✨ INBOX "WHAT'S NEW" DELIVERY
                • All system update announcements and changelogs are now delivered directly to your personal Inbox!
                • Never miss a new feature, improvement, or platform enhancement.
                • Read release notes anytime right from your dashboard inbox.

                🚀 ANCHOR-BASED PORTABLE DIRECTORY RESOLVER
                • Smart directory resolver automatically identifies portable installation folders via structural file anchors (AcadsCatchUp.exe, app/AcadsCatchUp.cfg, runtime/).
                • Works reliably regardless of extraction folder name or installation location (Downloads, Desktop, OneDrive, or custom folders).

                🔄 DUAL-PATH CLASSPATH SYNCHRONIZATION
                • Automatic updates synchronize both the root directory and the internal app/ directory (AcadsCatchUp.jar & acadscatchup-app.jar).
                • Ensures the native Windows launcher (AcadsCatchUp.exe) always runs the latest build.

                ⚡ RESILIENT DIRECT-TO-LOGIN AUTO-UPDATER
                • Sub-second background handoff bypasses Windows JVM open-file write restrictions smoothly.
                • Automatically opens the Login screen with '--direct-login' upon update without splash delays or restart prompts.

                📱 UPDATES & WHAT'S NEW HUB
                • Access release history, what's new highlights, and changelog version switches from the new "Updates" button in your dashboard header.

                Thank you for using AcadsCatchUp!
                — Engineered with care by F4TAL""";
        }

        return "Welcome to AcadsCatchUp v" + version + "!\n\nPlease check the Updates dialog for full release details.";
    }

    /**
     * Ensures that the specified user has received the current version's What's New notice in their inbox.
     */
    public static void ensureUserUpdateNotice(User user) {
        if (user == null) return;
        new Thread(() -> {
            try {
                String version = UpdateSplash.CURRENT_VERSION;
                String title = "🚀 What's New in AcadsCatchUp v" + version;
                String message = getWhatsNewText(version);
                INBOX_DAO.sendUpdateNoticeIfNew(user.getId(), user.getFullName(), version, title, message);
            } catch (Exception e) {
                System.err.println("[UpdateNoticeUtil] ensureUserUpdateNotice error: " + e.getMessage());
            }
        }, "UpdateNotice-UserCheck").start();
    }

    /**
     * Broadcasts the current version's What's New notice to all registered users who have not received it yet.
     */
    public static void broadcastCurrentVersionUpdateNotice() {
        new Thread(() -> {
            try {
                String version = UpdateSplash.CURRENT_VERSION;
                String title = "🚀 What's New in AcadsCatchUp v" + version;
                String message = getWhatsNewText(version);
                int count = INBOX_DAO.broadcastSystemUpdateNotice(version, title, message);
                if (count > 0) {
                    System.out.println("[UpdateNoticeUtil] Delivered v" + version + " What's New notice to " + count + " user(s).");
                }
            } catch (Exception e) {
                System.err.println("[UpdateNoticeUtil] broadcastCurrentVersionUpdateNotice error: " + e.getMessage());
            }
        }, "UpdateNotice-Broadcast").start();
    }

    /**
     * Manually delivers the release notes of the specified version to the current user's inbox.
     */
    public static boolean sendNoticeToUser(User user, String version, String message) {
        if (user == null) return false;
        String cleanVer = version.replace("v", "").trim();
        String title = "🚀 What's New in AcadsCatchUp v" + cleanVer;
        return INBOX_DAO.sendMessage(
                0,
                "AcadsCatchUp System",
                "SYSTEM",
                user.getId(),
                user.getFullName(),
                title,
                message.replace("{recipientName}", user.getFullName()),
                null,
                null,
                null,
                "UPDATE",
                null,
                null,
                null
        );
    }
}
