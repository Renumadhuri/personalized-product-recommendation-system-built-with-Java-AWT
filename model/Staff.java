package model;

/**
 * A Staff member (administrator) IS-A User too, but with different abilities:
 * staff can register products, customers cannot.
 */
public class Staff extends User {

    private final String department;

    public Staff(int id, String name, String email, String passwordHash, String department) {
        super(id, name, email, passwordHash);
        this.department = department;
    }

    public String getDepartment() { return department; }

    // Product registration is a STAFF behaviour
    public Product registerProduct(int id, String name, Category category,double price, int stock, String addedOn) {
        return new Product(id, name, category, price, stock, addedOn);
    }

    @Override
    public String getRole() {
        return "Staff";
    }

    @Override
    public String describe() {
        return super.describe() + " | department: " + department;
    }
}
