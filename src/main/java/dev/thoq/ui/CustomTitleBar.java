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
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CustomTitleBar extends JPanel {
    private final JLabel titleLabel;
    private final JPanel controlsPanel;

    public CustomTitleBar(JFrame frame, String title) {
        super(new BorderLayout());

        setOpaque(true);
        setBorder(new CompoundBorder(new LineBorder(Theme.accent(), 1, true), new EmptyBorder(6, 10, 6, 10)));

        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controlsPanel.setOpaque(false);

        JButton minimizeBtn = new JButton("–");
        JButton closeBtn = new JButton("×");
        Dimension btnSize = new Dimension(28, 20);
        minimizeBtn.setPreferredSize(btnSize);
        closeBtn.setPreferredSize(btnSize);
        minimizeBtn.addActionListener(_ -> frame.setState(Frame.ICONIFIED));
        closeBtn.addActionListener(_ -> System.exit(0));

        controlsPanel.add(minimizeBtn);
        controlsPanel.add(closeBtn);

        MouseAdapter dragger = new MouseAdapter() {
            Point offset;

            @Override
            public void mousePressed(MouseEvent e) {
                offset = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = e.getLocationOnScreen();
                frame.setLocation(p.x - offset.x, p.y - offset.y);
            }
        };
        addMouseListener(dragger);
        addMouseMotionListener(dragger);

        add(titleLabel, BorderLayout.WEST);
        add(controlsPanel, BorderLayout.EAST);
    }

    public void applyTheme(Color background, Color foreground, Color accent) {
        setBackground(background);
        setBorder(new CompoundBorder(new LineBorder(accent, 1, true), new EmptyBorder(6, 10, 6, 10)));

        titleLabel.setForeground(foreground);

        for(Component c : controlsPanel.getComponents()) {
            if(c instanceof JButton button) {
                button.setBackground(background);
                button.setForeground(foreground);
                button.setFocusPainted(false);
                button.setBorder(new CompoundBorder(new LineBorder(accent, 1, true), new EmptyBorder(2, 8, 2, 8)));
                button.setOpaque(true);
            }
        }
    }
}
