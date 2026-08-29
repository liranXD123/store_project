package server;

import exceptions.AuthenticationException;

public class PasswordPolicyValidator {
    private static final int MIN_LENGTH = 6;

    /**
     * בדיקת תקינות סיסמה:
     * לפחות 6 תווים, אות גדולה, אות קטנה, וספרה לפחות אחת.
     */
    public static void validatePassword(String password) throws AuthenticationException {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new AuthenticationException("הסיסמה חייבת להכיל לפחות " + MIN_LENGTH + " תווים.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }

        if (!hasUpper || !hasLower || !hasDigit) {
            throw new AuthenticationException("הסיסמה חייבת להכיל לפחות אות גדולה אחת באנגלית, אות קטנה אחת, וספרה אחת.");
        }
    }
}