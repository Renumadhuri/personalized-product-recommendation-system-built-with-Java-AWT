package gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Simple modal message and confirm dialogs built with AWT. */
public class Dialogs {

    public static void message(Window owner, String title, String text) {
        show(owner, title, text, false);
    }

    /** Returns true if the user pressed Yes. */
    public static boolean confirm(Window owner, String text) {
        return show(owner, "Confirm", text, true);
    }

    private static boolean show(Window owner, String title, String text, boolean ask) {
        final boolean[] result = {false};
        final Dialog d = new Dialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout(10, 10));

        String[] lines = text.split("\n");
        Panel body = new Panel(new GridLayout(lines.length, 1));
        for (String line : lines) body.add(new Label(line));
        d.add(body, BorderLayout.CENTER);

        Panel buttons = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        if (ask) {
            Button yes = new Button("Yes");
            Button no = new Button("No");
            yes.addActionListener(e -> { result[0] = true; d.dispose(); });
            no.addActionListener(e -> d.dispose());
            buttons.add(yes);
            buttons.add(no);
        } else {
            Button ok = new Button("OK");
            ok.addActionListener(e -> d.dispose());
            buttons.add(ok);
        }
        d.add(buttons, BorderLayout.SOUTH);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                d.dispose();
            }
        });
        d.pack();
        d.setLocationRelativeTo(owner);
        d.setVisible(true);   // blocks until the dialog is closed
        return result[0];
    }
}
