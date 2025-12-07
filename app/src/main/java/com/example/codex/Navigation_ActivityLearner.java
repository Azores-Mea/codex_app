package com.example.codex;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import homepage_learner.HomeFragment;
import homepage_learner.LearnFragment;
import homepage_learner.ProgressFragment;
import homepage_learner.ReviewFragment;

public class Navigation_ActivityLearner extends AppCompatActivity {

    private TextView userName, userClass;
    private ImageView avatar;

    private HomeFragment homeFragment;
    private LearnFragment learnFragment;
    private ReviewFragment reviewFragment;
    private ProgressFragment progressFragment;

    private DatabaseReference userRef;
    private ValueEventListener userListener;

    private String userId;
    private BottomNavigationView bottomNavigationView;

    private Fragment currentFragment; // Track current visible fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_main);

        userName = findViewById(R.id.user_name);
        userClass = findViewById(R.id.user_class);
        avatar = findViewById(R.id.imageViewAvatar);

        // Load userId from SessionManager
        SessionManager session = new SessionManager(this);
        userId = String.valueOf(session.getUserId());
        if (userId.equals("-1")) {
            Log.e("NavigationLearner", "Invalid userId, cannot load data!");
            return;
        }

        // Initialize fragments
        homeFragment = new HomeFragment();
        learnFragment = new LearnFragment();
        reviewFragment = new ReviewFragment();
        progressFragment = new ProgressFragment();

        // Setup bottom navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment toShow = null;
            int id = item.getItemId();

            if (id == R.id.home) toShow = homeFragment;
            else if (id == R.id.learn) toShow = learnFragment;
            else if (id == R.id.review) toShow = reviewFragment;
            else if (id == R.id.progress) toShow = progressFragment;

            if (toShow != null) {
                showFragment(toShow);
            }
            return true;
        });

        // Load default fragments
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, progressFragment, "progress").hide(progressFragment)
                    .add(R.id.fragment_container, reviewFragment, "review").hide(reviewFragment)
                    .add(R.id.fragment_container, learnFragment, "learn").hide(learnFragment)
                    .add(R.id.fragment_container, homeFragment, "home")
                    .commit();
            currentFragment = homeFragment;
        }

        // Listen for Start Learning button in HomeFragment
        getSupportFragmentManager().setFragmentResultListener("openLearnFragmentRequest", this, (requestKey, bundle) -> {
            openLearnFragment();
        });

        // Check intent extra to open Learn fragment directly
        if (getIntent().getBooleanExtra("openLearnFragment", false)) {
            openLearnFragment();
        }

        // Setup Firebase listener for real-time updates
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        setupUserListener();
    }

    /**
     * Show a fragment and notify it that it's now visible
     * This ensures fragments reload their data when displayed
     */
    private void showFragment(Fragment toShow) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment[] fragments = {homeFragment, learnFragment, reviewFragment, progressFragment};

        for (Fragment f : fragments) {
            if (f == toShow) {
                ft.show(f);
                // Notify fragment it's visible - trigger data reload
                if (f.isAdded()) {
                    notifyFragmentVisible(f);
                }
            } else {
                ft.hide(f);
            }
        }
        ft.commit();
        currentFragment = toShow;
    }

    /**
     * Notify fragment to reload its data
     * Each fragment should implement a method to refresh data
     */
    private void notifyFragmentVisible(Fragment fragment) {
        try {
            if (fragment instanceof HomeFragment) {
                ((HomeFragment) fragment).onFragmentVisible();
            } else if (fragment instanceof ReviewFragment) {
                ((ReviewFragment) fragment).onFragmentVisible();
            } else if (fragment instanceof ProgressFragment) {
                ((ProgressFragment) fragment).onFragmentVisible();
            }
        } catch (Exception e) {
            Log.e("NavigationLearner", "Error notifying fragment: " + e.getMessage());
        }
    }

    private void setupUserListener() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }

                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String classification = snapshot.child("classification").getValue(String.class);

                runOnUiThread(() -> {
                    userName.setText(firstName);
                    updateClassificationBadge(classification);

                    // Refresh current fragment when user data changes
                    if (currentFragment != null && currentFragment.isAdded()) {
                        View fragmentContainer = findViewById(R.id.fragment_container);
                        if (fragmentContainer.getVisibility() != View.VISIBLE) {
                            fragmentContainer.setVisibility(View.VISIBLE);
                        }
                        notifyFragmentVisible(currentFragment);
                    }

                    if ("notClassified".equalsIgnoreCase(classification)) {
                        showInitialTest();
                    }
                });

                Log.d("NavigationLearner", "User data updated: " + snapshot.getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NavigationLearner", "Firebase listener cancelled: " + error.getMessage());
            }
        };

        userRef.addValueEventListener(userListener);
    }

    private void updateClassificationBadge(String classification) {
        if (classification == null) classification = "notClassified";

        switch (classification.toLowerCase()) {
            case "beginner":
                userClass.setBackgroundResource(R.drawable.beginner_classifier);
                break;
            case "intermediate":
                userClass.setBackgroundResource(R.drawable.intermediate_classifier);
                break;
            case "advanced":
                userClass.setBackgroundResource(R.drawable.advanced_classifier);
                break;
            default:
                userClass.setBackgroundResource(R.drawable.none_classifier);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh current fragment when activity resumes
        if (currentFragment != null && currentFragment.isAdded()) {
            notifyFragmentVisible(currentFragment);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Refresh current fragment when activity starts
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }

    /**
     * Public method to show LearnFragment
     */
    public void openLearnFragment() {
        showFragment(learnFragment);

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.learn);
        }
    }

    /**
     * Public method to refresh all fragments
     */
    public void refreshAllFragments() {
        Fragment[] fragments = {homeFragment, learnFragment, reviewFragment, progressFragment};
        for (Fragment f : fragments) {
            if (f != null && f.isAdded()) {
                notifyFragmentVisible(f);
            }
        }
    }

    /**
     * Public method to refresh current visible fragment
     */
    public void refreshCurrentFragment() {
        if (currentFragment != null && currentFragment.isAdded()) {
            notifyFragmentVisible(currentFragment);
        }
    }

    private void showInitialTest() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        if (!isFinishing() && !isDestroyed()) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setLayout(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setDimAmount(0.6f);
            }
        } else {
            Log.w("NavigationLearner", "Activity finishing/destroyed - cannot show initial test dialog");
            return;
        }

        String firstName = userName.getText().toString();
        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Welcome, " + firstName + "!");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView message = dialogView.findViewById(R.id.dialog_message);
        message.setText(Html.fromHtml("To proceed, start with a quick Java proficiency test.", Html.FROM_HTML_MODE_LEGACY));

        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        int redColor = Color.parseColor("#E31414");
        btnNo.setTextColor(redColor);
        btnNo.setStrokeColor(android.content.res.ColorStateList.valueOf(redColor));

        btnNo.setText("Close");
        btnYes.setText("Take");

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, InitialTestActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}