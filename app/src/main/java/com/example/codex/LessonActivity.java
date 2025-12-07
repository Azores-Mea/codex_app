package com.example.codex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.webkit.WebView;
import com.google.firebase.database.*;
import android.text.Html;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class LessonActivity extends AppCompatActivity {

    LinearLayout lessonContainer;
    DatabaseReference ref;
    SessionManager sessionManager;
    String lessonMainTitle = "";
    boolean isLessonDone = false;

    Button markDoneBtn;
    Button practiceTestBtn;
    Button quizBtn;
    Button syntaxErrorBtn;
    Button tracingBtn;
    Button machineProblemBtn;

    // Track available assessments
    boolean hasQuiz = false;
    boolean hasSyntaxError = false;
    boolean hasTracing = false;
    boolean hasMachineProblem = false;

    // Track completion status
    int completedAssessments = 0;
    boolean quizCompleted = false;
    boolean syntaxErrorCompleted = false;
    boolean tracingCompleted = false;
    boolean machineProblemCompleted = false;

    // Activity result launchers
    private ActivityResultLauncher<Intent> practiceQuizLauncher;
    private ActivityResultLauncher<Intent> quizLauncher;
    private ActivityResultLauncher<Intent> syntaxErrorLauncher;
    private ActivityResultLauncher<Intent> tracingLauncher;
    private ActivityResultLauncher<Intent> machineProblemLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lesson_view);

        // Initialize activity result launchers
        practiceQuizLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Practice quiz doesn't affect lesson status
                    }
                }
        );

        quizLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        reloadAssessmentStatus();
                    }
                }
        );

        syntaxErrorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        reloadAssessmentStatus();
                    }
                }
        );

        tracingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        reloadAssessmentStatus();
                    }
                }
        );

        machineProblemLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        reloadAssessmentStatus();
                    }
                }
        );

        ImageView backBtn = findViewById(R.id.back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        lessonContainer = findViewById(R.id.lesson_container);
        sessionManager = new SessionManager(this);

        String lessonId = sessionManager.getSelectedLesson();
        if (lessonId == null || lessonId.isEmpty()) {
            lessonId = "L1";
        }

        loadLessonHeader(lessonId);
        ref = FirebaseDatabase.getInstance().getReference("Lessons")
                .child(lessonId)
                .child("content");

        loadLessonContent(lessonId);
    }




    private void loadLessonHeader(String lessonId) {
        DatabaseReference lessonRef = FirebaseDatabase.getInstance().getReference("Lessons").child(lessonId);

        lessonRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return;

            DataSnapshot snap = task.getResult();
            String mainTitle = snap.child("main_title").getValue(String.class);

            if (mainTitle != null) {
                mainTitle = mainTitle.replaceAll("(?i)<p>", "")
                        .replaceAll("(?i)</p>", "")
                        .trim();
                lessonMainTitle = mainTitle;
            }

            TextView headerTitle = findViewById(R.id.main_title);
            if (headerTitle != null && mainTitle != null && !mainTitle.isEmpty()) {
                headerTitle.setText(mainTitle);
            }
        });
    }

    private void reloadAssessmentStatus() {
        String userId = String.valueOf(sessionManager.getUserId());
        String lessonId = sessionManager.getSelectedLesson();

        DatabaseReference assessmentRef = FirebaseDatabase.getInstance().getReference("assessment").child(lessonId);

        // Check what assessments are available
        assessmentRef.get().addOnSuccessListener(snapshot -> {
            hasQuiz = snapshot.child("Quiz").exists();
            hasSyntaxError = snapshot.child("FindingSyntaxError").exists();
            hasTracing = snapshot.child("ProgramTracing").exists();
            hasMachineProblem = snapshot.child("MachineProblem").exists();

            checkAssessmentCompletion(userId, lessonId);
        });
    }

    private void checkAssessmentCompletion(String userId, String lessonId) {
        DatabaseReference resultsRef = FirebaseDatabase.getInstance().getReference();

        completedAssessments = 0;

        // Check Quiz
        if (hasQuiz) {
            resultsRef.child("quizResults").child(userId).child(lessonId)
                    .get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String passedStatus = snapshot.child("passed").getValue(String.class);
                            if ("Passed".equalsIgnoreCase(passedStatus)) {
                                quizCompleted = true;
                                completedAssessments++;
                            } else {
                                quizCompleted = false;
                            }
                        } else {
                            quizCompleted = false;
                        }
                        updateAssessmentUI();
                    });
        }

        // Check Syntax Error
        if (hasSyntaxError) {
            resultsRef.child("syntaxErrorResults").child(userId).child(lessonId)
                    .get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists() && Boolean.TRUE.equals(snapshot.child("completed").getValue(Boolean.class))) {
                            syntaxErrorCompleted = true;
                            completedAssessments++;
                        } else {
                            syntaxErrorCompleted = false;
                        }
                        updateAssessmentUI();
                    });
        }

        // Check Program Tracing
        if (hasTracing) {
            resultsRef.child("tracingResults").child(userId).child(lessonId)
                    .get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists() && Boolean.TRUE.equals(snapshot.child("completed").getValue(Boolean.class))) {
                            tracingCompleted = true;
                            completedAssessments++;
                        } else {
                            tracingCompleted = false;
                        }
                        updateAssessmentUI();
                    });
        }

        // Check Machine Problem
        if (hasMachineProblem) {
            resultsRef.child("machineProblemResults").child(userId).child(lessonId)
                    .get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists() && Boolean.TRUE.equals(snapshot.child("completed").getValue(Boolean.class))) {
                            machineProblemCompleted = true;
                            completedAssessments++;
                        } else {
                            machineProblemCompleted = false;
                        }
                        updateAssessmentUI();
                    });
        }
    }

    private void loadLessonContent(String lessonId) {
        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            for (DataSnapshot snap : task.getResult().getChildren()) {

                // --- TITLE BLOCK ---
                if (snap.child("TITLE").exists()) {
                    DataSnapshot t = snap.child("TITLE");
                    View view = getLayoutInflater().inflate(R.layout.lesson_title, lessonContainer, false);

                    setHtmlText(view.findViewById(R.id.title), t.child("title").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.desc), t.child("description").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper1), t.child("helper1").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper2), t.child("helper2").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper3), t.child("helper3").getValue(String.class));

                    loadImage(view.findViewById(R.id.image1), t.child("helper1Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image2), t.child("helper2Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image3), t.child("helper3Drawable").getValue(String.class));

                    setCodeHtml(view.findViewById(R.id.code1), t.child("helper1Code").getValue(String.class));
                    setCodeHtml(view.findViewById(R.id.code2), t.child("helper2Code").getValue(String.class));
                    setCodeHtml(view.findViewById(R.id.code3), t.child("helper3Code").getValue(String.class));

                    lessonContainer.addView(view);
                }

                // --- EXAMPLE BLOCK ---
                if (snap.child("EXAMPLE").exists()) {
                    DataSnapshot e = snap.child("EXAMPLE");
                    if (e.hasChild("TYPES")) e = e.child("TYPES");

                    View view = getLayoutInflater().inflate(R.layout.lesson_example_type, lessonContainer, false);

                    setHtmlText(view.findViewById(R.id.example_type), e.child("exampleTitle").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.shortDesc), e.child("exampleDescription").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper4), e.child("helper4").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper5), e.child("helper5").getValue(String.class));

                    loadImage(view.findViewById(R.id.image4), e.child("helper4Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image5), e.child("helper5Drawable").getValue(String.class));

                    setCodeHtml(view.findViewById(R.id.code4), e.child("helper4Code").getValue(String.class));
                    setCodeHtml(view.findViewById(R.id.code5), e.child("helper5Code").getValue(String.class));

                    lessonContainer.addView(view);
                }

                // --- OUTPUT BLOCK ---
                if (snap.child("OUTPUT").exists() || snap.child("SUBTITLE").child("OUTPUT").exists()) {
                    DataSnapshot o = snap.child("OUTPUT").exists() ?
                            snap.child("OUTPUT") :
                            snap.child("SUBTITLE").child("OUTPUT");

                    View view = getLayoutInflater().inflate(R.layout.lesson_output, lessonContainer, false);

                    setHtmlText(view.findViewById(R.id.output), o.child("subtitle").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper6), o.child("helper6").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper7), o.child("helper7").getValue(String.class));

                    loadImage(view.findViewById(R.id.image6), o.child("helper6Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image7), o.child("helper7Drawable").getValue(String.class));

                    setCodeHtml(view.findViewById(R.id.code6), o.child("helper6Code").getValue(String.class));
                    setCodeHtml(view.findViewById(R.id.code7), o.child("helper7Code").getValue(String.class));

                    lessonContainer.addView(view);
                }

                // --- TOOLTIP BLOCK ---
                if (snap.child("TOOLTIP").exists()) {
                    DataSnapshot ttip = snap.child("TOOLTIP");
                    String tooltipHtml = ttip.child("tooltip").getValue(String.class);

                    View tipView = getLayoutInflater().inflate(R.layout.lesson_tooltip, lessonContainer, false);
                    ImageView tipIcon = tipView.findViewById(R.id.tipButton);

                    tipIcon.setOnClickListener(v -> showTooltip(v, tooltipHtml));
                    lessonContainer.addView(tipView);
                }
            }

            View endView = getLayoutInflater().inflate(R.layout.lesson_end, lessonContainer, false);
            String moduleNumber = lessonId.replaceAll("[^0-9]", "");
            TextView moduleText = endView.findViewById(R.id.module_text);
            if (lessonMainTitle != null && !lessonMainTitle.trim().isEmpty()) {
                moduleText.setText("End of Module " + moduleNumber + ". " + lessonMainTitle);
            } else {
                moduleText.setText("End of Module " + moduleNumber);
            }

            markDoneBtn = endView.findViewById(R.id.markDone);
            practiceTestBtn = endView.findViewById(R.id.takeQuiz);
            Button assessmentBtn = endView.findViewById(R.id.takeCodingExercises);

            quizBtn = endView.findViewById(R.id.assessment_quiz);
            syntaxErrorBtn = endView.findViewById(R.id.assessment_syntaxError);
            tracingBtn = endView.findViewById(R.id.assessment_tracing);
            machineProblemBtn = endView.findViewById(R.id.assessment_machine);

            // Practice Test is always enabled
            practiceTestBtn.setEnabled(true);

            // Assessment button starts disabled
            assessmentBtn.setEnabled(false);
            assessmentBtn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            assessmentBtn.setTextColor(Color.BLACK);
            assessmentBtn.setText("Assessments");

            // Assessment items start hidden
            quizBtn.setVisibility(View.GONE);
            syntaxErrorBtn.setVisibility(View.GONE);
            tracingBtn.setVisibility(View.GONE);
            machineProblemBtn.setVisibility(View.GONE);

            String userId = String.valueOf(sessionManager.getUserId());
            DatabaseReference recentRef = FirebaseDatabase.getInstance().getReference("RecentLesson");
            DatabaseReference assessmentRef = FirebaseDatabase.getInstance().getReference("assessment").child(lessonId);

            // Check what assessments are available
            assessmentRef.get().addOnSuccessListener(snapshot -> {
                hasQuiz = snapshot.child("Quiz").exists();
                hasSyntaxError = snapshot.child("FindingSyntaxError").exists();
                hasTracing = snapshot.child("ProgramTracing").exists();
                hasMachineProblem = snapshot.child("MachineProblem").exists();

                checkAssessmentCompletion(userId, lessonId);
            });

            // Check lesson done status
            recentRef.child(userId).child(lessonId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            isLessonDone = snapshot.exists();
                            updateLessonUI();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) { }
                    });

            // Practice Test button - always enabled, launches practice quiz
            practiceTestBtn.setOnClickListener(v -> {
                sessionManager.saveSelectedLesson(lessonId);
                Intent intent = new Intent(LessonActivity.this, PracticeQuizActivity.class);
                intent.putExtra("lessonId", lessonId);
                practiceQuizLauncher.launch(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            // Mark Done button - enables assessment button
            markDoneBtn.setOnClickListener(v -> {
                if (isLessonDone) return;

                markDoneBtn.setText("Done");
                markDoneBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0B5C19")));
                markDoneBtn.setEnabled(false);
                markDoneBtn.setTextColor(Color.WHITE);;

                // Enable assessment button
                assessmentBtn.setEnabled(true);
                assessmentBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));
                assessmentBtn.setTextColor(Color.WHITE);
                if (assessmentBtn instanceof com.google.android.material.button.MaterialButton) {
                    ((com.google.android.material.button.MaterialButton) assessmentBtn).setStrokeWidth(0);
                }

                Map<String, Object> lessonData = new HashMap<>();
                lessonData.put("title", lessonMainTitle);
                lessonData.put("timestamp", ServerValue.TIMESTAMP);

                recentRef.child(userId).child(lessonId)
                        .setValue(lessonData)
                        .addOnSuccessListener(aVoid -> {
                            isLessonDone = true;
                            updateLessonUI();
                        });
            });

            // Assessment button - toggles visibility of assessment items
            assessmentBtn.setOnClickListener(v -> {
                if (!isLessonDone) return;

                // Toggle visibility of assessment buttons
                boolean isVisible = quizBtn.getVisibility() == View.VISIBLE ||
                        syntaxErrorBtn.getVisibility() == View.VISIBLE ||
                        tracingBtn.getVisibility() == View.VISIBLE ||
                        machineProblemBtn.getVisibility() == View.VISIBLE;

                if (isVisible) {
                    // Hide all
                    if (hasQuiz) quizBtn.setVisibility(View.GONE);
                    if (hasSyntaxError) syntaxErrorBtn.setVisibility(View.GONE);
                    if (hasTracing) tracingBtn.setVisibility(View.GONE);
                    if (hasMachineProblem) machineProblemBtn.setVisibility(View.GONE);
                } else {
                    // Show available assessments
                    if (hasQuiz) quizBtn.setVisibility(View.VISIBLE);
                    if (hasSyntaxError) syntaxErrorBtn.setVisibility(View.VISIBLE);
                    if (hasTracing) tracingBtn.setVisibility(View.VISIBLE);
                    if (hasMachineProblem) machineProblemBtn.setVisibility(View.VISIBLE);
                }
            });

            // Quiz button - disabled after passing
            quizBtn.setOnClickListener(v -> {
                if (!isLessonDone) return;

                // Don't allow clicking if already passed (button is disabled in updateAssessmentUI)
                if (quizCompleted) return;

                sessionManager.saveSelectedLesson(lessonId);
                Intent intent = new Intent(LessonActivity.this, LessonQuizActivity.class);
                quizLauncher.launch(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            // Syntax Error button
            syntaxErrorBtn.setOnClickListener(v -> {
                if (!isLessonDone || syntaxErrorCompleted) return;

                sessionManager.saveSelectedLesson(lessonId);
                Intent intent = new Intent(LessonActivity.this, SyntaxErrorActivity.class);
                intent.putExtra("lessonId", lessonId);
                syntaxErrorLauncher.launch(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            // Program Tracing button
            tracingBtn.setOnClickListener(v -> {
                if (!isLessonDone || tracingCompleted) return;

                sessionManager.saveSelectedLesson(lessonId);
                Intent intent = new Intent(LessonActivity.this, ProgramTracingActivity.class);
                intent.putExtra("lessonId", lessonId);
                tracingLauncher.launch(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            // Machine Problem button - always available after lesson done
            machineProblemBtn.setOnClickListener(v -> {
                if (!isLessonDone || machineProblemCompleted) return;

                sessionManager.saveSelectedLesson(lessonId);
                Intent intent = new Intent(LessonActivity.this, MachineProblemActivity.class);
                intent.putExtra("lessonId", lessonId);
                machineProblemLauncher.launch(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            lessonContainer.addView(endView);
        });
    }

    private void updateLessonUI() {
        if (markDoneBtn == null || practiceTestBtn == null) return;

        // Find the assessment button from the same parent view
        View endView = (View) markDoneBtn.getParent();
        Button assessmentBtn = endView.findViewById(R.id.takeCodingExercises);

        // Handle Mark as Done button
        if (isLessonDone) {
            markDoneBtn.setText("Done");
            markDoneBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0B5C19")));
            markDoneBtn.setEnabled(false);
            markDoneBtn.setTextColor(Color.WHITE);;

            // Enable assessment button
            if (assessmentBtn != null) {
                assessmentBtn.setEnabled(true);
                assessmentBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));
                assessmentBtn.setTextColor(Color.WHITE);
                if (assessmentBtn instanceof com.google.android.material.button.MaterialButton) {
                    ((com.google.android.material.button.MaterialButton) assessmentBtn).setStrokeWidth(0);
                }
            }
        } else {
            markDoneBtn.setText("Mark as done");
            markDoneBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            markDoneBtn.setEnabled(true);

            // Disable assessment button
            if (assessmentBtn != null) {
                assessmentBtn.setEnabled(false);
                assessmentBtn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                assessmentBtn.setTextColor(Color.BLACK);
            }

            // Hide all assessment items
            if (quizBtn != null) quizBtn.setVisibility(View.GONE);
            if (syntaxErrorBtn != null) syntaxErrorBtn.setVisibility(View.GONE);
            if (tracingBtn != null) tracingBtn.setVisibility(View.GONE);
            if (machineProblemBtn != null) machineProblemBtn.setVisibility(View.GONE);
        }

        // Practice Test is always enabled
        practiceTestBtn.setEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload assessment status when returning to this activity
        String userId = String.valueOf(sessionManager.getUserId());
        String lessonId = sessionManager.getSelectedLesson();
        if (lessonId != null && !lessonId.isEmpty()) {
            reloadAssessmentStatus();
        }
    }
    private void updateAssessmentUI() {
        if (!isLessonDone) return;

        // Count required assessments (excluding machine problem)
        int requiredCount = 0;
        int completedRequired = 0;

        if (hasQuiz) {
            requiredCount++;
            if (quizCompleted) completedRequired++;
        }
        if (hasSyntaxError) {
            requiredCount++;
            if (syntaxErrorCompleted) completedRequired++;
        }
        if (hasTracing) {
            requiredCount++;
            if (tracingCompleted) completedRequired++;
        }

        // Update individual button states
        if (hasQuiz && quizBtn != null) {
            if (quizCompleted) {
                // Quiz passed - disable button
                quizBtn.setText("Passed");
                quizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0B5C19")));
                quizBtn.setTextColor(Color.WHITE);
                quizBtn.setEnabled(false);  // Disable after passing
                if (quizBtn instanceof com.google.android.material.button.MaterialButton) {
                    ((com.google.android.material.button.MaterialButton) quizBtn).setIcon(null);
                    ((com.google.android.material.button.MaterialButton) quizBtn).setStrokeWidth(0);
                }
            } else {
                // Quiz not passed or not taken - allow (re)take
                quizBtn.setText("Retake");
                quizBtn.setEnabled(true);
                quizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));
                quizBtn.setTextColor(Color.WHITE);
            }
        }

        if (hasSyntaxError && syntaxErrorBtn != null) {
            if (syntaxErrorCompleted) {
                syntaxErrorBtn.setText("Completed");
                syntaxErrorBtn.setEnabled(false);
                syntaxErrorBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0B5C19")));
                syntaxErrorBtn.setTextColor(Color.WHITE);
                if (syntaxErrorBtn instanceof com.google.android.material.button.MaterialButton) {
                    ((com.google.android.material.button.MaterialButton) syntaxErrorBtn).setIcon(null);
                    ((com.google.android.material.button.MaterialButton) syntaxErrorBtn).setStrokeWidth(0);
                }
            } else {
                syntaxErrorBtn.setEnabled(true);
                syntaxErrorBtn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                syntaxErrorBtn.setTextColor(Color.parseColor("#0D47A1"));
            }
        }

        if (hasTracing && tracingBtn != null) {
            if (tracingCompleted) {
                tracingBtn.setText("Completed");
                tracingBtn.setEnabled(false);
                tracingBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0B5C19")));
                tracingBtn.setTextColor(Color.WHITE);
                if (tracingBtn instanceof com.google.android.material.button.MaterialButton) {
                    ((com.google.android.material.button.MaterialButton) tracingBtn).setIcon(null);
                    ((com.google.android.material.button.MaterialButton) tracingBtn).setStrokeWidth(0);
                }
            } else {
                tracingBtn.setEnabled(true);
                tracingBtn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                tracingBtn.setTextColor(Color.parseColor("#0D47A1"));
            }
        }

        if (hasMachineProblem && machineProblemBtn != null) {
            if (machineProblemCompleted) {
                machineProblemBtn.setText("Completed");
                machineProblemBtn.setEnabled(false);
                machineProblemBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0B5C19")));
                machineProblemBtn.setTextColor(Color.WHITE);
                if (machineProblemBtn instanceof com.google.android.material.button.MaterialButton) {
                    ((com.google.android.material.button.MaterialButton) machineProblemBtn).setIcon(null);
                    ((com.google.android.material.button.MaterialButton) machineProblemBtn).setStrokeWidth(0);
                }
            } else {
                machineProblemBtn.setEnabled(true);
                machineProblemBtn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                machineProblemBtn.setTextColor(Color.parseColor("#0D47A1"));
            }
        }

        // Check if ALL required assessments are completed (excluding machine problem)
        // Unlock next lesson when ALL required assessments are completed
        if (completedRequired == requiredCount && requiredCount > 0) {
            // Unlock next lesson
            String lessonId = sessionManager.getSelectedLesson();
            String userId = String.valueOf(sessionManager.getUserId());
            DatabaseReference unlockedRef = FirebaseDatabase.getInstance().getReference("unlockedLessons");

            // Calculate next lesson
            int currentLessonNum = Integer.parseInt(lessonId.replaceAll("[^0-9]", ""));
            String nextLessonId = "L" + (currentLessonNum + 1);

            Map<String, Object> unlockData = new HashMap<>();
            unlockData.put("unlocked", true);
            unlockData.put("timestamp", ServerValue.TIMESTAMP);

            unlockedRef.child(userId).child(nextLessonId).setValue(unlockData);
        }
    }

    private void rotateArrow(ImageView arrow, float fromDegree, float toDegree) {
        RotateAnimation rotate = new RotateAnimation(
                fromDegree, toDegree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        arrow.startAnimation(rotate);
    }

    private void showTooltip(View anchorView, String tooltipHtml) {
        if (tooltipHtml == null || tooltipHtml.trim().isEmpty()) return;

        View popupView = LayoutInflater.from(this).inflate(R.layout.tooltip_bubble, null);
        TextView text = popupView.findViewById(R.id.tooltip_text);
        text.setText(Html.fromHtml(tooltipHtml, Html.FROM_HTML_MODE_LEGACY));

        int topPadding = (int) (10 * getResources().getDisplayMetrics().density);
        text.setPadding(text.getPaddingLeft(), topPadding, text.getPaddingRight(), text.getPaddingBottom());

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int measuredWidth = popupView.getMeasuredWidth();
        int measuredHeight = popupView.getMeasuredHeight();

        PopupWindow popupWindow = new PopupWindow(popupView, measuredWidth, measuredHeight, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(10);

        if (anchorView instanceof ImageView) {
            ((ImageView) anchorView).setImageResource(R.drawable.tooltip_open);
        }

        anchorView.post(() -> {
            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);
            int anchorHeight = anchorView.getHeight();
            int popupHeight = popupView.getMeasuredHeight();

            int yOffset = -(anchorHeight / 2) - (popupHeight / 2);
            int xOffset = anchorView.getWidth() + (int) (10 * getResources().getDisplayMetrics().density);
            popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
        });

        popupWindow.setOnDismissListener(() -> {
            if (anchorView instanceof ImageView) {
                ((ImageView) anchorView).setImageResource(R.drawable.tooltip_close);
            }
        });

        popupView.postDelayed(popupWindow::dismiss, 3000);
    }

    private void setHtmlText(TextView view, String value) {
        if (value == null) {
            view.setVisibility(View.GONE);
            return;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("<br>")
                || trimmed.equalsIgnoreCase("<br/>")
                || trimmed.equalsIgnoreCase("<p><br></p>")
                || trimmed.equalsIgnoreCase("<p><br/></p>")) {
            view.setVisibility(View.GONE);
            return;
        }

        // Convert single asterisks to bold BEFORE processing HTML
        // This handles *text* -> <b>text</b>
        // Uses negative lookbehind/lookahead to avoid matching ** (double asterisks)
        trimmed = trimmed.replaceAll("(?<!\\*)\\*(?!\\*)([^*]+?)(?<!\\*)\\*(?!\\*)", "<b>$1</b>");

        boolean hasListItems = trimmed.toLowerCase().contains("<li>");
        if (hasListItems) {
            SpannableStringBuilder sb = new SpannableStringBuilder();
            final int indentPx = (int) (view.getResources().getDisplayMetrics().density * 24);

            // Process the HTML character by character to handle mixed content
            int currentPos = 0;

            // Find all list blocks (both ul and ol)
            java.util.regex.Pattern listPattern = java.util.regex.Pattern.compile("(?is)<(ul|ol)[^>]*>(.*?)</\\1>");
            java.util.regex.Matcher listMatcher = listPattern.matcher(trimmed);

            while (listMatcher.find()) {
                // Add any text before this list
                String beforeList = trimmed.substring(currentPos, listMatcher.start());
                if (!beforeList.trim().isEmpty()) {
                    String cleanBefore = beforeList
                            .replaceAll("(?i)<\\/?p[^>]*>", "")
                            .replaceAll("(<br>\\s*){2,}", "<br>")
                            .trim();
                    if (!cleanBefore.isEmpty()) {
                        CharSequence beforeSpanned = Html.fromHtml(cleanBefore, Html.FROM_HTML_MODE_LEGACY);
                        sb.append(beforeSpanned);
                        sb.append("\n\n");
                    }
                }

                String listType = listMatcher.group(1); // "ul" or "ol"
                String listContent = listMatcher.group(2);

                // Find all list items
                java.util.regex.Pattern liPattern = java.util.regex.Pattern.compile("(?is)<li>(.*?)</li>");
                java.util.regex.Matcher liMatcher = liPattern.matcher(listContent);

                int itemNumber = 1;
                while (liMatcher.find()) {
                    String liContent = liMatcher.group(1).trim();
                    if (liContent.isEmpty()) continue;

                    CharSequence liSpanned = Html.fromHtml(liContent, Html.FROM_HTML_MODE_LEGACY);
                    int start = sb.length();

                    // Use number for ol, bullet for ul
                    String prefix;
                    int hangingIndent;
                    if ("ol".equalsIgnoreCase(listType)) {
                        prefix = String.valueOf(itemNumber) + ". ";
                        itemNumber++;
                        // Calculate hanging indent based on prefix length
                        hangingIndent = (int) (prefix.length() * 8 * view.getResources().getDisplayMetrics().density);
                    } else {
                        prefix = "• ";
                        // Fixed hanging indent for bullet (bullet + space)
                        hangingIndent = (int) (12 * view.getResources().getDisplayMetrics().density);
                    }

                    sb.append(prefix).append(liSpanned);
                    int end = sb.length();
                    sb.append("\n");

                    // Apply hanging indent (0 for first line, hangingIndent for wrapped lines)
                    android.text.style.LeadingMarginSpan.Standard marginSpan =
                            new android.text.style.LeadingMarginSpan.Standard(0, hangingIndent);
                    sb.setSpan(marginSpan, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                currentPos = listMatcher.end();
            }

            // Add any remaining text after the last list
            if (currentPos < trimmed.length()) {
                String afterLists = trimmed.substring(currentPos);
                if (!afterLists.trim().isEmpty()) {
                    String cleanAfter = afterLists
                            .replaceAll("(?i)<\\/?p[^>]*>", "")
                            .replaceAll("(<br>\\s*){2,}", "<br>")
                            .trim();
                    if (!cleanAfter.isEmpty()) {
                        sb.append("\n");
                        CharSequence afterSpanned = Html.fromHtml(cleanAfter, Html.FROM_HTML_MODE_LEGACY);
                        sb.append(afterSpanned);
                    }
                }
            }

            // Remove trailing whitespace
            while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.delete(sb.length() - 1, sb.length());
            }

            view.setText(sb);
            view.setVisibility(View.VISIBLE);
            view.setLineSpacing(0, 1.15f);
            view.setIncludeFontPadding(false);
            view.setPadding(0, 0, 0, 0);
            return;
        }

        // No lists - simple formatting
        String formatted = trimmed
                .replaceAll("(?i)<p>", "")
                .replaceAll("(?i)</p>", "<br>")
                .replaceAll("(<br>\\s*){2,}", "<br>")
                .replaceAll("(<br>)+$", "");

        CharSequence spanned = Html.fromHtml(formatted, Html.FROM_HTML_MODE_LEGACY);
        view.setText(spanned);
        view.setVisibility(View.VISIBLE);
        view.setPadding(0, 0, 0, 0);
        view.setLineSpacing(0, 1.15f);
        view.setIncludeFontPadding(false);
    }
    @SuppressLint("SetJavaScriptEnabled")
    private void setCodeHtml(WebView webView, String content) {
        if (content == null || content.trim().isEmpty()) {
            webView.setVisibility(View.GONE);
            return;
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);

        String clean = content
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)<p>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&quot;", "\"")
                .replaceAll("\t", "    ")
                .trim();

        boolean looksLikeCode = clean.contains(";")
                || clean.contains("{")
                || clean.contains("}")
                || clean.contains("//")
                || clean.contains("/*")
                || clean.contains("=")
                || clean.matches("(?s).*\\b(class|public|if|else|for|while|int|String)\\b.*");

        String escaped = clean
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        if (!escaped.endsWith("\n")) escaped += "\n";

        String html;

        if (looksLikeCode) {
            html =
                    "<html>" +
                            "<head>" +
                            "  <link href='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css' rel='stylesheet' />" +
                            "  <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js'></script>" +
                            "  <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js'></script>" +
                            "  <style>" +
                            "    body { background: transparent; margin: 0; padding: 8px; }" +
                            "    pre { background: #f5f5f5; border-radius: 8px; padding: 12px 16px; font-size: 12px; " +
                            "          font-family: 'JetBrains Mono', 'Fira Code', monospace; line-height: 1.5; white-space: pre-wrap; overflow-x: auto; color: #2d2d2d; }" +
                            "    code { display: block; white-space: pre-wrap; }" +
                            "  </style>" +
                            "</head>" +
                            "<body><pre><code class='language-java'>" + escaped + "</code></pre></body></html>";
        } else {
            html =
                    "<html><body style='background:transparent; margin:0; padding:12px;'>" +
                            "<div style='background:#f5f5f5; border-radius:8px; padding:10px 14px; " +
                            "font-family:monospace; color:#333; font-size:12px; line-height:1.4; " +
                            "white-space:pre-wrap;'>" +
                            escaped +
                            "</div></body></html>";
        }

        if (escaped.trim().isEmpty()) {
            webView.setVisibility(View.GONE);
            return;
        }

        webView.setVisibility(View.VISIBLE);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }
    private void loadImage(ImageView view, String drawableName) {
        if (drawableName == null || drawableName.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }

        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) {
            view.setImageResource(resId);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}