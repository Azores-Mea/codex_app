package com.example.codex;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class AddCodingExerciseActivity extends AppCompatActivity {

    private EditText inputTitle, inputDescription, inputCode, inputExpectedOutput;
    private MaterialButton btnSaveExercise;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_coding_exercise);

        // Firebase base reference
        databaseReference = FirebaseDatabase.getInstance().getReference("coding_exercises");

        // Bind views
        inputTitle = findViewById(R.id.inputTitle);
        inputDescription = findViewById(R.id.inputDescription);
        inputCode = findViewById(R.id.inputCode);
        inputExpectedOutput = findViewById(R.id.inputExpectedOutput);
        btnSaveExercise = findViewById(R.id.btnSaveExercise);

        btnSaveExercise.setOnClickListener(v -> saveExercise());
    }

    private void saveExercise() {
        String lessonId = ((EditText) findViewById(R.id.lessonId)).getText().toString().trim();
        String title = inputTitle.getText().toString().trim();
        String description = inputDescription.getText().toString();
        String code = inputCode.getText().toString();
        String expectedOutput = inputExpectedOutput.getText().toString();

        if (TextUtils.isEmpty(lessonId) || TextUtils.isEmpty(title) || TextUtils.isEmpty(description)
                || TextUtils.isEmpty(code) || TextUtils.isEmpty(expectedOutput)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert both code and description to HTML-safe format
        String htmlCode = textToHtml(code);
        String htmlDescription = textToHtml(description);

        String language = "java";
        String versionIndex = "4";

        HashMap<String, Object> exerciseMap = new HashMap<>();
        exerciseMap.put("title", title);
        exerciseMap.put("description", htmlDescription); // now saved like code
        exerciseMap.put("code", htmlCode);
        exerciseMap.put("expectedOutput", expectedOutput);
        exerciseMap.put("language", language);
        exerciseMap.put("versionIndex", versionIndex);

        DatabaseReference lessonRef = databaseReference.child(lessonId);

        lessonRef.get().addOnSuccessListener(snapshot -> {
            long count = snapshot.getChildrenCount();
            String exerciseNumber = String.valueOf(count + 1);

            lessonRef.child(exerciseNumber)
                    .setValue(exerciseMap)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Saved as Exercise " + exerciseNumber + " under " + lessonId, Toast.LENGTH_SHORT).show();
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
        inputTitle.setText("");
        inputDescription.setText("");
        inputCode.setText("");
        inputExpectedOutput.setText("");
        inputTitle.requestFocus();
    }
}
