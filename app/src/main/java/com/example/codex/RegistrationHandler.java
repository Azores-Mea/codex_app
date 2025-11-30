package com.example.codex;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Html;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

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

    private final ConstraintLayout registrationLayout;
    private final MaterialButton confirmBtn;
    private final List<EditText> fields = new ArrayList<>();
    private final EditText regName, regLName, regEmail, regFieldOfStudy;
    private final TextInputEditText regPassword, regConfirmPassword;
    private final AutoCompleteTextView regEducationalBackground;
    private final TextView checkEmail, checkPass, checkConfirmPass;
    private final TextInputLayout emailLayout, passLayout, confirmPassLayout, educationalBackgroundLayout,
            firstNameLayout, lastNameLayout, fieldOfStudyLayout;

    private boolean isVisible = false;
    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;
    private boolean isConfirmPasswordValid = false;

    private final int COLOR_DEFAULT = Color.parseColor("#91C3F7");
    private final int COLOR_VALID = Color.parseColor("#06651A");
    private final int COLOR_INVALID = Color.parseColor("#F44336");

    // Callback interface
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
        confirmBtn = activity.findViewById(R.id.confirmBtn);

        regName = activity.findViewById(R.id.firsNameInput);
        regLName = activity.findViewById(R.id.lastNameInput);
        regEmail = activity.findViewById(R.id.emailInput);
        regPassword = activity.findViewById(R.id.passwordInput);
        regConfirmPassword = activity.findViewById(R.id.confirmPasswordInput);
        regEducationalBackground = activity.findViewById(R.id.educationalBackgroundInput);
        regFieldOfStudy = activity.findViewById(R.id.fieldOfStudyInput);

        checkEmail = activity.findViewById(R.id.checkEmail_reg);
        checkPass = activity.findViewById(R.id.checkPass_reg);
        checkConfirmPass = activity.findViewById(R.id.checkConfirmPass_reg);

        firstNameLayout = activity.findViewById(R.id.firstNameLayout);
        lastNameLayout = activity.findViewById(R.id.lastNameLayout);
        emailLayout = activity.findViewById(R.id.emailLayout);
        passLayout = activity.findViewById(R.id.passwordLayout);
        confirmPassLayout = activity.findViewById(R.id.confirmPasswordLayout);
        educationalBackgroundLayout = activity.findViewById(R.id.educationalBackgroundLayout);
        fieldOfStudyLayout = activity.findViewById(R.id.fieldOfStudyLayout);

        // Add required fields (excluding optional Field of Study)
        fields.add(regName);
        fields.add(regLName);
        fields.add(regEmail);
        fields.add(regPassword);
        fields.add(regConfirmPassword);

        registrationLayout.setVisibility(ConstraintLayout.GONE);
        confirmBtn.setVisibility(MaterialButton.GONE);
        confirmBtn.setEnabled(false);
        confirmBtn.setAlpha(0.5f);

        // Setup Educational Background dropdown
        setupEducationalBackgroundDropdown();

        // Set default colors for all fields
        setDefaultFieldColors();

        // Hide passwords initially
        regPassword.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        regPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passLayout.setEndIconActivated(false);

        regConfirmPassword.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        regConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        confirmPassLayout.setEndIconActivated(false);

        setupListeners();
        TextView title = activity.findViewById(R.id.registration_title);
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");
    }

    private void setDefaultFieldColors() {
        // Set default color for all text input layouts
        ColorStateList defaultColorStateList = createColorStateList();

        firstNameLayout.setBoxStrokeColorStateList(defaultColorStateList);
        firstNameLayout.setHintTextColor(defaultColorStateList);

        lastNameLayout.setBoxStrokeColorStateList(defaultColorStateList);
        lastNameLayout.setHintTextColor(defaultColorStateList);

        emailLayout.setBoxStrokeColorStateList(defaultColorStateList);
        emailLayout.setHintTextColor(defaultColorStateList);

        passLayout.setBoxStrokeColorStateList(defaultColorStateList);
        passLayout.setHintTextColor(defaultColorStateList);

        confirmPassLayout.setBoxStrokeColorStateList(defaultColorStateList);
        confirmPassLayout.setHintTextColor(defaultColorStateList);

        educationalBackgroundLayout.setBoxStrokeColorStateList(defaultColorStateList);
        educationalBackgroundLayout.setHintTextColor(defaultColorStateList);

        fieldOfStudyLayout.setBoxStrokeColorStateList(defaultColorStateList);
        fieldOfStudyLayout.setHintTextColor(defaultColorStateList);
    }

    private ColorStateList createColorStateList() {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_focused },  // focused (typing)
                new int[] { -android.R.attr.state_focused }  // unfocused (default)
        };

        int[] colors = new int[] {
                Color.parseColor("#062C54"),  // focused color
                COLOR_DEFAULT                  // unfocused color (#91C3F7)
        };

        return new ColorStateList(states, colors);
    }

    private void setupEducationalBackgroundDropdown() {
        String[] educationLevels = {"Junior High School", "Senior High School", "College", "Graduate"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity,
                android.R.layout.simple_dropdown_item_1line,
                educationLevels
        ) {
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);

                // Set white background for dropdown
                view.setBackgroundColor(Color.WHITE);

                return view;
            }
        };

        regEducationalBackground.setAdapter(adapter);

        // Add listener to validate when selection changes
        regEducationalBackground.setOnItemClickListener((parent, view, position, id) -> {
            checkAnyFieldFilled();
            validateAllConditions();
        });
    }

    private void setupListeners() {
        for (EditText field : fields) {
            field.addTextChangedListener(new SimpleTextWatcher(() -> {
                checkAnyFieldFilled();
                validateAllConditions();
            }));
        }

        // Auto-lowercase email
        regEmail.addTextChangedListener(new SimpleTextWatcher(() -> {
            String currentText = regEmail.getText().toString();
            String lowerText = currentText.toLowerCase();

            if (!currentText.equals(lowerText)) {
                regEmail.setText(lowerText);
                regEmail.setSelection(lowerText.length());
            }

            validateEmail();
        }));

        // Password validation
        regPassword.addTextChangedListener(new SimpleTextWatcher(() -> {
            validatePassword();
            validateConfirmPassword(); // Re-validate confirm password when password changes
        }));

        // Confirm Password validation
        regConfirmPassword.addTextChangedListener(new SimpleTextWatcher(this::validateConfirmPassword));

        activity.findViewById(R.id.close_registration).setOnClickListener(v -> hideRegistrationForm());
        confirmBtn.setOnClickListener(v -> showConfirmationDialog());
    }

    // --- Email validation ---
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
        setDefaultFieldColors(); // This will reset all fields properly
        isEmailValid = false;
    }

    private void setValidEmail(String msg) {
        checkEmail.setText(msg);
        checkEmail.setTextColor(COLOR_VALID);
        emailLayout.setHintTextColor(ColorStateList.valueOf(COLOR_VALID));
        emailLayout.setBoxStrokeColorStateList(ColorStateList.valueOf(COLOR_VALID));
    }

    private void setInvalidEmail(String msg) {
        checkEmail.setText(msg);
        checkEmail.setTextColor(COLOR_INVALID);
        emailLayout.setHintTextColor(ColorStateList.valueOf(COLOR_INVALID));
        emailLayout.setBoxStrokeColorStateList(ColorStateList.valueOf(COLOR_INVALID));
    }

    // --- Password validation ---
    private void validatePassword() {
        String password = regPassword.getText().toString();

        if (password.isEmpty()) {
            checkPass.setText("");
            setDefaultFieldColors(); // Reset to default
            isPasswordValid = false;
            validateAllConditions();
            return;
        }

        if (!ValidationUtils.isStrongPassword(password)) {
            checkPass.setText("Use 8-20 characters with upper and lowercase letters, numbers, and special symbols. No spaces or common passwords allowed.");
            checkPass.setTextColor(COLOR_INVALID);
            passLayout.setHintTextColor(ColorStateList.valueOf(COLOR_INVALID));
            passLayout.setBoxStrokeColorStateList(ColorStateList.valueOf(COLOR_INVALID));
            isPasswordValid = false;
        } else {
            checkPass.setText("Strong password.");
            checkPass.setTextColor(COLOR_VALID);
            passLayout.setHintTextColor(ColorStateList.valueOf(COLOR_VALID));
            passLayout.setBoxStrokeColorStateList(ColorStateList.valueOf(COLOR_VALID));
            isPasswordValid = true;
        }

        validateAllConditions();
    }

    // --- Confirm Password validation ---
    private void validateConfirmPassword() {
        String password = regPassword.getText().toString();
        String confirmPassword = regConfirmPassword.getText().toString();

        if (confirmPassword.isEmpty()) {
            checkConfirmPass.setText("");
            setDefaultFieldColors(); // Reset to default
            isConfirmPasswordValid = false;
            validateAllConditions();
            return;
        }

        if (!password.equals(confirmPassword)) {
            checkConfirmPass.setText("Passwords do not match.");
            checkConfirmPass.setTextColor(COLOR_INVALID);
            confirmPassLayout.setHintTextColor(ColorStateList.valueOf(COLOR_INVALID));
            confirmPassLayout.setBoxStrokeColorStateList(ColorStateList.valueOf(COLOR_INVALID));
            isConfirmPasswordValid = false;
        } else {
            checkConfirmPass.setText("Passwords match.");
            checkConfirmPass.setTextColor(COLOR_VALID);
            confirmPassLayout.setHintTextColor(ColorStateList.valueOf(COLOR_VALID));
            confirmPassLayout.setBoxStrokeColorStateList(ColorStateList.valueOf(COLOR_VALID));
            isConfirmPasswordValid = true;
        }

        validateAllConditions();
    }

    // --- Button state ---
    private void checkAnyFieldFilled() {
        boolean anyFilled = fields.stream().anyMatch(f -> !f.getText().toString().trim().isEmpty());
        if (anyFilled && confirmBtn.getVisibility() == MaterialButton.GONE) {
            confirmBtn.setVisibility(MaterialButton.VISIBLE);
        }
    }

    private void validateAllConditions() {
        boolean allRequiredFilled = fields.stream().allMatch(f -> !f.getText().toString().trim().isEmpty());
        boolean educationSelected = !regEducationalBackground.getText().toString().trim().isEmpty();
        boolean canEnable = allRequiredFilled && educationSelected && isEmailValid && isPasswordValid && isConfirmPasswordValid;

        confirmBtn.setEnabled(canEnable);
        confirmBtn.setAlpha(canEnable ? 1f : 0.5f);
        confirmBtn.setBackgroundTintList(activity.getColorStateList(
                canEnable ? android.R.color.black : android.R.color.darker_gray));
        confirmBtn.setTextColor(canEnable ? Color.WHITE : Color.LTGRAY);
    }

    // --- Confirmation dialog ---
    private void showConfirmationDialog() {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
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

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        String htmlText = "<b><font color='#09417D'>Double-check</font></b> your details before signing up.<b><font color='#09417D'> Continue</font></b>?";
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

    // --- Success dialog ---
    @SuppressLint("InflateParams")
    private void showFinalConfirmationDialog() {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
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
            hideRegistrationForm();
        });
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onLoginRequested();
        });
    }

    // --- Save user to Firebase ---
    private void saveToFirebase() {
        String firstName = regName.getText().toString().trim();
        String lastName = regLName.getText().toString().trim();
        String email = regEmail.getText().toString().trim();
        String password = regPassword.getText().toString().trim();
        String educationalBackground = regEducationalBackground.getText().toString().trim();
        String fieldOfStudy = regFieldOfStudy.getText().toString().trim();
        String usertype = "Learner";
        String classification = "notClassified";
        String learningMode = "none";

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || educationalBackground.isEmpty()) return;
        if (!isEmailValid || !isPasswordValid || !isConfirmPasswordValid) return;

        usersRef.orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long newUserId = 1;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                long lastId = Long.parseLong(Objects.requireNonNull(child.getKey()));
                                newUserId = lastId + 1;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }

                        User user = new User(firstName, lastName, email, password, usertype, classification, fieldOfStudy, educationalBackground);
                        user.userId = newUserId;
                        user.learningMode = learningMode;
                        user.educationalBackground = educationalBackground;
                        user.fieldOfStudy = fieldOfStudy.isEmpty() ? "Not specified" : fieldOfStudy;

                        usersRef.child(String.valueOf(newUserId)).setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    clearFields();
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

    // --- Show / Hide form ---
    public void showRegistrationForm() {
        if (isVisible) return;
        isVisible = true;

        registrationLayout.setClickable(true);
        registrationLayout.setVisibility(ConstraintLayout.VISIBLE);
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
                registrationLayout.setVisibility(ConstraintLayout.GONE);
                clearFields();
                registrationLayout.setClickable(false);
                isVisible = false;
            }
        });
    }

    // --- Reset form ---
    private void clearFields() {
        for (EditText f : fields) {
            f.setText("");
            f.clearFocus();
        }
        regEducationalBackground.setText("");
        regFieldOfStudy.setText("");

        checkEmail.setText("");
        checkPass.setText("");
        checkConfirmPass.setText("");

        // Reset to default colors
        setDefaultFieldColors();

        isEmailValid = false;
        isPasswordValid = false;
        isConfirmPasswordValid = false;

        confirmBtn.setEnabled(false);
        confirmBtn.setAlpha(0.5f);

        regPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        regConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passLayout.setEndIconActivated(false);
        confirmPassLayout.setEndIconActivated(false);
    }
}