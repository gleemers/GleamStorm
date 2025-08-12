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
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimplePathPicker extends JDialog {
    public enum Mode {DIRECTORY, OPEN_FILE, SAVE_FILE}

    private final Mode mode;
    private final String[] extensions;
    private final JTextField pathField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final DefaultListModel<File> listModel = new DefaultListModel<>();
    private final JList<File> list = new JList<>(listModel);
    private File currentDir;
    private File result;

    public SimplePathPicker(JFrame owner, Mode mode, String[] extensions) {
        super(owner, true);

        this.mode = mode;
        this.extensions = extensions;

        setUndecorated(true);
        setSize(700, 500);
        setLocationRelativeTo(owner);

        Color bg = new Color(24, 24, 24);
        Color fg = new Color(230, 230, 230);
        Color border = new Color(40, 40, 40);
        Color accent = new Color(140, 140, 140);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(bg);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);

        pathField.setEditable(false);
        pathField.setBackground(new Color(30, 30, 30));
        pathField.setForeground(fg);
        pathField.setBorder(new LineBorder(border, 1, true));

        JButton upBtn = new JButton("Up");
        upBtn.setBackground(new Color(30, 30, 30));
        upBtn.setForeground(fg);
        upBtn.setBorder(new LineBorder(accent, 1, true));
        upBtn.addActionListener(_ -> navigateUp());
        top.add(pathField, BorderLayout.CENTER);
        top.add(upBtn, BorderLayout.EAST);

        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                File f = (File) value;
                lbl.setText(f.getName().isEmpty() ? f.getAbsolutePath() : f.getName());
                lbl.setBorder(new EmptyBorder(6, 10, 6, 10));
                lbl.setBackground(isSelected ? new Color(60, 60, 60) : new Color(28, 28, 28));
                lbl.setForeground(fg);
                return lbl;
            }
        });

        list.setBackground(new Color(28, 28, 28));
        list.setForeground(fg);
        list.setSelectionBackground(new Color(60, 60, 60));
        list.setBorder(new LineBorder(border, 1, true));
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) openSelected();
            }
        });

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(new LineBorder(border, 1, true));
        sp.getViewport().setBackground(new Color(28, 28, 28));

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);

        if(mode == Mode.SAVE_FILE) {
            nameField.setBackground(new Color(30, 30, 30));
            nameField.setForeground(fg);
            nameField.setBorder(new LineBorder(border, 1, true));
            bottom.add(nameField, BorderLayout.CENTER);
        }

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton select = new JButton(mode == Mode.DIRECTORY ? "Select Folder" : mode == Mode.OPEN_FILE ? "Open" : "Save");

        for(JButton b : Arrays.asList(cancel, select)) {
            b.setBackground(new Color(30, 30, 30));
            b.setForeground(fg);
            b.setBorder(new LineBorder(accent, 1, true));
        }

        cancel.addActionListener(_ -> {
            result = null;
            dispose();
        });

        select.addActionListener(_ -> confirmSelection());
        actions.add(cancel);
        actions.add(select);
        bottom.add(actions, BorderLayout.EAST);

        root.add(top, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        File start = new File(System.getProperty("user.home"));
        if(owner != null && owner.getTitle() != null) setTitle(owner.getTitle());

        setDirectory(start);
    }

    private void setDirectory(File dir) {
        if(dir == null || !dir.isDirectory()) return;

        currentDir = dir;
        pathField.setText(dir.getAbsolutePath());
        listModel.clear();

        List<File> items = new ArrayList<>();
        File[] kids = dir.listFiles();

        if(kids != null) {
            for(File f : kids)
                if(f.isDirectory()) items.add(f);

            if(mode != Mode.DIRECTORY) {
                for(File f : kids)
                    if(f.isFile() && acceptFile(f)) items.add(f);
            }
        }

        items.sort((a, b) -> {
            if(a.isDirectory() && !b.isDirectory())
                return -1;

            if(!a.isDirectory() && b.isDirectory())
                return 1;

            return a.getName().compareToIgnoreCase(b.getName());
        });

        for(File f : items) listModel.addElement(f);
    }

    private boolean acceptFile(File f) {
        if(extensions == null || extensions.length == 0) return true;
        String name = f.getName().toLowerCase();
        for(String ext : extensions) {
            if(name.endsWith("." + ext.toLowerCase())) return true;
        }
        return false;
    }

    private void navigateUp() {
        if(currentDir == null) return;
        File p = currentDir.getParentFile();
        if(p != null) setDirectory(p);
    }

    private void openSelected() {
        File sel = list.getSelectedValue();
        if(sel == null) return;
        if(sel.isDirectory()) setDirectory(sel);
        else if(mode != Mode.DIRECTORY) {
            nameField.setText(sel.getName());
            confirmSelection();
        }
    }

    private void confirmSelection() {
        if(mode == Mode.DIRECTORY) {
            result = currentDir;
        } else if(mode == Mode.OPEN_FILE) {
            File sel = list.getSelectedValue();
            if(sel != null && sel.isFile()) result = sel;
        } else {
            String name = nameField.getText().trim();
            if(name.isEmpty()) return;
            result = new File(currentDir, name);
        }
        if(result != null) dispose();
    }

    public File pick() {
        setVisible(true);
        return result;
    }
}
