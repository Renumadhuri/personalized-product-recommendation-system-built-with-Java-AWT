package engine;

import app.AppProduct;
import app.AppUser;
import app.DataStore;
import app.PurchaseRecord;
import java.io.IOException;
import java.util.*;

/**
 * Multi-signal recommendation engine.
 *
 * A product is scored from SIX signals (the old version used season only):
 *   1. AGE       - product audience (Kids / Teens / Adults) matches the customer's age group
 *   2. SEASON    - product season matches the season the customer chose
 *   3. PURCHASE  - categories and items the customer bought before (more + recent = stronger)
 *   4. SEARCH    - words the customer typed in the product search box
 *   5. GENDER    - what other customers of the same gender AND age group bought (data-driven,
 *                  no fixed "boys/girls" rules)
 *   6. TOGETHER  - basket analysis: items usually bought in the same order as the customer's items
 * plus a small POPULARITY bonus, and a penalty for items already bought.
 */
public class RecommendationEngine {

    // ---- weights: change these to tune the system ----
    static final double W_AGE_EXACT   = 3.0;
    static final double W_AGE_ANY     = 1.0;   // product audience "All"
    static final double W_SEASON      = 3.0;
    static final double W_SEASON_ANY  = 1.0;   // product season "All"
    static final double W_CATEGORY    = 2.5;   // x share of customer's purchases in that category
    static final double W_SEARCH_NAME = 2.0;   // per matching search term found in product name
    static final double W_SEARCH_CAT  = 1.0;   // per matching search term found in category
    static final double W_PEERS       = 2.0;   // x share of same gender+age-group purchases
    static final double W_TOGETHER    = 1.5;   // per co-purchase
    static final double W_POPULAR     = 1.0;   // x relative popularity (0..1)
    static final double P_ALREADY     = 4.0;   // penalty if bought before
    static final int    RECENT_SEARCHES = 15;  // only the latest N searches count

    private final DataStore store;

    public RecommendationEngine(DataStore store) {
        this.store = store;
    }

    // ------------------------------------------------------------------ helpers

    /** Season for today's date in India: Mar-Jun Summer, Jul-Sep Monsoon, Oct-Feb Winter. */
    public static String currentSeason() {
        int m = java.time.LocalDate.now().getMonthValue();
        if (m >= 3 && m <= 6) return "Summer";
        if (m >= 7 && m <= 9) return "Monsoon";
        return "Winter";
    }

    public static String ageGroup(int age) {
        if (age <= 12) return "Kids";
        if (age <= 19) return "Teens";
        return "Adults";
    }

    /** Basket analysis: productId -> number of orders where it was bought together with {@code productId}. */
    public Map<Integer, Integer> boughtTogether(int productId) {
        Map<Integer, Set<Integer>> orders = new HashMap<>();
        for (PurchaseRecord r : store.purchases)
            orders.computeIfAbsent(r.orderId, k -> new HashSet<>()).add(r.productId);
        Map<Integer, Integer> out = new LinkedHashMap<>();
        for (Set<Integer> items : orders.values()) {
            if (!items.contains(productId)) continue;
            for (int other : items) if (other != productId) out.merge(other, 1, Integer::sum);
        }
        return out;
    }

    private static List<String> tokens(String text) {
        List<String> t = new ArrayList<>();
        for (String w : text.toLowerCase().split("[^a-z0-9]+"))
            if (w.length() >= 3) t.add(w);
        return t;
    }

    // ------------------------------------------------------------------ main entry

    /**
     * @param season "All", "Summer", "Monsoon" or "Winter"
     * @param limit  how many products to return
     */
    public List<Recommendation> recommend(AppUser user, String season, int limit) {
        if ("Auto".equals(season)) season = currentSeason();   // "Auto" = use today's season
        String group = ageGroup(user.age);

        // ---- signal data: this customer's purchases ----
        Set<Integer> boughtIds = new HashSet<>();
        Map<String, Double> catWeight = new HashMap<>();   // category -> weighted units bought
        double totalWeight = 0;
        List<PurchaseRecord> mine = new ArrayList<>();
        for (PurchaseRecord r : store.purchases) if (r.customerId == user.id) mine.add(r);
        for (int i = 0; i < mine.size(); i++) {
            PurchaseRecord r = mine.get(i);
            AppProduct p = store.findProduct(r.productId);
            if (p == null) continue;
            boughtIds.add(r.productId);
            // newer purchases (later in the file) count a bit more: 1.0 .. 2.0
            double recency = 1.0 + (mine.size() == 1 ? 0 : (double) i / (mine.size() - 1));
            double w = r.quantity * recency;
            catWeight.merge(p.category, w, Double::sum);
            totalWeight += w;
        }

        // ---- signal data: this customer's recent search words ----
        Map<String, Integer> searchTerms = new LinkedHashMap<>();
        try {
            List<String[]> s = store.loadSearches(user.id);          // {customerId, dateTime, term}
            for (int i = Math.max(0, s.size() - RECENT_SEARCHES); i < s.size(); i++)
                for (String w : tokens(s.get(i)[2])) searchTerms.merge(w, 1, Integer::sum);
        } catch (IOException ignored) { /* no search file yet */ }

        // ---- signal data: peers = same gender AND same age group (excluding this customer) ----
        Map<Integer, Integer> peerBuys = new HashMap<>();             // productId -> units
        int peerTotal = 0;
        for (PurchaseRecord r : store.purchases) {
            AppUser u = store.findUser(r.customerId);
            if (u == null || u.id == user.id) continue;
            if (u.gender.equalsIgnoreCase(user.gender) && ageGroup(u.age).equals(group)) {
                peerBuys.merge(r.productId, r.quantity, Integer::sum);
                peerTotal += r.quantity;
            }
        }

        // ---- store-wide popularity ----
        Map<Integer, Integer> lines = new HashMap<>();
        int maxLines = 1;
        for (PurchaseRecord r : store.purchases)
            maxLines = Math.max(maxLines, lines.merge(r.productId, 1, Integer::sum));

        // ---- score every product ----
        List<Recommendation> out = new ArrayList<>();
        for (AppProduct p : store.products) {
            if (p.stock <= 0) continue;                                              // can't sell it
            if (!p.audience.equals("All") && !p.audience.equals(group)) continue;    // wrong age group
            if (!season.equals("All") && !p.season.equals("All") && !p.season.equals(season)) continue;

            double score = 0;
            List<String> why = new ArrayList<>();

            // 1. AGE
            if (p.audience.equals(group)) { score += W_AGE_EXACT; why.add("Made for " + group.toLowerCase()); }
            else score += W_AGE_ANY;

            // 2. SEASON
            if (!season.equals("All") && p.season.equals(season)) { score += W_SEASON; why.add("Suits the " + season + " season"); }
            else if (p.season.equals("All")) score += W_SEASON_ANY;

            // 3. PURCHASE HISTORY
            if (totalWeight > 0 && catWeight.containsKey(p.category)) {
                score += W_CATEGORY * (catWeight.get(p.category) / totalWeight);
                why.add("You often buy " + p.category);
            }

            // 4. SEARCH HISTORY
            String name = p.name.toLowerCase(), cat = p.category.toLowerCase();
            double sBoost = 0; String hit = null;
            for (Map.Entry<String, Integer> e : searchTerms.entrySet()) {
                if (name.contains(e.getKey()))     { sBoost += W_SEARCH_NAME * Math.min(e.getValue(), 3); hit = e.getKey(); }
                else if (cat.contains(e.getKey())) { sBoost += W_SEARCH_CAT  * Math.min(e.getValue(), 3); if (hit == null) hit = e.getKey(); }
            }
            if (sBoost > 0) { score += Math.min(sBoost, 6.0); why.add("You searched for \"" + hit + "\""); }

            // 5. GENDER + AGE PEERS
            if (peerTotal > 0 && peerBuys.containsKey(p.id)) {
                score += W_PEERS * peerBuys.get(p.id) / (double) peerTotal * 3;      // x3 so small shops still show an effect
                why.add("Popular with " + group.toLowerCase() + " (" + user.gender.toLowerCase() + ")");
            }

            // 6. BOUGHT TOGETHER
            int together = 0; String withName = null;
            for (int bid : boughtIds) {
                Integer n = boughtTogether(bid).get(p.id);
                if (n != null) { together += n; if (withName == null) { AppProduct b = store.findProduct(bid); withName = b == null ? "" : b.name; } }
            }
            if (together > 0) { score += W_TOGETHER * together; why.add("Often bought with " + withName); }

            // popularity + repeat penalty
            double pop = lines.getOrDefault(p.id, 0) / (double) maxLines;
            score += W_POPULAR * pop;
            if (why.isEmpty() && pop > 0) why.add("Popular in the store");
            if (boughtIds.contains(p.id)) { score -= P_ALREADY; why.add("You bought this before"); }
            if (why.isEmpty()) why.add("Available in stock");

            out.add(new Recommendation(p, score, String.join(" | ", why.subList(0, Math.min(3, why.size())))));
        }

        out.sort((a, b) -> {
            int c = Double.compare(b.score, a.score);
            return c != 0 ? c : a.product.name.compareTo(b.product.name);
        });
        // No cap: return EVERY eligible product, best first (the 'limit' passed by the panel is ignored).
        return out;
    }
}