package com.example.codex;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

public class ReviewDisplayFragment extends Fragment {

    private static final String ARG_LESSON_ID = "lessonId";
    private static final String ARG_TITLE = "mainTitle";
    private static final String ARG_DIFFICULTY = "difficulty";
    private static final String TAG = "ReviewDisplayFragment";

    private String lessonId, mainTitle, difficulty;
    private MaterialButton btnQuiz, btnCoding, btnPrev, btnNext;
    private LinearLayout container;
    private ImageView backButton;
    private TextView headerTitle;

    private SessionManager sessionManager;
    private int userId;

    private List<HashMap<String, String>> questionList = new ArrayList<>();
    private HashMap<Integer, String> userAnswers = new HashMap<>();
    private int currentQuestionIndex = 0;

    public static ReviewDisplayFragment newInstance(String lessonId, String mainTitle, String difficulty) {
        ReviewDisplayFragment fragment = new ReviewDisplayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LESSON_ID, lessonId);
        args.putString(ARG_TITLE, mainTitle);
        args.putString(ARG_DIFFICULTY, difficulty);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            lessonId = getArguments().getString(ARG_LESSON_ID);
            mainTitle = getArguments().getString(ARG_TITLE);
            difficulty = getArguments().getString(ARG_DIFFICULTY);
        }

        sessionManager = new SessionManager(requireContext());
        userId = sessionManager.getUserId();

        Log.d(TAG, "LessonId: " + lessonId + ", UserId: " + userId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.review_display, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views from review_display.xml
        container = view.findViewById(R.id.review_container);
        btnQuiz = view.findViewById(R.id.quiz);
        btnCoding = view.findViewById(R.id.coding);
        backButton = view.findViewById(R.id.back);
        headerTitle = view.findViewById(R.id.headerTitle);

        if (container == null || btnQuiz == null || btnCoding == null) {
            Log.e(TAG, "Required views not found!");
            return;
        }

        // Set header title
        if (mainTitle != null) {
            headerTitle.setText(Html.fromHtml(mainTitle, Html.FROM_HTML_MODE_COMPACT));
        }

        // Set back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Set Quiz button as active by default
        setActiveButton(btnQuiz);

        // Set up button click listeners
        setupButtonListeners();

        // Check for coding exercise availability
        checkCodingExerciseAvailability();

        // Load quiz content by default
        loadQuizContent();
    }

    private void setupButtonListeners() {
        btnQuiz.setOnClickListener(v -> {
            Log.d(TAG, "Quiz button clicked");
            setActiveButton(btnQuiz);
            currentQuestionIndex = 0;
            loadQuizContent();
        });

        btnCoding.setOnClickListener(v -> {
            Log.d(TAG, "Coding button clicked");

            if (btnCoding.isEnabled()) {
                // Open ExerciseActivity
                Intent intent = new Intent(requireContext(), ExerciseActivity.class);
                intent.putExtra("lessonId", lessonId);
                intent.putExtra("mainTitle", mainTitle);
                intent.putExtra("difficulty", difficulty);
                startActivity(intent);
            }
        });

    }

    private void setActiveButton(MaterialButton activeButton) {

        // --- RESET all buttons to default ---
        resetSelectButton(btnQuiz);
        resetSelectButton(btnCoding);

        // --- APPLY ACTIVE STATE ---
        activeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));
        activeButton.setTextColor(Color.parseColor("#F5F5F5"));
        activeButton.setEnabled(true);
        activeButton.setAlpha(1f);
    }

    private void resetSelectButton(MaterialButton button) {
        button.setBackgroundTintList(null);
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        // ❌ DO NOT touch enabled/alpha here
    }




    private void checkCodingExerciseAvailability() {
        if (!isAdded() || lessonId == null || lessonId.isEmpty()) {
            Log.w(TAG, "LessonId is null or empty. Disabling coding button.");
            disableCodingButton();
            return;
        }

        DatabaseReference codingRef = FirebaseDatabase.getInstance()
                .getReference("coding_exercises")
                .child(lessonId);

        codingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (snapshot.exists() && snapshot.getChildrenCount() > 1) {
                    // There is a valid coding exercise (index 0 is null so at least 1 real item)
                    Log.d(TAG, "Coding exercises found for lesson: " + lessonId);
                    enableCodingButton();
                } else {
                    Log.d(TAG, "No coding exercises found for lesson: " + lessonId);
                    disableCodingButton();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Error checking coding exercises: " + error.getMessage());
                disableCodingButton();
            }
        });
    }


    private void enableCodingButton() {
        if (btnCoding != null) {
            btnCoding.setEnabled(true);
            btnCoding.setAlpha(1.0f);
        }
    }

    private void disableCodingButton() {
        if (btnCoding != null) {
            btnCoding.setEnabled(false);
            btnCoding.setAlpha(0.5f);
        }
    }

    private void loadQuizContent() {
        container.removeAllViews();

        // Inflate quiz_review.xml
        View quizLayout = getLayoutInflater().inflate(R.layout.quiz_review, container, false);
        container.addView(quizLayout);

        // Find navigation buttons
        btnPrev = quizLayout.findViewById(R.id.prev);
        btnNext = quizLayout.findViewById(R.id.next);

        // Set up navigation
        setupNavigationButtons();

        // Load quiz data
        loadQuizData();
    }

    private void setupNavigationButtons() {
        btnPrev.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                displayQuestion(currentQuestionIndex);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                displayQuestion(currentQuestionIndex);
            }
        });
    }

    private void loadQuizData() {
        DatabaseReference lessonRef = FirebaseDatabase.getInstance()
                .getReference("Lessons")
                .child(lessonId)
                .child("difficulty");

        lessonRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                String diff = snapshot.getValue(String.class);
                if (diff == null) {
                    showError("Difficulty not found");
                    return;
                }

                // Load questions
                DatabaseReference quizRef = FirebaseDatabase.getInstance()
                        .getReference("quizQuestions")
                        .child(diff)
                        .child(lessonId);

                quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        questionList.clear();
                        for (DataSnapshot qSnap : snapshot.getChildren()) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("question", qSnap.child("question").getValue(String.class));
                            map.put("choiceA", qSnap.child("choiceA").getValue(String.class));
                            map.put("choiceB", qSnap.child("choiceB").getValue(String.class));
                            map.put("choiceC", qSnap.child("choiceC").getValue(String.class));
                            map.put("choiceD", qSnap.child("choiceD").getValue(String.class));
                            map.put("correctAnswer", qSnap.child("answer").getValue(String.class));
                            questionList.add(map);
                        }

                        // Load user answers
                        loadUserAnswers();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        showError("Failed to load quiz questions");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                showError("Failed to load difficulty");
            }
        });
    }

    private void loadUserAnswers() {
        DatabaseReference answersRef = FirebaseDatabase.getInstance()
                .getReference("quizResults")
                .child(String.valueOf(userId))
                .child(lessonId)
                .child("answers");

        answersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                userAnswers.clear();
                for (DataSnapshot ansSnap : snapshot.getChildren()) {
                    int index = Integer.parseInt(ansSnap.getKey());
                    String answer = ansSnap.getValue(String.class);
                    userAnswers.put(index, answer);
                }

                // Display first question
                if (!questionList.isEmpty()) {
                    displayQuestion(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                showError("Failed to load answers");
            }
        });
    }

    private void displayQuestion(int index) {
        if (index < 0 || index >= questionList.size()) return;

        View quizView = container.getChildAt(0);
        if (quizView == null) return;

        HashMap<String, String> question = questionList.get(index);
        String userAnswer = userAnswers.get(index);
        String correctAnswer = question.get("correctAnswer");

        // Find all views
        TextView questionNumber = quizView.findViewById(R.id.questionNumber);
        TextView qNum = quizView.findViewById(R.id.qNum);
        TextView questionText = quizView.findViewById(R.id.question);

        MaterialCardView choiceA = quizView.findViewById(R.id.choiceA);
        MaterialCardView choiceB = quizView.findViewById(R.id.choiceB);
        MaterialCardView choiceC = quizView.findViewById(R.id.choiceC);
        MaterialCardView choiceD = quizView.findViewById(R.id.choiceD);

        TextView choiceAText = quizView.findViewById(R.id.choiceAText);
        TextView choiceBText = quizView.findViewById(R.id.choiceBText);
        TextView choiceCText = quizView.findViewById(R.id.choiceCText);
        TextView choiceDText = quizView.findViewById(R.id.choiceDText);

        TextView choiceALetter = quizView.findViewById(R.id.choiceALetter);
        TextView choiceBLetter = quizView.findViewById(R.id.choiceBLetter);
        TextView choiceCLetter = quizView.findViewById(R.id.choiceCLetter);
        TextView choiceDLetter = quizView.findViewById(R.id.choiceDLetter);

        LinearLayout choiceABg = quizView.findViewById(R.id.choiceABg);
        LinearLayout choiceBBg = quizView.findViewById(R.id.choiceBBg);
        LinearLayout choiceCBg = quizView.findViewById(R.id.choiceCBg);
        LinearLayout choiceDBg = quizView.findViewById(R.id.choiceDBg);

        // Set question data
        questionNumber.setText("Question " + (index + 1) + " of " + questionList.size());
        qNum.setText((index + 1) + ".");
        questionText.setText(Html.fromHtml(question.get("question"), Html.FROM_HTML_MODE_COMPACT));

        // Set choices
        choiceAText.setText(Html.fromHtml(question.get("choiceA"), Html.FROM_HTML_MODE_COMPACT));
        choiceBText.setText(Html.fromHtml(question.get("choiceB"), Html.FROM_HTML_MODE_COMPACT));
        choiceCText.setText(Html.fromHtml(question.get("choiceC"), Html.FROM_HTML_MODE_COMPACT));
        choiceDText.setText(Html.fromHtml(question.get("choiceD"), Html.FROM_HTML_MODE_COMPACT));

        // Reset all choices to default style
        resetChoice(choiceA, choiceABg, choiceAText, choiceALetter);
        resetChoice(choiceB, choiceBBg, choiceBText, choiceBLetter);
        resetChoice(choiceC, choiceCBg, choiceCText, choiceCLetter);
        resetChoice(choiceD, choiceDBg, choiceDText, choiceDLetter);

        // Highlight user's answer and correct answer
        boolean isCorrect = userAnswer != null && userAnswer.equals(correctAnswer);

        if (userAnswer != null) {
            switch (userAnswer) {
                case "A":
                    highlightAnswer(choiceA, choiceABg, choiceAText, choiceALetter, isCorrect);
                    break;
                case "B":
                    highlightAnswer(choiceB, choiceBBg, choiceBText, choiceBLetter, isCorrect);
                    break;
                case "C":
                    highlightAnswer(choiceC, choiceCBg, choiceCText, choiceCLetter, isCorrect);
                    break;
                case "D":
                    highlightAnswer(choiceD, choiceDBg, choiceDText, choiceDLetter, isCorrect);
                    break;
            }
        }

        // Always highlight correct answer in green if user got it wrong
        if (!isCorrect && correctAnswer != null) {
            switch (correctAnswer) {
                case "A":
                    highlightCorrectAnswer(choiceA, choiceABg, choiceAText, choiceALetter);
                    break;
                case "B":
                    highlightCorrectAnswer(choiceB, choiceBBg, choiceBText, choiceBLetter);
                    break;
                case "C":
                    highlightCorrectAnswer(choiceC, choiceCBg, choiceCText, choiceCLetter);
                    break;
                case "D":
                    highlightCorrectAnswer(choiceD, choiceDBg, choiceDText, choiceDLetter);
                    break;
            }
        }

        // Update navigation buttons
        updateNavigationButtons(index);
    }

    private void resetChoice(MaterialCardView card, LinearLayout bg, TextView text, TextView letter) {
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        card.setStrokeWidth(2);
        bg.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bgText));
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        letter.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        letter.setBackgroundResource(R.drawable.choice_circle_background);
    }

    private void highlightAnswer(MaterialCardView card, LinearLayout bg, TextView text,
                                 TextView letter, boolean isCorrect) {
        if (isCorrect) {
            // Green for correct answer
            card.setStrokeColor(Color.parseColor("#4CAF50"));
            card.setStrokeWidth(6);
            bg.setBackgroundColor(Color.parseColor("#C8E6C9"));
            text.setTextColor(Color.parseColor("#2E7D32"));
            letter.setTextColor(Color.WHITE);
            letter.setBackgroundResource(R.drawable.choice_circle_selected);
        } else {
            // Red for wrong answer
            card.setStrokeColor(Color.parseColor("#F44336"));
            card.setStrokeWidth(6);
            bg.setBackgroundColor(Color.parseColor("#FFCDD2"));
            text.setTextColor(Color.parseColor("#C62828"));
            letter.setTextColor(Color.WHITE);
            letter.setBackgroundResource(R.drawable.choice_circle_selected);
        }
    }

    private void highlightCorrectAnswer(MaterialCardView card, LinearLayout bg,
                                        TextView text, TextView letter) {
        card.setStrokeColor(Color.parseColor("#4CAF50"));
        card.setStrokeWidth(6);
        bg.setBackgroundColor(Color.parseColor("#C8E6C9"));
        text.setTextColor(Color.parseColor("#2E7D32"));
        letter.setTextColor(Color.WHITE);
        letter.setBackgroundResource(R.drawable.choice_circle_selected);
    }

    private void updateNavigationButtons(int index) {
        // Previous button
        if (index > 0) {
            btnPrev.setEnabled(true);
            btnPrev.setAlpha(1f);
            btnPrev.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));
            btnPrev.setTextColor(Color.WHITE);
        } else {
            btnPrev.setEnabled(false);
            btnPrev.setAlpha(0.5f);
            btnPrev.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnPrev.setTextColor(Color.BLACK);
        }

        // Next button
        if (index < questionList.size() - 1) {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1f);
            btnNext.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));
            btnNext.setTextColor(Color.WHITE);
            btnNext.setText("Next");
        } else {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
            btnNext.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnNext.setTextColor(Color.BLACK);
            btnNext.setText("End");
        }
    }

    private void loadCodingContent() {
        container.removeAllViews();

        TextView placeholder = new TextView(requireContext());
        placeholder.setText("Coding Exercise Review\n\nComing soon...");
        placeholder.setPadding(32, 32, 32, 32);
        placeholder.setTextSize(16);
        placeholder.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        container.addView(placeholder);
    }

    private void showError(String message) {
        container.removeAllViews();

        TextView error = new TextView(requireContext());
        error.setText("Error: " + message);
        error.setPadding(32, 32, 32, 32);
        error.setTextColor(Color.RED);
        container.addView(error);
    }
}