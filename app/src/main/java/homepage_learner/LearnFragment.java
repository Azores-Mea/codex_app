package homepage_learner;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private DatabaseReference usersRef;
    private DatabaseReference syntaxErrorResultsRef;
    private DatabaseReference tracingResultsRef;
    private DatabaseReference machineProblemResultsRef;
    private DatabaseReference assessmentRef;

    // Store lesson data for side panel
    private Map<String, List<LessonData>> lessonsByDifficultyMap = new HashMap<>();
    // Store lesson data for side panel

    // Add this new flag
    private boolean isLoadingLessons = false;

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
        usersRef = FirebaseDatabase.getInstance().getReference("Users"); // NEW: Initialize Users reference

        syntaxErrorResultsRef = FirebaseDatabase.getInstance().getReference("syntaxErrorResults");
        tracingResultsRef = FirebaseDatabase.getInstance().getReference("tracingResults");
        machineProblemResultsRef = FirebaseDatabase.getInstance().getReference("machineProblemResults");
        assessmentRef = FirebaseDatabase.getInstance().getReference("assessment");

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

// Listen for quiz completion changes - CHANGED to single value event
        quizResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadLessons(inflater, sessionManager);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

// Listen for exercise completion changes - CHANGED to single value event
        exerciseResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadLessons(inflater, sessionManager);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        syntaxErrorResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadLessons(inflater, sessionManager);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

// Listen for tracing completion changes - CHANGED to single value event
        tracingResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadLessons(inflater, sessionManager);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

// REMOVE the unlockedLessons listener completely

        loadLessons(inflater, sessionManager);

        return view;
    }

    /**
     * NEW: Get the classification level as a numeric value
     * 0 = not classified, 1 = beginner, 2 = intermediate, 3 = advanced
     */
    private int getClassificationLevel(String classification) {
        if (classification == null) return 0;
        switch (classification.toLowerCase()) {
            case "beginner":
                return 1;
            case "intermediate":
                return 2;
            case "advanced":
                return 3;
            default:
                return 0;
        }
    }

    /**
     * NEW: Update user classification in Firebase
     */
    private void updateUserClassification(String newClassification, SessionManager sessionManager) {
        String userId = String.valueOf(sessionManager.getUserId());

        usersRef.child(userId).child("classification").setValue(newClassification)
                .addOnSuccessListener(aVoid -> {
                    // Update session manager
                    sessionManager.setClassification(newClassification);

                    // Update UI badge
                    updateClassificationBadge(newClassification);

                    Log.d("LearnFragment", "Classification updated to: " + newClassification);
                })
                .addOnFailureListener(e -> {
                    Log.e("LearnFragment", "Failed to update classification: " + e.getMessage());
                });
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
        // Prevent multiple simultaneous loads
        if (isLoadingLessons) {
            Log.d("LearnFragment", "Already loading lessons, skipping...");
            return;
        }

        isLoadingLessons = true;
        String userId = String.valueOf(sessionManager.getUserId());

        quizResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot quizSnapshot) {

                exerciseResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot exerciseSnapshot) {

                        // NEW: Load syntax error results
                        syntaxErrorResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot syntaxErrorSnapshot) {

                                // NEW: Load tracing results
                                tracingResultsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot tracingSnapshot) {

                                        codingExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot codingExercisesSnapshot) {

                                                // NEW: Load assessment data to check which assessments exist
                                                assessmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot assessmentSnapshot) {

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

                                                                    // Check completion - NOW INCLUDING ALL ASSESSMENTS
                                                                    boolean isCompleted = checkLessonCompletionSync(
                                                                            lessonKey,
                                                                            quizSnapshot,
                                                                            exerciseSnapshot,
                                                                            codingExercisesSnapshot,
                                                                            syntaxErrorSnapshot,
                                                                            tracingSnapshot,
                                                                            assessmentSnapshot
                                                                    );

                                                                    LessonData data = new LessonData();
                                                                    data.key = lessonKey;
                                                                    data.title = titleHtml;
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

                                                                // Get user's initial classification level
                                                                String userClassification = sessionManager.getClassification();
                                                                int userClassLevel = getClassificationLevel(userClassification);

                                                                // Check if classification should be upgraded
                                                                if (allBeginnerCompleted && userClassLevel < 2) {
                                                                    updateUserClassification("Intermediate", sessionManager);
                                                                    userClassLevel = 2;
                                                                }

                                                                if (allBeginnerCompleted && allIntermediateCompleted && userClassLevel < 3) {
                                                                    updateUserClassification("Advanced", sessionManager);
                                                                    userClassLevel = 3;
                                                                }

                                                                // Show/hide intermediate cards
                                                                if (cardIntermediate != null && cardIntermediateLocked != null) {
                                                                    if (allBeginnerCompleted || userClassLevel >= 2) {
                                                                        cardIntermediate.setVisibility(View.VISIBLE);
                                                                        cardIntermediateLocked.setVisibility(View.GONE);
                                                                    } else {
                                                                        cardIntermediate.setVisibility(View.GONE);
                                                                        cardIntermediateLocked.setVisibility(View.VISIBLE);
                                                                    }
                                                                }

                                                                // Show/hide advanced cards
                                                                if (cardAdvanced != null && cardAdvancedLocked != null) {
                                                                    if ((allBeginnerCompleted && allIntermediateCompleted) || userClassLevel >= 3) {
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
                                                                        lessonsByDiff, userClassLevel);

                                                                boolean intermediateUnlocked = allBeginnerCompleted || userClassLevel >= 2;
                                                                populateSection("intermediate", sectionIntermediateContent, intermediateProgress,
                                                                        lessonsByDiff.get("intermediate"), intermediateUnlocked,
                                                                        totalByDiff.getOrDefault("intermediate", 0),
                                                                        intermediateUnlocked ? completedByDiff.getOrDefault("intermediate", 0) : 0,
                                                                        inflater, sessionManager, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                                                                        lessonsByDiff, userClassLevel);

                                                                boolean advancedUnlocked = (allBeginnerCompleted && allIntermediateCompleted) || userClassLevel >= 3;
                                                                populateSection("advanced", sectionAdvancedContent, advancedProgress,
                                                                        lessonsByDiff.get("advanced"), advancedUnlocked,
                                                                        totalByDiff.getOrDefault("advanced", 0),
                                                                        advancedUnlocked ? completedByDiff.getOrDefault("advanced", 0) : 0,
                                                                        inflater, sessionManager, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                                                                        lessonsByDiff, userClassLevel);

                                                                if (!snapshot.hasChildren()) {
                                                                    Toast.makeText(requireContext(), "No lessons available.", Toast.LENGTH_SHORT).show();
                                                                }

                                                                // Reset loading flag
                                                                isLoadingLessons = false;
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                                Toast.makeText(requireContext(), "Failed to load lessons: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                                // Reset loading flag on error too
                                                                isLoadingLessons = false;
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

    private boolean checkLessonCompletionSync(
            String lessonKey,
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot codingExercisesSnapshot,
            DataSnapshot syntaxErrorSnapshot,
            DataSnapshot tracingSnapshot,
            DataSnapshot assessmentSnapshot
    ) {
        // 1. Check if quiz is passed
        boolean passedQuiz = false;
        if (quizSnapshot.child(lessonKey).exists()) {
            String passed = quizSnapshot.child(lessonKey).child("passed").getValue(String.class);
            passedQuiz = passed != null && passed.equalsIgnoreCase("Passed");
        }

        if (!passedQuiz) return false;

        // 2. Check coding exercises if they exist
        boolean hasCodingExercises = codingExercisesSnapshot.child(lessonKey).exists()
                && codingExercisesSnapshot.child(lessonKey).hasChildren();

        if (hasCodingExercises) {
            if (!exerciseSnapshot.child(lessonKey).exists()) {
                return false;
            }

            Boolean completed = exerciseSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
            if (completed == null || !completed) {
                return false;
            }
        }

        // 3. Check syntax error assessment if it exists
        boolean hasSyntaxError = assessmentSnapshot.child(lessonKey).child("FindingSyntaxError").exists();
        if (hasSyntaxError) {
            if (!syntaxErrorSnapshot.child(lessonKey).exists()) {
                return false;
            }

            Boolean syntaxCompleted = syntaxErrorSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
            if (syntaxCompleted == null || !syntaxCompleted) {
                return false;
            }
        }

        // 4. Check program tracing assessment if it exists
        boolean hasTracing = assessmentSnapshot.child(lessonKey).child("ProgramTracing").exists();
        if (hasTracing) {
            if (!tracingSnapshot.child(lessonKey).exists()) {
                return false;
            }

            Boolean tracingCompleted = tracingSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
            if (tracingCompleted == null || !tracingCompleted) {
                return false;
            }
        }

        // All required assessments are completed
        return true;
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
                                 Map<String, List<LessonData>> lessonsByDiff, int userClassLevel) {
        content.removeAllViews();
        progress.setText(completed + "/" + total);

        if (!unlocked) {
            // Don't populate content if section is locked
            return;
        }

        if (lessons == null) return;

        // Process each lesson asynchronously
        for (LessonData data : lessons) {
            // Check lock status asynchronously
            shouldLockLesson(quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                    data.key, diff, lessonsByDiff, userClassLevel, isLocked -> {

                        // Inflate the correct layout based on lock status
                        View finalLessonView = isLocked ?
                                inflater.inflate(R.layout.lesson_locked, content, false) :
                                inflater.inflate(R.layout.lesson_unlocked, content, false);

                        if (!isLocked) {
                            MaterialCardView card = finalLessonView.findViewById(R.id.lesson1);
                            TextView lTitle = finalLessonView.findViewById(R.id.l_title);
                            TextView lAbout = finalLessonView.findViewById(R.id.l_about);
                            ImageView diffBadge = finalLessonView.findViewById(R.id.difficulty);
                            ImageView lProgress = finalLessonView.findViewById(R.id.l_progress);

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

                            // FIXED: Use data.isCompleted instead of rechecking
                            if (data.isCompleted) {
                                lProgress.setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC_IN);
                            } else {
                                lProgress.setColorFilter(getResources().getColor(R.color.gray), PorterDuff.Mode.SRC_IN);
                            }
                        }

                        // Add the final view to content at the correct position
                        // Find the correct index to maintain lesson order
                        int targetIndex = 0;
                        for (int i = 0; i < lessons.size(); i++) {
                            if (lessons.get(i).key.equals(data.key)) {
                                targetIndex = i;
                                break;
                            }
                        }

                        // Insert at the correct position, or append if position exceeds current size
                        if (targetIndex < content.getChildCount()) {
                            content.addView(finalLessonView, targetIndex);
                        } else {
                            content.addView(finalLessonView);
                        }
                    });
        }
    }

    /**
     * UPDATED: Determines if a lesson should be locked based on previous lesson completion AND user classification
     */
    private void shouldLockLesson(
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot codingExercisesSnapshot,
            String lessonId,
            String currentDifficulty,
            Map<String, List<LessonData>> lessonsByDiff,
            int userClassLevel,
            OnLockStatusChecked callback
    ) {
        // L1 is always unlocked
        if (lessonId.equals("L1")) {
            callback.onChecked(false);
            return;
        }

        // NEW: Check if lesson is explicitly unlocked in Firebase
        String userId = String.valueOf(new SessionManager(requireContext()).getUserId());
        DatabaseReference unlockedRef = FirebaseDatabase.getInstance().getReference("unlockedLessons");

        unlockedRef.child(userId).child(lessonId).get().addOnSuccessListener(unlockedSnapshot -> {
            if (unlockedSnapshot.exists() && Boolean.TRUE.equals(unlockedSnapshot.child("unlocked").getValue(Boolean.class))) {
                // Lesson is explicitly unlocked
                callback.onChecked(false);
                return;
            }

            // Continue with existing logic if not explicitly unlocked
            performLockCheck(quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                    lessonId, currentDifficulty, lessonsByDiff, userClassLevel, callback);
        }).addOnFailureListener(e -> {
            // If Firebase check fails, continue with existing logic
            performLockCheck(quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                    lessonId, currentDifficulty, lessonsByDiff, userClassLevel, callback);
        });
    }
    /**
     * Helper method containing the original lock checking logic
     */
    private void performLockCheck(
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot codingExercisesSnapshot,
            String lessonId,
            String currentDifficulty,
            Map<String, List<LessonData>> lessonsByDiff,
            int userClassLevel,
            OnLockStatusChecked callback
    ) {
        // Check if this is the first lesson of current difficulty level
        List<LessonData> currentLevelLessons = lessonsByDiff.get(currentDifficulty.toLowerCase());
        boolean isFirstInLevel = false;
        int lessonIndexInLevel = -1;

        if (currentLevelLessons != null && !currentLevelLessons.isEmpty()) {
            for (int i = 0; i < currentLevelLessons.size(); i++) {
                if (currentLevelLessons.get(i).key.equals(lessonId)) {
                    lessonIndexInLevel = i;
                    isFirstInLevel = (i == 0);
                    break;
                }
            }
        }

        // Auto-unlock logic based on user classification
        if (currentDifficulty.equalsIgnoreCase("beginner")) {
            if (userClassLevel >= 2) {
                callback.onChecked(false); // Unlock all beginner lessons
                return;
            }
        } else if (currentDifficulty.equalsIgnoreCase("intermediate")) {
            if (userClassLevel >= 2) {
                if (isFirstInLevel) {
                    callback.onChecked(false); // Unlock first intermediate lesson
                    return;
                }
                if (lessonIndexInLevel > 0) {
                    LessonData previousLesson = currentLevelLessons.get(lessonIndexInLevel - 1);
                    isLessonCompleted(previousLesson.key, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                            isCompleted -> callback.onChecked(!isCompleted));
                    return;
                }
            } else {
                if (isFirstInLevel) {
                    areAllLessonsCompleted(lessonsByDiff.get("beginner"), quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                            allCompleted -> callback.onChecked(!allCompleted));
                    return;
                }
            }
        } else if (currentDifficulty.equalsIgnoreCase("advanced")) {
            if (userClassLevel >= 3) {
                if (isFirstInLevel) {
                    callback.onChecked(false); // Unlock first advanced lesson
                    return;
                }
                if (lessonIndexInLevel > 0) {
                    LessonData previousLesson = currentLevelLessons.get(lessonIndexInLevel - 1);
                    isLessonCompleted(previousLesson.key, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                            isCompleted -> callback.onChecked(!isCompleted));
                    return;
                }
            } else {
                if (isFirstInLevel) {
                    areAllLessonsCompleted(lessonsByDiff.get("beginner"), quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                            beginnerComplete -> {
                                if (!beginnerComplete) {
                                    callback.onChecked(true);
                                    return;
                                }
                                areAllLessonsCompleted(lessonsByDiff.get("intermediate"), quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                                        intermediateComplete -> callback.onChecked(!intermediateComplete));
                            });
                    return;
                }
            }
        }

        // Normal sequential check: check previous lesson by number
        String previousLessonId = getPreviousLessonId(lessonId);
        if (previousLessonId == null) {
            callback.onChecked(true);
            return;
        }

        // Check if previous lesson is completed
        isLessonCompleted(previousLessonId, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot,
                isCompleted -> callback.onChecked(!isCompleted));
    }
    private interface OnLockStatusChecked {
        void onChecked(boolean isLocked);
    }

    /**
     * NEW: Helper method to check if a single lesson is completed
     */
    /**
     * UPDATED: Check if a lesson is completed (quiz passed AND all required assessments completed, excluding machine problem)
     */
    private void isLessonCompleted(
            String lessonId,
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot codingExercisesSnapshot,
            OnLessonCompletionChecked callback
    ) {
        // Check if quiz is passed
        boolean passedQuiz = false;
        if (quizSnapshot.child(lessonId).exists()) {
            String passed = quizSnapshot.child(lessonId).child("passed").getValue(String.class);
            passedQuiz = passed != null && passed.equalsIgnoreCase("Passed");
        }

        // If quiz is not passed, lesson is not completed
        if (!passedQuiz) {
            callback.onChecked(false);
            return;
        }

        // Check if lesson has coding exercises
        boolean hasCodingExercises = codingExercisesSnapshot.child(lessonId).exists()
                && codingExercisesSnapshot.child(lessonId).hasChildren();

        // If lesson has coding exercises, check if they're completed
        if (hasCodingExercises) {
            if (!exerciseSnapshot.child(lessonId).exists()) {
                callback.onChecked(false);
                return;
            }

            Boolean completed = exerciseSnapshot.child(lessonId).child("completed").getValue(Boolean.class);
            boolean exerciseCompleted = completed != null && completed;

            if (!exerciseCompleted) {
                callback.onChecked(false);
                return;
            }
        }

        // NEW: Check all other assessments (excluding machine problem)
        String userId = String.valueOf(new SessionManager(requireContext()).getUserId());

        assessmentRef.child(lessonId).get().addOnSuccessListener(assessmentSnapshot -> {
            boolean hasSyntaxError = assessmentSnapshot.child("FindingSyntaxError").exists();
            boolean hasTracing = assessmentSnapshot.child("ProgramTracing").exists();
            // Note: We're NOT checking MachineProblem

            // Count how many assessments we need to check
            int totalChecks = 0;
            if (hasSyntaxError) totalChecks++;
            if (hasTracing) totalChecks++;

            if (totalChecks == 0) {
                // No additional assessments, lesson is completed
                callback.onChecked(true);
                return;
            }

            // Use AtomicInteger and AtomicBoolean for thread-safe operations in lambdas
            final AtomicInteger checksCompleted = new AtomicInteger(0);
            final AtomicBoolean allPassed = new AtomicBoolean(true);
            final int finalTotalChecks = totalChecks;

            if (hasSyntaxError) {
                syntaxErrorResultsRef.child(userId).child(lessonId).get().addOnSuccessListener(syntaxSnapshot -> {
                    if (!syntaxSnapshot.exists() ||
                            !Boolean.TRUE.equals(syntaxSnapshot.child("completed").getValue(Boolean.class))) {
                        allPassed.set(false);
                    }

                    if (checksCompleted.incrementAndGet() == finalTotalChecks) {
                        callback.onChecked(allPassed.get());
                    }
                }).addOnFailureListener(e -> {
                    allPassed.set(false);
                    if (checksCompleted.incrementAndGet() == finalTotalChecks) {
                        callback.onChecked(false);
                    }
                });
            }

            if (hasTracing) {
                tracingResultsRef.child(userId).child(lessonId).get().addOnSuccessListener(tracingSnapshot -> {
                    if (!tracingSnapshot.exists() ||
                            !Boolean.TRUE.equals(tracingSnapshot.child("completed").getValue(Boolean.class))) {
                        allPassed.set(false);
                    }

                    if (checksCompleted.incrementAndGet() == finalTotalChecks) {
                        callback.onChecked(allPassed.get());
                    }
                }).addOnFailureListener(e -> {
                    allPassed.set(false);
                    if (checksCompleted.incrementAndGet() == finalTotalChecks) {
                        callback.onChecked(false);
                    }
                });
            }
        }).addOnFailureListener(e -> {
            // If we can't check assessments, fall back to just quiz + exercises
            callback.onChecked(true);
        });
    }

    /**
     * Check if all lessons in a difficulty level are completed
     */
    private void areAllLessonsCompleted(
            List<LessonData> lessons,
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot codingExercisesSnapshot,
            OnAllLessonsCompletionChecked callback
    ) {
        if (lessons == null || lessons.isEmpty()) {
            callback.onChecked(false);
            return;
        }

        final AtomicInteger checksCompleted = new AtomicInteger(0);
        final AtomicBoolean allCompleted = new AtomicBoolean(true);
        final int totalLessons = lessons.size();

        for (LessonData lesson : lessons) {
            isLessonCompleted(lesson.key, quizSnapshot, exerciseSnapshot, codingExercisesSnapshot, isCompleted -> {
                if (!isCompleted) {
                    allCompleted.set(false);
                }

                if (checksCompleted.incrementAndGet() == totalLessons) {
                    callback.onChecked(allCompleted.get());
                }
            });
        }
    }

    // Add callback interface
    private interface OnAllLessonsCompletionChecked {
        void onChecked(boolean allCompleted);
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
    private interface OnLessonCompletionChecked {
        void onChecked(boolean isCompleted);
    }
}