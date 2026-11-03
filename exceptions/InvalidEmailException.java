package exceptions;

public class InvalidEmailException extends ValidationException {
    public InvalidEmailException(String email) {
        super("email", "Invalid email address '" + email + "'. Use the format name@domain.com");
    }
}
