package gui;

import app.AppUser;
import app.DataStore;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Login screen: 3 attempts, then the Login button is disabled. */
public class LoginFrame extends Frame {
    private final DataStore store;
    private final TextField email = new TextField(24);
    private final TextField password = new TextField(24);
    private final Label status = new Label("", Label.CENTER);
    private final Button loginBtn = new Button("Login");
    private int attempts = 0;

    public LoginFrame(DataStore store) {
        super("MINISO - Login");
        this.store = store;
        setLayout(new BorderLayout(10, 10));

        Label title = new Label("MINISO Recommendation System", Label.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setBackground(new Color(228, 0, 43));
        add(title, BorderLayout.NORTH);

        password.setEchoChar('*');
        status.setForeground(Color.RED);
        status.setPreferredSize(new Dimension(340, 22));

        Panel form = new Panel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; form.add(new Label("Email:"), g);
        g.gridx = 1; form.add(email, g);
        g.gridx = 0; g.gridy = 1; form.add(new Label("Password:"), g);
        g.gridx = 1; form.add(password, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.anchor = GridBagConstraints.CENTER;
        form.add(status, g);
        add(form, BorderLayout.CENTER);

        Button registerBtn = new Button("Register");
        Button exitBtn = new Button("Exit");
        Panel buttons = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttons.add(loginBtn);
        buttons.add(registerBtn);
        buttons.add(exitBtn);
        add(buttons, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> doLogin());
        password.addActionListener(e -> doLogin());          // Enter key in password box
        email.addActionListener(e -> password.requestFocus()); // Enter key in email box
        registerBtn.addActionListener(e -> new RegisterDialog(this, store));
        exitBtn.addActionListener(e -> System.exit(0));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        setSize(480, 330);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void doLogin() {
        String em = email.getText().trim();
        String pw = password.getText();
        if (em.isEmpty() || pw.isEmpty()) {
            status.setText("Please enter email and password");
            return;
        }
        AppUser u = store.findUserByEmail(em);
        if (u != null && u.passwordHash.equals(AppUser.hash(pw))) {
            dispose();
            MainFrame mainFrame = new MainFrame(store, u);
            mainFrame.toFront();
            return;
        }
        attempts++;
        int left = 3 - attempts;
        password.setText("");
        if (left <= 0) {
            loginBtn.setEnabled(false);
            status.setText("Too many failed attempts. Login disabled.");
            Dialogs.message(this, "Locked", "3 failed attempts.\nRestart the program to try again.");
        } else {
            status.setText("Invalid email or password. Attempts left: " + left);
        }
    }
}
