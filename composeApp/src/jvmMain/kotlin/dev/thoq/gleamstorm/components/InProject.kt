package dev.thoq.gleamstorm.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.thoq.gleamstorm.utils.logger.Logger
import java.io.File

@Composable
@Preview
fun InProject(colorScheme: ColorScheme = darkColorScheme()) {
    val currentFile = remember { mutableStateOf<File?>(null) }
    val editorText = remember { mutableStateOf("") }

    Logger.debug("project-view", "currentFile: ${currentFile.value?.absolutePath}")

    val onFileClick: (File) -> Unit = { file ->
        if(!file.isDirectory) {
            currentFile.value = file
            try {
                editorText.value = file.readText()
            } catch(e: Exception) {
                editorText.value = "Error reading file: ${e.message}"
            }
        }
    }

    val onSave: (String, String) -> Unit = { content, fileName ->
        currentFile.value?.let { file ->
            try {
                file.writeText(content)
                Logger.info("file-save", "Successfully saved ${file.absolutePath}")
            } catch(e: Exception) {
                Logger.error("file-save", "Failed to save ${file.absolutePath}: ${e.message}")
            }
        } ?: run {
            Logger.warning("file-save", "No file selected to save")
        }
    }

    MaterialTheme(colorScheme) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                FileTree(
                    directory = "/home/thoq/Projects/glee",
                    colorScheme = colorScheme,
                    onFileClick = onFileClick
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Editor(
                    placeholder = currentFile.value?.let { "Editing: ${it.name}" } ?: "Select a file to edit",
                    text = editorText,
                    fileName = currentFile.value?.name ?: "untitled.txt",
                    colorScheme = colorScheme,
                    onSave = onSave
                )
            }
        }
    }
}
