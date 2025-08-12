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

package dev.thoq.core;

import dev.thoq.integration.erlang.ErlangSyntaxHighlighter;
import dev.thoq.integration.gleam.GleamLSPClient;
import dev.thoq.integration.gleam.GleamSyntaxHighlighter;
import dev.thoq.integration.highlight.ISyntaxHighlighter;
import dev.thoq.integration.lsp.RDiagnostic;
import dev.thoq.ui.CustomScrollBarUI;
import dev.thoq.ui.CustomTitleBar;
import dev.thoq.ui.SquigglePainter;
import dev.thoq.ui.ThemedTreeCellRenderer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@SuppressWarnings({"CallToPrintStackTrace", "deprecation", "MagicConstant"})
public class GleamStorm extends JFrame {
    private JTextPane textPane;
    private JLabel statusLabel;
    private File currentFile;
    private File currentFolder;
    private boolean isDarkTheme = true;
    private ISyntaxHighlighter syntaxHighlighter;
    private GleamLSPClient lspClient;
    private JScrollPane scrollPane;
    private JPanel mainPanel;
    private CustomTitleBar titleBar;
    private JSplitPane splitPane;
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private Timer highlightTimer;
    private Timer suggestionTimer;
    private JPopupMenu suggestionPopup;
    private JList<String> suggestionList;
    private DefaultListModel<String> suggestionModel;
    private final java.util.List<Object> diagnosticHighlights = new ArrayList<>();
    private final Highlighter.HighlightPainter errorPainter = new SquigglePainter(new Color(255, 64, 64), 2, 4);
    private final Highlighter.HighlightPainter warningPainter = new SquigglePainter(new Color(255, 165, 0), 2, 4);
    private Timer hoverTimer;
    private Point lastMousePoint;
    private int lastHoverOffset = -1;
    private String hoverText = null;
    private static final Color DARK_BG = new Color(18, 18, 18);
    private static final Color DARK_FG = new Color(230, 230, 230);
    private static final Color DARK_MENU_BG = new Color(24, 24, 24);
    private static final Color DARK_BORDER = new Color(30, 30, 30);
    private static final Color DARK_SELECTION = new Color(64, 64, 64);
    private static final Color DARK_SCROLL = new Color(72, 72, 72);
    private static final Color DARK_ACCENT = new Color(160, 160, 160);
    private static final Color LIGHT_BG = new Color(250, 250, 250);
    private static final Color LIGHT_FG = new Color(32, 32, 32);
    private static final Color LIGHT_MENU_BG = new Color(242, 242, 242);
    private static final Color LIGHT_BORDER = new Color(235, 235, 235);
    private static final Color LIGHT_SELECTION = new Color(220, 220, 220);
    private static final Color LIGHT_SCROLL = new Color(200, 200, 200);
    private static final Color LIGHT_ACCENT = new Color(128, 128, 128);
    private static final String[] GLEAM_KEYWORDS = {
            "as", "assert", "case", "const", "external", "fn", "if", "import",
            "let", "opaque", "pub", "todo", "try", "type", "use"
    };

    private static final String[] GLEAM_FUNCTIONS = {
            "map", "filter", "fold", "length", "head", "tail", "append", "prepend",
            "reverse", "sort", "zip", "unzip", "flatten", "concat", "join", "split"
    };

    public GleamStorm() {
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

        loadIcon();
        initializeComponents();
        setupUI();
        applyTheme();
        initializeLSP();
    }

    private void loadIcon() {
        try {
            BufferedImage img = null;
            InputStream iconStream = getClass().getResourceAsStream("/icon.png");
            if(iconStream != null) {
                img = ImageIO.read(iconStream);
                iconStream.close();
                System.out.println("Icon loaded successfully from /icon.png");
            } else {
                iconStream = getClass().getResourceAsStream("/resources/icon.png");
                if(iconStream != null) {
                    img = ImageIO.read(iconStream);
                    iconStream.close();
                    System.out.println("Icon loaded from /resources/icon.png");
                }
            }

            if(img != null) {
                setIconImage(img);

                try {
                    Taskbar taskbar = Taskbar.getTaskbar();
                    taskbar.setIconImage(img);
                } catch(UnsupportedOperationException | SecurityException ignored) {

                }
            } else {
                System.out.println("Warning: Icon not found. Place icon.png in src/main/resources/");
            }
        } catch(Exception e) {
            System.out.println("Warning: Could not load icon: " + e.getMessage());
        }
    }

    private void initializeComponents() {
        setTitle("GleamStorm - Gleam Code Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        textPane = new JTextPane() {
            @Override
            public String getToolTipText(MouseEvent e) {
                return (hoverText != null && !hoverText.isEmpty()) ? hoverText : null;
            }
        };
        textPane.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
        textPane.setEditable(true);
        textPane.setMargin(new Insets(15, 15, 15, 15));

        initializeSuggestions();

        textPane.setSelectionColor(isDarkTheme ? DARK_SELECTION : LIGHT_SELECTION);

        syntaxHighlighter = new GleamSyntaxHighlighter();

        highlightTimer = new Timer(300, _ -> {
            syntaxHighlighter.highlight(textPane);
            if(lspClient != null && lspClient.isConnected() && FileTypeUtil.isGleamFile(currentFile)) {
                lspClient.checkSyntax(currentFile.getAbsolutePath(), textPane.getText());
            }
        });
        highlightTimer.setRepeats(false);

        suggestionTimer = new Timer(200, _ -> performSuggestionCheck());
        suggestionTimer.setRepeats(false);

        ToolTipManager.sharedInstance().registerComponent(textPane);
        hoverTimer = new Timer(350, _ -> triggerHoverLookup());
        hoverTimer.setRepeats(false);
        textPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                lastMousePoint = e.getPoint();
                hoverTimer.restart();
            }
        });

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                highlightTimer.restart();
                checkForSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                highlightTimer.restart();
                hideSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                highlightTimer.restart();
            }
        });

        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(suggestionPopup.isVisible()) {
                    handleSuggestionNavigation(e);
                } else if(e.getKeyCode() == KeyEvent.VK_TAB) {
                    e.consume();
                    try {
                        textPane.getDocument().insertString(textPane.getCaretPosition(), "  ", null);
                    } catch(BadLocationException ex) {
                        ex.printStackTrace();
                    }
                } else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE) {
                    e.consume();
                    performSuggestionCheck();
                }
            }
        });

        int menuMask;
        try {
            menuMask = (Integer) Toolkit.class.getMethod("getMenuShortcutKeyMaskEx").invoke(Toolkit.getDefaultToolkit());
        } catch(Exception ex) {
            try {
                menuMask = (int) Toolkit.class.getMethod("getMenuShortcutKeyMask").invoke(Toolkit.getDefaultToolkit());
            } catch(Exception ex2) {
                menuMask = InputEvent.CTRL_DOWN_MASK;
            }
        }

        InputMap im = textPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = textPane.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask), "gs-save");
        am.put("gs-save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask | KeyEvent.SHIFT_DOWN_MASK), "gs-format");
        am.put("gs-format", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formatDocument();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuMask), "gs-open");
        am.put("gs-open", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        statusLabel = new JLabel(" Ready");
        statusLabel.setBorder(new EmptyBorder(8, 15, 8, 15));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        lspClient = new GleamLSPClient();
    }


    private void setHighlighterForFile(File f) {
        if(FileTypeUtil.isErlangFile(f)) {
            if(!(syntaxHighlighter instanceof ErlangSyntaxHighlighter)) {
                syntaxHighlighter = new ErlangSyntaxHighlighter();
            }
        } else {
            if(!(syntaxHighlighter instanceof GleamSyntaxHighlighter)) {
                syntaxHighlighter = new GleamSyntaxHighlighter();
            }
        }
    }

    private void initializeSuggestions() {
        suggestionModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(8);
        suggestionList.setFocusable(false);
        suggestionList.setFixedCellHeight(22);
        suggestionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
                lbl.setOpaque(true);
                return lbl;
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 1) {
                    applySuggestion();
                }
            }
        });

        suggestionPopup = new JPopupMenu();
        suggestionPopup.setFocusable(false);
        suggestionPopup.setOpaque(true);
        JScrollPane suggestionScroll = new JScrollPane(suggestionList);
        suggestionScroll.setPreferredSize(new Dimension(240, 160));
        suggestionScroll.setFocusable(false);
        suggestionScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        suggestionPopup.add(suggestionScroll);
    }

    private void checkForSuggestions() {
        if(suggestionTimer.isRunning()) {
            suggestionTimer.restart();
        } else {
            suggestionTimer.start();
        }
    }

    private void performSuggestionCheck() {
        try {
            String currentWord = getCurrentWordAtCaret();
            if(!currentWord.isEmpty() && currentWord.length() <= 32) {
                SwingUtilities.invokeLater(() -> showSuggestionsForWord(currentWord));
            } else {
                SwingUtilities.invokeLater(this::hideSuggestions);
            }
        } catch(Exception e) {
            hideSuggestions();
        }
    }

    private String getCurrentWordAtCaret() {
        try {
            int caretPos = textPane.getCaretPosition();
            String text = textPane.getText();
            if(caretPos == 0 || caretPos > text.length()) return "";

            int start = caretPos;
            while(start > 0) {
                char c = text.charAt(start - 1);
                if(Character.isLetterOrDigit(c) || c == '_') {
                    start--;
                } else {
                    break;
                }
            }
            return text.substring(start, caretPos);
        } catch(Exception ignored) {
            return "";
        }
    }

    private void showSuggestionsForWord(String prefix) {
        suggestionModel.clear();
        List<String> matches = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();

        for(String keyword : GLEAM_KEYWORDS) {
            if(keyword.startsWith(lowerPrefix) && !keyword.equalsIgnoreCase(lowerPrefix)) {
                matches.add(keyword);
            }
        }
        for(String func : GLEAM_FUNCTIONS) {
            if(func.startsWith(lowerPrefix) && !func.equalsIgnoreCase(lowerPrefix)) {
                matches.add(func);
            }
        }

        try {
            LinkedHashSet<String> unique = getStrings(lowerPrefix);

            java.util.HashSet<String> existing = new java.util.HashSet<>();
            for(String m : matches) existing.add(m.toLowerCase());
            for(String u : unique) {
                if(existing.add(u.toLowerCase())) {
                    matches.add(u);
                }
                if(matches.size() >= 50) break;
            }
        } catch(Exception ignored) {
        }

        if(!matches.isEmpty()) {
            matches.sort((a, b) -> {
                int cmp = Integer.compare(a.length(), b.length());
                if(cmp != 0) return cmp;
                return a.compareToIgnoreCase(b);
            });
            for(String match : matches) {
                suggestionModel.addElement(match);
            }
            showSuggestions();
        } else {
            hideSuggestions();
        }
    }

    private LinkedHashSet<String> getStrings(String lowerPrefix) {
        String text = textPane.getText();
        String[] words = text.split("[^A-Za-z0-9_]+");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for(String w : words) {
            if(w == null) continue;
            String lw = w.toLowerCase();
            if(lw.length() < 2) continue;
            if(lw.equals(lowerPrefix)) continue;
            if(lw.startsWith(lowerPrefix)) {
                unique.add(w);
            }
        }
        return unique;
    }

    private void showSuggestions() {
        if(suggestionModel.getSize() > 0) {
            try {
                Rectangle caretRect = textPane.modelToView(textPane.getCaretPosition());
                suggestionPopup.show(textPane,
                        caretRect.x,
                        caretRect.y + caretRect.height);
                suggestionList.setSelectedIndex(0);

                SwingUtilities.invokeLater(() -> textPane.requestFocus());

            } catch(BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private void hideSuggestions() {
        if(suggestionPopup.isVisible()) {
            suggestionPopup.setVisible(false);
        }
    }

    private void handleSuggestionNavigation(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_UP) {
            e.consume();
            int selected = suggestionList.getSelectedIndex();
            if(selected > 0) {
                suggestionList.setSelectedIndex(selected - 1);
            }
        } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
            e.consume();
            int selected = suggestionList.getSelectedIndex();
            if(selected < suggestionModel.getSize() - 1) {
                suggestionList.setSelectedIndex(selected + 1);
            }
        } else if(e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
            e.consume();
            applySuggestion();
        } else if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            e.consume();
            hideSuggestions();
            textPane.requestFocus();
        }
    }

    private void applySuggestion() {
        String selected = suggestionList.getSelectedValue();
        if(selected != null) {
            try {
                int caretPos = textPane.getCaretPosition();
                String text = textPane.getText();

                int wordStart = caretPos;
                while(wordStart > 0) {
                    char c = text.charAt(wordStart - 1);
                    if(Character.isLetterOrDigit(c) || c == '_') {
                        wordStart--;
                    } else {
                        break;
                    }
                }

                String completion = selected.substring(caretPos - wordStart);
                textPane.getDocument().insertString(caretPos, completion, null);

                hideSuggestions();
                textPane.requestFocus();

            } catch(BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupUI() {
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        JToolBar toolBar = createToolBar();
        titleBar = new CustomTitleBar(this, "GleamStorm");
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        boolean useScreenMenuBar = "true".equalsIgnoreCase(System.getProperty("apple.laf.useScreenMenuBar"));

        scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(createCustomBorder());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No folder");
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.setCellRenderer(new ThemedTreeCellRenderer(
                isDarkTheme ? DARK_BG : LIGHT_BG,
                isDarkTheme ? DARK_FG : LIGHT_FG,
                isDarkTheme ? DARK_SELECTION : LIGHT_SELECTION
        ));
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if(path != null) {
                        Object last = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                        if(last instanceof File file) {
                            if(file.isFile()) {
                                openSpecificFile(file);
                            }
                        }
                    }
                }
            }
        });
        JScrollPane treeScroll = new JScrollPane(fileTree);
        treeScroll.setBorder(createCustomBorder());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, scrollPane);
        splitPane.setDividerLocation(260);
        splitPane.setBorder(null);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.add(titleBar);

        if(!(isMac && useScreenMenuBar)) {
            northStack.add(toolBar);
        }

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(northStack, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private Border createCustomBorder() {
        Color borderColor = isDarkTheme ? DARK_BORDER : LIGHT_BORDER;

        Border rounded = new LineBorder(borderColor, 1, true);
        Border padding = new EmptyBorder(2, 2, 2, 2);
        return new CompoundBorder(rounded, padding);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(createCustomMenuBorder());

        JMenu newMenu = new JMenu("New");
        JMenuItem newFileItem = new JMenuItem("New File");
        JMenuItem newFolderItem = new JMenuItem("New Folder...");
        newFileItem.addActionListener(_ -> newFile());
        newFolderItem.addActionListener(_ -> newFolder());
        newMenu.add(newFileItem);
        newMenu.add(newFolderItem);

        JMenu fileMenu = getJMenu();

        JMenu editMenu = getMenu();

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem formatItem = new JMenuItem("Format Document");
        JMenuItem checkItem = new JMenuItem("Check Syntax");

        formatItem.addActionListener(_ -> formatDocument());
        checkItem.addActionListener(_ -> checkSyntax());

        toolsMenu.add(formatItem);
        toolsMenu.add(checkItem);

        menuBar.add(newMenu);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(toolsMenu);

        return menuBar;
    }

    private JMenu getMenu() {
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem cutItem = new JMenuItem("Cut");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");

        cutItem.addActionListener(_ -> textPane.cut());
        copyItem.addActionListener(_ -> textPane.copy());
        pasteItem.addActionListener(_ -> textPane.paste());

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        return editMenu;
    }

    private JMenu getJMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem openFolderItem = new JMenuItem("Open Folder...");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As");
        JMenuItem exitItem = new JMenuItem("Exit");

        newItem.addActionListener(_ -> newFile());
        openItem.addActionListener(_ -> openFile());
        openFolderItem.addActionListener(_ -> openFolder());
        saveItem.addActionListener(_ -> saveFile());
        saveAsItem.addActionListener(_ -> saveAsFile());
        exitItem.addActionListener(_ -> System.exit(0));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(openFolderItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        return fileMenu;
    }

    private Border createCustomMenuBorder() {
        Color borderColor = isDarkTheme ? DARK_BORDER : LIGHT_BORDER;
        return new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(2, 2, 2, 2)
        );
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(new CompoundBorder(
                new LineBorder(isDarkTheme ? DARK_BORDER : LIGHT_BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));

        JButton newBtn = createStyledButton("New");
        JButton openBtn = createStyledButton("Open");
        JButton openFolderBtn = createStyledButton("Open Folder");
        JButton saveBtn = createStyledButton("Save");
        JButton themeBtn = createStyledButton("Theme");
        JButton testLSPBtn = createStyledButton("Test LSP");

        newBtn.addActionListener(_ -> newFile());
        openBtn.addActionListener(_ -> openFile());
        openFolderBtn.addActionListener(_ -> openFolder());
        saveBtn.addActionListener(_ -> saveFile());
        themeBtn.addActionListener(_ -> toggleTheme());
        testLSPBtn.addActionListener(_ -> testLSPConnection());

        toolBar.add(newBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(openBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(openFolderBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(saveBtn);
        toolBar.add(Box.createHorizontalStrut(15));
        toolBar.add(themeBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(testLSPBtn);

        return toolBar;
    }

    private void testLSPConnection() {
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> statusLabel.setText(" Testing LSP connection..."));

            System.out.println("\n" + "=".repeat(50));
            System.out.println("GLEAM LSP CONNECTION TEST");
            System.out.println("=".repeat(50));

            boolean result = lspClient.testConnection();

            SwingUtilities.invokeLater(() -> {
                if(result) {
                    statusLabel.setText(" LSP Connection: SUCCESS - Syntax checking available");
                    JOptionPane.showMessageDialog(this,
                            """
                                    LSP connection successful.
                                    - Syntax checking is now available
                                    - Document formatting is enabled
                                    - Check console for technical details""",
                            "LSP Test - Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    statusLabel.setText(" LSP Connection: FAILED - Running in offline mode");
                    JOptionPane.showMessageDialog(this,
                            """
                                    LSP connection failed.
                                    The editor will work but without LSP features.
                                    
                                    To enable LSP features:
                                    - Install Gleam: brew install gleam
                                    - Verify: gleam --version
                                    - Run from a directory with gleam.toml
                                    
                                    Check console for detailed error information.""",
                            "LSP Test - Failed", JOptionPane.WARNING_MESSAGE);
                }
            });
        }).start();
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setPreferredSize(new Dimension(70, 30));

        Color accentColor = isDarkTheme ? DARK_ACCENT : LIGHT_ACCENT;

        button.setBorder(new CompoundBorder(
                new LineBorder(accentColor, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));

        return button;
    }

    private void applyTheme() {
        Color bgColor = isDarkTheme ? DARK_BG : LIGHT_BG;
        Color fgColor = isDarkTheme ? DARK_FG : LIGHT_FG;
        Color menuBgColor = isDarkTheme ? DARK_MENU_BG : LIGHT_MENU_BG;
        Color borderColor = isDarkTheme ? DARK_BORDER : LIGHT_BORDER;
        Color scrollColor = isDarkTheme ? DARK_SCROLL : LIGHT_SCROLL;
        Color selectionColor = isDarkTheme ? DARK_SELECTION : LIGHT_SELECTION;

        mainPanel.setBackground(bgColor);
        textPane.setBackground(bgColor);
        textPane.setForeground(fgColor);
        textPane.setCaretColor(fgColor);
        textPane.setSelectionColor(selectionColor);

        statusLabel.setBackground(menuBgColor);
        statusLabel.setForeground(fgColor);
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(8, 15, 8, 15)
        ));

        scrollPane.setBackground(bgColor);
        scrollPane.getViewport().setBackground(bgColor);
        scrollPane.setBorder(createCustomBorder());

        suggestionList.setBackground(menuBgColor);
        suggestionList.setForeground(fgColor);
        suggestionList.setSelectionBackground(selectionColor);
        suggestionPopup.setBackground(menuBgColor);
        suggestionPopup.setBorder(new LineBorder(borderColor, 1, true));

        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI(scrollColor, bgColor));
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI(scrollColor, bgColor));

        JMenuBar menuBar = getJMenuBar();
        if(menuBar != null) {

            UIManager.put("MenuBar.background", menuBgColor);
            UIManager.put("Menu.background", menuBgColor);
            UIManager.put("MenuItem.background", menuBgColor);
            UIManager.put("PopupMenu.background", menuBgColor);
            UIManager.put("MenuItem.selectionBackground", selectionColor);
            UIManager.put("MenuItem.foreground", fgColor);
            UIManager.put("Menu.foreground", fgColor);
            UIManager.put("PopupMenu.foreground", fgColor);

            UIManager.put("ToolTip.background", menuBgColor);
            UIManager.put("ToolTip.foreground", fgColor);
            UIManager.put("ToolTip.border", new LineBorder(borderColor, 1, true));

            UIManager.put("TextField.background", menuBgColor);
            UIManager.put("TextField.foreground", fgColor);
            UIManager.put("TextField.caretForeground", fgColor);
            UIManager.put("TextField.border", new LineBorder(borderColor, 1, true));

            UIManager.put("PasswordField.background", menuBgColor);
            UIManager.put("PasswordField.foreground", fgColor);
            UIManager.put("PasswordField.caretForeground", fgColor);
            UIManager.put("PasswordField.border", new LineBorder(borderColor, 1, true));

            UIManager.put("TextArea.background", menuBgColor);
            UIManager.put("TextArea.foreground", fgColor);
            UIManager.put("TextArea.caretForeground", fgColor);
            UIManager.put("TextArea.border", new LineBorder(borderColor, 1, true));

            UIManager.put("ComboBox.background", menuBgColor);
            UIManager.put("ComboBox.foreground", fgColor);
            UIManager.put("ComboBox.border", new LineBorder(borderColor, 1, true));

            menuBar.setBackground(menuBgColor);
            menuBar.setOpaque(true);
            menuBar.setBorder(createCustomMenuBorder());
            for(int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if(menu == null) continue;
                menu.setBackground(menuBgColor);
                menu.setForeground(fgColor);
                menu.setOpaque(true);
                JPopupMenu popup = menu.getPopupMenu();
                if(popup != null) {
                    popup.setOpaque(true);
                    popup.setBackground(menuBgColor);
                    popup.setBorder(new LineBorder(borderColor, 1, true));
                }
                styleMenuItems(menu, menuBgColor, fgColor);
            }
            SwingUtilities.updateComponentTreeUI(menuBar);
        }

        if(titleBar != null) {
            Color accent = isDarkTheme ? DARK_ACCENT : LIGHT_ACCENT;
            titleBar.applyTheme(menuBgColor, fgColor, accent);
        }

        if(fileTree != null) {
            fileTree.setCellRenderer(new ThemedTreeCellRenderer(bgColor, fgColor, selectionColor));
            fileTree.setBackground(bgColor);
            fileTree.setForeground(fgColor);
            fileTree.setBorder(new EmptyBorder(4, 8, 4, 4));
        }
        if(splitPane != null) {
            splitPane.setBackground(menuBgColor);
            splitPane.setDividerSize(6);
        }

        Component[] components = mainPanel.getComponents();
        for(Component comp : components) {
            if(comp instanceof JToolBar toolBar) {
                toolBar.setBackground(menuBgColor);
                toolBar.setBorder(new CompoundBorder(
                        new LineBorder(borderColor, 1, true),
                        new EmptyBorder(8, 8, 8, 8)
                ));
                styleToolBar(toolBar, menuBgColor, fgColor);
            }
        }

        syntaxHighlighter.setTheme(isDarkTheme);
        syntaxHighlighter.highlight(textPane);

        repaint();
    }

    private void styleMenuItems(JMenu menu, Color bgColor, Color fgColor) {
        for(int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if(item != null) {
                item.setBackground(bgColor);
                item.setForeground(fgColor);
                item.setOpaque(true);
            }
        }
    }

    private void styleToolBar(JToolBar toolBar, Color bgColor, Color fgColor) {
        Component[] components = toolBar.getComponents();
        for(Component comp : components) {
            if(comp instanceof JButton button) {
                button.setBackground(bgColor);
                button.setForeground(fgColor);

                Color accentColor = isDarkTheme ? DARK_ACCENT : LIGHT_ACCENT;
                button.setBorder(new CompoundBorder(
                        new LineBorder(accentColor, 1, true),
                        new EmptyBorder(4, 8, 4, 8)
                ));
                button.setOpaque(true);
            }
        }
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
        statusLabel.setText(isDarkTheme ? " Dark theme enabled" : " Light theme enabled");
    }

    private void newFile() {
        textPane.setText("");
        currentFile = null;
        setTitle("GleamStorm - New File");
        statusLabel.setText(" New file created");
        textPane.requestFocus();
    }

    private void openFolder() {
        dev.thoq.ui.SimplePathPicker picker = new dev.thoq.ui.SimplePathPicker(this, dev.thoq.ui.SimplePathPicker.Mode.DIRECTORY, null);
        java.io.File sel = picker.pick();
        if(sel != null) {
            openProject(sel);
        }
    }

    public void openProject(File folder) {
        if(folder == null || !folder.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Invalid folder selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        currentFolder = folder;
        setTitle("GleamStorm - " + currentFolder.getName());
        statusLabel.setText(" Opened folder: " + currentFolder.getAbsolutePath());
        refreshFileTree();

        try {
            if(lspClient != null) {
                lspClient.disconnect();
                lspClient.setWorkspaceRoot(currentFolder.getAbsolutePath());
                initializeLSP();
            }
        } catch(Exception ignored) {
        }

        File[] files = currentFolder.listFiles((_, name) -> name.endsWith(".gleam"));
        if(files != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(files.length).append(" .gleam file(s):\n\n");
            for(File f : files) sb.append("- ").append(f.getName()).append('\n');
            if(files.length > 0) {
                JOptionPane.showMessageDialog(this, sb.toString(), "Folder Contents", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void openFile() {
        dev.thoq.ui.SimplePathPicker picker = new dev.thoq.ui.SimplePathPicker(this, dev.thoq.ui.SimplePathPicker.Mode.OPEN_FILE, new String[]{"gleam", "erl", "hrl", "xrl", "yrl"});
        java.io.File sel = picker.pick();
        if(sel != null) {
            currentFile = sel;
            try {
                String content = new String(Files.readAllBytes(currentFile.toPath()));
                textPane.setText(content);
                clearDiagnosticHighlights();
                setTitle("GleamStorm - " + currentFile.getName());
                statusLabel.setText(" Opened: " + currentFile.getName());
                textPane.requestFocus();

                setHighlighterForFile(currentFile);
                syntaxHighlighter.setTheme(isDarkTheme);
                syntaxHighlighter.highlight(textPane);

                if(currentFolder == null && lspClient != null) {
                    File parent = currentFile.getParentFile();
                    if(parent != null && parent.isDirectory()) {
                        try {
                            lspClient.disconnect();
                            lspClient.setWorkspaceRoot(parent.getAbsolutePath());
                            initializeLSP();
                        } catch(Exception ignored) {
                        }
                    }
                }

                assert lspClient != null;
                if(FileTypeUtil.isGleamFile(currentFile) && lspClient.isConnected()) {
                    lspClient.openDocument(currentFile.getAbsolutePath(), content);
                }
            } catch(IOException e) {
                JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        if(currentFile == null) {
            saveAsFile();
            return;
        }

        try {
            Files.write(currentFile.toPath(), textPane.getText().getBytes());
            statusLabel.setText(" Saved: " + currentFile.getName());

            if(FileTypeUtil.isGleamFile(currentFile) && lspClient.isConnected()) {
                lspClient.saveDocument(currentFile.getAbsolutePath(), textPane.getText());
            }
        } catch(IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAsFile() {
        dev.thoq.ui.SimplePathPicker picker = new dev.thoq.ui.SimplePathPicker(this, dev.thoq.ui.SimplePathPicker.Mode.SAVE_FILE, new String[]{"gleam"});
        java.io.File sel = picker.pick();
        if(sel != null) {
            currentFile = sel.getName().endsWith(".gleam") ? sel : new File(sel.getAbsolutePath() + ".gleam");
            saveFile();
            setTitle("GleamStorm - " + currentFile.getName());
        }
    }

    private void formatDocument() {
        if(FileTypeUtil.isGleamFile(currentFile) && lspClient.isConnected()) {
            String formatted = lspClient.formatDocument(currentFile.getAbsolutePath());
            if(formatted != null) {
                textPane.setText(formatted);
                statusLabel.setText(" Document formatted");
            }
        } else if(FileTypeUtil.isErlangFile(currentFile)) {
            statusLabel.setText(" Formatting not available for Erlang in this editor");
        } else {
            statusLabel.setText(" LSP not connected - cannot format");
        }
    }

    private void checkSyntax() {
        if(lspClient.isConnected() && currentFile != null) {
            boolean isValid = lspClient.checkSyntax(currentFile.getAbsolutePath(), textPane.getText());
            statusLabel.setText(isValid ? " Syntax check passed" : " Syntax errors found");
        } else {
            statusLabel.setText(" LSP not connected - cannot check syntax");
        }
    }


    private void refreshFileTree() {
        if(currentFolder != null && fileTree != null) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(currentFolder.getName());
            dev.thoq.core.FileTreeBuilder.buildFileTreeNode(currentFolder, root);
            treeModel.setRoot(root);
            fileTree.setRootVisible(true);

            if(fileTree.getRowCount() > 0) fileTree.expandRow(0);
        }
    }

    private void openSpecificFile(File file) {
        try {
            currentFile = file;
            String content = new String(Files.readAllBytes(file.toPath()));
            textPane.setText(content);
            clearDiagnosticHighlights();
            setTitle("GleamStorm - " + currentFile.getName());
            statusLabel.setText(" Opened: " + currentFile.getName());
            textPane.requestFocus();

            setHighlighterForFile(currentFile);
            syntaxHighlighter.setTheme(isDarkTheme);
            syntaxHighlighter.highlight(textPane);

            if(FileTypeUtil.isGleamFile(currentFile) && lspClient.isConnected()) {
                lspClient.openDocument(currentFile.getAbsolutePath(), content);
            }
        } catch(IOException e) {
            JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void newFolder() {
        if(currentFolder == null) {
            JOptionPane.showMessageDialog(this, "Open a folder first to create a subfolder.", "No Folder", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = JOptionPane.showInputDialog(this, "New folder name:", "New Folder", JOptionPane.PLAIN_MESSAGE);
        if(name == null) return;
        name = name.trim();
        if(name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Folder name cannot be empty.", "Invalid Name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File newDir = new File(currentFolder, name);
        if(newDir.exists()) {
            JOptionPane.showMessageDialog(this, "Folder already exists.", "Exists", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(newDir.mkdirs()) {
            statusLabel.setText(" Created folder: " + newDir.getAbsolutePath());
            refreshFileTree();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create folder.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeLSP() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);

                if(lspClient.connect()) {
                    lspClient.setDiagnosticsListener((uri, diagnostics) -> {
                        if(currentFile == null) return;
                        String fileUri = "file://" + currentFile.getAbsolutePath().replace("\\", "/");
                        if(!fileUri.equals(uri)) return;
                        SwingUtilities.invokeLater(() -> applyDiagnostics(diagnostics));
                    });

                    Thread.sleep(2000);

                    SwingUtilities.invokeLater(() -> {
                        if(lspClient.isConnected()) {
                            statusLabel.setText(" Connected to Gleam LSP");
                        } else {
                            statusLabel.setText(" LSP connection unstable");
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() ->
                            statusLabel.setText(" Failed to connect to Gleam LSP"));
                }
            } catch(Exception e) {
                SwingUtilities.invokeLater(() ->
                        statusLabel.setText(" LSP connection error: " + e.getMessage()));
            }
        }).start();
    }

    private void applyDiagnostics(java.util.List<RDiagnostic> diagnostics) {
        Highlighter hl = textPane.getHighlighter();
        for(Object tag : diagnosticHighlights) {
            try {
                hl.removeHighlight(tag);
            } catch(Exception ignored) {
            }
        }
        diagnosticHighlights.clear();

        if(diagnostics == null || diagnostics.isEmpty()) {
            statusLabel.setText(" No diagnostics");
            return;
        }

        String text;
        try {
            text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
        } catch(BadLocationException e) {
            return;
        }

        String firstMsg = null;
        for(RDiagnostic d : diagnostics) {
            int start = TextPositionUtil.offsetFromLineChar(text, d.startLine(), d.startChar());
            int end = TextPositionUtil.offsetFromLineChar(text, d.endLine(), d.endChar());
            if(end < start) {
                int tmp = start;
                start = end;
                end = tmp;
            }
            try {
                Highlighter.HighlightPainter painter = (d.severity() != null && d.severity() == 1) ? errorPainter : warningPainter;
                Object tag = hl.addHighlight(Math.max(0, start), Math.min(end, text.length()), painter);
                diagnosticHighlights.add(tag);
                if(firstMsg == null && d.message() != null && !d.message().isEmpty()) {
                    firstMsg = d.message();
                }
            } catch(BadLocationException ignored) {
            }
        }

        if(firstMsg != null) {
            statusLabel.setText(" " + firstMsg);
        } else {
            statusLabel.setText(" Diagnostics: " + diagnostics.size());
        }
    }


    private void clearDiagnosticHighlights() {
        Highlighter hl = textPane.getHighlighter();
        for(Object tag : diagnosticHighlights) {
            try {
                hl.removeHighlight(tag);
            } catch(Exception ignored) {
            }
        }
        diagnosticHighlights.clear();
    }

    private void triggerHoverLookup() {
        if(lspClient == null || !lspClient.isConnected() || lastMousePoint == null || !FileTypeUtil.isGleamFile(currentFile))
            return;
        int offset = textPane.viewToModel(lastMousePoint);
        if(offset < 0) return;
        if(offset == lastHoverOffset) return;
        lastHoverOffset = offset;
        String text;
        try {
            text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
        } catch(BadLocationException e) {
            return;
        }
        int[] lc = TextPositionUtil.lineCharFromOffset(text, offset);
        lspClient.requestHover(currentFile.getAbsolutePath(), lc[0], lc[1], result -> SwingUtilities.invokeLater(() -> {
            hoverText = (result != null && !result.trim().isEmpty()) ? HoverHtmlFormatter.formatHoverHtml(result) : null;
            textPane.setToolTipText(hoverText);
        }));
    }

}