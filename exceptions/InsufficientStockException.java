package exceptions;

public class InsufficientStockException extends Exception {
    private final int requested;
    private final int available;

    public InsufficientStockException(String product, int requested, int available) {
        super("Not enough stock for '" + product + "': requested " + requested
                + ", available " + available);
        this.requested = requested;
        this.available = available;
    }

    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
