package com.example.codex;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.text.LineBreaker;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExerciseActivity extends AppCompatActivity {

    EditText codeEditor;
    Button runButton;
    Button nextCode;
    TextView outputBox, exerciseTitle, expectedOutputBox, descriptionBox;
    TextView progressPercent;
    ProgressBar progressBar;
    Exercise exercise;

    int currentIndex = 1;
    int totalExercises = 0;
    int correctExercises = 0;

    Handler cooldownHandler = new Handler();

    private static final int MIN_REQUEST_INTERVAL = 250;
    private long lastRequestTime = 0;

    private int requestsThisSecond = 0;
    private long currentSecondStart = 0;
    private static final int MAX_REQUESTS_PER_SECOND = 4;

    private Map<Integer, Submission> submissionCache = new HashMap<>();

    private SessionManager sessionManager;
    private String lessonId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coding);

        sessionManager = new SessionManager(this);

        codeEditor = findViewById(R.id.codeEditor);
        runButton = findViewById(R.id.runButton);
        outputBox = findViewById(R.id.outputBox);
        exerciseTitle = findViewById(R.id.exerciseTitle);
        expectedOutputBox = findViewById(R.id.expectedOutputBox);

        descriptionBox = findViewById(R.id.descriptionBox);
        descriptionBox.setMovementMethod(new ScrollingMovementMethod());

        nextCode = findViewById(R.id.nextCode);
        progressBar = findViewById(R.id.progressBar);
        progressPercent = findViewById(R.id.progressPercent);

        nextCode.setEnabled(false);
        nextCode.setAlpha(0.4f);

        lessonId = getIntent().getStringExtra("lessonId");
        if (lessonId == null || lessonId.isEmpty()) {
            lessonId = "L1";
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("coding_exercises/" + lessonId);

        String finalLessonId = lessonId;
        ref.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                totalExercises = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.getValue() != null) totalExercises++;
                }
                loadExercise(finalLessonId);
                loadPreviousAnswers(finalLessonId);
            }
        });

        runButton.setOnClickListener(v -> {
            if (!canMakeRequest()) {
                Toast.makeText(this,
                        "Please wait a moment before running again",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            runButton.setEnabled(false);
            cooldownHandler.postDelayed(() -> runButton.setEnabled(true), MIN_REQUEST_INTERVAL);

            nextCode.setEnabled(false);
            nextCode.setAlpha(0.4f);

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(codeEditor.getWindowToken(), 0);
            codeEditor.clearFocus();

            outputBox.setTextColor(Color.parseColor("#000000"));
            outputBox.setText("Compiling...");

            runCode();
        });

        nextCode.setOnClickListener(v -> {
            if (currentIndex == totalExercises) {
                showSubmitConfirmationDialog();
            } else {
                currentIndex++;
                nextCode.setEnabled(false);
                nextCode.setAlpha(0.4f);
                outputBox.setText("");
                loadExercise(finalLessonId);
            }
        });
    }

    private boolean canMakeRequest() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastRequestTime < MIN_REQUEST_INTERVAL) {
            return false;
        }

        if (currentTime - currentSecondStart > 1000) {
            currentSecondStart = currentTime;
            requestsThisSecond = 0;
        }

        return requestsThisSecond < MAX_REQUESTS_PER_SECOND;
    }

    private void recordRequest() {
        lastRequestTime = System.currentTimeMillis();
        requestsThisSecond++;
    }

    private String getPistonLanguage(String language) {
        switch (language.toLowerCase()) {
            case "python":
            case "python3":
                return "python";
            case "java":
                return "java";
            case "cpp":
            case "c++":
                return "c++";
            case "c":
                return "c";
            case "javascript":
            case "nodejs":
                return "javascript";
            default:
                return language.toLowerCase();
        }
    }

    /**
     * Load previously saved user answers from Firebase
     */
    private void loadPreviousAnswers(String lessonId) {
        DatabaseReference userAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userExerciseAnswers")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId);

        userAnswersRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                    try {
                        int exerciseIndex = Integer.parseInt(exerciseSnap.getKey());
                        String savedCode = exerciseSnap.child("code").getValue(String.class);
                        String savedOutput = exerciseSnap.child("output").getValue(String.class);
                        Boolean isCorrect = exerciseSnap.child("isCorrect").getValue(Boolean.class);

                        if (savedCode != null && savedOutput != null && isCorrect != null) {
                            submissionCache.put(exerciseIndex,
                                    new Submission(savedCode, savedOutput, isCorrect));
                        }
                    } catch (NumberFormatException e) {
                        Log.e("ExerciseActivity", "Invalid exercise index: " + exerciseSnap.getKey());
                    }
                }

                // Reload current exercise to show saved answer if exists
                if (submissionCache.containsKey(currentIndex)) {
                    loadExercise(lessonId);
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("ExerciseActivity", "Failed to load previous answers: " + e.getMessage());
        });
    }

    private void loadExercise(String finalLessonId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("coding_exercises/" + finalLessonId +"/"+ currentIndex);

        ref.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists() || snapshot.getValue() == null) {
                Toast.makeText(this, "Exercise not found", Toast.LENGTH_SHORT).show();
                return;
            }

            exercise = snapshot.getValue(Exercise.class);
            if (exercise != null) {

                exerciseTitle.setText(exercise.title);

                // Check if there's a saved answer for this exercise
                if (submissionCache.containsKey(currentIndex)) {
                    Submission prev = submissionCache.get(currentIndex);
                    if (prev != null) {
                        codeEditor.setText(prev.code);
                        outputBox.setText(prev.output);

                        if (prev.isCorrect) {
                            outputBox.setTextColor(Color.parseColor("#06651A"));
                            nextCode.setEnabled(true);
                            nextCode.setAlpha(1f);
                        } else {
                            outputBox.setTextColor(Color.parseColor("#E31414"));
                        }
                    }
                } else {
                    // Load default code
                    String formattedCode = exercise.code
                            .replace("&nbsp;", " ")
                            .replace("&emsp;", "\t")
                            .replace("<br>", "\n");

                    codeEditor.setText(formattedCode);
                    outputBox.setText("");
                }

                expectedOutputBox.setText(exercise.expectedOutput);

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
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading exercise: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void updateProgress() {
        if (totalExercises == 0) return;

        float progress = ((float) currentIndex - 1) / totalExercises;
        int percent = Math.round(progress * 100);

        progressBar.setProgress(percent);
        progressPercent.setText(percent + "%");
    }

    private void runCode() {
        if (exercise == null) return;

        String userCode = codeEditor.getText().toString();

        if (submissionCache.containsKey(currentIndex)) {
            Submission prev = submissionCache.get(currentIndex);
            if (prev != null && prev.code.equals(userCode)) {
                outputBox.setText(prev.output);
                checkAnswer(prev.output);
                return;
            }
        }

        recordRequest();

        PistonService api = RetrofitClient.getPiston();

        String pistonLanguage = getPistonLanguage(exercise.language);
        String version = "*";

        PistonExecuteRequest req = new PistonExecuteRequest(
                pistonLanguage,
                version,
                userCode
        );

        api.execute(req).enqueue(new Callback<PistonExecuteResponse>() {
            @Override
            public void onResponse(Call<PistonExecuteResponse> call,
                                   Response<PistonExecuteResponse> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 429) {
                        outputBox.setTextColor(Color.parseColor("#E31414"));
                        outputBox.setText("Rate limit exceeded. Please wait a moment.\n\n" +
                                "Tip: Code is cached - only run when you've made changes!");
                    } else {
                        outputBox.setText("HTTP Error: " + response.code());
                    }
                    return;
                }

                PistonExecuteResponse res = response.body();
                if (res == null) {
                    outputBox.setText("Error: No response from server");
                    return;
                }

                String actual = res.getOutput();
                outputBox.setText(actual);

                boolean isCorrect = false;
                if (res.isSuccess()) {
                    isCorrect = checkAnswer(actual);
                } else {
                    outputBox.setTextColor(Color.parseColor("#E31414"));
                }

                // Save submission to cache
                submissionCache.put(currentIndex, new Submission(userCode, actual, isCorrect));

                // Save to Firebase for future review
                saveUserAnswer(currentIndex, userCode, actual, isCorrect);
            }

            @Override
            public void onFailure(Call<PistonExecuteResponse> call, Throwable t) {
                outputBox.setTextColor(Color.parseColor("#E31414"));
                outputBox.setText("Error: " + t.getMessage());
            }
        });
    }

    /**
     * Save user's answer to Firebase for future review
     */
    private void saveUserAnswer(int exerciseIndex, String code, String output, boolean isCorrect) {
        DatabaseReference userAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userExerciseAnswers")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId)
                .child(String.valueOf(exerciseIndex));

        HashMap<String, Object> answerData = new HashMap<>();
        answerData.put("code", code);
        answerData.put("output", output);
        answerData.put("isCorrect", isCorrect);
        answerData.put("timestamp", ServerValue.TIMESTAMP);

        userAnswersRef.setValue(answerData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ExerciseActivity", "Answer saved successfully for exercise " + exerciseIndex);
                })
                .addOnFailureListener(e -> {
                    Log.e("ExerciseActivity", "Failed to save answer: " + e.getMessage());
                });
    }

    private boolean checkAnswer(String actual) {
        String expected = exercise.expectedOutput;
        boolean isCorrect = actual.trim().equals(expected.trim());

        if (isCorrect) {
            outputBox.setTextColor(Color.parseColor("#06651A"));
            nextCode.setEnabled(true);
            nextCode.setAlpha(1f);
            return true;
        } else {
            outputBox.setTextColor(Color.parseColor("#E31414"));
            return false;
        }
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
        // Calculate correct answers
        correctExercises = 0;
        for (Map.Entry<Integer, Submission> entry : submissionCache.entrySet()) {
            Submission submission = entry.getValue();
            if (submission.isCorrect) {
                correctExercises++;
            }
        }

        int totalAttempted = submissionCache.size();
        boolean allCorrect = (correctExercises == totalExercises);

        // Save exercise completion to Firebase
        DatabaseReference exerciseResultsRef = FirebaseDatabase.getInstance()
                .getReference("exerciseResults")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId);

        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("completed", allCorrect);
        resultData.put("correctCount", correctExercises);
        resultData.put("totalExercises", totalExercises);
        resultData.put("timestamp", ServerValue.TIMESTAMP);

        exerciseResultsRef.setValue(resultData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            allCorrect ? "All exercises completed!" :
                                    "Submitted! You got " + correctExercises + "/" + totalExercises + " correct.",
                            Toast.LENGTH_LONG).show();

                    // Return to LessonActivity with result flag
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("exercisesCompleted", allCorrect);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save results: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    public static class Submission {
        public String code, output;
        public boolean isCorrect;

        public Submission() {}

        public Submission(String code, String output, boolean isCorrect) {
            this.code = code;
            this.output = output;
            this.isCorrect = isCorrect;
        }
    }
}