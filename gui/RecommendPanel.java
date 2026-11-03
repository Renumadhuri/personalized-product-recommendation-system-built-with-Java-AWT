package gui;

import app.AppUser;
import app.DataStore;
import engine.Recommendation;
import engine.RecommendationEngine;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.io.IOException;

/** Shows top-5 recommendations with reasons, saves them to history, exports history to CSV. */
public class RecommendPanel extends Panel {
    private final DataStore store;
    private final AppUser user;
    private final RecommendationEngine engine;
    private final Frame owner;
    private final Choice customer = new Choice();
    private final Choice season = new Choice();
    private final TextArea out = new TextArea("", 15, 70, TextArea.SCROLLBARS_VERTICAL_ONLY);

    public RecommendPanel(DataStore store, AppUser user, RecommendationEngine engine, Frame owner) {
        this.store = store;
        this.user = user;
        this.engine = engine;
        this.owner = owner;
        setLayout(new BorderLayout(8, 8));
        out.setEditable(false);
        out.setFont(new Font("Monospaced", Font.PLAIN, 13));

        season.add("Auto");
        season.add("Summer");
        season.add("Monsoon");
        season.add("Winter");

        Panel top = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        Button goBtn = new Button("Recommend");
        top.add(new Label("Customer:"));
        top.add(customer);
        top.add(new Label("Season:"));
        top.add(season);
        top.add(goBtn);
        add(top, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);

        Panel bottom = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        Button historyBtn = new Button("Show History");
        Button exportBtn = new Button("Export History to CSV");
        bottom.add(historyBtn);
        bottom.add(exportBtn);
        add(bottom, BorderLayout.SOUTH);

        goBtn.addActionListener(e -> recommend());
        historyBtn.addActionListener(e -> showHistory());
        exportBtn.addActionListener(e -> exportHistory());
    }

    public void refresh() {
        customer.removeAll();
        if (user.isAdmin()) {
            for (AppUser u : store.users) if (!u.isAdmin()) customer.add(u.id + " - " + u.name);
        } else {
            customer.add(user.id + " - " + user.name);
        }
        out.setText("""
                Choose a season (or Auto) and press Recommend.
                Current season by date: %s
                """.formatted(RecommendationEngine.currentSeason()));
    }

    private AppUser selectedUser() {
        String item = customer.getSelectedItem();
        if (item == null) return null;
        int id = Integer.parseInt(item.substring(0, item.indexOf(' ')));
        return store.findUser(id);
    }

    private void recommend() {
        AppUser u = selectedUser();
        if (u == null) {
            Dialogs.message(owner, "Recommend", "No customer selected.");
            return;
        }
        java.util.List<Recommendation> recs = engine.recommend(u, season.getSelectedItem(), 5);
        StringBuilder sb = new StringBuilder();
        sb.append("Recommendations for ").append(u.name).append(" (age ").append(u.age)
                .append(", ").append(RecommendationEngine.ageGroup(u.age)).append(", ")
                .append(u.city).append(")\n");
        sb.append("Season: ").append(season.getSelectedItem());
        if ("Auto".equals(season.getSelectedItem())) sb.append(" -> ").append(RecommendationEngine.currentSeason());
        sb.append("\n\n");
        if (recs.isEmpty()) {
            sb.append("No recommendations available right now.");
        }
        int n = 1;
        for (Recommendation r : recs) {
            sb.append(n++).append(". ").append(r.product.name).append("  [").append(r.product.category)
                    .append("]  Rs ").append(String.format("%.0f", r.product.price))
                    .append("  score ").append(r.score).append("\n");
            sb.append("   Why: ").append(r.reasons).append("\n\n");
        }
        try {
            for (Recommendation r : recs) store.appendHistory(u.id, r.product.name);
            if (!recs.isEmpty()) sb.append("(Saved to recommendation history)");
        } catch (IOException ex) {
            sb.append("(Could not save history: ").append(ex.getMessage()).append(")");
        }
        out.setText(sb.toString());
    }

    private void showHistory() {
        AppUser u = selectedUser();
        if (u == null) return;
        try {
            StringBuilder sb = new StringBuilder("Recommendation history for " + u.name + "\n\n");
            int count = 0;
            for (String[] h : store.loadHistory(u.id)) {
                sb.append(h[1]).append("   ").append(h[2]).append("\n");
                count++;
            }
            if (count == 0) sb.append("(no history yet)");
            out.setText(sb.toString());
        } catch (IOException ex) {
            Dialogs.message(owner, "Error", "Could not read history:\n" + ex.getMessage());
        }
    }

    private void exportHistory() {
        try {
            int n = store.exportHistory(user.isAdmin() ? -1 : user.id);
            Dialogs.message(owner, "Export done", "Exported " + n + " row(s) to\ndata/app_history_export.csv");
        } catch (IOException ex) {
            Dialogs.message(owner, "Error", "Export failed:\n" + ex.getMessage());
        }
    }
}
