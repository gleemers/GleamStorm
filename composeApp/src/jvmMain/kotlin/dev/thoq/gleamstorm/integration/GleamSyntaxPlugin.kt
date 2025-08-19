package dev.thoq.gleamstorm.integration

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.thoq.gleamstorm.plugin.IEditorPlugin
import dev.thoq.gleamstorm.utils.logger.Logger
import java.util.regex.Pattern

class GleamSyntaxPlugin : IEditorPlugin {
    override val name: String = "Gleam Syntax Highlighter"
    override val version: String = "1.0.0"

    private val maxHighlightLength = 100000
    private val maxLinesToHighlight = 10000

    private val keywords = setOf(
        "pub", "fn", "let", "assert", "case", "if", "else", "import", "type", "const",
        "external", "opaque", "use", "try", "panic", "todo", "as", "when", "Ok", "Error"
    )

    private val types = setOf(
        "String", "Int", "Float", "Bool", "List", "Result", "Option", "Nil",
        "BitArray", "Dict", "Set"
    )

    private val tokenPattern = Pattern.compile(
        "(\"(?:[^\"\\\\]|\\\\.)*\")" +
                "|(//.*$)" +
                "|(\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)" +
                "|(\\b\\d+(?:\\.\\d+)?\\b)"
    )

    private fun getKeywordStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.primary,
        fontWeight = FontWeight.Bold
    )

    private fun getTypeStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.tertiary,
        fontWeight = FontWeight.SemiBold
    )

    private fun getStringStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.secondary
    )

    private fun getNumberStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.primaryContainer.copy(
            red = if (colorScheme.primaryContainer.red < 0.5f)
                minOf(1.0f, colorScheme.primaryContainer.red + 0.3f)
            else maxOf(0.0f, colorScheme.primaryContainer.red - 0.3f),
            green = if (colorScheme.primaryContainer.green < 0.5f)
                minOf(1.0f, colorScheme.primaryContainer.green + 0.3f)
            else maxOf(0.0f, colorScheme.primaryContainer.green - 0.3f),
            blue = if (colorScheme.primaryContainer.blue < 0.5f)
                minOf(1.0f, colorScheme.primaryContainer.blue + 0.3f)
            else maxOf(0.0f, colorScheme.primaryContainer.blue - 0.3f)
        )
    )

    private fun getCommentStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.onSurface.copy(alpha = 0.7f)
    )

    private fun getConstructorStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.secondary.copy(
            red = minOf(1.0f, colorScheme.secondary.red + 0.2f),
            blue = minOf(1.0f, colorScheme.secondary.blue + 0.2f)
        )
    )

    override fun isCompatible(fileExtension: String): Boolean {
        return fileExtension.lowercase() in setOf("gleam", "gl")
    }

    override fun onShutdown() {
        Logger.debug("syntax-highlighting", "Unloading...")
        super.onShutdown()
    }

    @Composable
    override fun renderEditor(
        text: MutableState<String>,
        placeholder: String,
        colorScheme: ColorScheme,
        modifier: Modifier,
    ) {
        MaterialTheme(colorScheme) {
            val visualTransformation = remember(colorScheme) {
                SyntaxHighlightingTransformation(colorScheme)
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

    private inner class SyntaxHighlightingTransformation(private val colorScheme: ColorScheme) : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val highlightedText = if (shouldHighlight(text.text)) {
                highlight(text.text, colorScheme)
            } else {
                buildAnnotatedString { append(text) }
            }
            return TransformedText(highlightedText, OffsetMapping.Identity)
        }
    }


    private fun shouldHighlight(text: String): Boolean {
        return text.length <= maxHighlightLength &&
                text.count { it == '\n' } <= maxLinesToHighlight
    }

    private fun highlight(text: String, colorScheme: ColorScheme): AnnotatedString {
        return buildAnnotatedString {
            val lines = text.split('\n')
            val maxLines = minOf(lines.size, maxLinesToHighlight)

            for (lineIndex in 0 until maxLines) {
                val line = lines[lineIndex]
                if (line.length > 500) {
                    append(line)
                } else {
                    highlightLine(line, colorScheme)
                }

                if (lineIndex < maxLines - 1) {
                    append('\n')
                }
            }

            if (lines.size > maxLinesToHighlight) {
                withStyle(SpanStyle(color = colorScheme.onSurface)) {
                    for (i in maxLinesToHighlight until lines.size) {
                        append('\n')
                        append(lines[i])
                    }
                }
            }
        }
    }

    private fun AnnotatedString.Builder.highlightLine(line: String, colorScheme: ColorScheme) {
        if (line.isEmpty()) return

        try {
            val matcher = tokenPattern.matcher(line)
            var lastMatchEnd = 0

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                if (start > lastMatchEnd) {
                    append(line.substring(lastMatchEnd, start))
                }

                val style = when {
                    matcher.group(1) != null -> getStringStyle(colorScheme)
                    matcher.group(2) != null -> getCommentStyle(colorScheme)
                    matcher.group(3) != null -> {
                        val word = matcher.group(3)
                        when {
                            keywords.contains(word) -> getKeywordStyle(colorScheme)
                            types.contains(word) -> getTypeStyle(colorScheme)
                            word.isNotEmpty() && word[0].isUpperCase() -> getConstructorStyle(colorScheme)
                            else -> null
                        }
                    }

                    matcher.group(4) != null -> getNumberStyle(colorScheme)
                    else -> null
                }

                val text = line.substring(start, end)
                if (style != null) {
                    withStyle(style) {
                        append(text)
                    }
                } else {
                    append(text)
                }

                lastMatchEnd = end
            }

            if (lastMatchEnd < line.length) {
                append(line.substring(lastMatchEnd))
            }
        } catch (e: OutOfMemoryError) {
            append(line)
            Logger.error("syntax-highlighting", "MemoryFull: ${e.message}")
        }
    }
}