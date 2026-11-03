package app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** A registered user (customer or admin). Saved in data/app_users.csv */
public class AppUser {
    public final int id;
    public final String name, email, passwordHash, role, gender, city;
    public final int age;

    public AppUser(int id, String name, String email, String passwordHash,
                   String role, int age, String gender, String city) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.age = age;
        this.gender = gender;
        this.city = city;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public String toCsv() {
        return id + "," + name + "," + email + "," + passwordHash + "," + role + ","
                + age + "," + gender + "," + city;
    }

    public static AppUser fromCsv(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 8) throw new IllegalArgumentException("Bad user line: " + line);
        return new AppUser(Integer.parseInt(f[0].trim()), f[1], f[2], f[3], f[4],
                Integer.parseInt(f[5].trim()), f[6], f[7]);
    }

    /** SHA-256 hash of a password, as hex text. */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
