package gui;

import app.AppProduct;
import app.AppUser;
import app.DataStore;
import app.PurchaseRecord;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

/** Order history: a customer sees their own orders, the admin sees all. Can export to CSV. */
public class OrdersPanel extends Panel {
    private final DataStore store;
    private final AppUser user;
    private final Frame owner;
    private final TextArea out = new TextArea("", 20, 70, TextArea.SCROLLBARS_VERTICAL_ONLY);

    public OrdersPanel(DataStore store, AppUser user, Frame owner) {
        this.store = store;
        this.user = user;
        this.owner = owner;
        setLayout(new BorderLayout(8, 8));
        out.setEditable(false);
        out.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(out, BorderLayout.CENTER);

        Panel bottom = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        Button exportBtn = new Button("Export to CSV");
        bottom.add(exportBtn);
        add(bottom, BorderLayout.SOUTH);
        exportBtn.addActionListener(e -> export());
    }

    private ArrayList<PurchaseRecord> visible() {
        ArrayList<PurchaseRecord> rows = new ArrayList<>();
        for (PurchaseRecord r : store.purchases) {
            if (user.isAdmin() || r.customerId == user.id) rows.add(r);
        }
        return rows;
    }

    public void refresh() {
        TreeMap<Integer, ArrayList<PurchaseRecord>> byOrder = new TreeMap<>();
        for (PurchaseRecord r : visible()) {
            byOrder.computeIfAbsent(r.orderId, k -> new ArrayList<>()).add(r);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(user.isAdmin() ? "ALL ORDERS" : "MY ORDERS").append("\n\n");
        if (byOrder.isEmpty()) sb.append("No orders yet.");
        for (Integer orderId : byOrder.keySet()) {
            ArrayList<PurchaseRecord> lines = byOrder.get(orderId);
            AppUser c = store.findUser(lines.get(0).customerId);
            sb.append("Order #").append(orderId).append("  |  ").append(lines.get(0).date)
                    .append("  |  ").append(c == null ? "unknown" : c.name).append("\n");
            double sum = 0;
            for (PurchaseRecord r : lines) {
                AppProduct p = store.findProduct(r.productId);
                double price = p == null ? 0 : p.price;
                sum += price * r.quantity;
                sb.append("   - ").append(p == null ? "unknown product" : p.name).append(" x").append(r.quantity)
                        .append(" @ Rs ").append(String.format("%.0f", price)).append("\n");
            }
            sb.append("   Order total: Rs ").append(String.format("%.0f", sum)).append("\n\n");
        }
        out.setText(sb.toString());
    }

    private void export() {
        try {
            int n = store.exportOrders(visible());
            Dialogs.message(owner, "Export done", "Exported " + n + " row(s) to\ndata/app_orders_export.csv");
        } catch (IOException ex) {
            Dialogs.message(owner, "Error", "Export failed:\n" + ex.getMessage());
        }
    }
}
