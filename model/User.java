package model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class User {
    private final int id;
    private final String name;
    private final String email;
    private final String passwordHash;

    public User(int id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }

    // Login check: hash what the user typed and compare with the saved hash
    public boolean login(String rawPassword) {
        return passwordHash.equals(hash(rawPassword));
    }

    // ABSTRACTION: every subclass MUST say what its role is
    public abstract String getRole();

    public String describe() {
        return getRole() + " #" + id + " - " + name + " (" + email + ")";
    }

    // Turns a password into a SHA-256 hash (same method the main program uses)
    public static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
