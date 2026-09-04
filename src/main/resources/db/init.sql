-- AcadsCatchUp Database Schema
-- Developed by: F4TAL (Stevenson James G. Gastanes)
-- Project: Computer Programming Final Project
-- Run this once to initialize all tables and seed demo data

USE acadscatchup;

-- ============================================================
-- TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    role       ENUM('PROFESSOR','STUDENT') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subjects (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    code  VARCHAR(20)  UNIQUE NOT NULL,
    name  VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS enrollments (
    student_id  INT NOT NULL,
    subject_id  INT NOT NULL,
    PRIMARY KEY (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS missed_items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    student_id  INT NOT NULL,
    subject_id  INT NOT NULL,
    item_type   ENUM('ACTIVITY','QUIZ','EXAM','ASSIGNMENT') NOT NULL,
    item_name   VARCHAR(150) NOT NULL,
    date_missed DATE NOT NULL,
    deadline    DATE,
    status      ENUM('PENDING','SUBMITTED','GRADED') DEFAULT 'PENDING',
    notes       TEXT,
    created_by  INT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS help_reports (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    user_name  VARCHAR(100) NOT NULL,
    user_role  VARCHAR(20) NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT NOT NULL,
    status     VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Master Administrator account (F4TAL) — ALWAYS PRESERVED
INSERT IGNORE INTO users (username, password, full_name, role) VALUES
('F4TAL', 'SHA256:lpJNC+SU9Yq//MK+jwIxaQ==:MSO0EGwa3wqm9hHE2Lvqn/4k5Mi/qmzmg1LKHrxjvec=', 'System Administrator', 'ADMIN');

-- Subjects
INSERT IGNORE INTO subjects (code, name) VALUES
('CS101', 'Introduction to Computer Science'),
('MATH101', 'College Algebra'),
('ENG101', 'Communication Arts'),
('PHYS101', 'General Physics');


