package com.example.codex;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import java.util.Map;

public class ReviewDisplayFragment extends Fragment {

    private static final String ARG_LESSON_ID = "lessonId";
    private static final String ARG_TITLE = "mainTitle";
    private static final String ARG_DIFFICULTY = "difficulty";
    private static final String TAG = "ReviewDisplayFragment";

    private String lessonId, mainTitle, difficulty;
    private MaterialButton btnPrev, btnNext;
    private LinearLayout container;
    private ImageView backButton;
    private TextView headerTitle;
    private Spinner assessmentTypeSpinner;

    private SessionManager sessionManager;
    private int userId;

    private List<String> assessmentTypes = new ArrayList<>();
    private String currentAssessmentType = "Quiz";

    // Availability flags
    private boolean codingAvailable = false;
    private boolean machineProblemAvailable = false;
    private boolean syntaxErrorAvailable = false;
    private boolean programTracingAvailable = false;
    private boolean quizTaken = false;

    // Quiz data
    private List<HashMap<String, String>> questionList = new ArrayList<>();
    private HashMap<Integer, String> userAnswers = new HashMap<>();
    private int currentQuestionIndex = 0;

    // Syntax Error data
    private List<Exercise> syntaxExerciseList = new ArrayList<>();
    private Map<Integer, SyntaxSubmission> syntaxSubmissions = new HashMap<>();
    private int currentSyntaxIndex = 0;

    // Program Tracing data
    private List<TracingExercise> tracingExerciseList = new ArrayList<>();
    private Map<Integer, TracingAnswerSubmission> tracingSubmissions = new HashMap<>();
    private int currentTracingIndex = 0;
    // Fragment Lifecycle Methods

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

        // Find views
        container = view.findViewById(R.id.review_container);
        assessmentTypeSpinner = view.findViewById(R.id.assessmentTypeSpinner);
        backButton = view.findViewById(R.id.back);
        headerTitle = view.findViewById(R.id.headerTitle);

        if (container == null || assessmentTypeSpinner == null) {
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

        // Check all assessments availability
        checkAllAssessmentsAvailability();
    }
    // Assessment Availability Methods

    private void checkAllAssessmentsAvailability() {
        if (!isAdded() || lessonId == null || lessonId.isEmpty()) {
            Log.w(TAG, "LessonId is null or empty.");
            setupAssessmentTypeSpinner();
            return;
        }

        DatabaseReference assessmentRef = FirebaseDatabase.getInstance()
                .getReference("assessment")
                .child(lessonId);

        assessmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                // Check Machine Problem
                if (snapshot.hasChild("MachineProblem")) {
                    DataSnapshot mpSnap = snapshot.child("MachineProblem");
                    for (DataSnapshot child : mpSnap.getChildren()) {
                        if (child.exists() && child.getValue() != null) {
                            machineProblemAvailable = true;
                            break;
                        }
                    }
                }

                // Check Finding Syntax Error
                if (snapshot.hasChild("FindingSyntaxError")) {
                    DataSnapshot seSnap = snapshot.child("FindingSyntaxError");
                    int validCount = 0;
                    for (DataSnapshot child : seSnap.getChildren()) {
                        if (child.exists() && child.getValue() != null) {
                            validCount++;
                        }
                    }
                    syntaxErrorAvailable = validCount > 1;
                }

                // Check Program Tracing
                if (snapshot.hasChild("ProgramTracing")) {
                    DataSnapshot ptSnap = snapshot.child("ProgramTracing");
                    int validCount = 0;
                    for (DataSnapshot child : ptSnap.getChildren()) {
                        if (child.exists() && child.getValue() != null) {
                            validCount++;
                        }
                    }
                    programTracingAvailable = validCount > 1;
                }

                // Check Coding Exercises
                DatabaseReference codingRef = FirebaseDatabase.getInstance()
                        .getReference("coding_exercises")
                        .child(lessonId);

                codingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot codingSnapshot) {
                        if (!isAdded()) return;

                        codingAvailable = codingSnapshot.exists() && codingSnapshot.getChildrenCount() > 1;

                        Log.d(TAG, "Assessments available - Coding: " + codingAvailable +
                                ", MP: " + machineProblemAvailable +
                                ", Syntax: " + syntaxErrorAvailable +
                                ", Tracing: " + programTracingAvailable);

                        setupAssessmentTypeSpinner();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Error checking coding exercises: " + error.getMessage());
                        setupAssessmentTypeSpinner();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Error checking assessments: " + error.getMessage());
                setupAssessmentTypeSpinner();
            }
        });
    }
    // Spinner Setup Methods

    private void setupAssessmentTypeSpinner() {
        assessmentTypes.clear();

        // Always add Quiz first (most common)
        assessmentTypes.add("Quiz");

        // Add only available assessments
        if (codingAvailable) {
            assessmentTypes.add("Exercise");
        }
        if (machineProblemAvailable) {
            assessmentTypes.add("Machine Problem");
        }
        if (syntaxErrorAvailable) {
            assessmentTypes.add("Finding Syntax Error");
        }
        if (programTracingAvailable) {
            assessmentTypes.add("Program Tracing");
        }

        // Create custom adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                assessmentTypes
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                text.setTextSize(16);
                text.setPadding(16, 16, 16, 16);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                text.setTextSize(16);
                text.setPadding(32, 24, 32, 24);

                // Highlight selected item
                if (assessmentTypes.get(position).equals(currentAssessmentType)) {
                    view.setBackgroundColor(Color.parseColor("#E3F2FD"));
                } else {
                    view.setBackgroundColor(Color.WHITE);
                }

                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assessmentTypeSpinner.setAdapter(adapter);

        assessmentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentAssessmentType = assessmentTypes.get(position);
                currentQuestionIndex = 0;
                currentSyntaxIndex = 0;
                currentTracingIndex = 0;
                loadAssessmentContent();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Load initial content (Quiz by default)
        loadAssessmentContent();
    }

    private void loadAssessmentContent() {
        switch (currentAssessmentType) {
            case "Quiz":
                loadQuizContent();
                break;
            case "Exercise":
                openExerciseActivity();
                break;
            case "Machine Problem":
                loadMachineProblemContent();
                break;
            case "Finding Syntax Error":
                loadSyntaxErrorContent();
                break;
            case "Program Tracing":
                loadProgramTracingContent();
                break;
            default:
                loadQuizContent();
        }
    }

    private void openExerciseActivity() {
        Intent intent = new Intent(requireContext(), ExerciseActivity.class);
        intent.putExtra("lessonId", lessonId);
        intent.putExtra("mainTitle", mainTitle);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);

        // Reset spinner back to Quiz after opening exercise
        assessmentTypeSpinner.setSelection(0);
    }
    // Quiz Review Methods

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

    // Modify the loadQuizData() method to check if quiz was taken:
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

                // First check if user has taken the quiz
                DatabaseReference quizResultsRef = FirebaseDatabase.getInstance()
                        .getReference("quizResults")
                        .child(String.valueOf(userId))
                        .child(lessonId);

                quizResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot resultSnapshot) {
                        if (!isAdded()) return;

                        if (!resultSnapshot.exists()) {
                            // User hasn't taken the quiz
                            quizTaken = false;
                            showQuizNotTakenMessage();
                            return;
                        }

                        quizTaken = true;

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
                        showError("Failed to check quiz status");
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

    // Add this new method to show a message when quiz hasn't been taken:
    private void showQuizNotTakenMessage() {
        container.removeAllViews();

        LinearLayout messageLayout = new LinearLayout(requireContext());
        messageLayout.setOrientation(LinearLayout.VERTICAL);
        messageLayout.setGravity(android.view.Gravity.CENTER);
        messageLayout.setPadding(48, 48, 48, 48);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        messageLayout.setLayoutParams(layoutParams);

        // Icon or emoji
        TextView icon = new TextView(requireContext());
        icon.setText("📝");
        icon.setTextSize(48);
        icon.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        iconParams.bottomMargin = 16;
        icon.setLayoutParams(iconParams);

        // Title
        TextView title = new TextView(requireContext());
        title.setText("Quiz Not Taken");
        title.setTextSize(20);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = 8;
        title.setLayoutParams(titleParams);

        // Message
        TextView message = new TextView(requireContext());
        message.setText("You haven't taken this quiz yet.\nPlease complete the quiz first to view your review.");
        message.setTextSize(16);
        message.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        message.setGravity(android.view.Gravity.CENTER);
        message.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        messageLayout.addView(icon);
        messageLayout.addView(title);
        messageLayout.addView(message);

        container.addView(messageLayout);
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
    // Quiz Display Methods

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
            card.setStrokeColor(Color.parseColor("#4CAF50"));
            card.setStrokeWidth(6);
            bg.setBackgroundColor(Color.parseColor("#C8E6C9"));
            text.setTextColor(Color.parseColor("#2E7D32"));
            letter.setTextColor(Color.WHITE);
            letter.setBackgroundResource(R.drawable.choice_circle_selected);
        } else {
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
            btnPrev.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#03162A")));
            btnPrev.setTextColor(Color.WHITE);
        } else {
            btnPrev.setEnabled(false);
            btnPrev.setAlpha(0.5f);
            btnPrev.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnPrev.setTextColor(Color.BLACK);
        }

        // Next button
        if (index < questionList.size() - 1) {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1f);
            btnNext.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#03162A")));
            btnNext.setTextColor(Color.WHITE);
            btnNext.setText("Next");
        } else {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
            btnNext.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnNext.setTextColor(Color.BLACK);
            btnNext.setText("End");
        }
    }
    // Machine Problem Review Methods

    private void loadMachineProblemContent() {
        container.removeAllViews();

        // Inflate machine_problem.xml
        View mpLayout = getLayoutInflater().inflate(R.layout.activity_machine_problem, container, false);
        container.addView(mpLayout);

        // Hide/Remove submit button and run button (review mode)
        MaterialButton submitButton = mpLayout.findViewById(R.id.submitButton);
        MaterialButton runButton = mpLayout.findViewById(R.id.runButton);
        if (submitButton != null) submitButton.setVisibility(View.GONE);
        if (runButton != null) runButton.setVisibility(View.GONE);

        // Load exercise data
        DatabaseReference exerciseRef = FirebaseDatabase.getInstance()
                .getReference("assessment")
                .child(lessonId)
                .child("MachineProblem");

        exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Exercise exercise = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Exercise ex = child.getValue(Exercise.class);
                    if (ex != null) {
                        exercise = ex;
                        break;
                    }
                }

                if (exercise == null) {
                    showError("Machine problem not found");
                    return;
                }

                final Exercise finalExercise = exercise;

                // Load user answer
                DatabaseReference userAnswerRef = FirebaseDatabase.getInstance()
                        .getReference("userMachineProblemAnswers")
                        .child(String.valueOf(userId))
                        .child(lessonId);

                userAnswerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot answerSnapshot) {
                        if (!isAdded()) return;

                        String userCode = "";
                        String userOutput = "";
                        Boolean isCorrect = false;

                        if (answerSnapshot.exists()) {
                            userCode = answerSnapshot.child("code").getValue(String.class);
                            userOutput = answerSnapshot.child("output").getValue(String.class);
                            isCorrect = answerSnapshot.child("isCorrect").getValue(Boolean.class);
                        }

                        displayMachineProblemReview(mpLayout, finalExercise,
                                userCode != null ? userCode : "",
                                userOutput != null ? userOutput : "",
                                isCorrect != null ? isCorrect : false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        showError("Failed to load user answer");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                showError("Failed to load machine problem");
            }
        });
    }

    private void displayMachineProblemReview(View mpLayout, Exercise exercise,
                                             String userCode, String userOutput, boolean isCorrect) {

        // Find views from the inflated layout
        TextView exerciseTitle = mpLayout.findViewById(R.id.exerciseTitle);
        ConstraintLayout header = mpLayout.findViewById(R.id.constraintLayout);
        header.setVisibility(View.GONE);
        TextView descriptionBox = mpLayout.findViewById(R.id.descriptionBox);
        EditText codeEditor = mpLayout.findViewById(R.id.codeEditor);
        TextView expectedOutputBox = mpLayout.findViewById(R.id.expectedOutputBox);
        TextView outputBox = mpLayout.findViewById(R.id.outputBox);

        // Set title and description
        if (exerciseTitle != null) {
            exerciseTitle.setText(exercise.title);
        }

        if (descriptionBox != null) {
            String html = exercise.description.replace("&nbsp;", " ")
                    .replace("word-break: break-all;", "")
                    .replace("word-wrap: break-word;", "");
            descriptionBox.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        }

        // Set expected output
        if (expectedOutputBox != null) {
            expectedOutputBox.setText(exercise.expectedOutput);
        }

        // Make code editor READ-ONLY
        if (codeEditor != null) {
            // NEW: Check if user has submitted code
            if (userCode.isEmpty()) {
                codeEditor.setText("None - No code submitted yet");
                codeEditor.setTextColor(Color.parseColor("#999999"));
            } else {
                codeEditor.setText(userCode);
                codeEditor.setTextColor(Color.parseColor("#424242"));
            }

            codeEditor.setEnabled(false);
            codeEditor.setFocusable(false);
            codeEditor.setFocusableInTouchMode(false);
            codeEditor.setKeyListener(null);
            codeEditor.setCursorVisible(false);
            codeEditor.setTextIsSelectable(false);
            codeEditor.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        // Set user output with color based on correctness
        if (outputBox != null) {
            // NEW: Check if user has output
            if (userOutput.isEmpty()) {
                outputBox.setText("None - No output yet");
                outputBox.setTextColor(Color.parseColor("#999999"));
            } else {
                outputBox.setText(userOutput);
                outputBox.setTextColor(isCorrect ?
                        Color.parseColor("#06651A") : Color.parseColor("#E31414"));
            }
        }
    }
    // Syntax Error Review Methods

    private void loadSyntaxErrorContent() {
        container.removeAllViews();

        // Inflate find_syntax_error.xml
        View syntaxLayout = getLayoutInflater().inflate(R.layout.activity_syntax_error, container, false);
        container.addView(syntaxLayout);

        // Hide run button and next button initially
        MaterialButton runButton = syntaxLayout.findViewById(R.id.runButton);
        MaterialButton nextButton = syntaxLayout.findViewById(R.id.nextCode);
        if (runButton != null) runButton.setVisibility(View.GONE);
        if (nextButton != null) nextButton.setVisibility(View.GONE);

        // Load exercises
        DatabaseReference exerciseRef = FirebaseDatabase.getInstance()
                .getReference("assessment")
                .child(lessonId)
                .child("FindingSyntaxError");

        exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                syntaxExerciseList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Exercise ex = child.getValue(Exercise.class);
                    if (ex != null) {
                        syntaxExerciseList.add(ex);
                    }
                }

                syntaxExerciseList.removeIf(ex -> ex == null);
                if (syntaxExerciseList.size() > 2) {
                    syntaxExerciseList = syntaxExerciseList.subList(0, 2);
                }

                if (syntaxExerciseList.isEmpty()) {
                    showError("No syntax error exercises found");
                    return;
                }

                // Load user answers
                loadSyntaxErrorAnswers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                showError("Failed to load syntax error exercises");
            }
        });
    }

    private void loadSyntaxErrorAnswers() {
        DatabaseReference answersRef = FirebaseDatabase.getInstance()
                .getReference("userSyntaxErrorAnswers")
                .child(String.valueOf(userId))
                .child(lessonId);

        answersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                syntaxSubmissions.clear();
                for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                    try {
                        int index = Integer.parseInt(exerciseSnap.getKey());
                        String code = exerciseSnap.child("code").getValue(String.class);
                        String output = exerciseSnap.child("output").getValue(String.class);
                        Boolean isCorrect = exerciseSnap.child("isCorrect").getValue(Boolean.class);

                        if (code != null && output != null && isCorrect != null) {
                            syntaxSubmissions.put(index,
                                    new SyntaxSubmission(code, output, isCorrect));
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid index: " + exerciseSnap.getKey());
                    }
                }

                currentSyntaxIndex = 0;
                displaySyntaxErrorReview();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                showError("Failed to load user answers");
            }
        });
    }

    private void displaySyntaxErrorReview() {
        if (currentSyntaxIndex < 0 || currentSyntaxIndex >= syntaxExerciseList.size()) {
            return;
        }

        View syntaxView = container.getChildAt(0);
        if (syntaxView == null) return;

        Exercise exercise = syntaxExerciseList.get(currentSyntaxIndex);
        SyntaxSubmission submission = syntaxSubmissions.get(currentSyntaxIndex + 1);

        // Find views
        ConstraintLayout header = syntaxView.findViewById(R.id.constraintLayout);
        header.setVisibility(View.GONE);
        TextView exerciseTitle = syntaxView.findViewById(R.id.exerciseTitle);
        TextView descriptionBox = syntaxView.findViewById(R.id.descriptionBox);
        EditText codeEditor = syntaxView.findViewById(R.id.codeEditor);
        TextView expectedOutputBox = syntaxView.findViewById(R.id.expectedOutputBox);
        TextView outputBox = syntaxView.findViewById(R.id.outputBox);
        TextView userClass = syntaxView.findViewById(R.id.user_class);

        // Set title and description
        if (exerciseTitle != null) {
            exerciseTitle.setText(exercise.title);
        }

        if (descriptionBox != null) {
            String html = exercise.description.replace("&nbsp;", " ")
                    .replace("word-break: break-all;", "")
                    .replace("word-wrap: break-word;", "");
            descriptionBox.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        }

        // Set user class/difficulty
        if (userClass != null && difficulty != null) {
            userClass.setText(difficulty);
            userClass.setVisibility(View.VISIBLE);
        }

        // Set expected output
        if (expectedOutputBox != null) {
            expectedOutputBox.setText(exercise.expectedOutput);
        }

        // Make code editor READ-ONLY
        if (codeEditor != null) {
            // NEW: Check if submission exists
            if (submission != null) {
                codeEditor.setText(submission.code);
                codeEditor.setTextColor(Color.parseColor("#424242"));
            } else {
                codeEditor.setText("None - No code submitted yet");
                codeEditor.setTextColor(Color.parseColor("#999999"));
            }

            codeEditor.setEnabled(false);
            codeEditor.setFocusable(false);
            codeEditor.setFocusableInTouchMode(false);
            codeEditor.setKeyListener(null);
            codeEditor.setCursorVisible(false);
            codeEditor.setTextIsSelectable(false);
            codeEditor.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        // Set user output with color based on correctness
        if (outputBox != null) {
            // NEW: Check if submission exists
            if (submission != null) {
                outputBox.setText(submission.output);
                outputBox.setTextColor(submission.isCorrect ?
                        Color.parseColor("#06651A") : Color.parseColor("#E31414"));
            } else {
                outputBox.setText("None - No output yet");
                outputBox.setTextColor(Color.parseColor("#999999"));
            }
        }

        // Setup navigation
        setupSyntaxErrorNavigation(syntaxView);
    }

    private void setupSyntaxErrorNavigation(View syntaxView) {
        ScrollView scrollCoding = syntaxView.findViewById(R.id.scrollCoding);
        if (scrollCoding != null && scrollCoding.getChildAt(0) instanceof ViewGroup) {
            ViewGroup mainContainer = (ViewGroup) scrollCoding.getChildAt(0);

            View existingNav = mainContainer.findViewWithTag("nav_buttons");
            if (existingNav != null) mainContainer.removeView(existingNav);

            SyntaxSubmission submission = syntaxSubmissions.get(currentSyntaxIndex + 1);

            LinearLayout navLayout = new LinearLayout(requireContext());
            navLayout.setTag("nav_buttons");
            navLayout.setOrientation(LinearLayout.HORIZONTAL);
            navLayout.setGravity(android.view.Gravity.CENTER);
            navLayout.setPadding(16, 8, 16, 16);

            LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            navLayout.setLayoutParams(navParams);

            // Previous button
            MaterialButton prevBtn = new MaterialButton(requireContext());
            prevBtn.setText("Previous");
            prevBtn.setEnabled(currentSyntaxIndex > 0);

            LinearLayout.LayoutParams prevBtnParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT);
            prevBtnParams.weight = 1;
            prevBtnParams.rightMargin = 8;
            prevBtn.setLayoutParams(prevBtnParams);

            if (currentSyntaxIndex > 0) {
                prevBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.primary)));
                prevBtn.setTextColor(Color.WHITE);
                prevBtn.setAlpha(1f);
            } else {
                prevBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#E0E0E0")));
                prevBtn.setTextColor(Color.parseColor("#9E9E9E"));
                prevBtn.setAlpha(0.5f);
            }

            prevBtn.setOnClickListener(v -> {
                if (currentSyntaxIndex > 0) {
                    currentSyntaxIndex--;
                    displaySyntaxErrorReview();
                }
            });

            // Next button
            MaterialButton nextBtn = new MaterialButton(requireContext());
            nextBtn.setText(currentSyntaxIndex < syntaxExerciseList.size() - 1 ? "Next" : "End");
            nextBtn.setEnabled(currentSyntaxIndex < syntaxExerciseList.size() - 1);

            LinearLayout.LayoutParams nextBtnParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT);
            nextBtnParams.weight = 1;
            nextBtnParams.leftMargin = 8;
            nextBtn.setLayoutParams(nextBtnParams);

            if (currentSyntaxIndex < syntaxExerciseList.size() - 1) {
                nextBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.primary)));
                nextBtn.setTextColor(Color.WHITE);
                nextBtn.setAlpha(1f);
            } else {
                nextBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#E0E0E0")));
                nextBtn.setTextColor(Color.parseColor("#9E9E9E"));
                nextBtn.setAlpha(0.5f);
            }

            nextBtn.setOnClickListener(v -> {
                if (currentSyntaxIndex < syntaxExerciseList.size() - 1) {
                    currentSyntaxIndex++;
                    displaySyntaxErrorReview();
                }
            });

            navLayout.addView(prevBtn);
            navLayout.addView(nextBtn);
            mainContainer.addView(navLayout, 1); // Insert at position 1 (after status)
        }
    }
    // Program Tracing Review Methods

    private void loadProgramTracingContent() {
        container.removeAllViews();

        // Inflate program_tracing.xml
        View tracingLayout = getLayoutInflater().inflate(R.layout.activity_program_tracing, container, false);
        container.addView(tracingLayout);

        // Hide check answer and next button initially
        MaterialButton checkButton = tracingLayout.findViewById(R.id.checkButton);
        MaterialButton nextButton = tracingLayout.findViewById(R.id.nextTracing);
        TextView feedbackText = tracingLayout.findViewById(R.id.feedbackText);

        if (checkButton != null) checkButton.setVisibility(View.GONE);
        if (nextButton != null) nextButton.setVisibility(View.GONE);
        if (feedbackText != null) feedbackText.setVisibility(View.GONE);

        // Load exercises
        DatabaseReference exerciseRef = FirebaseDatabase.getInstance()
                .getReference("assessment")
                .child(lessonId)
                .child("ProgramTracing");

        exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                tracingExerciseList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    TracingExercise ex = child.getValue(TracingExercise.class);
                    if (ex != null) {
                        tracingExerciseList.add(ex);
                    }
                }

                tracingExerciseList.removeIf(ex -> ex == null);
                if (tracingExerciseList.size() > 2) {
                    tracingExerciseList = tracingExerciseList.subList(0, 2);
                }

                if (tracingExerciseList.isEmpty()) {
                    showError("No program tracing exercises found");
                    return;
                }

                // Load user answers
                loadProgramTracingAnswers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                showError("Failed to load program tracing exercises");
            }
        });
    }

    private void loadProgramTracingAnswers() {
        DatabaseReference answersRef = FirebaseDatabase.getInstance()
                .getReference("userTracingAnswers")
                .child(String.valueOf(userId))
                .child(lessonId);

        answersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                tracingSubmissions.clear();
                for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                    try {
                        int index = Integer.parseInt(exerciseSnap.getKey());
                        String answer = exerciseSnap.child("answer").getValue(String.class);
                        Boolean isCorrect = exerciseSnap.child("isCorrect").getValue(Boolean.class);

                        if (answer != null && isCorrect != null) {
                            tracingSubmissions.put(index,
                                    new TracingAnswerSubmission(answer, isCorrect));
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid index: " + exerciseSnap.getKey());
                    }
                }

                currentTracingIndex = 0;
                displayProgramTracingReview();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                showError("Failed to load user answers");
            }
        });
    }

    private void displayProgramTracingReview() {
        if (currentTracingIndex < 0 || currentTracingIndex >= tracingExerciseList.size()) {
            return;
        }

        View tracingView = container.getChildAt(0);
        if (tracingView == null) return;

        TracingExercise exercise = tracingExerciseList.get(currentTracingIndex);
        TracingAnswerSubmission submission = tracingSubmissions.get(currentTracingIndex + 1);

        // Find views
        TextView exerciseTitle = tracingView.findViewById(R.id.exerciseTitle);
        TextView descriptionBox = tracingView.findViewById(R.id.descriptionBox);
        TextView codeDisplayText = tracingView.findViewById(R.id.codeDisplay_text);
        EditText answerInput = tracingView.findViewById(R.id.answerInput);
        TextView userClass = tracingView.findViewById(R.id.user_class);
        ConstraintLayout header = tracingView.findViewById(R.id.constraintLayout);
        header.setVisibility(View.GONE);

        // Set title and description
        if (exerciseTitle != null) {
            exerciseTitle.setText(exercise.title);
        }

        if (descriptionBox != null) {
            String html = exercise.description.replace("&nbsp;", " ")
                    .replace("word-break: break-all;", "")
                    .replace("word-wrap: break-word;", "");
            descriptionBox.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        }

        // Set user class/difficulty
        if (userClass != null && difficulty != null) {
            userClass.setText(difficulty);
            userClass.setVisibility(View.VISIBLE);
        }

        // Set code display (already read-only as TextView)
        if (codeDisplayText != null) {
            String formattedCode = exercise.code
                    .replace("&nbsp;", " ")
                    .replace("&emsp;", "\t")
                    .replace("<br>", "\n");
            codeDisplayText.setText(formattedCode);
        }

        // Make answer input READ-ONLY and show submission status
        if (answerInput != null) {
            // NEW: Check if submission exists
            if (submission != null) {
                answerInput.setText(submission.answer);
                answerInput.setTextColor(submission.isCorrect ?
                        Color.parseColor("#06651A") : Color.parseColor("#E31414"));
                answerInput.setHint(""); // Clear hint if there's an answer
            } else {
                answerInput.setText(""); // Keep empty
                answerInput.setHint("None - No answer submitted yet");
                answerInput.setHintTextColor(Color.parseColor("#999999"));
                answerInput.setTextColor(Color.parseColor("#999999"));
            }

            answerInput.setEnabled(false);
            answerInput.setFocusable(false);
            answerInput.setFocusableInTouchMode(false);
            answerInput.setKeyListener(null);
            answerInput.setCursorVisible(false);
            answerInput.setTextIsSelectable(false);
            answerInput.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        // Setup navigation and display expected output
        setupProgramTracingNavigation(tracingView, exercise, submission);
    }

    private void setupProgramTracingNavigation(View tracingView, TracingExercise exercise,
                                               TracingAnswerSubmission submission) {
        ScrollView scrollTracing = tracingView.findViewById(R.id.scrollTracing);
        if (scrollTracing != null && scrollTracing.getChildAt(0) instanceof ViewGroup) {
            ViewGroup mainContainer = (ViewGroup) scrollTracing.getChildAt(0);

            View existingNav = mainContainer.findViewWithTag("nav_buttons");
            if (existingNav != null) mainContainer.removeView(existingNav);

            // Add Expected Output Section
            LinearLayout expectedSection = new LinearLayout(requireContext());
            expectedSection.setTag("expected_output_section");
            expectedSection.setOrientation(LinearLayout.VERTICAL);
            expectedSection.setPadding(16, 16, 16, 0);

            // Add navigation buttons
            LinearLayout navLayout = new LinearLayout(requireContext());
            navLayout.setTag("nav_buttons");
            navLayout.setOrientation(LinearLayout.HORIZONTAL);
            navLayout.setGravity(android.view.Gravity.CENTER);
            navLayout.setPadding(16, 16, 16, 24);

            LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            navLayout.setLayoutParams(navParams);

            // Previous button
            MaterialButton prevBtn = new MaterialButton(requireContext());
            prevBtn.setText("Previous");
            prevBtn.setEnabled(currentTracingIndex > 0);

            LinearLayout.LayoutParams prevBtnParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT);
            prevBtnParams.weight = 1;
            prevBtnParams.rightMargin = 8;
            prevBtn.setLayoutParams(prevBtnParams);

            if (currentTracingIndex > 0) {
                prevBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.primary)));
                prevBtn.setTextColor(Color.WHITE);
                prevBtn.setAlpha(1f);
            } else {
                prevBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#E0E0E0")));
                prevBtn.setTextColor(Color.parseColor("#9E9E9E"));
                prevBtn.setAlpha(0.5f);
            }

            prevBtn.setOnClickListener(v -> {
                if (currentTracingIndex > 0) {
                    currentTracingIndex--;
                    displayProgramTracingReview();
                }
            });

            // Next button
            MaterialButton nextBtn = new MaterialButton(requireContext());
            nextBtn.setText(currentTracingIndex < tracingExerciseList.size() - 1 ? "Next" : "End");
            nextBtn.setEnabled(currentTracingIndex < tracingExerciseList.size() - 1);

            LinearLayout.LayoutParams nextBtnParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT);
            nextBtnParams.weight = 1;
            nextBtnParams.leftMargin = 8;
            nextBtn.setLayoutParams(nextBtnParams);

            if (currentTracingIndex < tracingExerciseList.size() - 1) {
                nextBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.primary)));
                nextBtn.setTextColor(Color.WHITE);
                nextBtn.setAlpha(1f);
            } else {
                nextBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#E0E0E0")));
                nextBtn.setTextColor(Color.parseColor("#9E9E9E"));
                nextBtn.setAlpha(0.5f);
            }

            nextBtn.setOnClickListener(v -> {
                if (currentTracingIndex < tracingExerciseList.size() - 1) {
                    currentTracingIndex++;
                    displayProgramTracingReview();
                }
            });

            navLayout.addView(prevBtn);
            navLayout.addView(nextBtn);
            mainContainer.addView(navLayout);
        }
    }
    // Helper Methods

    private void showPlaceholder(String title, String message) {
        container.removeAllViews();

        TextView placeholder = new TextView(requireContext());
        placeholder.setText(title + "\n\n" + message);
        placeholder.setPadding(32, 32, 32, 32);
        placeholder.setTextSize(16);
        placeholder.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        placeholder.setGravity(android.view.Gravity.CENTER);
        container.addView(placeholder);
    }

    private void showError(String message) {
        container.removeAllViews();

        TextView error = new TextView(requireContext());
        error.setText("Error: " + message);
        error.setPadding(32, 32, 32, 32);
        error.setTextSize(16);
        error.setTextColor(Color.RED);
        error.setGravity(android.view.Gravity.CENTER);
        container.addView(error);
    }

    // Helper Classes

    public static class SyntaxSubmission {
        public String code;
        public String output;
        public boolean isCorrect;

        public SyntaxSubmission() {}

        public SyntaxSubmission(String code, String output, boolean isCorrect) {
            this.code = code;
            this.output = output;
            this.isCorrect = isCorrect;
        }
    }

    public static class TracingAnswerSubmission {
        public String answer;
        public boolean isCorrect;

        public TracingAnswerSubmission() {}

        public TracingAnswerSubmission(String answer, boolean isCorrect) {
            this.answer = answer;
            this.isCorrect = isCorrect;
        }
    }

    public static class TracingExercise {
        public String title;
        public String description;
        public String code;
        public String expectedOutput;
        public String language;
        public String versionIndex;

        public TracingExercise() {}
    }
}

// End of ReviewDisplayFragment class