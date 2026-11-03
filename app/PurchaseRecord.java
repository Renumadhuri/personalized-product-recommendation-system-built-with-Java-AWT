package app;

/** One line of an order. Lines with the same orderId belong to the same basket. */
public class PurchaseRecord {
    public final int orderId, customerId, productId, quantity;
    public final String date;

    public PurchaseRecord(int orderId, int customerId, int productId, int quantity, String date) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.date = date;
    }

    public String toCsv() {
        return orderId + "," + customerId + "," + productId + "," + quantity + "," + date;
    }

    public static PurchaseRecord fromCsv(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 5) throw new IllegalArgumentException("Bad purchase line: " + line);
        return new PurchaseRecord(Integer.parseInt(f[0].trim()), Integer.parseInt(f[1].trim()),
                Integer.parseInt(f[2].trim()), Integer.parseInt(f[3].trim()), f[4]);
    }
}
