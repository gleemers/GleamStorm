package dev.thoq.gleamstorm.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import dev.thoq.gleamstorm.utils.state.ProjectState
import java.io.File

@Composable
@Preview
fun InProject(directory: String, colorScheme: ColorScheme = darkColorScheme()) {
    val projectState = remember { ProjectState() }

    val onFileClick: (File) -> Unit = { file ->
        projectState.openFile(file)
    }

    val onSave: (String, String) -> Unit = { _, _ ->
        projectState.saveActiveFile()
    }

    val isCtrlPressed: (KeyEvent) -> Boolean = {
        System.getProperty("os.name").startsWith("Mac")
    }

    MaterialTheme(colorScheme) {
        Row(
            modifier = Modifier.fillMaxSize().onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.W && (if (isCtrlPressed(event)) event.isMetaPressed else event.isCtrlPressed)) {
                    projectState.activeFile.value?.let { projectState.closeFile(it) }
                    true
                } else {
                    false
                }
            }
        ) {
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
                Editor(
                    placeholder = projectState.activeFile.value?.let { "Editing: ${it.name}" } ?: "Select a file to edit",
                    text = projectState.editorText,
                    fileName = projectState.activeFile.value?.name ?: "untitled.txt",
                    colorScheme = colorScheme,
                    onSave = onSave
                )
            }
        }
    }
}
