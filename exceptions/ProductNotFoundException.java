package exceptions;

public class ProductNotFoundException extends Exception {
    public ProductNotFoundException(int productId) {
        super("No product found with ID " + productId);
    }
}
