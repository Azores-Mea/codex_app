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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReviewFragment extends Fragment {

    private static final String TAG = "ReviewFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private LinearLayout lessonsContainer;
    private DatabaseReference lessonsRef;
    private DatabaseReference assessmentRef;

    // User answer references
    private DatabaseReference machineProblemAnswersRef;
    private DatabaseReference syntaxErrorAnswersRef;
    private DatabaseReference programTracingAnswersRef;
    private DatabaseReference quizResultsRef;

    private SessionManager sessionManager;

    private MaterialButton btnBeginner, btnIntermediate, btnAdvanced;
    private String cachedData = null;
    private String currentFilter = "All";

    // Cache all lesson data after first load
    private List<LessonData> allLessonsCache = new ArrayList<>();
    private boolean isDataLoaded = false;

    public ReviewFragment() {}

    public void onFragmentVisible() {
        Log.d("ReviewFragment", "Fragment is now visible - checking for changes");
        checkAndReloadIfNeeded();
    }

    private void checkAndReloadIfNeeded() {
        int userId = sessionManager.getUserId();
        String userIdString = String.valueOf(userId);

        // Check for any changes in user answers
        machineProblemAnswersRef.child(userIdString)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String newHash = generateHash(snapshot);
                        if (!newHash.equals(cachedData)) {
                            cachedData = newHash;
                            Log.d("ReviewFragment", "Changes detected - reloading lessons");
                            isDataLoaded = false; // Force reload
                            loadLessons();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Check cancelled: " + error.getMessage());
                    }
                });
    }

    private String generateHash(DataSnapshot snapshot) {
        StringBuilder hash = new StringBuilder();
        hash.append(snapshot.getChildrenCount()).append("_");
        for (DataSnapshot child : snapshot.getChildren()) {
            hash.append(child.getKey()).append(":");
            hash.append(child.getChildrenCount()).append("|");
        }
        return hash.toString();
    }

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
        assessmentRef = FirebaseDatabase.getInstance().getReference("assessment");

        machineProblemAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userMachineProblemAnswers");
        syntaxErrorAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userSyntaxErrorAnswers");
        programTracingAnswersRef = FirebaseDatabase.getInstance()
                .getReference("userProgramTracingAnswers");
        quizResultsRef = FirebaseDatabase.getInstance()
                .getReference("quizResults");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_review, container, false);

        lessonsContainer = view.findViewById(R.id.lessonsContainer);

        btnBeginner = view.findViewById(R.id.beginner);
        btnIntermediate = view.findViewById(R.id.intermediate);
        btnAdvanced = view.findViewById(R.id.advanced);

        currentFilter = "All";

        setupFilterButtons();
        updateButtonStates();
        loadLessons();

        return view;
    }

    private void setupFilterButtons() {
        btnBeginner.setOnClickListener(v -> {
            if (currentFilter.equals("Beginner")) {
                currentFilter = "All";
            } else {
                currentFilter = "Beginner";
            }
            sessionManager.saveLessonDifficulty(currentFilter);
            updateButtonStates();
            filterAndDisplayLessons();
        });

        btnIntermediate.setOnClickListener(v -> {
            if (currentFilter.equals("Intermediate")) {
                currentFilter = "All";
            } else {
                currentFilter = "Intermediate";
            }
            sessionManager.saveLessonDifficulty(currentFilter);
            updateButtonStates();
            filterAndDisplayLessons();
        });

        btnAdvanced.setOnClickListener(v -> {
            if (currentFilter.equals("Advanced")) {
                currentFilter = "All";
            } else {
                currentFilter = "Advanced";
            }
            sessionManager.saveLessonDifficulty(currentFilter);
            updateButtonStates();
            filterAndDisplayLessons();
        });
    }

    private void updateButtonStates() {
        resetButton(btnBeginner);
        resetButton(btnIntermediate);
        resetButton(btnAdvanced);

        if (!currentFilter.equals("All")) {
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
                    return;
            }

            selectedButton.setBackgroundColor(Color.parseColor("#03162A"));
            selectedButton.setTextColor(getResources().getColor(android.R.color.white, null));
            selectedButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#03162A")));
        }
    }

    private void resetButton(MaterialButton button) {
        button.setBackgroundColor(getResources().getColor(android.R.color.white, null));
        button.setTextColor(getResources().getColor(android.R.color.holo_blue_light, null));
        button.setStrokeColorResource(android.R.color.holo_blue_light);
    }

    private void loadLessons() {
        if (isDataLoaded) {
            filterAndDisplayLessons();
            return;
        }

        int userId = sessionManager.getUserId();
        String userIdString = String.valueOf(userId);

        lessonsContainer.removeAllViews();
        TextView loadingText = new TextView(getContext());
        loadingText.setText("Loading lessons...");
        loadingText.setTextSize(16);
        loadingText.setPadding(32, 32, 32, 32);
        loadingText.setGravity(android.view.Gravity.CENTER);
        lessonsContainer.addView(loadingText);

        final DataSnapshot[] allSnapshots = new DataSnapshot[6];
        final int[] loadedCount = {0};
        final int totalLoads = 6;

        ValueEventListener loadListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                synchronized (loadedCount) {
                    loadedCount[0]++;

                    if (snapshot.getRef().equals(lessonsRef)) {
                        allSnapshots[0] = snapshot;
                    } else if (snapshot.getRef().equals(assessmentRef)) {
                        allSnapshots[1] = snapshot;
                    } else if (snapshot.getRef().getParent().equals(machineProblemAnswersRef)) {
                        allSnapshots[2] = snapshot;
                    } else if (snapshot.getRef().getParent().equals(syntaxErrorAnswersRef)) {
                        allSnapshots[3] = snapshot;
                    } else if (snapshot.getRef().getParent().equals(programTracingAnswersRef)) {
                        allSnapshots[4] = snapshot;
                    } else if (snapshot.getRef().getParent().equals(quizResultsRef)) {
                        allSnapshots[5] = snapshot;
                    }

                    if (loadedCount[0] == totalLoads) {
                        processLessons(allSnapshots[0], allSnapshots[1],
                                allSnapshots[2], allSnapshots[3], allSnapshots[4], allSnapshots[5]);
                        isDataLoaded = true;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Load error: " + error.getMessage());
                Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
            }
        };

        lessonsRef.addListenerForSingleValueEvent(loadListener);
        assessmentRef.addListenerForSingleValueEvent(loadListener);
        machineProblemAnswersRef.child(userIdString).addListenerForSingleValueEvent(loadListener);
        syntaxErrorAnswersRef.child(userIdString).addListenerForSingleValueEvent(loadListener);
        programTracingAnswersRef.child(userIdString).addListenerForSingleValueEvent(loadListener);
        quizResultsRef.child(userIdString).addListenerForSingleValueEvent(loadListener);
    }

    private void filterAndDisplayLessons() {
        lessonsContainer.removeAllViews();

        List<LessonData> filteredLessons = new ArrayList<>();

        for (LessonData lessonData : allLessonsCache) {
            if (currentFilter.equals("All") ||
                    (lessonData.difficulty != null && lessonData.difficulty.equalsIgnoreCase(currentFilter))) {
                filteredLessons.add(lessonData);
            }
        }

        for (LessonData lessonData : filteredLessons) {
            View lessonCard = createLessonCard(
                    lessonData.lessonId,
                    lessonData.mainTitle,
                    lessonData.difficulty,
                    lessonData.titleDesc,
                    lessonData.totalAssessments,
                    lessonData.assessmentPercent
            );
            lessonsContainer.addView(lessonCard);
        }

        if (filteredLessons.isEmpty()) {
            TextView noLessons = new TextView(getContext());
            String filterText = currentFilter.equals("All") ? "any difficulty" : currentFilter;
            noLessons.setText("No lessons found for " + filterText);
            noLessons.setTextSize(16);
            noLessons.setPadding(32, 32, 32, 32);
            noLessons.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
            lessonsContainer.addView(noLessons);
        }
    }

    private void processLessons(DataSnapshot lessonsSnapshot, DataSnapshot assessmentSnapshot,
                                DataSnapshot mpSnapshot, DataSnapshot seSnapshot,
                                DataSnapshot ptSnapshot, DataSnapshot quizSnapshot) {

        allLessonsCache.clear();

        for (DataSnapshot lessonSnapshot : lessonsSnapshot.getChildren()) {
            String lessonId = lessonSnapshot.getKey();
            String mainTitle = lessonSnapshot.child("main_title").getValue(String.class);
            String difficulty = lessonSnapshot.child("difficulty").getValue(String.class);
            String titleDesc = lessonSnapshot.child("title_desc").getValue(String.class);

            Long orderLong = lessonSnapshot.child("order").getValue(Long.class);
            int lessonOrder = orderLong != null ? orderLong.intValue() : 999;

            DataSnapshot lessonAssessment = assessmentSnapshot.child(lessonId);
            int totalAssessments = 0;
            int completedAssessments = 0;

            if (lessonAssessment.hasChild("MachineProblem")) {
                totalAssessments++;
                if (mpSnapshot.hasChild(lessonId) &&
                        mpSnapshot.child(lessonId).getChildrenCount() > 0) {
                    completedAssessments++;
                }
            }

            if (lessonAssessment.hasChild("FindingSyntaxError")) {
                totalAssessments++;
                if (seSnapshot.hasChild(lessonId) &&
                        seSnapshot.child(lessonId).getChildrenCount() > 0) {
                    completedAssessments++;
                }
            }

            if (lessonAssessment.hasChild("ProgramTracing")) {
                totalAssessments++;
                if (ptSnapshot.hasChild(lessonId) &&
                        ptSnapshot.child(lessonId).getChildrenCount() > 0) {
                    completedAssessments++;
                }
            }

            if (lessonAssessment.hasChild("Quiz")) {
                totalAssessments++;
                if (quizSnapshot.hasChild(lessonId)) {
                    completedAssessments++;
                }
            }

            int assessmentPercent = totalAssessments > 0
                    ? (completedAssessments * 100) / totalAssessments
                    : 0;

            allLessonsCache.add(new LessonData(
                    lessonId, mainTitle, difficulty, titleDesc,
                    totalAssessments, assessmentPercent, lessonOrder
            ));
        }

        Collections.sort(allLessonsCache, new Comparator<LessonData>() {
            @Override
            public int compare(LessonData l1, LessonData l2) {
                if (l1.assessmentPercent != l2.assessmentPercent) {
                    return Integer.compare(l2.assessmentPercent, l1.assessmentPercent);
                }
                return Integer.compare(l1.lessonOrder, l2.lessonOrder);
            }
        });

        filterAndDisplayLessons();
    }

    private static class LessonData {
        String lessonId;
        String mainTitle;
        String difficulty;
        String titleDesc;
        int totalAssessments;
        int assessmentPercent;
        int lessonOrder;

        LessonData(String lessonId, String mainTitle, String difficulty, String titleDesc,
                   int totalAssessments, int assessmentPercent, int lessonOrder) {
            this.lessonId = lessonId;
            this.mainTitle = mainTitle;
            this.difficulty = difficulty;
            this.titleDesc = titleDesc;
            this.totalAssessments = totalAssessments;
            this.assessmentPercent = assessmentPercent;
            this.lessonOrder = lessonOrder;
        }
    }

    private View createLessonCard(String lessonId, String mainTitle, String difficulty,
                                  String titleDesc, int totalAssessments, int assessmentPercent) {

        View cardView = LayoutInflater.from(getContext()).inflate(
                R.layout.review_quizcode, lessonsContainer, false);

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
        TextView dateTextView = cardView.findViewById(R.id.date);
        TextView assessmentPercentText = cardView.findViewById(R.id.assessment_percent);
        MaterialCardView assessmentCard = cardView.findViewById(R.id.assessment);
        View sideBar = cardView.findViewById(R.id.view3);
        MaterialCardView lessonCard = cardView.findViewById(R.id.lesson_card);

        if (mainTitle != null) {
            titleTextView.setText(Html.fromHtml(mainTitle, Html.FROM_HTML_MODE_COMPACT));
        }

        dateTextView.setText("Assessment (" + totalAssessments + ")");
        assessmentPercentText.setText(assessmentPercent + "%");
        assessmentCard.setVisibility(View.VISIBLE);

        if (difficulty != null) {
            switch (difficulty.toLowerCase()) {
                case "beginner":
                    difficultyImageView.setImageResource(R.drawable.beginner_classifier);
                    break;
                case "intermediate":
                    difficultyImageView.setImageResource(R.drawable.intermediate_classifier);
                    titleTextView.setTextColor(Color.parseColor("#66ABF4"));
                    dateTextView.setTextColor(Color.parseColor("#66ABF4"));
                    assessmentPercentText.setTextColor(Color.parseColor("#66ABF4"));
                    assessmentCard.setStrokeColor(Color.parseColor("#66ABF4"));
                    sideBar.setBackgroundColor(Color.parseColor("#66ABF4"));
                    lessonCard.setStrokeColor(Color.parseColor("#66ABF4"));
                    break;
                case "advanced":
                    difficultyImageView.setImageResource(R.drawable.advanced_classifier);
                    titleTextView.setTextColor(Color.parseColor("#A666F4"));
                    dateTextView.setTextColor(Color.parseColor("#A666F4"));
                    assessmentPercentText.setTextColor(Color.parseColor("#A666F4"));
                    assessmentCard.setStrokeColor(Color.parseColor("#A666F4"));
                    sideBar.setBackgroundColor(Color.parseColor("#A666F4"));
                    lessonCard.setStrokeColor(Color.parseColor("#A666F4"));
                    break;
                default:
                    difficultyImageView.setImageResource(R.drawable.beginner_classifier);
            }
        }

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

            int containerId = createFullscreenContainer();

            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right,
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
            );

            transaction.replace(containerId, reviewDisplayFragment);
            transaction.addToBackStack("ReviewDisplay");
            transaction.commit();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening review: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
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

        ((ViewGroup) requireActivity()
                .findViewById(android.R.id.content))
                .addView(container);

        return generatedId;
    }

    @Override
    public void onResume() {
        super.onResume();
        sessionManager.clearSelectedLesson();
        if (isDataLoaded) {
            checkAndReloadIfNeeded();
        }
    }
}