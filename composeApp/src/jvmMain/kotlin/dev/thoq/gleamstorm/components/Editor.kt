package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.thoq.gleamstorm.plugin.EditorPluginManager
import dev.thoq.gleamstorm.plugin.IEditorPlugin
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

class Editor {
    companion object {
        val pluginManager = EditorPluginManager()
    }

    fun addPlugin(plugin: IEditorPlugin): EditorPluginManager {
        return pluginManager.addPlugin(plugin)
    }
}

sealed class EditorState {
    object Loading : EditorState()
    object Ready : EditorState()
    data class Error(val message: String) : EditorState()
}

@Composable
fun EditorLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = "Opening...",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
@Preview
fun Editor(
    placeholder: String = "",
    text: MutableState<String>,
    colorScheme: ColorScheme = darkColorScheme(),
    fileName: String = "untitled.txt",
    onSave: ((String, String) -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val editorState = remember { mutableStateOf<EditorState>(EditorState.Loading) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(fileName) {
        editorState.value = EditorState.Loading

        coroutineScope.launch {
            try {
                editorState.value = EditorState.Ready
            } catch(e: Exception) {
                editorState.value = EditorState.Error("Failed to open file: ${e.message}")
            }
        }
    }

    LaunchedEffect(editorState.value) {
        if(editorState.value is EditorState.Ready) {
            focusRequester.requestFocus()
        }
    }

    MaterialTheme(colorScheme) {
        when(val state = editorState.value) {
            is EditorState.Loading -> EditorLoadingIndicator()

            is EditorState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is EditorState.Ready -> {
                Editor.pluginManager.renderEditorWithPlugin(
                    fileName = fileName,
                    text = text,
                    placeholder = placeholder,
                    colorScheme = colorScheme,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if(keyEvent.type == KeyEventType.KeyDown) {
                                val isCtrlPressed = keyEvent.isCtrlPressed
                                val isSKey = keyEvent.key == Key.S

                                if(isCtrlPressed && isSKey) {
                                    onSave?.invoke(text.value, fileName)
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        },
                    fallbackRenderer = { textState, placeholderText, scheme, modifier ->
                        BasicTextField(
                            value = textState.value,
                            onValueChange = { textState.value = it },
                            modifier = modifier
                                .background(color = scheme.background),
                            textStyle = TextStyle(
                                color = scheme.onSurface,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(scheme.primary),
                            decorationBox = { innerTextField ->
                                if(textState.value.isEmpty()) {
                                    Text(
                                        text = placeholderText,
                                        color = scheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                )
            }
        }
    }
}
