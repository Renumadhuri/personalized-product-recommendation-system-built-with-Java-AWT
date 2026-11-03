package model;

/** A product for sale. It HAS a Category (composition). */
public class Product {

    private final int id;
    private final String name;
    private final Category category;
    private final double price;
    private int stock;
    private final String addedOn;

    public Product(int id, String name, Category category, double price, int stock, String addedOn) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.addedOn = addedOn;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getAddedOn() { return addedOn; }

    public boolean isInStock() {
        return stock > 0;
    }

    // Returns true if the stock was reduced, false if the request was not possible
    public boolean reduceStock(int quantity) {
        if (quantity <= 0 || quantity > stock) {
            return false;
        }
        stock -= quantity;
        return true;
    }

    @Override
    public String toString() {
        return id + " - " + name + " [" + category + "] Rs. " + String.format("%.2f", price);
    }
}
