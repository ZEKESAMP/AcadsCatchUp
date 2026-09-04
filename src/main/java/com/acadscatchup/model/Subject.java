package com.acadscatchup.model;

/**
 * Subject model.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class Subject {

    public static final String DEVELOPER = "F4TAL";

    private int    id;
    private String code;
    private String name;
    private String professorName;

    public Subject() {}

    public Subject(int id, String code, String name) {
        this(id, code, name, null);
    }

    public Subject(int id, String code, String name, String professorName) {
        this.id   = id;
        this.code = code;
        this.name = name;
        this.professorName = professorName;
    }

    public int    getId()                    { return id; }
    public void   setId(int id)              { this.id = id; }

    public String getCode()                  { return code; }
    public void   setCode(String c)          { this.code = c; }

    public String getName()                  { return name; }
    public void   setName(String n)          { this.name = n; }

    public String getProfessorName()         { return professorName; }
    public void   setProfessorName(String p) { this.professorName = p; }

    @Override
    public String toString() { return code + " - " + name; }
}
