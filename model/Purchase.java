package model;

public class Purchase {

    private final int purchaseId;
    private final int customerId;
    private final Product product;
    private final int quantity;
    private final double unitPrice;
    private final double total;
    private final String dateTime;

    public Purchase(int purchaseId, int customerId, Product product, int quantity, String dateTime) {
        this.purchaseId = purchaseId;
        this.customerId = customerId;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.total = Math.round(quantity * unitPrice * 100.0) / 100.0;
        this.dateTime = dateTime;
    }

    public int getPurchaseId() { return purchaseId; }
    public int getCustomerId() { return customerId; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotal() { return total; }
    public String getDateTime() { return dateTime; }

    @Override
    public String toString() {
        return "#" + purchaseId + " " + product.getName() + " x" + quantity
                + " = Rs. " + String.format("%.2f", total) + " (" + dateTime + ")";
    }
}
