package exceptions;

/** Unchecked: a programming error, so callers are not forced to catch it. */
public class InvalidPriceException extends RuntimeException {
    public InvalidPriceException(double price) {
        super("Price must be greater than 0 but was " + price);
    }
}
