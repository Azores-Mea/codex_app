package com.example.codex;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class GoogleSignInHandler {
    private static final String TAG = "GoogleSignInHandler";
    public static final int RC_SIGN_IN = 9001;

    private final Activity activity;
    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference usersRef;
    private GoogleSignInClient googleSignInClient;

    private OnGoogleSignInListener listener;
    private boolean isRegistrationFlow = false;

    public interface OnGoogleSignInListener {
        void onSignInSuccess(boolean isNewUser, String email, String firstName, String lastName);
        void onSignInFailure(String error);
    }

    public GoogleSignInHandler(Activity activity) {
        this.activity = activity;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.usersRef = FirebaseDatabase.getInstance().getReference("Users");

        configureGoogleSignIn();
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public void setOnGoogleSignInListener(OnGoogleSignInListener listener) {
        this.listener = listener;
    }

    /**
     * Get sign-in intent and sign out first to force account picker
     * @param isRegistration true if this is for registration, false if for login
     */
    public Intent getSignInIntent(boolean isRegistration) {
        this.isRegistrationFlow = isRegistration;

        Log.d(TAG, "getSignInIntent called with isRegistration: " + isRegistration);

        // Sign out from Google and Firebase to force account picker
        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d(TAG, "Sign out completed, showing account picker");
        });

        return googleSignInClient.getSignInIntent();
    }

    public void signOut() {
        firebaseAuth.signOut();
        googleSignInClient.signOut();
    }

    public void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                Log.d(TAG, "Google Sign-In successful for: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign in failed", e);
            if (listener != null) {
                listener.onSignInFailure("Google sign in failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase authentication successful");
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            checkUserInDatabase(firebaseUser);
                        }
                    } else {
                        Log.e(TAG, "signInWithCredential:failure", task.getException());
                        if (listener != null) {
                            listener.onSignInFailure("Authentication failed.");
                        }
                    }
                });
    }

    private void checkUserInDatabase(FirebaseUser firebaseUser) {
        String email = firebaseUser.getEmail();
        String displayName = firebaseUser.getDisplayName();

        Log.d(TAG, "===========================================");
        Log.d(TAG, "Checking user in database: " + email);
        Log.d(TAG, "Is registration flow: " + isRegistrationFlow);
        Log.d(TAG, "===========================================");

        // Split display name into first and last name
        String firstName = "";
        String lastName = "";
        if (displayName != null && !displayName.isEmpty()) {
            String[] nameParts = displayName.trim().split("\\s+");
            firstName = nameParts[0];
            if (nameParts.length > 1) {
                lastName = nameParts[nameParts.length - 1];
            }
        }

        final String finalFirstName = firstName;
        final String finalLastName = lastName;

        // Check if user exists in database
        usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean userExists = snapshot.exists();

                        Log.d(TAG, "User exists in database: " + userExists);
                        Log.d(TAG, "Registration flow: " + isRegistrationFlow);

                        if (userExists && isRegistrationFlow) {
                            // User trying to register with existing Google account
                            Log.d(TAG, "BLOCKING: User already exists - blocking registration");

                            // Sign out the user
                            signOut();

                            String errorMsg = "Google account already registered. Please login instead.";

                            Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();

                            if (listener != null) {
                                listener.onSignInFailure(errorMsg);
                            }

                        } else if (!userExists && !isRegistrationFlow) {
                            // User trying to login but doesn't exist
                            Log.d(TAG, "BLOCKING: User doesn't exist - blocking login");

                            // Sign out the user
                            signOut();

                            String errorMsg = "Account not found. Please register first.";

                            Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();

                            if (listener != null) {
                                listener.onSignInFailure(errorMsg);
                            }

                        } else if (userExists && !isRegistrationFlow) {
                            // User exists - proceed to login
                            Log.d(TAG, "SUCCESS: User exists - proceeding to login");
                            if (listener != null) {
                                listener.onSignInSuccess(false, email, finalFirstName, finalLastName);
                            }
                        } else {
                            // New user registration - proceed
                            Log.d(TAG, "SUCCESS: New user - needs to complete profile");
                            if (listener != null) {
                                listener.onSignInSuccess(true, email, finalFirstName, finalLastName);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                        if (listener != null) {
                            listener.onSignInFailure("Database error: " + error.getMessage());
                        }
                    }
                });
    }

    public void saveNewGoogleUser(String email, String firstName, String lastName,
                                  String educationalBackground, String fieldOfStudy,
                                  OnUserSavedListener callback) {

        Log.d(TAG, "Starting to save new Google user: " + email);
        Log.d(TAG, "Educational Background: " + educationalBackground);
        Log.d(TAG, "Field of Study: " + fieldOfStudy);

        usersRef.orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long newUserId = 1;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                long lastId = Long.parseLong(child.getKey());
                                newUserId = lastId + 1;
                                Log.d(TAG, "Last user ID: " + lastId + ", New user ID: " + newUserId);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing user ID", e);
                            }
                        }

                        final long finalUserId = newUserId;

                        // Create user object using HashMap for better debugging
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("userId", finalUserId);
                        userMap.put("firstName", firstName);
                        userMap.put("lastName", lastName);
                        userMap.put("email", email);
                        userMap.put("password", "google_auth");
                        userMap.put("usertype", "Learner");
                        userMap.put("classification", "notClassified");
                        userMap.put("learningMode", "none");
                        userMap.put("educationalBackground", educationalBackground);
                        userMap.put("fieldOfStudy", fieldOfStudy.isEmpty() ? "Not specified" : fieldOfStudy);

                        Log.d(TAG, "Saving user to Firebase with ID: " + finalUserId);

                        usersRef.child(String.valueOf(finalUserId)).setValue(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User saved successfully with ID: " + finalUserId);
                                    Toast.makeText(activity, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                                    if (callback != null) {
                                        callback.onSuccess((int) finalUserId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save user", e);
                                    Toast.makeText(activity, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    if (callback != null) {
                                        callback.onFailure(e.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error getting last user ID: " + error.getMessage());
                        if (callback != null) {
                            callback.onFailure(error.getMessage());
                        }
                    }
                });
    }

    public interface OnUserSavedListener {
        void onSuccess(int userId);
        void onFailure(String error);
    }
}