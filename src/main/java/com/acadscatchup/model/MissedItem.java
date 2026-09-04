package com.acadscatchup.model;

import java.time.LocalDate;

/**
 * MissedItem — the core entity representing a missed activity, quiz, exam, or assignment.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class MissedItem {

    public static final String DEVELOPER = "F4TAL";

    private int       id;
    private int       studentId;
    private String    studentName;   // denormalized for display
    private int       subjectId;
    private String    subjectCode;   // denormalized for display
    private String    subjectName;
    private String    itemType;      // ACTIVITY | QUIZ | EXAM | ASSIGNMENT
    private String    itemName;
    private LocalDate dateMissed;
    private LocalDate deadline;
    private String    status;        // PENDING | SUBMITTED | GRADED
    private String    notes;
    private int       createdBy;
    private String    profName;        // assigned professor name for display
    private String    attachmentType;  // LINK | FILE
    private String    attachmentName;  // file name or link label
    private String    attachmentUrl;   // URL or Base64 file content

    public MissedItem() {}

    // ── Getters & Setters ──────────────────────────────────────────────────
    public int       getId()                  { return id; }
    public void      setId(int id)            { this.id = id; }

    public int       getStudentId()           { return studentId; }
    public void      setStudentId(int id)     { this.studentId = id; }

    public String    getStudentName()              { return studentName; }
    public void      setStudentName(String name)   { this.studentName = name; }

    public int       getSubjectId()           { return subjectId; }
    public void      setSubjectId(int id)     { this.subjectId = id; }

    public String    getSubjectCode()              { return subjectCode; }
    public void      setSubjectCode(String c)      { this.subjectCode = c; }

    public String    getSubjectName()              { return subjectName; }
    public void      setSubjectName(String n)      { this.subjectName = n; }

    public String    getItemType()            { return itemType; }
    public void      setItemType(String t)    { this.itemType = t; }

    public String    getItemName()            { return itemName; }
    public void      setItemName(String n)    { this.itemName = n; }

    public LocalDate getDateMissed()          { return dateMissed; }
    public void      setDateMissed(LocalDate d){ this.dateMissed = d; }

    public LocalDate getDeadline()            { return deadline; }
    public void      setDeadline(LocalDate d) { this.deadline = d; }

    public String    getStatus()              { return status; }
    public void      setStatus(String s)      { this.status = s; }

    public String    getNotes()               { return notes; }
    public void      setNotes(String n)       { this.notes = n; }

    public int       getCreatedBy()           { return createdBy; }
    public void      setCreatedBy(int id)     { this.createdBy = id; }

    public String    getProfName()                 { return profName; }
    public void      setProfName(String name)      { this.profName = name; }

    public String    getAttachmentType()           { return attachmentType; }
    public void      setAttachmentType(String t)   { this.attachmentType = t; }

    public String    getAttachmentName()           { return attachmentName; }
    public void      setAttachmentName(String n)   { this.attachmentName = n; }

    public String    getAttachmentUrl()            { return attachmentUrl; }
    public void      setAttachmentUrl(String u)    { this.attachmentUrl = u; }

    /** Returns true if the deadline has passed and item is still PENDING */
    public boolean isOverdue() {
        return "PENDING".equals(status)
                && deadline != null
                && deadline.isBefore(LocalDate.now());
    }
}