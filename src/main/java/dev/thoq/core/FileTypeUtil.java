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

import java.io.File;

public final class FileTypeUtil {
    private FileTypeUtil() {}

    public static boolean isGleamFile(File f) {
        return f != null && f.getName().toLowerCase().endsWith(".gleam");
    }

    public static boolean isErlangFile(File f) {
        if(f == null)
            return false;

        String n = f.getName().toLowerCase();
        return n.endsWith(".erl") || n.endsWith(".hrl") || n.endsWith(".xrl") || n.endsWith(".yrl");
    }
}
