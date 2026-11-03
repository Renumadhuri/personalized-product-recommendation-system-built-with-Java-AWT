import app.DataStore;
import gui.LoginFrame;
import java.io.IOException;

/** Entry point: loads the data, then opens the login screen. */
public class MinisoApp {
    public static void main(String[] args) {
        DataStore store = new DataStore();
        try {
            store.load();
        } catch (IOException e) {
            System.err.println("Could not load data: " + e.getMessage());
            System.exit(1);
        }
        new LoginFrame(store).setVisible(true);
    }
}

