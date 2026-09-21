# AcadsCatchUp — Comprehensive System Documentation & Technical Reference Manual

> **Document Type:** Official Academic Capstone Technical Reference Manual & System Architecture Specification  
> **Target System:** AcadsCatchUp (Desktop Academic Deficiency & Remediation Management System)  
> **Lead Developer & System Architect:** Stevenson James G. Gastanes (Alias: **F4TAL**)  
> **Documentation Author:** [@n-mee (Jnzl)](https://github.com/n-mee)  
> **Milestone Status:** Capstone Final Defense & Production Release  
> **Current Version:** v1.1.0-PROD-F4TAL  
> **Architecture Pattern:** Model-View-Controller (MVC), Data Access Object (DAO), & Centralized UI Framework  
> **Primary Runtime:** Java 21 LTS (Oracle / Eclipse Temurin OpenJDK)  
> **Date of Finalization:** September 2026  

---

## Table of Contents

1. [Executive Summary & System Identity](#1-executive-summary--system-identity)
2. [Complete Programming Languages & Technologies](#2-complete-programming-languages--technologies)
3. [Exhaustive File Types & Extensions Dictionary](#3-exhaustive-file-types--extensions-dictionary)
4. [Cloud Hostings, Backend Services & Infrastructure](#4-cloud-hostings-backend-services--infrastructure)
5. [System Architecture & Software Design Patterns](#5-system-architecture--software-design-patterns)
6. [Complete Database Schema & Relational Model](#6-complete-database-schema--relational-model)
7. [User Roles & Functional Capabilities](#7-user-roles--functional-capabilities)
8. [UI/UX Design System & Theme Specifications](#8-uiux-design-system--theme-specifications)
9. [Build, Packaging & Multi-Platform Distribution](#9-build-packaging--multi-platform-distribution)
10. [Security Controls & Anti-Tampering Protections](#10-security-controls--anti-tampering-protections)

---

## 1. Executive Summary & System Identity

**AcadsCatchUp** is a centralized, enterprise-grade academic task management and deficiency tracking desktop application. Designed specifically for higher education institutions, it provides an audited workflow for students, professors, and academic administrators to record, submit, review, and grade missed coursework (Activities, Quizzes, Examinations, and Assignments) resulting from absences, medical emergencies, or schedule conflicts.

### Key Metrics & Properties
- **Application Name:** AcadsCatchUp
- **Production Build Version:** `1.1.0-PROD-F4TAL`
- **Current Version Tag:** `v1.1.0`
- **Lead Developer:** Stevenson James G. Gastanes (F4TAL)
- **Repository:** [`https://github.com/ZEKESAMP/AcadsCatchUp`](https://github.com/ZEKESAMP/AcadsCatchUp)
- **Supported Platforms:** Windows 10/11 (64-bit), Linux (Debian/Ubuntu/Fedora/Arch), macOS (via Java 21+ JRE)
- **Deployment Models:** Standalone Fat JAR, Portable Windows ZIP with embedded JRE, Dedicated Linux TAR.GZ/ZIP, Native Windows Executable (`.exe`), and GUI Setup Installer.

---

## 2. Complete Programming Languages & Technologies

AcadsCatchUp combines compiled, declarative, scripted, and query languages across its presentation, persistence, build, and security layers:

### 2.1 Primary & Supporting Languages

| Language | Primary Role / Usage in Project | Standard File Extensions |
| :--- | :--- | :--- |
| **Java (Java 21 LTS)** | Core business logic, UI event routing, network protocols, reflection guards, connection pooling, and multi-threaded background daemons. | `.java`, `.class`, `.jar` |
| **FXML (XML Markup)** | Declarative scene-graph definitions for all windows, views, tables, and dialog modals. | `.fxml` |
| **CSS3 (JavaFX CSS)** | Styling, color tokens, dark-mode gradients, state pseudo-classes (`:hover`, `:focused`), and animations. | `.css` |
| **SQL (MySQL / SQLite)** | Relational database schema definitions (DDL), indexes, foreign keys, and transactional data access (DML). | `.sql`, `.db` |
| **PowerShell (v5.1 & v7+)** | Windows packaging scripts, Fat JAR unpack/repack assembly, Unix LF normalization, and DeveloperGuard verification. | `.ps1` |
| **Windows Command Script** | Native Windows launchers, fallback builders, and detached process replacement scripts. | `.bat`, `.cmd` |
| **Unix Shell Script (Bash)** | Native Linux application launch script (`Launch_AcadsCatchUp.sh`) and permission flags. | `.sh` |
| **VBScript** | Windows silent background runner (`Launch_AcadsCatchUp.vbs`) suppressing console windows. | `.vbs` |
| **JSON** | Remote semantic version manifests and auto-update release notes metadata. | `.json` |
| **Java Properties** | Runtime database configuration fallback key-value file. | `.properties` |
| **YAML** | GitHub Actions CI/CD workflows for automated compilation, packaging, and release publishing. | `.yml` |

### 2.2 Frameworks, Drivers & Libraries

| Library / Dependency | Version | Maven Artifact / Provider | Role |
| :--- | :--- | :--- | :--- |
| **JavaFX Controls** | 21.0.4 | `org.openjfx:javafx-controls` | Native UI controls (`TableView`, `ComboBox`, `Button`, `ListView`). |
| **JavaFX FXML** | 21.0.4 | `org.openjfx:javafx-fxml` | Model-View-Controller controller binding and XML view loading. |
| **MySQL Connector/J** | 8.3.0 | `com.mysql:mysql-connector-j` | High-performance JDBC driver for remote TiDB Cloud MySQL cluster. |
| **SQLite JDBC Driver** | 3.45.3.0 | `org.xerial:sqlite-jdbc` | Zero-configuration local embedded SQLite database engine. |
| **OpenCSV** | 5.9 | `com.opencsv:opencsv` | Generates structured CSV spreadsheets for student deficiency checklists. |
| **Apache Commons Lang3** | 3.13.0 | `org.apache.commons:commons-lang3` | String utilities, escaping, and formatting. |
| **Apache Commons BeanUtils** | 1.9.4 | `commons-beanutils:commons-beanutils` | Dynamic property extraction for table cell binding. |
| **SLF4J API** | 1.7.36 | `org.slf4j:slf4j-api` | Standardized logging abstraction. |

---

## 3. Exhaustive File Types & Extensions Dictionary

The repository contains 19 distinct file extensions. Below is the technical catalog describing each file type's purpose and location:

```
AcadsCatchUp
├── .github/workflows/*.yml          ──► CI/CD automation pipelines
├── dist/                            ──► Distribution binaries & portable archives
│   ├── *.jar, *.exe, *.zip, *.tar.gz
│   ├── *.bat, *.sh, *.vbs, *.txt
│   └── acadscatchup.db
├── src/main/java/**/*.java          ──► Compiled Java application sources
├── src/main/resources/
│   ├── com/acadscatchup/css/*.css   ──► Dark-mode JavaFX stylesheet
│   ├── com/acadscatchup/fxml/*.fxml ──► Declarative view layouts
│   ├── com/acadscatchup/img/*.png   ──► Icons and branding assets
│   └── db/*.sql                     ──► Database initialization script
├── *.properties, *.json, *.xml      ──► Configuration, versioning & POM
└── *.ps1, *.bat                     ──► Build, packaging & verification scripts
```

### Detailed File Type Catalog

| Extension | Category | Exact Purpose in AcadsCatchUp | Concrete Example Files |
| :--- | :--- | :--- | :--- |
| **`.java`** | Source Code | Contains all application logic: controllers, models, DAOs, utilities, and security guards. | `AppLauncher.java`, `DBConnection.java`, `EmailService.java`, `LiveSyncService.java` |
| **`.fxml`** | UI Definition | XML-formatted layout trees defining user interfaces for screens and dialogs. | `login.fxml`, `prof_dashboard.fxml`, `student_dashboard.fxml`, `admin_dashboard.fxml` |
| **`.css`** | Stylesheet | Custom stylesheet applying dark-theme color tokens, borders, and button hover states. | `src/main/resources/com/acadscatchup/css/style.css` |
| **`.sql`** | SQL Script | DDL/DML script defining the initial database schema, indexes, and seed records. | `src/main/resources/db/init.sql` |
| **`.db`** | Database | SQLite embedded database file containing local offline tables and records. | `acadscatchup.db`, `dist/AcadsCatchUp-Portable/acadscatchup.db` |
| **`.properties`** | Configuration | Java standard key-value configuration file for external database credentials. | `database.properties`, `src/main/resources/database.properties` |
| **`.json`** | Metadata | Manifest declaring latest available version, download URLs, and release notes. | `version.json` (root and remote on GitHub) |
| **`.xml`** | Build Descriptor | Maven Project Object Model (POM) declaring dependencies, plugins, and JDK target. | `pom.xml` |
| **`.bat`** | Windows Script | Windows command shell scripts for compiling, packaging, launching, and updating. | `build_app.bat`, `run.bat`, `Launch_AcadsCatchUp.bat`, `update_handoff.bat` |
| **`.ps1`** | PowerShell Script | Automation scripts for fat JAR packaging, Linux archive creation, and DeveloperGuard tests. | `package_fatjar.ps1`, `package_linux.ps1`, `verify_developer_guard.ps1` |
| **`.sh`** | Unix Shell Script | Linux launcher script with environment setup and executable path resolution. | `dist/AcadsCatchUp-Linux/Launch_AcadsCatchUp.sh` |
| **`.vbs`** | VBScript | Silent launcher invoking batch files without popping open a Windows command prompt. | `dist/AcadsCatchUp-Portable/Launch_AcadsCatchUp.vbs` |
| **`.png`** | Image Asset | Branding images, blue book icons, and application logos. | `book_icon_blue.png`, `Acads_Catch_UPp-removebg-preview.png` |
| **`.jpg`** | Image Asset | High-resolution background and institutional banner imagery. | `src/main/resources/com/acadscatchup/img/ollc.jpg` |
| **`.ico`** | Icon File | Multi-resolution Windows application icon embedded in `.exe` files and taskbars. | `dist/app_icon.ico` |
| **`.jar`** | Executable Archive | Runnable Java archives containing compiled classes and bundled libraries. | `dist/AcadsCatchUp.jar`, `AcadsCatchUp-Setup.jar` |
| **`.zip`** | Archive | Portable distribution bundles for Windows and Linux users. | `dist/AcadsCatchUp-v1.0.zip`, `dist/AcadsCatchUp-Linux.zip` |
| **`.tar.gz`**| Unix Archive | Standard gzipped tar archive for Unix/Linux distribution. | `dist/AcadsCatchUp-Linux.tar.gz` |
| **`.txt`** | Text Document | System summaries, compiler inputs, and end-user setup documentation. | `PROJECT_SUMMARY.txt`, `HOW_TO_RUN.txt`, `HOW_TO_RUN_LINUX.txt` |
| **`.md`** | Markdown Document | Documentation and algorithmic specifications. | `README.md`, `TEMP_ACADSCATCHUP_ALGORITHM.md` |
| **`.yml`** | CI/CD Workflow | GitHub Actions workflow definitions for continuous integration and automated releases. | `.github/workflows/ci.yml`, `.github/workflows/release.yml` |

---

## 4. Cloud Hostings, Backend Services & Infrastructure

AcadsCatchUp uses cloud infrastructure for persistence, messaging, update distribution, and continuous deployment:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ACADSCATCHUP CLOUD ECOSYSTEM                    │
└────────────────────────────────────────────────────────────────────────┘

 [Central Database Cluster]              [Email & Auth Relay]
  TiDB Cloud Serverless (PingCAP)         Brevo SMTP Relay & Gmail SSL
  AWS ap-southeast-1 (Singapore)          Port 587 (STARTTLS) / Port 465
  gateway01.ap-southeast-1.prod           smtp-relay.brevo.com
  .aws.tidbcloud.com:4000
             ▲                                       ▲
             │                                       │
             │                   Direct              │
             └─────────────┐   TLS/SSL   ┌───────────┘
                           │   Sockets   │
                           │             │
                    ┌──────┴─────────────┴──────┐
                    │    AcadsCatchUp Client    │
                    │   (Java 21 Desktop App)   │
                    └──────┬─────────────┬──────┘
                           │             │
             ┌─────────────┘   HTTPS     └───────────┐
             │                 REST                  │
             ▼                                       ▼
 [Version & Updates CDN]                 [Mailbox Reputation API]
  GitHub Raw & GitHub Releases            AbstractAPI Email Reputation
  raw.githubusercontent.com               api.abstractapi.com
  /ZEKESAMP/AcadsCatchUp                  (5-Key Round-Robin Pool)
```

### 4.1 Database Cloud Hosting: TiDB Cloud Serverless (PingCAP)
- **Service Type:** Distributed SQL (MySQL 8.0 wire-compatible) Cloud Database.
- **Provider:** PingCAP TiDB Cloud Serverless.
- **Underlying Cloud Host:** Amazon Web Services (AWS).
- **Hosting Region:** Asia Pacific — Singapore (`ap-southeast-1`).
- **Endpoint Host:** `gateway01.ap-southeast-1.prod.aws.tidbcloud.com`
- **Connection Port:** `4000` (Standard TiDB Cloud MySQL port).
- **Database Name:** `acadscatchup`
- **Transport Security:** Mandatory TLS/SSL encryption with `allowPublicKeyRetrieval=true` and `serverTimezone=UTC`.
- **High Availability & Sync:** Cloud architecture enables concurrent live syncing across students and faculty from any network or geographic location.
- **Offline Fallback:** Local embedded SQLite database (`acadscatchup.db`) with Write-Ahead Logging (WAL) and foreign key enforcement.

### 4.2 Email Relay & Messaging Hosting
- **Primary SMTP Relay Provider:** Brevo (formerly Sendinblue).
  - Host: `smtp-relay.brevo.com`
  - Port: `587`
  - Protocol: STARTTLS with TLSv1.3 cryptographic socket upgrade.
  - Role: Delivers automated 6-digit 2FA login verification codes, password resets, and account activation notices.
- **Fallback Email Provider:** Google Gmail SMTP.
  - Host: `smtp.gmail.com`
  - Port: `465`
  - Protocol: Direct SSL/TLS (`SSLSocketFactory`).
- **Deliverability & Validation API:** AbstractAPI Email Reputation REST Service.
  - Endpoint: `https://emailvalidation.abstractapi.com/v1/`
  - Key Management: Obfuscated pool of 5 API keys rotated round-robin to provide 500 free real-time reputation checks per month.
  - Verification: Live DNS MX lookup, spam evaluation, disposable domain rejection, and syntax checks.

### 4.3 Version Manifest, Binary CDN & Distribution Hosting
- **Source Code & Project Host:** GitHub ([`https://github.com/ZEKESAMP/AcadsCatchUp`](https://github.com/ZEKESAMP/AcadsCatchUp)).
- **Version Manifest Hosting:** GitHub Raw Content Delivery Network.
  - URL: `https://raw.githubusercontent.com/ZEKESAMP/AcadsCatchUp/main/version.json`
  - Role: Queried asynchronously by `UpdateSplash` during startup to determine update availability without full repo cloning.
- **Binary Download CDN:** GitHub Releases.
  - Release Endpoint: `https://github.com/ZEKESAMP/AcadsCatchUp/releases/download/v1.1.0/`
  - Hosted Artifacts: `AcadsCatchUp.jar` (fat executable), `AcadsCatchUp-v1.0.zip` (portable Windows bundle), `AcadsCatchUp-Linux.tar.gz` (Linux distribution), and `AcadsCatchUp.exe`.
- **Continuous Integration & Delivery (CI/CD):** GitHub Actions.
  - Workflow Runners: `windows-latest` virtual environments.
  - Actions: Automated compilation on push/tags, DeveloperGuard compliance check, fatjar building, Linux artifact generation, and automatic GitHub Release publication.

---

## 5. System Architecture & Software Design Patterns

AcadsCatchUp follows a modular **Model-View-Controller (MVC)** and **Data Access Object (DAO)** architecture:

```
com.acadscatchup
├── AppLauncher.java                  # Standalone bootstrap launcher & CLI dispatcher
├── Main.java                         # JavaFX lifecycle entry point & scene initialization
│
├── controller                        # [CONTROLLER LAYER] View events & business rules
│   ├── LoginController.java          # Authentication, 2FA prompt, session creation
│   ├── StudentDashboardController.java # Student deficiency checklist, instant search
│   ├── ProfDashboardController.java  # Professor deficiency grading & scoped views
│   ├── AdminDashboardController.java # Master administrator management console
│   ├── ManageUsersController.java    # Account provisioning, year level & subject assign
│   ├── EnrollStudentController.java  # Batch course enrollment modal
│   ├── SubmitItemController.java     # Student remediation file/link submit modal
│   ├── AddEditItemController.java    # Deficiency creation & editing dialog
│   ├── AddSubjectController.java     # Subject catalog management dialog
│   ├── UserInboxController.java      # In-app messaging & alert interface
│   ├── AdminInboxController.java     # Helpdesk report audit & administrative replies
│   ├── HelpReportController.java     # User support request dispatch dialog
│   └── FAQController.java            # Interactive help & system FAQ viewer
│
├── dao                               # [DATA ACCESS LAYER] Parameterized SQL abstraction
│   ├── UserDAO.java                  # User CRUD, role management, salted SHA-256 login
│   ├── SubjectDAO.java               # Course catalog, faculty teaching assignments
│   ├── MissedItemDAO.java            # Academic deficiencies, submissions, grading
│   ├── InboxDAO.java                 # In-app notifications & student alerts
│   └── HelpReportDAO.java            # System auditing & user feedback submissions
│
├── db                                # [PERSISTENCE LAYER] Dual database engine
│   └── DBConnection.java             # TiDB Cloud MySQL, connection pool, SQLite fallback
│
├── model                             # [MODEL LAYER] Domain entity representations
│   ├── User.java                     # System user entity (ID, username, role, year)
│   ├── Subject.java                  # Course entity (ID, code, name)
│   ├── MissedItem.java               # Academic deficiency record
│   ├── InboxMessage.java             # Direct messaging & alert notification entity
│   └── HelpReport.java               # Helpdesk ticket entity
│
├── installer                         # [DEPLOYMENT LAYER] Setup & removal wizards
│   ├── AcadsCatchUpInstaller.java    # Standalone desktop installation wizard
│   └── Uninstaller.java              # Clean uninstaller wizard
│
└── util                              # [UTILITY LAYER] Cross-cutting concerns
    ├── DeveloperGuard.java           # Runtime class reflection integrity scanner
    ├── PasswordUtil.java             # Salted SHA-256 password hashing & migration
    ├── EmailService.java             # Low-level SMTP engine & 6-digit OTP generator
    ├── GmailLookupUtil.java          # DNS MX verification & AbstractAPI validator
    ├── LiveSyncService.java          # Real-time background mutation detection daemon
    ├── UpdateSplash.java             # Discord-style splash & self-updating handoff
    ├── UpdatesDialog.java            # Interactive release notes & changelog modal
    ├── UpdateNoticeUtil.java         # Automated What's New inbox broadcaster
    ├── CSVExporter.java              # Structured CSV checklist generation
    ├── AppTrayManager.java           # Windows system tray close-to-tray manager
    ├── WindowsNotificationUtil.java  # Native OS toast alerts & sound dispatcher
    ├── InAppNotification.java        # Non-intrusive animated in-app toast banners
    ├── LoadingOverlay.java           # Asynchronous UI thread spinner blocker
    ├── ModalOverlay.java             # Undecorated dark-theme dialog wrapper
    ├── CustomAlert.java              # Styled confirmation & warning modals
    ├── PasswordToggleHelper.java     # Eye icon show/hide password helper
    ├── ResponsiveLayoutUtil.java     # Dynamic resolution & layout scaler
    ├── WindowUtil.java               # Fullscreen centering & screen-bound calculator
    ├── OSCompat.java                 # Cross-platform font & emoji normalizer
    └── Session.java                  # Global authenticated user state holder
```

### Design Patterns Implemented
1. **Model-View-Controller (MVC):** Strict boundary between XML view definitions (`.fxml`), domain models (`model.*`), and event controllers (`controller.*`).
2. **Data Access Object (DAO):** Isolates all SQL statements within dedicated DAO classes, guaranteeing parameterized `PreparedStatement` usage against SQL injection.
3. **Dynamic Proxy Connection Pool:** `DBConnection` wraps physical JDBC connections in a `java.lang.reflect.Proxy` to intercept `.close()`, returning connections to a `BlockingQueue` instead of severing remote TLS sockets.
4. **Observer / Listener Pattern:** `LiveSyncService.SyncListener` registers callbacks executed on the JavaFX Application Thread (`Platform.runLater()`) when database fingerprints mutate.
5. **Failover / Dual Engine Strategy:** Dynamic switching between cloud MySQL and local SQLite based on live socket reachability.
6. **Integrity Guard Scanner:** Reflection-based fail-fast verification auditing class bytecodes against modification.

---

## 6. Complete Database Schema & Relational Model

The relational database architecture is defined in `init.sql` and reinforced dynamically by `DBConnection.java`:

```
               ┌───────────────────────────┐
               │           users           │
               ├───────────────────────────┤
               │ id (PK)                   │◄──────┐
               │ username (UQ)             │       │
               │ password                  │       │
               │ full_name                 │       │
               │ email                     │       │
               │ role                      │       │
               │ program                   │       │
               │ year_level                │       │
               │ is_verified               │       │
               │ created_at                │       │
               └─────────────┬─────────────┘       │
                             │                     │
      ┌──────────────────────┼─────────────────────┼──────────────────────┐
      │ 1:N                  │ 1:N                 │ 1:N                  │ 1:N
      ▼                      ▼                     ▼                      ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│ enrollments  │      │professor_sub │      │ missed_items │      │inbox_messages│
├──────────────┤      ├──────────────┤      ├──────────────┤      ├──────────────┤
│ student_id*  │      │professor_id* │      │ id (PK)      │      │ id (PK)      │
│ subject_id*  │      │subject_id*   │      │ student_id*  │      │ sender_id*   │
└──────┬───────┘      └──────┬───────┘      │ subject_id*  │      │ recipient_id*│
       │                     │              │ item_type    │      │ title        │
       │ N:1                 │ N:1          │ item_name    │      │ message      │
       └──────────────┬──────┘              │ date_missed  │      │ msg_type     │
                      ▼                     │ deadline     │      │ is_read      │
               ┌──────────────┐             │ status       │      │ created_at   │
               │   subjects   │             │ notes        │      └──────────────┘
               ├──────────────┤             │ created_by*  │
               │ id (PK)      │◄────────────┤ attachment_* │
               │ code (UQ)    │             │ created_at   │
               │ name         │             └──────────────┘
               └──────────────┘
```

### 6.1 Tables & Schema Definitions

#### 1. `users` Table
Stores authenticated credentials, roles, and profile metadata.
```sql
CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(150) DEFAULT NULL,
    role       VARCHAR(20) NOT NULL, -- 'ADMIN', 'PROFESSOR', 'STUDENT'
    program    VARCHAR(100) DEFAULT 'BSIT',
    year_level INT DEFAULT 0,        -- 0 = All Years, 1 = 1st Year, etc.
    is_verified TINYINT DEFAULT 0,   -- 1 = Email OTP verified
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
```

#### 2. `subjects` Table
Master catalog of dedicated curriculum courses.
```sql
CREATE TABLE IF NOT EXISTS subjects (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL
) ENGINE=InnoDB;
```

#### 3. `enrollments` Table
Many-to-many relationship mapping students to their enrolled subjects.
```sql
CREATE TABLE IF NOT EXISTS enrollments (
    student_id INT NOT NULL,
    subject_id INT NOT NULL,
    PRIMARY KEY (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

#### 4. `professor_subjects` Table
Many-to-many relationship mapping faculty members to assigned teaching subjects.
```sql
CREATE TABLE IF NOT EXISTS professor_subjects (
    professor_id INT NOT NULL,
    subject_id   INT NOT NULL,
    PRIMARY KEY (professor_id, subject_id),
    FOREIGN KEY (professor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id)   REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

#### 5. `missed_items` Table
Core deficiency records tracking missed student requirements and submissions.
```sql
CREATE TABLE IF NOT EXISTS missed_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    subject_id      INT NOT NULL,
    item_type       VARCHAR(30) NOT NULL, -- 'ACTIVITY', 'QUIZ', 'EXAM', 'ASSIGNMENT'
    item_name       VARCHAR(150) NOT NULL,
    date_missed     VARCHAR(50) NOT NULL,
    deadline        VARCHAR(50) DEFAULT NULL,
    status          VARCHAR(30) DEFAULT 'PENDING', -- 'PENDING', 'SUBMITTED', 'GRADED'
    notes           TEXT DEFAULT NULL,
    created_by      INT DEFAULT NULL,
    attachment_type VARCHAR(20) DEFAULT NULL,
    attachment_name VARCHAR(255) DEFAULT NULL,
    attachment_url  MEDIUMTEXT DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;
```

#### 6. `inbox_messages` Table
Internal notification and communication system.
```sql
CREATE TABLE IF NOT EXISTS inbox_messages (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    sender_id       INT NOT NULL,
    sender_name     VARCHAR(100) NOT NULL,
    sender_role     VARCHAR(20) NOT NULL,
    recipient_id    INT NOT NULL,
    recipient_name  VARCHAR(100) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         TEXT NOT NULL,
    item_id         INT DEFAULT NULL,
    item_name       VARCHAR(150) DEFAULT NULL,
    subject_code    VARCHAR(50) DEFAULT NULL,
    msg_type        VARCHAR(50) NOT NULL, -- 'NOTICE', 'SUBMISSION', 'GRADED', 'ENROLLMENT'
    attachment_type VARCHAR(20) DEFAULT NULL,
    attachment_name VARCHAR(255) DEFAULT NULL,
    attachment_url  MEDIUMTEXT DEFAULT NULL,
    is_read         TINYINT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

#### 7. `email_config` Table
Global configuration for SMTP credentials and 2FA toggles.
```sql
CREATE TABLE IF NOT EXISTS email_config (
    id             INT PRIMARY KEY DEFAULT 1,
    sender_email   VARCHAR(150) DEFAULT NULL,
    app_password   VARCHAR(255) DEFAULT NULL,
    api_key        VARCHAR(255) DEFAULT NULL,
    is_2fa_enabled TINYINT DEFAULT 0,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
```

#### 8. `help_reports` Table
Helpdesk ticket audit table for reporting software issues to administrators.
```sql
CREATE TABLE IF NOT EXISTS help_reports (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    user_name  VARCHAR(100) NOT NULL,
    user_role  VARCHAR(20) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    message    TEXT NOT NULL,
    status     VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

### 6.2 Dedicated Curriculum Subjects
The database seeds 7 core academic subjects:
1. `PE` — Physical Education
2. `RHET` — Rhetoric
3. `CP` — Computer Programming
4. `MTS` — Mathematics, Technology and Science
5. `DSA` — Data Structure and Algorithms
6. `RIZAL` — Life and Works of Rizal
7. `IT-ERA` — Living in the IT Era

---

## 7. User Roles & Functional Capabilities

AcadsCatchUp enforces strict Role-Based Access Control (RBAC):

```
                       ┌─────────────────────────┐
                       │   ADMINISTRATOR (F4TAL) │
                       │    Master System Owner  │
                       └────────────┬────────────┘
                                    │
            ┌───────────────────────┴───────────────────────┐
            │                                               │
            ▼                                               ▼
┌─────────────────────────┐                     ┌─────────────────────────┐
│        PROFESSOR        │                     │         STUDENT         │
│   Instruction & Grading │                     │ Tracking & Remediation  │
└─────────────────────────┘                     └─────────────────────────┘
```

### 7.1 Student Capabilities
- **Deficiency Tracking:** View all assigned make-up items with status indicators (Pending, Submitted, Graded, Overdue).
- **Remediation Submission:** Submit make-up work directly to instructors attaching notes, external links, or local documents.
- **Enrolled Subjects:** Inspect all courses in which the student is currently active.
- **Instant Search:** Zero-latency client-side filtering across subjects, deficiency names, and dates.
- **CSV Export:** Export personal deficiency checklist to CSV for offline logging.
- **In-App Messaging:** Receive notifications for new deficiencies, graded submissions, and course enrollments.
- **Support Reports:** Submit bug reports directly to the master administrator.

### 7.2 Professor Capabilities
- **Scoped Dashboard:** Manage deficiencies isolated to assigned subjects and teaching year levels.
- **Deficiency Authoring:** Record new missed activities, quizzes, exams, and assignments for single or multiple students.
- **Batch Enrollment Modal:** Self-service modal to enroll students into assigned courses via interactive search and checkboxes.
- **Review & Grading:** Inspect student remediation submissions, assign grades, and provide feedback.
- **Live Sync & Alerts:** Receive real-time alerts when students submit remediation work.
- **Data Export:** Export section deficiencies and student rosters to CSV spreadsheets.

### 7.3 Administrator Capabilities (F4TAL)
- **User Account Management:** Provision, edit, verify, or remove Admin, Professor, and Student accounts.
- **Curriculum Assignment:** Assign single or multiple curriculum subjects and year levels to faculty members.
- **Subject Catalog Management:** Add, edit, or delete institutional course offerings.
- **Helpdesk Ticket Resolution:** Audit student and faculty bug reports and send administrative replies.
- **Global Security & SMTP Configuration:** Manage SMTP email credentials, Brevo API keys, and toggle campus-wide 2FA requirements.
- **Full Database Auditing:** Authority over all records with cascade deletion safeguards.

---

## 8. UI/UX Design System & Theme Specifications

AcadsCatchUp uses an enterprise dark-theme palette designed for reduced eye strain during long academic sessions:

### 8.1 Color Tokens & Design Palette

| Token Name | Hex Code / Value | Usage & Visual Role |
| :--- | :--- | :--- |
| **Canvas Base** | `#0f1117` | Root window backdrop and main application frame. |
| **Card / Panel** | `#151825` | Sidebar, navigation headers, stat cards, and modal containers. |
| **Elevated Surface** | `#1a1d2e` | Dialog root surfaces, table rows, and input wrappers. |
| **Border Stroke** | `#2d3255` | Subtle high-contrast dividers, borders, and modal outlines. |
| **Accent Primary** | `#5865f2` | Blurple primary buttons, active tabs, and progress indicators. |
| **Status Emerald** | `#10b981` | "GRADED" badge, "Live Sync" indicator, and success toasts. |
| **Status Amber** | `#f59e0b` | "SUBMITTED" badge, "Reconnecting..." indicator, and warnings. |
| **Status Crimson** | `#ef4444` | "PENDING" / "OVERDUE" badge, "Offline" indicator, and errors. |
| **Text Primary** | `#f8fafc` | High-emphasis headings, titles, and table text. |
| **Text Secondary**| `#94a3b8` | Subtitles, field labels, metadata timestamps, and hints. |

### 8.2 Typography & Styling Elements
- **Font Stack:** `'Segoe UI', -apple-system, BlinkMacSystemFont, 'Inter', sans-serif`
- **Border Radii:** `14px` on modals, `10px` on cards, `8px` on text fields, and `6px` on status badges.
- **Shadows:** Smooth Gaussian drop-shadows (`dropshadow(gaussian, rgba(0,0,0,0.65), 24, 0, 0, 8)`).
- **System Tray:** Windows system tray integration with close-to-tray behavior, preventing accidental exits during live sync monitoring.

---

## 9. Build, Packaging & Multi-Platform Distribution

### 9.1 Build Requirements
- **JDK:** Java Development Kit 21 LTS or newer (configured on `JAVA_HOME`).
- **Build System:** Apache Maven 3.9+ or local PowerShell packaging scripts.
- **Optional Tools:** 7-Zip (`7z`) for Windows archive creation; `tar` for Unix archives.

### 9.2 Build & Packaging Commands

#### 1. Compile & Assemble via Maven:
```bash
mvn clean package
```
Generates shaded runnable JAR in `target/acadscatchup-1.0.0.jar`.

#### 2. Native Windows Build Script (`build_app.bat`):
```cmd
.\build_app.bat
```
- Compiles all classes under `src/main/java`.
- Copies resources from `src/main/resources`.
- Executes `package_fatjar.ps1` to assemble `dist/AcadsCatchUp.jar`.
- Synchronizes runtime folders and updates `AcadsCatchUp-v1.0.zip`.
- Invokes `package_linux.ps1` to generate Linux packages.

#### 3. Linux Distribution Packaging (`package_linux.ps1`):
```powershell
powershell -ExecutionPolicy Bypass -File package_linux.ps1
```
- Formats text files (`Launch_AcadsCatchUp.sh`, `AcadsCatchUp.desktop`) with Unix LF line endings.
- Generates `dist/AcadsCatchUp-Linux.tar.gz` and `dist/AcadsCatchUp-Linux.zip`.

---

## 10. Security Controls & Anti-Tampering Protections

AcadsCatchUp incorporates defensive programming across transport, persistence, and runtime integrity:

| Security Layer | Technical Implementation | Defense Target |
| :--- | :--- | :--- |
| **Password Storage** | Salted SHA-256 using 16-byte random salt (`SecureRandom`) with `MessageDigest.isEqual()` constant-time comparison. | Rainbow tables, brute force, timing side-channel attacks. |
| **Credential Obfuscation**| Sensitive cloud database hosts, users, and passwords obfuscated with multi-byte XOR (`0x5A`). | Static string inspection and decompiler reverse-engineering. |
| **DeveloperGuard Scanner** | Reflection audit requiring `DEVELOPER = "F4TAL"` static field on all compiled classes before execution. | Code tampering, unauthorized forks, and repackaging. |
| **Two-Factor Auth (2FA)**| 6-digit one-time passcode (OTP) with 5-minute lifespan and 5-attempt rate-limiting lock. | Credential stuffing and unauthorized login attempts. |
| **Mailbox Deliverability**| AbstractAPI REST verification and live DNS MX socket validation before dispatching credentials. | Fake accounts, bounce storms, and spam traps. |
| **Transport Security** | TLSv1.3 and STARTTLS on port 587; SSL on port 465; TLS on TiDB Cloud port 4000. | Man-in-the-Middle (MitM) eavesdropping and packet snooping. |
| **SQL Injection Defense** | 100% parameterized queries via JDBC `PreparedStatement` across all DAO components. | Malicious SQL payload injection. |
