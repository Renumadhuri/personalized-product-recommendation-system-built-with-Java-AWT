package exceptions;

public class WeakPasswordException extends ValidationException {
    public WeakPasswordException(String reason) {
        super("password", "Weak password: " + reason);
    }
}
