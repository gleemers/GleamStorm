package dev.thoq.gleamstorm.integration

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import dev.thoq.gleamstorm.plugin.IEditorPlugin
import dev.thoq.gleamstorm.utils.internal.SyntaxHighlighting
import dev.thoq.gleamstorm.utils.logger.Logger
import java.util.regex.Pattern

class GleamSyntaxPlugin : IEditorPlugin {
    override val name: String = "Gleam Syntax Highlighter"
    override val version: String = "1.0.0"

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
        SyntaxHighlighting.render(text, placeholder, colorScheme, modifier, tokenPattern, keywords, types)
    }
}