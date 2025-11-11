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

    private EditText lessonId, inputQuestion, inputChoiceA, inputChoiceB, inputChoiceC, inputChoiceD, inputCorrectAnswer;
    private MaterialButton btnSave;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_question);

        // Firebase base reference
        databaseReference = FirebaseDatabase.getInstance().getReference("quizQuestions");

        // Bind Views
        lessonId = findViewById(R.id.lessonId);
        inputQuestion = findViewById(R.id.inputQuestion);
        inputChoiceA = findViewById(R.id.inputChoiceA);
        inputChoiceB = findViewById(R.id.inputChoiceB);
        inputChoiceC = findViewById(R.id.inputChoiceC);
        inputChoiceD = findViewById(R.id.inputChoiceD);
        inputCorrectAnswer = findViewById(R.id.inputCorrectAnswer);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveQuestion());
    }

    private void saveQuestion() {
        String lesson = lessonId.getText().toString().trim();
        String question = inputQuestion.getText().toString().trim();
        String choiceA = inputChoiceA.getText().toString().trim();
        String choiceB = inputChoiceB.getText().toString().trim();
        String choiceC = inputChoiceC.getText().toString().trim();
        String choiceD = inputChoiceD.getText().toString().trim();
        String correct = inputCorrectAnswer.getText().toString().trim().toUpperCase();

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

        // Convert text to HTML-safe format (preserve spacing and newlines)
        String htmlQuestion = textToHtml(question);
        String htmlA = textToHtml(choiceA);
        String htmlB = textToHtml(choiceB);
        String htmlC = textToHtml(choiceC);
        String htmlD = textToHtml(choiceD);

        // Prepare data
        HashMap<String, Object> questionMap = new HashMap<>();
        questionMap.put("question", htmlQuestion);
        questionMap.put("choiceA", htmlA);
        questionMap.put("choiceB", htmlB);
        questionMap.put("choiceC", htmlC);
        questionMap.put("choiceD", htmlD);
        questionMap.put("correctAnswer", correct);

        // Save under quizQuestions/{lessonId}/{autoId}
        String questionId = databaseReference.child(lesson).push().getKey();

        databaseReference.child(lesson).child(questionId).setValue(questionMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Question saved successfully!", Toast.LENGTH_SHORT).show();
                        clearInputs();
                    } else {
                        Toast.makeText(this, "Failed to save question", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Converts text to HTML-like safe format
    private String textToHtml(String text) {
        return text
                .replace(" ", "&nbsp;")
                .replace("\t", "&emsp;")
                .replace("\n", "<br>");
    }

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
