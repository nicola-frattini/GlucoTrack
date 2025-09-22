package it.glucotrack.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/*
* MAIL HELPER
*/



public class MailHelper {

    public static void openMailClient(String to) {
        if (!Desktop.isDesktopSupported()) {
            System.err.println("Desktop not supported");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.MAIL)) {
            System.err.println("Mail action is not supported");
            return;
        }

        try {
            URI mailto = new URI("mailto:" + to);
            desktop.mail(mailto);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
