package com.example.codex;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Html;
import android.text.InputFilter;
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
import android.widget.ScrollView;
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

    private final ScrollView registrationScrollView;
    private final MaterialButton gmailAccount;
    private final List<EditText> fields = new ArrayList<>();
    private final EditText regName, regLName, regEmail, regFieldOfStudy, regFieldOfStudyComplete;
    private final TextInputEditText regPassword, regConfirmPassword;
    private final AutoCompleteTextView regEducationalBackground, regEducationalBackgroundComplete;
    private final TextView checkEmail, checkPass, checkConfirmPass;
    private final TextInputLayout emailLayout, passLayout, confirmPassLayout, educationalBackgroundLayout,
            firstNameLayout, lastNameLayout, fieldOfStudyLayout, educationalBackgroundLayoutComplete, fieldOfStudyLayoutComplete;
    private boolean isVisible = false;
    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;
    private boolean isConfirmPasswordValid = false;

    private final int COLOR_DEFAULT = Color.parseColor("#91C3F7");
    private final int COLOR_VALID = Color.parseColor("#06651A");
    private final int COLOR_INVALID = Color.parseColor("#F44336");

    private boolean shouldBringWCFormToFront = true;
    // Callback interface
    public interface OnRegistrationCompleteListener {
        void onLoginRequested();
    }

    private OnRegistrationCompleteListener listener;

    public void setOnRegistrationCompleteListener(OnRegistrationCompleteListener listener) {
        this.listener = listener;
    }

    public void setShouldBringWCFormToFront(boolean shouldBring) {
        this.shouldBringWCFormToFront = shouldBring;
    }

    public RegistrationHandler(Activity activity, DatabaseReference databaseReference) {
        this.activity = activity;
        this.usersRef = databaseReference;

        registrationLayout = activity.findViewById(R.id.registration);
        confirmBtn = activity.findViewById(R.id.confirmBtn);
        gmailAccount = activity.findViewById(R.id.gmailAccount);
        registrationScrollView = activity.findViewById(R.id.scrollView); // Use your actual ScrollView ID

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

        educationalBackgroundLayoutComplete = activity.findViewById(R.id.educationalBackgroundLayoutComplete);
        fieldOfStudyLayoutComplete = activity.findViewById(R.id.fieldOfStudyLayoutComplete);
        regEducationalBackgroundComplete = activity.findViewById(R.id.educationalBackgroundInputComplete);
        regFieldOfStudyComplete = activity.findViewById(R.id.fieldOfStudyInputComplete);

        // Add required fields (excluding optional Field of Study)
        fields.add(regName);
        fields.add(regLName);
        fields.add(regEmail);
        fields.add(regPassword);
        fields.add(regConfirmPassword);

        registrationLayout.setVisibility(ConstraintLayout.GONE);
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

        InputFilter nameFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);

                // Allow letters, digits, spaces, hyphens
                if (!Character.isLetterOrDigit(c) && c != ' ' && c != '-') {
                    return ""; // Reject invalid characters
                }
            }

            return null; // Accept input
        };

// Attach filter to first name and last name
        regName.setFilters(new InputFilter[]{ nameFilter });
        regLName.setFilters(new InputFilter[]{ nameFilter });


        // Attach filter to first name and last name
        regName.setFilters(new InputFilter[]{ nameFilter });
        regLName.setFilters(new InputFilter[]{ nameFilter });

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

        educationalBackgroundLayoutComplete.setBoxStrokeColorStateList(defaultColorStateList);
        educationalBackgroundLayoutComplete.setHintTextColor(defaultColorStateList);

        fieldOfStudyLayoutComplete.setBoxStrokeColorStateList(defaultColorStateList);
        fieldOfStudyLayoutComplete.setHintTextColor(defaultColorStateList);

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
            validateConfirmPassword();
        }));
        regPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // When password field loses focus, reset to default if not valid
                String password = regPassword.getText().toString();
                if (password.isEmpty() || !ValidationUtils.isStrongPassword(password)) {
                    passLayout.setBoxStrokeColorStateList(createColorStateList());
                    passLayout.setHintTextColor(createColorStateList());
                }
            } else {
                // When focused, validate immediately
                validatePassword();
            }
        });

        regConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // When confirm password field loses focus, reset to default if not valid
                String password = regPassword.getText().toString();
                String confirmPassword = regConfirmPassword.getText().toString();
                if (confirmPassword.isEmpty() || !password.equals(confirmPassword)) {
                    confirmPassLayout.setBoxStrokeColorStateList(createColorStateList());
                    confirmPassLayout.setHintTextColor(createColorStateList());
                }
            } else {
                // When focused, validate immediately
                validateConfirmPassword();
            }
        });

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
            setInvalidEmail("Please input a valid email address.");
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
                            setValidEmail("");
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
            // Reset to default colors
            passLayout.setBoxStrokeColorStateList(createColorStateList());
            passLayout.setHintTextColor(createColorStateList());
            isPasswordValid = false;
            validateAllConditions();
            return;
        }

        if (!ValidationUtils.isStrongPassword(password)) {
            checkPass.setText("Use 8-20 characters with upper and lowercase letters, numbers, and special symbols. No spaces or common passwords allowed.");
            checkPass.setTextColor(COLOR_INVALID);

            // Create ColorStateList for invalid state
            int[][] states = new int[][] {
                    new int[] { android.R.attr.state_focused },
                    new int[] { -android.R.attr.state_focused }
            };
            int[] colors = new int[] { COLOR_INVALID, COLOR_INVALID };
            ColorStateList invalidColorStateList = new ColorStateList(states, colors);

            passLayout.setBoxStrokeColorStateList(invalidColorStateList);
            passLayout.setHintTextColor(invalidColorStateList);

            isPasswordValid = false;
        } else {
            checkPass.setText("");
            checkPass.setTextColor(COLOR_VALID);

            // Create ColorStateList for valid state
            int[][] states = new int[][] {
                    new int[] { android.R.attr.state_focused },
                    new int[] { -android.R.attr.state_focused }
            };
            int[] colors = new int[] { COLOR_VALID, COLOR_VALID };
            ColorStateList validColorStateList = new ColorStateList(states, colors);

            passLayout.setBoxStrokeColorStateList(validColorStateList);
            passLayout.setHintTextColor(validColorStateList);

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
            // Reset ONLY confirm password field to default
            confirmPassLayout.setBoxStrokeColorStateList(createColorStateList());
            confirmPassLayout.setHintTextColor(createColorStateList());
            isConfirmPasswordValid = false;
            validateAllConditions();
            return;
        }

        if (!password.equals(confirmPassword)) {
            checkConfirmPass.setText("Passwords do not match.");
            checkConfirmPass.setTextColor(COLOR_INVALID);

            // Create ColorStateList for invalid state
            int[][] states = new int[][] {
                    new int[] { android.R.attr.state_focused },
                    new int[] { -android.R.attr.state_focused }
            };
            int[] colors = new int[] { COLOR_INVALID, COLOR_INVALID };
            ColorStateList invalidColorStateList = new ColorStateList(states, colors);

            confirmPassLayout.setBoxStrokeColorStateList(invalidColorStateList);
            confirmPassLayout.setHintTextColor(invalidColorStateList);

            isConfirmPasswordValid = false;
        } else {
            checkConfirmPass.setText("");
            checkConfirmPass.setTextColor(COLOR_VALID);

            // Create ColorStateList for valid state
            int[][] states = new int[][] {
                    new int[] { android.R.attr.state_focused },
                    new int[] { -android.R.attr.state_focused }
            };
            int[] colors = new int[] { COLOR_VALID, COLOR_VALID };
            ColorStateList validColorStateList = new ColorStateList(states, colors);

            confirmPassLayout.setBoxStrokeColorStateList(validColorStateList);
            confirmPassLayout.setHintTextColor(validColorStateList);

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
            shouldBringWCFormToFront = false;
            hideRegistrationForm();
            // then show login form
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
    public void hideRegistrationForm() {
        if (!isVisible) return;

        // Disable interaction immediately
        registrationLayout.setClickable(false);
        registrationLayout.setEnabled(false);
        gmailAccount.setEnabled(false);
        regEducationalBackground.setEnabled(false);
        regFieldOfStudy.setEnabled(false);
        for (EditText field : fields) {
            field.setEnabled(false);
        }

        Animation slideDown = AnimationUtils.loadAnimation(activity, R.anim.formslidedown);
        registrationLayout.startAnimation(slideDown);

        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Animation started - form already disabled above
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                registrationLayout.setVisibility(ConstraintLayout.GONE);
                isVisible = false;
                clearFields();

                // Reset scroll position
                if (registrationScrollView != null) {
                    registrationScrollView.smoothScrollTo(0, 0);
                }

                // Immediately make wcform available - no delay
                View wcform = activity.findViewById(R.id.wcform);
                if (wcform != null) {
                    wcform.setClickable(true);
                    wcform.setEnabled(true);
                    wcform.setFocusable(true);
                    if (shouldBringWCFormToFront) {
                        wcform.bringToFront();
                        wcform.requestLayout(); // Force layout update
                    }
                }
                shouldBringWCFormToFront = true; // reset for next time
            }
        });
    }

    public void showRegistrationForm() {
        if (isVisible) return;
        isVisible = true;

        // Enable fields
        for (EditText field : fields) {
            field.setEnabled(true);
        }
        gmailAccount.setEnabled(true);
        regEducationalBackground.setEnabled(true);
        regFieldOfStudy.setEnabled(true);

        // Make wcform non-interactive immediately
        View wcform = activity.findViewById(R.id.wcform);
        if (wcform != null) {
            wcform.setClickable(false);
            wcform.setEnabled(false);
        }

        // Setup registration form
        registrationLayout.setVisibility(ConstraintLayout.VISIBLE);
        registrationLayout.setClickable(true);
        registrationLayout.setEnabled(true);
        registrationLayout.bringToFront();
        registrationLayout.requestLayout(); // Force layout update

        Animation slideUp = AnimationUtils.loadAnimation(activity, R.anim.formslideup);
        registrationLayout.startAnimation(slideUp);

        clearFields();
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