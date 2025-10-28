package homepage_learner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.codex.R;

public class LearnFragment extends Fragment {

    private LinearLayout sectionBeginnerHeader;
    private LinearLayout sectionBeginnerContent;
    private ImageView iconArrow;
    private boolean isContentVisible = false;

    public LearnFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_learn, container, false);

        sectionBeginnerHeader = view.findViewById(R.id.sectionBeginnerHeader);
        sectionBeginnerContent = view.findViewById(R.id.sectionBeginnerContent);
        iconArrow = view.findViewById(R.id.iconArrow);

        if (sectionBeginnerHeader != null) {
            sectionBeginnerHeader.setOnClickListener(v -> {
                toggleSection();
            });
        }

        return view;
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
                from,
                to,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        iconArrow.startAnimation(rotate);
    }
}
