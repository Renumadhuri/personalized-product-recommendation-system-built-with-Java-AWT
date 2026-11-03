package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * All file handling for Week 10: reading and writing CSV files.
 * Files live in the "data" folder of the project root.
 */
public class CsvStore {

    public static final String PRODUCT_FILE = "data/week10_products.csv";
    public static final String HISTORY_FILE = "data/week10_history.csv";
    public static final String EXPORT_FILE  = "data/week10_history_export.csv";

    /** Creates the data folder if it does not exist yet. */
    public static void ensureDataFolder() {
        File dir = new File("data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // ---------- PRODUCTS ----------

    /** Reads all products from the CSV file into an ArrayList. */
    public static ArrayList<StoreItem> loadItems() {
        ArrayList<StoreItem> list = new ArrayList<>();
        File f = new File(PRODUCT_FILE);
        if (!f.exists()) {
            return list;                       // first run: nothing saved yet
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            br.readLine();                     // skip the header line
            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    list.add(StoreItem.fromCsv(line));
                } catch (IllegalArgumentException e) {
                    System.out.println("  [WARN] Skipped bad line " + lineNo + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not read " + PRODUCT_FILE + ": " + e.getMessage());
        }
        return list;
    }

    /** Writes all products to the CSV file (overwrites the old file). */
    public static void saveItems(List<StoreItem> items) {
        try (PrintWriter out = new PrintWriter(new FileWriter(PRODUCT_FILE))) {
            out.println("id,name,category,price,stock");
            for (StoreItem it : items) {
                out.println(it.toCsv());
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not save " + PRODUCT_FILE + ": " + e.getMessage());
        }
    }

    // ---------- RECOMMENDATION HISTORY ----------

    /** Adds ONE history line to the end of the file (append mode = true). */
    public static void appendHistory(int customerId, String entry) {
        try (PrintWriter out = new PrintWriter(new FileWriter(HISTORY_FILE, true))) {
            out.println(customerId + "," + entry);
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not write " + HISTORY_FILE + ": " + e.getMessage());
        }
    }

    /** Reads the history file into a HashMap: customer ID -> list of entries. */
    public static HashMap<Integer, ArrayList<String>> loadHistory() {
        HashMap<Integer, ArrayList<String>> map = new HashMap<>();
        File f = new File(HISTORY_FILE);
        if (!f.exists()) {
            return map;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",", 2);     // "id" and "date,product"
                if (parts.length != 2) {
                    continue;
                }
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    map.computeIfAbsent(id, k -> new ArrayList<>()).add(parts[1]);
                } catch (NumberFormatException e) {
                    System.out.println("  [WARN] Skipped bad history line: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not read " + HISTORY_FILE + ": " + e.getMessage());
        }
        return map;
    }

    // ---------- CSV EXPORT ----------

    /**
     * Exports the whole history as a report CSV (opens in Excel).
     * Returns the number of rows written, or -1 if it failed.
     */
    public static int exportHistory(Map<Integer, ArrayList<String>> history) {
        int rows = 0;
        try (PrintWriter out = new PrintWriter(new FileWriter(EXPORT_FILE))) {
            out.println("customer_id,date_time,product");
            // TreeMap keeps the customers sorted by ID
            for (Map.Entry<Integer, ArrayList<String>> e : new TreeMap<>(history).entrySet()) {
                for (String entry : e.getValue()) {
                    out.println(e.getKey() + "," + entry);
                    rows++;
                }
            }
        } catch (IOException ex) {
            System.out.println("  [ERROR] Export failed: " + ex.getMessage());
            return -1;
        }
        return rows;
    }
}
