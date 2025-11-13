package com.example.codex;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
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

    // Timer view
    private TextView timerText;

    private List<HashMap<String, String>> questionList = new ArrayList<>();
    private HashMap<Integer, String> userAnswers = new HashMap<>();
    private int currentIndex = 0;

    private DatabaseReference quizResultsRef;
    private int userId;

    // Timer fields
    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME_MILLIS = 45L * 60L * 1000L; // 45 minutes
    private long timeLeftMillis = TOTAL_TIME_MILLIS;
    private long endTimeMillis = 0L;

    private static final String PREFS_NAME = "InitialTestTimerPrefs";
    // key will be PREF_END_TIME_USER_<userId>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialtest);

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        Log.d("InitialTestActivity", "UserId = " + userId);

        quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");

        initViews();

        // 🔄 Always reset timer when activity restarts
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove("PREF_END_TIME_USER_" + userId)
                .apply();

        // Load or start timer
        restoreOrStartTimer();

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

        // Timer textview (make sure to add this in your layout - id: timerText)
        timerText = findViewById(R.id.timeText);
    }

    private void loadQuestionsFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("quizQuestions");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<HashMap<String, String>> beginnerList = new ArrayList<>();
                List<HashMap<String, String>> intermediateList = new ArrayList<>();
                List<HashMap<String, String>> advancedList = new ArrayList<>();

                // Traverse classifications
                for (DataSnapshot levelSnap : snapshot.getChildren()) {
                    String classification = levelSnap.getKey(); // Beginner, Intermediate, Advanced

                    for (DataSnapshot lessonSnap : levelSnap.getChildren()) {
                        for (DataSnapshot questionSnap : lessonSnap.getChildren()) {
                            HashMap<String, String> q = new HashMap<>();
                            q.put("question", questionSnap.child("question").getValue(String.class));
                            q.put("choiceA", questionSnap.child("choiceA").getValue(String.class));
                            q.put("choiceB", questionSnap.child("choiceB").getValue(String.class));
                            q.put("choiceC", questionSnap.child("choiceC").getValue(String.class));
                            q.put("choiceD", questionSnap.child("choiceD").getValue(String.class));
                            q.put("correctAnswer", questionSnap.child("answer").getValue(String.class));
                            q.put("classification", classification); // track classification
                            q.put("lessonId", lessonSnap.getKey());

                            switch (classification) {
                                case "Beginner":
                                    beginnerList.add(q);
                                    break;
                                case "Intermediate":
                                    intermediateList.add(q);
                                    break;
                                case "Advanced":
                                    advancedList.add(q);
                                    break;
                            }
                        }
                    }
                }

                // Now build 30-item test
                int beginnerCount = Math.round(30 * 0.25f); // 7 or 8
                int intermediateCount = Math.round(30 * 0.35f); // ~10
                int advancedCount = 30 - beginnerCount - intermediateCount; // rest (~12)

                questionList.clear();
                questionList.addAll(getRandomSubset(beginnerList, beginnerCount));
                questionList.addAll(getRandomSubset(intermediateList, intermediateCount));
                questionList.addAll(getRandomSubset(advancedList, advancedCount));

                if (!questionList.isEmpty()) {
                    currentIndex = 0;
                    loadQuestion(0);
                } else {
                    Toast.makeText(InitialTestActivity.this, "No questions available.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(InitialTestActivity.this, "Failed to load questions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper function to randomly pick N items
    private List<HashMap<String, String>> getRandomSubset(List<HashMap<String, String>> list, int count) {
        List<HashMap<String, String>> copy = new ArrayList<>(list);
        List<HashMap<String, String>> result = new ArrayList<>();
        java.util.Collections.shuffle(copy);
        for (int i = 0; i < Math.min(count, copy.size()); i++) {
            result.add(copy.get(i));
        }
        return result;
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

    // NEW: Classification based on difficulty level performance
    private String getClassification(int beginnerScore, int beginnerTotal,
                                     int intermediateScore, int intermediateTotal,
                                     int advancedScore, int advancedTotal) {
        // Calculate passing threshold (60% for each level)
        boolean passedBeginner = beginnerTotal > 0 &&
                (beginnerScore * 100.0 / beginnerTotal) >= 60;
        boolean passedIntermediate = intermediateTotal > 0 &&
                (intermediateScore * 100.0 / intermediateTotal) >= 60;
        boolean passedAdvanced = advancedTotal > 0 &&
                (advancedScore * 100.0 / advancedTotal) >= 60;

        // Classification logic:
        // Advanced: Must pass all three levels
        if (passedBeginner && passedIntermediate && passedAdvanced) {
            return "Advanced";
        }
        // Intermediate: Must pass Beginner and Intermediate (but not Advanced)
        else if (passedBeginner && passedIntermediate) {
            return "Intermediate";
        }
        // Beginner: Default classification (failed one or more required levels)
        else {
            return "Beginner";
        }
    }

    // NEW: Calculate detailed scores by difficulty level
    private HashMap<String, Integer> calculateDetailedScore() {
        int beginnerScore = 0, beginnerTotal = 0;
        int intermediateScore = 0, intermediateTotal = 0;
        int advancedScore = 0, advancedTotal = 0;
        int totalScore = 0;

        for (int i = 0; i < questionList.size(); i++) {
            HashMap<String, String> q = questionList.get(i);
            String classification = q.get("classification");
            String correct = q.get("correctAnswer");
            String userAns = userAnswers.get(i);

            boolean isCorrect = userAns != null && correct != null &&
                    userAns.equalsIgnoreCase(correct);

            if (classification != null) {
                switch (classification) {
                    case "Beginner":
                        beginnerTotal++;
                        if (isCorrect) {
                            beginnerScore++;
                            totalScore++;
                        }
                        break;
                    case "Intermediate":
                        intermediateTotal++;
                        if (isCorrect) {
                            intermediateScore++;
                            totalScore++;
                        }
                        break;
                    case "Advanced":
                        advancedTotal++;
                        if (isCorrect) {
                            advancedScore++;
                            totalScore++;
                        }
                        break;
                }
            }
        }

        HashMap<String, Integer> scores = new HashMap<>();
        scores.put("totalScore", totalScore);
        scores.put("beginnerScore", beginnerScore);
        scores.put("beginnerTotal", beginnerTotal);
        scores.put("intermediateScore", intermediateScore);
        scores.put("intermediateTotal", intermediateTotal);
        scores.put("advancedScore", advancedScore);
        scores.put("advancedTotal", advancedTotal);

        return scores;
    }

    // OLD: Keep for backward compatibility if needed
    private int calculateScore() {
        return calculateDetailedScore().get("totalScore");
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
        // stop timer and clear stored end time because user submitted
        cancelTimerAndClearPref();

        int totalQuestions = questionList.size();
        if (totalQuestions == 0) {
            Toast.makeText(this, "Cannot submit: no questions loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get detailed scores
        HashMap<String, Integer> scoreDetails = calculateDetailedScore();
        int score = scoreDetails.get("totalScore");
        int percentage = (int) (((float) score / totalQuestions) * 100);

        // NEW: Classification based on difficulty levels
        String classification = getClassification(
                scoreDetails.get("beginnerScore"), scoreDetails.get("beginnerTotal"),
                scoreDetails.get("intermediateScore"), scoreDetails.get("intermediateTotal"),
                scoreDetails.get("advancedScore"), scoreDetails.get("advancedTotal")
        );

        // Determine if user passed (overall 60% or based on classification)
        String passed = percentage >= 60 ? "Passed" : "Failed";

        // Convert userAnswers to simple array
        List<String> answersList = new ArrayList<>();
        for (int i = 0; i < totalQuestions; i++) {
            answersList.add(userAnswers.getOrDefault(i, ""));
        }

        // compute time info
        long startTime = (endTimeMillis == 0) ? System.currentTimeMillis() - (TOTAL_TIME_MILLIS - timeLeftMillis)
                : (endTimeMillis - TOTAL_TIME_MILLIS);
        long timeFinishedAt = System.currentTimeMillis();
        long timeSpentMillis = timeFinishedAt - startTime; // elapsed
        String timeSpentFormatted = formatMillisToMinutesSeconds(timeSpentMillis);

        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("userId", userId);
        resultData.put("lessonId", "ITest1");
        resultData.put("quizType", "InitialTest");
        resultData.put("answers", answersList);
        resultData.put("score", score);
        resultData.put("total", totalQuestions);
        resultData.put("classification", classification);
        // NEW: Add detailed scores to Firebase
        resultData.put("beginnerScore", scoreDetails.get("beginnerScore"));
        resultData.put("beginnerTotal", scoreDetails.get("beginnerTotal"));
        resultData.put("intermediateScore", scoreDetails.get("intermediateScore"));
        resultData.put("intermediateTotal", scoreDetails.get("intermediateTotal"));
        resultData.put("advancedScore", scoreDetails.get("advancedScore"));
        resultData.put("advancedTotal", scoreDetails.get("advancedTotal"));
        resultData.put("passed", passed);
        resultData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        // timer fields
        resultData.put("timeFinishedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timeFinishedAt)));
        resultData.put("timeSpentMillis", timeSpentMillis);
        resultData.put("timeSpentFormatted", timeSpentFormatted);

        FirebaseDatabase.getInstance().getReference("quizResults").push().setValue(resultData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Quiz submitted successfully!", Toast.LENGTH_SHORT).show();
                    // Update user classification in database
                    updateUserClassification(classification);
                    // also save RecentAct
                    saveRecentActivity(score, totalQuestions, timeSpentMillis, timeSpentFormatted);
                    showFinalConfirmationDialog(score, totalQuestions, classification);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to submit quiz.", Toast.LENGTH_SHORT).show());
    }


    private void saveRecentActivity(int score, int totalQuestions, long timeSpentMillis, String timeSpentFormatted) {
        DatabaseReference recentActRef = FirebaseDatabase.getInstance()
                .getReference("RecentAct")
                .child(String.valueOf(userId));

        HashMap<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("title", "Basic Java Proficiency Test");
        record.put("type", "InitialTest");
        record.put("score", score);
        record.put("items", totalQuestions);
        record.put("timeSpentMillis", timeSpentMillis);
        record.put("timeSpentFormatted", timeSpentFormatted);
        record.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        recentActRef.push().setValue(record)
                .addOnFailureListener(e -> Log.e("FirebaseError", "Failed to save RecentAct", e));
    }

    private void updateUserClassification(String classification) {
        // Update in Users array at index userId
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(String.valueOf(userId));

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("classification", classification);

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("InitialTestActivity", "User classification updated to: " + classification);
                    // Also update in SessionManager to keep local session in sync
                    SessionManager sessionManager = new SessionManager(InitialTestActivity.this);
                    sessionManager.setClassification(classification);
                })
                .addOnFailureListener(e -> Log.e("FirebaseError", "Failed to update user classification", e));
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
            classText.setText("You're getting better — stay consistent and curious!");
        } else {
            colorText = Color.parseColor("#A666F4");
            colorStroke = Color.parseColor("#A666F4");
            bgColor = "#F3E8FF";
            classText.setText("Excellent work — you've mastered this test!");
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

    // ---------- Timer related methods ----------

    private void restoreOrStartTimer() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String key = "PREF_END_TIME_USER_" + userId;
        endTimeMillis = prefs.getLong(key, 0L);

        if (endTimeMillis == 0L) {
            // no existing end time -> start new timer
            endTimeMillis = System.currentTimeMillis() + TOTAL_TIME_MILLIS;
            prefs.edit().putLong(key, endTimeMillis).apply();
        }

        long millisLeft = endTimeMillis - System.currentTimeMillis();
        if (millisLeft <= 0L) {
            // time already up
            timeLeftMillis = 0L;
            timerExpired();
        } else {
            timeLeftMillis = millisLeft;
            startCountDown(timeLeftMillis);
        }
    }

    private void startCountDown(long startMillis) {
        // cancel existing if any
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(startMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                timerText.setText(formatMillisToMinutesSeconds(timeLeftMillis));
            }

            @Override
            public void onFinish() {
                timeLeftMillis = 0;
                timerText.setText(formatMillisToMinutesSeconds(0));
                timerExpired();
            }
        }.start();
    }

    private void timerExpired() {
        Toast.makeText(this, "Time is up — test will be submitted automatically.", Toast.LENGTH_LONG).show();

        // auto-submit
        // If we don't yet have questions loaded, still attempt to submit (submit handler checks)
        submitQuizResults();
    }

    private void cancelTimerAndClearPref() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        // clear saved end time
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String key = "PREF_END_TIME_USER_" + userId;
        prefs.edit().remove(key).apply();
    }

    private String formatMillisToMinutesSeconds(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remSeconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remSeconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // don't clear pref here; only clear when user submits or timer expires
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}