package homepage_learner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    private LinearLayout currentLearner, scoreHistoryContainer;
    private CardView newLearner;
    private DatabaseReference dbRefAct, userRef;
    private SessionManager sessionManager;

    private TextView userName, userClass;
    private ImageView avatar;
    private ValueEventListener userListener;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new SessionManager(requireContext());

        // ---------- HEADER ----------
        userName = view.findViewById(R.id.user_name);
        userClass = view.findViewById(R.id.user_class);
        avatar = view.findViewById(R.id.imageViewAvatar);
        TextView greetName = view.findViewById(R.id.learner_name);

        // ---------- SCORE VIEWS ----------
        currentLearner = view.findViewById(R.id.current_learner);
        scoreHistoryContainer = view.findViewById(R.id.scoreHistoryContainer);
        newLearner = view.findViewById(R.id.new_learner);

        showNewLearner();

        // ---------- FIREBASE REFERENCES ----------
        dbRefAct = FirebaseDatabase.getInstance().getReference("RecentAct");

        // ---------- NEW LEARNER BUTTON ----------
        newLearner.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Navigation_ActivityLearner.class);
            startActivity(intent);
        });

        MaterialButton startLearning = view.findViewById(R.id.start_learning);
        startLearning.setOnClickListener(v -> {
            // Notify the parent activity to switch to LearnFragment
            Bundle result = new Bundle();
            getParentFragmentManager().setFragmentResult("openLearnFragmentRequest", result);
        });

        if (sessionManager.isLoggedIn()) {
            int userId = sessionManager.getUserId();
            Log.d("HomeFragment", "Logged in user ID: " + userId);

            if (userId != -1) {
                userRef = FirebaseDatabase.getInstance().getReference("Users").child(String.valueOf(userId));
                attachUserListener(greetName);
                attachScoreListener(userId);
            } else {
                showNewLearner();
            }
        } else {
            showNewLearner();
        }

        return view;
    }

    private void attachUserListener(TextView greetName) {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String classification = snapshot.child("classification").getValue(String.class);

                String fullName = (firstName != null ? firstName : "User") +
                        (lastName != null ? " " + lastName : "");
                userName.setText(fullName);
                greetName.setText(fullName);

                if (classification != null) {
                    switch (classification.toLowerCase()) {
                        case "beginner":
                            userClass.setBackgroundResource(R.drawable.beginner_classifier);
                            break;
                        case "intermediate":
                            userClass.setBackgroundResource(R.drawable.intermediate_classifier);
                            break;
                        case "advanced":
                            userClass.setBackgroundResource(R.drawable.advanced_classifier);
                            break;
                        default:
                            userClass.setBackgroundResource(R.drawable.none_classifier);
                            break;
                    }
                } else {
                    userClass.setBackgroundResource(R.drawable.none_classifier);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        userRef.addValueEventListener(userListener);
    }

    private void attachScoreListener(int userId) {
        Log.d("HomeFragment", "Listening for RecentAct userId: " + userId);
        dbRefAct.child(String.valueOf(userId))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("HomeFragment", "Score Snapshot Exists: " + snapshot.exists() +
                                " | Children: " + snapshot.getChildrenCount());

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            Log.w("HomeFragment", "No RecentAct data found for user " + userId);
                            hideAllSections();
                            showNewLearner();
                            return;
                        }

                        showCurrentLearner();
                        loadScoreHistory(snapshot);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeFragment", "Score listener cancelled: " + error.getMessage());
                    }
                });
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
            long scoreValue = (score != null) ? score : 0;
            scoreViewText.setText("Score: " + scoreValue + "/" + totalItems);

            scoreHistoryContainer.addView(scoreView);
        }

        if (scoreHistoryContainer.getChildCount() == 0) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No score history found.");
            scoreHistoryContainer.addView(emptyText);
        }
    }

    private void showCurrentLearner() {
        currentLearner.setVisibility(View.GONE);
        scoreHistoryContainer.setVisibility(View.VISIBLE);
        newLearner.setVisibility(View.VISIBLE); // keep visible alongside score history
    }

    private void showNewLearner() {
        currentLearner.setVisibility(View.GONE);
        scoreHistoryContainer.setVisibility(View.GONE);
        newLearner.setVisibility(View.VISIBLE);
    }

    private void hideAllSections() {
        currentLearner.setVisibility(View.GONE);
        scoreHistoryContainer.setVisibility(View.GONE);
        newLearner.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }
}
