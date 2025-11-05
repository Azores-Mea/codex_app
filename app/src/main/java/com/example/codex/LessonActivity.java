package com.example.codex;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.google.firebase.database.*;
import android.text.Html;

public class LessonActivity extends AppCompatActivity {

    LinearLayout lessonContainer;
    DatabaseReference ref;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lesson_view);

        lessonContainer = findViewById(R.id.lesson_container);
        ref = FirebaseDatabase.getInstance().getReference("Lessons").child("L1");

        // 🔙 Back button setup
        ImageView backBtn = findViewById(R.id.back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                finish(); // Close current activity
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Optional fade effect
            });
        }

        loadLessonContent();
    }

    private void loadLessonContent() {
        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            for (DataSnapshot snap : task.getResult().getChildren()) {

                // TITLE BLOCK
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

                    lessonContainer.addView(view);
                }

                // EXAMPLE BLOCK
                if (snap.child("EXAMPLE").exists()) {
                    DataSnapshot e = snap.child("EXAMPLE");
                    View view = getLayoutInflater().inflate(R.layout.lesson_example_type, lessonContainer, false);

                    setHtmlText(view.findViewById(R.id.example_type), e.child("title").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.shortDesc), e.child("description").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper4), e.child("helper1").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper5), e.child("helper2").getValue(String.class));

                    loadImage(view.findViewById(R.id.image4), e.child("helper1Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image5), e.child("helper2Drawable").getValue(String.class));

                    lessonContainer.addView(view);
                }

                // OUTPUT BLOCK
                if (snap.child("OUTPUT").exists()) {
                    DataSnapshot o = snap.child("OUTPUT");
                    View view = getLayoutInflater().inflate(R.layout.lesson_output, lessonContainer, false);

                    setHtmlText(view.findViewById(R.id.output), o.child("title").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper6), o.child("helper1").getValue(String.class));
                    setHtmlText(view.findViewById(R.id.helper7), o.child("helper2").getValue(String.class));

                    loadImage(view.findViewById(R.id.image6), o.child("helper1Drawable").getValue(String.class));
                    loadImage(view.findViewById(R.id.image7), o.child("helper2Drawable").getValue(String.class));

                    lessonContainer.addView(view);
                }

                // TOOLTIP BLOCK
                if (snap.child("TOOLTIP").exists()) {
                    DataSnapshot ttip = snap.child("TOOLTIP");
                    String tooltipHtml = ttip.child("tooltip").getValue(String.class);

                    View tipView = getLayoutInflater().inflate(R.layout.lesson_tooltip, lessonContainer, false);
                    ImageView tipIcon = tipView.findViewById(R.id.tipButton);

                    tipIcon.setOnClickListener(v -> showTooltip(v, tooltipHtml));

                    lessonContainer.addView(tipView);
                }
            }
        });
    }

    // --- Tooltip popup handler ---
    // --- Tooltip popup handler ---
    private void showTooltip(View anchorView, String tooltipHtml) {
        if (tooltipHtml == null || tooltipHtml.trim().isEmpty()) return;

        // Convert HTML text
        View popupView = LayoutInflater.from(this).inflate(R.layout.tooltip_bubble, null);
        TextView text = popupView.findViewById(R.id.tooltip_text);
        text.setText(Html.fromHtml(tooltipHtml, Html.FROM_HTML_MODE_LEGACY));

        // Top padding for better readability
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

        // ✅ Change tooltip icon while popup is visible
        if (anchorView instanceof ImageView) {
            ImageView icon = (ImageView) anchorView;
            icon.setImageResource(R.drawable.tooltip_open); // your "active" icon
        }

        anchorView.post(() -> {
            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);

            int anchorHeight = anchorView.getHeight();
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int popupHeight = popupView.getMeasuredHeight();

            int yOffset = -(anchorHeight / 2) - (popupHeight / 2);
            int xOffset = anchorView.getWidth() + (int) (10 * getResources().getDisplayMetrics().density);

            popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
        });

        // Revert icon when popup dismisses
        popupWindow.setOnDismissListener(() -> {
            if (anchorView instanceof ImageView) {
                ImageView icon = (ImageView) anchorView;
                icon.setImageResource(R.drawable.tooltip_close); // your normal icon
            }
        });

        // Auto dismiss after 3s
        popupView.postDelayed(popupWindow::dismiss, 3000);
    }

    // --- HTML text formatting ---
    private void setHtmlText(TextView view, String value) {
        if (value == null) {
            view.setVisibility(View.GONE);
            return;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("<br>") ||
                trimmed.equalsIgnoreCase("<br/>") ||
                trimmed.equalsIgnoreCase("<p><br></p>") ||
                trimmed.equalsIgnoreCase("<p><br/></p>")) {
            view.setVisibility(View.GONE);
            return;
        }

        boolean hasListItems = trimmed.toLowerCase().contains("<li>");

        if (trimmed.toLowerCase().contains("<p>") && hasListItems) {
            String introPart = trimmed.replaceAll("(?is)<ul[\\s\\S]*?</ul>", "");
            java.util.regex.Matcher ulMatcher = java.util.regex.Pattern.compile("(?is)<ul[\\s\\S]*?</ul>").matcher(trimmed);

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
                java.util.regex.Matcher liMatcher = java.util.regex.Pattern.compile("(?is)<li>(.*?)</li>").matcher(ulBlock);
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

                if (ulMatcher.hitEnd()) break;
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

    private void loadImage(ImageView img, String drawableName) {
        if (drawableName == null || drawableName.isEmpty()) {
            img.setVisibility(View.GONE);
            return;
        }

        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) img.setImageResource(resId);
        else img.setVisibility(View.GONE);
    }
}
