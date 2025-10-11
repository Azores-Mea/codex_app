package com.example.codex;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.text.Html;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RegistrationHandler {

    private final Activity activity;
    private final DatabaseReference usersRef;

    private final LinearLayout registrationLayout;
    private final ImageView closeRegistration;
    private final MaterialButton confirmBtn;
    private final List<EditText> fields = new ArrayList<>();
    private final EditText regName, regLName, regEmail;
    private final TextInputEditText regPassword;
    private final TextView checkEmail, checkPass;
    private final TextInputLayout emailLayout, passLayout;

    private boolean isVisible = false;
    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;

    private final int COLOR_DEFAULT = Color.parseColor("#594AE2");
    private final int COLOR_VALID = Color.parseColor("#00C853");
    private final int COLOR_INVALID = Color.parseColor("#D50000");

    // ✅ Callback listener
    public interface OnRegistrationCompleteListener {
        void onLoginRequested();
    }

    private OnRegistrationCompleteListener listener;

    public void setOnRegistrationCompleteListener(OnRegistrationCompleteListener listener) {
        this.listener = listener;
    }

    public RegistrationHandler(Activity activity, DatabaseReference databaseReference) {
        this.activity = activity;
        this.usersRef = databaseReference;

        registrationLayout = activity.findViewById(R.id.registration);
        closeRegistration = activity.findViewById(R.id.close_registration);
        confirmBtn = activity.findViewById(R.id.confirmBtn);

        regName = activity.findViewById(R.id.firsNameInput);
        regLName = activity.findViewById(R.id.lastNameInput);
        regEmail = activity.findViewById(R.id.emailInput);
        regPassword = activity.findViewById(R.id.passwordInput);

        checkEmail = activity.findViewById(R.id.checkEmail_reg);
        checkPass = activity.findViewById(R.id.checkPass_reg);
        emailLayout = activity.findViewById(R.id.emailLayout);
        passLayout = activity.findViewById(R.id.passwordLayout);

        fields.add(regName);
        fields.add(regLName);
        fields.add(regEmail);
        fields.add(regPassword);

        registrationLayout.setVisibility(LinearLayout.GONE);
        confirmBtn.setVisibility(MaterialButton.GONE);
        confirmBtn.setEnabled(false);
        confirmBtn.setAlpha(0.5f);

        // ✅ Ensure password starts hidden with toggle off
        regPassword.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        regPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passLayout.setEndIconActivated(false);

        setupListeners();
    }

    private void setupListeners() {
        for (EditText field : fields) {
            field.addTextChangedListener(new SimpleTextWatcher(() -> {
                checkAnyFieldFilled();
                validateAllConditions();
            }));
        }

        // ✅ Email field listener with auto lowercase conversion
        regEmail.addTextChangedListener(new SimpleTextWatcher(() -> {
            String currentText = regEmail.getText().toString();
            String lowerText = currentText.toLowerCase();

            // Avoid recursive update
            if (!currentText.equals(lowerText)) {
                regEmail.setText(lowerText);
                regEmail.setSelection(lowerText.length()); // keep cursor at end
            }

            validateEmail();
        }));

        // ✅ Password validation
        regPassword.addTextChangedListener(new SimpleTextWatcher(this::validatePassword));

        closeRegistration.setOnClickListener(v -> hideRegistrationForm());
        confirmBtn.setOnClickListener(v -> showConfirmationDialog());
    }

    // ---------------- EMAIL VALIDATION ----------------
    private void validateEmail() {
        String email = regEmail.getText().toString().trim();

        if (email.isEmpty()) {
            resetEmailValidation();
            validateAllConditions();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            setInvalidEmail("Invalid email format (example@gmail.com)");
            isEmailValid = false;
            validateAllConditions();
            return;
        }

        usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            setInvalidEmail("This email is already in use.");
                            isEmailValid = false;
                        } else {
                            setValidEmail("Valid email address.");
                            isEmailValid = true;
                        }
                        validateAllConditions();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(activity, "Error checking email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetEmailValidation() {
        checkEmail.setText("");
        emailLayout.setBoxStrokeColor(COLOR_DEFAULT);
        isEmailValid = false;
    }

    private void setValidEmail(String msg) {
        checkEmail.setText(msg);
        checkEmail.setTextColor(COLOR_VALID);
        emailLayout.setBoxStrokeColor(COLOR_VALID);
    }

    private void setInvalidEmail(String msg) {
        checkEmail.setText(msg);
        checkEmail.setTextColor(COLOR_INVALID);
        emailLayout.setBoxStrokeColor(COLOR_INVALID);
    }

    // ---------------- PASSWORD VALIDATION ----------------
    private void validatePassword() {
        String password = regPassword.getText().toString();

        if (password.isEmpty()) {
            checkPass.setText("");
            passLayout.setBoxStrokeColor(COLOR_DEFAULT);
            isPasswordValid = false;
            validateAllConditions();
            return;
        }

        if (!ValidationUtils.isStrongPassword(password)) {
            checkPass.setText("Use 8–20 chars w/ upper, lower, number, and symbol.");
            checkPass.setTextColor(COLOR_INVALID);
            passLayout.setBoxStrokeColor(COLOR_INVALID);
            isPasswordValid = false;
        } else {
            checkPass.setText("Strong Password");
            checkPass.setTextColor(COLOR_VALID);
            passLayout.setBoxStrokeColor(COLOR_VALID);
            isPasswordValid = true;
        }

        validateAllConditions();
    }

    // ---------------- BUTTON VISIBILITY ----------------
    private void checkAnyFieldFilled() {
        boolean anyFilled = fields.stream().anyMatch(f -> !f.getText().toString().trim().isEmpty());
        if (anyFilled && confirmBtn.getVisibility() == MaterialButton.GONE) {
            confirmBtn.setVisibility(MaterialButton.VISIBLE);
        }
    }

    private void validateAllConditions() {
        boolean allFilled = fields.stream().allMatch(f -> !f.getText().toString().trim().isEmpty());
        boolean canEnable = allFilled && isEmailValid && isPasswordValid;

        confirmBtn.setEnabled(canEnable);
        confirmBtn.setAlpha(canEnable ? 1f : 0.5f);
        confirmBtn.setBackgroundTintList(activity.getColorStateList(
                canEnable ? android.R.color.black : android.R.color.darker_gray));
        confirmBtn.setTextColor(canEnable ? Color.WHITE : Color.LTGRAY);
    }

    // ---------------- CONFIRMATION POPUP ----------------
    private void showConfirmationDialog() {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

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

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        String htmlText = "<b><font color='#09417D'>Double-check</font></b> your details before signing up. Do you want to <b><font color='#09417D'>continue</font></b> with registration?";
        dialogMessage.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));

        TextView title = dialogView.findViewById(R.id.dialog_title);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            saveToFirebase();
            showFinalConfirmationDialog();
        });
    }

    // ---------------- FINAL CONFIRMATION POPUP ----------------
    @SuppressLint("InflateParams")
    private void showFinalConfirmationDialog() {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

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
        title.setText("Account Created!");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView message = dialogView.findViewById(R.id.dialog_message);
        String htmlText = "Your registration was <b><font color='#09417D'>successful!</font></b> You can now log in and start exploring <b><font color='#09417D'>CodeX: Java</font></b>";
        message.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));

        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        btnNo.setText("Cancel");
        btnYes.setText("Log In");

        btnNo.setOnClickListener(v -> {
            dialog.dismiss();
            hideRegistrationForm();
        });
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onLoginRequested();
            }
        });
    }

    // ---------------- SAVE USER ----------------

    private void saveToFirebase() {
        String firstName = regName.getText().toString().trim();
        String lastName = regLName.getText().toString().trim();
        String email = regEmail.getText().toString().trim();
        String password = regPassword.getText().toString().trim();
        String usertype = "Learner";
        String classification = "";

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) return;
        if (!isEmailValid || !isPasswordValid) return;

        // ✅ Query last user by key
        usersRef.orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long newUserId = 1; // default for first user

                        // ✅ Get the last user key and increment
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                long lastId = Long.parseLong(Objects.requireNonNull(child.getKey()));
                                newUserId = lastId + 1;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }

                        // ✅ Create user object
                        User user = new User(firstName, lastName, email, password, usertype, classification);
                        user.userId = newUserId;

                        // ✅ Save with incremented ID
                        usersRef.child(String.valueOf(newUserId)).setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    clearFields();
                                    Toast.makeText(activity, "User registered successfully!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(activity, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(activity, "Error checking last user ID.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // ---------------- ANIMATIONS ----------------
    public void showRegistrationForm() {
        if (isVisible) return;
        isVisible = true;

        registrationLayout.setClickable(true);
        registrationLayout.setVisibility(LinearLayout.VISIBLE);
        registrationLayout.bringToFront();
        registrationLayout.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.formslideup));

        confirmBtn.setVisibility(MaterialButton.GONE);
        clearFields();
    }

    public void hideRegistrationForm() {
        if (!isVisible) return;

        Animation slideDown = AnimationUtils.loadAnimation(activity, R.anim.formslidedown);
        registrationLayout.startAnimation(slideDown);

        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                registrationLayout.setVisibility(LinearLayout.GONE);
                clearFields();
                registrationLayout.setClickable(false);
                isVisible = false;
            }
        });
    }

    private void clearFields() {
        for (EditText f : fields) {
            f.setText("");
            f.clearFocus();
        }
        checkEmail.setText("");
        checkPass.setText("");
        emailLayout.setBoxStrokeColor(COLOR_DEFAULT);
        passLayout.setBoxStrokeColor(COLOR_DEFAULT);
        isEmailValid = false;
        isPasswordValid = false;
        confirmBtn.setEnabled(false);
        confirmBtn.setAlpha(0.5f);

        // ✅ Reset password to hidden when reopening
        regPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passLayout.setEndIconActivated(false);
    }
}
