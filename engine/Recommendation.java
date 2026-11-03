package engine;

import app.AppProduct;

/** One recommended product with its score and the human-readable reasons behind it. */
public class Recommendation {
    public final AppProduct product;
    public final double score;
    public final String reasons;   // what RecommendPanel prints after "Why:"

    public Recommendation(AppProduct product, double score, String reason) {
        this.product = product;
        this.score = score;
        this.reasons = reason;
    }
}
