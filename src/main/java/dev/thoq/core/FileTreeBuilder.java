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

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.util.Arrays;

public final class FileTreeBuilder {
    private FileTreeBuilder() {
    }

    public static DefaultMutableTreeNode buildNode(File file) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file) {
            @Override
            public String toString() {
                File f = (File) getUserObject();
                return f.getName();
            }
        };

        if(file.isDirectory())
            buildFileTreeNode(file, node);

        return node;
    }

    public static void buildFileTreeNode(File file, DefaultMutableTreeNode node) {
        File[] children = file.listFiles();

        if(children == null)
            return;

        Arrays.sort(children, (a, b) -> {
            if(a.isDirectory() && !b.isDirectory())
                return -1;

            if(!a.isDirectory() && b.isDirectory())
                return 1;

            return a.getName().compareToIgnoreCase(b.getName());
        });

        for(File child : children)
            node.add(buildNode(child));
    }
}
