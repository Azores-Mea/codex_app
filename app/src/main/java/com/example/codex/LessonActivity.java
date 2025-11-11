package com.example.codex;

import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LessonActivity extends AppCompatActivity {

    LinearLayout lessonContainer;
    DatabaseReference ref;
    SessionManager sessionManager;
    String lessonMainTitle = ""; // ✅ store for reuse later
    boolean isLessonDone = false; // ✅ new flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lesson_view);

        // 🔙 Back button setup
        ImageView backBtn = findViewById(R.id.back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        lessonContainer = findViewById(R.id.lesson_container);
        sessionManager = new SessionManager(this);

        // ✅ Get selected lesson ID from SessionManager
        String lessonId = sessionManager.getSelectedLesson();
        if (lessonId == null || lessonId.isEmpty()) {
            lessonId = "L1"; // fallback
        }

        // ✅ Load header info (main_title)
        loadLessonHeader(lessonId);

        // ✅ Load lesson content
        ref = FirebaseDatabase.getInstance().getReference("Lessons")
                .child(lessonId)
                .child("content");

        loadLessonContent(lessonId);

    }

    // --- 🔹 Load Lesson Header (main_title) ---
    private void loadLessonHeader(String lessonId) {
        DatabaseReference lessonRef = FirebaseDatabase.getInstance().getReference("Lessons").child(lessonId);

        lessonRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return;

            DataSnapshot snap = task.getResult();

            // --- Get value ---
            String mainTitle = snap.child("main_title").getValue(String.class);

            // --- Clean up HTML tags ---
            if (mainTitle != null) {
                mainTitle = mainTitle.replaceAll("(?i)<p>", "")
                        .replaceAll("(?i)</p>", "")
                        .trim();
                lessonMainTitle = mainTitle; // ✅ store globally for later use
            }

            // --- Apply to UI ---
            TextView headerTitle = findViewById(R.id.main_title);
            if (headerTitle != null && mainTitle != null && !mainTitle.isEmpty()) {
                headerTitle.setText(mainTitle);
            }
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

            // ✅ Add lesson_end.xml at the end
            View endView = getLayoutInflater().inflate(R.layout.lesson_end, lessonContainer, false);
            String moduleNumber = lessonId.replaceAll("[^0-9]", "");
            TextView moduleText = endView.findViewById(R.id.module_text);
            if (lessonMainTitle != null && !lessonMainTitle.trim().isEmpty()) {
                moduleText.setText("End of Module " + moduleNumber + ". " + lessonMainTitle);
            } else {
                moduleText.setText("End of Module " + moduleNumber);
            }

            Button markDoneBtn = endView.findViewById(R.id.markDone);
            Button takeQuizBtn = endView.findViewById(R.id.takeQuiz);
            takeQuizBtn.setEnabled(false);
            takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            takeQuizBtn.setTextColor(Color.BLACK);

            String userId = String.valueOf(sessionManager.getUserId());
            DatabaseReference recentRef = FirebaseDatabase.getInstance().getReference("RecentLesson");

            // ✅ Improved query: fetch all user's lessons and check by lessonId
            recentRef.orderByChild("userId").equalTo(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            boolean found = false;
                            if (snapshot.exists()) {
                                for (DataSnapshot s : snapshot.getChildren()) {
                                    String lId = s.child("lessonId").getValue(String.class);
                                    if (lessonId.equals(lId)) {
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            isLessonDone = found; // ✅ store flag
                            updateLessonUI(markDoneBtn, takeQuizBtn);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) { }
                    });

            takeQuizBtn.setOnClickListener(v -> {
                sessionManager.saveSelectedLesson(lessonId);
                Intent intent = new Intent(LessonActivity.this, LessonQuizActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            markDoneBtn.setOnClickListener(v -> {
                if (isLessonDone) return; // prevent duplicates

                markDoneBtn.setText("✓ Completed");
                markDoneBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                markDoneBtn.setEnabled(false);
                takeQuizBtn.setEnabled(true);
                takeQuizBtn.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                takeQuizBtn.setTextColor(Color.WHITE);

                Map<String, Object> lessonData = new HashMap<>();
                lessonData.put("userId", userId);
                lessonData.put("lessonId", lessonId);
                lessonData.put("title", lessonMainTitle);
                lessonData.put("timestamp", ServerValue.TIMESTAMP);

                recentRef.push().setValue(lessonData).addOnSuccessListener(aVoid -> {
                    isLessonDone = true;
                    updateLessonUI(markDoneBtn, takeQuizBtn);
                });
            });

            lessonContainer.addView(endView);
        });
    }
    private void updateLessonUI(Button markDoneBtn, Button takeQuizBtn) {
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
    }


    // --- Tooltip popup handler ---
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

    // --- HTML text formatting ---
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

    // --- ✅ Code Block Loader (HTML safe) ---
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

    // --- Drawable loader ---
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