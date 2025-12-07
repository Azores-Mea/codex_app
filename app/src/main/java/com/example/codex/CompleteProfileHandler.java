package com.example.codex;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

public class CompleteProfileHandler {

    private static final String TAG = "CompleteProfileHandler";

    private RegistrationHandler registrationHandler;

    private final Activity activity;
    private final LinearLayout completeInfoLayout;
    private final AutoCompleteTextView educationalBackgroundInput;
    private final EditText fieldOfStudyInput;
    private final MaterialButton confirmBtn;
    private final ImageButton closeBtn;
    private final TextInputLayout educationalBackgroundLayout;

    private boolean isVisible = false;
    private String userEmail;
    private String userFirstName;
    private String userLastName;

    private OnProfileCompleteListener listener;

    public interface OnProfileCompleteListener {
        void onProfileComplete(String email, String firstName, String lastName,
                               String educationalBackground, String fieldOfStudy);
    }

    public CompleteProfileHandler(Activity activity) {
        this.activity = activity;

        completeInfoLayout = activity.findViewById(R.id.completeInfo);
        educationalBackgroundInput = activity.findViewById(R.id.educationalBackgroundInputComplete);
        fieldOfStudyInput = activity.findViewById(R.id.fieldOfStudyInputComplete);
        confirmBtn = activity.findViewById(R.id.confirmBtnComplete);
        closeBtn = activity.findViewById(R.id.close_complete);
        educationalBackgroundLayout = activity.findViewById(R.id.educationalBackgroundLayoutComplete);

        completeInfoLayout.setVisibility(LinearLayout.GONE);

        setupEducationalBackgroundDropdown();
        setupListeners();

        Log.d(TAG, "CompleteProfileHandler initialized");
    }

    public void setRegistrationHandler(RegistrationHandler handler) {
        this.registrationHandler = handler;
    }

    public void setOnProfileCompleteListener(OnProfileCompleteListener listener) {
        this.listener = listener;
        Log.d(TAG, "OnProfileCompleteListener set");
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
                view.setBackgroundColor(Color.WHITE);
                return view;
            }
        };

        educationalBackgroundInput.setAdapter(adapter);

        educationalBackgroundInput.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            Log.d(TAG, "Educational background selected: " + selected);
            validateForm();
        });
    }

    private void setupListeners() {
        educationalBackgroundInput.addTextChangedListener(new SimpleTextWatcher(() -> {
            Log.d(TAG, "Educational background text changed: " + educationalBackgroundInput.getText().toString());
            validateForm();
        }));

        fieldOfStudyInput.addTextChangedListener(new SimpleTextWatcher(() -> {
            Log.d(TAG, "Field of study text changed: " + fieldOfStudyInput.getText().toString());
            validateForm();
        }));

        // Add close button listener
        closeBtn.setOnClickListener(v -> {
            Log.d(TAG, "Close button clicked");
            hideCompleteInfoForm();
        });

        confirmBtn.setOnClickListener(v -> {
            Log.d(TAG, "Confirm button clicked");

            String educationalBackground = educationalBackgroundInput.getText().toString().trim();
            String fieldOfStudy = fieldOfStudyInput.getText().toString().trim();

            Log.d(TAG, "Email: " + userEmail);
            Log.d(TAG, "First Name: " + userFirstName);
            Log.d(TAG, "Last Name: " + userLastName);
            Log.d(TAG, "Educational Background: " + educationalBackground);
            Log.d(TAG, "Field of Study: " + fieldOfStudy);

            if (validateForm()) {
                if (listener != null) {
                    Log.d(TAG, "Calling onProfileComplete listener");
                    listener.onProfileComplete(userEmail, userFirstName, userLastName,
                            educationalBackground, fieldOfStudy);
                } else {
                    Log.e(TAG, "Listener is null!");
                    Toast.makeText(activity, "Error: Listener not set", Toast.LENGTH_SHORT).show();
                }

                hideCompleteInfoForm();
            } else {
                Log.e(TAG, "Form validation failed");
                Toast.makeText(activity, "Please select an educational background", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateForm() {
        String educationalBackground = educationalBackgroundInput.getText().toString().trim();
        boolean isValid = !educationalBackground.isEmpty();

        Log.d(TAG, "Form validation - Educational Background: '" + educationalBackground + "', Valid: " + isValid);

        confirmBtn.setEnabled(isValid);
        confirmBtn.setAlpha(isValid ? 1f : 0.5f);
        confirmBtn.setBackgroundTintList(activity.getColorStateList(
                isValid ? android.R.color.black : android.R.color.darker_gray));
        confirmBtn.setTextColor(isValid ? Color.WHITE : Color.LTGRAY);

        return isValid;
    }

    public void hideCompleteInfoForm() {
        if (!isVisible) {
            Log.d(TAG, "Form already hidden");
            return;
        }

        Log.d(TAG, "Hiding complete info form");

        // Disable interaction immediately
        completeInfoLayout.setClickable(false);
        completeInfoLayout.setEnabled(false);
        educationalBackgroundInput.setEnabled(false);
        fieldOfStudyInput.setEnabled(false);

        Animation slideDown = AnimationUtils.loadAnimation(activity, R.anim.formslidedown);
        completeInfoLayout.startAnimation(slideDown);

        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Animation started - form already disabled above
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                completeInfoLayout.setVisibility(LinearLayout.GONE);
                isVisible = false;
                resetForm();

                // Immediately make wcform available - no delay
                View wcform = activity.findViewById(R.id.wcform);
                if (wcform != null) {
                    wcform.setClickable(true);
                    wcform.setEnabled(true);
                    wcform.setFocusable(true);
                    wcform.bringToFront();
                    wcform.requestLayout(); // Force layout update
                }
            }
        });
    }

    public void showCompleteInfoForm(String email, String firstName, String lastName) {
        if (isVisible) {
            Log.d(TAG, "Form already visible");
            return;
        }

        // Tell registration handler not to bring wcform to front
        if (registrationHandler != null) {
            registrationHandler.setShouldBringWCFormToFront(false);
        }

        Log.d(TAG, "Showing complete info form for: " + email);
        isVisible = true;

        this.userEmail = email;
        this.userFirstName = firstName;
        this.userLastName = lastName;

        // Enable fields
        educationalBackgroundInput.setEnabled(true);
        fieldOfStudyInput.setEnabled(true);

        // Make wcform non-interactive immediately
        View wcform = activity.findViewById(R.id.wcform);
        if (wcform != null) {
            wcform.setClickable(false);
            wcform.setEnabled(false);
        }

        // Setup complete info form
        completeInfoLayout.setVisibility(LinearLayout.VISIBLE);
        completeInfoLayout.setClickable(true);
        completeInfoLayout.setEnabled(true);
        completeInfoLayout.bringToFront();
        completeInfoLayout.requestLayout(); // Force layout update

        Animation slideUp = AnimationUtils.loadAnimation(activity, R.anim.formslideup);
        completeInfoLayout.startAnimation(slideUp);

        resetForm();
    }

    private void resetForm() {
        Log.d(TAG, "Resetting form");
        educationalBackgroundInput.setText("");
        fieldOfStudyInput.setText("");
        confirmBtn.setEnabled(false);
        confirmBtn.setAlpha(0.5f);
    }
}