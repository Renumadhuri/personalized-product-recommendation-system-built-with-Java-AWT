package gui;

import app.AppUser;
import app.DataStore;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/** Modal form to register a new customer, with input validation. */
public class RegisterDialog extends Dialog {
    private final DataStore store;
    private final TextField name = new TextField(24);
    private final TextField email = new TextField(24);
    private final TextField pass = new TextField(24);
    private final TextField confirm = new TextField(24);
    private final TextField age = new TextField(5);
    private final Choice gender = new Choice();
    private final TextField city = new TextField(24);
    private final Label error = new Label("");

    public RegisterDialog(Frame owner, DataStore store) {
        super(owner, "Register New Customer", true);
        this.store = store;
        setLayout(new BorderLayout(10, 10));

        gender.add("Male");
        gender.add("Female");
        gender.add("Other");
        pass.setEchoChar('*');
        confirm.setEchoChar('*');
        error.setForeground(Color.RED);
        error.setPreferredSize(new Dimension(380, 22));

        Panel form = new Panel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.anchor = GridBagConstraints.WEST;
        row(form, g, 0, "Full name:", name);
        row(form, g, 1, "Email:", email);
        row(form, g, 2, "Password:", pass);
        row(form, g, 3, "Confirm password:", confirm);
        row(form, g, 4, "Age:", age);
        row(form, g, 5, "Gender:", gender);
        row(form, g, 6, "City:", city);
        g.gridx = 0; g.gridy = 7; g.gridwidth = 2;
        form.add(error, g);
        add(form, BorderLayout.CENTER);

        Button registerBtn = new Button("Register");
        Button cancelBtn = new Button("Cancel");
        Panel buttons = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        buttons.add(registerBtn);
        buttons.add(cancelBtn);
        add(buttons, BorderLayout.SOUTH);

        registerBtn.addActionListener(e -> register());
        cancelBtn.addActionListener(e -> dispose());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        pack();
        setLocationRelativeTo(owner);
        setVisible(true);   // blocks until closed
    }

    private void row(Panel p, GridBagConstraints g, int y, String label, java.awt.Component c) {
        g.gridwidth = 1;
        g.gridx = 0; g.gridy = y; p.add(new Label(label), g);
        g.gridx = 1; p.add(c, g);
    }

    /** Returns an error message, or null if all inputs are valid. */
    private String checkInputs() {
        String n = name.getText().trim();
        if (!n.matches("[A-Za-z][A-Za-z .'-]{1,39}")) return "Name: 2-40 letters only (no digits or commas)";
        String e = email.getText().trim();
        if (!e.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) return "Enter a valid email address";
        if (store.findUserByEmail(e) != null) return "This email is already registered";
        String p = pass.getText();
        if (p.length() < 6 || !p.matches(".*[A-Za-z].*") || !p.matches(".*\\d.*"))
            return "Password: at least 6 characters, with a letter and a digit";
        if (!p.equals(confirm.getText())) return "Passwords do not match";
        int a;
        try {
            a = Integer.parseInt(age.getText().trim());
        } catch (NumberFormatException ex) {
            return "Age must be a whole number";
        }
        if (a < 5 || a > 100) return "Age must be between 5 and 100";
        String c = city.getText().trim();
        if (!c.matches("[A-Za-z][A-Za-z .'-]{1,29}")) return "City: letters only";
        return null;
    }

    private void register() {
        String problem = checkInputs();
        if (problem != null) {
            error.setText(problem);
            return;
        }
        AppUser u = new AppUser(store.nextUserId(), name.getText().trim(),
                email.getText().trim().toLowerCase(), AppUser.hash(pass.getText()), "CUSTOMER",
                Integer.parseInt(age.getText().trim()), gender.getSelectedItem(), city.getText().trim());
        store.users.add(u);
        try {
            store.saveUsers();
        } catch (IOException ex) {
            store.users.remove(u);
            error.setText("Could not save: " + ex.getMessage());
            return;
        }
        Dialogs.message(this, "Registered", "Account created for " + u.name + ".\nYou can log in now.");
        dispose();
    }
}
