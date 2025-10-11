package com.example.codex;

import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.widget.TextView;
import android.graphics.Color;

/**
 * Utility class for applying gradient text color to any TextView.
 */
public class GradientTextUtil {

    /**
     * Applies a horizontal gradient to a TextView.
     *
     * @param textView   The TextView to apply the gradient to.
     * @param startColor The starting color of the gradient (e.g., "#594AE2").
     * @param endColor   The ending color of the gradient (e.g., "#8C7BFF").
     */
    public static void applyGradient(TextView textView, String startColor, String endColor) {
        textView.post(() -> {
            float width = textView.getMeasuredWidth();

            // Prevent crash if textView width is 0 (not measured yet)
            if (width == 0) width = textView.getTextSize() * textView.getText().length();

            Shader textShader = new LinearGradient(
                    0, 0, width, textView.getTextSize(),
                    new int[]{
                            Color.parseColor(startColor),
                            Color.parseColor(endColor)
                    },
                    null,
                    Shader.TileMode.CLAMP
            );

            textView.getPaint().setShader(textShader);
            textView.invalidate();
        });
    }
}
