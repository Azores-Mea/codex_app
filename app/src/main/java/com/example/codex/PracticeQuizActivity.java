package com.example.codex;

import android.app.AlertDialog;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class PracticeQuizActivity extends AppCompatActivity {

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

    private DatabaseReference practiceResultsRef;
    private int userId;
    private String lessonId;
    private String quizTitle = "Practice Test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialtest);

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();
        lessonId = sessionManager.getSelectedLesson();

        practiceResultsRef = FirebaseDatabase.getInstance().getReference("practiceResults");

        initViews();
        loadLessonTitleAndQuiz();

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

    private void loadLessonTitleAndQuiz() {
        DatabaseReference lessonRef = FirebaseDatabase.getInstance()
                .getReference("Lessons")
                .child(lessonId)
                .child("main_title");

        TextView testTitle = findViewById(R.id.test_title);

        lessonRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String mainTitle = snapshot.getValue(String.class);

                    if (mainTitle != null) {
                        mainTitle = mainTitle.replaceAll("(?i)<p>", "")
                                .replaceAll("(?i)</p>", "")
                                .trim();
                        quizTitle = mainTitle;
                        testTitle.setText(quizTitle + " Practice Test");
                    } else {
                        testTitle.setText("Practice Test");
                    }
                } else {
                    testTitle.setText("Practice Test");
                }

                loadRandomizedQuizData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PracticeQuiz", "Failed to load lesson title: " + error.getMessage());
                testTitle.setText("Practice Test");
                loadRandomizedQuizData();
            }
        });
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

    private void loadRandomizedQuizData() {
        DatabaseReference lessonRef = FirebaseDatabase.getInstance()
                .getReference("Lessons")
                .child(lessonId)
                .child("difficulty");

        lessonRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(PracticeQuizActivity.this, "No difficulty found for this lesson.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String difficulty = snapshot.getValue(String.class);
                if (difficulty == null || difficulty.isEmpty()) {
                    Toast.makeText(PracticeQuizActivity.this, "Difficulty not set for this lesson.", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference quizRef = FirebaseDatabase.getInstance()
                        .getReference("quizQuestions")
                        .child(difficulty)
                        .child(lessonId);

                quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(PracticeQuizActivity.this, "No quiz found for this lesson.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<HashMap<String, String>> allQuestions = new ArrayList<>();
                        for (DataSnapshot qSnap : snapshot.getChildren()) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("question", qSnap.child("question").getValue(String.class));
                            map.put("choiceA", qSnap.child("choiceA").getValue(String.class));
                            map.put("choiceB", qSnap.child("choiceB").getValue(String.class));
                            map.put("choiceC", qSnap.child("choiceC").getValue(String.class));
                            map.put("choiceD", qSnap.child("choiceD").getValue(String.class));
                            map.put("correctAnswer", qSnap.child("answer").getValue(String.class));
                            allQuestions.add(map);
                        }

                        // Randomize and select 5 questions (or all if less than 5)
                        Collections.shuffle(allQuestions);
                        int numQuestions = Math.min(5, allQuestions.size());
                        questionList = allQuestions.subList(0, numQuestions);

                        if (!questionList.isEmpty()) {
                            currentIndex = 0;
                            loadQuestion(0);
                        } else {
                            Toast.makeText(PracticeQuizActivity.this, "No quiz questions available.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PracticeQuizActivity.this, "Failed to load quiz data.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PracticeQuizActivity.this, "Failed to load lesson difficulty.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestion(int index) {
        if (index < 0 || index >= questionList.size()) return;

        HashMap<String, String> q = questionList.get(index);

        question.setText(Html.fromHtml(q.get("question"), Html.FROM_HTML_MODE_LEGACY));
        choiceAText.setText(Html.fromHtml(q.get("choiceA"), Html.FROM_HTML_MODE_LEGACY));
        choiceBText.setText(Html.fromHtml(q.get("choiceB"), Html.FROM_HTML_MODE_LEGACY));
        choiceCText.setText(Html.fromHtml(q.get("choiceC"), Html.FROM_HTML_MODE_LEGACY));
        choiceDText.setText(Html.fromHtml(q.get("choiceD"), Html.FROM_HTML_MODE_LEGACY));

        qNum.setText((index + 1) + ".");
        questionNumber.setText("Question " + (index + 1) + " of " + questionList.size());
        progressQuestion.setText((index + 1) + " of " + questionList.size() + " Questions");

        progressBar.setMax(100);
        int percent = (int) (((index) / (float) questionList.size()) * 100);
        progressBar.setProgress(percent);
        progressPercent.setText(percent + "%");

        resetChoiceStyles();

        if (userAnswers.containsKey(index)) highlightChoice(userAnswers.get(index));

        prev.setEnabled(index > 0);
        prev.setAlpha(index > 0 ? 1f : 0.6f);
        prev.setBackgroundTintList(ColorStateList.valueOf(
                index > 0 ? Color.parseColor("#03162A") : Color.parseColor("#BDBDBD")));
        prev.setTextColor(index > 0 ? Color.WHITE : Color.BLACK);

        if (userAnswers.containsKey(index)) enableNextButton();
        else disableNextButton();

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
        } else return;

        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Submit practice test?");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        dialogMessage.setText(Html.fromHtml("This is for practice only and won't be recorded.", Html.FROM_HTML_MODE_LEGACY));

        MaterialButton btnNo = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnYes = dialogView.findViewById(R.id.yes_btn);

        btnNo.setText("Review Answer");
        btnYes.setText("Submit");

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            submitPracticeResults();
        });
    }

    private void submitPracticeResults() {
        int total = questionList.size();
        int score = calculateScore();
        int percent = (int) (((float) score / total) * 100);

        DatabaseReference userPracticeRef = practiceResultsRef
                .child(String.valueOf(userId))
                .child(lessonId)
                .push();

        HashMap<String, Object> answersMap = new HashMap<>();
        for (int i = 0; i < userAnswers.size(); i++) {
            answersMap.put(String.valueOf(i), userAnswers.get(i));
        }

        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("answers", answersMap);
        resultData.put("score", score);
        resultData.put("total", total);
        resultData.put("percentage", percent);
        resultData.put("timestamp", ServerValue.TIMESTAMP);

        userPracticeRef.setValue(resultData)
                .addOnSuccessListener(aVoid -> {
                    showResultDialog(score, total);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save results: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int calculateScore() {
        int score = 0;
        for (int i = 0; i < questionList.size(); i++) {
            String correct = questionList.get(i).get("correctAnswer");
            String ans = userAnswers.get(i);
            if (ans != null && correct != null && ans.equalsIgnoreCase(correct)) score++;
        }
        return score;
    }

    private void showResultDialog(int score, int total) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Practice Test Complete");
        builder.setMessage("You scored " + score + " out of " + total + " (" + ((score * 100) / total) + "%)");
        builder.setPositiveButton("OK", (dialog, which) -> {
            setResult(RESULT_OK);
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }
}