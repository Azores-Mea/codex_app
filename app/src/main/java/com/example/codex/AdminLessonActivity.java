package com.example.codex;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AdminLessonActivity extends AppCompatActivity {

    private EditText lessonIdInput, subSectionIdInput, lessonDif, mainTitle, titleDesc;
    private Spinner sectionSpinner;

    // ===== TITLE SECTION =====
    private EditText titleInput, descriptionInput;
    private EditText helper1, helper2, helper3;
    private EditText helper1Drawable, helper2Drawable, helper3Drawable;
    private EditText helper1Code, helper2Code, helper3Code;

    // ===== EXAMPLE/TYPES SECTION =====
    private EditText exampleTitleInput, exampleDescInput;
    private EditText helper4, helper5;
    private EditText helper4Drawable, helper5Drawable;
    private EditText helper4Code, helper5Code;

    // ===== SUBTITLE/OUTPUT SECTION =====
    private EditText subtitleInput;
    private EditText helper6, helper7;
    private EditText helper6Drawable, helper7Drawable;
    private EditText helper6Code, helper7Code;

    // ===== TOOLTIP SECTION =====
    private EditText tooltipInput;

    private LinearLayout titleSectionLayout, exampleSectionLayout, subtitleSectionLayout, tooltipSectionLayout;
    private Button uploadBtn;
    private DatabaseReference databaseRef;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lesson_entry);

        databaseRef = FirebaseDatabase.getInstance().getReference("Lessons");

        // ===== GENERAL LESSON INFO =====
        lessonIdInput = findViewById(R.id.lessonIdInput);
        subSectionIdInput = findViewById(R.id.subSectionIdInput);
        lessonDif = findViewById(R.id.lessonDif);
        mainTitle = findViewById(R.id.main_title);
        titleDesc = findViewById(R.id.titleDesc);
        sectionSpinner = findViewById(R.id.sectionSpinner);

        // ===== SECTION LAYOUTS =====
        titleSectionLayout = findViewById(R.id.titleSectionLayout);
        exampleSectionLayout = findViewById(R.id.exampleSectionLayout);
        subtitleSectionLayout = findViewById(R.id.subtitleSectionLayout);
        tooltipSectionLayout = findViewById(R.id.tooltipSectionLayout);

        // ===== TITLE SECTION =====
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        helper1 = findViewById(R.id.helper1);
        helper2 = findViewById(R.id.helper2);
        helper3 = findViewById(R.id.helper3);
        helper1Drawable = findViewById(R.id.helper1Drawable);
        helper2Drawable = findViewById(R.id.helper2Drawable);
        helper3Drawable = findViewById(R.id.helper3Drawable);
        helper1Code = findViewById(R.id.helper1Code);
        helper2Code = findViewById(R.id.helper2Code);
        helper3Code = findViewById(R.id.helper3Code);

        // ===== EXAMPLE/TYPES SECTION =====
        exampleTitleInput = findViewById(R.id.exampleTitleInput);
        exampleDescInput = findViewById(R.id.exampleDescInput);
        helper4 = findViewById(R.id.helper4);
        helper5 = findViewById(R.id.helper5);
        helper4Drawable = findViewById(R.id.helper4Drawable);
        helper5Drawable = findViewById(R.id.helper5Drawable);
        helper4Code = findViewById(R.id.helper4Code);
        helper5Code = findViewById(R.id.helper5Code);

        // ===== SUBTITLE/OUTPUT SECTION =====
        subtitleInput = findViewById(R.id.subtitleInput);
        helper6 = findViewById(R.id.helper6);
        helper7 = findViewById(R.id.helper7);
        helper6Drawable = findViewById(R.id.helper6Drawable);
        helper7Drawable = findViewById(R.id.helper7Drawable);
        helper6Code = findViewById(R.id.helper6Code);
        helper7Code = findViewById(R.id.helper7Code);

        // ===== TOOLTIP SECTION =====
        tooltipInput = findViewById(R.id.tooltipInput);

        uploadBtn = findViewById(R.id.uploadBtn);
        uploadBtn.setOnClickListener(v -> uploadData());

        // ===== SPINNER SETUP =====
        String[] sections = {"TITLE", "EXAMPLE/TYPES", "SUBTITLE/OUTPUT", "TOOLTIP"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections);
        sectionSpinner.setAdapter(adapter);

        sectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, android.view.View view, int i, long l) {
                titleSectionLayout.setVisibility(android.view.View.GONE);
                exampleSectionLayout.setVisibility(android.view.View.GONE);
                subtitleSectionLayout.setVisibility(android.view.View.GONE);
                tooltipSectionLayout.setVisibility(android.view.View.GONE);

                switch (sections[i]) {
                    case "TITLE":
                        titleSectionLayout.setVisibility(android.view.View.VISIBLE);
                        break;
                    case "EXAMPLE/TYPES":
                        exampleSectionLayout.setVisibility(android.view.View.VISIBLE);
                        break;
                    case "SUBTITLE/OUTPUT":
                        subtitleSectionLayout.setVisibility(android.view.View.VISIBLE);
                        break;
                    case "TOOLTIP":
                        tooltipSectionLayout.setVisibility(android.view.View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    // Convert markdown-like formatting to HTML
    private String toHtmlFormatted(String input) {
        if (input == null) return "";
        String normalized = input.replace("\r\n", "\n").replace("\r", "\n");
        normalized = normalized.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        normalized = normalized.replaceAll("(?<!\\*)\\*(?!\\*)(.*?)\\*(?<!\\*)", "<i>$1</i>");

        String[] lines = normalized.split("\n", -1);
        StringBuilder out = new StringBuilder();
        int listType = 0;

        for (String raw : lines) {
            String line = raw.trim();
            boolean isEmpty = line.isEmpty();

            if (!isEmpty && (line.matches("^([-\\*•])\\s+.*"))) {
                if (listType != 1) {
                    if (listType == 2) out.append("</ol>");
                    out.append("<ul>");
                    listType = 1;
                }
                String item = line.replaceFirst("^([-\\*•])\\s+", "");
                out.append("<li>").append(item).append("</li>");
                continue;
            }

            if (!isEmpty && (line.matches("^\\d+([\\.|\\)])\\s+.*"))) {
                if (listType != 2) {
                    if (listType == 1) out.append("</ul>");
                    out.append("<ol>");
                    listType = 2;
                }
                String item = line.replaceFirst("^\\d+([\\.|\\)])\\s+", "");
                out.append("<li>").append(item).append("</li>");
                continue;
            }

            if (listType == 1) { out.append("</ul>"); listType = 0; }
            else if (listType == 2) { out.append("</ol>"); listType = 0; }

            if (isEmpty) out.append("<br/>");
            else out.append("<p>").append(line).append("</p>");
        }

        if (listType == 1) out.append("</ul>");
        else if (listType == 2) out.append("</ol>");

        return out.toString();
    }

    private void uploadData() {
        String lessonId = lessonIdInput.getText().toString().trim();
        String subSectionId = subSectionIdInput.getText().toString().trim();
        String sectionType = sectionSpinner.getSelectedItem().toString();

        if (lessonId.isEmpty() || subSectionId.isEmpty()) {
            Toast.makeText(this, "Please enter Lesson ID and Sub Section ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== SAVE MAIN LESSON META =====
        DatabaseReference lessonRef = databaseRef.child(lessonId);
        Map<String, Object> lessonMeta = new HashMap<>();
        lessonMeta.put("difficulty", lessonDif.getText().toString().trim());
        lessonMeta.put("main_title", toHtmlFormatted(mainTitle.getText().toString().trim()));
        lessonMeta.put("title_desc", toHtmlFormatted(titleDesc.getText().toString().trim()));
        lessonRef.updateChildren(lessonMeta);

        // ===== SAVE CONTENT SECTION =====
        DatabaseReference sectionRef = databaseRef.child(lessonId)
                .child("content")
                .child(subSectionId)
                .child(sectionType);

        Map<String, Object> dataMap = new HashMap<>();

        switch (sectionType) {
            case "TITLE":
                dataMap.put("title", toHtmlFormatted(titleInput.getText().toString()));
                dataMap.put("description", toHtmlFormatted(descriptionInput.getText().toString()));
                dataMap.put("helper1", toHtmlFormatted(helper1.getText().toString()));
                dataMap.put("helper1Drawable", helper1Drawable.getText().toString());
                dataMap.put("helper1Code", toHtmlFormatted(helper1Code.getText().toString()));
                dataMap.put("helper2", toHtmlFormatted(helper2.getText().toString()));
                dataMap.put("helper2Drawable", helper2Drawable.getText().toString());
                dataMap.put("helper2Code", toHtmlFormatted(helper2Code.getText().toString()));
                dataMap.put("helper3", toHtmlFormatted(helper3.getText().toString()));
                dataMap.put("helper3Drawable", helper3Drawable.getText().toString());
                dataMap.put("helper3Code", toHtmlFormatted(helper3Code.getText().toString()));
                break;

            case "EXAMPLE/TYPES":
                dataMap.put("exampleTitle", toHtmlFormatted(exampleTitleInput.getText().toString()));
                dataMap.put("exampleDescription", toHtmlFormatted(exampleDescInput.getText().toString()));
                dataMap.put("helper4", toHtmlFormatted(helper4.getText().toString()));
                dataMap.put("helper4Drawable", helper4Drawable.getText().toString());
                dataMap.put("helper4Code", toHtmlFormatted(helper4Code.getText().toString()));
                dataMap.put("helper5", toHtmlFormatted(helper5.getText().toString()));
                dataMap.put("helper5Drawable", helper5Drawable.getText().toString());
                dataMap.put("helper5Code", toHtmlFormatted(helper5Code.getText().toString()));
                break;

            case "SUBTITLE/OUTPUT":
                dataMap.put("subtitle", toHtmlFormatted(subtitleInput.getText().toString()));
                dataMap.put("helper6", toHtmlFormatted(helper6.getText().toString()));
                dataMap.put("helper6Drawable", helper6Drawable.getText().toString());
                dataMap.put("helper6Code", toHtmlFormatted(helper6Code.getText().toString()));
                dataMap.put("helper7", toHtmlFormatted(helper7.getText().toString()));
                dataMap.put("helper7Drawable", helper7Drawable.getText().toString());
                dataMap.put("helper7Code", toHtmlFormatted(helper7Code.getText().toString()));
                break;

            case "TOOLTIP":
                dataMap.put("tooltip", toHtmlFormatted(tooltipInput.getText().toString()));
                break;
        }

        saveToDatabase(sectionRef, dataMap);
    }

    private void saveToDatabase(DatabaseReference sectionRef, Map<String, Object> dataMap) {
        sectionRef.setValue(dataMap)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "✅ Upload successful!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "❌ Database error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
