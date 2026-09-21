# AcadsCatchUp — Technical Algorithms Specification & Procedural Logic Reference

> **Document Type:** Official Academic Capstone Algorithm Analysis & Computational Complexity Reference  
> **Target Project:** AcadsCatchUp (Desktop Academic Deficiency & Remediation Management System)  
> **Lead Developer & System Architect:** Stevenson James G. Gastanes (Alias: **F4TAL**)  
> **Architecture:** Pure Java 21 LTS (MVC + DAO + Multi-Threaded Sync Daemons)  
> **Milestone Status:** Capstone Final Defense & Production Release  
> **Production Version:** v1.1.1-PROD-F4TAL  
> **Date of Finalization:** September 2026  

---

## Table of Contents

1. [Application Bootstrapping & Runtime Integrity Verification Algorithm](#1-application-bootstrapping--runtime-integrity-verification-algorithm)
2. [Discord-Style Auto-Update, Download & Hot Process Handoff Algorithm](#2-discord-style-auto-update-download--hot-process-handoff-algorithm)
3. [Dual-Engine Database Persistence & Failover Algorithm](#3-dual-engine-database-persistence--failover-algorithm)
4. [Real-Time Data Mutation Detection & LiveSync Fingerprint Algorithm](#4-real-time-data-mutation-detection--livesync-fingerprint-algorithm)
5. [Cryptographic Password Hashing & Transparent Migration Algorithm](#5-cryptographic-password-hashing--transparent-migration-algorithm)
6. [Two-Factor Authentication (2FA) & One-Time Password (OTP) Engine](#6-two-factor-authentication-2fa--one-time-password-otp-engine)
7. [Email Deliverability, DNS MX & Reputation Validation Algorithm](#7-email-deliverability-dns-mx--reputation-validation-algorithm)
8. [Role-Based Access Control (RBAC) & Course Scoping Algorithm](#8-role-based-access-control-rbac--course-scoping-algorithm)
9. [Academic Deficiency & Remediation Lifecycle State Machine](#9-academic-deficiency--remediation-lifecycle-state-machine)
10. [Responsive UI Layout & Dynamic View Scaling Algorithm](#10-responsive-ui-layout--dynamic-view-scaling-algorithm)

---

## 1. Application Bootstrapping & Runtime Integrity Verification Algorithm

### 1.1 Classpath Bypass & Launch Dispatcher (`AppLauncher`)
Standard JavaFX applications launched via `Application.launch()` require JavaFX runtime modules on the `--module-path`. To enable a zero-dependency standalone Fat JAR execution via `java -jar AcadsCatchUp.jar` or native launcher binaries without modular CLI flags, AcadsCatchUp implements a non-subclassed wrapper:

```
[User / OS Execution]
       │
       ▼
AppLauncher.main(args)
       │
       ├───► 1. Execute DeveloperGuard.verifyAll() [Integrity Gate]
       │         └── If Fail: Terminate Process with Fatal RuntimeException
       │
       ├───► 2. Evaluate CLI Arguments
       │         ├── "--install" / "-i" / "--setup"  ──► AcadsCatchUpInstaller.main(args)
       │         ├── "--uninstall" / "-u"            ──► Uninstaller.main(args)
       │         └── Else                             ──► Proceed to Main.main(args)
       │
       ▼
Main.start(primaryStage)
       ├───► If "--direct-login": Bypass splash screen -> showLoginScreen()
       └───► Else: Launch Discord-Style UpdateSplash
```

### 1.2 Runtime Developer Integrity Guard Algorithm (`DeveloperGuard`)
To protect against unauthorized redistribution, decompilation modifications, and code tampering, AcadsCatchUp runs an automated classpath reflection audit across all compiled classes prior to UI or database initialization.

#### Algorithmic Steps:
1. **Package Discovery**: The class loader inspects resources mapped to `com/acadscatchup`.
2. **Resource Protocol Branching**:
   - If protocol is `file`: Traverses local filesystem directories recursively looking for `.class` files.
   - If protocol is `jar`: Opens `JarFile` / `JarURLConnection`, enumerating all `JarEntry` records matching `com/acadscatchup/*.class`.
3. **Class Reflection & Signature Validation**:
   - Loads class `clazz = Class.forName(className)`.
   - Queries `Field field = clazz.getDeclaredField("DEVELOPER")`.
   - Verifies `Modifier.isStatic(field.getModifiers()) == true`.
   - Asserts `field.get(null).equals("F4TAL")`.
4. **Enforcement Gate**:
   - Failures across all 44 classes are collected in a `List<String> failures`.
   - If `failures.size() > 0`: Formats a fatal terminal report and throws `new RuntimeException("[F4TAL Guard] FATAL — Developer integrity check FAILED")`, terminating execution before any sensitive logic or network sockets open.

---

## 2. Discord-Style Auto-Update, Download & Hot Process Handoff Algorithm

The update subsystem (`UpdateSplash`, `UpdatesDialog`, `UpdateNoticeUtil`) checks for software updates on launch, streams binary updates, and circumvents Windows JVM file locks through detached process execution.

```
       [UpdateSplash Initiated]
                  │
                  ▼
   1. Fetch Remote version.json (GitHub Raw CDN)
                  │
        [Is Version Newer?]
           ├── NO  ──► Display "v1.1.0 is up to date" ──► Launch Login Screen
           └── YES ──► Proceed to Update Flow
                            │
                            ▼
   2. Download Update to update_staging.jar (32KB Chunked Stream)
                  │
        [Integrity Guard Check]
           ├── Size < 95% Expected OR < 1MB ──► Delete stagingJar & Abort
           └── Passed ──► Proceed to Hot Handoff
                            │
                            ▼
   3. Locate AcadsCatchUp-Portable Directory (4-Tier Heuristic)
                  │
                  ▼
   4. Spawn Detached update_handoff.bat with Running Process PID
                  │
                  ▼
   5. Current Java Process Terminates (Releasing File Handles)
                  │
                  ▼
   6. Batch Script Polls PID -> Replaces Target JARs -> Relaunches Exe -> Self-Deletes
```

### 2.1 Semantic Version Comparison Algorithm
Version strings follow semantic patterns (e.g., `1.1.0`, `v1.0.9`). Comparison parses both remote and local strings:
```java
public static boolean isNewerVersion(String remoteVer, String currentVer) {
    String cleanRemote = remoteVer.trim().replaceAll("^[vV]", "");
    String cleanCurrent = currentVer.trim().replaceAll("^[vV]", "");

    String[] remoteParts = cleanRemote.split("[.-]");
    String[] currentParts = cleanCurrent.split("[.-]");
    int length = Math.max(remoteParts.length, currentParts.length);

    for (int i = 0; i < length; i++) {
        int r = (i < remoteParts.length) ? parseNumeric(remoteParts[i]) : 0;
        int c = (i < currentParts.length) ? parseNumeric(currentParts[i]) : 0;
        if (r > c) return true;
        if (r < c) return false;
    }
    return false;
}
```

### 2.2 HTTP/HTTPS Redirect Resolver Algorithm
GitHub Releases binary downloads redirect across multiple domains (e.g., `github.com` -> `objects.githubusercontent.com` on AWS S3 / Azure CDN). Standard `HttpURLConnection` drops authorization or fails cross-protocol redirection. AcadsCatchUp handles this via an iterative redirect loop:
- Loop bound: `redirects < 7`
- Response codes monitored: `301 (Moved Permanently)`, `302 (Found)`, `303 (See Other)`, `307 (Temporary Redirect)`, `308 (Permanent Redirect)`
- Location header parsing: updates `currentUrl = conn.getHeaderField("Location")` and re-establishes connection.

### 2.3 Portable Directory Resolution Heuristic
To determine where to deploy the updated `.jar`, the application executes a 4-tier discovery strategy:
1. **Tier 1 (`java.home`)**: Checks `System.getProperty("java.home")` and inspects parent folders for `isPortableRoot()` (presence of `app/` directory, `runtime/` folder, or `AcadsCatchUp.exe`).
2. **Tier 2 (`ProtectionDomain`)**: Resolves URI of running `UpdateSplash.class.getProtectionDomain().getCodeSource().getLocation()`.
3. **Tier 3 (`CWD`)**: Traverses current working directory `new File(".")` and its `dist/` subdirectories.
4. **Tier 4 (Filesystem Sweep)**: Checks common user directories (`Downloads`, `Desktop`, `Documents`, `OneDrive`, `%LOCALAPPDATA%`).

### 2.4 Detached Process Hot-Handoff Algorithm
Because Windows locks executing `.jar` files in memory, a running JVM cannot overwrite its own executable archive. AcadsCatchUp solves this by spawning a detached native batch script (`update_handoff.bat`) passing the running PID:
1. Generates `update_handoff.bat`.
2. Emits batch logic:
   - Queries `tasklist /fi "PID eq %OLD_PID%"` up to 6 cycles.
   - Forces termination via `taskkill /F /PID %OLD_PID% /T` if parent does not exit voluntarily.
   - Replaces `AcadsCatchUp.jar`, `app/AcadsCatchUp.jar`, and `app/acadscatchup-app.jar`.
   - Verifies file size equality between source and destination.
   - Spawns `AcadsCatchUp.exe` or `Launch_AcadsCatchUp.bat`.
   - Cleans up and self-deletes via `del "%~f0"`.
3. Launches batch using `ProcessBuilder("cmd.exe", "/c", "start", "/b", batFile.getAbsolutePath(), ...)` and immediately calls `System.exit(0)`.

---

## 3. Dual-Engine Database Persistence & Failover Algorithm

AcadsCatchUp implements an online-first architecture backed by a remote cloud MySQL cluster with automatic fallback to a local embedded SQLite database.

```
                  [Database Request Initiated]
                               │
                               ▼
               1. Network Check: hasInternet()?
                    Socket to 8.8.8.8 / 1.1.1.1:53
                    (15-second timestamp caching)
                               │
            ┌──────────────────┴──────────────────┐
        [ONLINE]                              [OFFLINE]
            │                                     │
            ▼                                     ▼
 2. Connect to Remote Cloud MySQL       Throw NO_INTERNET exception
    (TiDB Cloud Serverless)             or route to Embedded SQLite
            │
    [Connection OK?]
      ├── YES ──► Use TiDB Cloud MySQL
      └── NO  ──► Failover to Local SQLite (acadscatchup.db)
```

### 3.1 Network Verification Algorithm
To prevent network timeouts on mobile hotspots or unstable Wi-Fi:
- Tests TCP connection to Google Public DNS (`8.8.8.8:53`) with 2000ms timeout.
- Secondary fallback: Tests Cloudflare DNS (`1.1.1.1:53`) with 2000ms timeout.
- Result is cached for 15,000ms (`INTERNET_CACHE_MS`) to eliminate overhead during repeated queries.

### 3.2 Dynamic Dynamic Proxy Connection Pool
To avoid opening and closing TLS handshakes on every query, `DBConnection` maintains a `BlockingQueue<Connection>` (capacity 10):
- Intercepts `connection.close()` using `java.lang.reflect.Proxy`.
- When `close()` is called on the proxy, instead of severing the remote TCP/TLS socket, the physical connection is validated (`isValid(1)`) and returned to `connectionPool.offer(physicalConn)`.
- If the pool is saturated (> 4 connections), the physical connection is closed cleanly.

### 3.3 Obfuscation Algorithm for Cloud Credentials
To prevent database connection strings from being scraped from the binary, constants are obfuscated via single-byte XOR:
$$\text{DecodedChar}_i = \text{Data}_i \oplus \text{0x5A}$$
- Obfuscated values include Cloud Host (`gateway01.ap-southeast-1.prod.aws.tidbcloud.com`), Port (`4000`), DB Name (`acadscatchup`), User (`3LNgb79tqhGamgv.root`), and connection parameters.

### 3.4 SQL Dialect Abstraction
Differences between MySQL and SQLite syntax are resolved programmatically:
- **Group Concatenation**:
  - MySQL: `GROUP_CONCAT(col SEPARATOR ', ')`
  - SQLite: `GROUP_CONCAT(col, ', ')`
  - Managed by `DBConnection.formatGroupConcat(column, separator)`.
- **UPSERT Operations**:
  - MySQL: `INSERT INTO ... ON DUPLICATE KEY UPDATE ...`
  - SQLite: `INSERT OR REPLACE INTO ...`

---

## 4. Real-Time Data Mutation Detection & LiveSync Fingerprint Algorithm

Rather than pulling complete entity datasets across the network on every poll, `LiveSyncService` calculates an aggregate fingerprint:

$$\text{Fingerprint} = H(\text{MissedItems}) \parallel \text{"\#"} \parallel H(\text{Inbox}) \parallel \text{"\#"} \parallel H(\text{Enrollments}) \parallel \dots$$

```
   [Background Daemon Thread: LiveSync-Daemon]
                      │
   [Every 4-8 Seconds Interval - Non-Blocking]
                      │
                      ▼
   1. Execute Fingerprint SQL Query (<10ms on Cloud)
                      │
                      ▼
   2. Current Fingerprint String Generated
                      │
        [Matches lastFingerprint?]
           ├── YES ──► No changes; sleep until next cycle
           └── NO  ──► Mutation Detected!
                            │
                            ▼
   3. Update: lastFingerprint = newFingerprint
   4. Dispatch Platform.runLater(() -> listener.onDataChanged())
   5. UI Dashboard Refreshes Table & Stat Cards Automatically
```

### 4.1 Fingerprint Computation Logic

#### For Students:
```sql
SELECT CONCAT(
    (SELECT CONCAT(COUNT(*), ':', COALESCE(SUM(id), 0), ':', 
                   COALESCE(SUM(CASE WHEN status='GRADED' THEN 5 
                                     WHEN status='SUBMITTED' THEN 3 
                                     ELSE 1 END), 0))
     FROM missed_items WHERE student_id = ?),
    '#',
    (SELECT CONCAT(COUNT(*), ':', COALESCE(MAX(id), 0), ':', COALESCE(SUM(is_read), 0))
     FROM inbox_messages WHERE recipient_id = ?),
    '#',
    (SELECT CONCAT(COUNT(*), ':', COALESCE(SUM(subject_id), 0))
     FROM enrollments WHERE student_id = ?),
    '#',
    (SELECT CONCAT(COUNT(*), ':', COALESCE(MAX(id), 0))
     FROM subjects),
    '#',
    (SELECT CONCAT(COUNT(*), ':', COALESCE(SUM(professor_id), 0), ':', COALESCE(SUM(subject_id), 0))
     FROM professor_subjects),
    '#',
    (SELECT CONCAT(COUNT(*), ':', COALESCE(SUM(id), 0), ':', COALESCE(MAX(id), 0))
     FROM users WHERE role = 'PROFESSOR')
) AS fingerprint;
```

#### Mutation Sensitivity:
- A new item changes `COUNT(*)`.
- A status transition (`PENDING` -> `SUBMITTED` -> `GRADED`) changes the weighted sum ($1 \to 3 \to 5$).
- A newly enrolled subject changes `SUM(subject_id)`.
- A newly received or read inbox message changes `MAX(id)` or `SUM(is_read)`.

---

## 5. Cryptographic Password Hashing & Transparent Migration Algorithm

### 5.1 Salted SHA-256 Hashing Algorithm (`PasswordUtil`)
Passwords are never stored in plaintext. Hashing applies a 16-byte cryptographically secure random salt:

$$S \leftarrow \text{SecureRandom}(16)$$
$$H \leftarrow \text{SHA-256}(S \parallel \text{PlainPassword})$$
$$\text{StoredHash} = \text{"SHA256:"} \parallel \text{Base64}(S) \parallel \text{":"} \parallel \text{Base64}(H)$$

```java
public static String hash(String plainPassword) {
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    String saltB64 = Base64.getEncoder().encodeToString(salt);
    String hashB64 = computeHash(plainPassword, salt);
    return "SHA256:" + saltB64 + ":" + hashB64;
}
```

### 5.2 Constant-Time Verification & Side-Channel Mitigation
To prevent timing attacks where string comparisons terminate early on byte mismatches, verification utilizes `MessageDigest.isEqual()`:

```java
MessageDigest.isEqual(
    expectedHashBytes,
    actualHashBytes
);
```

### 5.3 Transparent Migration Algorithm
When legacy test accounts login with unhashed passwords:
1. `PasswordUtil.verify()` detects lack of `SHA256:` prefix and validates against plaintext.
2. If authentication succeeds, `PasswordUtil.needsUpgrade(storedPassword)` returns `true`.
3. The system generates a salted hash and updates the database row in the background:
```sql
UPDATE users SET password = ? WHERE id = ?;
```

---

## 6. Two-Factor Authentication (2FA) & One-Time Password (OTP) Engine

### 6.1 OTP Generation & Expiration Algorithm (`EmailService`)
```
[User Action: Login / Reset Password / Email Change]
                     │
                     ▼
1. Generate 6-Digit Cryptographic Code:
   code = SecureRandom.nextInt(900000) + 100000; [100000 - 999999]
                     │
                     ▼
2. Establish Expiry Timestamp:
   expiry = System.currentTimeMillis() + (5 * 60 * 1000); [5 Minutes]
                     │
                     ▼
3. Store in ConcurrentHashMap:
   OTP_CACHE.put(email, new OtpEntry(code, expiry, attempts: 0));
                     │
                     ▼
4. Send Email via Brevo SMTP / Gmail SSL Socket
```

### 6.2 Rate Limiting & Verification State Machine
Validation returns one of five discrete statuses:
```
                    [verifyOtp(email, enteredCode)]
                                  │
                       [Key in OTP_CACHE?]
                          ├── NO  ──► NOT_FOUND
                          └── YES ──► Check Expiry
                                          │
                            [Current Time > Expiry?]
                               ├── YES ──► Remove key ──► EXPIRED
                               └── NO  ──► Check Attempt Count
                                               │
                                [Attempts >= 5?]
                                   ├── YES ──► Remove key ──► TOO_MANY_ATTEMPTS
                                   └── NO  ──► Check Code Equality
                                                   │
                                     [code == enteredCode?]
                                        ├── YES ──► Remove key ──► SUCCESS
                                        └── NO  ──► attempts++  ──► INVALID_CODE
```

### 6.3 Low-Level SMTP TLS/SSL Socket Implementation
`EmailService` does not rely on third-party mail libraries. It implements RFC-5321 SMTP directly via Java SE `Socket` and `SSLSocket`:
1. Opens socket connection to `smtp-relay.brevo.com:587`.
2. Reads greeting `220`.
3. Sends `EHLO localhost`.
4. Sends `STARTTLS` and waits for `220 Ready to start TLS`.
5. Upgrades plain socket to `SSLSocket` via `SSLSocketFactory.getDefault().createSocket(...)`.
6. Executes TLS handshake `sslSocket.startHandshake()`.
7. Authenticates with base64 encoded login and API key (`AUTH LOGIN`).
8. Transmits envelope: `MAIL FROM:<...>`, `RCPT TO:<...>`, `DATA`.
9. Delivers styled HTML email template and sends `QUIT`.

---

## 7. Email Deliverability, DNS MX & Reputation Validation Algorithm

To prevent bogus email registrations and SMTP rejections, `GmailLookupUtil` conducts a 5-tier pipeline:

```
[Input Email]
     │
     ├──► 1. RFC Structure Regex Validation (EMAIL_PATTERN)
     │         └── Fails if syntax violates RFC-5322
     │
     ├──► 2. Gmail Specific Constraints (if @gmail.com)
     │         ├── Username length: 6 to 30 characters
     │         ├── Allowed characters: alphanumeric and dots [a-zA-Z0-9.]
     │         └── No consecutive dots ("..") or leading/trailing dots
     │
     ├──► 3. Domain Typo Heuristic Detection
     │         └── Flag common typos: @gamil.com, @gmial.com, @yaho.com, @hotmial.com
     │
     ├──► 4. Live DNS MX Record Evaluation (JNDI InitialDirContext)
     │         └── Queries DNS for Mail Exchange records; fails if domain has no MX
     │
     └──► 5. AbstractAPI Email Reputation REST Query (5-Key Rotation Pool)
               ├── Round-robin key rotation: currentKeyIndex.getAndIncrement() % 5
               ├── Checks: deliverability ("DELIVERABLE"), spam score, disposable address
               └── Results cached in VALIDATION_CACHE to eliminate redundant API calls
```

---

## 8. Role-Based Access Control (RBAC) & Course Scoping Algorithm

### 8.1 Role Permission Matrix
AcadsCatchUp enforces strict role boundaries:

| Permission / Action | Student | Professor | Admin (F4TAL) |
| :--- | :---: | :---: | :---: |
| View Personal Deficiencies | Yes | — | — |
| Submit Remediation Work | Yes | — | — |
| View Enrolled Subjects | Yes | — | — |
| In-App Messaging & Alerts | Yes | Yes | Yes |
| Submit Helpdesk Report | Yes | Yes | — |
| Grade Submissions | — | Yes | — |
| Create & Assign Deficiencies | — | Yes | Yes |
| Batch Enroll Students | — | Yes | Yes |
| Export Records to CSV | — | Yes | Yes |
| Manage User Accounts | — | — | Yes |
| Manage Curriculum Subjects | — | — | Yes |
| View System Helpdesk Reports | — | — | Yes |
| Configure SMTP & 2FA Keys | — | — | Yes |

### 8.2 Professor Course Scoping Algorithm
Professors may teach multiple subjects and multiple year levels. To isolate sensitive student records:
1. On login, retrieve professor's assigned subjects from `professor_subjects`:
   $$\text{AssignedSubjects} = \{ s \in \text{Subjects} \mid (profId, s.id) \in \text{professor\_subjects} \}$$
2. The UI Scope Selector builds filter predicates:
   - **"All My Subjects"**: `WHERE subject_id IN (AssignedSubjects)`
   - **"Only [Code] Students"**: `WHERE subject_id = selectedSubjectId`
   - **"Year Level Filter"**: `AND (u.year_level = selectedYear OR selectedYear = 0)`
3. Deficiencies assigned by other faculty are strictly excluded from queries.

---

## 9. Academic Deficiency & Remediation Lifecycle State Machine

Deficiencies move through a three-stage lifecycle:

```
                  [Professor Creates Deficiency]
                               │
                               ▼
                        ┌──────────────┐
                        │   PENDING    │◄────── Overdue Evaluation:
                        └──────┬───────┘        Current Date > Deadline
                               │
                [Student Submits Remediation]
                (With Notes, Links, or Files)
                               │
                               ▼
                        ┌──────────────┐
                        │  SUBMITTED   │
                        └──────┬───────┘
                               │
                [Professor Reviews & Assigns Grade]
                               │
                               ▼
                        ┌──────────────┐
                        │    GRADED    │
                        └──────────────┘
```

### 9.1 Overdue Calculation Algorithm
```java
public boolean isOverdue() {
    if ("GRADED".equalsIgnoreCase(status) || "SUBMITTED".equalsIgnoreCase(status)) {
        return false;
    }
    if (deadline == null || deadline.isBlank()) {
        return false;
    }
    try {
        LocalDate due = LocalDate.parse(deadline);
        return LocalDate.now().isAfter(due);
    } catch (Exception e) {
        return false;
    }
}
```

### 9.2 Submission & Automated Alert Flow
When a student submits via `SubmitItemController`:
1. `missed_items` is updated (`status = 'SUBMITTED'`, `attachment_url`, `notes`).
2. An automated notification is inserted into `inbox_messages` targeted to the assigned professor.
3. `LiveSyncService` detects the mutation within 4-8 seconds.
4. Professor's dashboard displays a desktop toast and plays an audio notification.

---

## 10. Responsive UI Layout & Dynamic View Scaling Algorithm

To support screens from small laptop displays (1366x768) to high-resolution monitors (4K / 1440p), AcadsCatchUp implements dynamic scaling:

### 10.1 Window Centering & Minimum Boundaries (`WindowUtil`)
```java
public static void initFullScreenWithCentering(Stage stage, double minWidth, double minHeight) {
    stage.setMinWidth(minWidth);
    stage.setMinHeight(minHeight);

    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    double targetWidth = Math.min(screenBounds.getWidth() * 0.85, 1280);
    double targetHeight = Math.min(screenBounds.getHeight() * 0.85, 820);

    stage.setWidth(targetWidth);
    stage.setHeight(targetHeight);
    stage.setX((screenBounds.getWidth() - targetWidth) / 2);
    stage.setY((screenBounds.getHeight() - targetHeight) / 2);
}
```

### 10.2 Dynamic Wrapping & View Hierarchy (`ResponsiveLayoutUtil`)
- **Version Selector FlowPane**: Uses `FlowPane` with `hgap = 8` and `vgap = 8` to wrap version badges without horizontal scroll clipping.
- **Table Auto-Resizing**: Table columns apply `CONSTRAINED_RESIZE_POLICY` with percentage-weighted column constraints to prevent text cutoff.
- **Overlay Management**: Modals utilize a root `StackPane` with semi-transparent backdrop (`rgba(0, 0, 0, 0.65)`) to focus user attention and prevent background click interactions.
