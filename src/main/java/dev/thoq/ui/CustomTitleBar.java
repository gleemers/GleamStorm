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
    public CustomTitleBar(JFrame frame, String title) {
        super(new BorderLayout());

        JLabel titleLabel = new JLabel(title);
        JPanel controlsPanel = getControlsPanel(frame);

        setBorder(new CompoundBorder(new LineBorder(new Color(0, 0, 0, 0), 1, true), new EmptyBorder(6, 10, 6, 10)));
        setOpaque(true);

        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

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

    private static JPanel getControlsPanel(JFrame frame) {
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controlsPanel.setOpaque(false);

        JButton minimizeBtn = new JButton("–");
        JButton maximizeBtn = new JButton("+");
        JButton closeBtn = new JButton("x");

        Dimension btnSize = new Dimension(28, 20);

        minimizeBtn.setPreferredSize(btnSize);
        maximizeBtn.setPreferredSize(btnSize);
        closeBtn.setPreferredSize(btnSize);

        minimizeBtn.addActionListener(_ -> frame.setState(Frame.ICONIFIED));
        maximizeBtn.addActionListener(_ -> frame.setState(Frame.MAXIMIZED_BOTH));
        closeBtn.addActionListener(_ -> System.exit(0));

        controlsPanel.add(minimizeBtn);
        if(frame.isResizable()) controlsPanel.add(maximizeBtn);
        controlsPanel.add(closeBtn);

        return controlsPanel;
    }
}
