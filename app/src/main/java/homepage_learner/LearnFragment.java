package homepage_learner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.codex.LessonActivity;
import com.example.codex.R;
import com.google.android.material.card.MaterialCardView;
import com.example.codex.SessionManager;

public class LearnFragment extends Fragment {

    private LinearLayout sectionBeginnerHeader;
    private LinearLayout sectionBeginnerContent;
    private ImageView iconArrow;
    private boolean isContentVisible = false;
    private MaterialCardView lesson1; // Added for lesson card click
    private TextView tvUserName;
    private TextView tvLevel;

    public LearnFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_learn, container, false);

        // Initialize views
        sectionBeginnerHeader = view.findViewById(R.id.sectionBeginnerHeader);
        sectionBeginnerContent = view.findViewById(R.id.sectionBeginnerContent);
        iconArrow = view.findViewById(R.id.iconArrow);
        lesson1 = view.findViewById(R.id.lesson1);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvLevel = view.findViewById(R.id.tvLevel);

        // Initialize SessionManager and retrieve session data
        SessionManager sessionManager = new SessionManager(requireContext());

        if (sessionManager.isLoggedIn()) {
            String firstName = sessionManager.getFirstName();
            String lastName = sessionManager.getLastName();
            String fullName = firstName + " " + lastName;
            String classification = sessionManager.getClassification();

            // Update the user name and classification badge
            tvUserName.setText(fullName);

            // Update classification badge
            updateClassificationBadge(classification);
        }

        // Header click toggles section visibility
        if (sectionBeginnerHeader != null) {
            sectionBeginnerHeader.setOnClickListener(v -> toggleSection());
        }

        // Card click opens LessonActivity
        if (lesson1 != null) {
            lesson1.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LessonActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }

    // Toggle the section visibility when header is clicked
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

    // Rotate the arrow icon
    private void rotateArrow(float from, float to) {
        RotateAnimation rotate = new RotateAnimation(
                from, to, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        iconArrow.startAnimation(rotate);
    }

    // Update the classification badge based on user's classification
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
