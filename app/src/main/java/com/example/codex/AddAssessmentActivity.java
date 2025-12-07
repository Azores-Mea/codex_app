package com.example.codex;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class AddAssessmentActivity extends AppCompatActivity {

    private EditText inputLessonId;
    private Spinner spinnerAssessmentType, spinnerCodeType;
    private LinearLayout layoutQuizFields, layoutCodeFields;

    // Quiz fields
    private EditText inputQuestion, inputChoiceA, inputChoiceB, inputChoiceC, inputChoiceD, inputCorrectAnswer;

    // Code fields (single problem)
    private EditText inputTitle, inputDescription, inputCode, inputExpectedOutput;

    private MaterialButton btnSaveAssessment;
    private DatabaseReference databaseReference;

    private String selectedType = "";
    private String selectedCodeType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_assessment);

        // Firebase base reference
        databaseReference = FirebaseDatabase.getInstance().getReference("assessment");

        // Bind common views
        inputLessonId = findViewById(R.id.inputLessonId);
        spinnerAssessmentType = findViewById(R.id.spinnerAssessmentType);
        spinnerCodeType = findViewById(R.id.spinnerCodeType);
        layoutQuizFields = findViewById(R.id.layoutQuizFields);
        layoutCodeFields = findViewById(R.id.layoutCodeFields);
        btnSaveAssessment = findViewById(R.id.btnSaveAssessment);

        // Bind Quiz fields
        inputQuestion = findViewById(R.id.inputQuestion);
        inputChoiceA = findViewById(R.id.inputChoiceA);
        inputChoiceB = findViewById(R.id.inputChoiceB);
        inputChoiceC = findViewById(R.id.inputChoiceC);
        inputChoiceD = findViewById(R.id.inputChoiceD);
        inputCorrectAnswer = findViewById(R.id.inputCorrectAnswer);

        // Bind Code fields
        inputTitle = findViewById(R.id.inputTitle);
        inputDescription = findViewById(R.id.inputDescription);
        inputCode = findViewById(R.id.inputCode);
        inputExpectedOutput = findViewById(R.id.inputExpectedOutput);

        // Setup spinners
        setupAssessmentTypeSpinner();
        setupCodeTypeSpinner();

        // Save button
        btnSaveAssessment.setOnClickListener(v -> saveAssessment());
    }

    private void setupAssessmentTypeSpinner() {
        String[] types = {"Select Type", "Quiz", "Code"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssessmentType.setAdapter(adapter);

        spinnerAssessmentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = types[position];
                toggleFieldsVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupCodeTypeSpinner() {
        String[] codeTypes = {"Select Code Type", "Finding Syntax Error", "Program Tracing", "Machine Problem"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, codeTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCodeType.setAdapter(adapter);

        spinnerCodeType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCodeType = codeTypes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void toggleFieldsVisibility() {
        if (selectedType.equals("Quiz")) {
            layoutQuizFields.setVisibility(View.VISIBLE);
            layoutCodeFields.setVisibility(View.GONE);
            spinnerCodeType.setVisibility(View.GONE);
        } else if (selectedType.equals("Code")) {
            layoutQuizFields.setVisibility(View.GONE);
            layoutCodeFields.setVisibility(View.VISIBLE);
            spinnerCodeType.setVisibility(View.VISIBLE);
        } else {
            layoutQuizFields.setVisibility(View.GONE);
            layoutCodeFields.setVisibility(View.GONE);
            spinnerCodeType.setVisibility(View.GONE);
        }
    }

    private void saveAssessment() {
        String lessonId = inputLessonId.getText().toString().trim();

        if (TextUtils.isEmpty(lessonId)) {
            Toast.makeText(this, "Please enter Lesson ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedType.equals("Select Type")) {
            Toast.makeText(this, "Please select assessment type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedType.equals("Quiz")) {
            saveQuizAssessment(lessonId);
        } else if (selectedType.equals("Code")) {
            if (selectedCodeType.equals("Select Code Type")) {
                Toast.makeText(this, "Please select code type", Toast.LENGTH_SHORT).show();
                return;
            }
            saveCodeAssessment(lessonId);
        }
    }

    private void saveQuizAssessment(String lessonId) {
        String question = inputQuestion.getText().toString().trim();
        String choiceA = inputChoiceA.getText().toString();
        String choiceB = inputChoiceB.getText().toString();
        String choiceC = inputChoiceC.getText().toString();
        String choiceD = inputChoiceD.getText().toString();
        String correct = inputCorrectAnswer.getText().toString().trim().toUpperCase();

        // Validation
        if (TextUtils.isEmpty(question) || TextUtils.isEmpty(choiceA) || TextUtils.isEmpty(choiceB)
                || TextUtils.isEmpty(choiceC) || TextUtils.isEmpty(choiceD) || TextUtils.isEmpty(correct)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!correct.matches("[ABCD]")) {
            Toast.makeText(this, "Correct answer must be A, B, C, or D", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert to HTML-safe format
        String htmlQuestion = textToHtml(question);
        String htmlChoiceA = textToHtml(choiceA);
        String htmlChoiceB = textToHtml(choiceB);
        String htmlChoiceC = textToHtml(choiceC);
        String htmlChoiceD = textToHtml(choiceD);

        // Prepare data
        HashMap<String, Object> assessmentMap = new HashMap<>();
        assessmentMap.put("question", htmlQuestion);
        assessmentMap.put("choiceA", htmlChoiceA);
        assessmentMap.put("choiceB", htmlChoiceB);
        assessmentMap.put("choiceC", htmlChoiceC);
        assessmentMap.put("choiceD", htmlChoiceD);
        assessmentMap.put("answer", correct);

        // Save path: assessment/{lessonId}/Quiz/{autoId}
        String itemId = databaseReference
                .child(lessonId)
                .child("Quiz")
                .push()
                .getKey();

        databaseReference
                .child(lessonId)
                .child("Quiz")
                .child(itemId)
                .setValue(assessmentMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Quiz assessment saved successfully!", Toast.LENGTH_SHORT).show();
                        clearInputs();
                    } else {
                        Toast.makeText(this, "Failed to save assessment", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveCodeAssessment(String lessonId) {
        String title = inputTitle.getText().toString().trim();
        String description = inputDescription.getText().toString();
        String code = inputCode.getText().toString();
        String expectedOutput = inputExpectedOutput.getText().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description)
                || TextUtils.isEmpty(code) || TextUtils.isEmpty(expectedOutput)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert to HTML-safe format
        String htmlCode = textToHtml(code);
        String htmlDescription = textToHtml(description);

        String language = "java";
        String versionIndex = "4";

        HashMap<String, Object> assessmentMap = new HashMap<>();
        assessmentMap.put("title", title);
        assessmentMap.put("description", htmlDescription);
        assessmentMap.put("code", htmlCode);
        assessmentMap.put("expectedOutput", expectedOutput);
        assessmentMap.put("language", language);
        assessmentMap.put("versionIndex", versionIndex);

        // Determine the category based on selected code type
        String category = selectedCodeType.replace(" ", ""); // Remove spaces

        // Get the count to determine the next number
        DatabaseReference categoryRef = databaseReference.child(lessonId).child(category);

        categoryRef.get().addOnSuccessListener(snapshot -> {
            long count = snapshot.getChildrenCount();
            String itemNumber = String.valueOf(count + 1);

            categoryRef.child(itemNumber)
                    .setValue(assessmentMap)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Saved as " + selectedCodeType + " #" + itemNumber, Toast.LENGTH_SHORT).show();
                            clearInputs();
                        } else {
                            Toast.makeText(this, "Saving failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }).addOnFailureListener(e -> Toast.makeText(this, "Database read failed", Toast.LENGTH_SHORT).show());
    }

    // Converts text to HTML-safe format (preserve spaces, tabs, and line breaks)
    private String textToHtml(String text) {
        return text.replace(" ", "&nbsp;")
                .replace("\t", "&emsp;")
                .replace("\n", "<br>");
    }

    private void clearInputs() {
        // Clear quiz fields
        inputQuestion.setText("");
        inputChoiceA.setText("");
        inputChoiceB.setText("");
        inputChoiceC.setText("");
        inputChoiceD.setText("");
        inputCorrectAnswer.setText("");

        // Clear code fields
        inputTitle.setText("");
        inputDescription.setText("");
        inputCode.setText("");
        inputExpectedOutput.setText("");

        // Keep lesson ID and type selection
        inputLessonId.requestFocus();
    }
}