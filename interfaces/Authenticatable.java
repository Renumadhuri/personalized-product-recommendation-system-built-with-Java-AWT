package interfaces;

public interface Authenticatable {
    boolean login(String password);   // abstract
    String getRole();                 // abstract

    default boolean isAdmin() {       // default method
        return "STAFF".equals(getRole());
    }
}
