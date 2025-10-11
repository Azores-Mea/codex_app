package com.example.codex;

import android.util.Patterns;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        // Check if it's a valid email format AND ends with @gmail.com
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
                && email.toLowerCase().endsWith("@gmail.com");
    }

    public static boolean isStrongPassword(String password) {
        // 8–20 chars, uppercase, lowercase, number, special symbol, no spaces
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,20}$";
        return password.matches(regex);
    }
}
