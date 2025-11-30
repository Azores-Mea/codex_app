package homepage_learner;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.codex.GradientTextUtil;
import com.example.codex.MainActivity;
import com.example.codex.Navigation_ActivityLearner;
import com.example.codex.R;
import com.example.codex.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class HomeFragment extends Fragment {

    private LinearLayout currentLearner, scoreHistoryContainer;
    private CardView newLearner;
    private DatabaseReference dbRefAct, userRef;
    private SessionManager sessionManager;

    private TextView userName, userClass;
    private ImageView avatar;
    private ValueEventListener userListener;
    private FrameLayout nextLessonContainer;
    private DatabaseReference lessonsRef, quizResultsRef, exerciseResultsRef, codingExercisesRef;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new SessionManager(requireContext());

        nextLessonContainer = view.findViewById(R.id.nextLessonContainer);

// Initialize Firebase references for lessons
        lessonsRef = FirebaseDatabase.getInstance().getReference("Lessons");
        quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");
        exerciseResultsRef = FirebaseDatabase.getInstance().getReference("exerciseResults");
        codingExercisesRef = FirebaseDatabase.getInstance().getReference("coding_exercises");

        // ---------- HEADER ----------
        userName = view.findViewById(R.id.user_name);
        userClass = view.findViewById(R.id.user_class);
        avatar = view.findViewById(R.id.logout);
        TextView greetName = view.findViewById(R.id.learner_name);

        // ---------- SCORE VIEWS ----------
        currentLearner = view.findViewById(R.id.current_learner);
        scoreHistoryContainer = view.findViewById(R.id.scoreHistoryContainer);
        newLearner = view.findViewById(R.id.new_learner);

        showNewLearner();
        avatar.setOnClickListener(v -> showLogoutDialog());

        // ---------- FIREBASE REFERENCES ----------
        dbRefAct = FirebaseDatabase.getInstance().getReference("RecentAct");

        // ---------- NEW LEARNER BUTTON ----------
        newLearner.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Navigation_ActivityLearner.class);
            startActivity(intent);
        });

        MaterialButton startLearning = view.findViewById(R.id.start_learning);
        startLearning.setOnClickListener(v -> {
            // Notify the parent activity to switch to LearnFragment
            Bundle result = new Bundle();
            getParentFragmentManager().setFragmentResult("openLearnFragmentRequest", result);
        });

        if (sessionManager.isLoggedIn()) {
            int userId = sessionManager.getUserId();
            Log.d("HomeFragment", "Logged in user ID: " + userId);

            if (userId != -1) {
                userRef = FirebaseDatabase.getInstance().getReference("Users").child(String.valueOf(userId));
                attachUserListener(greetName);
                attachScoreListener(userId);
            } else {
                showNewLearner();
            }
        } else {
            showNewLearner();
        }

        return view;
    }

    private void loadNextLesson(int userId) {
        String userIdStr = String.valueOf(userId);

        quizResultsRef.child(userIdStr).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot quizSnapshot) {
                exerciseResultsRef.child(userIdStr).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot exerciseSnapshot) {
                        codingExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot codingSnapshot) {
                                lessonsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot lessonsSnapshot) {
                                        String nextLessonId = findNextUnlockedLesson(lessonsSnapshot, quizSnapshot, exerciseSnapshot, codingSnapshot);

                                        if (nextLessonId != null) {
                                            displayNextLesson(lessonsSnapshot.child(nextLessonId), nextLessonId, quizSnapshot, exerciseSnapshot, codingSnapshot);
                                        } else {
                                            // No next lesson found - hide continue learning section
                                            currentLearner.setVisibility(View.GONE);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("HomeFragment", "Failed to load lessons: " + error.getMessage());
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String findNextUnlockedLesson(DataSnapshot lessonsSnapshot, DataSnapshot quizSnapshot,
                                          DataSnapshot exerciseSnapshot, DataSnapshot codingSnapshot) {
        // Start from L1 and check each lesson sequentially
        for (int i = 1; i <= 50; i++) { // Adjust max number based on your lesson count
            String lessonId = "L" + i;

            if (!lessonsSnapshot.child(lessonId).exists()) {
                continue; // Skip if lesson doesn't exist
            }

            // Check if this lesson is locked
            if (lessonId.equals("L1")) {
                // L1 is always available
                // Check if L1 is completed
                if (!isLessonCompleted(lessonId, quizSnapshot, exerciseSnapshot, codingSnapshot)) {
                    return lessonId;
                }
            } else {
                // Check previous lesson
                String prevLessonId = "L" + (i - 1);

                // If previous lesson is completed, this is the next one
                if (isLessonCompleted(prevLessonId, quizSnapshot, exerciseSnapshot, codingSnapshot)) {
                    // Check if current lesson is not completed
                    if (!isLessonCompleted(lessonId, quizSnapshot, exerciseSnapshot, codingSnapshot)) {
                        return lessonId;
                    }
                }
            }
        }

        return null; // All lessons completed
    }

    private boolean isLessonCompleted(String lessonId, DataSnapshot quizSnapshot,
                                      DataSnapshot exerciseSnapshot, DataSnapshot codingSnapshot) {
        // Check quiz completion
        boolean quizPassed = false;
        if (quizSnapshot.child(lessonId).exists()) {
            String passed = quizSnapshot.child(lessonId).child("passed").getValue(String.class);
            quizPassed = passed != null && passed.equalsIgnoreCase("Passed");
        }

        if (!quizPassed) return false;

        // Check if lesson has coding exercises
        boolean hasCodingExercises = codingSnapshot.child(lessonId).exists()
                && codingSnapshot.child(lessonId).hasChildren();

        if (hasCodingExercises) {
            Boolean exerciseCompleted = exerciseSnapshot.child(lessonId).child("completed").getValue(Boolean.class);
            return exerciseCompleted != null && exerciseCompleted;
        }

        // Quiz passed and no exercises, so completed
        return true;
    }

    private void displayNextLesson(DataSnapshot lessonSnapshot, String lessonId,
                                   DataSnapshot quizSnapshot, DataSnapshot exerciseSnapshot,
                                   DataSnapshot codingSnapshot) {
        nextLessonContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View lessonView = inflater.inflate(R.layout.lesson_unlocked, nextLessonContainer, false);

        com.google.android.material.card.MaterialCardView card = lessonView.findViewById(R.id.lesson1);
        TextView lTitle = lessonView.findViewById(R.id.l_title);
        TextView lAbout = lessonView.findViewById(R.id.l_about);
        ImageView diffBadge = lessonView.findViewById(R.id.difficulty);
        ImageView lProgress = lessonView.findViewById(R.id.l_progress);

        String titleHtml = lessonSnapshot.child("main_title").getValue(String.class);
        String descHtml = lessonSnapshot.child("title_desc").getValue(String.class);
        String difficulty = lessonSnapshot.child("difficulty").getValue(String.class);

        if (titleHtml == null) titleHtml = lessonId;
        if (descHtml == null) descHtml = "No description available";
        if (difficulty == null) difficulty = "beginner";

        String truncatedTitle = truncateText(Html.fromHtml(titleHtml, Html.FROM_HTML_MODE_LEGACY).toString(), 60);
        String truncatedDesc = truncateText(Html.fromHtml(descHtml, Html.FROM_HTML_MODE_LEGACY).toString(), 120);

        lTitle.setText(Html.fromHtml(truncatedTitle, Html.FROM_HTML_MODE_LEGACY));
        lAbout.setText(Html.fromHtml(truncatedDesc, Html.FROM_HTML_MODE_LEGACY));

        // Set difficulty badge and card color
        switch (difficulty.toLowerCase()) {
            case "beginner":
                diffBadge.setImageResource(R.drawable.beginner_classifier);
                break;
            case "intermediate":
                diffBadge.setImageResource(R.drawable.intermediate_classifier);
                card.setStrokeColor(Color.parseColor("#66ABF4"));
                break;
            case "advanced":
                diffBadge.setImageResource(R.drawable.advanced_classifier);
                card.setStrokeColor(Color.parseColor("#A666F4"));
                break;
            default:
                diffBadge.setImageResource(R.drawable.none_classifier);
                break;
        }

        // Set progress icon (should be gray since it's not completed)
        lProgress.setColorFilter(getResources().getColor(R.color.gray), PorterDuff.Mode.SRC_IN);

        // Set click listener to open lesson
        card.setOnClickListener(v -> {
            sessionManager.saveSelectedLesson(lessonId);
            Intent intent = new Intent(requireContext(), com.example.codex.LessonActivity.class);
            intent.putExtra("lessonId", lessonId);
            startActivity(intent);
        });

        nextLessonContainer.addView(lessonView);
        currentLearner.setVisibility(View.VISIBLE);
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3).trim() + "...";
        }
        return text;
    }

    private void attachUserListener(TextView greetName) {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String classification = snapshot.child("classification").getValue(String.class);

                String fullName = (firstName != null ? firstName : "User") +
                        (lastName != null ? " " + lastName : "");
                userName.setText(fullName);
                greetName.setText(fullName);

                if (classification != null) {
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
                } else {
                    userClass.setBackgroundResource(R.drawable.none_classifier);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        userRef.addValueEventListener(userListener);
    }

    private void attachScoreListener(int userId) {
        Log.d("HomeFragment", "Listening for RecentAct userId: " + userId);

        // Load next lesson
        loadNextLesson(userId);

        dbRefAct.child(String.valueOf(userId))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("HomeFragment", "Score Snapshot Exists: " + snapshot.exists() +
                                " | Children: " + snapshot.getChildrenCount());

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            Log.w("HomeFragment", "No RecentAct data found for user " + userId);
                            hideAllSections();
                            showNewLearner();
                            return;
                        }

                        showCurrentLearner();
                        loadScoreHistory(snapshot);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeFragment", "Score listener cancelled: " + error.getMessage());
                    }
                });
    }

    private void loadScoreHistory(DataSnapshot snapshot) {
        scoreHistoryContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (DataSnapshot record : snapshot.getChildren()) {
            String title = record.child("title").getValue(String.class);
            Long score = record.child("score").getValue(Long.class);
            Long items = record.child("items").getValue(Long.class);

            View scoreView = inflater.inflate(R.layout.recent_scores, scoreHistoryContainer, false);
            TextView titleView = scoreView.findViewById(R.id.testTitle);
            TextView scoreViewText = scoreView.findViewById(R.id.testScore);

            titleView.setText(title != null ? title : "Untitled Test");

            long totalItems = (items != null) ? items : 20;
            long scoreValue = (score != null) ? score : 0;
            scoreViewText.setText("Score: " + scoreValue + "/" + totalItems);

            scoreHistoryContainer.addView(scoreView);
        }

        if (scoreHistoryContainer.getChildCount() == 0) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No score history found.");
            scoreHistoryContainer.addView(emptyText);
        }
    }

    @SuppressLint("InflateParams")
    private void showLogoutDialog() {

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow())
                .setBackgroundDrawableResource(android.R.color.transparent);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setDimAmount(0.6f);

        // ----- TITLE WITH GRADIENT -----
        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Are you sure you want to log out?");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        // ----- MESSAGE (optional, if your layout has dialog_message) -----
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        if (dialogMessage != null) {
            String html = "<b><font color='#09417D'>Are you sure</font></b> you want to "
                    + "<b><font color='#09417D'>log out</font></b>?";
            dialogMessage.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        }

        // ----- BUTTONS -----
        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        btnNo.setText("No");
        btnYes.setText("Log Out");
        btnYes.setTextColor(ColorStateList.valueOf(Color.parseColor("#E31414")));
        btnYes.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        btnYes.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E31414")));
        int strokeInDp = 2;
        int strokeWidthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                strokeInDp,
                btnYes.getResources().getDisplayMetrics()
        );
        btnYes.setStrokeWidth(strokeWidthPx);

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();

            sessionManager.logout(); // Clears session + sets loggedOutFlag

            // Ensure old prefs removed:
            SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(requireActivity(), MainActivity.class);
            startActivity(intent);

            requireActivity().finish();
        });

    }


    private void showCurrentLearner() {
        currentLearner.setVisibility(View.VISIBLE);
        scoreHistoryContainer.setVisibility(View.VISIBLE);
        newLearner.setVisibility(View.GONE);  // ✅ hide new learner card
    }


    private void showNewLearner() {
        currentLearner.setVisibility(View.GONE);
        scoreHistoryContainer.setVisibility(View.GONE);
        newLearner.setVisibility(View.VISIBLE);
    }

    private void hideAllSections() {
        currentLearner.setVisibility(View.GONE);
        scoreHistoryContainer.setVisibility(View.GONE);
        newLearner.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }
}
