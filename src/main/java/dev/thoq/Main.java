/*
 * Copyright (c) GleamStorm 2025.
 *
 *  This file is a part of the GleamStorm IDE, an IDE for
 *  the Gleam programming language.
 *
 * GleamStorm GitHub: https://github.com/gleemers/gleamstorm.git
 *
 * GleamStorm does NOT come with a warranty.
 *
 * GleamStorm is licensed under the MIT license.
 * Whilst contributing, modifying, or distributing, make sure
 * you agree to the MIT license.
 * If you did not receive a copy of the MIT license,
 * you may obtain one here:
 * MIT License: https://opensource.org/license/mit
 */

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
