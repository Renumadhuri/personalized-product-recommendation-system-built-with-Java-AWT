package model;

/** One suggestion shown to a customer: which product, and why. */
public class Recommendation {

    private final int recId;
    private final int customerId;
    private final Product product;
    private final String reason;
    private final String dateTime;
    private boolean wasBought;    // becomes true if the customer later buys it

    public Recommendation(int recId, int customerId, Product product, String reason, String dateTime) {
        this.recId = recId;
        this.customerId = customerId;
        this.product = product;
        this.reason = reason;
        this.dateTime = dateTime;
        this.wasBought = false;
    }

    public int getRecId() { return recId; }
    public int getCustomerId() { return customerId; }
    public Product getProduct() { return product; }
    public String getReason() { return reason; }
    public String getDateTime() { return dateTime; }
    public boolean isWasBought() { return wasBought; }

    public void markBought() {
        wasBought = true;
    }

    @Override
    public String toString() {
        return product.getName() + " (Rs. " + String.format("%.2f", product.getPrice())
                + ") - " + reason;
    }
}
