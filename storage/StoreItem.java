package storage;
public class StoreItem {

    private final int id;
    private final String name;
    private final String category;
    private final double price;
    private final int stock;

    public StoreItem(int id, String name, String category, double price, int stock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public int getId()          { return id; }
    public String getName()     { return name; }
    public String getCategory() { return category; }
    public double getPrice()    { return price; }
    public int getStock()       { return stock; }

    /** Converts this item to one CSV line. */
    public String toCsv() {
        return id + "," + name + "," + category + "," + price + "," + stock;
    }

    /**
     * Builds an item from one CSV line.
     * Throws IllegalArgumentException (NumberFormatException is a subclass)
     * when the line is damaged, so the caller can skip it.
     */
    public static StoreItem fromCsv(String line) {
        String[] p = line.split(",");
        if (p.length != 5) {
            throw new IllegalArgumentException("expected 5 fields but found " + p.length);
        }
        return new StoreItem(
                Integer.parseInt(p[0].trim()),
                p[1].trim(),
                p[2].trim(),
                Double.parseDouble(p[3].trim()),
                Integer.parseInt(p[4].trim()));
    }

    @Override
    public String toString() {
        return String.format("#%-4d | %-28s | %-12s | Rs. %8.2f | stock %d",
                id, name, category, price, stock);
    }
}
