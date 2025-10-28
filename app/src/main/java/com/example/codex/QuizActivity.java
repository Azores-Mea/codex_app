package com.example.codex;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

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
    private boolean prevEnabledOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialtest);

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
                Toast.makeText(this, "Quiz Submitted!", Toast.LENGTH_SHORT).show();
            }
        });

        setChoiceListeners();
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
                    loadQuestion(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuizActivity.this, "Failed to load questions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestion(int index) {
        HashMap<String, String> q = questionList.get(index);

        question.setText(Html.fromHtml(q.get("question")));
        choiceAText.setText(Html.fromHtml(q.get("choiceA")));
        choiceBText.setText(Html.fromHtml(q.get("choiceB")));
        choiceCText.setText(Html.fromHtml(q.get("choiceC")));
        choiceDText.setText(Html.fromHtml(q.get("choiceD")));

        qNum.setText((index + 1) + ".");
        questionNumber.setText("Question " + (index + 1) + " of " + questionList.size());
        progressQuestion.setText((index) + " of " + questionList.size() + " Questions");

        // ✅ progress starts AFTER first question
        int percent = (int) ((index / (float) questionList.size()) * 100);
        progressBar.setProgress(percent);
        progressPercent.setText(percent + "%");

        resetChoiceStyles();

        if (userAnswers.containsKey(index)) {
            highlightChoice(userAnswers.get(index));
        }

        // ✅ Properly disable "Previous" button on first question
        if (index == 0) {
            prev.setEnabled(false);
            prev.setAlpha(0.6f);
            prev.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
            prev.setTextColor(Color.BLACK);
        } else {
            prev.setEnabled(true);
            prev.setAlpha(1f);
            prev.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));
            prev.setTextColor(Color.WHITE);
        }

        // ✅ Enable/disable "Next" depending on user’s answer
        if (userAnswers.containsKey(index)) {
            enableNextButton();
        } else {
            disableNextButton();
        }

        // ✅ Last question becomes "Submit"
        if (index == questionList.size() - 1) {
            next.setText("Submit");
        } else {
            next.setText("Next");
        }
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

        // enable prev button if on first question after selection
        if (currentIndex == 0) {
            prevEnabledOnce = true;
        }
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
        card.setStrokeColor(getColor(R.color.secondary));
        bg.setBackgroundColor(getColor(R.color.bgText));
        label.setTextColor(getColor(R.color.secondary));

        letter.setBackgroundResource(R.drawable.choice_circle_background);
        letter.setTextColor(getColor(R.color.secondary));
    }

    private void setSelected(MaterialCardView card, TextView label, TextView letter, LinearLayout bg) {
        card.setStrokeColor(getColor(R.color.primary));
        bg.setBackgroundColor(Color.parseColor("#D9D9D9"));
        label.setTextColor(getColor(R.color.black));

        letter.setBackgroundResource(R.drawable.choice_circle_selected);
        letter.setTextColor(getColor(R.color.bgText));
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
        next.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        next.setTextColor(Color.BLACK);
    }
}
