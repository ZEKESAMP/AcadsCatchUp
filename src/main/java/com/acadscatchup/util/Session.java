package com.acadscatchup.util;

import com.acadscatchup.model.User;

/**
 * Holds the currently authenticated user for the session.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class Session {

    public static final String DEVELOPER = "F4TAL";

    private static User currentUser;

    private Session() {}

    public static User getCurrentUser()           { return currentUser; }
    public static void setCurrentUser(User user)  { currentUser = user; }
    public static void clear()                    { currentUser = null; }

    public static boolean isProfessor() {
        return currentUser != null && currentUser.isProfessor();
    }

    public static boolean isStudent() {
        return currentUser != null && currentUser.isStudent();
    }
}
