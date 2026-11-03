package gui;

import app.AppUser;
import app.DataStore;
import engine.RecommendationEngine;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Main window: menu bar, button bar and a CardLayout that switches between screens. */
public class MainFrame extends Frame {
    private final DataStore store;
    private final AppUser user;
    private final CardLayout cards = new CardLayout();
    private final Panel center = new Panel(cards);
    private final ProductsPanel products;
    private final RecommendPanel recommend;
    private final OrdersPanel orders;
    private AdminPanel admin;

    public MainFrame(DataStore store, AppUser user) {
        super("MINISO - " + user.name + " (" + user.role + ")");
        this.store = store;
        this.user = user;
        RecommendationEngine engine = new RecommendationEngine(store);
        setLayout(new BorderLayout());

        products = new ProductsPanel(store, user, engine, this);
        recommend = new RecommendPanel(store, user, engine, this);
        orders = new OrdersPanel(store, user, this);
        center.add(products, "products");
        center.add(recommend, "recommend");
        center.add(orders, "orders");
        if (user.isAdmin()) {
            admin = new AdminPanel(store, this);
            center.add(admin, "admin");
        }
        add(center, BorderLayout.CENTER);

        // ----- menu bar -----
        MenuBar mb = new MenuBar();
        Menu file = new Menu("File");
        MenuItem logoutItem = new MenuItem("Logout");
        MenuItem exitItem = new MenuItem("Exit");
        file.add(logoutItem);
        file.addSeparator();
        file.add(exitItem);
        mb.add(file);

        Menu shop = new Menu("Shop");
        MenuItem productsItem = new MenuItem("Products");
        MenuItem ordersItem = new MenuItem(user.isAdmin() ? "All Orders" : "My Orders");
        shop.add(productsItem);
        shop.add(ordersItem);
        mb.add(shop);

        Menu rec = new Menu("Recommend");
        MenuItem recItem = new MenuItem("Get Recommendations");
        rec.add(recItem);
        mb.add(rec);

        if (user.isAdmin()) {
            Menu adminMenu = new Menu("Admin");
            MenuItem manage = new MenuItem("Manage Products");
            manage.addActionListener(e -> showCard("admin"));
            adminMenu.add(manage);
            mb.add(adminMenu);
        }

        Menu help = new Menu("Help");
        MenuItem about = new MenuItem("About");
        help.add(about);
        mb.add(help);
        setMenuBar(mb);

        logoutItem.addActionListener(e -> logout());
        exitItem.addActionListener(e -> exitApp());
        productsItem.addActionListener(e -> showCard("products"));
        ordersItem.addActionListener(e -> showCard("orders"));
        recItem.addActionListener(e -> showCard("recommend"));
        about.addActionListener(e -> Dialogs.message(this, "About",
                "MINISO Recommendation System\nJava AWT project (Weeks 11 and 12)\nLogged in as: " + user.name));

        // ----- button bar -----
        Panel bar = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(new Color(228, 0, 43));
        Label hello = new Label("Hello, " + user.name + "   ");
        hello.setForeground(Color.WHITE);
        hello.setFont(new Font("SansSerif", Font.BOLD, 14));
        bar.add(hello);
        addNav(bar, "Products", "products");
        addNav(bar, "Recommendations", "recommend");
        addNav(bar, user.isAdmin() ? "All Orders" : "My Orders", "orders");
        if (user.isAdmin()) addNav(bar, "Admin", "admin");
        Button logoutBtn = new Button("Logout");
        logoutBtn.addActionListener(e -> logout());
        bar.add(logoutBtn);
        add(bar, BorderLayout.NORTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });

        showCard("products");
        setSize(980, 640);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void addNav(Panel bar, String text, String card) {
        Button b = new Button(text);
        b.addActionListener(e -> showCard(card));
        bar.add(b);
    }

    /** Shows a screen and refreshes its data first. */
    private void showCard(String name) {
        switch (name) {
            case "products":  products.refresh();  break;
            case "recommend": recommend.refresh(); break;
            case "orders":    orders.refresh();    break;
            case "admin":     if (admin != null) admin.refresh(); break;
            default: break;
        }
        cards.show(center, name);
    }

    private void logout() {
        if (Dialogs.confirm(this, "Do you want to log out?")) {
            dispose();
            new LoginFrame(store);
        }
    }

    private void exitApp() {
        if (Dialogs.confirm(this, "Do you want to exit the application?")) {
            System.exit(0);
        }
    }
}
