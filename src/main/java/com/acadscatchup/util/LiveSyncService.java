package com.acadscatchup.util;

import com.acadscatchup.db.DBConnection;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real-Time Database Synchronization Engine.
 * Periodically evaluates ultra-lightweight cryptographic/aggregate fingerprints
 * (<10ms execution on TiDB Cloud) to detect real-time data mutations across clients
 * and pushes updates to the JavaFX UI without blocking.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class LiveSyncService {

    public static final String DEVELOPER = "F4TAL";

    public enum SyncStatus {
        CONNECTED("🟢 Live Sync", "#10b981", "rgba(16,185,129,0.12)"),
        SYNCING("🔄 Syncing...", "#3b82f6", "rgba(59,130,246,0.12)"),
        RECONNECTING("🟠 Reconnecting...", "#f59e0b", "rgba(245,158,11,0.12)"),
        OFFLINE("🔴 Offline", "#ef4444", "rgba(239,68,68,0.12)");

        public final String label;
        public final String textColor;
        public final String bgColor;

        SyncStatus(String label, String textColor, String bgColor) {
            this.label = OSCompat.label(label);
            this.textColor = textColor;
            this.bgColor = bgColor;
        }
    }

    public interface SyncListener {
        /**
         * Fired on the JavaFX Application Thread when the database fingerprint changes.
         */
        void onDataChanged();

        /**
         * Fired on the JavaFX Application Thread when connection status changes.
         */
        default void onStatusChanged(SyncStatus status) {}
    }

    private final String querySql;
    private final Object[] queryParams;
    private final SyncListener listener;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LiveSync-Daemon");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> scheduledTask = null;
    private String lastFingerprint = null;
    private SyncStatus currentStatus = SyncStatus.SYNCING;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isChecking = new AtomicBoolean(false);

    /**
     * Create a LiveSyncService with a custom fingerprint SQL query and params.
     */
    public LiveSyncService(String querySql, Object[] queryParams, SyncListener listener) {
        this.querySql = querySql;
        this.queryParams = queryParams != null ? queryParams : new Object[0];
        this.listener = listener;
    }

    /**
     * Factory method for Student view real-time synchronization.
     */
    public static LiveSyncService forStudent(int studentId, SyncListener listener) {
        String sql = """
            SELECT CONCAT(
                (SELECT CONCAT(COUNT(*), ':', COALESCE(SUM(id), 0), ':', COALESCE(SUM(CASE WHEN status='GRADED' THEN 5 WHEN status='SUBMITTED' THEN 3 ELSE 1 END), 0))
                 FROM missed_items WHERE student_id = ?),
                '#',
                (SELECT CONCAT(COUNT(*), ':', COALESCE(MAX(id), 0), ':', COALESCE(SUM(is_read), 0))
                 FROM inbox_messages WHERE recipient_id = ?),
                '#',
                (SELECT CONCAT(COUNT(*), ':', COALESCE(SUM(subject_id), 0))
                 FROM enrollments WHERE student_id = ?),
                '#',
                (SELECT CONCAT(COUNT(*), ':', COALESCE(MAX(id), 0))
                 FROM subjects)
            ) AS fingerprint
            """;
        return new LiveSyncService(sql, new Object[]{studentId, studentId, studentId}, listener);
    }

    /**
     * Factory method for Professor / Admin view real-time synchronization.
     */
    public static LiveSyncService forProfessor(int profId, boolean isAdmin, SyncListener listener) {
        String sql = """
            SELECT CONCAT(
                (SELECT CONCAT(COUNT(*), ':', COALESCE(SUM(id), 0), ':', COALESCE(SUM(CASE WHEN status='GRADED' THEN 5 WHEN status='SUBMITTED' THEN 3 ELSE 1 END), 0))
                 FROM missed_items),
                '#',
                (SELECT CONCAT(COUNT(*), ':', COALESCE(MAX(id), 0), ':', COALESCE(SUM(is_read), 0))
                 FROM inbox_messages WHERE recipient_id = ?),
                '#',
                (SELECT CONCAT(COUNT(*), ':', COALESCE(MAX(id), 0), ':', COALESCE(SUM(status='OPEN'), 0))
                 FROM help_reports),
                '#',
                (SELECT CONCAT(COUNT(*), ':', COALESCE(SUM(student_id), 0), ':', COALESCE(SUM(subject_id), 0))
                 FROM enrollments),
                '#',
                (SELECT CONCAT(COUNT(*), ':', COALESCE(MAX(id), 0))
                 FROM subjects)
            ) AS fingerprint
            """;
        return new LiveSyncService(sql, new Object[]{profId}, listener);
    }

    /**
     * Start the real-time background sync loop.
     * Default heartbeat: 3.5 seconds.
     */
    public synchronized void start() {
        start(1000, 3500);
    }

    /**
     * Start with custom initial delay and interval in milliseconds.
     */
    public synchronized void start(long initialDelayMs, long periodMs) {
        if (isRunning.getAndSet(true)) return;

        updateStatus(SyncStatus.SYNCING);
        scheduledTask = scheduler.scheduleWithFixedDelay(
                this::checkFingerprintSafe,
                initialDelayMs,
                periodMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Triggers an immediate fingerprint check in the background (0ms latency for local user writes).
     */
    public void triggerImmediateSync() {
        if (!isRunning.get() || scheduler.isShutdown()) return;
        scheduler.execute(this::checkFingerprintSafe);
    }

    private void checkFingerprintSafe() {
        if (!isChecking.compareAndSet(false, true)) {
            // Already checking, avoid queue buildup
            return;
        }

        try {
            String newFingerprint = queryFingerprint();

            if (newFingerprint != null) {
                updateStatus(SyncStatus.CONNECTED);

                if (lastFingerprint == null) {
                    // Initial baseline established
                    lastFingerprint = newFingerprint;
                } else if (!newFingerprint.equals(lastFingerprint)) {
                    // Real-time mutation detected in cloud database!
                    lastFingerprint = newFingerprint;
                    if (listener != null) {
                        runOnFxThread(listener::onDataChanged);
                    }
                }
            } else {
                updateStatus(SyncStatus.RECONNECTING);
            }
        } catch (SQLException e) {
            System.err.println("[LiveSync] Connection warning: " + e.getMessage());
            updateStatus(SyncStatus.RECONNECTING);
        } catch (Exception e) {
            System.err.println("[LiveSync] Sync error: " + e.getMessage());
            updateStatus(SyncStatus.OFFLINE);
        } finally {
            isChecking.set(false);
        }
    }

    private String queryFingerprint() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySql)) {

            for (int i = 0; i < queryParams.length; i++) {
                ps.setObject(i + 1, queryParams[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private void updateStatus(SyncStatus newStatus) {
        if (currentStatus != newStatus) {
            currentStatus = newStatus;
            if (listener != null) {
                runOnFxThread(() -> listener.onStatusChanged(newStatus));
            }
        }
    }

    private void runOnFxThread(Runnable action) {
        try {
            Platform.runLater(action);
        } catch (IllegalStateException e) {
            // Toolkit not active (e.g. test or non-GUI thread)
            action.run();
        }
    }

    public SyncStatus getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Gracefully stops the heartbeat daemon.
     */
    public synchronized void shutdown() {
        isRunning.set(false);
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }
        scheduler.shutdownNow();
    }
}
