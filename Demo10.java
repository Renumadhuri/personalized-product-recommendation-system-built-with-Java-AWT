import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;
import storage.CsvStore;
import storage.RecommendationHistory;
import storage.StoreItem;

public class Demo10 {

    static final Scanner sc = new Scanner(System.in);

    static ArrayList<StoreItem> items = new ArrayList<>();                       // ArrayList
    static HashMap<Integer, StoreItem> itemById = new HashMap<>();               // HashMap
    static Stack<StoreItem> recentlyViewed = new Stack<>();                      // Stack
    static ArrayList<StoreItem> recommendations = new ArrayList<>();             // ArrayList
    static RecommendationHistory history = new RecommendationHistory();          // HashMap inside

    public static void main(String[] args) {
        CsvStore.ensureDataFolder();
        loadData();

        int choice;
        do {
            printMenu();
            choice = readInt("Choose an option: ");
            System.out.println();
            switch (choice) {
                case 1 -> showAllProducts();
                case 2 -> searchById();
                case 3 -> showRecentlyViewed();
                case 4 -> undoLastView();
                case 5 -> recommend();
                case 6 -> showHistory();
                case 7 -> addProduct();
                case 8 -> sortByPrice();
                case 9 -> exportCsv();
                case 0 -> System.out.println("Goodbye!");
                default -> System.out.println("  [!] Please choose a number from 0 to 9.");
            }
        } while (choice != 0);
    }

    // ---------------- start-up ----------------

    static void loadData() {
        items = CsvStore.loadItems();
        if (items.isEmpty()) {
            System.out.println("No product file found. Creating default MINISO products...");
            items.add(new StoreItem(101, "MINISO Cute Bear Plush Toy", "Toys", 799.00, 35));
            items.add(new StoreItem(102, "MINISO Panda Plush Toy", "Toys", 649.00, 20));
            items.add(new StoreItem(103, "MINISO Building Blocks Set", "Toys", 499.00, 15));
            items.add(new StoreItem(110, "MINISO Notebook and Pen Set", "Stationery", 299.00, 45));
            items.add(new StoreItem(111, "MINISO Gel Pen Pack of 10", "Stationery", 149.00, 80));
            items.add(new StoreItem(120, "MINISO Wireless Earbuds", "Electronics", 1299.00, 25));
            items.add(new StoreItem(121, "MINISO Phone Stand", "Electronics", 349.00, 40));
            items.add(new StoreItem(130, "MINISO Scented Candle", "Home", 399.00, 30));
            CsvStore.saveItems(items);
        }
        rebuildIndex();
        history.load();
        System.out.println("Loaded " + items.size() + " products.");
    }

    /** Rebuilds the HashMap so we can find a product by its ID instantly. */
    static void rebuildIndex() {
        itemById.clear();
        for (StoreItem it : items) {
            itemById.put(it.getId(), it);
        }
    }

    static void printMenu() {
        System.out.println();
        System.out.println("========== WEEK 10 : MINISO COLLECTIONS MENU ==========");
        System.out.println(" 1. Show all products            (ArrayList)");
        System.out.println(" 2. Search product by ID         (HashMap + Stack push)");
        System.out.println(" 3. Show recently viewed         (Stack)");
        System.out.println(" 4. Undo last view               (Stack pop)");
        System.out.println(" 5. Recommend for a customer     (Vector + history file)");
        System.out.println(" 6. Show recommendation history  (HashMap)");
        System.out.println(" 7. Add a new product            (ArrayList + file save)");
        System.out.println(" 8. Show products sorted by price(Collections.sort)");
        System.out.println(" 9. Export history to CSV        (file handling)");
        System.out.println(" 0. Exit");
        System.out.println("=======================================================");
    }

    // ---------------- menu actions ----------------

    static void showAllProducts() {
        System.out.println("--- All products (" + items.size() + ") ---");
        for (StoreItem it : items) {
            System.out.println(it);
        }
    }

    static void searchById() {
        int id = readInt("Enter product ID: ");
        StoreItem found = itemById.get(id);                 // HashMap lookup
        if (found == null) {
            System.out.println("  [!] No product found with ID " + id);
            return;
        }
        System.out.println("Found: " + found);
        recentlyViewed.push(found);                         // Stack push
        System.out.println("(added to recently viewed)");
    }

    static void showRecentlyViewed() {
        if (recentlyViewed.isEmpty()) {
            System.out.println("Nothing viewed yet. Use option 2 first.");
            return;
        }
        System.out.println("--- Recently viewed (newest first) ---");
        for (int i = recentlyViewed.size() - 1; i >= 0; i--) {
            System.out.println(recentlyViewed.get(i).getName());
        }
        System.out.println("Top of stack (peek): " + recentlyViewed.peek().getName());
    }

    static void undoLastView() {
        if (recentlyViewed.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        StoreItem removed = recentlyViewed.pop();           // Stack pop
        System.out.println("Removed from recently viewed: " + removed.getName());
    }

    static void recommend() {
        if (recentlyViewed.isEmpty()) {
            System.out.println("View a product first (option 2) so I know what the customer likes.");
            return;
        }
        int customerId = readInt("Customer ID: ");
        StoreItem last = recentlyViewed.peek();

        // Step 1: same category, different product, in stock
        recommendations.clear();
        for (StoreItem it : items) {
            if (it.getId() != last.getId()
                    && it.getCategory().equalsIgnoreCase(last.getCategory())
                    && it.getStock() > 0) {
                recommendations.add(it);
            }
        }
        // Step 2: if the category has nothing else, suggest any other in-stock products
        if (recommendations.isEmpty()) {
            for (StoreItem it : items) {
                if (it.getId() != last.getId() && it.getStock() > 0) {
                    recommendations.add(it);
                }
            }
        }
        // Step 3: keep only the first 3
        while (recommendations.size() > 3) {
            recommendations.remove(recommendations.size() - 1);
        }

        System.out.println("Because customer " + customerId + " viewed '" + last.getName() + "', we recommend:");
        for (StoreItem it : recommendations) {
            System.out.println("  -> " + it.getName() + "  (Rs. " + it.getPrice() + ")");
            history.add(customerId, it.getName());          // saves to memory and file
        }
        System.out.println("(saved to recommendation history)");
    }

    static void showHistory() {
        int customerId = readInt("Customer ID: ");
        ArrayList<String> list = history.get(customerId);
        if (list.isEmpty()) {
            System.out.println("No recommendation history for customer " + customerId);
            return;
        }
        System.out.println("--- History for customer " + customerId + " ---");
        int n = 1;
        for (String entry : list) {
            String[] parts = entry.split(",", 2);           // date_time , product
            System.out.println(n++ + ". " + parts[0] + "  ->  " + parts[1]);
        }
    }

    static void addProduct() {
        int id = readInt("New product ID: ");
        if (itemById.containsKey(id)) {
            System.out.println("  [!] ID " + id + " already exists.");
            return;
        }
        System.out.print("Name: ");
        String name = sc.nextLine().trim().replace(",", " ");
        System.out.print("Category: ");
        String category = sc.nextLine().trim().replace(",", " ");
        double price = readDouble("Price (Rs.): ");
        int stock = readInt("Stock quantity: ");

        if (name.isEmpty() || category.isEmpty() || price <= 0 || stock < 0) {
            System.out.println("  [!] Invalid values. Product not added.");
            return;
        }
        StoreItem item = new StoreItem(id, name, category, price, stock);
        items.add(item);
        itemById.put(id, item);
        CsvStore.saveItems(items);
        System.out.println("Added and saved: " + item);
    }

    static void sortByPrice() {
        ArrayList<StoreItem> copy = new ArrayList<>(items);
        Collections.sort(copy, Comparator.comparingDouble(StoreItem::getPrice));
        System.out.println("--- Products sorted by price (low to high) ---");
        for (StoreItem it : copy) {
            System.out.println(it);
        }
    }

    static void exportCsv() {
        int rows = CsvStore.exportHistory(history.getAll());
        if (rows < 0) {
            System.out.println("Export failed.");
        } else {
            System.out.println("Exported " + rows + " rows to " + CsvStore.EXPORT_FILE);
            System.out.println("Open this file in Excel to see the report.");
        }
    }

    // ---------------- safe input helpers ----------------

    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("  [!] '" + s + "' is not a whole number. Try again.");
            }
        }
    }

    static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("  [!] '" + s + "' is not a number. Try again.");
            }
        }
    }
}
