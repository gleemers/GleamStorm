package dev.thoq.gleamstorm.integration

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import dev.thoq.gleamstorm.plugin.IEditorPlugin
import dev.thoq.gleamstorm.utils.internal.SyntaxHighlighting
import dev.thoq.gleamstorm.utils.logger.Logger
import java.util.regex.Pattern

class ErlangSyntaxPlugin : IEditorPlugin {
    override val name: String = "Erlang Syntax Highlighter"
    override val version: String = "1.0.0"

    // todo separate builtins
    private val keywords = setOf(
        "after", "and", "andalso", "band", "begin", "bnot", "bor", "bsl", "bsr", "bxor",
        "case", "catch", "cond", "div", "end", "fun", "if", "let", "not", "of", "or",
        "orelse", "receive", "rem", "try", "when", "xor", "export", "import", "module",
        "compile", "vsn", "author", "behavior", "behaviour", "spec", "type", "opaque",
        "callback", "export_type", "record", "define", "undef", "ifdef", "ifndef",
        "else", "endif", "include", "include_lib",
        // ---
        "abs", "apply", "atom_to_list", "binary_to_list", "bitstring_to_list", "element",
        "exit", "float", "hd", "integer_to_list", "is_atom", "is_binary", "is_bitstring",
        "is_boolean", "is_float", "is_function", "is_integer", "is_list", "is_map",
        "is_number", "is_pid", "is_port", "is_record", "is_reference", "is_tuple",
        "length", "list_to_atom", "list_to_binary", "list_to_bitstring", "list_to_float",
        "list_to_integer", "list_to_pid", "list_to_tuple", "make_ref", "max", "min",
        "node", "now", "open_port", "pid_to_list", "port_to_list", "put", "ref_to_list",
        "round", "self", "setelement", "size", "spawn", "spawn_link", "spawn_monitor",
        "spawn_opt", "split_binary", "term_to_binary", "throw", "time", "tl", "trunc",
        "tuple_size", "tuple_to_list", "whereis"
    )

    private val types = setOf(
        "atom", "binary", "bitstring", "boolean", "byte", "char", "float", "function",
        "integer", "iodata", "iolist", "list", "map", "neg_integer", "nil", "no_return",
        "node", "non_neg_integer", "number", "pid", "port", "pos_integer", "reference",
        "string", "term", "timeout", "tuple", "any", "none"
    )

    private val tokenPattern = Pattern.compile(
        "(\"(?:[^\"\\\\]|\\\\.)*\")" +
                "|(%.*$)" +
                "|(\\b[a-z][a-zA-Z0-9_]*\\b)" +
                "|('(?:[^'\\\\]|\\\\.)*')" +
                "|(\\b[A-Z][a-zA-Z0-9_]*\\b)" +
                "|(\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)" +
                "|([?][a-zA-Z_][a-zA-Z0-9_]*)" +
                "|(#[a-zA-Z_][a-zA-Z0-9_]*)"
    )

    override fun isCompatible(fileExtension: String): Boolean {
        return fileExtension.lowercase() in setOf("erl", "hrl", "escript")
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