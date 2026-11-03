package shop;

import exceptions.InsufficientStockException;
import exceptions.InvalidPriceException;
import exceptions.ValidationException;
import interfaces.Describable;
import interfaces.Discountable;
import interfaces.Validatable;

public class ShopProduct implements Validatable, Describable, Discountable {
    private final int id;
    private final String name;
    private final double price;
    private int stock;

    public ShopProduct(int id, String name, double price, int stock) {
        if (price <= 0) {
            throw new InvalidPriceException(price);   // unchecked
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getStock() { return stock; }

    @Override
    public double getPrice() { return price; }

    public void reduceStock(int qty) throws InsufficientStockException {
        if (qty > stock) {
            throw new InsufficientStockException(name, qty, stock);
        }
        stock -= qty;
    }

    @Override
    public void validate() throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "Product name cannot be empty.");
        }
        if (stock < 0) {
            throw new ValidationException("stock", "Stock cannot be negative.");
        }
    }

    @Override
    public String describe() {
        return String.format("Product #%d | %s | Rs. %.2f | stock %d", id, name, price, stock);
    }
}
