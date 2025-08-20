package dev.thoq.gleamstorm

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import dev.thoq.gleamstorm.components.Editor
import dev.thoq.gleamstorm.components.InProject
import dev.thoq.gleamstorm.integration.GleamSyntaxPlugin
import dev.thoq.gleamstorm.utils.logger.Logger
import dev.thoq.gleamstorm.utils.state.EditorState
import org.jetbrains.compose.ui.tooling.preview.Preview
import dev.thoq.gleamstorm.components.CustomTitleBar

@Composable
@Preview
fun FrameWindowScope.App(window: ComposeWindow, onExit: () -> Unit = {}, dir: String, colorScheme: ColorScheme = darkColorScheme()) {
    val state = remember { EditorState.Project }
    val editor = Editor()

    remember {
        editor.addPlugin(plugin = GleamSyntaxPlugin())
    }

    Logger.debug("app", "State: $state")

    MaterialTheme(colorScheme = colorScheme) {
        Column {
            CustomTitleBar(
                title = "GleamStorm",
                onMinimize = { window.isMinimized = true },
                onMaximize = { window.placement = if (window.placement == WindowPlacement.Maximized)
                    WindowPlacement.Floating else WindowPlacement.Maximized },
                onClose = { onExit() },
                colorScheme = colorScheme,
            )

            when(state) {
                EditorState.Home -> {}
                EditorState.Wizard -> {}
                EditorState.Project -> InProject(directory = dir, colorScheme = colorScheme)
                EditorState.Settings -> {}
            }
        }
    }
}