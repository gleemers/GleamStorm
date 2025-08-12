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

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class CustomScrollBarUI extends BasicScrollBarUI {
    private final Color thumbColor;
    private final Color trackColor;

    public CustomScrollBarUI(Color thumbColor, Color trackColor) {
        this.thumbColor = thumbColor;
        this.trackColor = trackColor;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(thumbColor);
        g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 10, 10);
        g2.dispose();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(trackColor);
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2.dispose();
    }
}
