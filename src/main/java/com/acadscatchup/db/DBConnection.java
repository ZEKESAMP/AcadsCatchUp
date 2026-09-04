package com.acadscatchup.db;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.*;
import java.util.Properties;

/**
 * Online Database Connection Manager with Mandatory Internet Verification.
 * Connects to the central online database so all users on any PC/network sync in real time.
 * When there is no internet connection, access is strictly prevented.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class DBConnection {

    public static final String DEVELOPER = "F4TAL";

    private static final java.util.concurrent.BlockingQueue<Connection> connectionPool = new java.util.concurrent.LinkedBlockingQueue<>(10);
    private static final java.util.Set<Connection> allOpenConnections = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static volatile boolean schemaInitialized = false;

    private static volatile boolean cachedInternet = true;
    private static volatile long lastInternetCheck = 0;
    private static final long INTERNET_CACHE_MS = 15000; // 15 seconds

    private static Properties dbConfig = new Properties();
    private static boolean isMySQLEngine = true;

    // Obfuscated cloud database configuration (Protected against decompiler string inspection)
    private static final int OBF_KEY = 0x5A;
    private static final int[] OBF_HOST = new int[]{61,59,46,63,45,59,35,106,107,116,59,42,119,41,53,47,46,50,63,59,41,46,119,107,116,42,40,53,62,116,59,45,41,116,46,51,62,56,57,54,53,47,62,116,57,53,55};
    private static final int[] OBF_PORT = new int[]{110,106,106,106};
    private static final int[] OBF_NAME = new int[]{59,57,59,62,41,57,59,46,57,50,47,42};
    private static final int[] OBF_USER = new int[]{105,22,20,61,56,109,99,46,43,50,29,59,55,61,44,116,40,53,53,46};
    private static final int[] OBF_PASS = new int[]{27,52,15,110,28,18,27,107,61,44,15,50,12,109,47,47};
    private static final int[] OBF_PARAMS = new int[]{47,41,63,9,9,22,103,46,40,47,63,124,59,54,54,53,45,10,47,56,54,51,57,17,63,35,8,63,46,40,51,63,44,59,54,103,46,40,47,63,124,41,63,40,44,63,40,14,51,55,63,32,53,52,63,103,15,14,25,124,57,53,52,52,63,57,46,14,51,55,63,53,47,46,103,111,106,106,106};

    private static String decode(int[] data, int key) {
        char[] chars = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            chars[i] = (char) (data[i] ^ key);
        }
        return new String(chars);
    }

    static {
        loadConfiguration();
    }

    private DBConnection() {}

    /**
     * Checks if the device has an active internet connection.
     * Uses 15-second caching for high UI responsiveness.
     */
    public static boolean hasInternet() {
        return hasInternet(false);
    }

    /**
     * Checks if the device has an active internet connection.
     * @param forceCheck If true, bypasses cache and tests the socket directly.
     */
    public static boolean hasInternet(boolean forceCheck) {
        long now = System.currentTimeMillis();
        if (!forceCheck && (now - lastInternetCheck < INTERNET_CACHE_MS)) {
            return cachedInternet;
        }
        boolean ok = checkInternetSocket();
        cachedInternet = ok;
        lastInternetCheck = now;
        return ok;
    }

    private static boolean checkInternetSocket() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 2000);
            return true;
        } catch (Exception e) {
            try (Socket socket2 = new Socket()) {
                socket2.connect(new InetSocketAddress("1.1.1.1", 53), 2000);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    public static boolean isMySQL() {
        return isMySQLEngine;
    }

    public static String formatGroupConcat(String column, String separator) {
        if (isMySQLEngine) {
            return "GROUP_CONCAT(" + column + " SEPARATOR '" + separator + "')";
        } else {
            return "GROUP_CONCAT(" + column + ", '" + separator + "')";
        }
    }

    /**
     * Returns a high-performance pooled Connection.
     * Closing this connection returns it to the pool instead of severing the remote TLS socket,
     * ensuring blazing fast query execution and zero UI thread freeze.
     */
    public static Connection getConnection() {
        if (!hasInternet()) {
            throw new RuntimeException("NO_INTERNET: AcadsCatchUp requires an active internet connection to access the online database.");
        }

        Connection physicalConn = null;
        while (!connectionPool.isEmpty()) {
            Connection candidate = connectionPool.poll();
            if (candidate != null) {
                try {
                    if (!candidate.isClosed() && candidate.isValid(1)) {
                        physicalConn = candidate;
                        break;
                    } else {
                        try { candidate.close(); } catch (Exception ignored) {}
                        allOpenConnections.remove(candidate);
                    }
                } catch (Exception e) {
                    try { candidate.close(); } catch (Exception ignored) {}
                    allOpenConnections.remove(candidate);
                }
            }
        }

        if (physicalConn == null) {
            try {
                physicalConn = openConnection();
                allOpenConnections.add(physicalConn);
            } catch (SQLException e) {
                System.err.println("[DB] SQL Connection error: " + e.getMessage());
                throw new RuntimeException("Cannot connect to online database: " + e.getMessage(), e);
            }
        }

        // Schema initialization - strictly ONCE across the entire application lifecycle
        if (!schemaInitialized) {
            synchronized (DBConnection.class) {
                if (!schemaInitialized) {
                    try {
                        if (isMySQLEngine) {
                            initMySQLSchemaIfNeeded(physicalConn);
                        } else {
                            initSQLiteSchemaIfNeeded(physicalConn);
                        }
                        schemaInitialized = true;
                    } catch (SQLException e) {
                        System.err.println("[DB] Schema init error: " + e.getMessage());
                    }
                }
            }
        }

        final Connection finalPhysical = physicalConn;
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("close".equals(name)) {
                        try {
                            if (!finalPhysical.isClosed()) {
                                if (!finalPhysical.getAutoCommit()) {
                                    finalPhysical.setAutoCommit(true);
                                }
                                if (connectionPool.size() < 4) {
                                    connectionPool.offer(finalPhysical);
                                } else {
                                    finalPhysical.close();
                                    allOpenConnections.remove(finalPhysical);
                                }
                            }
                        } catch (Exception ignored) {}
                        return null;
                    }
                    if ("isClosed".equals(name)) {
                        return finalPhysical.isClosed();
                    }
                    if ("unwrap".equals(name) && args != null && args.length == 1) {
                        Class<?> targetClass = (Class<?>) args[0];
                        if (targetClass.isInstance(finalPhysical)) {
                            return finalPhysical;
                        }
                    }
                    try {
                        return method.invoke(finalPhysical, args);
                    } catch (java.lang.reflect.InvocationTargetException ite) {
                        throw ite.getCause();
                    }
                }
        );
    }



    private static Connection openConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String host = dbConfig.getProperty("db.host", decode(OBF_HOST, OBF_KEY)).trim();
            String port = dbConfig.getProperty("db.port", decode(OBF_PORT, OBF_KEY)).trim();
            String name = dbConfig.getProperty("db.name", decode(OBF_NAME, OBF_KEY)).trim();
            String user = dbConfig.getProperty("db.user", decode(OBF_USER, OBF_KEY)).trim();
            String pass = dbConfig.getProperty("db.password", decode(OBF_PASS, OBF_KEY)).trim();
            String params = dbConfig.getProperty("db.params", decode(OBF_PARAMS, OBF_KEY)).trim();

            String url = "jdbc:mysql://" + host + ":" + port + "/" + name + "?" + params;
            System.out.println("[DB] Connecting to Secure Online Cloud Database");
            Connection conn = DriverManager.getConnection(url, user, pass);
            isMySQLEngine = true;
            return conn;
        } catch (Exception e) {
            System.err.println("[DB] Remote MySQL connection failed (" + e.getMessage() + "). Falling back to embedded database...");
            return openSQLiteConnection();
        }
    }

    private static Connection openSQLiteConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC Driver not found", e);
        }
        String dbFile = getDBPath();
        System.out.println("[DB] Using Embedded Database: " + dbFile);
        isMySQLEngine = false;
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
        return conn;
    }

    public static synchronized void closeConnection() {
        connectionPool.clear();
        for (Connection c : allOpenConnections) {
            try {
                if (c != null && !c.isClosed()) c.close();
            } catch (SQLException ignored) {}
        }
        allOpenConnections.clear();
        schemaInitialized = false;
    }

    public static Properties getConfig() {
        return dbConfig;
    }

    public static synchronized void saveConfig(String host, String port, String name, String user, String pass) {
        dbConfig.setProperty("db.type", "mysql");
        dbConfig.setProperty("db.host", host);
        dbConfig.setProperty("db.port", port);
        dbConfig.setProperty("db.name", name);
        dbConfig.setProperty("db.user", user);
        dbConfig.setProperty("db.password", pass);

        File target = getConfigFile();
        try (FileOutputStream fos = new FileOutputStream(target)) {
            dbConfig.store(fos, "AcadsCatchUp Online Database Configuration");
            System.out.println("[DB] Saved updated database configuration to " + target.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[DB] Error saving config: " + e.getMessage());
        }
        closeConnection();
    }

    private static File getConfigFile() {
        String jpackageAppPath = System.getProperty("jpackage.app-path");
        if (jpackageAppPath != null && !jpackageAppPath.isBlank()) {
            File exe = new File(jpackageAppPath);
            if (exe.getParentFile() != null) {
                return new File(exe.getParentFile(), "database.properties");
            }
        }
        return new File("database.properties");
    }

    private static void loadConfiguration() {
        // Hardcoded secure cloud configuration — obfuscated against public tampering and DDoS
        dbConfig.setProperty("db.type", "mysql");
        dbConfig.setProperty("db.host", decode(OBF_HOST, OBF_KEY));
        dbConfig.setProperty("db.port", decode(OBF_PORT, OBF_KEY));
        dbConfig.setProperty("db.name", decode(OBF_NAME, OBF_KEY));
        dbConfig.setProperty("db.user", decode(OBF_USER, OBF_KEY));
        dbConfig.setProperty("db.password", decode(OBF_PASS, OBF_KEY));
        dbConfig.setProperty("db.params", decode(OBF_PARAMS, OBF_KEY));
    }

    private static String getDBPath() {
        String jpackageAppPath = System.getProperty("jpackage.app-path");
        if (jpackageAppPath != null && !jpackageAppPath.isBlank()) {
            File exeFile = new File(jpackageAppPath);
            File portableDir = exeFile.getParentFile();
            if (portableDir != null && portableDir.isDirectory()) {
                return new File(portableDir, "acadscatchup.db").getAbsolutePath();
            }
        }
        return new File("acadscatchup.db").getAbsolutePath();
    }

    private static void initMySQLSchemaIfNeeded(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id         INT AUTO_INCREMENT PRIMARY KEY,
                    username   VARCHAR(50) UNIQUE NOT NULL,
                    password   VARCHAR(255) NOT NULL,
                    full_name  VARCHAR(100) NOT NULL,
                    email      VARCHAR(150) DEFAULT NULL,
                    role       VARCHAR(20) NOT NULL,
                    program    VARCHAR(100),
                    year_level INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id   INT AUTO_INCREMENT PRIMARY KEY,
                    code VARCHAR(20) UNIQUE NOT NULL,
                    name VARCHAR(100) NOT NULL
                ) ENGINE=InnoDB""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS professor_subjects (
                    professor_id INT NOT NULL,
                    subject_id   INT NOT NULL,
                    PRIMARY KEY (professor_id, subject_id),
                    FOREIGN KEY (professor_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id)   REFERENCES subjects(id) ON DELETE CASCADE
                ) ENGINE=InnoDB""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS enrollments (
                    student_id INT NOT NULL,
                    subject_id INT NOT NULL,
                    PRIMARY KEY (student_id, subject_id),
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
                ) ENGINE=InnoDB""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS missed_items (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    student_id  INT NOT NULL,
                    subject_id  INT NOT NULL,
                    item_type   VARCHAR(30) NOT NULL,
                    item_name   VARCHAR(150) NOT NULL,
                    date_missed VARCHAR(50) NOT NULL,
                    deadline    VARCHAR(50),
                    status      VARCHAR(30) DEFAULT 'PENDING',
                    notes       TEXT,
                    created_by  INT,
                    attachment_type VARCHAR(20) DEFAULT NULL,
                    attachment_name VARCHAR(255) DEFAULT NULL,
                    attachment_url  MEDIUMTEXT DEFAULT NULL,
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
                ) ENGINE=InnoDB""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS inbox_messages (
                    id             INT AUTO_INCREMENT PRIMARY KEY,
                    sender_id      INT NOT NULL,
                    sender_name    VARCHAR(100) NOT NULL,
                    sender_role    VARCHAR(20) NOT NULL,
                    recipient_id   INT NOT NULL,
                    recipient_name VARCHAR(100) NOT NULL,
                    title          VARCHAR(200) NOT NULL,
                    message        TEXT NOT NULL,
                    item_id        INT,
                    item_name      VARCHAR(150),
                    subject_code   VARCHAR(50),
                    msg_type       VARCHAR(50) NOT NULL,
                    attachment_type VARCHAR(20) DEFAULT NULL,
                    attachment_name VARCHAR(255) DEFAULT NULL,
                    attachment_url  MEDIUMTEXT DEFAULT NULL,
                    is_read        TINYINT DEFAULT 0,
                    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB""");

            // Safe migrations for attachments and email in existing tables
            try { st.executeUpdate("ALTER TABLE users ADD COLUMN email VARCHAR(150) DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE users ADD COLUMN is_verified TINYINT DEFAULT 0"); } catch (Exception ignored) {}
            try { st.executeUpdate("UPDATE users SET is_verified = 1 WHERE role = 'ADMIN'"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE inbox_messages ADD COLUMN attachment_type VARCHAR(20) DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE inbox_messages ADD COLUMN attachment_name VARCHAR(255) DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE inbox_messages ADD COLUMN attachment_url MEDIUMTEXT DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE missed_items ADD COLUMN attachment_type VARCHAR(20) DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE missed_items ADD COLUMN attachment_name VARCHAR(255) DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE missed_items ADD COLUMN attachment_url MEDIUMTEXT DEFAULT NULL"); } catch (Exception ignored) {}

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS email_config (
                    id             INT PRIMARY KEY DEFAULT 1,
                    sender_email   VARCHAR(150) DEFAULT NULL,
                    app_password   VARCHAR(255) DEFAULT NULL,
                    api_key        VARCHAR(255) DEFAULT NULL,
                    is_2fa_enabled TINYINT DEFAULT 0,
                    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB""");
            try { st.executeUpdate("ALTER TABLE email_config ADD COLUMN api_key VARCHAR(255) DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("INSERT IGNORE INTO email_config (id, sender_email, app_password, is_2fa_enabled) VALUES (1, '', '', 0)"); } catch (Exception ignored) {}

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS help_reports (
                    id         INT AUTO_INCREMENT PRIMARY KEY,
                    user_id    INT NOT NULL,
                    user_name  VARCHAR(100) NOT NULL,
                    user_role  VARCHAR(20) NOT NULL,
                    title      VARCHAR(200) NOT NULL,
                    message    TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status     VARCHAR(20) DEFAULT 'OPEN',
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB""");

            st.executeUpdate("INSERT IGNORE INTO subjects (code, name) VALUES ('PE', 'P.E')");
            st.executeUpdate("INSERT IGNORE INTO subjects (code, name) VALUES ('RHET', 'Rhetoric')");
            st.executeUpdate("INSERT IGNORE INTO subjects (code, name) VALUES ('CP', 'Computer Programming')");
            st.executeUpdate("INSERT IGNORE INTO subjects (code, name) VALUES ('MTS', 'Mathematics, Technology and Science')");
            st.executeUpdate("INSERT IGNORE INTO subjects (code, name) VALUES ('DSA', 'Data Structure')");
            st.executeUpdate("INSERT IGNORE INTO subjects (code, name) VALUES ('RIZAL', 'Rizal')");
            st.executeUpdate("INSERT IGNORE INTO subjects (code, name) VALUES ('IT-ERA', 'Living in the IT Era')");

            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                st.executeUpdate("INSERT INTO users (id, username, password, full_name, role, program, year_level) VALUES (1, 'F4TAL', 'zekesamp', 'System Administrator', 'ADMIN', 'ADMIN', 0)");
                st.executeUpdate("INSERT INTO users (id, username, password, full_name, role, program, year_level) VALUES (8, 'Ace', 'ace', 'Sir. Ace', 'PROFESSOR', 'BSIT', 0)");
                st.executeUpdate("INSERT INTO users (id, username, password, full_name, role, program, year_level) VALUES (11, 'Jesusa', 'jesusa', 'Del rosario Jesusa', 'PROFESSOR', 'BSIT', 0)");
                st.executeUpdate("INSERT INTO users (id, username, password, full_name, role, program, year_level) VALUES (12, 'kurt', 'test', 'Rebundella Kurt', 'STUDENT', 'BSIT', 1)");
                st.executeUpdate("INSERT INTO users (id, username, password, full_name, role, program, year_level) VALUES (13, 'yoshi', 'yoshi', 'Yoshi', 'STUDENT', 'BSIT', 2)");
            }
        }
    }

    private static void initSQLiteSchemaIfNeeded(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    username  TEXT UNIQUE NOT NULL,
                    password  TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    email     TEXT DEFAULT NULL,
                    role      TEXT NOT NULL CHECK(role IN ('PROFESSOR','STUDENT','ADMIN')),
                    program   TEXT,
                    year_level INTEGER DEFAULT 0,
                    created_at TEXT DEFAULT (datetime('now'))
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT UNIQUE NOT NULL,
                    name TEXT NOT NULL
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS professor_subjects (
                    professor_id INTEGER NOT NULL,
                    subject_id   INTEGER NOT NULL,
                    PRIMARY KEY (professor_id, subject_id),
                    FOREIGN KEY (professor_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id)   REFERENCES subjects(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS enrollments (
                    student_id INTEGER NOT NULL,
                    subject_id INTEGER NOT NULL,
                    PRIMARY KEY (student_id, subject_id),
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS missed_items (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id  INTEGER NOT NULL,
                    subject_id  INTEGER NOT NULL,
                    item_type   TEXT NOT NULL CHECK(item_type IN ('ACTIVITY','QUIZ','EXAM','ASSIGNMENT')),
                    item_name   TEXT NOT NULL,
                    date_missed TEXT NOT NULL,
                    deadline    TEXT,
                    status      TEXT DEFAULT 'PENDING' CHECK(status IN ('PENDING','SUBMITTED','GRADED')),
                    notes       TEXT,
                    created_by  INTEGER,
                    attachment_type TEXT,
                    attachment_name TEXT,
                    attachment_url  TEXT,
                    created_at  TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
                    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS inbox_messages (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_id      INTEGER NOT NULL,
                    sender_name    TEXT NOT NULL,
                    sender_role    TEXT NOT NULL,
                    recipient_id   INTEGER NOT NULL,
                    recipient_name TEXT NOT NULL,
                    title          TEXT NOT NULL,
                    message        TEXT NOT NULL,
                    item_id        INTEGER,
                    item_name      TEXT,
                    subject_code   TEXT,
                    msg_type       TEXT NOT NULL,
                    attachment_type TEXT,
                    attachment_name TEXT,
                    attachment_url  TEXT,
                    is_read        INTEGER DEFAULT 0,
                    created_at     TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
                )""");

            // Safe migrations for attachments and email in existing SQLite tables
            try { st.executeUpdate("ALTER TABLE users ADD COLUMN email TEXT DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE users ADD COLUMN is_verified INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { st.executeUpdate("UPDATE users SET is_verified = 1 WHERE role = 'ADMIN'"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE inbox_messages ADD COLUMN attachment_type TEXT"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE inbox_messages ADD COLUMN attachment_name TEXT"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE inbox_messages ADD COLUMN attachment_url TEXT"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE missed_items ADD COLUMN attachment_type TEXT"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE missed_items ADD COLUMN attachment_name TEXT"); } catch (Exception ignored) {}
            try { st.executeUpdate("ALTER TABLE missed_items ADD COLUMN attachment_url TEXT"); } catch (Exception ignored) {}

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS email_config (
                    id             INTEGER PRIMARY KEY DEFAULT 1,
                    sender_email   TEXT DEFAULT NULL,
                    app_password   TEXT DEFAULT NULL,
                    api_key        TEXT DEFAULT NULL,
                    is_2fa_enabled INTEGER DEFAULT 0
                )""");
            try { st.executeUpdate("ALTER TABLE email_config ADD COLUMN api_key TEXT DEFAULT NULL"); } catch (Exception ignored) {}
            try { st.executeUpdate("INSERT OR IGNORE INTO email_config (id, sender_email, app_password, is_2fa_enabled) VALUES (1, '', '', 0)"); } catch (Exception ignored) {}

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS help_reports (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id    INTEGER NOT NULL,
                    user_name  TEXT NOT NULL,
                    user_role  TEXT NOT NULL,
                    title      TEXT NOT NULL,
                    message    TEXT NOT NULL,
                    created_at TEXT DEFAULT (datetime('now', 'localtime')),
                    status     TEXT DEFAULT 'OPEN',
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )""");
        }
    }
}
