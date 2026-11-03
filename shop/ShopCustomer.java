package shop;

import exceptions.ValidationException;
import interfaces.Authenticatable;
import interfaces.Describable;
import interfaces.Validatable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import validation.Validator;

public class ShopCustomer implements Authenticatable, Validatable, Describable {
    private final int id;
    private final String name;
    private final String email;
    private final String passwordHash;

    public ShopCustomer(int id, String name, String email, String rawPassword) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = hash(rawPassword);
    }

    static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(text.getBytes(StandardCharsets.UTF_8))) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public boolean login(String password) {
        return password != null && passwordHash.equals(hash(password));
    }

    @Override
    public String getRole() { return "CUSTOMER"; }

    @Override
    public void validate() throws ValidationException {
        Validator.validateName(name);
        Validator.validateEmail(email);
    }

    @Override
    public String describe() {
        return "Customer #" + id + " | " + name + " | " + email;
    }
}
