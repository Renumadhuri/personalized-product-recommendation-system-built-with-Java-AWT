import java.util.ArrayList;
import model.Category;
import model.Customer;
import model.Product;
import model.Purchase;
import model.Recommendation;
import model.RecommendationEngine;
import model.Staff;
import model.User;

public class modeldemo {

    public static void main(String[] args) {

        // ---- 1. USERS: one list holds different kinds of users (polymorphism) ----
        Staff admin = new Staff(1, "Admin", "admin@miniso.com", User.hash("admin123"), "Store Management");
        Customer renu = new Customer(1001, "Renu Madhuri", "renu@gmail.com", User.hash("Renu@123"), "24-09-2026");
        Customer rahul = new Customer(1005, "Rahul Verma", "rahul@gmail.com", User.hash("Rahul@654"), "10-09-2026");
        Customer newbie = new Customer(1007, "Kiran Das", "kiran@gmail.com", User.hash("kiran123"), "25-09-2026");

        ArrayList<User> users = new ArrayList<>();
        users.add(admin);
        users.add(renu);
        users.add(rahul);
        users.add(newbie);

        System.out.println("=== USERS ===");
        for (User u : users) {
            System.out.println(u.describe());     // each class prints in its own way
        }

        System.out.println("\nLogin test (renu, right password): " + renu.login("Renu@123"));
        System.out.println("Login test (renu, wrong password): " + renu.login("nope"));

        // ---- 2. CATEGORIES ----
        Category toys = new Category(1, "Toys & Plush");
        Category electronics = new Category(2, "Electronics");
        Category bags = new Category(3, "Bags & Accessories");
        Category home = new Category(4, "Home & Living");
        Category stationery = new Category(5, "Stationery");

        // ---- 3. PRODUCT REGISTRATION: done by a Staff member ----
        ArrayList<Product> products = new ArrayList<>();
        products.add(admin.registerProduct(101, "MINISO Cute Bear Plush Toy", toys, 799.00, 35, "01-09-2026"));
        products.add(admin.registerProduct(103, "MINISO Wireless Bluetooth Earphones", electronics, 1299.00, 18, "03-09-2026"));
        products.add(admin.registerProduct(104, "MINISO Mini Shoulder Bag", bags, 999.00, 22, "04-09-2026"));
        products.add(admin.registerProduct(105, "MINISO Scented Candle - Vanilla", home, 499.00, 30, "05-09-2026"));
        products.add(admin.registerProduct(107, "MINISO Face Towels - Pack of 3", home, 349.00, 50, "07-09-2026"));
        products.add(admin.registerProduct(110, "MINISO Notebook and Pen Set", stationery, 299.00, 45, "10-09-2026"));
        products.add(admin.registerProduct(112, "MINISO LED Night Lamp", home, 749.00, 24, "12-09-2026"));
        products.add(admin.registerProduct(113, "MINISO Foldable Shopping Bag", bags, 249.00, 55, "13-09-2026"));

        System.out.println("\n=== PRODUCTS ===");
        for (Product p : products) {
            System.out.println(p);
        }

        // ---- 4. PURCHASES: stored inside each customer's purchase history ----
        renu.addPurchase(new Purchase(6001, renu.getId(), products.get(0), 1, "24-09-2026 20:48"));
        renu.addPurchase(new Purchase(6002, renu.getId(), products.get(5), 2, "23-09-2026 18:20"));

        rahul.addPurchase(new Purchase(6013, rahul.getId(), products.get(2), 1, "23-09-2026 16:10"));
        rahul.addPurchase(new Purchase(6014, rahul.getId(), products.get(6), 1, "20-09-2026 20:15"));
        rahul.addPurchase(new Purchase(6015, rahul.getId(), products.get(4), 2, "15-09-2026 09:40"));

        System.out.println("\n=== PURCHASE HISTORY: " + rahul.getName() + " ===");
        for (Purchase p : rahul.getPurchaseHistory()) {
            System.out.println(p);
        }
        System.out.printf("Total spent: Rs. %.2f%n", rahul.getTotalSpent());

        // ---- 5. RECOMMENDATIONS ----
        RecommendationEngine engine = new RecommendationEngine();

        System.out.println("\n=== RECOMMENDATIONS FOR " + rahul.getName() + " ===");
        for (Recommendation r : engine.recommendFor(rahul, products, 3)) {
            System.out.println(r);
        }

        System.out.println("\n=== RECOMMENDATIONS FOR " + newbie.getName() + " (no purchases yet) ===");
        for (Recommendation r : engine.recommendFor(newbie, products, 3)) {
            System.out.println(r);
        }
    }
}