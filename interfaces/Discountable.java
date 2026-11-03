package interfaces;

public interface Discountable {
    double MAX_DISCOUNT_PERCENT = 50.0;   // constant (public static final)

    double getPrice();

    default double priceAfterDiscount(double percent) {
        if (percent < 0 || percent > MAX_DISCOUNT_PERCENT) {
            throw new IllegalArgumentException(
                    "Discount must be between 0 and " + MAX_DISCOUNT_PERCENT + "%");
        }
        return getPrice() * (1 - percent / 100.0);
    }

    static double gst(double amount) {    // static method
        return amount * 0.18;
    }
}
