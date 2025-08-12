package dev.thoq;

import dev.thoq.log.Logger;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        Logger.info("GleamStorm starting...");
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "GleamStorm");
            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Logger.info("System LookAndFeel applied: " + UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                Logger.warn("Failed to set LookAndFeel: " + e.getMessage());
            }

            Logger.info("Showing WelcomeScreen...");
            dev.thoq.ui.WelcomeScreen welcome = new dev.thoq.ui.WelcomeScreen();
            welcome.setVisible(true);
        });
    }
}
