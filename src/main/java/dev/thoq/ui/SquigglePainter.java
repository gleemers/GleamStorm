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

import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.View;
import java.awt.*;

public class SquigglePainter extends LayeredHighlighter.LayerPainter implements Highlighter.HighlightPainter {
    private final Color color;
    private final int amplitude;
    private final int wavelength;

    public SquigglePainter(Color color, int amplitude, int wavelength) {
        this.color = color;
        this.amplitude = Math.max(1, amplitude);
        this.wavelength = Math.max(2, wavelength);
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
        paintSquiggle(g, bounds);
    }

    @Override
    public Shape paintLayer(Graphics g, int p0, int p1, Shape viewBounds, JTextComponent c, View view) {
        paintSquiggle(g, viewBounds);
        return viewBounds;
    }

    private void paintSquiggle(Graphics g, Shape s) {
        if(s == null) return;
        Rectangle r = (s instanceof Rectangle) ? (Rectangle) s : s.getBounds();
        Graphics2D g2 = (Graphics2D) g;
        Color old = g2.getColor();
        Object aa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        int baselineY = r.y + r.height - 2;
        int x = r.x;
        int end = r.x + r.width;
        int step = Math.max(2, wavelength / 2);
        boolean up = true;
        int prevX = x;
        int prevY = baselineY;
        for(int i = x; i <= end; i += step) {
            int ny = baselineY + (up ? -amplitude : amplitude);
            g2.drawLine(prevX, prevY, i, ny);
            prevX = i;
            prevY = ny;
            up = !up;
        }
        g2.drawLine(prevX, prevY, end, baselineY);
        g2.setColor(old);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }
}
