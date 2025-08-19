package dev.thoq.gleamstorm

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.thoq.gleamstorm.components.Editor
import dev.thoq.gleamstorm.components.InProject
import dev.thoq.gleamstorm.integration.GleamSyntaxPlugin
import dev.thoq.gleamstorm.utils.logger.Logger
import dev.thoq.gleamstorm.utils.state.EditorState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(dir: String, colorScheme: ColorScheme = darkColorScheme()) {
    val state = remember { EditorState.Project }
    val editor = Editor()

    remember {
        editor.addPlugin(plugin = GleamSyntaxPlugin())
    }

    Logger.debug("app", "State: $state")

    when(state) {
        EditorState.Home -> {}
        EditorState.Wizard -> {}
        EditorState.Project -> InProject(dir = dir, colorScheme = colorScheme)
        EditorState.Settings -> {}
    }
}