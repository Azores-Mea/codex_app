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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private LinearLayout currentLearner, scoreHistoryContainer;
    private CardView newLearner;
    private DatabaseReference dbRefAct, userRef, dbRefRecentLesson;
    private SessionManager sessionManager;

    private TextView userName, userClass, greetName;
    private ImageView avatar;
    private ValueEventListener userListener;
    private FrameLayout nextLessonContainer;
    private DatabaseReference lessonsRef, quizResultsRef, exerciseResultsRef, codingExercisesRef;

    private int userId = -1;
    private String cachedUserData = null;
    private String cachedNextLesson = null;
    private int cachedScoreCount = -1;

    // Store lesson data for sorting
    private static class LessonData {
        String key, title, desc, difficulty;
        boolean isCompleted;
    }

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
        greetName = view.findViewById(R.id.learner_name);

        // ---------- SCORE VIEWS ----------
        currentLearner = view.findViewById(R.id.current_learner);
        scoreHistoryContainer = view.findViewById(R.id.scoreHistoryContainer);
        newLearner = view.findViewById(R.id.new_learner);

        showNewLearner();
        avatar.setOnClickListener(v -> showLogoutDialog());

        // ---------- FIREBASE REFERENCES ----------
        dbRefAct = FirebaseDatabase.getInstance().getReference("RecentAct");
        dbRefRecentLesson = FirebaseDatabase.getInstance().getReference("RecentLesson");

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

        // Get userId
        userId = sessionManager.getUserId();

        // Initial load
        loadFragmentData();

        return view;
    }

    /**
     * Called when fragment becomes visible - reload all data
     */
    public void onFragmentVisible() {
        Log.d("HomeFragment", "Fragment is now visible - checking for changes");

        if (sessionManager.isLoggedIn() && userId != -1) {
            checkAndReloadIfNeeded();
        }
    }

    private void checkAndReloadIfNeeded() {
        // Check user data changes
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentUserData = snapshot.child("firstName").getValue(String.class) +
                        snapshot.child("classification").getValue(String.class);

                if (!currentUserData.equals(cachedUserData)) {
                    cachedUserData = currentUserData;
                    loadFragmentData();
                } else {
                    Log.d("HomeFragment", "No user data changes detected");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Check lesson progress changes
        dbRefRecentLesson.child(String.valueOf(userId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long currentLessonCount = snapshot.getChildrenCount();

                        if (cachedNextLesson == null || currentLessonCount != cachedScoreCount) {
                            cachedScoreCount = (int) currentLessonCount;
                            loadNextLesson(userId);
                        } else {
                            Log.d("HomeFragment", "No lesson progress changes detected");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /**
     * Load or reload all fragment data
     */
    private void loadFragmentData() {
        if (sessionManager.isLoggedIn() && userId != -1) {
            Log.d("HomeFragment", "Loading data for user ID: " + userId);

            // Detach old listener if exists
            if (userRef != null && userListener != null) {
                userRef.removeEventListener(userListener);
            }

            // Reattach user listener
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(String.valueOf(userId));
            attachUserListener(greetName);

            // Reload score data and lessons
            attachScoreListener(userId);
        } else {
            showNewLearner();
        }
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
                                        // Check if user has ANY lesson progress
                                        boolean hasProgress = hasAnyLessonProgress(quizSnapshot, exerciseSnapshot);

                                        if (!hasProgress) {
                                            // No progress at all - show new learner card only
                                            currentLearner.setVisibility(View.GONE);
                                            return;
                                        }

                                        // Build lesson map for processing
                                        Map<String, List<LessonData>> lessonsByDiff = buildLessonMap(lessonsSnapshot, quizSnapshot, exerciseSnapshot, codingSnapshot);

                                        String nextLessonId = findNextUnlockedLesson(lessonsByDiff, quizSnapshot, exerciseSnapshot, codingSnapshot);

                                        if (nextLessonId != null && !nextLessonId.equals("L1")) {
                                            // Display next lesson only if it's not L1
                                            displayNextLesson(lessonsSnapshot.child(nextLessonId), nextLessonId, quizSnapshot, exerciseSnapshot, codingSnapshot);
                                        } else {
                                            // Next lesson is L1 or null - hide continue learning section
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

    /**
     * Build a map of lessons organized by difficulty with completion status
     */
    private Map<String, List<LessonData>> buildLessonMap(DataSnapshot lessonsSnapshot,
                                                         DataSnapshot quizSnapshot,
                                                         DataSnapshot exerciseSnapshot,
                                                         DataSnapshot codingSnapshot) {
        Map<String, List<LessonData>> lessonsByDiff = new HashMap<>();

        for (DataSnapshot lessonSnap : lessonsSnapshot.getChildren()) {
            String lessonKey = lessonSnap.getKey();
            String titleHtml = lessonSnap.child("main_title").getValue(String.class);
            String descHtml = lessonSnap.child("title_desc").getValue(String.class);
            String difficulty = lessonSnap.child("difficulty").getValue(String.class);

            if (difficulty == null) difficulty = "beginner";
            if (titleHtml == null) titleHtml = lessonKey;
            if (descHtml == null) descHtml = "No description available";

            // Check completion
            boolean isCompleted = isLessonCompleted(lessonKey, quizSnapshot, exerciseSnapshot, codingSnapshot);

            LessonData data = new LessonData();
            data.key = lessonKey;
            data.title = titleHtml;
            data.desc = descHtml;
            data.difficulty = difficulty;
            data.isCompleted = isCompleted;

            lessonsByDiff.computeIfAbsent(difficulty.toLowerCase(), k -> new ArrayList<>()).add(data);
        }

        // Sort lessons within each difficulty by lesson number
        for (List<LessonData> lessons : lessonsByDiff.values()) {
            Collections.sort(lessons, new Comparator<LessonData>() {
                @Override
                public int compare(LessonData l1, LessonData l2) {
                    try {
                        int num1 = Integer.parseInt(l1.key.replaceAll("[^0-9]", ""));
                        int num2 = Integer.parseInt(l2.key.replaceAll("[^0-9]", ""));
                        return Integer.compare(num1, num2);
                    } catch (NumberFormatException e) {
                        return l1.key.compareTo(l2.key);
                    }
                }
            });
        }

        return lessonsByDiff;
    }

    /**
     * Find the next unlocked lesson based on LearnFragment logic
     */
    private String findNextUnlockedLesson(Map<String, List<LessonData>> lessonsByDiff,
                                          DataSnapshot quizSnapshot,
                                          DataSnapshot exerciseSnapshot,
                                          DataSnapshot codingSnapshot) {
        // Check beginner lessons first
        String nextBeginner = findNextInDifficulty(lessonsByDiff.get("beginner"), quizSnapshot, exerciseSnapshot, codingSnapshot, null);
        if (nextBeginner != null) {
            return nextBeginner;
        }

        // All beginner completed - check if intermediate is unlocked
        List<LessonData> beginnerLessons = lessonsByDiff.get("beginner");
        boolean allBeginnerCompleted = areAllLessonsCompleted(beginnerLessons, quizSnapshot, exerciseSnapshot, codingSnapshot);

        if (allBeginnerCompleted) {
            String nextIntermediate = findNextInDifficulty(lessonsByDiff.get("intermediate"), quizSnapshot, exerciseSnapshot, codingSnapshot, null);
            if (nextIntermediate != null) {
                return nextIntermediate;
            }

            // All intermediate completed - check if advanced is unlocked
            List<LessonData> intermediateLessons = lessonsByDiff.get("intermediate");
            boolean allIntermediateCompleted = areAllLessonsCompleted(intermediateLessons, quizSnapshot, exerciseSnapshot, codingSnapshot);

            if (allIntermediateCompleted) {
                String nextAdvanced = findNextInDifficulty(lessonsByDiff.get("advanced"), quizSnapshot, exerciseSnapshot, codingSnapshot, null);
                if (nextAdvanced != null) {
                    return nextAdvanced;
                }
            }
        }

        // All lessons completed
        return null;
    }

    /**
     * Find the next incomplete lesson within a difficulty level
     */
    private String findNextInDifficulty(List<LessonData> lessons,
                                        DataSnapshot quizSnapshot,
                                        DataSnapshot exerciseSnapshot,
                                        DataSnapshot codingSnapshot,
                                        String previousLessonId) {
        if (lessons == null || lessons.isEmpty()) return null;

        for (int i = 0; i < lessons.size(); i++) {
            LessonData lesson = lessons.get(i);

            // If this is the first lesson (L1 or first in level), it's always unlocked
            if (i == 0) {
                if (!lesson.isCompleted) {
                    return lesson.key;
                }
                continue;
            }

            // Check if previous lesson is completed
            LessonData previousLesson = lessons.get(i - 1);
            boolean previousCompleted = isLessonCompleted(previousLesson.key, quizSnapshot, exerciseSnapshot, codingSnapshot);

            // If previous lesson is completed but current is not, this is the next lesson
            if (previousCompleted && !lesson.isCompleted) {
                return lesson.key;
            }
        }

        return null;
    }

    /**
     * Check if all lessons in a difficulty level are completed
     */
    private boolean areAllLessonsCompleted(List<LessonData> lessons,
                                           DataSnapshot quizSnapshot,
                                           DataSnapshot exerciseSnapshot,
                                           DataSnapshot codingSnapshot) {
        if (lessons == null || lessons.isEmpty()) return false;

        for (LessonData lesson : lessons) {
            if (!isLessonCompleted(lesson.key, quizSnapshot, exerciseSnapshot, codingSnapshot)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasAnyLessonProgress(DataSnapshot quizSnapshot, DataSnapshot exerciseSnapshot) {
        if (quizSnapshot.exists() && quizSnapshot.hasChildren()) {
            for (DataSnapshot lesson : quizSnapshot.getChildren()) {
                String passed = lesson.child("passed").getValue(String.class);
                if (passed != null && passed.equalsIgnoreCase("Passed")) {
                    return true;
                }
            }
        }

        if (exerciseSnapshot.exists() && exerciseSnapshot.hasChildren()) {
            for (DataSnapshot lesson : exerciseSnapshot.getChildren()) {
                Boolean completed = lesson.child("completed").getValue(Boolean.class);
                if (completed != null && completed) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isLessonCompleted(String lessonId, DataSnapshot quizSnapshot,
                                      DataSnapshot exerciseSnapshot, DataSnapshot codingSnapshot) {
        // Step 1: Check if quiz is passed
        boolean quizPassed = false;
        if (quizSnapshot.child(lessonId).exists()) {
            String passed = quizSnapshot.child(lessonId).child("passed").getValue(String.class);
            quizPassed = passed != null && passed.equalsIgnoreCase("Passed");
        }

        // If quiz not passed, lesson is not completed
        if (!quizPassed) {
            return false;
        }

        // Step 2: Check if lesson has coding exercises
        boolean hasCodingExercises = codingSnapshot.child(lessonId).exists()
                && codingSnapshot.child(lessonId).hasChildren();

        // Step 3: If has exercises, check if they're completed
        if (hasCodingExercises) {
            Boolean exerciseCompleted = exerciseSnapshot.child(lessonId).child("completed").getValue(Boolean.class);
            return exerciseCompleted != null && exerciseCompleted;
        }

        // No exercises required, quiz passed = lesson completed
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

        lProgress.setColorFilter(getResources().getColor(R.color.gray), PorterDuff.Mode.SRC_IN);

        card.setOnClickListener(v -> {
            sessionManager.saveSelectedLesson(lessonId);
            Intent intent = new Intent(requireContext(), com.example.codex.LessonActivity.class);
            intent.putExtra("lessonId", lessonId);
            startActivity(intent);
        });

        nextLessonContainer.addView(lessonView);
        currentLearner.setVisibility(View.VISIBLE);

        Log.d("HomeFragment", "Displayed next lesson: " + lessonId);
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
                if (!snapshot.exists() || !isAdded()) return;

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

        loadNextLesson(userId);

        dbRefRecentLesson.child(String.valueOf(userId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot recentLessonSnapshot) {
                        if (!isAdded()) return;

                        boolean hasProgress = recentLessonSnapshot.exists() && recentLessonSnapshot.getChildrenCount() > 0;

                        if (hasProgress) {
                            // Immediately show continue learning card
                            currentLearner.setVisibility(View.VISIBLE);
                            newLearner.setVisibility(View.GONE);

                            // Load next lesson asynchronously (no UI freeze)
                            loadNextLesson(userId);
                        } else {
                            // Immediately show new learner card
                            currentLearner.setVisibility(View.GONE);
                            newLearner.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });


        dbRefAct.child(String.valueOf(userId))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        Log.d("HomeFragment", "Score Snapshot Exists: " + snapshot.exists() +
                                " | Children: " + snapshot.getChildrenCount());

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            Log.w("HomeFragment", "No RecentAct data found for user " + userId);
                            scoreHistoryContainer.setVisibility(View.GONE);
                            return;
                        }

                        scoreHistoryContainer.setVisibility(View.VISIBLE);
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

        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Are you sure you want to log out?");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        if (dialogMessage != null) {
            String html = "<b><font color='#09417D'>Are you sure</font></b> you want to "
                    + "<b><font color='#09417D'>log out</font></b>?";
            dialogMessage.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        }

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
            sessionManager.logout();
            SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void showNewLearner() {
        currentLearner.setVisibility(View.GONE);
        newLearner.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }
}