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

package dev.thoq.integration.gleam;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GleamSyntaxHighlighter implements dev.thoq.integration.highlight.ISyntaxHighlighter {
    private boolean isDarkTheme = true;

    private static final String[] KEYWORDS = {
            "as", "assert", "case", "const", "external", "fn", "if", "import",
            "let", "opaque", "pub", "todo", "try", "type", "use"
    };

    private static final String[] TYPES = {
        "Int", "Float", "String", "Bool", "List", "Result", "Option",
        "Nil", "True", "False"
    };

    private Pattern keywordPattern;
    private Pattern typePattern;
    private Pattern capitalizedIdentPattern;
    private Pattern stringPattern;
    private Pattern commentPattern;
    private Pattern numberPattern;
    private Pattern operatorPattern;
    private Pattern functionCallPattern;
    private Style keywordStyle;
    private Style typeStyle;
    private Style stringStyle;
    private Style commentStyle;
    private Style numberStyle;
    private Style operatorStyle;
    private Style functionStyle;
    private Style defaultStyle;

    public GleamSyntaxHighlighter() {
        buildPatterns();
        createStyles();
    }

    private void buildPatterns() {
        StringBuilder keywordBuilder = new StringBuilder("\\b(");
        for (int i = 0; i < KEYWORDS.length; i++) {
            keywordBuilder.append(KEYWORDS[i]);
            if (i < KEYWORDS.length - 1) keywordBuilder.append("|");
        }

        keywordBuilder.append(")\\b");
        keywordPattern = Pattern.compile(keywordBuilder.toString());

        StringBuilder typeBuilder = new StringBuilder("\\b(");

        for (int i = 0; i < TYPES.length; i++) {
            typeBuilder.append(TYPES[i]);
            if (i < TYPES.length - 1)
                typeBuilder.append("|");
        }
        typeBuilder.append(")\\b");

        typePattern = Pattern.compile(typeBuilder.toString());
        capitalizedIdentPattern = Pattern.compile("\\b[A-Z][A-Za-z0-9_]*\\b");
        stringPattern = Pattern.compile("\"([^\\\\\"]|\\\\.)*\"");
        commentPattern = Pattern.compile("//.*$", Pattern.MULTILINE);
        numberPattern = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
        operatorPattern = Pattern.compile("[+\\-*/<>=!&|]+");
        functionCallPattern = Pattern.compile("\\b(?:[a-z][a-z0-9_]*\\.)*([a-z][a-z0-9_]*)\\s*\\(");
    }

    private void createStyles() {
        StyleContext context = StyleContext.getDefaultStyleContext();

        defaultStyle = context.getStyle(StyleContext.DEFAULT_STYLE);
        keywordStyle = context.addStyle("keyword", null);
        typeStyle = context.addStyle("type", null);
        stringStyle = context.addStyle("string", null);
        commentStyle = context.addStyle("comment", null);
        numberStyle = context.addStyle("number", null);
        operatorStyle = context.addStyle("operator", null);
        functionStyle = context.addStyle("function", null);

        updateStyleColors();
    }

    private void updateStyleColors() {
        StyleConstants.setForeground(defaultStyle, isDarkTheme ? Color.WHITE : Color.BLACK);
        StyleConstants.setForeground(keywordStyle, getKeywordColor());
        StyleConstants.setForeground(typeStyle, getTypeColor());
        StyleConstants.setForeground(stringStyle, getStringColor());
        StyleConstants.setForeground(commentStyle, getCommentColor());
        StyleConstants.setForeground(numberStyle, getNumberColor());
        StyleConstants.setForeground(operatorStyle, getOperatorColor());
        StyleConstants.setForeground(functionStyle, getFunctionColor());
    }

    public void setTheme(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
        updateStyleColors();
    }

    public void highlight(JTextPane textPane) {
        StyledDocument doc = textPane.getStyledDocument();
        String text;

        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }

        if (text.length() > 10000) {
            return;
        }

        doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

        // Apply non-comment highlights first
        highlightPattern(doc, text, stringPattern, stringStyle);
        highlightPattern(doc, text, keywordPattern, keywordStyle);
        highlightPattern(doc, text, typePattern, typeStyle);
        highlightPattern(doc, text, capitalizedIdentPattern, typeStyle);
        highlightPattern(doc, text, numberPattern, numberStyle);
        highlightPattern(doc, text, operatorPattern, operatorStyle);
        highlightFunctionCalls(doc, text);
        // Comments last so they override any prior styling
        highlightPattern(doc, text, commentPattern, commentStyle);
    }

    private void highlightPattern(StyledDocument doc, String text, Pattern pattern, Style style) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false);
        }
    }

    private void highlightFunctionCalls(StyledDocument doc, String text) {
        Matcher m = functionCallPattern.matcher(text);
        while (m.find()) {
            int start = m.start(1);
            int end = m.end(1);
            String name = text.substring(start, end);
            int lookBack = Math.max(0, start - 5);
            String prefix = text.substring(lookBack, start);
            if (prefix.matches(".*\\bfn\\s+$") || isKeyword(name)) {
                continue;
            }
            doc.setCharacterAttributes(start, end - start, functionStyle, false);
        }
    }

    private boolean isKeyword(String s) {
        for (String k : KEYWORDS) {
            if (k.equals(s)) return true;
        }
        return false;
    }

    private Color getKeywordColor() {
        return isDarkTheme ? new Color(112, 50, 204) : new Color(128, 0, 128);
    }

    private Color getTypeColor() {
        return isDarkTheme ? new Color(78, 201, 176) : new Color(0, 128, 0);
    }

    private Color getStringColor() {
        return isDarkTheme ? new Color(89, 127, 135) : new Color(163, 21, 21);
    }

    private Color getCommentColor() {
        return isDarkTheme ? new Color(128, 128, 128) : new Color(64, 128, 64);
    }

    private Color getNumberColor() {
        return isDarkTheme ? new Color(104, 151, 187) : new Color(0, 0, 255);
    }

    private Color getOperatorColor() {
        return isDarkTheme ? new Color(204, 204, 204) : new Color(64, 64, 64);
    }

    private Color getFunctionColor() {
        return isDarkTheme ? new Color(97, 175, 239) : new Color(0, 102, 204);
    }
}