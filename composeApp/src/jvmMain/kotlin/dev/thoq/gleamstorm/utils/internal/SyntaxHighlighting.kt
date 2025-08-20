package dev.thoq.gleamstorm.utils.internal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import dev.thoq.gleamstorm.integration.Shared
import dev.thoq.gleamstorm.utils.logger.Logger
import java.util.regex.Pattern

object SyntaxHighlighting {
    private const val maxHighlightLength = 100000
    private const val maxLinesToHighlight = 10000

    fun highlight(
        text: String,
        colorScheme: ColorScheme,
        maxLinesToHighlight: Int,
        highlightLine: AnnotatedString.Builder.(line: String, colorScheme: ColorScheme) -> Unit
    ): AnnotatedString {
        return buildAnnotatedString {
            val lines = text.split('\n')
            val maxLines = minOf(lines.size, maxLinesToHighlight)

            for(lineIndex in 0 until maxLines) {
                val line = lines[lineIndex]
                if(line.length > 500) {
                    append(line)
                } else {
                    highlightLine(line, colorScheme)
                }

                if(lineIndex < maxLines - 1) {
                    append('\n')
                }
            }

            if(lines.size > maxLinesToHighlight) {
                withStyle(SpanStyle(color = colorScheme.onSurface)) {
                    for(i in maxLinesToHighlight until lines.size) {
                        append('\n')
                        append(lines[i])
                    }
                }
            }
        }
    }

    private fun shouldHighlight(text: String): Boolean {
        return text.length <= maxHighlightLength && text.count { it == '\n' } <= maxLinesToHighlight
    }

    private fun AnnotatedString.Builder.highlightLine(
        line: String, colorScheme: ColorScheme, tokenPattern: Pattern, keywords: Set<String>, types: Set<String>
    ) {
        if(line.isEmpty()) return

        try {
            val matcher = tokenPattern.matcher(line)
            var lastMatchEnd = 0

            while(matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                if(start > lastMatchEnd) {
                    append(line.substring(lastMatchEnd, start))
                }

                val style = when {
                    matcher.group(1) != null -> Shared.getStringStyle(colorScheme)
                    matcher.group(2) != null -> Shared.getCommentStyle(colorScheme)
                    matcher.group(3) != null -> {
                        val word = matcher.group(3)
                        when {
                            keywords.contains(word) -> Shared.getKeywordStyle(colorScheme)
                            types.contains(word) -> Shared.getTypeStyle(colorScheme)
                            word.isNotEmpty() && word[0].isUpperCase() -> Shared.getConstructorStyle(colorScheme)
                            else -> null
                        }
                    }

                    matcher.group(4) != null -> Shared.getNumberStyle(colorScheme)
                    else -> null
                }

                val text = line.substring(start, end)
                if(style != null) {
                    withStyle(style) {
                        append(text)
                    }
                } else {
                    append(text)
                }

                lastMatchEnd = end
            }

            if(lastMatchEnd < line.length) {
                append(line.substring(lastMatchEnd))
            }
        } catch(e: OutOfMemoryError) {
            append(line)
            Logger.error("syntax-highlighting", "MemoryFull: ${e.message}")
        }
    }

    class SyntaxHighlightingTransformation(
        private val colorScheme: ColorScheme,
        private val tokenPattern: Pattern,
        private val keywords: Set<String>,
        private val types: Set<String>,
    ) : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val highlightedText = if(shouldHighlight(text.text)) {
                highlight(text.text, colorScheme, maxLinesToHighlight) { line, colorScheme ->
                    highlightLine(line, colorScheme, tokenPattern, keywords, types)
                }
            } else {
                buildAnnotatedString { append(text) }
            }
            return TransformedText(highlightedText, OffsetMapping.Identity)
        }
    }

    @Composable
    fun render(
        text: MutableState<String>,
        placeholder: String,
        colorScheme: ColorScheme,
        modifier: Modifier,
        tokenPattern: Pattern,
        keywords: Set<String>,
        types: Set<String>
    ) {
        MaterialTheme(colorScheme) {
            val visualTransformation = remember(colorScheme) {
                SyntaxHighlightingTransformation(colorScheme, tokenPattern, keywords, types)
            }

            TextField(
                value = text.value,
                onValueChange = { text.value = it },
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurface
                ),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                },
                visualTransformation = visualTransformation,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    disabledContainerColor = colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
            )
        }
    }
}