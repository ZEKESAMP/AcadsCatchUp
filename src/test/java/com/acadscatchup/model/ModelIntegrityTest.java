package com.acadscatchup.model;

import java.time.LocalDate;

/**
 * Unit tests verifying domain entity integrity, state predicates, and formatting.
 * SQA Verification for Capstone Evaluation.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class ModelIntegrityTest {

    public static final String DEVELOPER = "F4TAL";

    public void testUserRolePredicates() {
        User admin = new User(1, "admin", "System Admin", "ADMIN");
        assert admin.isAdmin() : "Admin role must return isAdmin = true";
        assert !admin.isProfessor() : "Admin is not a professor";
        assert !admin.isStudent() : "Admin is not a student";

        User prof = new User(2, "prof", "Prof. Jane", "PROFESSOR");
        assert prof.isProfessor() : "Professor role must return isProfessor = true";
        assert !prof.isAdmin() : "Professor is not an admin";

        User student = new User(3, "student", "John Student", "STUDENT", "BSIT", 2);
        assert student.isStudent() : "Student role must return isStudent = true";
        assert "2nd Year".equals(student.getYearDisplay()) : "Year display for 2 must be '2nd Year'";
    }

    public void testMissedItemOverdueLogic() {
        MissedItem item = new MissedItem();
        item.setStatus("PENDING");
        item.setDeadline(LocalDate.now().minusDays(2));
        assert item.isOverdue() : "Past deadline pending item must be overdue";

        // Future deadline should not be overdue
        item.setDeadline(LocalDate.now().plusDays(2));
        assert !item.isOverdue() : "Future deadline item must not be overdue";

        // Submitted item should not be overdue even if deadline passed
        item.setStatus("SUBMITTED");
        item.setDeadline(LocalDate.now().minusDays(2));
        assert !item.isOverdue() : "Submitted item must not be marked overdue";
    }

    public void testSubjectDisplay() {
        Subject sub = new Subject(10, "IT101", "Introduction to IT");
        assert "IT101 - Introduction to IT".equals(sub.toString()) : "Subject toString must format as CODE - NAME";
    }
}
