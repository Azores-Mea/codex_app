package com.example.codex;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SelectionMode extends AppCompatActivity {

    private DatabaseReference userRef;
    private int userId; // use your session’s integer userId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selection_mode);

        // Initialize session and database
        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        ImageView back = findViewById(R.id.back);
        LinearLayout guided = findViewById(R.id.guided);
        LinearLayout module = findViewById(R.id.module);

        back.setOnClickListener(v -> onBackPressed());

        guided.setOnClickListener(v -> showGuidedPopup());
        module.setOnClickListener(v -> showModulePopup());
    }

    private void showGuidedPopup() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogue_mode, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setLayout(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setDimAmount(0.6f);
            }
        }

        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView message = dialogView.findViewById(R.id.dialog_message);
        TextView note = dialogView.findViewById(R.id.dialog_note);
        MaterialButton btnCancel = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnContinue = dialogView.findViewById(R.id.yes_btn);

        title.setText("Are you sure you want to continue?");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        message.setText(Html.fromHtml(
                "You’ve selected <b>Guided Mode</b>, where lessons unlock one by one in sequence.",
                Html.FROM_HTML_MODE_LEGACY));
        note.setText(Html.fromHtml(
                "<i><b>Note:</b> You can’t change modes once selected.</i>",
                Html.FROM_HTML_MODE_LEGACY));

        int redColor = Color.parseColor("#E31414");
        btnCancel.setTextColor(redColor);
        btnCancel.setStrokeColor(android.content.res.ColorStateList.valueOf(redColor));
        btnCancel.setText("No");
        btnContinue.setText("Yes");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            updateLearningMode("Guided Mode");
        });
    }

    private void showModulePopup() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogue_mode, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setLayout(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setDimAmount(0.6f);
            }
        }

        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView message = dialogView.findViewById(R.id.dialog_message);
        TextView note = dialogView.findViewById(R.id.dialog_note);
        MaterialButton btnCancel = dialogView.findViewById(R.id.no_btn);
        MaterialButton btnContinue = dialogView.findViewById(R.id.yes_btn);

        title.setText("Are you sure you want to continue?");
        GradientTextUtil.applyGradient(title, "#03162A", "#0A4B90");

        message.setText(Html.fromHtml(
                "You’ve selected <b>Module Mode</b>, giving you full access to all lessons and activities.",
                Html.FROM_HTML_MODE_LEGACY));
        note.setText(Html.fromHtml(
                "<i><b>Note:</b> You can’t change modes once selected.</i>",
                Html.FROM_HTML_MODE_LEGACY));

        int redColor = Color.parseColor("#E31414");
        btnCancel.setTextColor(redColor);
        btnCancel.setStrokeColor(android.content.res.ColorStateList.valueOf(redColor));
        btnCancel.setText("No");
        btnContinue.setText("Yes");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            updateLearningMode("Module Mode");
        });
    }

    private void updateLearningMode(String mode) {
        if (userId == -1) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert userId (int) to String for Firebase key
        String userKey = String.valueOf(userId);

        userRef.child(userKey).child("learningMode").setValue(mode)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Learning mode set to " + mode, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SelectionMode.this, Navigation_ActivityLearner.class);
                    intent.putExtra("openLearnFragment", true);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update mode: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
