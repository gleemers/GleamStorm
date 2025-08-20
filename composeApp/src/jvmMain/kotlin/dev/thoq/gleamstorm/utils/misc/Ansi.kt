package dev.thoq.gleamstorm.utils.misc

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

fun ansiToAnnotatedString(text: String): AnnotatedString {
    val ansiRegex = "\\u001B\\[([0-9;]*)m".toRegex()

    return buildAnnotatedString {
        var currentSpanStyle = SpanStyle()
        var lastIndex = 0

        ansiRegex.findAll(text).forEach { match ->
            val textBefore = text.substring(lastIndex, match.range.first)
            if(textBefore.isNotEmpty()) {
                val startIndex = length
                append(textBefore)
                addStyle(currentSpanStyle, startIndex, length)
            }

            val codes = match.groupValues[1].split(';').mapNotNull { it.toIntOrNull() }

            if(codes.isEmpty() || codes.contains(0)) {
                currentSpanStyle = SpanStyle()
            } else {
                for(code in codes) {
                    currentSpanStyle = when(code) {
                        1 -> currentSpanStyle.copy(fontWeight = FontWeight.Bold)
                        3 -> currentSpanStyle.copy(fontStyle = FontStyle.Italic)
                        4 -> currentSpanStyle.copy(textDecoration = TextDecoration.Underline)
                        9 -> currentSpanStyle.copy(textDecoration = TextDecoration.LineThrough)
                        30 -> currentSpanStyle.copy(color = Color.Black)
                        31 -> currentSpanStyle.copy(color = Color.Red)
                        32 -> currentSpanStyle.copy(color = Color.Green)
                        33 -> currentSpanStyle.copy(color = Color.Yellow)
                        34 -> currentSpanStyle.copy(color = Color.Blue)
                        35 -> currentSpanStyle.copy(color = Color.Magenta)
                        36 -> currentSpanStyle.copy(color = Color.Cyan)
                        37 -> currentSpanStyle.copy(color = Color.White)
                        90 -> currentSpanStyle.copy(color = Color.Gray)
                        91 -> currentSpanStyle.copy(color = Color(0xFFF44336))
                        92 -> currentSpanStyle.copy(color = Color(0xFF4CAF50))
                        93 -> currentSpanStyle.copy(color = Color(0xFFFFEB3B))
                        94 -> currentSpanStyle.copy(color = Color(0xFF2196F3))
                        95 -> currentSpanStyle.copy(color = Color(0xFFE91E63))
                        96 -> currentSpanStyle.copy(color = Color(0xFF00BCD4))
                        97 -> currentSpanStyle.copy(color = Color.White)
                        else -> currentSpanStyle
                    }
                }
            }

            lastIndex = match.range.last + 1
        }

        val remainingText = text.substring(lastIndex)
        if(remainingText.isNotEmpty()) {
            val startIndex = length
            append(remainingText)
            addStyle(currentSpanStyle, startIndex, length)
        }
    }
}