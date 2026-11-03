package app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Holds all data in memory and saves it to CSV files in the data folder. */
public class DataStore {
    public static final String DIR = "data";
    private static final Path USERS = Paths.get(DIR, "app_users.csv");
    private static final Path PRODUCTS = Paths.get(DIR, "app_products.csv");
    private static final Path PURCHASES = Paths.get(DIR, "app_purchases.csv");
    private static final Path HISTORY = Paths.get(DIR, "app_history.csv");
    private static final Path SEARCHES = Paths.get(DIR, "app_searches.csv");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public final List<AppUser> users = new ArrayList<>();
    public final List<AppProduct> products = new ArrayList<>();
    public final List<PurchaseRecord> purchases = new ArrayList<>();

    /** Loads the CSV files. On the very first run, creates sample data. */
    public void load() throws IOException {
        Files.createDirectories(Paths.get(DIR));
        users.clear();
        products.clear();
        purchases.clear();
        if (!Files.exists(USERS)) {
            seed();
            return;
        }
        for (String l : readLines(USERS)) {
            try { users.add(AppUser.fromCsv(l)); } catch (IllegalArgumentException e) { System.err.println("Skipped: " + e.getMessage()); }
        }
        for (String l : readLines(PRODUCTS)) {
            try { products.add(AppProduct.fromCsv(l)); } catch (IllegalArgumentException e) { System.err.println("Skipped: " + e.getMessage()); }
        }
        for (String l : readLines(PURCHASES)) {
            try { purchases.add(PurchaseRecord.fromCsv(l)); } catch (IllegalArgumentException e) { System.err.println("Skipped: " + e.getMessage()); }
        }
    }

    // ---------- lookups ----------
    public AppUser findUserByEmail(String email) {
        for (AppUser u : users) if (u.email.equalsIgnoreCase(email.trim())) return u;
        return null;
    }

    public AppUser findUser(int id) {
        for (AppUser u : users) if (u.id == id) return u;
        return null;
    }

    public AppProduct findProduct(int id) {
        for (AppProduct p : products) if (p.id == id) return p;
        return null;
    }

    public int nextUserId() {
        int m = 0;
        for (AppUser u : users) m = Math.max(m, u.id);
        return m + 1;
    }

    public int nextProductId() {
        int m = 100;
        for (AppProduct p : products) m = Math.max(m, p.id);
        return m + 1;
    }

    public int nextOrderId() {
        int m = 0;
        for (PurchaseRecord r : purchases) m = Math.max(m, r.orderId);
        return m + 1;
    }

    // ---------- saving ----------
    public void saveUsers() throws IOException {
        List<String> rows = new ArrayList<>();
        for (AppUser u : users) rows.add(u.toCsv());
        writeLines(USERS, "id,name,email,passwordHash,role,age,gender,city", rows);
    }

    public void saveProducts() throws IOException {
        List<String> rows = new ArrayList<>();
        for (AppProduct p : products) rows.add(p.toCsv());
        writeLines(PRODUCTS, "id,name,category,price,stock,season,audience", rows);
    }

    public void savePurchases() throws IOException {
        List<String> rows = new ArrayList<>();
        for (PurchaseRecord r : purchases) rows.add(r.toCsv());
        writeLines(PURCHASES, "orderId,customerId,productId,quantity,date", rows);
    }

    // ---------- recommendation history ----------
    public void appendHistory(int customerId, String productName) throws IOException {
        List<String> line = new ArrayList<>();
        if (!Files.exists(HISTORY)) line.add("customerId,dateTime,productName");
        line.add(customerId + "," + LocalDateTime.now().format(FMT) + "," + productName);
        Files.write(HISTORY, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Returns rows {customerId, dateTime, productName}. Use customerId -1 for everyone. */
    public List<String[]> loadHistory(int customerId) throws IOException {
        List<String[]> out = new ArrayList<>();
        for (String l : readLines(HISTORY)) {
            String[] f = l.split(",", -1);
            if (f.length != 3) continue;
            if (customerId == -1 || f[0].trim().equals(String.valueOf(customerId))) out.add(f);
        }
        return out;
    }

    /** Remembers what a customer typed in the product search box. */
    public void appendSearch(int customerId, String term) throws IOException {
        term = term.trim().replace(",", " ");
        if (term.isEmpty()) return;
        List<String> line = new ArrayList<>();
        if (!Files.exists(SEARCHES)) line.add("customerId,dateTime,term");
        line.add(customerId + "," + LocalDateTime.now().format(FMT) + "," + term);
        Files.write(SEARCHES, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Returns rows {customerId, dateTime, term} for one customer, oldest first. */
    public List<String[]> loadSearches(int customerId) throws IOException {
        List<String[]> out = new ArrayList<>();
        for (String l : readLines(SEARCHES)) {
            String[] f = l.split(",", -1);
            if (f.length != 3) continue;
            if (f[0].trim().equals(String.valueOf(customerId))) out.add(f);
        }
        return out;
    }

    /** Writes data/app_history_export.csv and returns the number of rows. */
    public int exportHistory(int customerId) throws IOException {
        List<String> rows = new ArrayList<>();
        for (String[] h : loadHistory(customerId)) {
            AppUser u = findUser(Integer.parseInt(h[0].trim()));
            rows.add(h[0] + "," + (u == null ? "unknown" : u.name) + "," + h[1] + "," + h[2]);
        }
        writeLines(Paths.get(DIR, "app_history_export.csv"), "customerId,customerName,dateTime,productName", rows);
        return rows.size();
    }

    /** Writes data/app_orders_export.csv and returns the number of rows. */
    public int exportOrders(List<PurchaseRecord> records) throws IOException {
        List<String> rows = new ArrayList<>();
        for (PurchaseRecord r : records) {
            AppUser u = findUser(r.customerId);
            AppProduct p = findProduct(r.productId);
            double price = p == null ? 0 : p.price;
            rows.add(r.orderId + "," + r.customerId + "," + (u == null ? "unknown" : u.name) + ","
                    + r.productId + "," + (p == null ? "unknown" : p.name) + "," + r.quantity + ","
                    + price + "," + (price * r.quantity) + "," + r.date);
        }
        writeLines(Paths.get(DIR, "app_orders_export.csv"),
                "orderId,customerId,customerName,productId,productName,quantity,unitPrice,lineTotal,date", rows);
        return rows.size();
    }

    // ---------- helpers ----------
    private List<String> readLines(Path p) throws IOException {
        List<String> out = new ArrayList<>();
        if (!Files.exists(p)) return out;
        boolean header = true;
        for (String l : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (header) { header = false; continue; }
            if (!l.trim().isEmpty()) out.add(l);
        }
        return out;
    }

    private void writeLines(Path p, String header, List<String> rows) throws IOException {
        List<String> all = new ArrayList<>();
        all.add(header);
        all.addAll(rows);
        Files.write(p, all, StandardCharsets.UTF_8);
    }

    private void seed() throws IOException {
        users.add(new AppUser(1, "Admin", "admin@miniso.com", AppUser.hash("admin123"), "ADMIN", 30, "Other", "Chennai"));
        users.add(new AppUser(2, "Priya", "priya@gmail.com", AppUser.hash("priya123"), "CUSTOMER", 22, "Female", "Chennai"));
        users.add(new AppUser(3, "Ravi", "ravi@gmail.com", AppUser.hash("ravi123"), "CUSTOMER", 35, "Male", "Sivakasi"));
        users.add(new AppUser(4, "Sneha", "sneha@gmail.com", AppUser.hash("sneha123"), "CUSTOMER", 12, "Female", "Sivakasi"));
        users.add(new AppUser(5, "Arun", "arun@gmail.com", AppUser.hash("arun123"), "CUSTOMER", 17, "Male", "Madurai"));

        products.add(new AppProduct(101, "Plush Toy Bear", "Toys", 499, 20, "All", "Kids"));
        products.add(new AppProduct(102, "Building Blocks Set", "Toys", 699, 15, "All", "Kids"));
        products.add(new AppProduct(103, "Water Gun", "Toys", 299, 25, "Summer", "Kids"));
        products.add(new AppProduct(110, "Sling Bag", "Bags", 899, 12, "All", "Teens"));
        products.add(new AppProduct(111, "Travel Backpack", "Bags", 1299, 10, "All", "Adults"));
        products.add(new AppProduct(120, "Lip Balm", "Beauty", 199, 40, "Winter", "Adults"));
        products.add(new AppProduct(121, "Sunscreen Lotion", "Beauty", 399, 30, "Summer", "Adults"));
        products.add(new AppProduct(130, "Umbrella", "Accessories", 449, 18, "Monsoon", "All"));
        products.add(new AppProduct(131, "Insulated Bottle", "Home", 599, 22, "All", "Adults"));
        products.add(new AppProduct(132, "Scented Candle", "Home", 349, 16, "Winter", "Adults"));
        products.add(new AppProduct(133, "Notebook Set", "Stationery", 249, 50, "All", "Teens"));
        products.add(new AppProduct(134, "Gel Pen Pack", "Stationery", 149, 60, "All", "Teens"));
        products.add(new AppProduct(135, "Raincoat", "Accessories", 799, 14, "Monsoon", "All"));

        String d = LocalDate.now().toString();
        purchases.add(new PurchaseRecord(1, 2, 121, 1, d));
        purchases.add(new PurchaseRecord(1, 2, 120, 1, d));
        purchases.add(new PurchaseRecord(2, 3, 111, 1, d));
        purchases.add(new PurchaseRecord(2, 3, 131, 1, d));
        purchases.add(new PurchaseRecord(3, 4, 101, 1, d));
        purchases.add(new PurchaseRecord(3, 4, 103, 1, d));
        purchases.add(new PurchaseRecord(4, 5, 133, 1, d));
        purchases.add(new PurchaseRecord(4, 5, 134, 1, d));
        purchases.add(new PurchaseRecord(5, 5, 110, 1, d));
        purchases.add(new PurchaseRecord(6, 3, 130, 1, d));
        purchases.add(new PurchaseRecord(6, 3, 135, 1, d));
        purchases.add(new PurchaseRecord(7, 2, 110, 1, d));
        purchases.add(new PurchaseRecord(7, 2, 133, 1, d));

        saveUsers();
        saveProducts();
        savePurchases();
    }
}
