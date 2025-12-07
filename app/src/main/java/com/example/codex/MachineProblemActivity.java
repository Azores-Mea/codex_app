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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MachineProblemActivity extends AppCompatActivity {

    EditText codeEditor;
    Button runButton;
    MaterialButton submitButton;
    TextView outputBox, exerciseTitle, expectedOutputBox, descriptionBox;
    Exercise exercise;

    Handler cooldownHandler = new Handler();

    private static final int MIN_REQUEST_INTERVAL = 250;
    private long lastRequestTime = 0;

    private int requestsThisSecond = 0;
    private long currentSecondStart = 0;
    private static final int MAX_REQUESTS_PER_SECOND = 4;

    private SessionManager sessionManager;
    private String lessonId;
    private String lastOutput = "";
    private boolean isCorrect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machine_problem);

        sessionManager = new SessionManager(this);

        codeEditor = findViewById(R.id.codeEditor);
        runButton = findViewById(R.id.runButton);
        outputBox = findViewById(R.id.outputBox);
        exerciseTitle = findViewById(R.id.exerciseTitle);
        expectedOutputBox = findViewById(R.id.expectedOutputBox);
        descriptionBox = findViewById(R.id.descriptionBox);
        submitButton = findViewById(R.id.submitButton);

        descriptionBox.setMovementMethod(new ScrollingMovementMethod());

        submitButton.setEnabled(false);
        submitButton.setAlpha(0.4f);

        lessonId = getIntent().getStringExtra("lessonId");
        if (lessonId == null || lessonId.isEmpty()) {
            lessonId = "L1";
        }

        loadExercise(lessonId);
        loadPreviousAnswer(lessonId);

        runButton.setOnClickListener(v -> {
            if (!canMakeRequest()) {
                Toast.makeText(this,
                        "Please wait a moment before running again",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            runButton.setEnabled(false);
            cooldownHandler.postDelayed(() -> runButton.setEnabled(true), MIN_REQUEST_INTERVAL);

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(codeEditor.getWindowToken(), 0);
            codeEditor.clearFocus();

            outputBox.setTextColor(Color.parseColor("#000000"));
            outputBox.setText("Compiling...");

            runCode();
        });

        submitButton.setOnClickListener(v -> {
            showSubmitConfirmationDialog();
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

    private void loadPreviousAnswer(String lessonId) {
        DatabaseReference userAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userMachineProblemAnswers")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId);

        userAnswersRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String savedCode = snapshot.child("code").getValue(String.class);
                String savedOutput = snapshot.child("output").getValue(String.class);
                Boolean savedIsCorrect = snapshot.child("isCorrect").getValue(Boolean.class);

                if (savedCode != null) {
                    codeEditor.setText(savedCode);
                }
                if (savedOutput != null) {
                    outputBox.setText(savedOutput);
                    lastOutput = savedOutput;
                }
                if (savedIsCorrect != null) {
                    isCorrect = savedIsCorrect;
                    if (isCorrect) {
                        outputBox.setTextColor(Color.parseColor("#06651A"));
                        submitButton.setEnabled(true);
                        submitButton.setAlpha(1f);
                    } else {
                        outputBox.setTextColor(Color.parseColor("#E31414"));
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("MachineProblemActivity", "Failed to load previous answer: " + e.getMessage());
        });
    }

    private void loadExercise(String lessonId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("assessment/" + lessonId + "/MachineProblem");

        ref.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(this, "Machine problem not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Get first non-null entry (skip index 0 if it's null)
            Exercise foundExercise = null;
            for (DataSnapshot child : snapshot.getChildren()) {
                Exercise ex = child.getValue(Exercise.class);
                if (ex != null) {
                    foundExercise = ex;
                    break;
                }
            }

            if (foundExercise == null) {
                Toast.makeText(this, "No valid machine problem found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            exercise = foundExercise;

            exerciseTitle.setText(exercise.title);

            String formattedCode = exercise.code
                    .replace("&nbsp;", " ")
                    .replace("&emsp;", "\t")
                    .replace("<br>", "\n");

            if (codeEditor.getText().toString().isEmpty()) {
                codeEditor.setText(formattedCode);
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

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading machine problem: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void runCode() {
        if (exercise == null) return;

        String userCode = codeEditor.getText().toString();

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
                lastOutput = actual;

                if (res.isSuccess()) {
                    isCorrect = checkAnswer(actual);
                } else {
                    outputBox.setTextColor(Color.parseColor("#E31414"));
                    isCorrect = false;
                }

                saveUserAnswer(userCode, actual, isCorrect);
            }

            @Override
            public void onFailure(Call<PistonExecuteResponse> call, Throwable t) {
                outputBox.setTextColor(Color.parseColor("#E31414"));
                outputBox.setText("Error: " + t.getMessage());
            }
        });
    }

    private void saveUserAnswer(String code, String output, boolean isCorrect) {
        DatabaseReference userAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userMachineProblemAnswers")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId);

        HashMap<String, Object> answerData = new HashMap<>();
        answerData.put("code", code);
        answerData.put("output", output);
        answerData.put("isCorrect", isCorrect);
        answerData.put("timestamp", ServerValue.TIMESTAMP);

        userAnswersRef.setValue(answerData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("MachineProblemActivity", "Answer saved successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("MachineProblemActivity", "Failed to save answer: " + e.getMessage());
                });
    }

    private boolean checkAnswer(String actual) {
        String expected = exercise.expectedOutput;
        boolean correct = actual.trim().equals(expected.trim());

        if (correct) {
            outputBox.setTextColor(Color.parseColor("#06651A"));
            submitButton.setEnabled(true);
            submitButton.setAlpha(1f);
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
        title.setText("Submit machine problem?");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        dialogMessage.setText(Html.fromHtml("Once submitted, you cannot make any changes.", Html.FROM_HTML_MODE_LEGACY));

        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        btnNo.setText("Cancel");
        btnYes.setText("Submit");

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            submitMachineProblem();
        });
    }

    private void submitMachineProblem() {
        DatabaseReference machineResultsRef = FirebaseDatabase.getInstance()
                .getReference("machineProblemResults")
                .child(String.valueOf(sessionManager.getUserId()))
                .child(lessonId);

        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("completed", isCorrect);
        resultData.put("timestamp", ServerValue.TIMESTAMP);

        machineResultsRef.setValue(resultData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            isCorrect ? "Machine problem completed!" : "Submitted!",
                            Toast.LENGTH_LONG).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("machineProblemCompleted", isCorrect);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save result: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}