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
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("CallToPrintStackTrace")
public class Terminal {
    private JPanel terminalPanel;
    private JTextPane terminalOutput;
    private JTextField terminalInput;
    private Process currentProcess;
    private boolean terminalVisible = false;
    public JSplitPane mainSplitPane;
    private StyledDocument terminalDoc;
    private static final Map<String, Color> ANSI_COLORS = new HashMap<>();

    static {
        ANSI_COLORS.put("30", Color.BLACK);
        ANSI_COLORS.put("31", new Color(205, 49, 49));
        ANSI_COLORS.put("32", new Color(13, 188, 121));
        ANSI_COLORS.put("33", new Color(229, 229, 16));
        ANSI_COLORS.put("34", new Color(36, 114, 200));
        ANSI_COLORS.put("35", new Color(188, 63, 188));
        ANSI_COLORS.put("36", new Color(17, 168, 205));
        ANSI_COLORS.put("37", Color.WHITE);
        ANSI_COLORS.put("39", Color.WHITE);
    }

    private void appendToTerminalWithColor(String text, Color color) {
        try {
            Style style = terminalOutput.addStyle("temp", null);
            StyleConstants.setForeground(style, color);
            StyleConstants.setFontFamily(style, "JetBrains Mono");
            StyleConstants.setFontSize(style, 12);

            terminalDoc.insertString(terminalDoc.getLength(), text, style);
            terminalOutput.setCaretPosition(terminalDoc.getLength());
        } catch(BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public void cleanupTerminal() {
        if(currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
        }
    }

    public void appendToTerminalWithAnsi(String text) {
        Pattern ansiPattern = Pattern.compile("\u001B\\[(\\d+)(;\\d+)*m");

        Matcher matcher = ansiPattern.matcher(text);

        Color currentColor = Color.WHITE;
        int lastEnd = 0;

        while(matcher.find()) {
            if(matcher.start() > lastEnd) {
                String beforeText = text.substring(lastEnd, matcher.start());
                if(!beforeText.isEmpty()) {
                    appendToTerminalWithColor(beforeText, currentColor);
                }
            }

            String colorCode = matcher.group(1);
            currentColor = ANSI_COLORS.getOrDefault(colorCode, Color.WHITE);

            lastEnd = matcher.end();
        }

        if(lastEnd < text.length()) {
            String remainingText = text.substring(lastEnd);
            remainingText = remainingText.replaceAll("\u001B\\[\\?25[lh]", "");
            remainingText = remainingText.replaceAll("\u001B\\[2K", "");
            remainingText = remainingText.replaceAll("\u001B\\[\\dK", "");

            if(!remainingText.isEmpty()) {
                appendToTerminalWithColor(remainingText, currentColor);
            }
        }
    }

    public void executeCommand(File currentFolder) {
        String command = terminalInput.getText().trim();
        if(command.isEmpty()) return;

        appendToTerminalWithColor(command + "\n", Color.WHITE);
        terminalInput.setText("");

        if(command.equals("clear")) {
            try {
                terminalDoc.remove(0, terminalDoc.getLength());
                appendToTerminalWithColor("] ", Color.WHITE);
            } catch(BadLocationException ex) {
                ex.printStackTrace();
            }
            return;
        }

        new Thread(() -> {
            try {
                String workingDir = currentFolder != null ? currentFolder.getAbsolutePath() : System.getProperty("user.home");
                ProcessBuilder pb = new ProcessBuilder();

                if(System.getProperty("os.name").toLowerCase().contains("windows")) {
                    pb.command("cmd", "/c", command);
                } else {
                    pb.command("sh", "-c", command);
                }

                pb.directory(new File(workingDir));
                pb.redirectErrorStream(true);

                currentProcess = pb.start();

                try(BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while((line = reader.readLine()) != null && currentProcess.isAlive()) {
                        final String outputLine = line;
                        SwingUtilities.invokeLater(() -> appendToTerminalWithAnsi(outputLine + "\n"));
                    }
                }

                int exitCode = currentProcess.waitFor();
                SwingUtilities.invokeLater(() -> {
                    if(exitCode != 0) {
                        appendToTerminalWithColor("Process exited with code: " + exitCode + "\n", Color.RED);
                    }
                    appendToTerminalWithColor("$ ", Color.WHITE);
                });

            } catch(Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    appendToTerminalWithColor("Error: " + ex.getMessage() + "\n", Color.RED);
                    appendToTerminalWithColor("$ ", Color.WHITE);
                });
            } finally {
                currentProcess = null;
            }
        }).start();
    }

    public void toggleTerminal(JTextPane textPane) {
        if (!terminalVisible) {
            mainSplitPane.setBottomComponent(terminalPanel);
            mainSplitPane.setDividerLocation(mainSplitPane.getHeight() - 250);
            mainSplitPane.setResizeWeight(0.7);
            terminalVisible = true;

            SwingUtilities.invokeLater(() -> {
                terminalInput.requestFocusInWindow();
                terminalInput.grabFocus();
            });
        } else {
            mainSplitPane.setBottomComponent(null);
            mainSplitPane.revalidate();
            terminalVisible = false;
            cleanupTerminal();
            textPane.requestFocus();
        }
    }

    public void initializeTerminal(File currentFolder, JTextPane textPane) {
        terminalPanel = new JPanel(new BorderLayout());
        terminalPanel.setPreferredSize(new Dimension(0, 200));

        terminalOutput = new JTextPane();
        terminalOutput.setEditable(false);
        terminalOutput.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        terminalOutput.setForeground(Color.WHITE);
        terminalOutput.setCaretColor(Color.WHITE);

        terminalDoc = terminalOutput.getStyledDocument();

        Style defaultStyle = terminalOutput.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);
        StyleConstants.setFontFamily(defaultStyle, "JetBrains Mono");
        StyleConstants.setFontSize(defaultStyle, 12);

        JScrollPane terminalScroll = new JScrollPane(terminalOutput);
        terminalScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        terminalInput = new JTextField();
        terminalInput.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        terminalInput.setForeground(Color.WHITE);
        terminalInput.setCaretColor(Color.WHITE);
        terminalInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        terminalInput.setFocusable(true);
        terminalInput.setEditable(true);
        terminalInput.setEnabled(true);
        terminalInput.addActionListener(_ -> executeCommand(currentFolder));
        terminalInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    if(currentProcess != null && currentProcess.isAlive()) {
                        currentProcess.destroyForcibly();
                        appendToTerminalWithColor("\n^C\n", Color.YELLOW);
                        appendToTerminalWithColor("$ ", Color.WHITE);
                        terminalInput.setText("");
                        e.consume();
                    }
                }
            }
        });

        JPanel terminalHeader = new JPanel(new BorderLayout());
        terminalHeader.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel terminalLabel = new JLabel("Terminal");
        terminalLabel.setForeground(Color.WHITE);
        terminalLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JButton closeBtn = new JButton("×");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBorder(null);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(25, 25));
        closeBtn.addActionListener(_ -> toggleTerminal(textPane));

        terminalHeader.add(terminalLabel, BorderLayout.WEST);
        terminalHeader.add(closeBtn, BorderLayout.EAST);

        terminalPanel.add(terminalHeader, BorderLayout.NORTH);
        terminalPanel.add(terminalScroll, BorderLayout.CENTER);
        terminalPanel.add(terminalInput, BorderLayout.SOUTH);

        String workingDir = currentFolder != null ? currentFolder.getAbsolutePath() : System.getProperty("user.home");
        appendToTerminalWithColor("Terminal initialized in: " + workingDir + "\n", Color.PINK);
        appendToTerminalWithColor("$ ", Color.WHITE);
    }
}
