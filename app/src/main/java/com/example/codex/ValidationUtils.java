package com.example.codex;

import android.util.Patterns;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        if (email == null) return false;

        // Convert to lowercase for consistent checking
        String lowerEmail = email.toLowerCase();

        // Basic email format validation
        if (!Patterns.EMAIL_ADDRESS.matcher(lowerEmail).matches()) {
            return false;
        }

        // Allowed domains
        String[] allowedDomains = {
                "@gmail.com",
                "@outlook.com",
                "@hotmail.com",
                "@yahoo.com",
                "@icloud.com",
                "@umak.edu.ph"
        };

        for (String domain : allowedDomains) {
            if (lowerEmail.endsWith(domain)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isStrongPassword(String password) {
        // 8–20 chars, uppercase, lowercase, number, special symbol, no spaces
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,20}$";
        return password.matches(regex);
    }

    // --- NAME VALIDATION (FIRST/LAST NAME) ---
    public static boolean isValidName(String name) {
        if (name == null) return false;

        // Max 50 characters, letters + spaces + hyphens only
        String regex = "^[A-Za-z\\s-]{1,50}$";

        return name.matches(regex);
    }
}
