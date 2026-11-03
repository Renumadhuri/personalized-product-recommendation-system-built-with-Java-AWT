import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


public class Login_Module {

    static Scanner sc = new Scanner(System.in);

    // In-memory lists (loaded from files at startup, saved after every change)
    static ArrayList<Customer> customers = new ArrayList<>();
    static ArrayList<Product> products = new ArrayList<>();
    static ArrayList<Purchase> purchases = new ArrayList<>();

    static int nextCustomerId = 1;
    static int nextProductId = 1;
    static int nextPurchaseId = 1;

    // Staff / administrator login details (change these if you like)
    static final String STAFF_USER = "admin";
    static final String STAFF_PASS = "admin123";
    static final int MAX_ATTEMPTS = 3;

    public static void main(String[] args) {
        FileManager.ensureFolders();
        loadAll();
        System.out.println("Loaded " + customers.size() + " customers, "
                + products.size() + " products, "
                + purchases.size() + " purchases.");

        boolean running = true;

        while (running) {
            System.out.println("\n========== WELCOME ==========");
            System.out.println("Are you a:");
            System.out.println("1. Customer");
            System.out.println("2. Staff");
            System.out.println("3. Exit");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 ->
                    customerMenu();
                case 2 ->
                    staffLogin();
                case 3 -> {
                    running = false;
                    BackupManager.createBackup(true);
                    System.out.println("Goodbye!");
                }
                default ->
                    System.out.println("Invalid choice. Please enter 1, 2 or 3.");
            }
        }
    }

    /* ---------- LOAD DATA AND WORK OUT NEXT IDs ---------- */

    static void loadAll() {
        customers = FileManager.loadCustomers();
        products = FileManager.loadProducts();
        purchases = FileManager.loadPurchases();

        nextCustomerId = 1;
        for (Customer c : customers) {
            if (c.getId() >= nextCustomerId) {
                nextCustomerId = c.getId() + 1;
            }
        }
        nextProductId = 1;
        for (Product p : products) {
            if (p.getId() >= nextProductId) {
                nextProductId = p.getId() + 1;
            }
        }
        nextPurchaseId = 1;
        for (Purchase p : purchases) {
            if (p.getPurchaseId() >= nextPurchaseId) {
                nextPurchaseId = p.getPurchaseId() + 1;
            }
        }
    }

    /* ---------- INPUT HELPERS ---------- */

    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                double value = Double.parseDouble(line);
                if (Double.isFinite(value)) {
                    return value;
                }
            } catch (NumberFormatException e) {
            }
            System.out.println("Please enter a valid finite number.");
        }
    }

    static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    /* ---------- SMALL UTILITIES ---------- */

    static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    static String today() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    // Turns a password into a SHA-256 hash so the real password is never saved
    static String hash(String text) {
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

    /* ---------- CUSTOMER SIDE ---------- */

    static void customerMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- CUSTOMER ---");
            System.out.println("1. Login");
            System.out.println("2. Sign up (new account)");
            System.out.println("3. Back");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> {
                    Customer c = loginCustomer();
                    if (c != null) {
                        customerDashboard(c);
                    }
                }
                case 2 ->
                    registerCustomer();
                case 3 ->
                    back = true;
                default ->
                    System.out.println("Invalid choice.");
            }
        }
    }

    static void registerCustomer() {
        System.out.println("\n--- CUSTOMER REGISTRATION ---");
        String name = FileManager.clean(readText("Full name: "));

        String email;
        while (true) {
            email = readText("Email: ").toLowerCase();
            if (!email.contains("@") || !email.contains(".")
                    || email.contains(",") || email.contains(" ")) {
                System.out.println("Please enter a valid email address (no spaces or commas).");
            } else if (findCustomerByEmail(email) != null) {
                System.out.println("This email is already registered. Please log in instead.");
                return;
            } else {
                break;
            }
        }

        String password;
        while (true) {
            password = readText("Password (minimum 4 characters): ");
            if (password.length() >= 4) {
                break;
            }
            System.out.println("Password is too short.");
        }

        Customer c = new Customer(nextCustomerId++, name, email, hash(password), today());
        customers.add(c);
        FileManager.saveCustomers(customers);   // <-- saved to file
        System.out.println("Account created! Your customer ID is " + c.getId()
                + ". You can now log in.");
    }

    static Customer loginCustomer() {
        System.out.println("\n--- CUSTOMER LOGIN ---");

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String email = readText("Email: ").toLowerCase();
            String password = readText("Password: ");

            Customer c = findCustomerByEmail(email);
            if (c != null && c.matchesHash(hash(password))) {
                System.out.println("Login successful. Welcome, " + c.getName() + "!");
                return c;
            }
            System.out.println("Wrong email or password. Attempts left: "
                    + (MAX_ATTEMPTS - attempt));
        }

        System.out.println("Too many failed attempts. Returning to menu.");
        return null;
    }

    static void customerDashboard(Customer c) {
        boolean logout = false;

        while (!logout) {
            System.out.println("\n--- CUSTOMER DASHBOARD (" + c.getName() + ") ---");
            System.out.println("1. View products");
            System.out.println("2. Buy a product");
            System.out.println("3. My purchase history");
            System.out.println("4. Logout");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 ->
                    viewProducts();
                case 2 ->
                    buyProduct(c);
                case 3 -> {
                    System.out.println("\n--- PURCHASE HISTORY: " + c.getName() + " ---");
                    printPurchases(c.getId());
                }
                case 4 -> {
                    logout = true;
                    System.out.println("Logged out.");
                }
                default ->
                    System.out.println("Invalid choice.");
            }
        }
    }

    static void buyProduct(Customer c) {
        if (products.isEmpty()) {
            System.out.println("No products are available yet.");
            return;
        }

        viewProducts();
        int id = readInt("Enter product ID to buy (0 to cancel): ");
        if (id == 0) {
            return;
        }

        Product p = findProductById(id);
        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

        int qty = readInt("Quantity: ");
        if (qty <= 0) {
            System.out.println("Quantity must be at least 1.");
            return;
        }
        if (qty > p.getStock()) {
            System.out.println("Sorry, only " + p.getStock() + " in stock.");
            return;
        }

        double total = Math.round(qty * p.getPrice() * 100.0) / 100.0;
        p.reduceStock(qty);
        Purchase purchase = new Purchase(nextPurchaseId++, c.getId(), p.getId(),
                p.getName(), qty, p.getPrice(), total, now());
        purchases.add(purchase);

        FileManager.saveProducts(products);     // <-- stock changed
        FileManager.savePurchases(purchases);   // <-- new purchase record

        System.out.printf("Purchase successful! You bought %d x %s. Total: Rs. %.2f%n",
                qty, p.getName(), total);
    }

    // Purchase history = all rows in purchases.csv that belong to this customer
    static void printPurchases(int customerId) {
        double grandTotal = 0;
        int count = 0;

        for (Purchase p : purchases) {
            if (p.getCustomerId() != customerId) {
                continue;
            }
            if (count == 0) {
                System.out.printf("%-4s %-20s %-5s %-12s %-12s %s%n",
                        "ID", "Product", "Qty", "Unit Price", "Total", "Date");
            }
            System.out.printf("%-4d %-20s %-5d %-12.2f %-12.2f %s%n",
                    p.getPurchaseId(), p.getProductName(), p.getQuantity(),
                    p.getUnitPrice(), p.getTotal(), p.getDateTime());
            grandTotal += p.getTotal();
            count++;
        }

        if (count == 0) {
            System.out.println("No purchases yet.");
        } else {
            System.out.printf("Total spent: Rs. %.2f%n", grandTotal);
        }
    }

    /* ---------- STAFF / ADMINISTRATOR SIDE ---------- */

    static void staffLogin() {
        System.out.println("\n--- STAFF LOGIN ---");

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String user = readText("Username: ");
            String pass = readText("Password: ");

            if (user.equals(STAFF_USER) && pass.equals(STAFF_PASS)) {
                System.out.println("Login successful. Welcome, administrator!");
                staffDashboard();
                return;
            }
            System.out.println("Wrong username or password. Attempts left: "
                    + (MAX_ATTEMPTS - attempt));
        }

        System.out.println("Too many failed attempts. Returning to menu.");
    }

    static void staffDashboard() {
        boolean logout = false;

        while (!logout) {
            System.out.println("\n--- STAFF DASHBOARD ---");
            System.out.println("1. Register a new product");
            System.out.println("2. View products");
            System.out.println("3. View all customers");
            System.out.println("4. View all purchase history");
            System.out.println("5. Create backup");
            System.out.println("6. Restore from backup");
            System.out.println("7. Logout");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 ->
                    registerProduct();
                case 2 ->
                    viewProducts();
                case 3 ->
                    viewAllCustomers();
                case 4 ->
                    viewAllPurchases();
                case 5 ->
                    BackupManager.createBackup(true);
                case 6 ->
                    restoreBackup();
                case 7 -> {
                    logout = true;
                    System.out.println("Logged out.");
                }
                default ->
                    System.out.println("Invalid choice.");
            }
        }
    }

    static void registerProduct() {
        System.out.println("\n--- PRODUCT REGISTRATION ---");
        String name = FileManager.clean(readText("Product name: "));

        if (findProductByName(name) != null) {
            System.out.println("A product with this name already exists.");
            return;
        }

        String category = FileManager.clean(readText("Category: "));

        double price;
        while (true) {
            price = readDouble("Price (Rs.): ");
            if (price > 0) {
                break;
            }
            System.out.println("Price must be greater than 0.");
        }

        int stock;
        while (true) {
            stock = readInt("Stock quantity: ");
            if (stock >= 0) {
                break;
            }
            System.out.println("Stock cannot be negative.");
        }

        Product p = new Product(nextProductId++, name, category, price, stock, today());
        products.add(p);
        FileManager.saveProducts(products);     // <-- saved to file
        System.out.println("Product registered with ID " + p.getId() + ".");
    }

    static void viewAllCustomers() {
        System.out.println("\n--- REGISTERED CUSTOMERS ---");
        if (customers.isEmpty()) {
            System.out.println("No customers registered yet.");
            return;
        }

        System.out.printf("%-5s %-20s %-30s %s%n", "ID", "Name", "Email", "Registered");
        for (Customer c : customers) {
            System.out.printf("%-5d %-20s %-30s %s%n",
                    c.getId(), c.getName(), c.getEmail(), c.getRegisteredOn());
        }
    }

    static void viewAllPurchases() {
        System.out.println("\n--- ALL PURCHASE HISTORY ---");
        if (customers.isEmpty()) {
            System.out.println("No customers registered yet.");
            return;
        }

        for (Customer c : customers) {
            System.out.println("\nCustomer: " + c.getName() + " (ID " + c.getId() + ")");
            printPurchases(c.getId());
        }
    }

    static void restoreBackup() {
        ArrayList<String> backups = BackupManager.listBackups();
        if (backups.isEmpty()) {
            System.out.println("No backups found.");
            return;
        }

        System.out.println("\n--- AVAILABLE BACKUPS (oldest first) ---");
        for (int i = 0; i < backups.size(); i++) {
            System.out.println((i + 1) + ". " + backups.get(i));
        }

        int choice = readInt("Enter backup number to restore (0 to cancel): ");
        if (choice == 0) {
            return;
        }
        if (choice < 0 || choice > backups.size()) {
            System.out.println("Invalid backup number.");
            return;
        }

        String confirm = readText("This replaces the current data. Type YES to continue: ");
        if (!confirm.equals("YES")) {
            System.out.println("Restore cancelled.");
            return;
        }

        BackupManager.createBackup(false);   // safety copy of the current data first
        if (BackupManager.restore(backups.get(choice - 1))) {
            loadAll();
            System.out.println("Data restored. Loaded " + customers.size() + " customers, "
                    + products.size() + " products, " + purchases.size() + " purchases.");
        }
    }

    /* ---------- SHARED HELPERS ---------- */

    static void viewProducts() {
        System.out.println("\n--- PRODUCT LIST ---");
        if (products.isEmpty()) {
            System.out.println("No products registered yet.");
            return;
        }

        System.out.printf("%-5s %-20s %-15s %-10s %s%n",
                "ID", "Name", "Category", "Price", "Stock");
        for (Product p : products) {
            System.out.printf("%-5d %-20s %-15s %-10.2f %d%n",
                    p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock());
        }
    }

    static Customer findCustomerByEmail(String email) {
        for (Customer c : customers) {
            if (c.getEmail().equals(email)) {
                return c;
            }
        }
        return null;
    }

    static Product findProductById(int id) {
        for (Product p : products) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    static Product findProductByName(String name) {
        for (Product p : products) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
}

/* ===================== DATA CLASSES ===================== */

class Customer {
    private final int id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final String registeredOn;

    public Customer(int id, String name, String email, String passwordHash, String registeredOn) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.registeredOn = registeredOn;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRegisteredOn() { return registeredOn; }

    public boolean matchesHash(String hashToCheck) {
        return passwordHash.equals(hashToCheck);
    }
}

class Product {
    private final int id;
    private final String name;
    private final String category;
    private final double price;
    private int stock;
    private final String addedOn;

    public Product(int id, String name, String category, double price, int stock, String addedOn) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.addedOn = addedOn;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getAddedOn() { return addedOn; }

    public void reduceStock(int qty) {
        stock -= qty;
    }
}

class Purchase {
    private final int purchaseId;
    private final int customerId;
    private final int productId;
    private final String productName;
    private final int quantity;
    private final double unitPrice;
    private final double total;
    private final String dateTime;

    public Purchase(int purchaseId, int customerId, int productId, String productName,
                    int quantity, double unitPrice, double total, String dateTime) {
        this.purchaseId = purchaseId;
        this.customerId = customerId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
        this.dateTime = dateTime;
    }

    public int getPurchaseId() { return purchaseId; }
    public int getCustomerId() { return customerId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotal() { return total; }
    public String getDateTime() { return dateTime; }
}

/* ===================== FILE MANAGER ===================== */
/* Reads and writes the CSV files inside the data/ folder.    */

class FileManager {
    static final String DATA_DIR = "data";
    static final String BACKUP_DIR = "backup";
    static final String CUSTOMERS = "customers.csv";
    static final String PRODUCTS = "products.csv";
    static final String PURCHASES = "purchases.csv";

    // Creates the data/ and backup/ folders if they do not exist yet
    static void ensureFolders() {
        new File(DATA_DIR).mkdirs();
        new File(BACKUP_DIR).mkdirs();
    }

    // Commas and line breaks would break the CSV format, so replace them with spaces
    static String clean(String text) {
        return text.replace(",", " ").replace("\n", " ").replace("\r", " ").trim();
    }

    private static List<String> readLines(String fileName) {
        Path p = Paths.get(DATA_DIR, fileName);
        if (!Files.exists(p)) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Could not read " + fileName + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Writes to a temporary file first, then swaps it in, so a crash
    // half-way through never leaves a broken data file behind
    private static void writeLines(String fileName, List<String> lines) {
        Path target = Paths.get(DATA_DIR, fileName);
        Path temp = Paths.get(DATA_DIR, fileName + ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("Could not save " + fileName + ": " + e.getMessage()
                    + " (is the file open in another program?)");
        }
    }

    /* ----- customers.csv ----- */

    static ArrayList<Customer> loadCustomers() {
        ArrayList<Customer> list = new ArrayList<>();
        List<String> lines = readLines(CUSTOMERS);
        for (int i = 1; i < lines.size(); i++) {          // line 0 is the header
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split(",", -1);
            try {
                list.add(new Customer(Integer.parseInt(f[0]), f[1], f[2], f[3], f[4]));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.out.println("Skipped a bad line in " + CUSTOMERS + ": " + line);
            }
        }
        return list;
    }

    static void saveCustomers(ArrayList<Customer> list) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("customerId,name,email,passwordHash,registeredOn");
        for (Customer c : list) {
            lines.add(c.getId() + "," + c.getName() + "," + c.getEmail() + ","
                    + c.getPasswordHash() + "," + c.getRegisteredOn());
        }
        writeLines(CUSTOMERS, lines);
    }

    /* ----- products.csv ----- */

    static ArrayList<Product> loadProducts() {
        ArrayList<Product> list = new ArrayList<>();
        List<String> lines = readLines(PRODUCTS);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split(",", -1);
            try {
                list.add(new Product(Integer.parseInt(f[0]), f[1], f[2],
                        Double.parseDouble(f[3]), Integer.parseInt(f[4]), f[5]));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.out.println("Skipped a bad line in " + PRODUCTS + ": " + line);
            }
        }
        return list;
    }

    static void saveProducts(ArrayList<Product> list) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("productId,name,category,price,stock,addedOn");
        for (Product p : list) {
            lines.add(p.getId() + "," + p.getName() + "," + p.getCategory() + ","
                    + String.format(Locale.US, "%.2f", p.getPrice()) + ","
                    + p.getStock() + "," + p.getAddedOn());
        }
        writeLines(PRODUCTS, lines);
    }

    /* ----- purchases.csv ----- */

    static ArrayList<Purchase> loadPurchases() {
        ArrayList<Purchase> list = new ArrayList<>();
        List<String> lines = readLines(PURCHASES);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split(",", -1);
            try {
                list.add(new Purchase(Integer.parseInt(f[0]), Integer.parseInt(f[1]),
                        Integer.parseInt(f[2]), f[3], Integer.parseInt(f[4]),
                        Double.parseDouble(f[5]), Double.parseDouble(f[6]), f[7]));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.out.println("Skipped a bad line in " + PURCHASES + ": " + line);
            }
        }
        return list;
    }

    static void savePurchases(ArrayList<Purchase> list) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("purchaseId,customerId,productId,productName,quantity,unitPrice,total,dateTime");
        for (Purchase p : list) {
            lines.add(p.getPurchaseId() + "," + p.getCustomerId() + "," + p.getProductId() + ","
                    + p.getProductName() + "," + p.getQuantity() + ","
                    + String.format(Locale.US, "%.2f", p.getUnitPrice()) + ","
                    + String.format(Locale.US, "%.2f", p.getTotal()) + ","
                    + p.getDateTime());
        }
        writeLines(PURCHASES, lines);
    }
}

/* ===================== BACKUP MANAGER ===================== */
/* Copies the data files into backup/<date_time>/ and restores them. */

class BackupManager {
    static final int KEEP_LAST = 5;
    static final String[] FILES = {
            FileManager.CUSTOMERS, FileManager.PRODUCTS, FileManager.PURCHASES
    };

    static void createBackup(boolean deleteOld) {
        // Only make a backup folder if there is at least one data file to copy
        boolean anyFile = false;
        for (String f : FILES) {
            if (Files.exists(Paths.get(FileManager.DATA_DIR, f))) {
                anyFile = true;
            }
        }
        if (!anyFile) {
            System.out.println("Nothing to back up yet.");
            return;
        }

        String stamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path backupPath = Paths.get(FileManager.BACKUP_DIR, stamp);

        try {
            Files.createDirectories(backupPath);
            for (String f : FILES) {
                Path source = Paths.get(FileManager.DATA_DIR, f);
                if (Files.exists(source)) {
                    Files.copy(source, backupPath.resolve(f), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println("Backup created: " + backupPath);
        } catch (IOException e) {
            System.out.println("Backup failed: " + e.getMessage());
            return;
        }

        if (deleteOld) {
            deleteOldBackups();
        }
    }

    // Returns backup folder names, oldest first (the names are date/time, so sorting works)
    static ArrayList<String> listBackups() {
        ArrayList<String> names = new ArrayList<>();
        File[] items = new File(FileManager.BACKUP_DIR).listFiles();
        if (items != null) {
            for (File item : items) {
                if (item.isDirectory()) {
                    names.add(item.getName());
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    static void deleteOldBackups() {
        ArrayList<String> names = listBackups();
        while (names.size() > KEEP_LAST) {
            String oldest = names.remove(0);
            File folder = new File(FileManager.BACKUP_DIR, oldest);
            File[] inside = folder.listFiles();
            if (inside != null) {
                for (File f : inside) {
                    f.delete();
                }
            }
            folder.delete();
            System.out.println("Deleted old backup: " + oldest);
        }
    }

    static boolean restore(String backupName) {
        Path backupPath = Paths.get(FileManager.BACKUP_DIR, backupName);
        try {
            for (String f : FILES) {
                Path source = backupPath.resolve(f);
                if (Files.exists(source)) {
                    Files.copy(source, Paths.get(FileManager.DATA_DIR, f),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return true;
        } catch (IOException e) {
            System.out.println("Restore failed: " + e.getMessage());
            return false;
        }
    }
}