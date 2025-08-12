package dev.thoq.integration.gleam;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GleamSyntaxHighlighter {
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
    private Pattern stringPattern;
    private Pattern commentPattern;
    private Pattern numberPattern;
    private Pattern operatorPattern;

    private Style keywordStyle;
    private Style typeStyle;
    private Style stringStyle;
    private Style commentStyle;
    private Style numberStyle;
    private Style operatorStyle;
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
            if (i < TYPES.length - 1) typeBuilder.append("|");
        }
        typeBuilder.append(")\\b");
        typePattern = Pattern.compile(typeBuilder.toString());

        stringPattern = Pattern.compile("\"([^\\\\\"]|\\\\.)*\"");
        commentPattern = Pattern.compile("//.*$", Pattern.MULTILINE);
        numberPattern = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
        operatorPattern = Pattern.compile("[+\\-*/<>=!&|]+");
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

        highlightPattern(doc, text, commentPattern, commentStyle);
        highlightPattern(doc, text, stringPattern, stringStyle);
        highlightPattern(doc, text, keywordPattern, keywordStyle);
        highlightPattern(doc, text, typePattern, typeStyle);
        highlightPattern(doc, text, numberPattern, numberStyle);
        highlightPattern(doc, text, operatorPattern, operatorStyle);
    }

    private void highlightPattern(StyledDocument doc, String text, Pattern pattern, Style style) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false);
        }
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
}