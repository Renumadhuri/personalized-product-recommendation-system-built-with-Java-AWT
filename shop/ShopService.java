package shop;

import exceptions.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import validation.Validator;

public class ShopService {
    private static final int MAX_ATTEMPTS = 3;

    private final List<ShopCustomer> customers = new ArrayList<>();
    private final List<ShopProduct> products = new ArrayList<>();
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private int nextId = 1001;

    public ShopCustomer register(String name, String email, String password)
            throws ValidationException {
        Validator.validateName(name);
        Validator.validateEmail(email);
        Validator.validatePassword(password);
        if (findByEmail(email) != null) {
            throw new ValidationException("email", "Email already registered: " + email);
        }
        ShopCustomer c = new ShopCustomer(nextId++, name.trim(), email.trim().toLowerCase(), password);
        c.validate();
        customers.add(c);
        return c;
    }

    public ShopCustomer login(String email, String password) throws LoginFailedException {
        ShopCustomer c = findByEmail(email);
        if (c == null) {
            throw new LoginFailedException("No account found for " + email, 0);
        }
        int fails = failedAttempts.getOrDefault(c.getEmail(), 0);
        if (fails >= MAX_ATTEMPTS) {
            throw new LoginFailedException("Account locked after " + MAX_ATTEMPTS
                    + " failed attempts.", 0);
        }
        if (!c.login(password)) {
            fails++;
            failedAttempts.put(c.getEmail(), fails);
            throw new LoginFailedException("Wrong password.", MAX_ATTEMPTS - fails);
        }
        failedAttempts.remove(c.getEmail());
        return c;
    }
        public void addProduct(ShopProduct p) throws ValidationException {
        p.validate();
        products.add(p);
    }


    public ShopProduct findProduct(int id) throws ProductNotFoundException {
        for (ShopProduct p : products) {
            if (p.getId() == id) {
                return p;
            }
        }
        throw new ProductNotFoundException(id);
    }

    public double purchase(ShopCustomer c, int productId, int qty)
            throws ValidationException, ProductNotFoundException, InsufficientStockException {
        Validator.validateQuantity(qty);
        ShopProduct p = findProduct(productId);
        p.reduceStock(qty);
        return p.getPrice() * qty;
    }

    private ShopCustomer findByEmail(String email) {
        if (email == null) {
            return null;
        }
        for (ShopCustomer c : customers) {
            if (c.getEmail().equalsIgnoreCase(email.trim())) {
                return c;
            }
        }
        return null;
    }
}
