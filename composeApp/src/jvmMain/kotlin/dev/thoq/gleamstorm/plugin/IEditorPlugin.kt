@file:Suppress("unused")

package dev.thoq.gleamstorm.plugin

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier

interface IEditorPlugin {
    val name: String
    val version: String

    fun isCompatible(fileExtension: String): Boolean

    @Composable
    fun renderEditor(
        text: MutableState<String>,
        placeholder: String,
        colorScheme: ColorScheme,
        modifier: Modifier,
    )

    fun onTextChanged(oldText: String, newText: String) {}
    fun onEditorFocused() {}
    fun onEditorBlurred() {}
    fun onShutdown() {}
}
