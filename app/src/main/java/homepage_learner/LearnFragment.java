package homepage_learner;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.codex.LessonActivity;
import com.example.codex.R;
import com.example.codex.SessionManager;
import androidx.cardview.widget.CardView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LearnFragment extends Fragment {

    private LinearLayout sectionBeginnerHeader;
    private LinearLayout sectionBeginnerContent;
    private LinearLayout sectionIntermediateHeader;
    private LinearLayout sectionIntermediateContent;
    private LinearLayout sectionAdvancedHeader;
    private LinearLayout sectionAdvancedContent;
    private ImageView iconArrow;
    private ImageView iconArrowIntermediate;
    private ImageView iconArrowAdvanced;
    private boolean isBeginnerVisible = false;
    private boolean isIntermediateVisible = false;
    private boolean isAdvancedVisible = false;
    private CardView lesson1;
    private TextView tvUserName;
    private TextView tvLevel;
    private TextView beginnerProgress;
    private TextView intermediateProgress;
    private TextView advancedProgress;

    // Progress indicators
    private ProgressBar progressCircle;
    private TextView tvProgressPercent;
    private TextView completedProgress;
    private TextView remainingProgress;
    private TextView totalProgress;

    // Card views for locked/unlocked states
    private CardView cardIntermediate;
    private CardView cardIntermediateLocked;
    private CardView cardAdvanced;
    private CardView cardAdvancedLocked;

    // Side panel components
    private CardView sidePanel;
    private ImageView lessonStructureBtn;
    private ImageView closePanelBtn;
    private LinearLayout beginnerLessonsList;
    private LinearLayout intermediateLessonsList;
    private LinearLayout advancedLessonsList;
    private boolean isSidePanelVisible = false;

    private DatabaseReference lessonsRef;
    private DatabaseReference quizResultsRef;
    private DatabaseReference exerciseResultsRef;
    private DatabaseReference codingExercisesRef;

    // Store lesson data for side panel
    private Map<String, List<LessonData>> lessonsByDifficultyMap = new HashMap<>();

    private static class LessonData {
        String key, title, desc, difficulty;
        boolean isCompleted;
    }

    public LearnFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_learn, container, false);

        // Initialize existing views
        sectionBeginnerHeader = view.findViewById(R.id.sectionBeginnerHeader);
        sectionBeginnerContent = view.findViewById(R.id.sectionBeginnerContent);
        sectionIntermediateHeader = view.findViewById(R.id.sectionIntermediateHeader);
        sectionIntermediateContent = view.findViewById(R.id.sectionIntermediateContent);
        sectionAdvancedHeader = view.findViewById(R.id.sectionAdvancedHeader);
        sectionAdvancedContent = view.findViewById(R.id.sectionAdvancedContent);
        iconArrow = view.findViewById(R.id.iconArrow);
        iconArrowIntermediate = view.findViewById(R.id.iconArrowIntermediate);
        iconArrowAdvanced = view.findViewById(R.id.iconArrowAdvanced);
        lesson1 = view.findViewById(R.id.lesson1);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvLevel = view.findViewById(R.id.tvLevel);
        beginnerProgress = view.findViewById(R.id.beginnerProgress);
        intermediateProgress = view.findViewById(R.id.intermediateProgress);
        advancedProgress = view.findViewById(R.id.advancedProgress);

        // Initialize progress indicators
        progressCircle = view.findViewById(R.id.progressCircle);
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        completedProgress = view.findViewById(R.id.completed_progress);
        remainingProgress = view.findViewById(R.id.remaining_progress);
        totalProgress = view.findViewById(R.id.total_progress);

        // Initialize card views
        cardIntermediate = view.findViewById(R.id.cardIntermediate);
        cardIntermediateLocked = view.findViewById(R.id.cardIntermediateLocked);
        cardAdvanced = view.findViewById(R.id.cardAdvanced);
        cardAdvancedLocked = view.findViewById(R.id.cardAdvancedLocked);

        // Initialize side panel components
        sidePanel = view.findViewById(R.id.sidePanel);
        lessonStructureBtn = view.findViewById(R.id.lessonStructure);
        closePanelBtn = view.findViewById(R.id.closePanelBtn);
        beginnerLessonsList = view.findViewById(R.id.beginnerLessonsList);
        intermediateLessonsList = view.findViewById(R.id.intermediateLessonsList);
        advancedLessonsList = view.findViewById(R.id.advancedLessonsList);

        lessonsRef = FirebaseDatabase.getInstance().getReference("Lessons");
        quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");
        exerciseResultsRef = FirebaseDatabase.getInstance().getReference("exerciseResults");
        codingExercisesRef = FirebaseDatabase.getInstance().getReference("coding_exercises");

        SessionManager sessionManager = new SessionManager(requireContext());

        if (sessionManager.isLoggedIn()) {
            String fullName = sessionManager.getFirstName() + " " + sessionManager.getLastName();
            String classification = sessionManager.getClassification();
            tvUserName.setText(fullName);
            updateClassificationBadge(classification);
        }

        // Set up click listeners
        if (sectionBeginnerHeader != null) {
            sectionBeginnerHeader.setOnClickListener(v -> toggleSectionBeginner());
        }
        if (sectionIntermediateHeader != null) {
            sectionIntermediateHeader.setOnClickListener(v -> toggleSectionIntermediate());
        }
        if (sectionAdvancedHeader != null) {
            sectionAdvancedHeader.setOnClickListener(v -> toggleSectionAdvanced());
        }

        // Side panel click listeners
        if (lessonStructureBtn != null) {
            lessonStructureBtn.setOnClickListener(v -> openSidePanel());
        }
        if (closePanelBtn != null) {
            closePanelBtn.setOnClickListener(v -> closeSidePanel());
        }

        String userId = String.valueOf(sessionManager.getUserId());

        // Listen for quiz completion changes
        quizResultsRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadLessons(inflater, sessionManager);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Listen for exercise completion changes
        exerciseResultsRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadLessons(inflater, sessionManager);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        loadLessons(inflater, sessionManager);

        return view;
    }

    /**
     * Opens the side panel with slide animation from left
     */
    private void openSidePanel() {
        if (isSidePanelVisible) return;

        // Make sure panel is visible before animating
        sidePanel.setVisibility(View.VISIBLE);

        // Animate the side panel sliding in from left (from -280dp to 0dp)
        ObjectAnimator slideIn = ObjectAnimator.ofFloat(sidePanel, "translationX", -sidePanel.getWidth(), 0f);
        slideIn.setDuration(300);
        slideIn.setInterpolator(new DecelerateInterpolator());
        slideIn.start();

        isSidePanelVisible = true;

        // Populate side panel with current lessons
        populateSidePanel();
    }

    /**
     * Closes the side panel with slide animation to left
     */
    private void closeSidePanel() {
        if (!isSidePanelVisible) return;

        // Animate the side panel sliding out to left (from 0dp to -280dp)
        ObjectAnimator slideOut = ObjectAnimator.ofFloat(sidePanel, "translationX", 0f, -sidePanel.getWidth());
        slideOut.setDuration(300);
        slideOut.setInterpolator(new DecelerateInterpolator());
        slideOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Optional: hide the view completely after animation
                // sidePanel.setVisibility(View.GONE);
            }
        });
        slideOut.start();

        isSidePanelVisible = false;
    }

    /**
     * Populates the side panel with lesson structure
     */
    private void populateSidePanel() {
        beginnerLessonsList.removeAllViews();
        intermediateLessonsList.removeAllViews();
        advancedLessonsList.removeAllViews();

        SessionManager sessionManager = new SessionManager(requireContext());

        // Populate beginner lessons
        List<LessonData> beginnerLessons = lessonsByDifficultyMap.get("beginner");
        if (beginnerLessons != null) {
            for (LessonData lesson : beginnerLessons) {
                addLessonItemToSidePanel(beginnerLessonsList, lesson, sessionManager);
            }
        }

        // Populate intermediate lessons
        List<LessonData> intermediateLessons = lessonsByDifficultyMap.get("intermediate");
        if (intermediateLessons != null) {
            for (LessonData lesson : intermediateLessons) {
                addLessonItemToSidePanel(intermediateLessonsList, lesson, sessionManager);
            }
        }

        // Populate advanced lessons
        List<LessonData> advancedLessons = lessonsByDifficultyMap.get("advanced");
        if (advancedLessons != null) {
            for (LessonData lesson : advancedLessons) {
                addLessonItemToSidePanel(advancedLessonsList, lesson, sessionManager);
            }
        }
    }

    /**
     * Adds a lesson item to the side panel list
     */
    private void addLessonItemToSidePanel(LinearLayout container, LessonData lesson, SessionManager sessionManager) {
        View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.lesson_item_structure, container, false);

        TextView lessonTitle = itemView.findViewById(R.id.lessonItemTitle);
        ImageView statusIcon = itemView.findViewById(R.id.lessonItemStatus);

        // Set lesson title (strip HTML tags for side panel)
        String plainTitle = Html.fromHtml(lesson.title, Html.FROM_HTML_MODE_LEGACY).toString();
        lessonTitle.setText(plainTitle);

        // Always show completion status icon
        statusIcon.setVisibility(View.VISIBLE);

        // Set color based on completion status
        if (lesson.isCompleted) {
            statusIcon.setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC_IN);
        } else {
            statusIcon.setColorFilter(getResources().getColor(R.color.gray), PorterDuff.Mode.SRC_IN);
        }

        // REMOVED: Click listener - panel is now non-interactive
        // Make the item non-clickable
        itemView.setClickable(false);
        itemView.setFocusable(false);

        container.addView(itemView);
    }

    private void loadLessons(LayoutInflater inflater, SessionManager sessionManager) {
        String userId = String.valueOf(sessionManager.getUserId());

        quizResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot quizSnapshot) {

                exerciseResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot exerciseSnapshot) {

                        codingExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot codingExercisesSnapshot) {

                                lessonsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        Map<String, List<LessonData>> lessonsByDiff = new HashMap<>();
                                        int totalLessons = 0;
                                        int totalCompleted = 0;

                                        for (DataSnapshot lessonSnap : snapshot.getChildren()) {
                                            String lessonKey = lessonSnap.getKey();
                                            String titleHtml = lessonSnap.child("main_title").getValue(String.class);
                                            String descHtml = lessonSnap.child("title_desc").getValue(String.class);
                                            String difficulty = lessonSnap.child("difficulty").getValue(String.class);

                                            if (difficulty == null) difficulty = "beginner";
                                            if (titleHtml == null) titleHtml = lessonKey;
                                            if (descHtml == null) descHtml = "No description available";

                                            Spanned title = Html.fromHtml(titleHtml, Html.FROM_HTML_MODE_LEGACY);
                                            Spanned about = Html.fromHtml(descHtml, Html.FROM_HTML_MODE_LEGACY);

                                            String truncatedTitle = truncateText(title.toString(), 60);
                                            String truncatedAbout = truncateText(about.toString(), 120);

                                            // Check completion
                                            boolean isCompleted = false;

                                            // Quiz check
                                            if (quizSnapshot.child(lessonKey).exists()) {
                                                String passed = quizSnapshot.child(lessonKey).child("passed").getValue(String.class);
                                                isCompleted = passed != null && passed.equalsIgnoreCase("Passed");
                                            }

                                            // Coding exercise check
                                            boolean hasCodingExercises = codingExercisesSnapshot.child(lessonKey).exists()
                                                    && codingExercisesSnapshot.child(lessonKey).hasChildren();

                                            if (hasCodingExercises) {
                                                Boolean completed = exerciseSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
                                                if (completed == null || !completed) {
                                                    isCompleted = false;
                                                }
                                            }

                                            LessonData data = new LessonData();
                                            data.key = lessonKey;
                                            data.title = titleHtml; // Store full HTML for side panel
                                            data.desc = truncatedAbout;
                                            data.difficulty = difficulty;
                                            data.isCompleted = isCompleted;

                                            lessonsByDiff.computeIfAbsent(difficulty.toLowerCase(), k -> new ArrayList<>()).add(data);

                                            // Count totals
                                            totalLessons++;
                                            if (isCompleted) totalCompleted++;
                                        }

                                        // Store lessons for side panel
                                        lessonsByDifficultyMap = lessonsByDiff;

                                        // Update overall progress indicators
                                        updateOverallProgress(totalCompleted, totalLessons);

                                        // Calculate totals and completed by difficulty
                                        Map<String, Integer> totalByDiff = new HashMap<>();
                                        Map<String, Integer> completedByDiff = new HashMap<>();
                                        for (String diff : lessonsByDiff.keySet()) {
                                            List<LessonData> list = lessonsByDiff.get(diff);
                                            totalByDiff.put(diff, list.size());
                                            int comp = 0;
                                            for (LessonData d : list) if (d.isCompleted) comp++;
                                            completedByDiff.put(diff, comp);
                                        }

                                        // Check if all lessons in previous levels are completed
                                        int beginnerTotal = totalByDiff.getOrDefault("beginner", 0);
                                        int beginnerCompleted = completedByDiff.getOrDefault("beginner", 0);
                                        int intermediateTotal = totalByDiff.getOrDefault("intermediate", 0);
                                        int intermediateCompleted = completedByDiff.getOrDefault("intermediate", 0);

                                        boolean allBeginnerCompleted = beginnerTotal > 0 && beginnerCompleted == beginnerTotal;
                                        boolean allIntermediateCompleted = intermediateTotal > 0 && intermediateCompleted == intermediateTotal;

                                        // Show/hide intermediate cards based on beginner completion
                                        if (cardIntermediate != null && cardIntermediateLocked != null) {
                                            if (allBeginnerCompleted) {
                                                cardIntermediate.setVisibility(View.VISIBLE);
                                                cardIntermediateLocked.setVisibility(View.GONE);
                                            } else {
                                                cardIntermediate.setVisibility(View.GONE);
                                                cardIntermediateLocked.setVisibility(View.VISIBLE);
                                            }
                                        }

                                        // Show/hide advanced cards based on beginner AND intermediate completion
                                        if (cardAdvanced != null && cardAdvancedLocked != null) {
                                            if (allBeginnerCompleted && allIntermediateCompleted) {
                                                cardAdvanced.setVisibility(View.VISIBLE);
                                                cardAdvancedLocked.setVisibility(View.GONE);
                                            } else {
                                                cardAdvanced.setVisibility(View.GONE);
                                                cardAdvancedLocked.setVisibility(View.VISIBLE);
                                            }
                                        }

                                        // Populate sections
                                        populateSection("beginner", sectionBeginnerContent, beginnerProgress,
                                                lessonsByDiff.get("beginner"), true,
                                                totalByDiff.getOrDefault("beginner", 0),
                                                completedByDiff.getOrDefault("beginner", 0),
                                                inflater, sessionManager, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                                                lessonsByDiff);

                                        boolean intermediateUnlocked = allBeginnerCompleted;
                                        populateSection("intermediate", sectionIntermediateContent, intermediateProgress,
                                                lessonsByDiff.get("intermediate"), intermediateUnlocked,
                                                totalByDiff.getOrDefault("intermediate", 0),
                                                intermediateUnlocked ? completedByDiff.getOrDefault("intermediate", 0) : 0,
                                                inflater, sessionManager, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                                                lessonsByDiff);

                                        boolean advancedUnlocked = allBeginnerCompleted && allIntermediateCompleted;
                                        populateSection("advanced", sectionAdvancedContent, advancedProgress,
                                                lessonsByDiff.get("advanced"), advancedUnlocked,
                                                totalByDiff.getOrDefault("advanced", 0),
                                                advancedUnlocked ? completedByDiff.getOrDefault("advanced", 0) : 0,
                                                inflater, sessionManager, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                                                lessonsByDiff);

                                        if (!snapshot.hasChildren()) {
                                            Toast.makeText(requireContext(), "No lessons available.", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(requireContext(), "Failed to load lessons: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) { }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * Update the overall progress indicators
     */
    private void updateOverallProgress(int completed, int total) {
        if (total == 0) {
            // No lessons available
            progressCircle.setProgress(0);
            tvProgressPercent.setText("0%\nComplete");
            completedProgress.setText("0");
            remainingProgress.setText("0");
            totalProgress.setText("0");
            return;
        }

        int remaining = total - completed;
        int percentage = (int) ((completed * 100.0) / total);

        // Update progress circle
        progressCircle.setProgress(percentage);

        // Update percentage text
        tvProgressPercent.setText(percentage + "%\nComplete");

        // Update stats
        completedProgress.setText(String.valueOf(completed));
        remainingProgress.setText(String.valueOf(remaining));
        totalProgress.setText(String.valueOf(total));
    }

    private void populateSection(String diff, LinearLayout content, TextView progress, List<LessonData> lessons,
                                 boolean unlocked, int total, int completed, LayoutInflater inflater,
                                 SessionManager sessionManager, DataSnapshot quizSnapshot,
                                 DataSnapshot exerciseSnapshot, DataSnapshot codingExercisesSnapshot,
                                 Map<String, List<LessonData>> lessonsByDiff) {
        content.removeAllViews();
        progress.setText(completed + "/" + total);

        if (!unlocked) {
            // Don't populate content if section is locked
            return;
        }

        if (lessons == null) return;

        for (LessonData data : lessons) {
            boolean isLocked = shouldLockLesson(quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                    data.key, diff, lessonsByDiff);
            View lessonView = isLocked ? inflater.inflate(R.layout.lesson_locked, content, false)
                    : inflater.inflate(R.layout.lesson_unlocked, content, false);

            if (!isLocked) {
                MaterialCardView card = lessonView.findViewById(R.id.lesson1);
                TextView lTitle = lessonView.findViewById(R.id.l_title);
                TextView lAbout = lessonView.findViewById(R.id.l_about);
                ImageView diffBadge = lessonView.findViewById(R.id.difficulty);
                ImageView lProgress = lessonView.findViewById(R.id.l_progress);

                String truncatedTitle = truncateText(Html.fromHtml(data.title, Html.FROM_HTML_MODE_LEGACY).toString(), 60);
                lTitle.setText(Html.fromHtml(truncatedTitle, Html.FROM_HTML_MODE_LEGACY));
                lAbout.setText(Html.fromHtml(data.desc, Html.FROM_HTML_MODE_LEGACY));

                switch (diff.toLowerCase()) {
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

                if (card != null) {
                    card.setOnClickListener(v -> {
                        sessionManager.saveSelectedLesson(data.key);
                        Intent intent = new Intent(requireContext(), LessonActivity.class);
                        intent.putExtra("lessonId", data.key);
                        startActivity(intent);
                    });
                }

                if (data.isCompleted) {
                    lProgress.setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC_IN);
                } else {
                    lProgress.setColorFilter(getResources().getColor(R.color.gray), PorterDuff.Mode.SRC_IN);
                }
            }

            content.addView(lessonView);
        }
    }

    /**
     * Determines if a lesson should be locked based on previous lesson completion
     */
    private boolean shouldLockLesson(
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot codingExercisesSnapshot,
            String lessonId,
            String currentDifficulty,
            Map<String, List<LessonData>> lessonsByDiff
    ) {
        // L1 is always unlocked
        if (lessonId.equals("L1")) return false;

        // Check if this is the first lesson of current difficulty level
        List<LessonData> currentLevelLessons = lessonsByDiff.get(currentDifficulty.toLowerCase());
        boolean isFirstInLevel = false;
        if (currentLevelLessons != null && !currentLevelLessons.isEmpty()) {
            isFirstInLevel = currentLevelLessons.get(0).key.equals(lessonId);
        }

        // If it's the first lesson in intermediate or advanced, check if previous level is completed
        if (isFirstInLevel) {
            if (currentDifficulty.equalsIgnoreCase("intermediate")) {
                // Check if all beginner lessons are completed
                if (areAllLessonsCompleted(lessonsByDiff.get("beginner"), quizSnapshot, exerciseSnapshot, codingExercisesSnapshot)) {
                    return false; // Unlock intermediate
                }
            } else if (currentDifficulty.equalsIgnoreCase("advanced")) {
                // Check if all beginner AND intermediate lessons are completed
                boolean beginnerComplete = areAllLessonsCompleted(lessonsByDiff.get("beginner"), quizSnapshot, exerciseSnapshot, codingExercisesSnapshot);
                boolean intermediateComplete = areAllLessonsCompleted(lessonsByDiff.get("intermediate"), quizSnapshot, exerciseSnapshot, codingExercisesSnapshot);
                if (beginnerComplete && intermediateComplete) {
                    return false; // Unlock advanced
                }
            }
        }

        // Normal sequential check: check previous lesson by number
        String previousLessonId = getPreviousLessonId(lessonId);
        if (previousLessonId == null) return true;

        // Check if quiz is passed
        boolean passedQuiz = false;
        if (quizSnapshot.child(previousLessonId).exists()) {
            String passed = quizSnapshot.child(previousLessonId).child("passed").getValue(String.class);
            passedQuiz = passed != null && passed.equalsIgnoreCase("Passed");
        }

        // If quiz is not passed, lock the lesson
        if (!passedQuiz) return true;

        // Check if previous lesson has coding exercises
        boolean hasCodingExercises = codingExercisesSnapshot.child(previousLessonId).exists()
                && codingExercisesSnapshot.child(previousLessonId).hasChildren();

        // If previous lesson has coding exercises, check if they're completed
        if (hasCodingExercises) {
            if (!exerciseSnapshot.child(previousLessonId).exists()) {
                // Exercise results don't exist yet, so not completed
                return true;
            }

            Boolean completed = exerciseSnapshot.child(previousLessonId).child("completed").getValue(Boolean.class);
            boolean exerciseCompleted = completed != null && completed;

            // Lock if exercise exists but not completed
            if (!exerciseCompleted) return true;
        }

        // Unlock: quiz passed AND (no exercise OR exercise completed)
        return false;
    }

    /**
     * Check if all lessons in a difficulty level are completed
     */
    private boolean areAllLessonsCompleted(
            List<LessonData> lessons,
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot codingExercisesSnapshot
    ) {
        if (lessons == null || lessons.isEmpty()) return false;

        for (LessonData lesson : lessons) {
            // Check quiz
            boolean passedQuiz = false;
            if (quizSnapshot.child(lesson.key).exists()) {
                String passed = quizSnapshot.child(lesson.key).child("passed").getValue(String.class);
                passedQuiz = passed != null && passed.equalsIgnoreCase("Passed");
            }

            if (!passedQuiz) return false;

            // Check coding exercises if they exist
            boolean hasCodingExercises = codingExercisesSnapshot.child(lesson.key).exists()
                    && codingExercisesSnapshot.child(lesson.key).hasChildren();

            if (hasCodingExercises) {
                Boolean completed = exerciseSnapshot.child(lesson.key).child("completed").getValue(Boolean.class);
                if (completed == null || !completed) {
                    return false;
                }
            }
        }

        return true;
    }

    private String getPreviousLessonId(String currentLessonId) {
        try {
            int num = Integer.parseInt(currentLessonId.replaceAll("[^0-9]", ""));
            if (num > 1) return "L" + (num - 1);
        } catch (NumberFormatException ignored) { }
        return null;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3).trim() + "...";
        }
        return text;
    }

    private void toggleSectionBeginner() {
        if (isBeginnerVisible) {
            sectionBeginnerContent.setVisibility(View.GONE);
            rotateArrow(iconArrow, 180, 0);
        } else {
            sectionBeginnerContent.setVisibility(View.VISIBLE);
            rotateArrow(iconArrow, 0, 180);
        }
        isBeginnerVisible = !isBeginnerVisible;
    }

    private void toggleSectionIntermediate() {
        if (isIntermediateVisible) {
            sectionIntermediateContent.setVisibility(View.GONE);
            rotateArrow(iconArrowIntermediate, 180, 0);
        } else {
            sectionIntermediateContent.setVisibility(View.VISIBLE);
            rotateArrow(iconArrowIntermediate, 0, 180);
        }
        isIntermediateVisible = !isIntermediateVisible;
    }

    private void toggleSectionAdvanced() {
        if (isAdvancedVisible) {
            sectionAdvancedContent.setVisibility(View.GONE);
            rotateArrow(iconArrowAdvanced, 180, 0);
        } else {
            sectionAdvancedContent.setVisibility(View.VISIBLE);
            rotateArrow(iconArrowAdvanced, 0, 180);
        }
        isAdvancedVisible = !isAdvancedVisible;
    }

    private void rotateArrow(ImageView arrow, float from, float to) {
        RotateAnimation rotate = new RotateAnimation(
                from, to,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        arrow.startAnimation(rotate);
    }

    private void updateClassificationBadge(String classification) {
        if (classification == null) classification = "notClassified";

        switch (classification.toLowerCase()) {
            case "beginner":
                tvLevel.setBackgroundResource(R.drawable.beginner_classifier);
                break;
            case "intermediate":
                tvLevel.setBackgroundResource(R.drawable.intermediate_classifier);
                break;
            case "advanced":
                tvLevel.setBackgroundResource(R.drawable.advanced_classifier);
                break;
            default:
                tvLevel.setBackgroundResource(R.drawable.none_classifier);
                break;
        }
    }
}