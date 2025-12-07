package homepage_learner;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.codex.GradientTextUtil;
import com.example.codex.MainActivity;
import com.example.codex.R;
import com.example.codex.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressFragment extends Fragment {

    // UI Components
    private TextView initialTestScore;
    private TextView currentClass;
    private TextView lessonComplete;
    private TextView beginnerProgress;
    private TextView intermediateProgress;
    private TextView advancedProgress;
    private ProgressBar progressBarBeginner;
    private ProgressBar progressBarIntermediate;
    private ProgressBar progressBarAdvanced;
    private ProgressBar progressCircle;
    private TextView progressPercent;
    private TextView assessment_progress;
    private ConstraintLayout noProgressLayout;
    private ConstraintLayout withProgressLayout;

    // Header Components
    private TextView userName;
    private TextView userClass;
    private ImageView logout;

    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference userRef;
    private SessionManager sessionManager;
    private int userId;
    private ValueEventListener userListener;

    // View holder
    private View rootView;
    private String cachedProgressData = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_progress, container, false);

        sessionManager = new SessionManager(requireContext());
        databaseReference = FirebaseDatabase.getInstance().getReference();
        userId = sessionManager.getUserId();

        initializeViews();
        setupHeader();
        loadUserProgress();

        return rootView;
    }

    public void onFragmentVisible() {
        Log.d("ProgressFragment", "Fragment is now visible - checking for changes");
        checkAndReloadIfNeeded();
    }

    private void checkAndReloadIfNeeded() {
        databaseReference.child("quizResults").child(String.valueOf(userId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String currentHash = generateDataHash(snapshot);

                        if (!currentHash.equals(cachedProgressData)) {
                            cachedProgressData = currentHash;
                            loadUserProgress();
                        } else {
                            Log.d("ProgressFragment", "No progress changes detected");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String generateDataHash(DataSnapshot snapshot) {
        StringBuilder hash = new StringBuilder();
        hash.append(snapshot.getChildrenCount());
        for (DataSnapshot child : snapshot.getChildren()) {
            hash.append(child.getKey());
            if (child.hasChild("passed")) {
                hash.append(child.child("passed").getValue());
            }
        }
        return hash.toString();
    }

    private void initializeViews() {
        userName = rootView.findViewById(R.id.user_name);
        userClass = rootView.findViewById(R.id.user_class);
        logout = rootView.findViewById(R.id.logout);

        initialTestScore = rootView.findViewById(R.id.initialTestScore);
        currentClass = rootView.findViewById(R.id.current_class);
        lessonComplete = rootView.findViewById(R.id.lesson_complete);

        beginnerProgress = rootView.findViewById(R.id.beginnerProgress);
        intermediateProgress = rootView.findViewById(R.id.intermediateProgress);
        advancedProgress = rootView.findViewById(R.id.advancedProgress);
        progressBarBeginner = rootView.findViewById(R.id.progressBarBeginner);
        progressBarIntermediate = rootView.findViewById(R.id.progressBarIntermediate);
        progressBarAdvanced = rootView.findViewById(R.id.progressBarAdvanced);

        progressCircle = rootView.findViewById(R.id.progressCircle);
        progressPercent = rootView.findViewById(R.id.progressPercent);
        assessment_progress = rootView.findViewById(R.id.assessment_progress);

        noProgressLayout = rootView.findViewById(R.id.no_progress);
        withProgressLayout = rootView.findViewById(R.id.with_progress);
    }

    private void setupHeader() {
        logout.setOnClickListener(v -> showLogoutDialog());

        if (userId != -1) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(String.valueOf(userId));
            attachUserListener();
        }
    }

    private void attachUserListener() {
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

    private void loadUserProgress() {
        loadInitialTestData();
        loadLessonsProgress();
    }

    private void loadInitialTestData() {
        databaseReference.child("quizResults")
                .orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot quizSnapshot : snapshot.getChildren()) {
                                String quizType = quizSnapshot.child("quizType").getValue(String.class);

                                if ("InitialTest".equals(quizType)) {
                                    Long score = quizSnapshot.child("score").getValue(Long.class);
                                    Long total = quizSnapshot.child("total").getValue(Long.class);
                                    String classification = quizSnapshot.child("classification").getValue(String.class);

                                    if (score != null && total != null) {
                                        initialTestScore.setText(score + "/" + total);
                                    }

                                    if (classification != null) {
                                        currentClass.setText(classification);
                                    }

                                    showProgressLayout();
                                    break;
                                }
                            }
                        } else {
                            showNoProgressLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showNoProgressLayout();
                    }
                });
    }

    private void loadLessonsProgress() {
        // Load all lessons first
        databaseReference.child("Lessons").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot lessonsSnapshot) {
                int totalLessons = (int) lessonsSnapshot.getChildrenCount();

                // Now check completion for each lesson
                checkAllLessonsCompletion(lessonsSnapshot, totalLessons);
                calculateAssessmentBasedProgress();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                lessonComplete.setText("0/30");
                updateModuleProgress(0, 10, 0, 10, 0, 10);
            }
        });
    }

    private void checkAllLessonsCompletion(DataSnapshot lessonsSnapshot, int totalLessons) {
        String userIdStr = String.valueOf(userId);

        // Load all required data first
        databaseReference.child("quizResults").child(userIdStr)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot quizSnapshot) {

                        databaseReference.child("exerciseResults").child(userIdStr)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot exerciseSnapshot) {

                                        databaseReference.child("syntaxErrorResults").child(userIdStr)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot syntaxSnapshot) {

                                                        databaseReference.child("tracingResults").child(userIdStr)
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot tracingSnapshot) {

                                                                        databaseReference.child("assessment")
                                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(@NonNull DataSnapshot assessmentSnapshot) {

                                                                                        databaseReference.child("coding_exercises")
                                                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                                    @Override
                                                                                                    public void onDataChange(@NonNull DataSnapshot codingSnapshot) {

                                                                                                        // Now count completed lessons
                                                                                                        countCompletedLessons(
                                                                                                                lessonsSnapshot,
                                                                                                                quizSnapshot,
                                                                                                                exerciseSnapshot,
                                                                                                                syntaxSnapshot,
                                                                                                                tracingSnapshot,
                                                                                                                assessmentSnapshot,
                                                                                                                codingSnapshot,
                                                                                                                totalLessons
                                                                                                        );
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

    private void countCompletedLessons(
            DataSnapshot lessonsSnapshot,
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot syntaxSnapshot,
            DataSnapshot tracingSnapshot,
            DataSnapshot assessmentSnapshot,
            DataSnapshot codingSnapshot,
            int totalLessons
    ) {
        int totalCompleted = 0;
        int beginnerCompleted = 0;
        int intermediateCompleted = 0;
        int advancedCompleted = 0;

        for (DataSnapshot lessonSnap : lessonsSnapshot.getChildren()) {
            String lessonKey = lessonSnap.getKey();

            if (lessonKey != null && isLessonCompleted(
                    lessonKey,
                    quizSnapshot,
                    exerciseSnapshot,
                    syntaxSnapshot,
                    tracingSnapshot,
                    assessmentSnapshot,
                    codingSnapshot
            )) {
                totalCompleted++;

                // Categorize by difficulty
                if (lessonKey.startsWith("L") && lessonKey.length() > 1) {
                    try {
                        int lessonNum = Integer.parseInt(lessonKey.substring(1));

                        if (lessonNum >= 1 && lessonNum <= 8) {
                            beginnerCompleted++;
                        } else if (lessonNum >= 9 && lessonNum <= 19) {
                            intermediateCompleted++;
                        } else if (lessonNum >= 20 && lessonNum <= 23) {
                            advancedCompleted++;
                        }
                    } catch (NumberFormatException e) {
                        Log.e("ProgressFragment", "Invalid lesson number: " + lessonKey);
                    }
                }
            }
        }

        // Update UI
        lessonComplete.setText(totalCompleted + "/" + totalLessons);
        updateModuleProgress(beginnerCompleted, 8, intermediateCompleted, 11, advancedCompleted, 4);
    }

    /**
     * Check if a lesson is fully completed (all required assessments)
     */
    private boolean isLessonCompleted(
            String lessonKey,
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot syntaxSnapshot,
            DataSnapshot tracingSnapshot,
            DataSnapshot assessmentSnapshot,
            DataSnapshot codingSnapshot
    ) {
        // 1. Check if quiz is passed
        boolean passedQuiz = false;
        if (quizSnapshot.child(lessonKey).exists()) {
            String passed = quizSnapshot.child(lessonKey).child("passed").getValue(String.class);
            passedQuiz = passed != null && passed.equalsIgnoreCase("Passed");
        }

        if (!passedQuiz) return false;

        // 2. Check coding exercises if they exist
        boolean hasCodingExercises = codingSnapshot.child(lessonKey).exists()
                && codingSnapshot.child(lessonKey).hasChildren();

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
            if (!syntaxSnapshot.child(lessonKey).exists()) {
                return false;
            }

            Boolean syntaxCompleted = syntaxSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
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

        // NOTE: Machine Problem is NOT required for completion
        // All required assessments are completed
        return true;
    }


// Add this method to your ProgressFragment class

    /**
     * Calculate overall progress based on total assessments vs completed assessments
     */
    private void calculateAssessmentBasedProgress() {
        String userIdStr = String.valueOf(userId);

        // Load all required data
        databaseReference.child("assessment")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot assessmentSnapshot) {

                        databaseReference.child("quizResults").child(userIdStr)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot quizSnapshot) {

                                        databaseReference.child("exerciseResults").child(userIdStr)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot exerciseSnapshot) {

                                                        databaseReference.child("syntaxErrorResults").child(userIdStr)
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot syntaxSnapshot) {

                                                                        databaseReference.child("tracingResults").child(userIdStr)
                                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(@NonNull DataSnapshot tracingSnapshot) {

                                                                                        databaseReference.child("machineProblemResults").child(userIdStr)
                                                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                                    @Override
                                                                                                    public void onDataChange(@NonNull DataSnapshot machineSnapshot) {

                                                                                                        databaseReference.child("coding_exercises")
                                                                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                                                    @Override
                                                                                                                    public void onDataChange(@NonNull DataSnapshot codingSnapshot) {

                                                                                                                        // Now count all assessments
                                                                                                                        countAllAssessments(
                                                                                                                                assessmentSnapshot,
                                                                                                                                quizSnapshot,
                                                                                                                                exerciseSnapshot,
                                                                                                                                syntaxSnapshot,
                                                                                                                                tracingSnapshot,
                                                                                                                                machineSnapshot,
                                                                                                                                codingSnapshot
                                                                                                                        );
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

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /**
     * Count total assessments available and completed by the user
     */
    private void countAllAssessments(
            DataSnapshot assessmentSnapshot,
            DataSnapshot quizSnapshot,
            DataSnapshot exerciseSnapshot,
            DataSnapshot syntaxSnapshot,
            DataSnapshot tracingSnapshot,
            DataSnapshot machineSnapshot,
            DataSnapshot codingSnapshot
    ) {
        int totalAssessments = 0;
        int completedAssessments = 0;

        // Breakdown by type
        int totalQuizzes = 0, completedQuizzes = 0;
        int totalCoding = 0, completedCoding = 0;
        int totalSyntax = 0, completedSyntax = 0;
        int totalTracing = 0, completedTracing = 0;
        int totalMachine = 0, completedMachine = 0;

        // Iterate through each lesson in assessments
        for (DataSnapshot lessonSnap : assessmentSnapshot.getChildren()) {
            String lessonKey = lessonSnap.getKey();
            if (lessonKey == null) continue;

            // 1. Count Quiz (every lesson should have one)
            if (lessonSnap.hasChild("Quiz")) {
                totalQuizzes++;
                totalAssessments++;

                // Check if completed
                if (quizSnapshot.hasChild(lessonKey)) {
                    String passed = quizSnapshot.child(lessonKey).child("passed").getValue(String.class);
                    if (passed != null && passed.equalsIgnoreCase("Passed")) {
                        completedQuizzes++;
                        completedAssessments++;
                    }
                }
            }

            // 2. Count Coding Exercises (if exists)
            if (codingSnapshot.hasChild(lessonKey) && codingSnapshot.child(lessonKey).hasChildren()) {
                totalCoding++;
                totalAssessments++;

                // Check if completed
                if (exerciseSnapshot.hasChild(lessonKey)) {
                    Boolean completed = exerciseSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
                    if (completed != null && completed) {
                        completedCoding++;
                        completedAssessments++;
                    }
                }
            }

            // 3. Count Syntax Error (if exists)
            if (lessonSnap.hasChild("FindingSyntaxError")) {
                totalSyntax++;
                totalAssessments++;

                // Check if completed
                if (syntaxSnapshot.hasChild(lessonKey)) {
                    Boolean completed = syntaxSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
                    if (completed != null && completed) {
                        completedSyntax++;
                        completedAssessments++;
                    }
                }
            }

            // 4. Count Program Tracing (if exists)
            if (lessonSnap.hasChild("ProgramTracing")) {
                totalTracing++;
                totalAssessments++;

                // Check if completed
                if (tracingSnapshot.hasChild(lessonKey)) {
                    Boolean completed = tracingSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
                    if (completed != null && completed) {
                        completedTracing++;
                        completedAssessments++;
                    }
                }
            }

            // 5. Count Machine Problem (if exists)
            if (lessonSnap.hasChild("MachineProblem")) {
                totalMachine++;
                totalAssessments++;

                // Check if completed
                if (machineSnapshot.hasChild(lessonKey)) {
                    Boolean completed = machineSnapshot.child(lessonKey).child("completed").getValue(Boolean.class);
                    if (completed != null && completed) {
                        completedMachine++;
                        completedAssessments++;
                    }
                }
            }
        }

        // Log detailed breakdown
        Log.d("ProgressFragment", "=== ASSESSMENT PROGRESS BREAKDOWN ===");
        Log.d("ProgressFragment", "Quizzes: " + completedQuizzes + "/" + totalQuizzes);
        Log.d("ProgressFragment", "Coding Exercises: " + completedCoding + "/" + totalCoding);
        Log.d("ProgressFragment", "Syntax Error: " + completedSyntax + "/" + totalSyntax);
        Log.d("ProgressFragment", "Program Tracing: " + completedTracing + "/" + totalTracing);
        Log.d("ProgressFragment", "Machine Problem: " + completedMachine + "/" + totalMachine);
        Log.d("ProgressFragment", "TOTAL: " + completedAssessments + "/" + totalAssessments);

        // Calculate percentage
        int progressPercentage = totalAssessments > 0
                ? (int) ((completedAssessments * 100.0) / totalAssessments)
                : 0;

        Log.d("ProgressFragment", "Overall Progress: " + progressPercentage + "%");

        // Update UI with assessment-based progress
        updateAssessmentProgressUI(completedAssessments, totalAssessments, progressPercentage);
    }

    private void updateAssessmentProgressUI(int completed, int total, int percentage) {
        int perc = (int) ((completed * 100.0) / total);
        assessment_progress.setText(perc + "%");
    }

    private void updateModuleProgress(int beginnerComp, int beginnerTotal,
                                      int intermediateComp, int intermediateTotal,
                                      int advancedComp, int advancedTotal) {
        // Calculate percentages
        int beginnerPercent = (int) ((beginnerComp * 100.0) / beginnerTotal);
        int intermediatePercent = (int) ((intermediateComp * 100.0) / intermediateTotal);
        int advancedPercent = (int) ((advancedComp * 100.0) / advancedTotal);
        int perc = (int) (((beginnerComp + intermediateComp + advancedComp) * 100.0) / (beginnerTotal + intermediateTotal + advancedTotal));

        // Update TextViews
        beginnerProgress.setText(beginnerPercent + "%");
        intermediateProgress.setText(intermediatePercent + "%");
        advancedProgress.setText(advancedPercent + "%");

        progressCircle.setProgress(perc);
        progressPercent.setText(perc + "%");

        // Update ProgressBars
        progressBarBeginner.setMax(100);
        progressBarBeginner.setProgress(beginnerPercent);

        progressBarIntermediate.setMax(100);
        progressBarIntermediate.setProgress(intermediatePercent);

        progressBarAdvanced.setMax(100);
        progressBarAdvanced.setProgress(advancedPercent);
    }

    private void showProgressLayout() {
        noProgressLayout.setVisibility(View.GONE);
        withProgressLayout.setVisibility(View.VISIBLE);
    }

    private void showNoProgressLayout() {
        noProgressLayout.setVisibility(View.VISIBLE);
        withProgressLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }
}