<p align="center">
  <a href="https://github.com/ZEKESAMP/AcadsCatchUp">
    <img src="src/main/resources/com/acadscatchup/img/Acads_Catch_UPp-removebg-preview.png" alt="AcadsCatchUp" width="180" />
  </a>
</p>

# AcadsCatchUp

Academic Deficiencies and Remediation Management System.

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-FF6F00?style=flat&logo=javafx&logoColor=white)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![SQLite](https://img.shields.io/badge/SQLite-3-003B57?style=flat&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg?style=flat)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-4E54C8?style=flat)](#installation)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Role Permissions](#role-permissions)
- [Architecture](#architecture)
- [Installation](#installation)
- [Building from Source](#building-from-source)
- [Database Configuration](#database-configuration)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)
- [Author](#author)

---

## Overview

AcadsCatchUp is a desktop application built with JavaFX for tracking, managing, and resolving student academic deficiencies. It provides direct workflows for faculty and students to handle missed coursework (Activities, Quizzes, Examinations, and Assignments) with automated status tracking, direct messaging, two-factor authentication, and dual database support (SQLite and MySQL).

### Core Highlights

- **Embedded and Network Persistence**: Runs out of the box with zero-configuration SQLite or connects to MySQL for campus-wide multi-user deployment.
- **Role-Based Access Control**: Tailored workspaces for Students, Instructors, and Administrators.
- **Automated Verification and Security**: Email deliverability validation via AbstractAPI, salted SHA-256 password hashing, and anti-tamper verification.
- **Cross-Platform**: Operates natively on Windows, Linux, and macOS.

---

## Features

### Student Portal
- View enrolled courses and real-time deficiency lists.
- Track requirement statuses: Pending, Submitted, Graded.
- Upload remediation submissions with attached notes and files.
- Communicate with instructors via in-app inbox.

### Professor Dashboard
- Course and section management.
- Batch enrollment and student directory management.
- Assign deficiencies and set submission deadlines.
- Review student submissions, assign grades, and provide feedback.
- Export deficiency records to CSV format.

### Administrator Console
- Institutional user management (create, audit, assign, and delete accounts).
- Full professor lifecycle management (including account removal).
- Global system settings, SMTP server configuration, and API keys.
- Audit logging and helpdesk report resolution.

### Live Synchronization and Alerts
- Background daemon sync keeps dashboards updated in real time.
- System notifications and audio alerts for incoming messages and status changes.

---

## Role Permissions

| Capability | Student | Professor | Administrator |
| :--- | :---: | :---: | :---: |
| View Personal Deficiencies | Yes | - | - |
| Submit Remediation Files | Yes | - | - |
| In-App Messaging and Alerts | Yes | Yes | Yes |
| Submit Helpdesk Report | Yes | Yes | - |
| Review and Grade Submissions | - | Yes | - |
| Create and Assign Deficiencies | - | Yes | Yes |
| Enroll and Manage Students | - | Yes | Yes |
| Export Data to CSV | - | Yes | Yes |
| Manage Professor Accounts | - | - | Yes |
| Configure Global SMTP and Security Keys | - | - | Yes |
| System Auditing and Account Deletion | - | - | Yes |

---

## Architecture

AcadsCatchUp follows a Model-View-Controller (MVC) and Data Access Object (DAO) architecture:

```text
AcadsCatchUp
├── com.acadscatchup
│   ├── AppLauncher.java              # Application entry point and lifecycle
│   ├── controller                    # UI event handlers and view controllers
│   │   ├── LoginController.java
│   │   ├── StudentDashboardController.java
│   │   ├── ProfDashboardController.java
│   │   ├── ManageUsersController.java
│   │   ├── AdminInboxController.java
│   │   └── ...
│   ├── dao                           # Data Access Object abstraction layer
│   │   ├── UserDAO.java
│   │   ├── SubjectDAO.java
│   │   ├── MissedItemDAO.java
│   │   ├── InboxDAO.java
│   │   └── HelpReportDAO.java
│   ├── db                            # Dual SQLite and MySQL persistence layer
│   │   └── DBConnection.java
│   ├── model                         # Domain entities
│   │   ├── User.java
│   │   ├── Subject.java
│   │   ├── MissedItem.java
│   │   └── InboxMessage.java
│   └── util                          # Security, networking, and validation
│       ├── PasswordUtil.java         # Salted SHA-256 hashing
│       ├── EmailService.java         # SMTP dispatcher and OTP generator
│       ├── GmailLookupUtil.java      # Mailbox deliverability verification
│       ├── DeveloperGuard.java       # Subsystem tamper protection
│       └── LiveSyncService.java      # Real-time background synchronization
```

---

## Installation

Download pre-built distributions from the [Releases](https://github.com/ZEKESAMP/AcadsCatchUp/releases) page.

### Windows
1. Download `AcadsCatchUp.exe` (standalone executable) or `AcadsCatchUp-v1.0.zip` (portable archive).
2. If using the zip archive, extract the files to your preferred directory.
3. Launch `AcadsCatchUp.exe` or execute `Launch_AcadsCatchUp.bat`.

### Linux
1. Install Java 21 and JavaFX:
   ```bash
   sudo apt update && sudo apt install openjdk-21-jre openjfx
   ```
2. Download and extract `AcadsCatchUp-Linux.tar.gz`.
3. Mark the launcher executable and run:
   ```bash
   chmod +x Launch_AcadsCatchUp.sh
   ./Launch_AcadsCatchUp.sh
   ```

### Cross-Platform JAR
Run the standalone JAR file using Java 21+:
```bash
java -jar AcadsCatchUp.jar
```

---

## Building from Source

### Prerequisites
- Java Development Kit (JDK) 21 or higher
- Git

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/ZEKESAMP/AcadsCatchUp.git
   cd AcadsCatchUp
   ```

2. Build on Windows:
   Run the build script:
   ```cmd
   build_app.bat
   ```
   The script compiles sources, verifies DeveloperGuard signatures, bundles resources, and outputs `dist/AcadsCatchUp.jar`.

3. Build on Linux or macOS:
   ```bash
   mkdir -p target/classes

   javac -encoding UTF-8 -d target/classes \
     -cp "target/libs/*:target/libs/javafx-sdk-21/lib/*" \
     $(find src/main/java -name "*.java")

   cp -r src/main/resources/* target/classes/

   jar -cfm dist/AcadsCatchUp.jar target/MANIFEST.MF -C target/classes .
   ```

4. Verify subsystem integrity:
   ```powershell
   pwsh ./verify_developer_guard.ps1
   ```

---

## Database Configuration

### SQLite (Default)
AcadsCatchUp runs in SQLite mode by default. If no MySQL database is configured or reachable, the application automatically initializes a local database file (`acadscatchup.db`). No manual configuration is required.

### MySQL (Optional Network Mode)
For shared multi-user environments:
1. Create a database on MySQL 8.0 or higher:
   ```sql
   CREATE DATABASE acadscatchup CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Initialize the schema:
   ```bash
   mysql -u root -p acadscatchup < src/main/resources/db/init.sql
   ```
3. Update connection parameters in application settings or environment variables.

---

## Security

| Security Layer | Implementation |
| :--- | :--- |
| Password Storage | Salted SHA-256 using 16-byte cryptographic random salt (`SecureRandom`). Plaintext passwords are never stored. |
| Two-Factor Authentication | Time-limited 6-digit one-time passcode (OTP) sent via SMTP for critical actions and authentication. |
| Credential Obfuscation | Stored keys and sensitive constants are obfuscated using multi-byte XOR encoding to mitigate memory and static analysis inspection. |
| Email Deliverability | Multi-endpoint AbstractAPI mailbox validation verifies recipient existence before dispatching sensitive notifications. |
| Transport Security | Encrypted email transport over TLSv1.3 and STARTTLS on port 587. |
| Anti-Tamper Guard | DeveloperGuard runtime verification across 44 compiled classes. |

---

## Contributing

1. Fork the repository.
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "feat: description of change"
   ```
4. Push to the branch:
   ```bash
   git push origin feature/your-feature-name
   ```
5. Open a Pull Request.

All Java classes must maintain the required developer signature constant (`DEVELOPER = "F4TAL"`) to comply with DeveloperGuard checks.

---

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for the full text.

```text
AcadsCatchUp — Academic Deficiencies & Make-Up Tasks Tracking System
Copyright (C) 2026 Stevenson James G. Gastanes (F4TAL)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

---

## Author

**Stevenson James G. Gastanes (F4TAL)**  
Lead Developer and System Architect  
GitHub: [@ZEKESAMP](https://github.com/ZEKESAMP)
