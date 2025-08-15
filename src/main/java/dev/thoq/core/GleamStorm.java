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
import dev.thoq.log.Logger;
import dev.thoq.ui.CustomTitleBar;
import dev.thoq.ui.SimplePathPicker;
import dev.thoq.ui.SquigglePainter;
import dev.thoq.ui.Terminal;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

@SuppressWarnings({"CallToPrintStackTrace", "deprecation", "MagicConstant", "unused"})
public class GleamStorm extends JFrame {
    private JTextPane textPane;
    private JLabel statusLabel;
    private File currentFile;
    private File currentFolder;
    private final boolean isDarkTheme = true;
    private ISyntaxHighlighter syntaxHighlighter;
    private GleamLSPClient lspClient;
    private JTree fileTree;
    private UndoManager undoManager;
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
    private BufferedWriter processWriter;
    private final Terminal terminal = new Terminal();

    private static final String[] GLEAM_KEYWORDS = {
            "as", "assert", "case", "const", "external", "fn", "if", "import",
            "let", "opaque", "pub", "todo", "try", "type", "use"
    };

    private static final String[] GLEAM_FUNCTIONS = {
            "map", "filter", "fold", "length", "head", "tail", "append", "prepend",
            "reverse", "sort", "zip", "unzip", "flatten", "concat", "join", "split"
    };

    public GleamStorm() {
        ToolTipManager.sharedInstance().setDismissDelay(10000);

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setResizable(true);

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
        initializeLSP();
    }

    private void loadIcon() {
        try {
            BufferedImage img;
            InputStream iconStream = getClass().getResourceAsStream("/icon.png");

            assert iconStream != null;
            img = ImageIO.read(iconStream);
            iconStream.close();

            assert img != null;
            setIconImage(img);

            try {
                Taskbar taskbar = Taskbar.getTaskbar();
                taskbar.setIconImage(img);
            } catch(UnsupportedOperationException | SecurityException ignored) {

            }
        } catch(Exception e) {
            Logger.warn("Warning: Could not load icon: " + e.getMessage());
        }
    }

    private void initializeComponents() {
        setTitle("GleamStorm");
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

            for(String match : matches)
                suggestionModel.addElement(match);

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
        CustomTitleBar titleBar = new CustomTitleBar(this, "GleamStorm");
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        boolean useScreenMenuBar = "true".equalsIgnoreCase(System.getProperty("apple.laf.useScreenMenuBar"));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No folder");
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() != 2) return;

                TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());

                if(path == null) return;

                Object last = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();

                if(last instanceof File file) {
                    if(!file.isFile())
                        return;

                    openSpecificFile(file);
                }
            }
        });

        JScrollPane treeScroll = new JScrollPane(fileTree);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, scrollPane);
        horizontalSplit.setDividerLocation(260);
        horizontalSplit.setBorder(null);

        terminal.initializeTerminal(currentFolder, textPane);
        terminal.mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        terminal.mainSplitPane.setTopComponent(horizontalSplit);
        terminal.mainSplitPane.setBorder(null);
        terminal.mainSplitPane.setResizeWeight(1.0);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.add(titleBar);

        if(!(isMac && useScreenMenuBar)) {
            northStack.add(toolBar);
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(northStack, BorderLayout.NORTH);
        mainPanel.add(terminal.mainSplitPane, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu newMenu = new JMenu("New");
        JMenuItem newFileItem = new JMenuItem("New File");
        JMenuItem newFolderItem = new JMenuItem("New Folder...");
        newFileItem.addActionListener(_ -> newFile());
        newFolderItem.addActionListener(_ -> newFolder());
        newMenu.add(newFileItem);
        newMenu.add(newFolderItem);

        JMenu fileMenu = getFileMenu();
        JMenu editMenu = getEditMenu();
        JMenu toolsMenu = getToolsMenu();

        menuBar.add(newMenu);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(toolsMenu);

        return menuBar;
    }

    private JMenu getToolsMenu() {
        JMenu toolsMenu = new JMenu("Tools");

        JMenuItem formatItem = new JMenuItem("Format Document");
        JMenuItem checkItem = new JMenuItem("Check Syntax");
        JMenuItem terminalItem = new JMenuItem("Terminal");

        formatItem.addActionListener(_ -> formatDocument());
        checkItem.addActionListener(_ -> checkSyntax());
        terminalItem.addActionListener(_ -> terminal.toggleTerminal(textPane));

        toolsMenu.add(formatItem);
        toolsMenu.add(checkItem);
        toolsMenu.add(terminalItem);

        return toolsMenu;
    }

    private JMenu getEditMenu() {
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem clearEditsItem = new JMenuItem("Clear edit history");
        JMenuItem cutItem = new JMenuItem("Cut");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");

        cutItem.addActionListener(_ -> cut());
        copyItem.addActionListener(_ -> copy());
        pasteItem.addActionListener(_ -> paste());
        undoItem.addActionListener(_ -> undo());
        redoItem.addActionListener(_ -> redo());
        clearEditsItem.addActionListener(_ -> clearUndoHistory());

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.add(clearEditsItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);

        return editMenu;
    }

    private JMenu getFileMenu() {
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

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton newBtn = createStyledButton("New");
        JButton openBtn = createStyledButton("Open");
        JButton openFolderBtn = createStyledButton("Open Folder");
        JButton saveBtn = createStyledButton("Save");

        newBtn.addActionListener(_ -> newFile());
        openBtn.addActionListener(_ -> openFile());
        openFolderBtn.addActionListener(_ -> openFolder());
        saveBtn.addActionListener(_ -> saveFile());

        toolBar.add(newBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(openBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(openFolderBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(saveBtn);
        toolBar.add(Box.createHorizontalStrut(15));

        return toolBar;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setPreferredSize(new Dimension(70, 30));

        return button;
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
                button.setOpaque(true);
            }
        }
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
        SimplePathPicker picker = new SimplePathPicker(this, SimplePathPicker.Mode.SAVE_FILE, new String[]{"gleam"});
        File sel = picker.pick();
        if(sel == null) return;

        currentFile = sel.getName().endsWith(".gleam") ? sel : new File(sel.getAbsolutePath() + ".gleam");
        saveFile();
        setTitle("GleamStorm - " + currentFile.getName());
    }

    private void undo() {
        try {
            if(!undoManager.canUndo()) return;
            undoManager.undo();
        } catch(CannotUndoException ex) {
            Logger.error("Unable to undo", ex);
            statusLabel.setText(" Unable to undo");
        }
    }

    private void redo() {
        try {
            if(!undoManager.canRedo()) return;

            undoManager.redo();
        } catch(CannotRedoException ex) {
            Logger.error("Unable to redo: " + ex.getMessage());
            statusLabel.setText("Unable to redo: " + ex.getMessage());
        }
    }

    public void clearUndoHistory() {
        undoManager.discardAllEdits();
    }

    private void cut() {
        String selection = textPane.getSelectedText();

        if(selection == null) return;

        textPane.copy();
        textPane.cut();
    }

    private void copy() {
        String selection = textPane.getSelectedText();
        if(selection == null) return;

        StringSelection stringSelection = new StringSelection(selection);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private void paste() {
        StringSelection stringSelection = (StringSelection) Toolkit
                .getDefaultToolkit()
                .getSystemClipboard()
                .getContents(null);

        if(stringSelection == null) return;

        textPane.replaceSelection(stringSelection.toString());
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
            FileTreeBuilder.buildFileTreeNode(currentFolder, root);
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
                if(lspClient.connect()) {
                    lspClient.setDiagnosticsListener((uri, diagnostics) -> {
                        if(currentFile == null) return;
                        String fileUri = "file://" + currentFile.getAbsolutePath().replace("\\", "/");
                        if(!fileUri.equals(uri)) return;
                        SwingUtilities.invokeLater(() -> applyDiagnostics(diagnostics));
                    });

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
