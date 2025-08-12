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

package dev.thoq.ui;

import java.awt.Color;

@SuppressWarnings("unused")
public final class Theme {
    public enum Mode { DARK, LIGHT }

    private static Mode mode = Mode.DARK;

    private Theme() {}

    public static Mode getMode() {
        return mode;
    }

    public static void setMode(Mode m) {
        if (m != null) mode = m;
    }

    public static void toggle() {
        mode = (mode == Mode.DARK) ? Mode.LIGHT : Mode.DARK;
    }

    public static boolean isDark() {
        return mode == Mode.DARK;
    }

    public static Color bg() { // window/content background
        return isDark() ? new Color(18, 18, 18) : new Color(250, 250, 250);
    }

    public static Color fg() { // primary foreground text
        return isDark() ? new Color(230, 230, 230) : new Color(32, 32, 32);
    }

    public static Color menuBg() { // panels / inputs / button bg
        return isDark() ? new Color(24, 24, 24) : new Color(242, 242, 242);
    }

    public static Color border() {
        return isDark() ? new Color(30, 30, 30) : new Color(235, 235, 235);
    }

    public static Color accent() { // subtle accents (lines, outlines, selection)
        return isDark() ? new Color(160, 160, 160) : new Color(128, 128, 128);
    }

    public static Color selection() {
        return isDark() ? new Color(70, 70, 70) : new Color(210, 210, 210);
    }

    public static Color scrollbarThumb() {
        return isDark() ? new Color(80, 80, 80) : new Color(200, 200, 200);
    }

    public static Color scrollbarTrack() {
        return isDark() ? new Color(30, 30, 30) : new Color(235, 235, 235);
    }
}
