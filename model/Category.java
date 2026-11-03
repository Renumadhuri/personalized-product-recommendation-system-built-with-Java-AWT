package model;

public class Category {

    private final int id;
    private final String name;

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Category)) return false;
        return name.equalsIgnoreCase(((Category) other).name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
