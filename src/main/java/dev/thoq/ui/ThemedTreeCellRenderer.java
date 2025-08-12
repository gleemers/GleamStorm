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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;

public class ThemedTreeCellRenderer extends DefaultTreeCellRenderer {
    private final Color bgColor;
    private final Color fgColor;
    private final Color selectionColor;

    public ThemedTreeCellRenderer(Color bgColor, Color fgColor, Color selectionColor) {
        super();
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        this.selectionColor = selectionColor;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        setBackgroundNonSelectionColor(bgColor);
        setBackgroundSelectionColor(selectionColor);
        setTextNonSelectionColor(fgColor);
        setTextSelectionColor(fgColor);

        if(value instanceof DefaultMutableTreeNode node) {
            Object uo = node.getUserObject();
            if(uo instanceof File file) {
                setText(file.getName().isEmpty() ? file.getPath() : file.getName());
            }
        }
        return c;
    }
}
