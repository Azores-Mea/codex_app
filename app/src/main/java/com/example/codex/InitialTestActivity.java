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
import android.widget.ScrollView;
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
import java.util.Collection;
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
    private HashMap<Integer, Long> questionStartTimes = new HashMap<>();
    private int currentIndex = 0;

    private DatabaseReference quizResultsRef;
    private int userId;

    // Timer fields
    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME_MILLIS = 20L * 60L * 1000L; // 45 minutes
    private long timeLeftMillis = TOTAL_TIME_MILLIS;
    private long endTimeMillis = 0L;
    private long testStartTime = 0L;

    private static final String PREFS_NAME = "InitialTestTimerPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialtest);

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        Log.d("InitialTestActivity", "UserId = " + userId);

        quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");

        initViews();

        // Reset timer when activity restarts
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove("PREF_END_TIME_USER_" + userId)
                .apply();

        // Record test start time
        testStartTime = System.currentTimeMillis();

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

        timerText = findViewById(R.id.timeText);
    }

    // Store lesson titles mapping
    private HashMap<String, String> lessonTitles = new HashMap<>();

    private void loadQuestionsFromFirebase() {
        // Load ALL lessons (L1, L2, L3, etc.) to get their difficulties and titles
        DatabaseReference lessonsRef = FirebaseDatabase.getInstance().getReference("Lessons");
        lessonsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, String> lessonDifficulties = new HashMap<>();
                List<String> lessonIds = new ArrayList<>();

                for (DataSnapshot lessonSnap : snapshot.getChildren()) {
                    String lessonId = lessonSnap.getKey();

                    // Get difficulty
                    String difficulty = lessonSnap.child("difficulty").getValue(String.class);

                    // Get main_title from content node
                    DataSnapshot contentSnap = lessonSnap.child("content");
                    String mainTitle = null;

                    if (contentSnap.exists()) {
                        mainTitle = contentSnap.child("main_title").getValue(String.class);
                    }

                    // If not found in content, try directly under lesson
                    if (mainTitle == null) {
                        mainTitle = lessonSnap.child("main_title").getValue(String.class);
                    }

                    if (lessonId != null && difficulty != null) {
                        lessonDifficulties.put(lessonId, difficulty);
                        lessonIds.add(lessonId);
                        Log.d("InitialTestActivity", "Loaded lesson: " + lessonId + " -> " + difficulty);
                    }

                    if (lessonId != null && mainTitle != null) {
                        String cleanTitle = mainTitle.replaceAll("<[^>]*>", "").trim();
                        lessonTitles.put(lessonId, cleanTitle);
                        Log.d("InitialTestActivity", "Loaded lesson title: " + lessonId + " -> " + cleanTitle);
                    }
                }

                Log.d("InitialTestActivity", "Total lessons loaded: " + lessonIds.size());

                // After loading all lesson data, load questions from all modules
                loadQuestionsFromAllModules(lessonIds, lessonDifficulties);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("InitialTestActivity", "Failed to load lesson data", error.toException());
                Toast.makeText(InitialTestActivity.this, "Failed to load lesson data", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadQuestionsFromAllModules(List<String> lessonIds, HashMap<String, String> lessonDifficulties) {
        List<HashMap<String, String>> beginnerList = new ArrayList<>();
        List<HashMap<String, String>> intermediateList = new ArrayList<>();
        List<HashMap<String, String>> advancedList = new ArrayList<>();

        // Counter to track completion
        final int[] completedLoads = {0};
        final int totalLoads = lessonIds.size();

        if (totalLoads == 0) {
            Toast.makeText(InitialTestActivity.this, "No lessons found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load questions from each module
        for (String lessonId : lessonIds) {
            DatabaseReference quizRef = FirebaseDatabase.getInstance()
                    .getReference("assessment")
                    .child(lessonId)
                    .child("Quiz");

            quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String difficulty = lessonDifficulties.get(lessonId);

                    for (DataSnapshot questionSnap : snapshot.getChildren()) {
                        String questionId = questionSnap.getKey();

                        // Extract all question data
                        String question = questionSnap.child("question").getValue(String.class);
                        String choiceA = questionSnap.child("choiceA").getValue(String.class);
                        String choiceB = questionSnap.child("choiceB").getValue(String.class);
                        String choiceC = questionSnap.child("choiceC").getValue(String.class);
                        String choiceD = questionSnap.child("choiceD").getValue(String.class);
                        String answer = questionSnap.child("answer").getValue(String.class);

                        if (question != null && answer != null) {
                            HashMap<String, String> q = new HashMap<>();
                            q.put("question", question);
                            q.put("choiceA", choiceA != null ? choiceA : "");
                            q.put("choiceB", choiceB != null ? choiceB : "");
                            q.put("choiceC", choiceC != null ? choiceC : "");
                            q.put("choiceD", choiceD != null ? choiceD : "");
                            q.put("correctAnswer", answer);
                            q.put("lessonId", lessonId);
                            q.put("questionId", questionId);

                            // Classify based on lesson difficulty
                            if (difficulty != null) {
                                q.put("classification", difficulty);

                                switch (difficulty) {
                                    case "Beginner":
                                        synchronized (beginnerList) {
                                            beginnerList.add(q);
                                        }
                                        break;
                                    case "Intermediate":
                                        synchronized (intermediateList) {
                                            intermediateList.add(q);
                                        }
                                        break;
                                    case "Advanced":
                                        synchronized (advancedList) {
                                            advancedList.add(q);
                                        }
                                        break;
                                    default:
                                        Log.w("InitialTestActivity", "Unknown difficulty: " + difficulty);
                                        break;
                                }
                            }
                        }
                    }

                    Log.d("InitialTestActivity", "Loaded " + snapshot.getChildrenCount() +
                            " questions from " + lessonId + " (" + difficulty + ")");

                    // Check if all modules have been loaded
                    completedLoads[0]++;
                    if (completedLoads[0] == totalLoads) {
                        finalizeQuestionSelection(beginnerList, intermediateList, advancedList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("InitialTestActivity", "Failed to load questions from " + lessonId, error.toException());

                    // Still check completion even on error
                    completedLoads[0]++;
                    if (completedLoads[0] == totalLoads) {
                        finalizeQuestionSelection(beginnerList, intermediateList, advancedList);
                    }
                }
            });
        }
    }

    private void finalizeQuestionSelection(List<HashMap<String, String>> beginnerList,
                                           List<HashMap<String, String>> intermediateList,
                                           List<HashMap<String, String>> advancedList) {

        // Distribution: 10 Beginner, 10 Intermediate, 10 Advanced
        int beginnerCount = 10;
        int intermediateCount = 10;
        int advancedCount = 10;

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
    private List<HashMap<String, String>> getRandomSubset(List<HashMap<String, String>> source, int count) {
        List<HashMap<String, String>> result = new ArrayList<>();

        if (source == null || source.isEmpty()) {
            return result;
        }

        // If we have fewer items than requested, return all items
        if (source.size() <= count) {
            result.addAll(source);
            return result;
        }

        // Create a copy to avoid modifying the original list
        List<HashMap<String, String>> copy = new ArrayList<>(source);

        // Shuffle and take the first 'count' items
        java.util.Collections.shuffle(copy);

        for (int i = 0; i < count && i < copy.size(); i++) {
            result.add(copy.get(i));
        }

        return result;
    }

    private void loadQuestion(int index) {
        currentIndex = index;

        if (!questionStartTimes.containsKey(index)) {
            questionStartTimes.put(index, System.currentTimeMillis());
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

    // NEW CLASSIFICATION LOGIC BASED ON GWA
    private String getClassification(double gwa) {
        if (gwa >= 85.0) {
            return "Advanced";
        } else if (gwa >= 65.0) {
            return "Intermediate";
        } else {
            return "Beginner";
        }
    }

    // Calculate weighted GWA with time-based adjustments
    // Replace the calculateDetailedScore() method with this corrected version

    private HashMap<String, Object> calculateDetailedScore() {
        int beginnerScore = 0, beginnerTotal = 0;
        int intermediateScore = 0, intermediateTotal = 0;
        int advancedScore = 0, advancedTotal = 0;
        int totalScore = 0;

        List<String> weakAreas = new ArrayList<>();
        HashMap<String, Integer> lessonScores = new HashMap<>();
        HashMap<String, Integer> lessonTotals = new HashMap<>();

        long beginnerTotalTime = 0;
        long intermediateTotalTime = 0;
        long advancedTotalTime = 0;

        for (int i = 0; i < questionList.size(); i++) {
            HashMap<String, String> q = questionList.get(i);
            String classification = q.get("classification");
            String lessonId = q.get("lessonId");
            String correct = q.get("correctAnswer");
            String userAns = userAnswers.get(i);

            boolean isCorrect = userAns != null && correct != null &&
                    userAns.equalsIgnoreCase(correct);

            // Calculate time spent on this specific question
            long questionStartTime = questionStartTimes.getOrDefault(i, testStartTime);
            long questionEndTime;

            // If this is not the last answered question, use the next question's start time
            if (i < questionList.size() - 1 && questionStartTimes.containsKey(i + 1)) {
                questionEndTime = questionStartTimes.get(i + 1);
            } else {
                // For the last question or if next question wasn't viewed, use current time
                questionEndTime = System.currentTimeMillis();
            }

            long timeSpentOnQuestionMillis = questionEndTime - questionStartTime;

            if (classification != null) {
                switch (classification) {
                    case "Beginner":
                        beginnerTotal++;
                        beginnerTotalTime += timeSpentOnQuestionMillis;
                        if (isCorrect) beginnerScore++;
                        break;

                    case "Intermediate":
                        intermediateTotal++;
                        intermediateTotalTime += timeSpentOnQuestionMillis;
                        if (isCorrect) intermediateScore++;
                        break;

                    case "Advanced":
                        advancedTotal++;
                        advancedTotalTime += timeSpentOnQuestionMillis;
                        if (isCorrect) advancedScore++;
                        break;
                }
            }

            if (isCorrect) {
                totalScore++;
            }

            // Track lesson performance for weak areas
            if (lessonId != null) {
                lessonTotals.put(lessonId, lessonTotals.getOrDefault(lessonId, 0) + 1);
                if (isCorrect) {
                    lessonScores.put(lessonId, lessonScores.getOrDefault(lessonId, 0) + 1);
                }
            }
        }

        // Calculate average time per question for each difficulty level
        double beginnerAvgTimeSecs = beginnerTotal > 0 ? (beginnerTotalTime / 1000.0 / beginnerTotal) : 0;
        double intermediateAvgTimeSecs = intermediateTotal > 0 ? (intermediateTotalTime / 1000.0 / intermediateTotal) : 0;
        double advancedAvgTimeSecs = advancedTotal > 0 ? (advancedTotalTime / 1000.0 / advancedTotal) : 0;

        // Calculate raw percentages for each difficulty level
        double beginnerRawPercentage = beginnerTotal > 0 ? (beginnerScore * 100.0 / beginnerTotal) : 0;
        double intermediateRawPercentage = intermediateTotal > 0 ? (intermediateScore * 100.0 / intermediateTotal) : 0;
        double advancedRawPercentage = advancedTotal > 0 ? (advancedScore * 100.0 / advancedTotal) : 0;

        // Apply time-based penalty per difficulty level
        double beginnerAdjustedPercentage = beginnerRawPercentage;
        if (beginnerAvgTimeSecs < 5.0) {
            beginnerAdjustedPercentage *= 0.90; // Reduce by 10%
        } else if (beginnerAvgTimeSecs > 20.0) {
            beginnerAdjustedPercentage *= 0.95; // Reduce by 5%
        }

        double intermediateAdjustedPercentage = intermediateRawPercentage;
        if (intermediateAvgTimeSecs < 5.0) {
            intermediateAdjustedPercentage *= 0.90; // Reduce by 10%
        } else if (intermediateAvgTimeSecs > 40.0) {
            intermediateAdjustedPercentage *= 0.95; // Reduce by 5%
        }

        double advancedAdjustedPercentage = advancedRawPercentage;
        if (advancedAvgTimeSecs < 5.0) {
            advancedAdjustedPercentage *= 0.90; // Reduce by 10%
        } else if (advancedAvgTimeSecs > 60.0) {
            advancedAdjustedPercentage *= 0.95; // Reduce by 5%
        }

        // Calculate weighted GWA using adjusted percentages
        // Weights: Beginner 25%, Intermediate 35%, Advanced 40%
        double weightedGWA = (beginnerAdjustedPercentage * 0.25) +
                (intermediateAdjustedPercentage * 0.35) +
                (advancedAdjustedPercentage * 0.40);

        // Overall average time per question (for display purposes)
        long totalTimeSpent = System.currentTimeMillis() - testStartTime;
        double avgTimePerQuestion = totalTimeSpent / 1000.0 / questionList.size();

        // Identify weak areas (lessons with < 50% correct)
        for (String lessonId : lessonTotals.keySet()) {
            int lessonTotal = lessonTotals.get(lessonId);
            int lessonScore = lessonScores.getOrDefault(lessonId, 0);
            double lessonPercentage = (lessonScore * 100.0 / lessonTotal);

            if (lessonPercentage < 50.0) {
                String lessonTitle = lessonTitles.get(lessonId);
                if (lessonTitle != null && !lessonTitle.isEmpty()) {
                    weakAreas.add(lessonTitle);
                    Log.d("InitialTestActivity", "Weak area: " + lessonId + " -> " + lessonTitle);
                } else {
                    weakAreas.add(lessonId);
                    Log.w("InitialTestActivity", "No title found for weak area: " + lessonId);
                }
            }
        }

        Log.d("InitialTestActivity", "=== Score Calculation Details ===");
        Log.d("InitialTestActivity", "Beginner: " + beginnerScore + "/" + beginnerTotal +
                " (Raw: " + String.format("%.2f", beginnerRawPercentage) + "%, Avg Time: " +
                String.format("%.2f", beginnerAvgTimeSecs) + "s, Adjusted: " +
                String.format("%.2f", beginnerAdjustedPercentage) + "%)");
        Log.d("InitialTestActivity", "Intermediate: " + intermediateScore + "/" + intermediateTotal +
                " (Raw: " + String.format("%.2f", intermediateRawPercentage) + "%, Avg Time: " +
                String.format("%.2f", intermediateAvgTimeSecs) + "s, Adjusted: " +
                String.format("%.2f", intermediateAdjustedPercentage) + "%)");
        Log.d("InitialTestActivity", "Advanced: " + advancedScore + "/" + advancedTotal +
                " (Raw: " + String.format("%.2f", advancedRawPercentage) + "%, Avg Time: " +
                String.format("%.2f", advancedAvgTimeSecs) + "s, Adjusted: " +
                String.format("%.2f", advancedAdjustedPercentage) + "%)");
        Log.d("InitialTestActivity", "Weighted GWA: " + String.format("%.2f", weightedGWA));
        Log.d("InitialTestActivity", "Overall avg time per question: " + String.format("%.2f", avgTimePerQuestion) + "s");
        Log.d("InitialTestActivity", "Total weak areas: " + weakAreas.size());

        HashMap<String, Object> scores = new HashMap<>();
        scores.put("totalScore", totalScore);
        scores.put("beginnerScore", beginnerScore);
        scores.put("beginnerTotal", beginnerTotal);
        scores.put("beginnerAvgTime", beginnerAvgTimeSecs);
        scores.put("intermediateScore", intermediateScore);
        scores.put("intermediateTotal", intermediateTotal);
        scores.put("intermediateAvgTime", intermediateAvgTimeSecs);
        scores.put("advancedScore", advancedScore);
        scores.put("advancedTotal", advancedTotal);
        scores.put("advancedAvgTime", advancedAvgTimeSecs);
        scores.put("weightedGWA", weightedGWA);
        scores.put("avgTimePerQuestion", avgTimePerQuestion);
        scores.put("weakAreas", weakAreas);

        return scores;
    }

    private void showConfirmationDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_ready_to_join, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (!isFinishing() && !isDestroyed()) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
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
        cancelTimerAndClearPref();

        int totalQuestions = questionList.size();
        if (totalQuestions == 0) {
            Toast.makeText(this, "Cannot submit: no questions loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> scoreDetails = calculateDetailedScore();
        int score = (int) scoreDetails.get("totalScore");
        double weightedGWA = (double) scoreDetails.get("weightedGWA");
        double avgTimePerQuestion = (double) scoreDetails.get("avgTimePerQuestion");
        List<String> weakAreas = (List<String>) scoreDetails.get("weakAreas");

        String classification = getClassification(weightedGWA);
        String passed = weightedGWA >= 65.0 ? "Passed" : "Failed";

        List<String> answersList = new ArrayList<>();
        for (int i = 0; i < totalQuestions; i++) {
            answersList.add(userAnswers.getOrDefault(i, ""));
        }

        long startTime = testStartTime;
        long timeFinishedAt = System.currentTimeMillis();
        long timeSpentMillis = timeFinishedAt - startTime;
        String timeSpentFormatted = formatMillisToMinutesSeconds(timeSpentMillis);

        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("classification", classification);
        resultData.put("weightedGWA", weightedGWA);
        resultData.put("beginnerScore", scoreDetails.get("beginnerScore"));
        resultData.put("intermediateScore", scoreDetails.get("intermediateScore"));
        resultData.put("advancedScore", scoreDetails.get("advancedScore"));
        resultData.put("weakAreas", weakAreas);
        resultData.put("passed", passed);
        resultData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        resultData.put("timeFinishedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timeFinishedAt)));
        resultData.put("timeSpentMillis", timeSpentMillis);
        resultData.put("timeSpentFormatted", timeSpentFormatted);
        resultData.put("userId", userId);
        resultData.put("lessonId", "ITest1");
        resultData.put("quizType", "InitialTest");
        resultData.put("answers", answersList);
        resultData.put("score", score);
        resultData.put("total", totalQuestions);resultData.put("advancedTotal", scoreDetails.get("advancedTotal"));
        resultData.put("beginnerTotal", scoreDetails.get("beginnerTotal"));resultData.put("intermediateTotal", scoreDetails.get("intermediateTotal"));
        resultData.put("avgTimePerQuestion", avgTimePerQuestion);

        FirebaseDatabase.getInstance().getReference("quizResults").push().setValue(resultData)
                .addOnSuccessListener(aVoid -> {
                    updateUserClassification(classification);
                    saveRecentActivity(score, totalQuestions, timeSpentMillis, timeSpentFormatted);
                    showFinalConfirmationDialog(score, totalQuestions, classification, weightedGWA, timeSpentFormatted, weakAreas);
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
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(String.valueOf(userId));

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("classification", classification);

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("InitialTestActivity", "User classification updated to: " + classification);
                    SessionManager sessionManager = new SessionManager(InitialTestActivity.this);
                    sessionManager.setClassification(classification);
                })
                .addOnFailureListener(e -> Log.e("FirebaseError", "Failed to update user classification", e));
    }

    private void showFinalConfirmationDialog(int score, int totalQuestions, String classification,
                                             double gwa, String timeCompleted, List<String> weakAreas) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogue_initial_test, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (!isFinishing() && !isDestroyed()) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
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

        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView scoreText = dialogView.findViewById(R.id.score);
        TextView timeCompletedText = dialogView.findViewById(R.id.timeCompleted);
        TextView weakAreasLabel = dialogView.findViewById(R.id.weakAreasLabel);
        ScrollView weakAreasScroll = dialogView.findViewById(R.id.weakAreasScroll);
        TextView weakAreasText = dialogView.findViewById(R.id.weakAreasText);
        MaterialButton btnContinue = dialogView.findViewById(R.id.yes_btn);

        MaterialCardView classCard = dialogView.findViewById(R.id.classification);
        LinearLayout classBg = dialogView.findViewById(R.id.classBg);
        TextView className = dialogView.findViewById(R.id.className);
        TextView percent = dialogView.findViewById(R.id.percent);

        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        scoreText.setText("Score: " + score + "/" + totalQuestions);
        timeCompletedText.setText(timeCompleted);
        percent.setText(String.format(Locale.getDefault(), "%.0f%%", gwa));

        // Display weak areas
        if (weakAreas != null && !weakAreas.isEmpty()) {
            weakAreasLabel.setVisibility(View.VISIBLE);
            weakAreasScroll.setVisibility(View.VISIBLE);
            weakAreasText.setVisibility(View.VISIBLE);
            StringBuilder weakAreasStr = new StringBuilder();
            for (int i = 0; i < weakAreas.size(); i++) {
                weakAreasStr.append(weakAreas.get(i));
                if (i < weakAreas.size() - 1) {
                    weakAreasStr.append("\n");
                }
            }
            weakAreasText.setText(weakAreasStr.toString());
        } else {
            weakAreasLabel.setVisibility(View.GONE);
            weakAreasScroll.setVisibility(View.GONE);
            weakAreasText.setVisibility(View.GONE);
        }

        int colorText;
        int colorStroke;
        String bgColor;

        if (classification.equals("Beginner")) {
            colorText = Color.parseColor("#E3AF64");
            colorStroke = Color.parseColor("#E3AF64");
            bgColor = "#FFF4E3";
        } else if (classification.equals("Intermediate")) {
            colorText = Color.parseColor("#66ABF4");
            colorStroke = Color.parseColor("#66ABF4");
            bgColor = "#E3F1FF";
        } else {
            colorText = Color.parseColor("#A666F4");
            colorStroke = Color.parseColor("#A666F4");
            bgColor = "#F3E8FF";
        }

        className.setText(classification);
        className.setTextColor(colorText);
        percent.setTextColor(colorText);
        classCard.setStrokeColor(colorStroke);
        classBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(bgColor)));

        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(InitialTestActivity.this, Navigation_ActivityLearner.class);
            startActivity(intent);
            finish();
        });
    }

    private void restoreOrStartTimer() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String key = "PREF_END_TIME_USER_" + userId;
        endTimeMillis = prefs.getLong(key, 0L);

        if (endTimeMillis == 0L) {
            endTimeMillis = System.currentTimeMillis() + TOTAL_TIME_MILLIS;
            prefs.edit().putLong(key, endTimeMillis).apply();
        }

        long millisLeft = endTimeMillis - System.currentTimeMillis();
        if (millisLeft <= 0L) {
            timeLeftMillis = 0L;
            timerExpired();
        } else {
            timeLeftMillis = millisLeft;
            startCountDown(timeLeftMillis);
        }
    }

    private void startCountDown(long startMillis) {
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
        submitQuizResults();
    }

    private void cancelTimerAndClearPref() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String key = "PREF_END_TIME_USER_" + userId;
        prefs.edit().remove(key).apply();
    }

    private String formatMillisToMinutesSeconds(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remSeconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d mins %02d secs", minutes, remSeconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}