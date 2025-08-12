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

import dev.thoq.core.GleamStorm;
import dev.thoq.log.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("SameParameterValue")
public class WelcomeScreen extends JFrame {
    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final CustomTitleBar titleBar = new CustomTitleBar(this, "GleamStorm");
    private final CardLayout centerLayout = new CardLayout();
    private final JPanel centerPanel = new JPanel(centerLayout);

    public WelcomeScreen() {
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                try {
                    setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                } catch(UnsupportedOperationException ignored) {
                }
            }
        });

        setTitle("GleamStorm - Welcome");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        loadIcon();

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.add(titleBar);

        JPanel home = new JPanel(new BorderLayout());
        home.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel logo = buildLogoLabel();
        JPanel logoWrap = new JPanel(new BorderLayout());
        logoWrap.add(logo, BorderLayout.NORTH);
        logoWrap.setOpaque(false);

        JPanel actions = buildActionsPanel();

        home.add(logoWrap, BorderLayout.WEST);
        home.add(actions, BorderLayout.CENTER);

        JPanel newProject = buildNewProjectPanel();
        JPanel clonePanel = buildClonePanel();

        centerPanel.add(home, "home");
        centerPanel.add(newProject, "new");
        centerPanel.add(clonePanel, "clone");
        centerLayout.show(centerPanel, "home");

        JLabel footer = new JLabel("Welcome to GleamStorm", SwingConstants.LEFT);
        footer.setBorder(new CompoundBorder(new LineBorder(getBorderColor(), 1, true), new EmptyBorder(10, 14, 10, 14)));

        mainPanel.add(northStack, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(footer, BorderLayout.SOUTH);

        add(mainPanel);
        applyTheme();
    }

    private void loadIcon() {
        try {
            BufferedImage img = null;
            InputStream iconStream = getClass().getResourceAsStream("/icon.png");

            if(iconStream != null) {
                img = ImageIO.read(iconStream);
                iconStream.close();
            }

            if(img != null) {
                setIconImage(img);
                try {
                    Taskbar.getTaskbar().setIconImage(img);
                } catch(Throwable ignored) {
                }
            }
        } catch(Exception ignored) {
        }
    }

    private JLabel buildLogoLabel() {
        Image image = null;

        try(InputStream is = getClass().getResourceAsStream("/gleamstorm_logo_text.png")) {
            if(is != null) {
                image = ImageIO.read(is);
            }
        } catch(IOException ignored) {
        }

        if(image == null) {
            File f = new File("brand/gleamstorm_logo_text.png");

            if(f.exists()) {
                try {
                    image = ImageIO.read(f);
                } catch(IOException ignored) {
                }
            }
        }

        if(image == null) {
            try(InputStream is = getClass().getResourceAsStream("/icon.png")) {
                if(is != null) image = ImageIO.read(is);
            } catch(IOException ignored) {
            }
        }

        JLabel label;
        if(image != null) {
            Image scaled = image.getScaledInstance(320, -1, Image.SCALE_SMOOTH);
            label = new JLabel(new ImageIcon(scaled));
        } else {
            label = new JLabel("GleamStorm", SwingConstants.LEFT);
            label.setFont(new Font("SansSerif", Font.BOLD, 28));
        }

        label.setBorder(new EmptyBorder(10, 10, 20, 10));

        return label;
    }

    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1;

        JButton newBtn = buildPrimaryButton("New Gleam Project");
        JButton openBtn = buildSecondaryButton("Open");
        JButton cloneBtn = buildSecondaryButton("Clone from Git");
        JButton themeBtn = buildSecondaryButton("Toggle Theme");

        newBtn.addActionListener(_ -> showNewProjectUI());
        openBtn.addActionListener(_ -> onOpenProject());
        cloneBtn.addActionListener(_ -> centerLayout.show(centerPanel, "clone"));
        themeBtn.addActionListener(_ -> {
            Theme.toggle();
            applyTheme();
        });

        gbc.gridy = 0;
        panel.add(newBtn, gbc);

        gbc.gridy = 1;
        panel.add(openBtn, gbc);

        gbc.gridy = 2;
        panel.add(cloneBtn, gbc);

        gbc.gridy = 3;
        panel.add(themeBtn, gbc);

        return panel;
    }

    private JButton buildPrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(getAccentColor(), 1, true), new EmptyBorder(12, 16, 12, 16)));

        return b;
    }

    private JButton buildSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 14));
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(getAccentColor(), 1, true), new EmptyBorder(10, 14, 10, 14)));

        return b;
    }

    private void applyTheme() {
        Color bg = Theme.bg();
        Color fg = Theme.fg();
        Color menu = Theme.menuBg();
        Color accent = Theme.accent();

        mainPanel.setBackground(bg);
        titleBar.applyTheme(menu, fg, accent);

        styleTree(getContentPane(), bg, fg, menu, accent);

        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    private void styleTree(Component comp, Color bg, Color fg, Color menu, Color accent) {
        if(comp == null) return;

        if(comp instanceof JPanel)
            comp.setBackground(bg);

        if(comp instanceof JLabel)
            comp.setForeground(fg);

        if(comp instanceof JButton b) {
            b.setBackground(menu);
            b.setForeground(fg);
            b.setOpaque(true);
            b.setBorder(new CompoundBorder(new LineBorder(accent, 1, true), new EmptyBorder(10, 14, 10, 14)));
        }

        if(comp instanceof JTextField textField) {
            textField.setBackground(menu);
            textField.setForeground(fg);
            textField.setCaretColor(fg);
            textField.setOpaque(true);
            textField.setBorder(new CompoundBorder(new LineBorder(getBorderColor(), 1, true), new EmptyBorder(6, 8, 6, 8)));
        }

        if(comp instanceof JTextArea textArea) {
            textArea.setBackground(menu);
            textArea.setForeground(fg);
            textArea.setCaretColor(fg);
            textArea.setOpaque(true);
            textArea.setBorder(new CompoundBorder(new LineBorder(getBorderColor(), 1, true), new EmptyBorder(6, 8, 6, 8)));
        }

        if(comp instanceof JPasswordField passwordField) {
            passwordField.setBackground(menu);
            passwordField.setForeground(fg);
            passwordField.setCaretColor(fg);
            passwordField.setOpaque(true);
            passwordField.setBorder(new CompoundBorder(new LineBorder(getBorderColor(), 1, true), new EmptyBorder(6, 8, 6, 8)));
        }

        if(comp instanceof JComboBox<?> comboBox) {
            comboBox.setBackground(menu);
            comboBox.setForeground(fg);
            comboBox.setOpaque(true);
            comboBox.setBorder(new CompoundBorder(new LineBorder(getBorderColor(), 1, true), new EmptyBorder(4, 6, 4, 6)));
        }

        if(comp instanceof JScrollPane scrollPane) {
            scrollPane.setBackground(bg);
            scrollPane.getViewport().setBackground(bg);
            scrollPane.setBorder(new CompoundBorder(new LineBorder(getBorderColor(), 1, true), new EmptyBorder(2, 2, 2, 2)));
        }

        if(comp instanceof JComponent)
            comp.setForeground(fg);

        if(comp instanceof Container) {
            for(Component child : ((Container) comp).getComponents())
                styleTree(child, bg, fg, menu, accent);
        }
    }

    private Color getAccentColor() {
        return Theme.accent();
    }

    private Color getBorderColor() {
        return Theme.border();
    }

    private void showNewProjectUI() {
        centerLayout.show(centerPanel, "new");
    }

    private JPanel buildNewProjectPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        JLabel title = new JLabel("Create New Gleam Project");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridwidth = 3;
        panel.add(title, gbc);
        gbc.gridwidth = 1;

        gbc.gridy++;
        panel.add(new JLabel("Project name:"), gbc);
        JTextField nameField = new JTextField();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(nameField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(Box.createHorizontalStrut(1), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Location (parent):"), gbc);
        JTextField dirField = new JTextField();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(dirField, gbc);
        JButton browse = new JButton("Browse...");

        browse.addActionListener(_ -> {
            dev.thoq.ui.SimplePathPicker picker = new dev.thoq.ui.SimplePathPicker(this, dev.thoq.ui.SimplePathPicker.Mode.DIRECTORY, null);
            java.io.File sel = picker.pick();

            if(sel != null)
                dirField.setText(sel.getAbsolutePath());
        });

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(browse, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        JCheckBox initCheck = new JCheckBox("Use gleam init (create folder if needed)");
        initCheck.setOpaque(false);
        panel.add(initCheck, gbc);
        gbc.gridwidth = 1;

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancel = new JButton("Cancel");
        JButton create = new JButton("Create Project");
        buttonRow.add(cancel);
        buttonRow.add(create);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        panel.add(buttonRow, gbc);

        cancel.addActionListener(_ -> centerLayout.show(centerPanel, "home"));
        create.addActionListener(_ -> {
            String name = nameField.getText().trim();
            String parent = dirField.getText().trim();

            if(name.isEmpty() || parent.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please provide project name and location.", "Missing info", JOptionPane.WARNING_MESSAGE);
                return;
            }

            File parentDir = new File(parent);
            if(!parentDir.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Invalid parent directory.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File projectDir = new File(parentDir, name);
            if(initCheck.isSelected()) {
                if(!projectDir.exists()) {
                    if(!projectDir.mkdirs()) {
                        JOptionPane.showMessageDialog(this, "Failed to create project directory.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                runCommandAsync(new String[]{"gleam", "init", "--yes"}, projectDir, "Initializing Gleam project...", success -> {
                    if(success) openInEditor(projectDir);
                    else
                        JOptionPane.showMessageDialog(this, errorMessageFor("gleam"), "Gleam Error", JOptionPane.ERROR_MESSAGE);
                });
            } else {
                if(projectDir.exists()) {
                    JOptionPane.showMessageDialog(this, "Destination already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                runCommandAsync(new String[]{"gleam", "new", name}, parentDir, "Creating Gleam project...", success -> {
                    if(success) openInEditor(projectDir);
                    else
                        JOptionPane.showMessageDialog(this, errorMessageFor("gleam"), "Gleam Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });

        return panel;
        }

        private JPanel buildClonePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        JLabel title = new JLabel("Clone From Git");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridwidth = 3;
        panel.add(title, gbc);
        gbc.gridwidth = 1;

        gbc.gridy++;
        panel.add(new JLabel("Git URL:"), gbc);
        JTextField urlField = new JTextField();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(urlField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(Box.createHorizontalStrut(1), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Location (parent):"), gbc);
        JTextField parentField = new JTextField();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(parentField, gbc);
        JButton browse = new JButton("Browse...");
        browse.addActionListener(_ -> {
            dev.thoq.ui.SimplePathPicker picker = new dev.thoq.ui.SimplePathPicker(this, dev.thoq.ui.SimplePathPicker.Mode.DIRECTORY, null);
            java.io.File sel = picker.pick();
            if(sel != null) parentField.setText(sel.getAbsolutePath());
        });

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(browse, gbc);

        gbc.gridx = 0;
        gbc.gridy++;

        panel.add(new JLabel("Folder name (optional):"), gbc);
        JTextField folderField = new JTextField();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(folderField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(Box.createHorizontalStrut(1), gbc);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton back = new JButton("Back");
        JButton clone = new JButton("Clone");

        buttonRow.add(back);
        buttonRow.add(clone);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        panel.add(buttonRow, gbc);

        back.addActionListener(_ -> centerLayout.show(centerPanel, "home"));
        clone.addActionListener(_ -> {
            String url = urlField.getText().trim();
            String parent = parentField.getText().trim();
            String name = folderField.getText().trim();

            if(url.isEmpty() || parent.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please provide Git URL and parent folder.", "Missing info", JOptionPane.WARNING_MESSAGE);
                return;
            }

            File parentDir = new File(parent);
            if(!parentDir.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Invalid parent directory.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add("git");
            cmd.add("clone");
            cmd.add(url);
            File clonedDir;

            if(!name.isEmpty()) {
                cmd.add(name);
                clonedDir = new File(parentDir, name);
            } else {
                String guess = url.replaceAll("/+", "/");
                int i = guess.lastIndexOf('/');
                String tail = i >= 0 ? guess.substring(i + 1) : "repo";
                if(tail.endsWith(".git")) tail = tail.substring(0, tail.length() - 4);
                clonedDir = new File(parentDir, tail);
            }

            runCommandAsync(cmd.toArray(new String[0]), parentDir, "Cloning repository...", success -> {
                if(success && clonedDir.isDirectory()) openInEditor(clonedDir);
                else JOptionPane.showMessageDialog(this, errorMessageFor("git"), "Git Error", JOptionPane.ERROR_MESSAGE);
            });
        });

        return panel;
        }

        private void onOpenProject() {
        dev.thoq.ui.SimplePathPicker picker = new dev.thoq.ui.SimplePathPicker(this, dev.thoq.ui.SimplePathPicker.Mode.DIRECTORY, null);
        java.io.File sel = picker.pick();

        if(sel != null)
            openInEditor(sel);
    }

    private void runCommandAsync(String[] cmd, File workDir, String status, java.util.function.Consumer<Boolean> done) {
        Logger.info("Starting command: " + String.join(" ", cmd) + " in " + workDir.getAbsolutePath());
        JDialog progress = buildProgressDialog(status);

        new Thread(() -> {
            boolean ok = runCommand(cmd, workDir);
            SwingUtilities.invokeLater(() -> {
                progress.dispose();
                Logger.info("Command finished (exit status: " + (ok ? "0" : "!=0") + "): " + String.join(" ", cmd));
                done.accept(ok);
            });
        }, "cmd-runner").start();
        progress.setVisible(true);
    }

    private boolean runCommand(String[] cmd, File workDir) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir);
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();

            new Thread(() -> {
                try {
                    p.getOutputStream().write('\n');
                    p.getOutputStream().flush();
                    p.getOutputStream().close();
                } catch(IOException ignored) {
                }
            }, "cmd-stdin").start();

            Thread outReader = new Thread(() -> {
                try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while((line = br.readLine()) != null) {
                        Logger.debug(line);
                    }
                } catch(IOException e) {
                    Logger.warn("Failed reading process output: " + e.getMessage());
                }
            }, "cmd-stdout");
            outReader.start();

            boolean finished = p.waitFor(5, TimeUnit.MINUTES);
            if(!finished) {
                Logger.error("Command timed out: " + String.join(" ", cmd));
                p.destroyForcibly();
                return false;
            }

            int code = p.exitValue();
            return code == 0;
        } catch(IOException | InterruptedException e) {
            Logger.error("Command failed: " + String.join(" ", cmd), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String errorMessageFor(String tool) {
        if("gleam".equals(tool)) {
            return """
                    Failed to run Gleam.
                    
                    Make sure Gleam is installed and available in PATH.
                    - macOS: brew install gleam
                    - Check: gleam --version""";
        } else if("git".equals(tool)) {
            return "Failed to run Git.\n\nPlease install Git and ensure it's available in PATH.";
        }
        return "Command failed.";
    }

    private JDialog buildProgressDialog(String text) {
        JDialog d = new JDialog(this, true);
        d.setUndecorated(true);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new CompoundBorder(new LineBorder(getBorderColor(), 1, true), new EmptyBorder(16, 20, 16, 20)));
        p.setBackground(Theme.menuBg());
        JLabel l = new JLabel(text);
        l.setForeground(Theme.fg());
        p.add(l, BorderLayout.CENTER);
        d.getContentPane().add(p);
        d.pack();
        d.setLocationRelativeTo(this);

        return d;
    }

    private void openInEditor(File folder) {
        Logger.info("Opening project in editor: " + folder.getAbsolutePath());
        GleamStorm app = new GleamStorm();
        app.setVisible(true);
        app.openProject(folder);
        dispose();
    }
}
