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
package dev.thoq.integration.erlang;

import dev.thoq.integration.highlight.ISyntaxHighlighter;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErlangSyntaxHighlighter implements ISyntaxHighlighter {
    private boolean isDarkTheme = true;

    private static final String[] KEYWORDS = {
            "after", "begin", "case", "catch", "end", "fun", "if", "let", "of", "receive", "try", "when", "andalso", "orelse", "div", "rem", "band", "bor", "bxor", "bnot", "not"
    };

    private Pattern keywordPattern;
    private Pattern atomPattern;
    private Pattern varPattern;
    private Pattern stringPattern;
    private Pattern commentPattern;
    private Pattern numberPattern;
    private Pattern operatorPattern;
    private Pattern functionCallPattern;

    private Style keywordStyle;
    private Style atomStyle;
    private Style varStyle;
    private Style stringStyle;
    private Style commentStyle;
    private Style numberStyle;
    private Style operatorStyle;
    private Style functionStyle;
    private Style defaultStyle;

    public ErlangSyntaxHighlighter() {
        buildPatterns();
        createStyles();
    }

    private void buildPatterns() {
        StringBuilder kw = new StringBuilder("\\b(");
        for(int i = 0; i < KEYWORDS.length; i++) {
            kw.append(KEYWORDS[i]);
            if(i < KEYWORDS.length - 1) kw.append("|");
        }
        kw.append(")\\b");
        keywordPattern = Pattern.compile(kw.toString());

        atomPattern = Pattern.compile("'[^'\\n]*'|\\b[a-z][a-zA-Z0-9_@]*\\b");
        varPattern = Pattern.compile("\\b[_A-Z][A-Za-z0-9_]*\\b");
        stringPattern = Pattern.compile("\"([^\\\\\"]|\\\\.)*\"");
        commentPattern = Pattern.compile("%.*$", Pattern.MULTILINE);
        numberPattern = Pattern.compile("\\b\\d+(?:#[0-9a-fA-F]+)?(?:\\.\\d+)?\\b");
        operatorPattern = Pattern.compile("[:=><!+\\-*/\\\\|&^~]+|::|");
        functionCallPattern = Pattern.compile("(?:[a-z][a-zA-Z0-9_]*:)?([a-z][a-zA-Z0-9_]*)\\s*\\(");
    }

    private void createStyles() {
        StyleContext context = StyleContext.getDefaultStyleContext();
        defaultStyle = context.getStyle(StyleContext.DEFAULT_STYLE);
        keywordStyle = context.addStyle("erlang-keyword", null);
        atomStyle = context.addStyle("erlang-atom", null);
        varStyle = context.addStyle("erlang-var", null);
        stringStyle = context.addStyle("erlang-string", null);
        commentStyle = context.addStyle("erlang-comment", null);
        numberStyle = context.addStyle("erlang-number", null);
        operatorStyle = context.addStyle("erlang-operator", null);
        functionStyle = context.addStyle("erlang-function", null);
        updateStyleColors();
    }

    private void updateStyleColors() {
        StyleConstants.setForeground(defaultStyle, isDarkTheme ? Color.WHITE : Color.BLACK);
        StyleConstants.setForeground(keywordStyle, isDarkTheme ? new Color(112, 50, 204) : new Color(128, 0, 128));
        StyleConstants.setForeground(atomStyle, isDarkTheme ? new Color(78, 201, 176) : new Color(0, 128, 0));
        StyleConstants.setForeground(varStyle, isDarkTheme ? new Color(244, 176, 62) : new Color(153, 102, 0));
        StyleConstants.setForeground(stringStyle, isDarkTheme ? new Color(89, 127, 135) : new Color(163, 21, 21));
        StyleConstants.setForeground(commentStyle, isDarkTheme ? new Color(128, 128, 128) : new Color(64, 128, 64));
        StyleConstants.setForeground(numberStyle, isDarkTheme ? new Color(104, 151, 187) : new Color(0, 0, 255));
        StyleConstants.setForeground(operatorStyle, isDarkTheme ? new Color(204, 204, 204) : new Color(64, 64, 64));
        StyleConstants.setForeground(functionStyle, isDarkTheme ? new Color(97, 175, 239) : new Color(0, 102, 204));
    }

    @Override
    public void setTheme(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
        updateStyleColors();
    }

    @Override
    public void highlight(JTextPane textPane) {
        StyledDocument doc = textPane.getStyledDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch(BadLocationException e) {
            return;
        }

        doc.setCharacterAttributes(0, text.length(), defaultStyle, true);
        highlightPattern(doc, text, stringPattern, stringStyle);
        highlightPattern(doc, text, keywordPattern, keywordStyle);
        highlightPattern(doc, text, numberPattern, numberStyle);
        highlightPattern(doc, text, operatorPattern, operatorStyle);
        highlightPattern(doc, text, varPattern, varStyle);
        highlightPattern(doc, text, atomPattern, atomStyle);
        highlightFunctionCalls(doc, text);
        highlightPattern(doc, text, commentPattern, commentStyle);
    }

    private void highlightPattern(StyledDocument doc, String text, Pattern pattern, Style style) {
        Matcher m = pattern.matcher(text);
        while(m.find()) {
            doc.setCharacterAttributes(m.start(), m.end() - m.start(), style, false);
        }
    }

    private void highlightFunctionCalls(StyledDocument doc, String text) {
        Matcher m = functionCallPattern.matcher(text);
        while(m.find()) {
            int start = m.start(1);
            int end = m.end(1);
            doc.setCharacterAttributes(start, end - start, functionStyle, false);
        }
    }
}
