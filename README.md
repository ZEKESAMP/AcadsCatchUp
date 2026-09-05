<p align="center">
  <a href="https://github.com/ZEKESAMP/AcadsCatchUp">
    <img src="src/main/resources/com/acadscatchup/img/Acads_Catch_UPp-removebg-preview.png" alt="AcadsCatchUp" width="180" />
  </a>
</p>

# AcadsCatchUp

Academic Task Management System.

Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Role Permissions](#role-permissions)
- [Architecture](#architecture)
- [Installation](#installation)
- [Database Configuration](#database-configuration)
- [Security](#security)
- [Author](#author)

## Overview

AcadsCatchUp is project aims to attempt at solving issues with missed activiy deadlines for students. It provides direct workflows for faculty and students to handle missed coursework (Activities, Quizzes, Examinations, and Assignments).

### Core Highlights

- **Dynamic syncing**: Automatically syncs into cloud for real-time campus-wide academic management system and updates.
- **Role-Based Access Control**: Professors and Students each have different UI and limitations.

## Features

### Student Portal
- View enrolled courses and real-time tracking list.
- Track requirement statuses: Pending, Submitted, Graded.
- Upload remediation submissions with attached notes and files.
- Communicate with professors via built-in inbox.

### Professor Dashboard
- Course and section management.
- Batch enrollment and student directory management.
- Assign deficiencies and set submission deadlines.
- Review student submissions, assign grades, and provide feedback.
- Export deficiency records to CSV format.

### Live Synchronization and Alerts
- Background daemon sync keeps dashboards updated in real time.
- System notifications and audio alerts for incoming messages and status changes.

## Role Permissions
Here's a more in-depth example of Role-Based Access Control system

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

## Security

| Security Layer | Implementation |
| :--- | :--- |
| Password Storage | Salted SHA-256 using 16-byte cryptographic random salt (`SecureRandom`). Plaintext passwords are never stored. |
| Two-Factor Authentication | Time-limited 6-digit one-time passcode (OTP) sent via SMTP for critical actions and authentication. |
| Credential Obfuscation | Stored keys and sensitive constants are obfuscated using multi-byte XOR encoding to mitigate memory and static analysis inspection. |
| Email Deliverability | Multi-endpoint AbstractAPI mailbox validation verifies recipient existence before dispatching sensitive notifications. |
| Transport Security | Encrypted email transport over TLSv1.3 and STARTTLS on port 587. |
| Anti-Tamper Guard | DeveloperGuard runtime verification across 44 compiled classes. |

## Author

**Stevenson James G. Gastanes (F4TAL)**  
Lead Developer and System Architect  
GitHub: [@ZEKESAMP](https://github.com/ZEKESAMP)

*Documented by [@n-mee(Jnzl)](https://github.com/n-mee)*
