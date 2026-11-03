package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Creates Recommendation objects.
 * Rule: suggest in-stock products from the categories the customer buys most,
 * skipping anything they have already bought.
 */
public class RecommendationEngine {

    private int nextRecId = 1;

    public ArrayList<Recommendation> recommendFor(Customer customer,
                                                  ArrayList<Product> allProducts, int max) {
        ArrayList<Recommendation> result = new ArrayList<>();
        ArrayList<Purchase> history = customer.getPurchaseHistory();

        // New customer with no purchases: suggest a few in-stock products
        if (history.isEmpty()) {
            for (Product p : allProducts) {
                if (result.size() >= max) {
                    break;
                }
                if (p.isInStock()) {
                    result.add(newRecommendation(customer, p, "Starter suggestion for new customers"));
                }
            }
            return result;
        }

        // 1. Count how many purchases the customer made in each category
        HashMap<Category, Integer> counts = new HashMap<>();
        for (Purchase p : history) {
            Category c = p.getProduct().getCategory();
            counts.put(c, counts.getOrDefault(c, 0) + 1);
        }

        // 2. Put the categories in order: most-bought first
        ArrayList<Category> ranked = new ArrayList<>(counts.keySet());
        ranked.sort((a, b) -> counts.get(b) - counts.get(a));

        // 3. Suggest unbought, in-stock products from those categories
        for (Category c : ranked) {
            for (Product p : allProducts) {
                if (result.size() >= max) {
                    return result;
                }
                if (p.getCategory().equals(c) && p.isInStock() && !hasBought(history, p)) {
                    result.add(newRecommendation(customer, p,
                            "Because you bought " + c.getName() + " items"));
                }
            }
        }
        return result;
    }

    private boolean hasBought(ArrayList<Purchase> history, Product product) {
        for (Purchase p : history) {
            if (p.getProduct().getId() == product.getId()) {
                return true;
            }
        }
        return false;
    }

    private Recommendation newRecommendation(Customer customer, Product product, String reason) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
        return new Recommendation(nextRecId++, customer.getId(), product, reason, now);
    }
}
