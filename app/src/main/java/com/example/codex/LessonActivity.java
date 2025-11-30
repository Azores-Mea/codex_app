package com.example.codex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.webkit.WebView;
import com.google.firebase.database.*;
import android.text.Html;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class LessonActivity extends AppCompatActivity {

    LinearLayout lessonContainer;
    DatabaseReference ref;
    SessionManager sessionManager;
    String lessonMainTitle = "";
    boolean isLessonDone = false;
    boolean hasPassedQuiz = false;
    boolean hasCodingExercises = false;
    boolean hasCompletedExercises = false;

    Button markDoneBtn;
    Button takeQuizBtn;
    Button takeCodingBtn;

    // Activity result launchers
    private ActivityResultLauncher<Intent> quizLauncher;
    private ActivityResultLauncher<Intent> exerciseLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lesson_view);

        // Initialize activity result launchers
        quizLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Reload lesson UI after quiz completion
                        reloadLessonStatus();
                    }
                }
        );

        exerciseLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Reload lesson UI after exercise completion
                        reloadLessonStatus();
                    }
                }
        );

        ImageView backBtn = findViewById(R.id.back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        lessonContainer = findViewById(R.id.lesson_container);
        sessionManager = new SessionManager(this);

        String lessonId = sessionManager.getSelectedLesson();
        if (lessonId == null || lessonId.isEmpty()) {
            lessonId = "L1";
        }

        loadLessonHeader(lessonId);
        ref = FirebaseDatabase.getInstance().getReference("Lessons")
                .child(lessonId)
                .child("content");

        loadLessonContent(lessonId);
    }

    private void loadLessonHeader(String lessonId) {
        DatabaseReference lessonRef = FirebaseDatabase.getInstance().getReference("Lessons").child(lessonId);

        lessonRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return;

            DataSnapshot snap = task.getResult();
            String mainTitle = snap.child("main_title").getValue(String.class);

            if (mainTitle != null) {
                mainTitle = mainTitle.replaceAll("(?i)<p>", "")
                        .replaceAll("(?i)</p>", "")
                        .trim();
                lessonMainTitle = mainTitle;
            }

            TextView headerTitle = findViewById(R.id.main_title);
            if (headerTitle != null && mainTitle != null && !mainTitle.isEmpty()) {
                headerTitle.setText(mainTitle);
            }
        });
    }

    private void reloadLessonStatus() {
        String userId = String.valueOf(sessionManager.getUserId());
        String lessonId = sessionManager.getSelectedLesson();

        DatabaseReference quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");
        DatabaseReference exerciseResultsRef = FirebaseDatabase.getInstance().getReference("exerciseResults");
        DatabaseReference recentRef = FirebaseDatabase.getInstance().getReference("RecentLesson");

        // Check quiz pass status
        quizResultsRef.child(userId).child(lessonId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String passed = snapshot.child("passed").getValue(String.class);
                        hasPassedQuiz = "Passed".equalsIgnoreCase(passed);
                    }
                    updateLessonUI(markDoneBtn, takeQuizBtn, takeCodingBtn);
                });

        // Check exercise completion status
        exerciseResultsRef.child(userId).child(lessonId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Boolean completed = snapshot.child("completed").getValue(Boolean.class);
                        hasCompletedExercises = completed != null && completed;
                    }
                    updateLessonUI(markDoneBtn, takeQuizBtn, takeCodingBtn);
                });

        // Check lesson done status
        recentRef.child(userId).child(lessonId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    isLessonDone = snapshot.exists();
                    updateLessonUI(markDoneBtn, takeQuizBtn, takeCodingBtn);
                });
    }

    private void loadLessonContent(String lessonId) {
        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            for (DataSnapshot snap : task.getResult().getChildren()) {

                // --- TITLE BLOCK ---
                if (snap.child("TITLE").exists()) {
                    DataSnapshot t = snap.child("TITLE");
                    View view = getLayoutInflater().inflate(R.layout.lesson_title, lessonContainer, false);

                    setHtmlText(view.findViewById(R.id.title), t.child("title").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.desc), t.child("description").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper1), t.child("helper1").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper2), t.child("helper2").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper3), t.child("helper3").getValue(String.class));

                    loadImage(view.findViewById(R.id.image1), t.child("helper1Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image2), t.child("helper2Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image3), t.child("helper3Drawable").getValue(String.class));

                    setCodeHtml(view.findViewById(R.id.code1), t.child("helper1Code").getValue(String.class));
                    setCodeHtml(view.findViewById(R.id.code2), t.child("helper2Code").getValue(String.class));
                    setCodeHtml(view.findViewById(R.id.code3), t.child("helper3Code").getValue(String.class));

                    lessonContainer.addView(view);
                }

                // --- EXAMPLE BLOCK ---
                if (snap.child("EXAMPLE").exists()) {
                    DataSnapshot e = snap.child("EXAMPLE");
                    if (e.hasChild("TYPES")) e = e.child("TYPES");

                    View view = getLayoutInflater().inflate(R.layout.lesson_example_type, lessonContainer, false);

                    setHtmlText(view.findViewById(R.id.example_type), e.child("exampleTitle").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.shortDesc), e.child("exampleDescription").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper4), e.child("helper4").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper5), e.child("helper5").getValue(String.class));

                    loadImage(view.findViewById(R.id.image4), e.child("helper4Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image5), e.child("helper5Drawable").getValue(String.class));

                    setCodeHtml(view.findViewById(R.id.code4), e.child("helper4Code").getValue(String.class));
                    setCodeHtml(view.findViewById(R.id.code5), e.child("helper5Code").getValue(String.class));

                    lessonContainer.addView(view);
                }

                // --- OUTPUT BLOCK ---
                if (snap.child("OUTPUT").exists() || snap.child("SUBTITLE").child("OUTPUT").exists()) {
                    DataSnapshot o = snap.child("OUTPUT").exists() ?
                            snap.child("OUTPUT") :
                            snap.child("SUBTITLE").child("OUTPUT");

                    View view = getLayoutInflater().inflate(R.layout.lesson_output, lessonContainer, false);

                    setHtmlText(view.findViewById(R.id.output), o.child("subtitle").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper6), o.child("helper6").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper7), o.child("helper7").getValue(String.class));

                    loadImage(view.findViewById(R.id.image6), o.child("helper6Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image7), o.child("helper7Drawable").getValue(String.class));

                    setCodeHtml(view.findViewById(R.id.code6), o.child("helper6Code").getValue(String.class));
                    setCodeHtml(view.findViewById(R.id.code7), o.child("helper7Code").getValue(String.class));

                    lessonContainer.addView(view);
                }

                // --- TOOLTIP BLOCK ---
                if (snap.child("TOOLTIP").exists()) {
                    DataSnapshot ttip = snap.child("TOOLTIP");
                    String tooltipHtml = ttip.child("tooltip").getValue(String.class);

                    View tipView = getLayoutInflater().inflate(R.layout.lesson_tooltip, lessonContainer, false);
                    ImageView tipIcon = tipView.findViewById(R.id.tipButton);

                    tipIcon.setOnClickListener(v -> showTooltip(v, tooltipHtml));
                    lessonContainer.addView(tipView);
                }
            }

            View endView = getLayoutInflater().inflate(R.layout.lesson_end, lessonContainer, false);
            String moduleNumber = lessonId.replaceAll("[^0-9]", "");
            TextView moduleText = endView.findViewById(R.id.module_text);
            if (lessonMainTitle != null && !lessonMainTitle.trim().isEmpty()) {
                moduleText.setText("End of Module " + moduleNumber + ". " + lessonMainTitle);
            } else {
                moduleText.setText("End of Module " + moduleNumber);
            }

            markDoneBtn = endView.findViewById(R.id.markDone);
            takeQuizBtn = endView.findViewById(R.id.takeQuiz);
            takeCodingBtn = endView.findViewById(R.id.takeCodingExercises);

            takeQuizBtn.setEnabled(false);
            takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            takeQuizBtn.setTextColor(Color.BLACK);

            takeCodingBtn.setEnabled(false);
            takeCodingBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            takeCodingBtn.setTextColor(Color.BLACK);

            String userId = String.valueOf(sessionManager.getUserId());
            DatabaseReference recentRef = FirebaseDatabase.getInstance().getReference("RecentLesson");
            DatabaseReference quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");
            DatabaseReference codingExercisesRef = FirebaseDatabase.getInstance().getReference("coding_exercises");
            DatabaseReference exerciseResultsRef = FirebaseDatabase.getInstance().getReference("exerciseResults");

            // Check if coding exercises exist for this lesson
            codingExercisesRef.child(lessonId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    hasCodingExercises = snapshot.exists() && snapshot.hasChildren();

                    if (!hasCodingExercises) {
                        takeCodingBtn.setVisibility(View.GONE);
                    } else {
                        takeCodingBtn.setVisibility(View.VISIBLE);
                    }

                    updateLessonUI(markDoneBtn, takeQuizBtn, takeCodingBtn);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    takeCodingBtn.setVisibility(View.GONE);
                }
            });

            // Check quiz pass status
            quizResultsRef.child(userId).child(lessonId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String passed = snapshot.child("passed").getValue(String.class);
                                hasPassedQuiz = "Passed".equalsIgnoreCase(passed);
                            }
                            updateLessonUI(markDoneBtn, takeQuizBtn, takeCodingBtn);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) { }
                    });

            // Check exercise completion status
            exerciseResultsRef.child(userId).child(lessonId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Boolean completed = snapshot.child("completed").getValue(Boolean.class);
                                hasCompletedExercises = completed != null && completed;
                            }
                            updateLessonUI(markDoneBtn, takeQuizBtn, takeCodingBtn);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) { }
                    });

            // Check lesson done status
            recentRef.child(userId).child(lessonId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            isLessonDone = snapshot.exists();
                            updateLessonUI(markDoneBtn, takeQuizBtn, takeCodingBtn);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) { }
                    });

            takeQuizBtn.setOnClickListener(v -> {
                // Disable click if already passed
                if (hasPassedQuiz) {
                    return;
                }

                sessionManager.saveSelectedLesson(lessonId);
                Intent intent = new Intent(LessonActivity.this, LessonQuizActivity.class);
                quizLauncher.launch(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            markDoneBtn.setOnClickListener(v -> {
                if (isLessonDone) return;

                markDoneBtn.setText("✓ Completed");
                markDoneBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                markDoneBtn.setEnabled(false);
                takeQuizBtn.setEnabled(true);
                takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                takeQuizBtn.setTextColor(Color.WHITE);

                Map<String, Object> lessonData = new HashMap<>();
                lessonData.put("title", lessonMainTitle);
                lessonData.put("timestamp", ServerValue.TIMESTAMP);

                recentRef.child(userId).child(lessonId)
                        .setValue(lessonData)
                        .addOnSuccessListener(aVoid -> {
                            isLessonDone = true;
                            updateLessonUI(markDoneBtn, takeQuizBtn, takeCodingBtn);
                        });
            });

            takeCodingBtn.setOnClickListener(v -> {
                // Disable click if not passed quiz or already completed
                if (!hasPassedQuiz || hasCompletedExercises) {
                    return;
                }

                sessionManager.saveSelectedLesson(lessonId);
                Intent intent = new Intent(LessonActivity.this, ExerciseActivity.class);
                intent.putExtra("lessonId", lessonId);
                exerciseLauncher.launch(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            lessonContainer.addView(endView);
        });
    }

    private void updateLessonUI(Button markDoneBtn, Button takeQuizBtn, Button takeCodingBtn) {
        if (markDoneBtn == null || takeQuizBtn == null || takeCodingBtn == null) return;

        // Handle Mark as Done button
        if (isLessonDone) {
            markDoneBtn.setText("✓ Completed");
            markDoneBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            markDoneBtn.setEnabled(false);
            takeQuizBtn.setEnabled(true);
            takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            takeQuizBtn.setTextColor(Color.WHITE);
        } else {
            markDoneBtn.setText("Mark as done");
            markDoneBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            markDoneBtn.setEnabled(true);
            takeQuizBtn.setEnabled(false);
            takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            takeQuizBtn.setTextColor(Color.BLACK);
        }

        // Handle Take Quiz button
        if (hasPassedQuiz) {
            // Quiz passed - disable retake
            takeQuizBtn.setText("Passed");
            takeQuizBtn.setEnabled(false);
            takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            takeQuizBtn.setTextColor(Color.WHITE);
        } else if (isLessonDone) {
            DatabaseReference quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");
            String userId = String.valueOf(sessionManager.getUserId());
            String lessonId = sessionManager.getSelectedLesson();

            quizResultsRef.child(userId).child(lessonId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                takeQuizBtn.setText("Retake");
                            } else {
                                takeQuizBtn.setText("Take Quiz");
                            }
                            takeQuizBtn.setEnabled(true);
                            takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                            takeQuizBtn.setTextColor(Color.WHITE);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            takeQuizBtn.setText("Take Quiz");
                            takeQuizBtn.setEnabled(true);
                            takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                            takeQuizBtn.setTextColor(Color.WHITE);
                        }
                    });
        }

        // Handle Coding Exercise button
        if (hasCodingExercises) {
            if (hasCompletedExercises) {
                // Show as completed - disable clicking
                takeCodingBtn.setText("✓ Completed");
                takeCodingBtn.setEnabled(false);
                takeCodingBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                takeCodingBtn.setTextColor(Color.WHITE);
            } else if (hasPassedQuiz) {
                // Enable to take exercises
                takeCodingBtn.setText("Take Coding Exercises");
                takeCodingBtn.setEnabled(true);
                takeCodingBtn.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                takeCodingBtn.setTextColor(Color.WHITE);
            } else {
                // Quiz not passed yet - keep disabled
                takeCodingBtn.setText("Take Coding Exercises");
                takeCodingBtn.setEnabled(false);
                takeCodingBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                takeCodingBtn.setTextColor(Color.BLACK);
            }
        } else {
            takeCodingBtn.setVisibility(View.GONE);
        }
    }

    private void showTooltip(View anchorView, String tooltipHtml) {
        if (tooltipHtml == null || tooltipHtml.trim().isEmpty()) return;

        View popupView = LayoutInflater.from(this).inflate(R.layout.tooltip_bubble, null);
        TextView text = popupView.findViewById(R.id.tooltip_text);
        text.setText(Html.fromHtml(tooltipHtml, Html.FROM_HTML_MODE_LEGACY));

        int topPadding = (int) (10 * getResources().getDisplayMetrics().density);
        text.setPadding(text.getPaddingLeft(), topPadding, text.getPaddingRight(), text.getPaddingBottom());

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int measuredWidth = popupView.getMeasuredWidth();
        int measuredHeight = popupView.getMeasuredHeight();

        PopupWindow popupWindow = new PopupWindow(popupView, measuredWidth, measuredHeight, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(10);

        if (anchorView instanceof ImageView) {
            ((ImageView) anchorView).setImageResource(R.drawable.tooltip_open);
        }

        anchorView.post(() -> {
            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);
            int anchorHeight = anchorView.getHeight();
            int popupHeight = popupView.getMeasuredHeight();

            int yOffset = -(anchorHeight / 2) - (popupHeight / 2);
            int xOffset = anchorView.getWidth() + (int) (10 * getResources().getDisplayMetrics().density);
            popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
        });

        popupWindow.setOnDismissListener(() -> {
            if (anchorView instanceof ImageView) {
                ((ImageView) anchorView).setImageResource(R.drawable.tooltip_close);
            }
        });

        popupView.postDelayed(popupWindow::dismiss, 3000);
    }

    private void setHtmlText(TextView view, String value) {
        if (value == null) {
            view.setVisibility(View.GONE);
            return;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("<br>")
                || trimmed.equalsIgnoreCase("<br/>")
                || trimmed.equalsIgnoreCase("<p><br></p>")
                || trimmed.equalsIgnoreCase("<p><br/></p>")) {
            view.setVisibility(View.GONE);
            return;
        }

        boolean hasListItems = trimmed.toLowerCase().contains("<li>");
        if (trimmed.toLowerCase().contains("<p>") && hasListItems) {
            String introPart = trimmed.replaceAll("(?is)<ul[\\s\\S]*?</ul>", "");
            java.util.regex.Matcher ulMatcher =
                    java.util.regex.Pattern.compile("(?is)<ul[\\s\\S]*?</ul>").matcher(trimmed);

            String cleanIntro = introPart
                    .replaceAll("(?i)<\\/?p[^>]*>", "")
                    .replaceAll("(<br>\\s*){2,}", "<br>")
                    .trim();

            SpannableStringBuilder sb = new SpannableStringBuilder();
            if (!cleanIntro.isEmpty()) {
                CharSequence introSpanned = Html.fromHtml(cleanIntro, Html.FROM_HTML_MODE_LEGACY);
                sb.append(introSpanned);
                sb.append("\n");
            }

            final int indentPx = (int) (view.getResources().getDisplayMetrics().density * 16);
            while (ulMatcher.find()) {
                String ulBlock = ulMatcher.group();
                java.util.regex.Matcher liMatcher =
                        java.util.regex.Pattern.compile("(?is)<li>(.*?)</li>").matcher(ulBlock);
                while (liMatcher.find()) {
                    String liContent = liMatcher.group(1).trim();
                    if (liContent.isEmpty()) continue;

                    CharSequence liSpanned = Html.fromHtml(liContent, Html.FROM_HTML_MODE_LEGACY);
                    int start = sb.length();
                    sb.append("• ").append(liSpanned);
                    int end = sb.length();
                    sb.append("\n");

                    android.text.style.LeadingMarginSpan.Standard marginSpan =
                            new android.text.style.LeadingMarginSpan.Standard(indentPx);
                    sb.setSpan(marginSpan, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                sb.append("\n");
            }

            while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.delete(sb.length() - 1, sb.length());
            }

            view.setText(sb);
            view.setVisibility(View.VISIBLE);
            view.setLineSpacing(0, 1.15f);
            view.setIncludeFontPadding(false);
            view.setPadding(0, 0, 0, 0);
            return;
        }

        String formatted = trimmed
                .replaceAll("(?i)<p>", "")
                .replaceAll("(?i)</p>", "<br>")
                .replace("<ul>", "")
                .replace("</ul>", "")
                .replace("<ol>", "")
                .replace("</ol>", "")
                .replace("<li>", "• ")
                .replace("</li>", "<br>")
                .replaceAll("(<br>\\s*){2,}", "<br>")
                .replaceAll("(<br>)+$", "");

        CharSequence spanned = Html.fromHtml(formatted, Html.FROM_HTML_MODE_LEGACY);
        view.setText(spanned);
        view.setVisibility(View.VISIBLE);

        if (hasListItems) {
            int indentPx = (int) (view.getResources().getDisplayMetrics().density * 16);
            view.setPadding(indentPx, 0, 0, 0);
        } else {
            view.setPadding(0, 0, 0, 0);
        }

        view.setLineSpacing(0, 1.15f);
        view.setIncludeFontPadding(false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setCodeHtml(WebView webView, String content) {
        if (content == null || content.trim().isEmpty()) {
            webView.setVisibility(View.GONE);
            return;
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);

        String clean = content
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)<p>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&quot;", "\"")
                .replaceAll("\t", "    ")
                .trim();

        boolean looksLikeCode = clean.contains(";")
                || clean.contains("{")
                || clean.contains("}")
                || clean.contains("//")
                || clean.contains("/*")
                || clean.contains("=")
                || clean.matches("(?s).*\\b(class|public|if|else|for|while|int|String)\\b.*");

        String escaped = clean
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        if (!escaped.endsWith("\n")) escaped += "\n";

        String html;

        if (looksLikeCode) {
            html =
                    "<html>" +
                            "<head>" +
                            "  <link href='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css' rel='stylesheet' />" +
                            "  <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js'></script>" +
                            "  <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js'></script>" +
                            "  <style>" +
                            "    body { background: transparent; margin: 0; padding: 8px; }" +
                            "    pre { background: #f5f5f5; border-radius: 8px; padding: 12px 16px; font-size: 12px; " +
                            "          font-family: 'JetBrains Mono', 'Fira Code', monospace; line-height: 1.5; white-space: pre-wrap; overflow-x: auto; color: #2d2d2d; }" +
                            "    code { display: block; white-space: pre-wrap; }" +
                            "  </style>" +
                            "</head>" +
                            "<body><pre><code class='language-java'>" + escaped + "</code></pre></body></html>";
        } else {
            html =
                    "<html><body style='background:transparent; margin:0; padding:12px;'>" +
                            "<div style='background:#f5f5f5; border-radius:8px; padding:10px 14px; " +
                            "font-family:monospace; color:#333; font-size:12px; line-height:1.4; " +
                            "white-space:pre-wrap;'>" +
                            escaped +
                            "</div></body></html>";
        }

        if (escaped.trim().isEmpty()) {
            webView.setVisibility(View.GONE);
            return;
        }

        webView.setVisibility(View.VISIBLE);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }
    private void loadImage(ImageView view, String drawableName) {
        if (drawableName == null || drawableName.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }

        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) {
            view.setImageResource(resId);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}