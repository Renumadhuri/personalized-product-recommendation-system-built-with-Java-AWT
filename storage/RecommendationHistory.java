package storage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Remembers which products were recommended to which customer.
 * Data structure: HashMap  (customer ID  ->  ArrayList of history entries).
 * Each entry looks like "2026-10-08 10:30,MINISO Cute Bear Plush Toy".
 */
public class RecommendationHistory {

    private final HashMap<Integer, ArrayList<String>> history = new HashMap<>();

    /** Loads old history from the CSV file (call once at start-up). */
    public void load() {
        history.clear();
        history.putAll(CsvStore.loadHistory());
    }

    /** Records one recommendation in memory AND in the CSV file. */
    public void add(int customerId, String productName) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String entry = time + "," + productName;
        history.computeIfAbsent(customerId, k -> new ArrayList<>()).add(entry);
        CsvStore.appendHistory(customerId, entry);
    }

    /** Returns the history of one customer (empty list if none). */
    public ArrayList<String> get(int customerId) {
        return history.getOrDefault(customerId, new ArrayList<>());
    }

    /** Returns the complete map (used for the CSV export). */
    public HashMap<Integer, ArrayList<String>> getAll() {
        return history;
    }
}
