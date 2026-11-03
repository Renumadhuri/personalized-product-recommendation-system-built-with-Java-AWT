import exceptions.*;
import interfaces.*;
import java.util.*;
import shop.*;
import validation.Validator;

public class Demo {

    static final ShopService shop = new ShopService();

    // Functional interface so a validation method can be passed as a parameter
    interface Check {
        void test(String input) throws ValidationException;
    }

    public static void main(String[] args) {
        try (ActivityLogger log = new ActivityLogger()) {
            ShopCustomer renu = demoInterfaces(log);
            demoRegistrationErrors(log);
            demoLogin(log);
            demoPurchases(log, renu);
            demoUncheckedException(log);
            demoInteractive(log);
        }
    }

    /* ---- 1. INTERFACES ---- */
    static ShopCustomer demoInterfaces(ActivityLogger log) {
        System.out.println("\n=== 1. INTERFACES & POLYMORPHISM ===");
        ShopCustomer renu;
        ShopProduct bear = new ShopProduct(101, "MINISO Cute Bear Plush Toy", 799.00, 35);
        ShopProduct notebook = new ShopProduct(110, "MINISO Notebook and Pen Set", 299.00, 45);
        try {
            renu = shop.register("Renu Madhuri", "renu@gmail.com", "Renu@123");
            shop.addProduct(bear);
            shop.addProduct(notebook);
            log.log("Setup complete");
        } catch (ValidationException e) {
            System.out.println("Setup failed: " + e.getMessage());
            return null;
        }

        List<Describable> things = new ArrayList<>();   // different classes, one interface
        things.add(renu);
        things.add(bear);
        things.add(notebook);
        for (Describable d : things) {
            d.printInfo();
        }

        Authenticatable a = renu;
        System.out.println("Role: " + a.getRole() + " | isAdmin: " + a.isAdmin()
                + " | login ok: " + a.login("Renu@123"));

        System.out.printf("Bear after 10%% discount: Rs. %.2f%n", bear.priceAfterDiscount(10));
        System.out.printf("GST on bear: Rs. %.2f%n", Discountable.gst(bear.getPrice()));
        try {
            bear.priceAfterDiscount(80);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        return renu;
    }

    /* ---- 2. CUSTOM EXCEPTIONS: registration ---- */
    static void demoRegistrationErrors(ActivityLogger log) {
        System.out.println("\n=== 2. REGISTRATION VALIDATION ===");
        String[][] tests = {
            {"R3nu", "renu2@gmail.com", "Renu@123"},           // bad name
            {"Rahul Verma", "rahul-at-gmail.com", "Rahul@654"},// bad email
            {"Kiran Das", "kiran@gmail.com", "abc"},           // weak password
            {"Renu Again", "renu@gmail.com", "Renu@123"},      // duplicate email
            {"Rahul Verma", "rahul@gmail.com", "Rahul@654"}    // valid
        };
        for (String[] t : tests) {
            try {
                ShopCustomer c = shop.register(t[0], t[1], t[2]);
                System.out.println("OK    -> " + c.describe());
            } catch (InvalidEmailException e) {                // most specific first
                System.out.println("EMAIL -> " + e.getMessage());
            } catch (WeakPasswordException e) {
                System.out.println("PASS  -> " + e.getMessage());
            } catch (ValidationException e) {                  // general one last
                System.out.println("FIELD(" + e.getField() + ") -> " + e.getMessage());
            }
        }
    }

    /* ---- 3. LOGIN with attempt limit ---- */
    static void demoLogin(ActivityLogger log) {
        System.out.println("\n=== 3. LOGIN ATTEMPTS ===");
        String[] passwords = {"wrong1", "wrong2", "wrong3", "Rahul@654"};
        for (String p : passwords) {
            try {
                ShopCustomer c = shop.login("rahul@gmail.com", p);
                System.out.println("Login success: " + c.getName());
            } catch (LoginFailedException e) {
                System.out.println(e.getMessage() + " Attempts left: " + e.getAttemptsLeft());
            }
        }
    }

    /* ---- 4. MULTI-CATCH + FINALLY ---- */
    static void demoPurchases(ActivityLogger log, ShopCustomer renu) {
        System.out.println("\n=== 4. PURCHASES ===");
        int[][] orders = {{101, 2}, {101, 500}, {999, 1}, {101, 0}};
        for (int[] o : orders) {
            try {
                double total = shop.purchase(renu, o[0], o[1]);
                System.out.printf("Order OK: product %d x%d = Rs. %.2f%n", o[0], o[1], total);
                log.log("Purchase recorded");
            } catch (InsufficientStockException | ProductNotFoundException e) {   // multi-catch
                System.out.println("Order failed: " + e.getMessage());
            } catch (ValidationException e) {
                System.out.println("Invalid input (" + e.getField() + "): " + e.getMessage());
            } finally {
                System.out.println("   (order attempt finished)");
            }
        }
    }

    /* ---- 5. UNCHECKED EXCEPTION + CHAINING ---- */
    static void demoUncheckedException(ActivityLogger log) {
        System.out.println("\n=== 5. UNCHECKED EXCEPTION & CHAINING ===");
        try {
            ShopProduct invalidProduct = new ShopProduct(500, "Free Gift", -10, 5);
            System.out.println("Unexpectedly created product: " + invalidProduct.getName());
        } catch (InvalidPriceException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        try {
            Validator.parseInt("twelve", "quantity");
        } catch (ValidationException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Original cause: " + e.getCause().getClass().getSimpleName());
        }
    }

    /* ---- 6. LIVE INPUT VALIDATION ---- */
    static void demoInteractive(ActivityLogger log) {
        System.out.println("\n=== 6. LIVE INPUT VALIDATION (try typing bad values) ===");
        Scanner sc = new Scanner(System.in);
        String name = ask(sc, "Full name: ", Validator::validateName);
        String email = ask(sc, "Email: ", Validator::validateEmail);
        String pass = ask(sc, "Password: ", Validator::validatePassword);
        try {
            ShopCustomer c = shop.register(name, email, pass);
            System.out.println("Registered: " + c.describe());
            log.log("New customer " + c.getId());
        } catch (ValidationException e) {
            System.out.println("Could not register: " + e.getMessage());
        }
    }

    // Keeps asking until the Check passes
    static String ask(Scanner sc, String prompt, Check check) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                check.test(input);
                return input;
            } catch (ValidationException e) {
                System.out.println("  [!] " + e.getMessage());
            }
        }
    }
}
