package com.example.codex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class AddQuestionActivity extends AppCompatActivity {

    private EditText difficulty, lessonId, inputQuestion, inputChoiceA, inputChoiceB, inputChoiceC, inputChoiceD, inputCorrectAnswer;
    private MaterialButton btnSave;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_question);

        // Firebase base reference
        databaseReference = FirebaseDatabase.getInstance().getReference("quizQuestions");

        // Bind Views
        difficulty = findViewById(R.id.difficulty);
        lessonId = findViewById(R.id.lessonId);
        inputQuestion = findViewById(R.id.inputQuestion);
        inputChoiceA = findViewById(R.id.inputChoiceA);
        inputChoiceB = findViewById(R.id.inputChoiceB);
        inputChoiceC = findViewById(R.id.inputChoiceC);
        inputChoiceD = findViewById(R.id.inputChoiceD);
        inputCorrectAnswer = findViewById(R.id.inputCorrectAnswer);
        btnSave = findViewById(R.id.btnSave);

        // Button listener
        btnSave.setOnClickListener(v -> saveQuestion());
    }

    private void saveQuestion() {
        String difficultyLevel = difficulty.getText().toString().trim();
        String lesson = lessonId.getText().toString().trim();
        String question = inputQuestion.getText().toString().trim();
        String choiceA = inputChoiceA.getText().toString();
        String choiceB = inputChoiceB.getText().toString();
        String choiceC = inputChoiceC.getText().toString();
        String choiceD = inputChoiceD.getText().toString();
        String correct = inputCorrectAnswer.getText().toString().trim().toUpperCase();

        // Validation
        if (TextUtils.isEmpty(difficultyLevel)) {
            Toast.makeText(this, "Please enter a Difficulty level", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(lesson)) {
            Toast.makeText(this, "Please enter a Lesson ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(question) || TextUtils.isEmpty(choiceA) || TextUtils.isEmpty(choiceB)
                || TextUtils.isEmpty(choiceC) || TextUtils.isEmpty(choiceD) || TextUtils.isEmpty(correct)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!correct.matches("[ABCD]")) {
            Toast.makeText(this, "Correct answer must be A, B, C, or D", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert to HTML-safe format (preserve line breaks)
        String htmlQuestion = textToHtml(question);
        String htmlChoiceA = textToHtml(choiceA);
        String htmlChoiceB = textToHtml(choiceB);
        String htmlChoiceC = textToHtml(choiceC);
        String htmlChoiceD = textToHtml(choiceD);

        // Prepare data
        HashMap<String, Object> questionMap = new HashMap<>();
        questionMap.put("question", htmlQuestion);
        questionMap.put("choiceA", htmlChoiceA);
        questionMap.put("choiceB", htmlChoiceB);
        questionMap.put("choiceC", htmlChoiceC);
        questionMap.put("choiceD", htmlChoiceD);
        questionMap.put("answer", correct);

        // Save path: quizQuestions/{difficulty}/{lesson}/{autoId}
        String questionId = databaseReference
                .child(difficultyLevel)
                .child(lesson)
                .push()
                .getKey();

        databaseReference
                .child(difficultyLevel)
                .child(lesson)
                .child(questionId)
                .setValue(questionMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Question saved successfully!", Toast.LENGTH_SHORT).show();
                        clearInputs(); // Only clear question and choices
                    } else {
                        Toast.makeText(this, "Failed to save question", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Converts question text to HTML-safe format
    private String textToHtml(String text) {
        return text
                .replace(" ", "&nbsp;")
                .replace("\t", "&emsp;")
                .replace("\n", "<br>");
    }

    // Clears all question-related inputs but keeps difficulty and lesson ID
    private void clearInputs() {
        inputQuestion.setText("");
        inputChoiceA.setText("");
        inputChoiceB.setText("");
        inputChoiceC.setText("");
        inputChoiceD.setText("");
        inputCorrectAnswer.setText("");
        inputQuestion.requestFocus();
    }

}