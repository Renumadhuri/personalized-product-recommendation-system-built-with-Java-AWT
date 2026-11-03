package exceptions;

public class LoginFailedException extends Exception {
    private final int attemptsLeft;

    public LoginFailedException(String message, int attemptsLeft) {
        super(message);
        this.attemptsLeft = attemptsLeft;
    }

    public int getAttemptsLeft() { return attemptsLeft; }
}
