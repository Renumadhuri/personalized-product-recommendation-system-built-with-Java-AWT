package shop;

public class ActivityLogger implements AutoCloseable {
    public ActivityLogger() {
        System.out.println("[LOG] Logger opened");
    }

    public void log(String message) {
        System.out.println("[LOG] " + message);
    }

    @Override
    public void close() {   // called automatically at the end of try-with-resources
        System.out.println("[LOG] Logger closed");
    }
}
