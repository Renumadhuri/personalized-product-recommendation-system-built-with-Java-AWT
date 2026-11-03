package model;

import java.util.ArrayList;

/**
 * INHERITANCE: a Customer IS-A User (it gets id, name, email, login() for free)
 * and adds customer-only things: registration date and purchase history.
 */
public class Customer extends User {

    private final String registeredOn;
    private final ArrayList<Purchase> purchaseHistory = new ArrayList<>();   // Customer HAS purchases

    public Customer(int id, String name, String email, String passwordHash, String registeredOn) {
        super(id, name, email, passwordHash);   // pass the shared fields to User
        this.registeredOn = registeredOn;
    }

    public String getRegisteredOn() { return registeredOn; }

    public void addPurchase(Purchase purchase) {
        purchaseHistory.add(purchase);
    }

    public ArrayList<Purchase> getPurchaseHistory() {
        return purchaseHistory;
    }

    public double getTotalSpent() {
        double sum = 0;
        for (Purchase p : purchaseHistory) {
            sum += p.getTotal();
        }
        return sum;
    }

    // POLYMORPHISM: each subclass fills in getRole() in its own way
    @Override
    public String getRole() {
        return "Customer";
    }

    @Override
    public String describe() {
        return super.describe() + " | purchases: " + purchaseHistory.size()
                + " | spent: Rs. " + String.format("%.2f", getTotalSpent());
    }
}
