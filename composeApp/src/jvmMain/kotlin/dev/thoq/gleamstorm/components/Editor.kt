package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import dev.thoq.gleamstorm.plugin.EditorPluginManager
import dev.thoq.gleamstorm.plugin.IEditorPlugin
import org.jetbrains.compose.ui.tooling.preview.Preview

class Editor {
    companion object {
        val pluginManager = EditorPluginManager()
    }

    fun addPlugin(plugin: IEditorPlugin): EditorPluginManager {
        return pluginManager.addPlugin(plugin)
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme(colorScheme) {
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