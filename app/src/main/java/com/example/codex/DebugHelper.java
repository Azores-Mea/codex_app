package com.example.codex;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Helper class to debug Google Sign-In and Firebase save issues
 * Add this temporarily to diagnose problems
 */
public class DebugHelper {
    private static final String TAG = "DebugHelper";

    public static void checkSetup(Activity activity) {
        Log.d(TAG, "=== DEBUGGING SETUP ===");

        // Check Firebase Auth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "✓ Firebase Auth User: " + currentUser.getEmail());
            Log.d(TAG, "✓ User ID: " + currentUser.getUid());
        } else {
            Log.d(TAG, "✗ No Firebase Auth user signed in");
        }

        // Check Firebase Database
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            Log.d(TAG, "✓ Firebase Database reference created");
            Log.d(TAG, "✓ Database URL: " + FirebaseDatabase.getInstance().getReference().toString());
        } catch (Exception e) {
            Log.e(TAG, "✗ Firebase Database error: " + e.getMessage());
        }

        // Check Web Client ID
        try {
            String webClientId = activity.getString(R.string.default_web_client_id);
            if (webClientId != null && !webClientId.equals("YOUR_WEB_CLIENT_ID_HERE")) {
                Log.d(TAG, "✓ Web Client ID configured: " + webClientId.substring(0, 20) + "...");
            } else {
                Log.e(TAG, "✗ Web Client ID NOT configured properly in strings.xml");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Web Client ID missing: " + e.getMessage());
        }

        Log.d(TAG, "=== END DEBUGGING ===");
    }

    public static void testDatabaseWrite(Activity activity) {
        Log.d(TAG, "=== TESTING DATABASE WRITE ===");

        DatabaseReference testRef = FirebaseDatabase.getInstance().getReference("_test");

        testRef.child("test_write").setValue("Test at " + System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Database write test SUCCESSFUL");
                    Toast.makeText(activity, "Database write test PASSED", Toast.LENGTH_SHORT).show();

                    // Clean up test data
                    testRef.child("test_write").removeValue();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Database write test FAILED: " + e.getMessage());
                    Toast.makeText(activity, "Database write test FAILED: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    public static void logUserData(String email, String firstName, String lastName,
                                   String educationalBackground, String fieldOfStudy) {
        Log.d(TAG, "=== USER DATA TO SAVE ===");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "First Name: " + firstName);
        Log.d(TAG, "Last Name: " + lastName);
        Log.d(TAG, "Educational Background: " + educationalBackground);
        Log.d(TAG, "Field of Study: " + fieldOfStudy);
        Log.d(TAG, "=== END USER DATA ===");
    }
}