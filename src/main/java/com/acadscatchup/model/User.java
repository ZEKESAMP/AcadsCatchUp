package com.acadscatchup.model;

/**
 * User model — shared by Professor, Student, and Admin.
 * Students have additional profile fields: program and yearLevel.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class User {

    public static final String DEVELOPER = "F4TAL";

    private int    id;
    private String username;
    private String fullName;
    private String email;
    private String role;        // "PROFESSOR", "STUDENT", "ADMIN"
    private String program;     // e.g. "BSIT", "BSCS" — students only
    private int    yearLevel;   // 1–4 — students only
    private String assignedSubject; // e.g. "Computer Programming" — professors only
    private boolean isVerified = false;

    public User() {}

    public User(int id, String username, String fullName, String role) {
        this(id, username, fullName, role, null);
    }

    public User(int id, String username, String fullName, String role, String email) {
        this.id       = id;
        this.username = username;
        this.fullName = fullName;
        this.role     = role;
        this.email    = email;
    }

    public User(int id, String username, String fullName, String role,
                String program, int yearLevel) {
        this(id, username, fullName, role, null, program, yearLevel);
    }

    public User(int id, String username, String fullName, String role, String email,
                String program, int yearLevel) {
        this(id, username, fullName, role, email);
        this.program   = program;
        this.yearLevel = yearLevel;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────
    public int    getId()       { return id; }
    public void   setId(int id) { this.id = id; }

    public String getUsername()         { return username; }
    public void   setUsername(String u) { this.username = u; }

    public String getFullName()             { return fullName; }
    public void   setFullName(String name)  { this.fullName = name; }

    public String getEmail()            { return email; }
    public void   setEmail(String email){ this.email = email; }

    public String getRole()             { return role; }
    public void   setRole(String role)  { this.role = role; }

    public String getProgram()              { return program; }
    public void   setProgram(String p)      { this.program = p; }

    public int    getYearLevel()            { return yearLevel; }
    public void   setYearLevel(int y)       { this.yearLevel = y; }

    /** Returns a readable year string, e.g. "2nd Year" */
    public String getYearDisplay() {
        return switch (yearLevel) {
            case 1 -> "1st Year";
            case 2 -> "2nd Year";
            case 3 -> "3rd Year";
            case 4 -> "4th Year";
            default -> "";
        };
    }

    public String getAssignedSubject() { return assignedSubject; }
    public void   setAssignedSubject(String as) { this.assignedSubject = as; }

    public boolean isProfessor() { return "PROFESSOR".equals(role); }
    public boolean isStudent()   { return "STUDENT".equals(role); }
    public boolean isAdmin()     { return "ADMIN".equals(role); }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean v) { this.isVerified = v; }

    @Override
    public String toString() {
        if (id == 0) return fullName; // For "All Students" placeholder
        if (program != null && !program.isBlank()) {
            return fullName + " (" + program + (yearLevel > 0 ? " • " + getYearDisplay() : "") + ")";
        }
        return fullName;
    }
}
