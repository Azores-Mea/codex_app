package com.example.codex;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.animation.AnimationUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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
    private boolean toastShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        TextView policyText = findViewById(R.id.policyText);
        setupPolicyLinks(policyText);
        TextView title = findViewById(R.id.wc);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        // Initialize handlers
        registrationHandler = new RegistrationHandler(this, databaseReference);
        loginHandler = new LoginHandler(this);
        googleSignInHandler = new GoogleSignInHandler(this);
        completeProfileHandler = new CompleteProfileHandler(this);

        // Connect the handlers so CompleteProfileHandler can control RegistrationHandler
        completeProfileHandler.setRegistrationHandler(registrationHandler);

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
            completeProfileHandler.hideCompleteInfoForm();
            loginHandler.hideLoginForm();
            registrationHandler.showRegistrationForm();
        });

        // Open login form
        signInBtn.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            completeProfileHandler.hideCompleteInfoForm();
            registrationHandler.hideRegistrationForm();
            loginHandler.showLoginForm();
        });

        // Google Sign-In for Registration
        gmailAccountReg.setOnClickListener(v -> {
            Log.d(TAG, "Registration Google button clicked");
            hideKeyboardAndClearFocus();
            toastShown = false; // Reset toast flag
            startGoogleSignIn(true); // TRUE = REGISTRATION
        });

        // Google Sign-In for Login
        gmailAccountLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login Google button clicked");
            hideKeyboardAndClearFocus();
            toastShown = false; // Reset toast flag
            startGoogleSignIn(false); // FALSE = LOGIN
        });
    }

    private void setupPolicyLinks(TextView textView) {
        String text = "By continuing you agree to our Terms and Conditions and Privacy Policy";
        SpannableString spannableString = new SpannableString(text);

        // Find the position of "Terms and Conditions"
        int termsStart = text.indexOf("Terms and Conditions");
        int termsEnd = termsStart + "Terms and Conditions".length();

        // Find the position of "Privacy Policy"
        int privacyStart = text.indexOf("Privacy Policy");
        int privacyEnd = privacyStart + "Privacy Policy".length();

        // Create clickable span for Terms and Conditions
        ClickableSpan termsClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showTermsAndConditionsDialog();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(0xFF0A4B90); // Your app's blue color
            }
        };

        // Create clickable span for Privacy Policy
        ClickableSpan privacyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPrivacyPolicyDialog();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(0xFF0A4B90); // Your app's blue color
            }
        };

        spannableString.setSpan(termsClickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClickableSpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT);
    }

    @SuppressLint("InflateParams")
    private void showTermsAndConditionsDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_terms_conditions, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        // Prevent dismissing by clicking outside
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        // Apply slide up animation
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.formslideup);
        dialogView.startAnimation(slideUp);

        dialog.show();
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setDimAmount(0.7f);

        // Apply gradient to title
        TextView title = dialogView.findViewById(R.id.dialog_title);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        // Close button
        ImageView closeButton = dialogView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dialog.dismiss());
    }

    @SuppressLint("InflateParams")
    private void showPrivacyPolicyDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_privacy_policy, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        // Prevent dismissing by clicking outside
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        // Apply slide up animation
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.formslideup);
        dialogView.startAnimation(slideUp);

        dialog.show();
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setDimAmount(0.7f);

        // Apply gradient to title
        TextView title = dialogView.findViewById(R.id.dialog_title);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        // Close button
        ImageView closeButton = dialogView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dialog.dismiss());
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
                    Log.d(TAG, "New user detected - showing complete profile form");
                    registrationHandler.hideRegistrationForm();
                    loginHandler.hideLoginForm();
                    completeProfileHandler.showCompleteInfoForm(email, firstName, lastName);
                } else {
                    // Existing user - proceed to login
                    Log.d(TAG, "Existing user detected - proceeding to login");
                    proceedToLogin(email, firstName, lastName);
                }
            }

            @Override
            public void onSignInFailure(String error) {
                Log.e(TAG, "Google Sign-In failed: " + error);
                // Toast is already shown in GoogleSignInHandler
            }
        });

        completeProfileHandler.setOnProfileCompleteListener(
                (email, firstName, lastName, educationalBackground, fieldOfStudy) -> {
                    Log.d(TAG, "Profile complete callback triggered");
                    Log.d(TAG, "Email: " + email);
                    Log.d(TAG, "Educational Background: " + educationalBackground);
                    Log.d(TAG, "Field of Study: " + fieldOfStudy);

                    // Save the new Google user to database
                    googleSignInHandler.saveNewGoogleUser(
                            email, firstName, lastName, educationalBackground, fieldOfStudy,
                            new GoogleSignInHandler.OnUserSavedListener() {
                                @Override
                                public void onSuccess(int userId) {
                                    Log.d(TAG, "User saved successfully with ID: " + userId);

                                    // Save session and show success dialog
                                    SessionManager sessionManager = new SessionManager(MainActivity.this);
                                    sessionManager.saveUserSession(email, firstName, lastName,
                                            "notClassified", "Learner", userId);
                                    sessionManager.setLoggedOutFlag(false);

                                    showRegistrationSuccessDialog();
                                }

                                @Override
                                public void onFailure(String error) {
                                    Log.e(TAG, "Failed to save user: " + error);
                                    if (!toastShown) {
                                        toastShown = true;
                                        Toast.makeText(MainActivity.this,
                                                "Failed to save profile: " + error, Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                    );
                }
        );
    }

    @SuppressLint("InflateParams")
    private void showRegistrationSuccessDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setDimAmount(0.6f);

        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Account Created!");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView message = dialogView.findViewById(R.id.dialog_message);
        String htmlText = "Your registration was <b><font color='#09417D'>successful!</font></b> You can now log in and explore <b><font color='#09417D'>CodeX: Java</font></b>";
        message.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));

        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        btnNo.setText("Cancel");
        btnYes.setText("Log In");

        btnNo.setOnClickListener(v -> {
            dialog.dismiss();
            completeProfileHandler.hideCompleteInfoForm();
        });

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            proceedToApp();
        });
    }

    private void startGoogleSignIn(boolean isRegistration) {
        Log.d(TAG, "startGoogleSignIn called with isRegistration: " + isRegistration);
        Intent signInIntent = googleSignInHandler.getSignInIntent(isRegistration);
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

                                    showLoginSuccessDialog(userType, firstName);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!toastShown) {
                            toastShown = true;
                            Toast.makeText(MainActivity.this,
                                    "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @SuppressLint("InflateParams")
    private void showLoginSuccessDialog(String userType, String firstName) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.login_confirm_msg, null);

        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setDimAmount(0.6f);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        TextView title = dialogView.findViewById(R.id.dialog_title);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            proceedToApp();
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