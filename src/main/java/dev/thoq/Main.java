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
import dev.thoq.ui.SplashScreen;
import dev.thoq.ui.WelcomeScreen;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Thread.currentThread().setName("Bootstrap");
        Logger.info("GleamStorm starting...");

        if(System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "GleamStorm");
            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        }

        SwingUtilities.invokeLater(() -> {
            Thread.currentThread().setName("Bootstrap");
            Logger.info("Loading GUI...");

            SplashScreen splashScreen = new SplashScreen();
            splashScreen.setVisible(true);

            SwingWorker<WelcomeScreen, Void> worker = new SwingWorker<>() {
                @Override
                protected WelcomeScreen doInBackground() {
                    Thread.currentThread().setName("Bootstrap");

                    Logger.info("Loading WelcomeScreen...");

                    return new WelcomeScreen();
                }

                @Override
                protected void done() {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Thread.currentThread().setName("Bootstrap");
                            WelcomeScreen welcome = get();

                            splashScreen.setVisible(false);
                            splashScreen.dispose();

                            Logger.info("Showing WelcomeScreen...");
                            welcome.setVisible(true);
                        } catch(Exception e) {
                            Logger.error("Failed to load WelcomeScreen: " + e.getMessage());
                            splashScreen.setVisible(false);
                            splashScreen.dispose();
                        }
                    });
                }
            };

            worker.execute();
        });
    }
}
