package homepage_learner;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.codex.R;
import com.example.codex.ReviewDisplayFragment;
import com.example.codex.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ReviewFragment extends Fragment {

    private static final String TAG = "ReviewFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private LinearLayout lessonsContainer;
    private DatabaseReference lessonsRef;

    // 🔥 NEW: quiz + exercise result references
    private DatabaseReference quizResultsRef;
    private DatabaseReference exerciseResultsRef;

    private SessionManager sessionManager;

    private MaterialButton btnBeginner, btnIntermediate, btnAdvanced;
    private String currentFilter = "Beginner"; // Default filter

    public ReviewFragment() {}

    public static ReviewFragment newInstance(String param1, String param2) {
        ReviewFragment fragment = new ReviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        sessionManager = new SessionManager(requireContext());
        lessonsRef = FirebaseDatabase.getInstance().getReference("Lessons");

        // 🔥 Get user ID for filtering quiz/exercise results
        int userId = sessionManager.getUserId();
        String userIdString = String.valueOf(userId);

        quizResultsRef = FirebaseDatabase.getInstance().getReference("quizResults").child(userIdString);
        exerciseResultsRef = FirebaseDatabase.getInstance().getReference("exerciseResults").child(userIdString);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_review, container, false);

        lessonsContainer = view.findViewById(R.id.lessonsContainer);

        btnBeginner = view.findViewById(R.id.beginner);
        btnIntermediate = view.findViewById(R.id.intermediate);
        btnAdvanced = view.findViewById(R.id.advanced);

        String savedDifficulty = sessionManager.getLessonDifficulty();
        if (savedDifficulty != null && !savedDifficulty.isEmpty()) {
            currentFilter = savedDifficulty;
        }

        setupFilterButtons();
        updateButtonStates();
        loadLessons();

        return view;
    }

    private void setupFilterButtons() {
        btnBeginner.setOnClickListener(v -> {
            currentFilter = "Beginner";
            sessionManager.saveLessonDifficulty(currentFilter);
            updateButtonStates();
            loadLessons();
        });

        btnIntermediate.setOnClickListener(v -> {
            currentFilter = "Intermediate";
            sessionManager.saveLessonDifficulty(currentFilter);
            updateButtonStates();
            loadLessons();
        });

        btnAdvanced.setOnClickListener(v -> {
            currentFilter = "Advanced";
            sessionManager.saveLessonDifficulty(currentFilter);
            updateButtonStates();
            loadLessons();
        });
    }

    private void updateButtonStates() {
        resetButton(btnBeginner);
        resetButton(btnIntermediate);
        resetButton(btnAdvanced);

        MaterialButton selectedButton;

        switch (currentFilter) {
            case "Beginner":
                selectedButton = btnBeginner;
                break;
            case "Intermediate":
                selectedButton = btnIntermediate;
                break;
            case "Advanced":
                selectedButton = btnAdvanced;
                break;
            default:
                selectedButton = btnBeginner;
        }

        selectedButton.setBackgroundColor(Color.parseColor("#03162A"));
        selectedButton.setTextColor(getResources().getColor(android.R.color.white, null));
        selectedButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#03162A")));
    }

    private void resetButton(MaterialButton button) {
        button.setBackgroundColor(getResources().getColor(android.R.color.white, null));
        button.setTextColor(getResources().getColor(android.R.color.holo_blue_light, null));
        button.setStrokeColorResource(android.R.color.holo_blue_light);
    }

    // ====================================================================================
    // 🔥 THIS IS WHERE THE FILTERING MAGIC HAPPENS
    // ====================================================================================
    private void loadLessons() {
        lessonsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot lessonsSnapshot) {

                lessonsContainer.removeAllViews();

                quizResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot quizSnapshot) {

                        exerciseResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot exerciseSnapshot) {

                                int lessonCount = 0;

                                for (DataSnapshot lessonSnapshot : lessonsSnapshot.getChildren()) {

                                    String lessonId = lessonSnapshot.getKey();
                                    String mainTitle = lessonSnapshot.child("main_title").getValue(String.class);
                                    String difficulty = lessonSnapshot.child("difficulty").getValue(String.class);
                                    String titleDesc = lessonSnapshot.child("title_desc").getValue(String.class);

                                    // 🔥 Difficulty filter first
                                    if (difficulty == null || !difficulty.equalsIgnoreCase(currentFilter)) {
                                        continue;
                                    }

                                    // 🔥 Check if user finished quiz
                                    boolean hasQuiz = quizSnapshot.hasChild(lessonId);

                                    // 🔥 Check if user finished exercise
                                    boolean hasExercise = false;
                                    if (exerciseSnapshot.hasChild(lessonId)) {
                                        Boolean completed = exerciseSnapshot.child(lessonId)
                                                .child("completed")
                                                .getValue(Boolean.class);
                                        hasExercise = completed != null && completed;
                                    }

                                    // 🔥 Skip if user has NOT done quiz or exercise
                                    if (!hasQuiz && !hasExercise) {
                                        continue;
                                    }

                                    // 🟦 User completed something → show lesson card
                                    View lessonCard = createLessonCard(lessonId, mainTitle, difficulty, titleDesc);
                                    lessonsContainer.addView(lessonCard);
                                    lessonCount++;
                                }

                                if (lessonCount == 0) {
                                    TextView noLessons = new TextView(getContext());
                                    noLessons.setText("No completed lessons found for " + currentFilter);
                                    noLessons.setTextSize(16);
                                    noLessons.setPadding(32, 32, 32, 32);
                                    noLessons.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                                    lessonsContainer.addView(noLessons);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(getContext(), "Error loading lessons", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ====================================================================================

    private View createLessonCard(String lessonId, String mainTitle, String difficulty, String titleDesc) {

        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.review_quizcode, lessonsContainer, false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(params);

        View actualCard = cardView.findViewById(R.id.lesson_card);
        if (actualCard == null) actualCard = cardView;

        TextView titleTextView = cardView.findViewById(R.id.l_title);
        ImageView difficultyImageView = cardView.findViewById(R.id.difficulty);
        MaterialCardView percentStroke = cardView.findViewById(R.id.percentStroke);
        TextView dateTextView = cardView.findViewById(R.id.date);
        TextView progressTextView = cardView.findViewById(R.id.progressPercent);
        View sideBar = cardView.findViewById(R.id.view3);
        MaterialCardView lessonCard = cardView.findViewById(R.id.lesson_card);

        if (mainTitle != null) {
            titleTextView.setText(Html.fromHtml(mainTitle, Html.FROM_HTML_MODE_COMPACT));
        }

        if (difficulty != null) {
            switch (difficulty.toLowerCase()) {
                case "beginner":
                    difficultyImageView.setImageResource(R.drawable.beginner_classifier);
                    break;
                case "intermediate":
                    difficultyImageView.setImageResource(R.drawable.intermediate_classifier);
                    titleTextView.setTextColor(Color.parseColor("#A666F4"));
                    dateTextView.setTextColor(Color.parseColor("#A666F4"));
                    progressTextView.setTextColor(Color.parseColor("#A666F4"));
                    percentStroke.setStrokeColor(Color.parseColor("#A666F4"));
                    sideBar.setBackgroundColor(Color.parseColor("#A666F4"));
                    lessonCard.setStrokeColor(Color.parseColor("#A666F4"));
                    break;
                case "advanced":
                    difficultyImageView.setImageResource(R.drawable.advanced_classifier);
                    titleTextView.setTextColor(Color.parseColor("#66ABF4"));
                    dateTextView.setTextColor(Color.parseColor("#66ABF4"));
                    progressTextView.setTextColor(Color.parseColor("#66ABF4"));
                    percentStroke.setStrokeColor(Color.parseColor("#66ABF4"));
                    sideBar.setBackgroundColor(Color.parseColor("#66ABF4"));
                    lessonCard.setStrokeColor(Color.parseColor("#66ABF4"));
                    break;
                default:
                    difficultyImageView.setImageResource(R.drawable.beginner_classifier);
            }
        }

        dateTextView.setText("MM/DD/YYYY");
        progressTextView.setText("0%");

        actualCard.setOnClickListener(v -> {
            sessionManager.saveSelectedLesson(lessonId);
            openReviewDisplay(lessonId, mainTitle, difficulty);
        });

        return cardView;
    }

    private void openReviewDisplay(String lessonId, String mainTitle, String difficulty) {
        try {
            ReviewDisplayFragment reviewDisplayFragment =
                    ReviewDisplayFragment.newInstance(lessonId, mainTitle, difficulty);

            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

            // 🔥 Create the fullscreen overlay ABOVE bottom navigation
            int containerId = createFullscreenContainer();

            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right,
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
            );

            // Load review fragment into the overlay
            transaction.replace(containerId, reviewDisplayFragment);
            transaction.addToBackStack("ReviewDisplay");
            transaction.commit();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening review: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    private int createFullscreenContainer() {
        FrameLayout container = new FrameLayout(requireContext());
        int generatedId = View.generateViewId();
        container.setId(generatedId);

        container.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Add to the Activity root (always ABOVE everything else)
        ((ViewGroup) requireActivity()
                .findViewById(android.R.id.content))
                .addView(container);

        return generatedId;
    }
    private int findFragmentContainerId() {
        String[] possibleIds = {
                "fragment_container",
                "main_container",
                "content_frame",
                "container",
                "nav_host_fragment",
                "fragment_holder",
                "main_fragment_container"
        };

        for (String idName : possibleIds) {
            int id = getResources().getIdentifier(idName, "id", requireContext().getPackageName());
            if (id != 0) {
                try {
                    View container = requireActivity().findViewById(id);
                    if (container != null) return id;
                } catch (Exception ignored) {}
            }
        }

        if (getView() != null && getView().getParent() instanceof View) {
            return ((View) getView().getParent()).getId();
        }

        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        sessionManager.clearSelectedLesson();
    }
}
