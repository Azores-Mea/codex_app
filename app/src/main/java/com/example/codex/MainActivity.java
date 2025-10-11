package com.example.codex;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    LinearLayout formContainer;
    ImageView imageView;
    MaterialButton createAccountBtn, signInBtn;

    DatabaseReference databaseReference;

    private RegistrationHandler registrationHandler;
    private LoginHandler loginHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.codexstart);
        formContainer = findViewById(R.id.wcform);
        createAccountBtn = findViewById(R.id.createAccountBtn);
        signInBtn = findViewById(R.id.signInBtn);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Gradient title
        TextView title = findViewById(R.id.wc);
        Shader textShader = new LinearGradient(
                0, 0, 0, title.getTextSize(),
                new int[]{Color.parseColor("#000405"), Color.parseColor("#00566B")},
                null,
                Shader.TileMode.CLAMP
        );
        title.getPaint().setShader(textShader);

        // Initialize handlers
        registrationHandler = new RegistrationHandler(this, databaseReference);
        loginHandler = new LoginHandler(this);

        // ✅ Handle "Log In" button from registration confirmation
        registrationHandler.setOnRegistrationCompleteListener(() -> {
            registrationHandler.hideRegistrationForm();
            loginHandler.showLoginForm();
        });

        // Logo animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein);
        imageView.startAnimation(fadeIn);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                Animation moveUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.logomoveup);
                imageView.startAnimation(moveUp);

                formContainer.setVisibility(View.VISIBLE);
                Animation slideUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.formslideup);
                formContainer.startAnimation(slideUp);
            }
        });

        createAccountBtn.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            loginHandler.hideLoginForm();
            registrationHandler.showRegistrationForm();
        });

        signInBtn.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            registrationHandler.hideRegistrationForm();
            loginHandler.showLoginForm();
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                int[] scrcoords = new int[2];
                v.getLocationOnScreen(scrcoords);
                float x = event.getRawX() + v.getLeft() - scrcoords[0];
                float y = event.getRawY() + v.getTop() - scrcoords[1];

                if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom()) {
                    hideKeyboardAndClearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    public void hideKeyboardAndClearFocus() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }
}
