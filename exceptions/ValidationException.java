package exceptions;

/** Base class for all input-validation errors (checked exception). */
public class ValidationException extends Exception {
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public ValidationException(String field, String message, Throwable cause) {
        super(message, cause);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
