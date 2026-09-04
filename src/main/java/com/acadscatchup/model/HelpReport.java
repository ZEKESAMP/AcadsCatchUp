package com.acadscatchup.model;

/**
 * Model representing a bug report or help inquiry submitted by a student or professor.
 * Reviewed by Admin (F4TAL).
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class HelpReport {

    public static final String DEVELOPER = "F4TAL";

    private int    id;
    private int    userId;
    private String userName;
    private String userRole;
    private String title;
    private String message;
    private String createdAt;
    private String status; // "OPEN", "RESOLVED"

    public HelpReport() {}

    public HelpReport(int id, int userId, String userName, String userRole, String title, String message, String createdAt, String status) {
        this.id        = id;
        this.userId    = userId;
        this.userName  = userName;
        this.userRole  = userRole;
        this.title     = title;
        this.message   = message;
        this.createdAt = createdAt;
        this.status    = status;
    }

    public int    getId()                 { return id; }
    public void   setId(int id)           { this.id = id; }

    public int    getUserId()             { return userId; }
    public void   setUserId(int userId)   { this.userId = userId; }

    public String getUserName()           { return userName; }
    public void   setUserName(String u)   { this.userName = u; }

    public String getUserRole()           { return userRole; }
    public void   setUserRole(String r)   { this.userRole = r; }

    public String getTitle()              { return title; }
    public void   setTitle(String t)      { this.title = t; }

    public String getMessage()            { return message; }
    public void   setMessage(String m)    { this.message = m; }

    public String getCreatedAt()          { return createdAt; }
    public void   setCreatedAt(String c)  { this.createdAt = c; }

    public String getStatus()             { return status; }
    public void   setStatus(String s)     { this.status = s; }

    public String getRoleBadge() {
        return "PROFESSOR".equalsIgnoreCase(userRole) ? "Professor" : "Student";
    }
}
