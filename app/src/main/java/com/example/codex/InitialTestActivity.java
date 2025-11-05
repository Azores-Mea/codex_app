package com.example.codex;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class InitialTestActivity extends AppCompatActivity {

    private TextView progressPercent, progressQuestion, questionNumber, qNum, question;
    private ProgressBar progressBar;
    private MaterialCardView choiceA, choiceB, choiceC, choiceD;
    private TextView choiceAText, choiceBText, choiceCText, choiceDText;
    private TextView choiceALetter, choiceBLetter, choiceCLetter, choiceDLetter;
    private LinearLayout choiceABg, choiceBBg, choiceCBg, choiceDBg;
    private MaterialButton prev, next;

    private List<HashMap<String, String>> questionList = new ArrayList<>();
    private HashMap<Integer, String> userAnswers = new HashMap<>();
    private int currentIndex = 0;

    private DatabaseReference quizResultsRef;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialtest);

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        Log.d("InitialTestActivity", "UserId = " + userId);

        quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");

        initViews();
        loadQuestionsFromFirebase();

        prev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                loadQuestion(currentIndex);
            }
        });

        next.setOnClickListener(v -> {
            if (currentIndex < questionList.size() - 1) {
                currentIndex++;
                loadQuestion(currentIndex);
            } else {
                showConfirmationDialog();
            }
        });

        setChoiceListeners();
    }

    private void initViews() {
        progressPercent = findViewById(R.id.progressPercent);
        progressBar = findViewById(R.id.progressBar);
        progressQuestion = findViewById(R.id.progressQuestion);
        questionNumber = findViewById(R.id.questionNumber);
        qNum = findViewById(R.id.qNum);
        question = findViewById(R.id.question);

        choiceA = findViewById(R.id.choiceA);
        choiceB = findViewById(R.id.choiceB);
        choiceC = findViewById(R.id.choiceC);
        choiceD = findViewById(R.id.choiceD);

        choiceAText = findViewById(R.id.choiceAText);
        choiceBText = findViewById(R.id.choiceBText);
        choiceCText = findViewById(R.id.choiceCText);
        choiceDText = findViewById(R.id.choiceDText);

        choiceALetter = findViewById(R.id.choiceALetter);
        choiceBLetter = findViewById(R.id.choiceBLetter);
        choiceCLetter = findViewById(R.id.choiceCLetter);
        choiceDLetter = findViewById(R.id.choiceDLetter);

        choiceABg = findViewById(R.id.choiceABg);
        choiceBBg = findViewById(R.id.choiceBBg);
        choiceCBg = findViewById(R.id.choiceCBg);
        choiceDBg = findViewById(R.id.choiceDBg);

        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);
    }

    private void loadQuestionsFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("quizQuestions");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questionList.clear();
                for (DataSnapshot questionSnap : snapshot.getChildren()) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("question", questionSnap.child("question").getValue(String.class));
                    map.put("choiceA", questionSnap.child("choiceA").getValue(String.class));
                    map.put("choiceB", questionSnap.child("choiceB").getValue(String.class));
                    map.put("choiceC", questionSnap.child("choiceC").getValue(String.class));
                    map.put("choiceD", questionSnap.child("choiceD").getValue(String.class));
                    map.put("correctAnswer", questionSnap.child("correctAnswer").getValue(String.class));
                    questionList.add(map);
                }

                if (!questionList.isEmpty()) {
                    currentIndex = 0;
                    loadQuestion(0);
                } else {
                    Log.w("InitialTestActivity", "No quiz questions found in Firebase.");
                    Toast.makeText(InitialTestActivity.this, "No questions available.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("InitialTestActivity", "Failed to load questions: " + error.getMessage(), error.toException());
                Toast.makeText(InitialTestActivity.this, "Failed to load questions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestion(int index) {
        currentIndex = index;

        if (questionList == null || questionList.isEmpty() || index < 0 || index >= questionList.size()) {
            Log.w("InitialTestActivity", "loadQuestion called with invalid index: " + index);
            return;
        }

        HashMap<String, String> q = questionList.get(index);

        String qText = q.get("question") != null ? q.get("question") : "";
        String a = q.get("choiceA") != null ? q.get("choiceA") : "";
        String b = q.get("choiceB") != null ? q.get("choiceB") : "";
        String c = q.get("choiceC") != null ? q.get("choiceC") : "";
        String d = q.get("choiceD") != null ? q.get("choiceD") : "";

        question.setText(Html.fromHtml(qText));
        choiceAText.setText(Html.fromHtml(a));
        choiceBText.setText(Html.fromHtml(b));
        choiceCText.setText(Html.fromHtml(c));
        choiceDText.setText(Html.fromHtml(d));

        qNum.setText((index + 1) + ".");
        questionNumber.setText("Question " + (index + 1) + " of " + questionList.size());
        progressQuestion.setText((index + 1) + " of " + questionList.size() + " Questions");

        progressBar.setMax(100);
        int percent = (int) (((index) / (float) questionList.size()) * 100);
        progressBar.setProgress(percent);
        progressPercent.setText(percent + "%");

        resetChoiceStyles();

        if (userAnswers.containsKey(index)) {
            highlightChoice(userAnswers.get(index));
        }

        prev.setEnabled(index > 0);
        prev.setAlpha(index > 0 ? 1f : 0.6f);
        prev.setBackgroundTintList(ColorStateList.valueOf(
                index > 0 ? Color.parseColor("#03162A") : Color.parseColor("#BDBDBD")));
        prev.setTextColor(index > 0 ? Color.WHITE : Color.BLACK);

        if (userAnswers.containsKey(index)) {
            enableNextButton();
        } else {
            disableNextButton();
        }

        next.setText(index == questionList.size() - 1 ? "Submit" : "Next");
    }

    private void setChoiceListeners() {
        choiceA.setOnClickListener(v -> selectAnswer("A"));
        choiceB.setOnClickListener(v -> selectAnswer("B"));
        choiceC.setOnClickListener(v -> selectAnswer("C"));
        choiceD.setOnClickListener(v -> selectAnswer("D"));
    }

    private void selectAnswer(String letter) {
        userAnswers.put(currentIndex, letter);
        highlightChoice(letter);
        enableNextButton();
    }

    private void highlightChoice(String letter) {
        resetChoiceStyles();
        switch (letter) {
            case "A": setSelected(choiceA, choiceAText, choiceALetter, choiceABg); break;
            case "B": setSelected(choiceB, choiceBText, choiceBLetter, choiceBBg); break;
            case "C": setSelected(choiceC, choiceCText, choiceCLetter, choiceCBg); break;
            case "D": setSelected(choiceD, choiceDText, choiceDLetter, choiceDBg); break;
        }
    }

    private void resetChoiceStyles() {
        resetChoice(choiceA, choiceAText, choiceALetter, choiceABg);
        resetChoice(choiceB, choiceBText, choiceBLetter, choiceBBg);
        resetChoice(choiceC, choiceCText, choiceCLetter, choiceCBg);
        resetChoice(choiceD, choiceDText, choiceDLetter, choiceDBg);
    }

    private void resetChoice(MaterialCardView card, TextView label, TextView letter, LinearLayout bg) {
        card.setStrokeColor(ContextCompat.getColor(this, R.color.secondary));
        bg.setBackgroundColor(ContextCompat.getColor(this, R.color.bgText));
        label.setTextColor(ContextCompat.getColor(this, R.color.secondary));
        letter.setBackgroundResource(R.drawable.choice_circle_background);
        letter.setTextColor(ContextCompat.getColor(this, R.color.secondary));
    }

    private void setSelected(MaterialCardView card, TextView label, TextView letter, LinearLayout bg) {
        card.setStrokeColor(ContextCompat.getColor(this, R.color.primary));
        bg.setBackgroundColor(Color.parseColor("#D9D9D9"));
        label.setTextColor(ContextCompat.getColor(this, R.color.black));
        letter.setBackgroundResource(R.drawable.choice_circle_selected);
        letter.setTextColor(ContextCompat.getColor(this, R.color.bgText));
    }

    private void enableNextButton() {
        next.setEnabled(true);
        next.setAlpha(1f);
        next.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));
        next.setTextColor(Color.WHITE);
    }

    private void disableNextButton() {
        next.setEnabled(false);
        next.setAlpha(0.6f);
        next.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
        next.setTextColor(Color.BLACK);
    }

    private String getClassification(int percentage) {
        if (percentage <= 40) return "Beginner";
        else if (percentage <= 70) return "Intermediate";
        else return "Advanced";
    }

    private void showConfirmationDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // show only if activity is valid
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
            Log.w("InitialTestActivity", "Activity finishing/destroyed - cannot show confirmation dialog");
            return;
        }

        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Submit test?");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        dialogMessage.setText(Html.fromHtml("Once submitted, you cannot make any changes.", Html.FROM_HTML_MODE_LEGACY));

        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        btnNo.setText("Review Answer");
        btnYes.setText("Submit");

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            submitQuizResults();
        });
    }

    private void submitQuizResults() {
        int totalQuestions = questionList == null ? 0 : questionList.size();
        if (totalQuestions == 0) {
            Toast.makeText(this, "Cannot submit: no questions loaded.", Toast.LENGTH_SHORT).show();
            Log.e("InitialTestActivity", "submitQuizResults called with totalQuestions = 0");
            return;
        }

        int score = calculateScore();
        int percentage = (int) (((float) score / totalQuestions) * 100);
        String newClassification = getClassification(percentage);

        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("userId", userId);
        resultData.put("score", score);
        resultData.put("totalQuestions", totalQuestions);
        resultData.put("percentage", percentage);
        resultData.put("classification", newClassification);

        HashMap<String, Object> answersMap = new HashMap<>();
        for (Integer index : userAnswers.keySet()) {
            answersMap.put(String.valueOf(index), userAnswers.get(index));
        }
        resultData.put("answers", answersMap);

        // Save results under quizResults/{userId} (note: will overwrite previous; if you want history, use push())
        quizResultsRef.child(String.valueOf(userId)).setValue(resultData)
                .addOnSuccessListener(aVoid -> {
                    saveRecentActivity(score, totalQuestions);

                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("Users")
                            .child(String.valueOf(userId));

                    userRef.child("classification").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String currentClassification = snapshot.getValue(String.class);

                            if (currentClassification == null || !currentClassification.equals(newClassification)) {
                                HashMap<String, Object> updates = new HashMap<>();
                                updates.put("classification", newClassification);

                                userRef.updateChildren(updates)
                                        .addOnSuccessListener(unused -> {
                                            // ensure running on UI thread
                                            runOnUiThread(() -> showFinalConfirmationDialog(score, totalQuestions, newClassification));
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("InitialTestActivity", "Failed to update classification: " + e.getMessage(), e);
                                            runOnUiThread(() ->
                                                    Toast.makeText(InitialTestActivity.this, "Failed to update classification: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                        });
                            } else {
                                runOnUiThread(() -> showFinalConfirmationDialog(score, totalQuestions, newClassification));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("InitialTestActivity", "Failed to check user classification: " + error.getMessage(), error.toException());
                            runOnUiThread(() -> Toast.makeText(InitialTestActivity.this, "Failed to check user classification", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("InitialTestActivity", "Quiz submission failed: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to submit quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveRecentActivity(int score, int totalQuestions) {
        DatabaseReference recentActRef = FirebaseDatabase.getInstance()
                .getReference("RecentAct")
                .child(String.valueOf(userId));

        HashMap<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("title", "Basic Java Proficiency Test");
        record.put("type", "InitialTest");
        record.put("score", score);
        record.put("items", totalQuestions);
        record.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        recentActRef.push().setValue(record)
                .addOnFailureListener(e -> Log.e("FirebaseError", "Failed to save RecentAct", e));
    }

    private int calculateScore() {
        int score = 0;
        for (int i = 0; i < questionList.size(); i++) {
            String correct = questionList.get(i).get("correctAnswer");
            String userAns = userAnswers.get(i);
            if (userAns != null && correct != null && userAns.equalsIgnoreCase(correct)) {
                score++;
            }
        }
        return score;
    }

    private void showFinalConfirmationDialog(int score, int totalQuestions, String classification) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogue_initial_test, null);

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
            Log.w("InitialTestActivity", "Activity finishing/destroyed - cannot show final dialog");
            startActivity(new Intent(InitialTestActivity.this, Navigation_ActivityLearner.class));
            finish();
            return;
        }

        // Bind views
        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView classText = dialogView.findViewById(R.id.classText);
        TextView scoreText = dialogView.findViewById(R.id.score);
        MaterialButton btnContinue = dialogView.findViewById(R.id.yes_btn);

        // Classification card elements
        MaterialCardView classCard = dialogView.findViewById(R.id.classification);
        LinearLayout classBg = dialogView.findViewById(R.id.classBg);
        TextView className = dialogView.findViewById(R.id.className);
        TextView percent = dialogView.findViewById(R.id.percent);

        // Apply gradient to title
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        // Calculate percentage
        int percentage = (int) (((float) score / totalQuestions) * 100);
        scoreText.setText("Score: " + score + "/" + totalQuestions);
        percent.setText(percentage + "%");

        // Colors based on classification
        int colorText;
        int colorStroke;
        String bgColor;

        if (classification.equals("Beginner")) {
            colorText = Color.parseColor("#E3AF64");
            colorStroke = Color.parseColor("#E3AF64");
            bgColor = "#FFF4E3";
            classText.setText("Every expert starts somewhere — keep learning!");
        } else if (classification.equals("Intermediate")) {
            colorText = Color.parseColor("#66ABF4");
            colorStroke = Color.parseColor("#66ABF4");
            bgColor = "#E3F1FF";
            classText.setText("You’re getting better — stay consistent and curious!");
        } else {
            colorText = Color.parseColor("#A666F4");
            colorStroke = Color.parseColor("#A666F4");
            bgColor = "#F3E8FF";
            classText.setText("Excellent work — you’ve mastered this test!");
        }

        // Apply colors dynamically
        className.setText(classification);
        className.setTextColor(colorText);
        percent.setTextColor(colorText);
        classCard.setStrokeColor(colorStroke);
        classBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(bgColor)));

        // Continue button
        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(InitialTestActivity.this, Navigation_ActivityLearner.class);
            startActivity(intent);
            finish();
        });
    }
}
