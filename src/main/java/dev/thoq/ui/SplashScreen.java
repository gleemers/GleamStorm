package dev.thoq.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

public class SplashScreen extends JFrame {
    public SplashScreen() {
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                try {
                    setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                } catch(UnsupportedOperationException ignored) {
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 45));
        panel.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0), 2));

        JLabel titleLabel = new JLabel("GleamStorm", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(50, 50, 20, 50));

        JLabel loadingLabel = new JLabel("Loading Modules...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        loadingLabel.setForeground(new Color(150, 150, 150));
        loadingLabel.setBorder(BorderFactory.createEmptyBorder(20, 50, 50, 50));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(loadingLabel, BorderLayout.SOUTH);

        add(panel);
        setSize(400, 200);
        setLocationRelativeTo(null);
    }
}
