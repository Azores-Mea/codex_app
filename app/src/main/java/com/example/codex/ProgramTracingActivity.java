package com.example.codex;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.text.LineBreaker;
import android.os.Bundle;
import android.text.Html;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgramTracingActivity extends AppCompatActivity {

    TextView codeDisplayText, exerciseTitle, descriptionBox, feedbackText;
    EditText answerInput;
    MaterialButton checkButton, nextTracing;
    TextView progressPercent;
    ProgressBar progressBar;
    TracingExercise exercise;

    int currentIndex = 1;
    int totalExercises = 0;
    int correctExercises = 0;

    private List<TracingExercise> exerciseList = new ArrayList<>();
    private Map<Integer, TracingSubmission> submissionCache = new HashMap<>();
    private SessionManager sessionManager;
    private String lessonId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program_tracing);

        sessionManager = new SessionManager(this);

        codeDisplayText = findViewById(R.id.codeDisplay_text);
        exerciseTitle = findViewById(R.id.exerciseTitle);
        descriptionBox = findViewById(R.id.descriptionBox);
        answerInput = findViewById(R.id.answerInput);
        checkButton = findViewById(R.id.checkButton);
        nextTracing = findViewById(R.id.nextTracing);
        feedbackText = findViewById(R.id.feedbackText);
        progressBar = findViewById(R.id.progressBar);
        progressPercent = findViewById(R.id.progressPercent);

        descriptionBox.setMovementMethod(new ScrollingMovementMethod());

        nextTracing.setEnabled(false);
        nextTracing.setAlpha(0.4f);

        lessonId = getIntent().getStringExtra("lessonId");
        if (lessonId == null || lessonId.isEmpty()) {
            lessonId = "L1";
        }

        loadExercises(lessonId);

        checkButton.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(answerInput.getWindowToken(), 0);
            answerInput.clearFocus();

            checkAnswer();
        });

        nextTracing.setOnClickListener(v -> {
            if (currentIndex >= totalExercises) {
                showSubmitConfirmationDialog();
            } else {
                currentIndex++;
                nextTracing.setEnabled(false);
                nextTracing.setAlpha(0.4f);

                // Re-enable input and check button for next exercise
                answerInput.setEnabled(true);
                checkButton.setEnabled(true);

                answerInput.setText("");
                feedbackText.setText("");
                loadCurrentExercise();
            }
        });
    }

    private void loadExercises(String lessonId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("assessment/" + lessonId + "/ProgramTracing");

        ref.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(this, "No exercises found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            exerciseList.clear();
            for (DataSnapshot child : snapshot.getChildren()) {
                TracingExercise ex = child.getValue(TracingExercise.class);
                if (ex != null) {
                    exerciseList.add(ex);
                }
            }

            // Remove null entries and limit to 2
            exerciseList.removeIf(ex -> ex == null);
            if (exerciseList.size() > 2) {
                exerciseList = exerciseList.subList(0, 2);
            }

            totalExercises = exerciseList.size();

            if (totalExercises == 0) {
                Toast.makeText(this, "No valid exercises found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            loadPreviousAnswers(lessonId);
            loadCurrentExercise();

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading exercises: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadPreviousAnswers(String lessonId) {
        DatabaseReference userAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userTracingAnswers")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId);

        userAnswersRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                    try {
                        int exerciseIndex = Integer.parseInt(exerciseSnap.getKey());
                        String savedAnswer = exerciseSnap.child("answer").getValue(String.class);
                        Boolean isCorrect = exerciseSnap.child("isCorrect").getValue(Boolean.class);

                        if (savedAnswer != null && isCorrect != null) {
                            submissionCache.put(exerciseIndex,
                                    new TracingSubmission(savedAnswer, isCorrect));
                        }
                    } catch (NumberFormatException e) {
                        Log.e("ProgramTracingActivity", "Invalid exercise index: " + exerciseSnap.getKey());
                    }
                }

                loadCurrentExercise();
            }
        }).addOnFailureListener(e -> {
            Log.e("ProgramTracingActivity", "Failed to load previous answers: " + e.getMessage());
        });
    }

    private void loadCurrentExercise() {
        if (currentIndex < 1 || currentIndex > exerciseList.size()) {
            Toast.makeText(this, "Invalid exercise index", Toast.LENGTH_SHORT).show();
            return;
        }

        exercise = exerciseList.get(currentIndex - 1);

        if (exercise != null) {
            exerciseTitle.setText(exercise.title);

            if (submissionCache.containsKey(currentIndex)) {
                TracingSubmission prev = submissionCache.get(currentIndex);
                if (prev != null) {
                    answerInput.setText(prev.answer);

                    if (prev.isCorrect) {
                        feedbackText.setText("✓ Correct!");
                        feedbackText.setTextColor(Color.parseColor("#06651A"));
                        nextTracing.setEnabled(true);
                        nextTracing.setAlpha(1f);
                        answerInput.setEnabled(false);
                        checkButton.setEnabled(false);
                    } else {
                        feedbackText.setText("✗ Incorrect. Try again!");
                        feedbackText.setTextColor(Color.parseColor("#E31414"));
                        // Keep input enabled for incorrect answers
                        answerInput.setEnabled(true);
                        checkButton.setEnabled(true);
                    }
                }
            } else {
                // New exercise - enable everything
                answerInput.setText("");
                feedbackText.setText("");
                answerInput.setEnabled(true);
                checkButton.setEnabled(true);
                nextTracing.setEnabled(false);
                nextTracing.setAlpha(0.4f);
            }

            String formattedCode = exercise.code
                    .replace("&nbsp;", " ")
                    .replace("&emsp;", "\t")
                    .replace("<br>", "\n");

            codeDisplayText.setText(formattedCode);

            String html = exercise.description;
            html = html.replace("&nbsp;", " ");
            html = html.replace("word-break: break-all;", "")
                    .replace("word-break:break-all;", "")
                    .replace("word-wrap: break-word;", "")
                    .replace("word-wrap:break-word;", "");

            html = html.replace("<span", "<div")
                    .replace("</span>", "</div>");

            descriptionBox.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
            descriptionBox.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
            descriptionBox.setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE);

            updateProgress();
        }
    }

    private void updateProgress() {
        if (totalExercises == 0) return;

        float progress = ((float) currentIndex - 1) / totalExercises;
        int percent = Math.round(progress * 100);

        progressBar.setProgress(percent);
        progressPercent.setText(percent + "%");
    }

    private void checkAnswer() {
        if (exercise == null) return;

        String userAnswer = answerInput.getText().toString().trim();
        String expectedAnswer = exercise.expectedOutput.trim();

        boolean isCorrect = userAnswer.equals(expectedAnswer);

        if (isCorrect) {
            feedbackText.setText("✓ Correct!");
            feedbackText.setTextColor(Color.parseColor("#06651A"));
            nextTracing.setEnabled(true);
            nextTracing.setAlpha(1f);
            answerInput.setEnabled(false);  // ← This disables the input
            checkButton.setEnabled(false);   // ← This disables the button
        } else {
            feedbackText.setText("✗ Incorrect. Try again!");
            feedbackText.setTextColor(Color.parseColor("#E31414"));
        }

        submissionCache.put(currentIndex, new TracingSubmission(userAnswer, isCorrect));
        saveUserAnswer(currentIndex, userAnswer, isCorrect);
    }

    private void saveUserAnswer(int exerciseIndex, String answer, boolean isCorrect) {
        DatabaseReference userAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userTracingAnswers")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId)
                .child(String.valueOf(exerciseIndex));

        HashMap<String, Object> answerData = new HashMap<>();
        answerData.put("answer", answer);
        answerData.put("isCorrect", isCorrect);
        answerData.put("timestamp", ServerValue.TIMESTAMP);

        userAnswersRef.setValue(answerData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ProgramTracingActivity", "Answer saved successfully for exercise " + exerciseIndex);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProgramTracingActivity", "Failed to save answer: " + e.getMessage());
                });
    }

    private void showSubmitConfirmationDialog() {
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (!isFinishing() && !isDestroyed()) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } else {
            return;
        }

        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Submit exercises?");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        dialogMessage.setText(Html.fromHtml("Once submitted, you cannot make any changes.", Html.FROM_HTML_MODE_LEGACY));

        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        btnNo.setText("Review Answers");
        btnYes.setText("Submit");

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            submitExercises();
        });
    }

    private void submitExercises() {
        correctExercises = 0;
        for (Map.Entry<Integer, TracingSubmission> entry : submissionCache.entrySet()) {
            TracingSubmission submission = entry.getValue();
            if (submission.isCorrect) {
                correctExercises++;
            }
        }

        boolean allCorrect = (correctExercises == totalExercises);

        DatabaseReference tracingResultsRef = FirebaseDatabase.getInstance()
                .getReference("tracingResults")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId);

        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("completed", allCorrect);
        resultData.put("correctCount", correctExercises);
        resultData.put("totalExercises", totalExercises);
        resultData.put("timestamp", ServerValue.TIMESTAMP);

        tracingResultsRef.setValue(resultData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            allCorrect ? "All exercises completed!" :
                                    "Submitted! You got " + correctExercises + "/" + totalExercises + " correct.",
                            Toast.LENGTH_LONG).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("tracingCompleted", allCorrect);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save results: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    public static class TracingSubmission {
        public String answer;
        public boolean isCorrect;

        public TracingSubmission() {}

        public TracingSubmission(String answer, boolean isCorrect) {
            this.answer = answer;
            this.isCorrect = isCorrect;
        }
    }
}