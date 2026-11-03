package app;

/** A MINISO product. Saved in data/app_products.csv */
public class AppProduct {
    public final int id;
    public final String name, category, season, audience;
    public final double price;
    public int stock;

    public AppProduct(int id, String name, String category, double price,
                      int stock, String season, String audience) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.season = season;
        this.audience = audience;
    }

    public String toCsv() {
        return id + "," + name + "," + category + "," + price + "," + stock + ","
                + season + "," + audience;
    }

    public static AppProduct fromCsv(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 7) throw new IllegalArgumentException("Bad product line: " + line);
        return new AppProduct(Integer.parseInt(f[0].trim()), f[1], f[2],
                Double.parseDouble(f[3].trim()), Integer.parseInt(f[4].trim()), f[5], f[6]);
    }

    /** One-line text for lists. */
    public String label() {
        return String.format("%d | %s | %s | Rs %.0f | stock %d", id, name, category, price, stock);
    }
}
