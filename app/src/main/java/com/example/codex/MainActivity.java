package com.example.codex;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    ConstraintLayout formContainer;
    ImageView imageView;
    MaterialButton createAccountBtn, signInBtn;
    MaterialButton gmailAccountReg, gmailAccountLogin;
    DatabaseReference databaseReference;

    private RegistrationHandler registrationHandler;
    private LoginHandler loginHandler;
    private GoogleSignInHandler googleSignInHandler;
    private CompleteProfileHandler completeProfileHandler;

    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DebugHelper.checkSetup(this);

        // Check if user is already logged in
        SessionManager sessionManager = new SessionManager(this);
        boolean isLoggedIn = sessionManager.isLoggedIn();
        boolean loggedOutFlag = sessionManager.getLoggedOutFlag();

        if (isLoggedIn && !loggedOutFlag) {
            startActivity(new Intent(MainActivity.this, Navigation_ActivityLearner.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.codexstart);
        formContainer = findViewById(R.id.wcform);
        createAccountBtn = findViewById(R.id.createAccountBtn);
        signInBtn = findViewById(R.id.signInBtn);
        gmailAccountReg = findViewById(R.id.gmailAccount);
        gmailAccountLogin = findViewById(R.id.gmailAccountLogin);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        TextView title = findViewById(R.id.wc);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        // Initialize handlers
        registrationHandler = new RegistrationHandler(this, databaseReference);
        loginHandler = new LoginHandler(this);
        googleSignInHandler = new GoogleSignInHandler(this);
        completeProfileHandler = new CompleteProfileHandler(this);

        // Setup Google Sign-In launcher
        setupGoogleSignInLauncher();

        // Handle registration complete -> show login
        registrationHandler.setOnRegistrationCompleteListener(() -> {
            registrationHandler.hideRegistrationForm();
            loginHandler.showLoginForm();
        });

        // Logo and form animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein);
        imageView.startAnimation(fadeIn);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                Animation moveUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.logomoveup);
                imageView.startAnimation(moveUp);

                formContainer.setVisibility(View.VISIBLE);
                Animation slideUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.formslideup);
                formContainer.startAnimation(slideUp);
            }
        });

        // Open registration form
        createAccountBtn.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            loginHandler.hideLoginForm();
            registrationHandler.showRegistrationForm();
        });

        // Open login form
        signInBtn.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            registrationHandler.hideRegistrationForm();
            loginHandler.showLoginForm();
        });

        // Google Sign-In for Registration
        gmailAccountReg.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            startGoogleSignIn();
        });

        // Google Sign-In for Login
        gmailAccountLogin.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            startGoogleSignIn();
        });
    }

    private void setupGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        googleSignInHandler.handleSignInResult(result.getData());
                    }
                }
        );

        googleSignInHandler.setOnGoogleSignInListener(new GoogleSignInHandler.OnGoogleSignInListener() {
            @Override
            public void onSignInSuccess(boolean isNewUser, String email, String firstName, String lastName) {
                if (isNewUser) {
                    // New user - show complete profile form
                    registrationHandler.hideRegistrationForm();
                    loginHandler.hideLoginForm();
                    completeProfileHandler.showCompleteInfoForm(email, firstName, lastName);
                } else {
                    // Existing user - proceed to login
                    proceedToLogin(email, firstName, lastName);
                }
            }

            @Override
            public void onSignInFailure(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        completeProfileHandler.setOnProfileCompleteListener(
                (email, firstName, lastName, educationalBackground, fieldOfStudy) -> {
                    android.util.Log.d("MainActivity", "Profile complete callback triggered");
                    android.util.Log.d("MainActivity", "Email: " + email);
                    android.util.Log.d("MainActivity", "Educational Background: " + educationalBackground);
                    android.util.Log.d("MainActivity", "Field of Study: " + fieldOfStudy);

                    // Save the new Google user to database
                    googleSignInHandler.saveNewGoogleUser(
                            email, firstName, lastName, educationalBackground, fieldOfStudy,
                            new GoogleSignInHandler.OnUserSavedListener() {
                                @Override
                                public void onSuccess(int userId) {
                                    android.util.Log.d("MainActivity", "User saved successfully with ID: " + userId);
                                    Toast.makeText(MainActivity.this,
                                            "Account created successfully!", Toast.LENGTH_SHORT).show();

                                    // Save session and proceed to app
                                    SessionManager sessionManager = new SessionManager(MainActivity.this);
                                    sessionManager.saveUserSession(email, firstName, lastName,
                                            "notClassified", "Learner", userId);
                                    sessionManager.setLoggedOutFlag(false);

                                    proceedToApp();
                                }

                                @Override
                                public void onFailure(String error) {
                                    android.util.Log.e("MainActivity", "Failed to save user: " + error);
                                    Toast.makeText(MainActivity.this,
                                            "Failed to save profile: " + error, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                }
        );
    }

    private void startGoogleSignIn() {
        Intent signInIntent = googleSignInHandler.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void proceedToLogin(String email, String firstName, String lastName) {
        // Fetch user data from database
        databaseReference.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                User user = userSnap.getValue(User.class);
                                if (user != null) {
                                    String classification = user.classification != null ?
                                            user.classification : "notClassified";
                                    String userType = user.usertype != null ? user.usertype : "Learner";
                                    int userId = (int) user.userId;

                                    // Save session
                                    SessionManager sessionManager = new SessionManager(MainActivity.this);
                                    sessionManager.saveUserSession(email, firstName, lastName,
                                            classification, userType, userId);
                                    sessionManager.setLoggedOutFlag(false);

                                    proceedToApp();
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this,
                                "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void proceedToApp() {
        Intent intent = new Intent(MainActivity.this, Navigation_ActivityLearner.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                int[] scrcoords = new int[2];
                v.getLocationOnScreen(scrcoords);
                float x = event.getRawX() + v.getLeft() - scrcoords[0];
                float y = event.getRawY() + v.getTop() - scrcoords[1];

                if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom()) {
                    hideKeyboardAndClearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    public void hideKeyboardAndClearFocus() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }
}