package server;

import exceptions.AuthenticationException;

public class PasswordPolicyValidator {
    private static final int MIN_LENGTH = 6;

    /**
     * Validates the given password against the defined password policy.
     * The password must be at least 6 characters long and contain at least one uppercase letter, one lowercase letter, and one digit.
     */
    public static void validatePassword(String password) throws AuthenticationException {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new AuthenticationException("the password must contain at least " + MIN_LENGTH + " characters.");
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
            throw new AuthenticationException(
                    "the password must contain at least one upper case letter, one lower case letter and one digit.");
        }
    }
}