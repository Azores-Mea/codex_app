package com.example.codex;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_CLASSIFICATION = "classification";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_SELECTED_LESSON = "selectedLesson";
    private static final String KEY_LESSON_DIFFICULTY = "lessonDifficulty";
    private static final String KEY_LEARNING_MODE = "learningMode"; // ✅ NEW

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveUserSession(String email, String firstName, String lastName,
                                String classification, String userType, int userId) {
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.putString(KEY_LAST_NAME, lastName);
        editor.putString(KEY_CLASSIFICATION, classification);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putInt(KEY_USER_ID, userId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    // ✅ Overloaded method to include learning mode
    public void saveUserSession(String email, String firstName, String lastName,
                                String classification, String userType, int userId, String learningMode) {
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.putString(KEY_LAST_NAME, lastName);
        editor.putString(KEY_CLASSIFICATION, classification);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_LEARNING_MODE, learningMode);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public boolean isLoggedIn() { return prefs.getBoolean(KEY_IS_LOGGED_IN, false); }

    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getFirstName() { return prefs.getString(KEY_FIRST_NAME, ""); }
    public String getLastName() { return prefs.getString(KEY_LAST_NAME, ""); }
    public String getClassification() { return prefs.getString(KEY_CLASSIFICATION, ""); }
    public String getUserType() { return prefs.getString(KEY_USER_TYPE, ""); }
    public int getUserId() { return prefs.getInt(KEY_USER_ID, -1); }

    public void logout() {
        editor.clear();
        editor.putBoolean("loggedOutFlag", true); // <-- prevent auto-login
        editor.apply();
    }



    public void setLoggedOutFlag(boolean value) {
        editor.putBoolean("loggedOutFlag", value);
        editor.apply();
    }

    public boolean getLoggedOutFlag() {
        return prefs.getBoolean("loggedOutFlag", false);
    }

    public void setClassification(String updatedClassification) {
        editor.putString(KEY_CLASSIFICATION, updatedClassification);
        editor.apply();
    }

    // ✅ Save and retrieve selected lesson
    public void saveSelectedLesson(String lessonId) {
        editor.putString(KEY_SELECTED_LESSON, lessonId);
        editor.apply();
    }

    public String getSelectedLesson() {
        return prefs.getString(KEY_SELECTED_LESSON, null);
    }

    public void clearSelectedLesson() {
        editor.remove(KEY_SELECTED_LESSON);
        editor.apply();
    }

    // ✅ Save and retrieve lesson difficulty
    public void saveLessonDifficulty(String difficulty) {
        editor.putString(KEY_LESSON_DIFFICULTY, difficulty);
        editor.apply();
    }


    public String getLessonDifficulty() {
        return prefs.getString(KEY_LESSON_DIFFICULTY, "");
    }

    // ✅ NEW: Save and retrieve learning mode
    public void saveLearningMode(String learningMode) {
        editor.putString(KEY_LEARNING_MODE, learningMode);
        editor.apply();
    }

    public String getLearningMode() {
        return prefs.getString(KEY_LEARNING_MODE, "Guided Mode"); // Default to Guided Mode
    }
}