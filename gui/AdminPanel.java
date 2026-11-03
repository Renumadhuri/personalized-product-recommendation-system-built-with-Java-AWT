package gui;

import app.AppProduct;
import app.DataStore;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.io.IOException;

/** Admin only: add a product, update a product (select it from the list), and see all products. */
public class AdminPanel extends Panel {
    private final DataStore store;
    private final Frame owner;
    private final TextField name = new TextField(14);
    private final TextField category = new TextField(10);
    private final TextField price = new TextField(6);
    private final TextField stock = new TextField(4);
    private final Choice season = new Choice();
    private final Choice audience = new Choice();
    private final Label error = new Label("");
    private final Label mode = new Label("Mode: adding a new product                    ");
    private final java.awt.List listing = new java.awt.List(15);

    public AdminPanel(DataStore store, Frame owner) {
        this.store = store;
        this.owner = owner;
        setLayout(new BorderLayout(8, 8));
        listing.setFont(new Font("Monospaced", Font.PLAIN, 13));
        error.setForeground(Color.RED);

        season.add("All"); season.add("Summer"); season.add("Monsoon"); season.add("Winter");
        audience.add("All"); audience.add("Kids"); audience.add("Teens"); audience.add("Adults");

        // Two fixed rows so nothing wraps and gets clipped
        Panel row1 = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        row1.add(new Label("Name:"));      row1.add(name);
        row1.add(new Label("Category:"));  row1.add(category);
        row1.add(new Label("Price:"));     row1.add(price);
        row1.add(new Label("Stock:"));     row1.add(stock);

        Button addBtn = new Button("Add Product");
        Button updateBtn = new Button("Update Selected");
        Button clearBtn = new Button("Clear Form");
        Panel row2 = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        row2.add(new Label("Season:"));    row2.add(season);
        row2.add(new Label("Audience:"));  row2.add(audience);
        row2.add(addBtn);
        row2.add(updateBtn);
        row2.add(clearBtn);

        Panel form = new Panel(new java.awt.GridLayout(2, 1));
        form.add(row1);
        form.add(row2);

        Panel messages = new Panel(new BorderLayout());
        messages.add(mode, BorderLayout.NORTH);
        messages.add(error, BorderLayout.SOUTH);

        Panel top = new Panel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(messages, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(listing, BorderLayout.CENTER);

        addBtn.addActionListener(e -> addProduct());
        updateBtn.addActionListener(e -> updateProduct());
        clearBtn.addActionListener(e -> clearForm());
        listing.addItemListener(e -> loadSelectedIntoForm());
    }

    public void refresh() {
        listing.removeAll();
        for (AppProduct p : store.products) {
            listing.add(String.format("%-4d %-22s %-12s Rs %-6.0f stock %-4d %-8s %s",
                    p.id, p.name, p.category, p.price, p.stock, p.season, p.audience));
        }
    }

    /** Fills the form with the product the admin clicked in the list. */
    private void loadSelectedIntoForm() {
        int idx = listing.getSelectedIndex();
        if (idx < 0 || idx >= store.products.size()) return;
        AppProduct p = store.products.get(idx);
        name.setText(p.name);
        category.setText(p.category);
        price.setText(String.valueOf(p.price));
        stock.setText(String.valueOf(p.stock));
        season.select(p.season);
        audience.select(p.audience);
        error.setText("");
        mode.setText("Mode: editing product #" + p.id + " (press Update Selected to save)");
    }

    private void clearForm() {
        name.setText(""); category.setText(""); price.setText(""); stock.setText("");
        season.select("All"); audience.select("All");
        if (listing.getSelectedIndex() >= 0) listing.deselect(listing.getSelectedIndex());
        error.setText("");
        mode.setText("Mode: adding a new product");
    }

    /**
     * Validates the form. Returns a new AppProduct with the given id,
     * or null (and shows the message) if something is invalid.
     * ignoreIndex = index of the product being edited (so its own name is not a "duplicate"), or -1.
     */
    private AppProduct readForm(int id, int ignoreIndex) {
        String n = name.getText().trim();
        String c = category.getText().trim();
        if (n.isEmpty() || n.length() > 40 || n.contains(",")) { error.setText("Name: 1-40 characters, no commas"); return null; }
        if (c.isEmpty() || c.length() > 25 || c.contains(",")) { error.setText("Category: 1-25 characters, no commas"); return null; }
        for (int i = 0; i < store.products.size(); i++) {
            if (i == ignoreIndex) continue;
            if (store.products.get(i).name.equalsIgnoreCase(n)) {
                error.setText("A product with this name already exists");
                return null;
            }
        }
        double pr;
        int st;
        try {
            pr = Double.parseDouble(price.getText().trim());
        } catch (NumberFormatException ex) {
            error.setText("Price must be a number");
            return null;
        }
        if (pr <= 0 || pr > 100000) { error.setText("Price must be between 1 and 100000"); return null; }
        try {
            st = Integer.parseInt(stock.getText().trim());
        } catch (NumberFormatException ex) {
            error.setText("Stock must be a whole number");
            return null;
        }
        if (st < 0 || st > 10000) { error.setText("Stock must be between 0 and 10000"); return null; }

        return new AppProduct(id, n, c, pr, st, season.getSelectedItem(), audience.getSelectedItem());
    }

    private void addProduct() {
        AppProduct p = readForm(store.nextProductId(), -1);
        if (p == null) return;

        store.products.add(p);
        try {
            store.saveProducts();
        } catch (IOException ex) {
            store.products.remove(p);
            error.setText("Could not save: " + ex.getMessage());
            return;
        }
        clearForm();
        refresh();
        Dialogs.message(owner, "Product added", p.name + " added with ID " + p.id + ".");
    }

    private void updateProduct() {
        int idx = listing.getSelectedIndex();
        if (idx < 0 || idx >= store.products.size()) {
            error.setText("Select a product from the list first");
            return;
        }
        AppProduct old = store.products.get(idx);
        AppProduct updated = readForm(old.id, idx);      // keeps the same ID
        if (updated == null) return;

        store.products.set(idx, updated);
        try {
            store.saveProducts();
        } catch (IOException ex) {
            store.products.set(idx, old);                // roll back if the file save fails
            error.setText("Could not save: " + ex.getMessage());
            return;
        }
        clearForm();
        refresh();
        Dialogs.message(owner, "Product updated", updated.name + " (ID " + updated.id + ") updated.");
    }
}