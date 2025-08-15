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

@SuppressWarnings("SameParameterValue")
public final class HoverHtmlFormatter {
    private HoverHtmlFormatter() {}

    public static String formatHoverHtml(String md) {
        if (md == null || md.trim().isEmpty()) {
            return null;
        }

        String html = md;

        html = escapeHtml(html);
        html = html.replaceAll("```([\\s\\S]*?)```", "$1");
        html = formatInlineWithEscape(html, "background:#2b2b2b;color:#e6e6e6;padding:2px 4px;border-radius:4px;font-family:monospace;");
        html = html.replaceAll("(?<!<code[^>]>.?)\\*\\*([^*\\n]+?)\\*\\*(?!.*?</code>)", "<strong>$1</strong>");
        html = html.replaceAll("(?<!<code[^>]>.?)\\*([^*\\n]+?)\\*(?!.*?</code>)", "<em>$1</em>");
        html = html.replace("\n", "<br/>");

        return "<html>" + html + "</html>";
    }

    static String formatInlineWithEscape(String text, String codeInlineStyle) {
        StringBuilder sb = new StringBuilder();

        boolean inCode = false;
        int i = 0;

        while(i < text.length()) {
            char c = text.charAt(i);
            if(c == '`') {
                inCode = !inCode;
                if(inCode) {
                    sb.append("<code style=\"").append(codeInlineStyle).append("\">");
                } else {
                    sb.append("</code>");
                }
                i++;
                continue;
            }
            if(inCode) {
                sb.append(c);
            } else {
                switch(c) {
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '&': sb.append("&amp;"); break;
                    case '"': sb.append("&quot;"); break;
                    default: sb.append(c);
                }
            }
            i++;
        }

        if(inCode)
            sb.append("</code>");

        return sb.toString();
    }

    static String escapeHtml(String s) {
        if(s == null)
            return null;

        StringBuilder sb = new StringBuilder(s.length());
        for(char c : s.toCharArray()) {
            switch(c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }

        return sb.toString();
    }
}
