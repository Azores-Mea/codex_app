package homepage_learner;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.codex.Navigation_ActivityLearner;
import com.example.codex.R;
import com.example.codex.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private LinearLayout currentLearner, scoreHistoryContainer;
    private CardView lessonHistory, newLearner, continueLessonCard;
    private DatabaseReference dbRefAct, dbRefLesson;
    private SessionManager sessionManager;

    private TextView lessonTitleView, lessonDescView, lessonProgressView;
    private ProgressBar lessonProgressBar;

    private TextView contLessonTitle, contChapter;
    private ProgressBar contProgressBar;

    // Header views
    private TextView userName, userClass;
    private ImageView avatar;

    // Executor for background operations
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        Button startLearningButton = view.findViewById(R.id.start_learning);

        startLearningButton.setOnClickListener(v -> {
            if (getActivity() instanceof Navigation_ActivityLearner) {
                ((Navigation_ActivityLearner) getActivity()).openLearnFragment();
            }
        });

        TextView greetName = view.findViewById(R.id.learner_name);

        sessionManager = new SessionManager(requireContext());
        dbRefAct = FirebaseDatabase.getInstance().getReference("RecentAct");
        dbRefLesson = FirebaseDatabase.getInstance().getReference("RecentLesson");

        // ---------- HEADER SECTION ----------
        userName = view.findViewById(R.id.user_name);
        userClass = view.findViewById(R.id.user_class);
        avatar = view.findViewById(R.id.imageViewAvatar);

        // Load user data from SessionManager
        if (sessionManager.isLoggedIn()) {
            String firstName = sessionManager.getFirstName();
            String lastName = sessionManager.getLastName();
            String classification = sessionManager.getClassification();

            String fullName;
            if (firstName != null && lastName != null) {
                fullName = firstName + " " + lastName;
            } else if (firstName != null) {
                fullName = firstName;
            } else if (lastName != null) {
                fullName = lastName;
            } else {
                fullName = "User";
            }

            userName.setText(fullName);
            greetName.setText(fullName);

            // Display classification & set badge/avatar background
            if (classification != null) {
                switch (classification.toLowerCase()) {
                    case "intermediate":
                        userClass.setBackgroundResource(R.drawable.intermediate_classifier);
                        break;
                    case "advanced":
                        userClass.setBackgroundResource(R.drawable.advanced_classifier);
                        break;
                    default:
                        userClass.setBackgroundResource(R.drawable.beginner_classifier);
                        break;
                }
            } else {
                userClass.setBackgroundResource(R.drawable.beginner_classifier);
            }
        }
        // ---------- END HEADER SECTION ----------

        currentLearner = view.findViewById(R.id.current_learner);
        scoreHistoryContainer = view.findViewById(R.id.scoreHistoryContainer);
        lessonHistory = view.findViewById(R.id.lessonHistory);
        newLearner = view.findViewById(R.id.new_learner);
        continueLessonCard = view.findViewById(R.id.continue_lesson);

        // 🔹 Initially hide all sections
        currentLearner.setVisibility(View.GONE);
        scoreHistoryContainer.setVisibility(View.GONE);
        lessonHistory.setVisibility(View.GONE);
        continueLessonCard.setVisibility(View.GONE);
        newLearner.setVisibility(View.GONE);

        lessonTitleView = view.findViewById(R.id.lesson_title);
        lessonDescView = view.findViewById(R.id.lesson_desc);
        lessonProgressView = view.findViewById(R.id.lesson_progress);
        lessonProgressBar = view.findViewById(R.id.progressBar);

        contLessonTitle = view.findViewById(R.id.cont_lesson_title);
        contChapter = view.findViewById(R.id.chapter);
        contProgressBar = view.findViewById(R.id.ProgressBar);

        checkUserRecord();

        return view;
    }

    private void checkUserRecord() {
        int userId = sessionManager.getUserId();

        if (userId == -1) {
            showNewLearner();
            return;
        }

        dbRefAct.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            showNewLearner();
                            return;
                        }

                        loadRecentLesson(userId);

                        backgroundExecutor.execute(() -> {
                            requireActivity().runOnUiThread(() -> loadScoreHistory(snapshot));
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showNewLearner();
                    }
                });
    }

    private void showCurrentLearner() {
        currentLearner.setVisibility(View.VISIBLE);
        scoreHistoryContainer.setVisibility(View.VISIBLE);
        lessonHistory.setVisibility(View.VISIBLE);
        continueLessonCard.setVisibility(View.VISIBLE);
        newLearner.setVisibility(View.GONE);
    }

    private void showNewLearner() {
        currentLearner.setVisibility(View.GONE);
        scoreHistoryContainer.setVisibility(View.GONE);
        lessonHistory.setVisibility(View.GONE);
        continueLessonCard.setVisibility(View.GONE);
        newLearner.setVisibility(View.VISIBLE);
    }

    private void loadScoreHistory(DataSnapshot snapshot) {
        scoreHistoryContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (DataSnapshot record : snapshot.getChildren()) {
            String title = record.child("title").getValue(String.class);
            Long score = record.child("score").getValue(Long.class);
            Long items = record.child("items").getValue(Long.class);

            View scoreView = inflater.inflate(R.layout.recent_scores, scoreHistoryContainer, false);
            TextView titleView = scoreView.findViewById(R.id.testTitle);
            TextView scoreViewText = scoreView.findViewById(R.id.testScore);

            titleView.setText(title != null ? title : "Untitled Test");
            long totalItems = (items != null) ? items : 20;
            scoreViewText.setText("Score: " + (score != null ? score : 0) + "/" + totalItems);

            scoreHistoryContainer.addView(scoreView);
        }
    }

    private void loadRecentLesson(int userId) {
        dbRefLesson.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists() || !snapshot.hasChildren()) {
                            requireActivity().runOnUiThread(() -> {
                                hideLessonSections();
                                showNewLearner();
                            });
                            return;
                        }

                        showCurrentLearner();

                        for (DataSnapshot record : snapshot.getChildren()) {
                            String title = record.child("lessonTitle").getValue(String.class);
                            if (title == null) continue;

                            String desc = record.child("lessonDesc").getValue(String.class);
                            Long completedLong = record.child("completed").getValue(Long.class);
                            Long totalLong = record.child("totalLessons").getValue(Long.class);

                            int completed = completedLong != null ? completedLong.intValue() : 0;
                            int total = (totalLong != null && totalLong > 0) ? totalLong.intValue() : 1;

                            updateLessonUI(title, desc, completed, total);
                            break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        requireActivity().runOnUiThread(() -> {
                            hideLessonSections();
                            showNewLearner();
                        });
                    }
                });
    }

    private void updateLessonUI(String title, String desc, int completed, int total) {
        requireActivity().runOnUiThread(() -> {
            lessonTitleView.setText(title);
            lessonDescView.setText(desc != null ? desc : "No description available");
            lessonProgressView.setText(completed + "/" + total + " lessons");

            lessonProgressBar.setMax(total);
            lessonProgressBar.setProgress(completed);

            contLessonTitle.setText(title);
            contChapter.setText("CHAPTER " + completed);
            contProgressBar.setMax(total);
            contProgressBar.setProgress(completed);
            contProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#03162A")));

            contProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#B0BEC5")));

            continueLessonCard.setVisibility(View.VISIBLE);
            lessonHistory.setVisibility(View.VISIBLE);
        });
    }

    private void hideLessonSections() {
        requireActivity().runOnUiThread(() -> {
            lessonTitleView.setText("");
            lessonDescView.setText("");
            lessonProgressView.setText("");
            lessonProgressBar.setProgress(0);
            continueLessonCard.setVisibility(View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        backgroundExecutor.shutdownNow();
    }
}
