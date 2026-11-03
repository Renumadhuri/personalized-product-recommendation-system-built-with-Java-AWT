package interfaces;

public interface Describable {
    String describe();

    default void printInfo() {
        System.out.println(describe());
    }
}
