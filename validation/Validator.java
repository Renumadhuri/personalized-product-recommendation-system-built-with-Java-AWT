package validation;

import exceptions.InvalidEmailException;
import exceptions.ValidationException;
import exceptions.WeakPasswordException;

public final class Validator {

    private Validator() { }   // utility class, no objects

    public static void validateName(String name) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "Name cannot be empty.");
        }
        if (!name.trim().matches("[A-Za-z ]{2,50}")) {
            throw new ValidationException("name", "Name must be 2-50 letters or spaces only.");
        }
    }

    public static void validateEmail(String email) throws InvalidEmailException {
        if (email == null || !email.trim().matches("^[\\w.+-]+@[\\w-]+\\.[A-Za-z]{2,}$")) {
            throw new InvalidEmailException(email);
        }
    }

    public static void validatePassword(String password) throws WeakPasswordException {
        if (password == null || password.length() < 6) {
            throw new WeakPasswordException("must be at least 6 characters long.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char ch : password.toCharArray()) {
            if (Character.isLetter(ch)) hasLetter = true;
            if (Character.isDigit(ch)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw new WeakPasswordException("must contain at least one letter and one digit.");
        }
    }

    public static void validateQuantity(int quantity) throws ValidationException {
        if (quantity < 1 || quantity > 100) {
            throw new ValidationException("quantity", "Quantity must be between 1 and 100.");
        }
    }

    /** Converts text to int; wraps NumberFormatException (exception chaining). */
    public static int parseInt(String text, String field) throws ValidationException {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(field, "'" + text + "' is not a whole number.", e);
        }
    }
}
