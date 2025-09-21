package it.glucotrack.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MailHelper {

    public static void openMailClient(String to) {
        if (!Desktop.isDesktopSupported()) {
            System.err.println("Desktop non supportato sul sistema");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.MAIL)) {
            System.err.println("L'azione MAIL non è supportata");
            return;
        }

        try {
            System.out.println("Eccomi qui ad aprire la mail per: " + to);
            URI mailto = new URI("mailto:" + to);
            desktop.mail(mailto);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
