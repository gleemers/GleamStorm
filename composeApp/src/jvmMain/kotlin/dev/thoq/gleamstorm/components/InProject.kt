package dev.thoq.gleamstorm.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.thoq.gleamstorm.utils.state.ProjectState
import java.io.File

@Composable
fun InProject(directory: String, colorScheme: ColorScheme = darkColorScheme()) {
    val projectState = remember { ProjectState() }
    var showTerminal by remember { mutableStateOf(false) }

    projectState.projectFolder = File(directory)

    val onFileClick: (File) -> Unit = { file ->
        projectState.openFile(file)
    }

    val onSave: (String, String) -> Unit = { _, _ ->
        projectState.saveActiveFile()
    }

    MaterialTheme(colorScheme) {
        Box(
            modifier = Modifier.fillMaxSize().onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if ((event.isCtrlPressed || event.isMetaPressed) && event.key == Key.W) {
                        projectState.activeFile.value?.let { projectState.closeFile(it) }
                        return@onKeyEvent true
                    }
                    if (event.isCtrlPressed && event.key == Key.Grave) {
                        showTerminal = !showTerminal
                        return@onKeyEvent true
                    }
                }
                false
            }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.width(width = 300.dp).fillMaxHeight()
                ) {
                    FileTree(
                        directory = directory, colorScheme = colorScheme, onFileClick = onFileClick
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                ) {
                    EditorTabs(
                        openFiles = projectState.openFiles.value.map { it.absolutePath },
                        activeFile = projectState.activeFile.value?.absolutePath,
                        onTabClick = { path ->
                            projectState.switchTab(File(path))
                        },
                        onTabClose = { path ->
                            projectState.closeFile(File(path))
                        },
                        colorScheme = colorScheme
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        Editor(
                            placeholder = projectState.activeFile.value?.let { "Editing: ${it.name}" }
                                ?: "Select a file to edit",
                            text = projectState.editorText,
                            fileName = projectState.activeFile.value?.name ?: "untitled.txt",
                            colorScheme = colorScheme,
                            onSave = onSave
                        )
                    }
                    if (showTerminal) {
                        Box(modifier = Modifier.height(200.dp)) {
                            Terminal(projectState)
                        }
                    }
                }
            }
        }
    }
}
