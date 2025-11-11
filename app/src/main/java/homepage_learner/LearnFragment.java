package homepage_learner;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.codex.LessonActivity;
import com.example.codex.R;
import com.example.codex.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LearnFragment extends Fragment {

    private LinearLayout sectionBeginnerHeader;
    private LinearLayout sectionBeginnerContent;
    private ImageView iconArrow;
    private boolean isContentVisible = false;
    private MaterialCardView lesson1;
    private TextView tvUserName;
    private TextView tvLevel;

    private DatabaseReference lessonsRef;
    private DatabaseReference quizResultsRef;

    public LearnFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_learn, container, false);

        // Initialize Views
        sectionBeginnerHeader = view.findViewById(R.id.sectionBeginnerHeader);
        sectionBeginnerContent = view.findViewById(R.id.sectionBeginnerContent);
        iconArrow = view.findViewById(R.id.iconArrow);
        lesson1 = view.findViewById(R.id.lesson1);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvLevel = view.findViewById(R.id.tvLevel);

        lessonsRef = FirebaseDatabase.getInstance().getReference("Lessons");
        quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults");

        SessionManager sessionManager = new SessionManager(requireContext());

        // ✅ Load user info if logged in
        if (sessionManager.isLoggedIn()) {
            String fullName = sessionManager.getFirstName() + " " + sessionManager.getLastName();
            String classification = sessionManager.getClassification();
            tvUserName.setText(fullName);
            updateClassificationBadge(classification);
        }

        // ✅ Toggle section visibility when header is clicked
        if (sectionBeginnerHeader != null) {
            sectionBeginnerHeader.setOnClickListener(v -> toggleSection());
        }

        // ✅ Load lessons dynamically from Firebase
        loadLessons(inflater, sessionManager);

        return view;
    }

    private void loadLessons(LayoutInflater inflater, SessionManager sessionManager) {
        String userId = String.valueOf(sessionManager.getUserId());

        // Load all quiz results first to know which lessons are passed
        quizResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot quizSnapshot) {
                lessonsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sectionBeginnerContent.removeAllViews();

                        for (DataSnapshot lessonSnap : snapshot.getChildren()) {
                            String lessonKey = lessonSnap.getKey();
                            String titleHtml = lessonSnap.child("main_title").getValue(String.class);
                            String descHtml = lessonSnap.child("title_desc").getValue(String.class);
                            String difficulty = lessonSnap.child("difficulty").getValue(String.class);

                            if (titleHtml == null) titleHtml = lessonKey;
                            if (descHtml == null) descHtml = "No description available";

                            Spanned title = Html.fromHtml(titleHtml, Html.FROM_HTML_MODE_LEGACY);
                            Spanned about = Html.fromHtml(descHtml, Html.FROM_HTML_MODE_LEGACY);

                            String truncatedTitle = truncateText(title.toString(), 60);
                            String truncatedAbout = truncateText(about.toString(), 120);

                            // ✅ Determine if lesson should be locked
                            boolean isLocked = shouldLockLesson(quizSnapshot, userId, lessonKey);

                            View lessonView;
                            if (isLocked) {
                                // 🔒 Inflate lesson_locked.xml
                                lessonView = inflater.inflate(R.layout.lesson_locked, sectionBeginnerContent, false);
                            } else {
                                // 🔓 Inflate lesson_unlocked.xml
                                lessonView = inflater.inflate(R.layout.lesson_unlocked, sectionBeginnerContent, false);

                                MaterialCardView card = lessonView.findViewById(R.id.lesson1);
                                TextView lTitle = lessonView.findViewById(R.id.l_title);
                                TextView lAbout = lessonView.findViewById(R.id.l_about);
                                ImageView diffBadge = lessonView.findViewById(R.id.difficulty);

                                lTitle.setText(Html.fromHtml(truncatedTitle, Html.FROM_HTML_MODE_LEGACY));
                                lAbout.setText(Html.fromHtml(truncatedAbout, Html.FROM_HTML_MODE_LEGACY));

                                // ✅ Difficulty badge
                                if (difficulty != null) {
                                    switch (difficulty.toLowerCase()) {
                                        case "beginner":
                                            diffBadge.setImageResource(R.drawable.beginner_classifier);
                                            break;
                                        case "intermediate":
                                            diffBadge.setImageResource(R.drawable.intermediate_classifier);
                                            break;
                                        case "advanced":
                                            diffBadge.setImageResource(R.drawable.advanced_classifier);
                                            break;
                                        default:
                                            diffBadge.setImageResource(R.drawable.none_classifier);
                                            break;
                                    }
                                }

                                // ✅ Click to open lesson
                                if (card != null) {
                                    card.setClickable(true);
                                    card.setFocusable(true);
                                    card.setOnClickListener(v -> {
                                        sessionManager.saveSelectedLesson(lessonKey);
                                        Intent intent = new Intent(requireContext(), LessonActivity.class);
                                        intent.putExtra("lessonId", lessonKey);
                                        startActivity(intent);
                                    });
                                }
                            }

                            sectionBeginnerContent.addView(lessonView);
                        }

                        if (!snapshot.hasChildren()) {
                            Toast.makeText(requireContext(), "No lessons available.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to load lessons: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load quiz results: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Determines if the lesson should be locked
    private boolean shouldLockLesson(DataSnapshot quizSnapshot, String userId, String lessonId) {
        if (lessonId.equals("L1")) return false; // Always unlocked

        String previousLessonId = getPreviousLessonId(lessonId);
        if (previousLessonId == null) return false;

        // Search quizResults for this user's passed quiz for previousLessonId
        for (DataSnapshot quiz : quizSnapshot.getChildren()) {
            String qUser = String.valueOf(quiz.child("userId").getValue());
            String qLesson = quiz.child("lessonId").getValue(String.class);
            String passed = quiz.child("passed").getValue(String.class);

            if (qUser.equals(userId)
                    && qLesson != null && qLesson.equals(previousLessonId)
                    && passed != null && passed.equalsIgnoreCase("Passed")) {
                return false; // ✅ Unlocked
            }
        }

        return true; // 🔒 Locked by default
    }

    // ✅ Get previous lesson id (L2 → L1, L3 → L2)
    private String getPreviousLessonId(String currentLessonId) {
        try {
            int num = Integer.parseInt(currentLessonId.replaceAll("[^0-9]", ""));
            if (num > 1) return "L" + (num - 1);
        } catch (NumberFormatException ignored) { }
        return null;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3).trim() + "...";
        }
        return text;
    }

    private void toggleSection() {
        if (isContentVisible) {
            sectionBeginnerContent.setVisibility(View.GONE);
            rotateArrow(180, 0);
        } else {
            sectionBeginnerContent.setVisibility(View.VISIBLE);
            rotateArrow(0, 180);
        }
        isContentVisible = !isContentVisible;
    }

    private void rotateArrow(float from, float to) {
        RotateAnimation rotate = new RotateAnimation(
                from, to,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        iconArrow.startAnimation(rotate);
    }

    private void updateClassificationBadge(String classification) {
        if (classification == null) classification = "notClassified";

        switch (classification.toLowerCase()) {
            case "beginner":
                tvLevel.setBackgroundResource(R.drawable.beginner_classifier);
                break;
            case "intermediate":
                tvLevel.setBackgroundResource(R.drawable.intermediate_classifier);
                break;
            case "advanced":
                tvLevel.setBackgroundResource(R.drawable.advanced_classifier);
                break;
            default:
                tvLevel.setBackgroundResource(R.drawable.none_classifier);
                break;
        }
    }
}
