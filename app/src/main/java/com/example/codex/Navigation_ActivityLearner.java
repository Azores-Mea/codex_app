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

public class Navigation_ActivityLearner extends AppCompatActivity {

    private TextView userName, userClass;
    private ImageView avatar;

    private HomeFragment homeFragment;
    private LearnFragment learnFragment;
    private ReviewFragment reviewFragment;
    private ProgressFragment progressFragment;

    private DatabaseReference userRef;
    private ValueEventListener userListener;

    private String userId; // store logged-in user ID

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

        // Check intent extra to open Learn fragment directly
        if (getIntent().getBooleanExtra("openLearnFragment", false)) {
            openLearnFragment();
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment toShow = null;
            int id = item.getItemId();

            if (id == R.id.home) toShow = homeFragment;
            else if (id == R.id.learn) toShow = learnFragment;
            else if (id == R.id.review) toShow = reviewFragment;
            else if (id == R.id.progress) toShow = progressFragment;

            if (toShow != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                Fragment[] fragments = {homeFragment, learnFragment, reviewFragment, progressFragment};
                for (Fragment f : fragments) {
                    if (f == toShow) ft.show(f);
                    else ft.hide(f);
                }
                ft.commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, progressFragment, "progress").hide(progressFragment)
                    .add(R.id.fragment_container, reviewFragment, "review").hide(reviewFragment)
                    .add(R.id.fragment_container, learnFragment, "learn").hide(learnFragment)
                    .add(R.id.fragment_container, homeFragment, "home")
                    .commit();
        }

        // Setup Firebase listener for real-time updates
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        setupUserListener();
    }

    private void setupUserListener() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String classification = snapshot.child("classification").getValue(String.class);

                // Update UI immediately
                runOnUiThread(() -> {
                    userName.setText(firstName);
                    updateClassificationBadge(classification);

                    // Show initial test if not classified
                    if ("notClassified".equalsIgnoreCase(classification)) {
                        showInitialTest();
                    }
                    // ✅ NEW: show Choose Mode dialog for intermediate/advanced users
                    else if ("intermediate".equalsIgnoreCase(classification) ||
                            "advanced".equalsIgnoreCase(classification)) {
                        showChooseModeDialog(classification);
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
    protected void onDestroy() {
        super.onDestroy();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }

    public void openLearnFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment[] fragments = {homeFragment, learnFragment, reviewFragment, progressFragment};
        for (Fragment f : fragments) {
            if (f == learnFragment) ft.show(f);
            else ft.hide(f);
        }
        ft.commit();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.learn);
    }

    private void showInitialTest() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        if (!isFinishing() && !isDestroyed()) {
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

    // ✅ Choose Mode dialog (redirects to SelectionMode activity)
    private void showChooseModeDialog(String classification) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        if (!isFinishing() && !isDestroyed()) {
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
            Log.w("NavigationLearner", "Activity finishing/destroyed - cannot show choose mode dialog");
            return;
        }

        String firstName = userName.getText().toString();
        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Hello, " + firstName + "!");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView message = dialogView.findViewById(R.id.dialog_message);
        message.setText(Html.fromHtml("You can now select your learning mode.", Html.FROM_HTML_MODE_LEGACY));

        MaterialButton btnCancel = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnSelect = dialogView.findViewById(R.id.yes_btn);

        int redColor = Color.parseColor("#E31414");
        btnCancel.setTextColor(redColor);
        btnCancel.setStrokeColor(android.content.res.ColorStateList.valueOf(redColor));

        btnCancel.setText("Cancel");
        btnSelect.setText("Select Mode");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // ✅ Open SelectionMode activity instead of dialog
        btnSelect.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, SelectionMode.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}
