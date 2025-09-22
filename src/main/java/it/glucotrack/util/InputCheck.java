package it.glucotrack.util;

/*
* INPUT CHECK CLASS
* */

public class InputCheck {


    public static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email != null && email.matches(emailRegex);
    }

    public static boolean isValidPassword(String password) {
        // At least 8 characters, one uppercase, one lowercase, one digit, one special character
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password != null && password.matches(passwordRegex);
    }


    // Check non-null, non-empty string
    public static boolean isValidString(String input) {
        return input != null && !input.trim().isEmpty();
    }

    public static boolean isAlphabetic(String input) {
        return isValidString(input) && input.matches("[a-zA-Z ]+");
    }

    public static boolean isPositiveInt(int n) {
        return n > 0;
    }

    public static boolean isValidYear(int year) {
        return year > 0 && year <= java.time.Year.now().getValue();
    }


    public static void requireValidString(String input, String fieldName) {
        if (!isValidString(input)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
    public static void requireValidYear(int year) {
        if (!isValidYear(year)) {
            throw new IllegalArgumentException("Invalid year: " + year);
        }
    }

    public static boolean hasLengthBetween(String input, int min, int max) {
        return isValidString(input) && input.length() >= min && input.length() <= max;
    }

    public static boolean isAlphanumeric(String input) {
        return isValidString(input) && input.matches("[a-zA-Z0-9 ]+");
    }

}

