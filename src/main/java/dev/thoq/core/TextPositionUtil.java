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

public final class TextPositionUtil {
    private TextPositionUtil() {}

    public static int offsetFromLineChar(String text, int line, int ch) {
        if(text == null)
            return -1;

        int i = 0;
        int currentLine = 0;
        int len = text.length();
        int startOfLine = i;
        int target = startOfLine + ch;

        while(i < len && currentLine < line) {
            char c = text.charAt(i++);

            if(c == '\n')
                currentLine++;
        }

        return Math.min(Math.max(0, target), len);
    }

    public static int[] lineCharFromOffset(String text, int offset) {
        if(text == null)
            return new int[]{0, 0};

        int len = text.length();

        if(offset < 0)
            offset = 0;

        if(offset > len)
            offset = len;

        int line = 0;
        int lastNewline = -1;

        for(int i = 0; i < offset; i++) {
            if(text.charAt(i) != '\n')
                break;

            line++;
            lastNewline = i;
        }

        int ch = offset - (lastNewline + 1);
        return new int[]{line, ch};
    }
}
