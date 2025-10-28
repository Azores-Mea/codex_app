package com.example.codex;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.*;

import java.util.Objects;

public class LoginHandler {

    private final Activity activity;
    private final DatabaseReference databaseReference;

    private final LinearLayout loginLayout;
    private final EditText loginEmail, loginPassword;
    private final ImageView closeLogin;
    private final MaterialButton loginBtn;
    private final Button loginCancel;
    private final TextView checkEmail, checkPass;
    private final TextInputLayout emailLayout, passLayout;

    private boolean isVisible = false;
    private final Handler handler = new Handler();
    private Runnable emailValidationRunnable, passwordCheckRunnable;

    private final int COLOR_DEFAULT = Color.parseColor("#594AE2");
    private final int COLOR_VALID = Color.parseColor("#06651A");
    private final int COLOR_INVALID = Color.parseColor("#F44336");

    private boolean isEmailValid = false;
    private boolean isPasswordMatched = false;

    public LoginHandler(Activity activity) {
        this.activity = activity;
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        loginLayout = activity.findViewById(R.id.login);
        loginEmail = activity.findViewById(R.id.login_email_in);
        loginPassword = activity.findViewById(R.id.login_pass_in);
        closeLogin = activity.findViewById(R.id.close_login);
        loginBtn = activity.findViewById(R.id.login_confirm);
        loginCancel = activity.findViewById(R.id.login_cancel);
        checkEmail = activity.findViewById(R.id.checkEmail_log);
        checkPass = activity.findViewById(R.id.checkPass_log);
        emailLayout = activity.findViewById(R.id.login_email);
        passLayout = activity.findViewById(R.id.login_pass);

        // Hide password initially
        loginPassword.setTransformationMethod(new android.text.method.PasswordTransformationMethod());
        passLayout.setEndIconActivated(false);

        loginLayout.setVisibility(LinearLayout.GONE);

        // Disable button initially
        loginBtn.setEnabled(false);
        loginBtn.setAlpha(0.5f);
        loginBtn.setBackgroundTintList(activity.getColorStateList(android.R.color.darker_gray));
        loginBtn.setTextColor(Color.WHITE);

        setupListeners();
        TextView title = activity.findViewById(R.id.login_title);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");


    }

    private void setupListeners() {
        // Auto-lowercase email
        loginEmail.addTextChangedListener(new SimpleTextWatcher(() -> {
            String currentText = loginEmail.getText().toString();
            String lowerText = currentText.toLowerCase();

            if (!currentText.equals(lowerText)) {
                loginEmail.setText(lowerText);
                loginEmail.setSelection(lowerText.length());
            }

            validateEmail();
        }));
        // Debounced email validation
        loginEmail.addTextChangedListener(new SimpleTextWatcher(() -> {
            if (emailValidationRunnable != null) handler.removeCallbacks(emailValidationRunnable);
            emailValidationRunnable = this::validateEmail;
            handler.postDelayed(emailValidationRunnable, 100);
        }));

        // Debounced password validation (check match in Firebase)
        loginPassword.addTextChangedListener(new SimpleTextWatcher(() -> {
            if (passwordCheckRunnable != null) handler.removeCallbacks(passwordCheckRunnable);
            passwordCheckRunnable = this::validatePasswordAgainstEmail;
            handler.postDelayed(passwordCheckRunnable, 100);
        }));

        closeLogin.setOnClickListener(v -> hideLoginForm());
        loginCancel.setOnClickListener(v -> hideLoginForm());
        loginBtn.setOnClickListener(v -> validateLogin());
    }

    @SuppressLint("SetTextI18n")
    private void validateEmail() {
        String email = loginEmail.getText().toString().trim();

        if (email.isEmpty()) {
            checkEmail.setText("");
            emailLayout.setBoxStrokeColor(COLOR_DEFAULT);
            emailLayout.setHintTextColor(ColorStateList.valueOf(COLOR_DEFAULT));
            isEmailValid = false;
            updateLoginButtonState();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            checkEmail.setText("Invalid email. Please try again.");
            checkEmail.setTextColor(COLOR_INVALID);
            emailLayout.setHintTextColor(ColorStateList.valueOf(COLOR_INVALID));
            emailLayout.setBoxStrokeColor(COLOR_INVALID);
            isEmailValid = false;
            updateLoginButtonState();
            return;
        }

        databaseReference.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            checkEmail.setText("");
                            emailLayout.setBoxStrokeColor(COLOR_VALID);
                            emailLayout.setHintTextColor(ColorStateList.valueOf(COLOR_VALID));
                            isEmailValid = true;
                        } else {
                            checkEmail.setText("Email not found. Please try again.");
                            checkEmail.setTextColor(COLOR_INVALID);
                            emailLayout.setHintTextColor(ColorStateList.valueOf(COLOR_INVALID));
                            emailLayout.setBoxStrokeColor(COLOR_INVALID);
                            isEmailValid = false;
                        }
                        updateLoginButtonState();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        checkEmail.setText("Database error.");
                        emailLayout.setHintTextColor(ColorStateList.valueOf(COLOR_INVALID));
                        checkEmail.setTextColor(COLOR_INVALID);
                        isEmailValid = false;
                        updateLoginButtonState();
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void validatePasswordAgainstEmail() {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            checkPass.setText("");
            passLayout.setHintTextColor(ColorStateList.valueOf(COLOR_DEFAULT));
            passLayout.setBoxStrokeColor(COLOR_DEFAULT);
            isPasswordMatched = false;
            updateLoginButtonState();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            isPasswordMatched = false;
            updateLoginButtonState();
            return;
        }

        databaseReference.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            User user = userSnap.getValue(User.class);
                            if (user != null) {
                                if (user.password.equals(password)) {
                                    checkPass.setText("");
                                    passLayout.setHintTextColor(ColorStateList.valueOf(COLOR_VALID));
                                    passLayout.setBoxStrokeColor(COLOR_VALID);
                                    isPasswordMatched = true;
                                } else {
                                    checkPass.setText("Incorrect password. Please try again.");
                                    checkPass.setTextColor(COLOR_INVALID);
                                    passLayout.setHintTextColor(ColorStateList.valueOf(COLOR_INVALID));
                                    passLayout.setBoxStrokeColor(COLOR_INVALID);
                                    isPasswordMatched = false;
                                }
                                updateLoginButtonState();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isPasswordMatched = false;
                        updateLoginButtonState();
                    }
                });
    }

    private void updateLoginButtonState() {
        boolean enable = isEmailValid && isPasswordMatched;
        loginBtn.setEnabled(enable);
        loginBtn.setAlpha(enable ? 1f : 0.5f);

        if (enable) {
            loginBtn.setBackgroundTintList(activity.getColorStateList(android.R.color.black));
            loginBtn.setTextColor(Color.WHITE);
        } else {
            loginBtn.setBackgroundTintList(activity.getColorStateList(android.R.color.darker_gray));
            loginBtn.setTextColor(Color.WHITE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void validateLogin() {
        if (!isEmailValid || !isPasswordMatched) return;

        String email = loginEmail.getText().toString().trim();

        // Fetch user info before success dialog
        databaseReference.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                User user = userSnap.getValue(User.class);
                                if (user != null) {
                                    String userType = user.usertype != null ? user.usertype : "Learner";
                                    String firstName = user.firstName != null ? user.firstName : "User";
                                    String lastName = user.lastName != null ? user.lastName : "";
                                    String classification = user.classification != null ? user.classification : "Beginner";
                                    int userId = Math.toIntExact(user.userId);

                                    // ✅ Save session info
                                    SessionManager sessionManager = new SessionManager(activity);
                                    sessionManager.saveUserSession(email, firstName, lastName, classification, userType, userId);

                                    showLoginSuccessDialog(userType, firstName);
                                }
                            }
                        } else {
                            Toast.makeText(activity, "User not found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(activity, "Database error.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("InflateParams")
    private void showLoginSuccessDialog(String userType, String firstName) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.login_confirm_msg, null);

        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setDimAmount(0.6f);

        TextView title = dialogView.findViewById(R.id.dialog_title);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();

            View spaceViewMain = activity.findViewById(R.id.space);
            if (spaceViewMain != null) spaceViewMain.setVisibility(View.GONE);

            if ("Admin".equalsIgnoreCase(userType)) {
                Toast.makeText(activity, "Redirecting to Admin Dashboard...", Toast.LENGTH_SHORT).show();
                // TODO: Launch AdminActivity if needed
            } else if ("Learner".equalsIgnoreCase(userType)) {
                Intent intent = new Intent(activity, Navigation_ActivityLearner.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                activity.finish();
            }
        });
    }

    public void showLoginForm() {
        if (isVisible) return;
        isVisible = true;

        loginLayout.setClickable(true);
        loginLayout.setFocusable(true);
        loginLayout.setFocusableInTouchMode(true);
        loginLayout.setVisibility(LinearLayout.VISIBLE);
        loginLayout.bringToFront();
        loginLayout.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.formslideup));

        resetForm();
    }

    public void hideLoginForm() {
        if (!isVisible) return;
        isVisible = false;

        loginLayout.setClickable(false);
        loginLayout.setFocusableInTouchMode(false);

        Animation slideDown = AnimationUtils.loadAnimation(activity, R.anim.formslidedown);
        loginLayout.startAnimation(slideDown);

        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                loginLayout.setVisibility(LinearLayout.GONE);
                resetForm();
            }
        });
    }

    private void resetForm() {
        loginEmail.setText("");
        loginPassword.setText("");
        checkEmail.setText("");
        checkPass.setText("");
        emailLayout.setBoxStrokeColor(COLOR_DEFAULT);
        passLayout.setBoxStrokeColor(COLOR_DEFAULT);
        loginBtn.setEnabled(false);
        loginBtn.setAlpha(0.5f);
        loginBtn.setBackgroundTintList(activity.getColorStateList(android.R.color.darker_gray));
        loginBtn.setTextColor(Color.WHITE);
        isEmailValid = false;
        isPasswordMatched = false;

        // Reset password visibility
        loginPassword.setTransformationMethod(new android.text.method.PasswordTransformationMethod());
        passLayout.setEndIconActivated(false);
    }
}
