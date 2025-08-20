package dev.thoq.gleamstorm.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import dev.thoq.gleamstorm.utils.state.ProjectState
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.io.File

@OptIn(ExperimentalSplitPaneApi::class)
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
            HorizontalSplitPane(
                splitPaneState = rememberSplitPaneState(initialPositionPercentage = 0.2f)
            ) {
                first {
                    FileTree(
                        directory = directory, colorScheme = colorScheme, onFileClick = onFileClick
                    )
                }
                second {
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
                        if (showTerminal) {
                            VerticalSplitPane(
                                splitPaneState = rememberSplitPaneState(initialPositionPercentage = 0.7f)
                            ) {
                                first {
                                    Editor(
                                        placeholder = projectState.activeFile.value?.let { "Editing: ${it.name}" }
                                            ?: "Select a file to edit",
                                        text = projectState.editorText,
                                        fileName = projectState.activeFile.value?.name ?: "untitled.txt",
                                        colorScheme = colorScheme,
                                        onSave = onSave
                                    )
                                }
                                second {
                                    Terminal(projectState)
                                }
                            }
                        } else {
                            Editor(
                                placeholder = projectState.activeFile.value?.let { "Editing: ${it.name}" }
                                    ?: "Select a file to edit",
                                text = projectState.editorText,
                                fileName = projectState.activeFile.value?.name ?: "untitled.txt",
                                colorScheme = colorScheme,
                                onSave = onSave
                            )
                        }
                    }
                }
            }
        }
    }
}
