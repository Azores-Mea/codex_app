package com.example.codex;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import homepage_learner.HomeFragment;
import homepage_learner.LearnFragment;

public class Navigation_ActivityLearner extends AppCompatActivity {

    private TextView userName, userClass;
    private ImageView avatar;

    // Keep fragment instances so they are not recreated every tab switch
    private HomeFragment homeFragment;
    private LearnFragment learnFragment;
    private ReviewFragment reviewFragment;
    private ProgressFragment progressFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_main);

        // Bind header views (they live in activity_home_main.xml)
        userName = findViewById(R.id.user_name);
        userClass = findViewById(R.id.user_class);
        avatar = findViewById(R.id.imageViewAvatar);

        // Load user data from SessionManager
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            String firstName = session.getFirstName();
            String lastName = session.getLastName();
            String classification = session.getUserType();

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

        // Initialize fragments
        homeFragment = new HomeFragment();
        learnFragment = new LearnFragment();
        reviewFragment = new ReviewFragment();
        progressFragment = new ProgressFragment();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            // Add all fragments, hide all except homeFragment
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, progressFragment, "progress").hide(progressFragment)
                    .add(R.id.fragment_container, reviewFragment, "review").hide(reviewFragment)
                    .add(R.id.fragment_container, learnFragment, "learn").hide(learnFragment)
                    .add(R.id.fragment_container, homeFragment, "home")
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment toShow = null;
            int id = item.getItemId();

            if (id == R.id.home) toShow = homeFragment;
            else if (id == R.id.learn) toShow = learnFragment;
            else if (id == R.id.review) toShow = reviewFragment;
            else if (id == R.id.progress) toShow = progressFragment;

            if (toShow != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                Fragment[] fragments = {homeFragment, learnFragment, reviewFragment, progressFragment};

                for (Fragment f : fragments) {
                    if (f == toShow) ft.show(f);
                    else ft.hide(f);
                }

                ft.commit();
            }
            return true;
        });
    }

    // Helper method for HomeFragment to open LearnFragment
    public void openLearnFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment[] fragments = {homeFragment, learnFragment, reviewFragment, progressFragment};

        for (Fragment f : fragments) {
            if (f == learnFragment) ft.show(f);
            else ft.hide(f);
        }
        ft.commit();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.learn);
    }
}
