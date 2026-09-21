<p align="center">
  <a href="https://github.com/ZEKESAMP/AcadsCatchUp">
    <img src="src/main/resources/com/acadscatchup/img/Acads_Catch_UPp-removebg-preview.png" alt="AcadsCatchUp" width="180" />
  </a>
</p>

# AcadsCatchUp — Academic Deficiency & Remediation Management System

<p align="center">
  <img src="https://img.shields.io/badge/Java-21_LTS-007396?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21 LTS" />
  <img src="https://img.shields.io/badge/JavaFX-21.0.4-FF6F00?style=for-the-badge&logo=java&logoColor=white" alt="JavaFX" />
  <img src="https://img.shields.io/badge/Database-MySQL_%7C_SQLite-005C84?style=for-the-badge&logo=mysql&logoColor=white" alt="Database" />
  <img src="https://img.shields.io/badge/SQA_Tests-100%25_Passing-10B981?style=for-the-badge&logo=checkmarx&logoColor=white" alt="SQA Tests Passed" />
  <img src="https://img.shields.io/badge/Security-DeveloperGuard_Active-6366F1?style=for-the-badge&logo=dependabot&logoColor=white" alt="Security" />
  <img src="https://img.shields.io/badge/Version-v1.1.0--PROD--F4TAL-blue?style=for-the-badge" alt="Production Version" />
</p>

An academic task management, deficiency tracking, and remediation management desktop application engineered for higher education institutions. AcadsCatchUp bridges students, professors, and academic administrators in an audited, real-time synchronized environment for managing missed coursework (Activities, Quizzes, Examinations, and Assignments).

---

## Table of Contents
- [Overview & Core Highlights](#overview--core-highlights)
- [System Architecture](#system-architecture)
- [Feature Matrix & Role Permissions](#feature-matrix--role-permissions)
- [Capstone Academic Documentation Suite](#capstone-academic-documentation-suite)
- [Software Quality Assurance & Automated Testing](#software-quality-assurance--automated-testing)
- [Security & Anti-Tampering Engine](#security--anti-tampering-engine)
- [Installation & Multi-Platform Deployment](#installation--multi-platform-deployment)
- [Authors & Credits](#authors--credits)

---

## Overview & Core Highlights

- **Real-Time Data Synchronization (`LiveSyncService`):** Background mutation daemon evaluating microsecond aggregate checksums (<10ms server overhead) to push live updates across distributed client dashboards.
- **Dual-Engine Persistence:** Automatic primary connection to TiDB Serverless Cloud MySQL with transparent offline failover to an embedded, zero-install SQLite engine (`acadscatchup.db`).
- **Runtime Anti-Tamper Security (`DeveloperGuard`):** Reflection-based classpath integrity gate enforcing signature verification (`DEVELOPER = "F4TAL"`) across all compiled classes at boot time.
- **Pure Java Network Protocols:** Native zero-dependency SMTP over SSL (Port 465) and HTTP REST clients for Two-Factor Authentication (2FA) and password resets.
- **Clean Code Architecture:** Completely free of code duplication and compiler warnings (`-Xlint:all` clean), with unified presentation utilities (`UIUtil`) and responsive layout support (`ResponsiveLayoutUtil`).

---

## System Architecture

AcadsCatchUp adheres to a strict multi-tier Model-View-Controller (MVC) and Data Access Object (DAO) architecture:

```text
AcadsCatchUp
├── com.acadscatchup
│   ├── AppLauncher.java              # Standalone fat JAR bootstrap & CLI router
│   ├── Main.java                     # JavaFX application entry & lifecycle
│   ├── controller                    # MVC View Controllers
│   │   ├── LoginController.java
│   │   ├── StudentDashboardController.java
│   │   ├── ProfDashboardController.java
│   │   ├── AdminDashboardController.java
│   │   ├── ManageUsersController.java
│   │   └── ...
│   ├── dao                           # Data Access Object persistence layer
│   │   ├── UserDAO.java
│   │   ├── SubjectDAO.java
│   │   ├── MissedItemDAO.java
│   │   ├── InboxDAO.java
│   │   └── HelpReportDAO.java
│   ├── db                            # Database connection pooling & failover
│   │   └── DBConnection.java
│   ├── model                         # Domain entities
│   │   ├── User.java
│   │   ├── Subject.java
│   │   ├── MissedItem.java
│   │   └── ...
│   └── util                          # Core utilities & security mechanisms
│       ├── DeveloperGuard.java       # Runtime reflection signature integrity gate
│       ├── UIUtil.java               # Centralized presentation routines & logout
│       ├── PasswordUtil.java         # Salted SHA-256 hashing & migration
│       ├── EmailService.java         # SMTP client, OTP generator & 2FA engine
│       ├── LiveSyncService.java      # Asynchronous mutation detection daemon
│       └── CSVExporter.java          # Academic checklist spreadsheet exporter
└── src/test/java                     # SQA Automated Testing Suite
    └── com.acadscatchup
        ├── util/PasswordUtilTest.java
        ├── util/DeveloperGuardTest.java
        ├── util/EmailServiceTest.java
        ├── util/CSVExporterTest.java
        ├── model/ModelIntegrityTest.java
        └── test/CapstoneTestRunner.java
```

---

## Feature Matrix & Role Permissions

| Capability | Student | Professor | Administrator |
| :--- | :---: | :---: | :---: |
| View Personal Academic Deficiencies | Yes | — | — |
| Submit Remediation Files & Notes | Yes | — | — |
| In-App Messaging & System Alerts | Yes | Yes | Yes |
| Submit Helpdesk / Bug Report | Yes | Yes | — |
| Review, Grade & Resolve Submissions | — | Yes | — |
| Record & Assign Deficiencies | — | Yes | Yes |
| Enroll & Manage Students | — | Yes | Yes |
| Export Deficiency Checklists to CSV | — | Yes | Yes |
| Manage Professor Accounts | — | — | Yes |
| Configure Global SMTP & Security Keys | — | — | Yes |
| System Auditing & User Deletion | — | — | Yes |

---

## Capstone Academic Documentation Suite

The project includes an exhaustive academic documentation package located in [`docs/`](docs/):

1. [**docs/CAPSTONE_SYSTEM_DOCUMENTATION.md**](docs/CAPSTONE_SYSTEM_DOCUMENTATION.md): Comprehensive 580+ line system architecture manual, complete relational schema (DDL), and technology stack dictionary.
2. [**docs/CAPSTONE_ALGORITHM_SPECIFICATION.md**](docs/CAPSTONE_ALGORITHM_SPECIFICATION.md): Formal computational complexity ($O$) analysis, B-Tree index scans, 2FA OTP state machines, and dual-engine failover algorithms.
3. [**docs/CAPSTONE_PROJECT_DEFENSE_GUIDE.md**](docs/CAPSTONE_PROJECT_DEFENSE_GUIDE.md): Executive oral defense handbook featuring slide presentation outlines, panelist Q&A preparation, and ISO/IEC 25010 quality model alignment.

---

## Software Quality Assurance & Automated Testing

AcadsCatchUp includes a zero-dependency standalone SQA test runner (`CapstoneTestRunner`) alongside standard Maven JUnit 5 configurations.

### Running the Test Suite
Execute the 1-click test script from the repository root:
```cmd
.\run_tests.bat
```
Or execute via Maven:
```cmd
mvn test
```

### Verified SQA Test Coverage
- **`PasswordUtilTest`:** Salted SHA-256 format, salt uniqueness, verification success/rejection, and transparent legacy plaintext migration.
- **`DeveloperGuardTest`:** Classpath reflection scan verifying `DEVELOPER = "F4TAL"` signature across all classes.
- **`EmailServiceTest`:** OTP purpose resolver routing (Login, Reset, Email Change, Activation) and null input validation.
- **`CSVExporterTest`:** Header structure, RFC 4180 delimiter escaping, and multi-row deficiency checklist exports.
- **`ModelIntegrityTest`:** Domain model role predicates, dynamic overdue calculations, and string representations.

**Overall Test Pass Rate: 100.0% (18/18 Unit Tests Passed)**

---

## Security & Anti-Tampering Engine

| Security Layer | Implementation Details |
| :--- | :--- |
| **Password Storage** | Salted SHA-256 with 16-byte cryptographically secure random salts (`SecureRandom`). Plaintext passwords are never stored. |
| **Transparent Migration** | Legacy passwords automatically upgrade to salted SHA-256 hashes upon successful authentication. |
| **Two-Factor Authentication (2FA)** | Time-limited 6-digit numeric OTPs dispatched over SSL (Port 465) with a 5-minute expiry and 5-attempt rate-limiting. |
| **Credential Masking** | Cloud database credentials and API keys are obfuscated via XOR masking (`OBF_KEY = 0x5A`) to prevent decompiler inspection. |
| **Anti-Tampering Gate** | `DeveloperGuard` scans the runtime classpath before application launch to guarantee code integrity. |

---

## Installation & Multi-Platform Deployment

Download pre-built distributions from the [Releases](https://github.com/ZEKESAMP/AcadsCatchUp/releases) page.

### Windows (Setup Wizard or Standalone)
1. Download `AcadsCatchUp-Setup.jar` or run `Install_AcadsCatchUp.bat` for the pure Java Inno Setup-style installation wizard.
2. Alternatively, download the portable `AcadsCatchUp-v1.0.zip` or `AcadsCatchUp.exe`.

### Linux (Debian / Ubuntu / Fedora / Arch)
1. Ensure Java 21 LTS is installed:
   ```bash
   sudo apt update && sudo apt install openjdk-21-jre openjfx
   ```
2. Extract `AcadsCatchUp-Linux.tar.gz` and execute:
   ```bash
   chmod +x Launch_AcadsCatchUp.sh
   ./Launch_AcadsCatchUp.sh
   ```

### Standalone Cross-Platform Fat JAR
Execute directly on any system equipped with Java 21+:
```bash
java -jar AcadsCatchUp.jar
```

---

## Authors & Credits

- **Stevenson James G. Gastanes (F4TAL)** — Lead Developer, System Architect & Author  
  GitHub: [@ZEKESAMP](https://github.com/ZEKESAMP)
- **Technical Documentation & Capstone Dossier:** [@n-mee (Jnzl)](https://github.com/n-mee)

---
*AcadsCatchUp • Engineered with Java 21 LTS & JavaFX*
