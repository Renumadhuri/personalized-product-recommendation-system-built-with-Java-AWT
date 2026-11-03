package gui;

import app.AppProduct;
import app.AppUser;
import app.DataStore;
import app.PurchaseRecord;
import engine.RecommendationEngine;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/** Browse and search products, see "bought together" (basket analysis), fill a basket, check out. */
public class ProductsPanel extends Panel {
    private final DataStore store;
    private final AppUser user;
    private final RecommendationEngine engine;
    private final Frame owner;

    private final TextField search = new TextField(14);
    private final Choice category = new Choice();
    private final java.awt.List list = new java.awt.List(12);
    private final TextArea details = new TextArea("", 8, 30, TextArea.SCROLLBARS_VERTICAL_ONLY);
    private final TextField qty = new TextField("1", 3);
    private final java.awt.List basketList = new java.awt.List(4);
    private final Label total = new Label("Total: Rs 0        ");
    private final ArrayList<AppProduct> shown = new ArrayList<>();
    private final Map<Integer, Integer> basket = new LinkedHashMap<>();
    private final Button addBtn = new Button("Add to Basket");
    private final Button removeBtn = new Button("Remove");
    private final Button checkoutBtn = new Button("Checkout");

    public ProductsPanel(DataStore store, AppUser user, RecommendationEngine engine, Frame owner) {
        this.store = store;
        this.user = user;
        this.engine = engine;
        this.owner = owner;
        setLayout(new BorderLayout(8, 8));
        details.setEditable(false);

        // top: search bar
        Panel top = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        Button searchBtn = new Button("Search");
        Button resetBtn = new Button("Reset");
        top.add(new Label("Search:"));
        top.add(search);
        top.add(new Label("Category:"));
        top.add(category);
        top.add(searchBtn);
        top.add(resetBtn);
        add(top, BorderLayout.NORTH);

        // middle: product list and details
        Panel middle = new Panel(new GridLayout(1, 2, 8, 0));
        middle.add(list);
        middle.add(details);
        add(middle, BorderLayout.CENTER);

        // bottom: basket
        Panel left = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        left.add(new Label("Qty:"));
        left.add(qty);
        left.add(addBtn);
        Panel right = new Panel(new GridLayout(3, 1, 4, 4));
        right.add(removeBtn);
        right.add(checkoutBtn);
        right.add(total);
        Panel bottom = new Panel(new BorderLayout(8, 4));
        bottom.add(new Label("Basket:"), BorderLayout.NORTH);
        bottom.add(left, BorderLayout.WEST);
        bottom.add(basketList, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        if (user.isAdmin()) {
            addBtn.setEnabled(false);
            checkoutBtn.setEnabled(false);
            removeBtn.setEnabled(false);
            details.setText("Admin accounts cannot place orders.\nUse the Admin screen to manage products.");
        }

        searchBtn.addActionListener(e -> applyFilter());
        search.addActionListener(e -> applyFilter());
        category.addItemListener(e -> applyFilter());
        resetBtn.addActionListener(e -> {
            search.setText("");
            category.select("All");
            applyFilter();
        });
        list.addItemListener(e -> showDetails());
        addBtn.addActionListener(e -> addToBasket());
        removeBtn.addActionListener(e -> removeFromBasket());
        checkoutBtn.addActionListener(e -> checkout());
    }

    /** Reloads the category list and the product list. */
    public void refresh() {
        String old = category.getItemCount() > 0 ? category.getSelectedItem() : "All";
        category.removeAll();
        category.add("All");
        TreeSet<String> cats = new TreeSet<>();
        for (AppProduct p : store.products) cats.add(p.category);
        for (String c : cats) category.add(c);
        category.select(cats.contains(old) ? old : "All");
        applyFilter();
        refreshBasket();
    }

    private void applyFilter() {
        String q = search.getText().trim().toLowerCase();
        if (!q.isEmpty() && "CUSTOMER".equals(user.role)) {
            try { store.appendSearch(user.id, q); } catch (java.io.IOException ignored) { }
        }
        String cat = category.getSelectedItem();
        shown.clear();
        list.removeAll();
        for (AppProduct p : store.products) {
            if (!"All".equals(cat) && !p.category.equals(cat)) continue;
            if (!q.isEmpty() && !p.name.toLowerCase().contains(q)
                    && !p.category.toLowerCase().contains(q)) continue;
            shown.add(p);
            list.add(p.label());
        }
        if (!user.isAdmin()) {
            details.setText(shown.isEmpty() ? "No products match your search."
                    : "Select a product to see details.");
        }
    }

    private void showDetails() {
        int idx = list.getSelectedIndex();
        if (idx < 0 || idx >= shown.size()) return;
        AppProduct p = shown.get(idx);
        StringBuilder sb = new StringBuilder();
        sb.append(p.name).append("\n");
        sb.append("Category : ").append(p.category).append("\n");
        sb.append("Price    : Rs ").append(String.format("%.0f", p.price)).append("\n");
        sb.append("In stock : ").append(p.stock).append("\n");
        sb.append("Season   : ").append(p.season).append("\n");
        sb.append("Audience : ").append(p.audience).append("\n\n");
        sb.append("Customers also bought:\n");
        Map<Integer, Integer> together = engine.boughtTogether(p.id);
        if (together.isEmpty()) {
            sb.append("  (no basket data yet)\n");
        } else {
            int shownCount = 0;
            for (Map.Entry<Integer, Integer> e : together.entrySet()) {
                AppProduct other = store.findProduct(e.getKey());
                if (other == null) continue;
                sb.append("  - ").append(other.name).append(" (").append(e.getValue()).append(" order(s))\n");
                if (++shownCount == 3) break;
            }
        }
        details.setText(sb.toString());
    }

    private void addToBasket() {
        int idx = list.getSelectedIndex();
        if (idx < 0 || idx >= shown.size()) {
            Dialogs.message(owner, "Basket", "Please select a product first.");
            return;
        }
        AppProduct p = shown.get(idx);
        int q;
        try {
            q = Integer.parseInt(qty.getText().trim());
        } catch (NumberFormatException ex) {
            Dialogs.message(owner, "Invalid quantity", "Quantity must be a whole number.");
            return;
        }
        if (q < 1) {
            Dialogs.message(owner, "Invalid quantity", "Quantity must be at least 1.");
            return;
        }
        int already = basket.getOrDefault(p.id, 0);
        if (already + q > p.stock) {
            Dialogs.message(owner, "Not enough stock", "Only " + p.stock + " of " + p.name + " in stock.");
            return;
        }
        basket.put(p.id, already + q);
        refreshBasket();
    }

    private void removeFromBasket() {
        int idx = basketList.getSelectedIndex();
        if (idx < 0) {
            Dialogs.message(owner, "Basket", "Select a basket line to remove.");
            return;
        }
        Integer key = new ArrayList<>(basket.keySet()).get(idx);
        basket.remove(key);
        refreshBasket();
    }

    private void refreshBasket() {
        basketList.removeAll();
        double sum = 0;
        for (Map.Entry<Integer, Integer> e : basket.entrySet()) {
            AppProduct p = store.findProduct(e.getKey());
            if (p == null) continue;
            double line = p.price * e.getValue();
            sum += line;
            basketList.add(p.name + " x" + e.getValue() + " = Rs " + String.format("%.0f", line));
        }
        total.setText("Total: Rs " + String.format("%.0f", sum));
    }

    private void checkout() {
        if (basket.isEmpty()) {
            Dialogs.message(owner, "Checkout", "Your basket is empty.");
            return;
        }
        double sum = 0;
        for (Map.Entry<Integer, Integer> e : basket.entrySet()) {
            AppProduct p = store.findProduct(e.getKey());
            if (p == null || p.stock < e.getValue()) {
                Dialogs.message(owner, "Checkout", "Stock changed. Please rebuild your basket.");
                basket.clear();
                refreshBasket();
                return;
            }
            sum += p.price * e.getValue();
        }
        if (!Dialogs.confirm(owner, "Place order for Rs " + String.format("%.0f", sum) + "?")) return;

        int orderId = store.nextOrderId();
        String date = LocalDate.now().toString();
        for (Map.Entry<Integer, Integer> e : basket.entrySet()) {
            store.purchases.add(new PurchaseRecord(orderId, user.id, e.getKey(), e.getValue(), date));
            store.findProduct(e.getKey()).stock -= e.getValue();
        }
        try {
            store.savePurchases();
            store.saveProducts();
        } catch (IOException ex) {
            Dialogs.message(owner, "Warning", "Order placed but could not be saved:\n" + ex.getMessage());
        }
        basket.clear();
        refresh();
        Dialogs.message(owner, "Order placed", "Thank you! Order #" + orderId + " placed.\nTotal: Rs "
                + String.format("%.0f", sum));
    }
}
