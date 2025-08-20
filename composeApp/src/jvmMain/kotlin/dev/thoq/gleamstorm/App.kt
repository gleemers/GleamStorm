package dev.thoq.gleamstorm

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
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
import dev.thoq.gleamstorm.components.Home
import dev.thoq.gleamstorm.integration.ErlangSyntaxPlugin

@Composable
@Preview
fun FrameWindowScope.App(
    window: ComposeWindow,
    onExit: () -> Unit = {},
    dir: String,
    colorScheme: ColorScheme = darkColorScheme(),
) {
    var currentState by remember { mutableStateOf<EditorState>(EditorState.Home) }
    val editor = Editor()

    remember {
        editor.addPlugin(plugin = GleamSyntaxPlugin())
        editor.addPlugin(plugin = ErlangSyntaxPlugin())
    }

    Logger.debug("app", "State: $currentState")

    MaterialTheme(colorScheme = colorScheme) {
        Column {
            CustomTitleBar(
                title = when(currentState) {
                    is EditorState.Home -> "GleamStorm"
                    is EditorState.Wizard -> "GleamStorm - Project Wizard"
                    is EditorState.Project -> "GleamStorm - ${
                        (currentState as EditorState.Project).projectPath.split(
                            '/'
                        ).lastOrNull() ?: "Project"
                    }"

                    is EditorState.Settings -> "GleamStorm - Settings"
                },
                onMinimize = { window.isMinimized = true },
                onMaximize = {
                    window.placement = if(window.placement == WindowPlacement.Maximized)
                        WindowPlacement.Floating
                    else
                        WindowPlacement.Maximized
                },
                onClose = { onExit() },
                colorScheme = colorScheme,
            )

            if(dir.isNotEmpty()) {
                InProject(
                    directory = dir,
                    colorScheme = colorScheme,
                )
            } else {
                when(val state = currentState) {
                    is EditorState.Home -> Home(
                        colorScheme = colorScheme,
                        onStateChange = { newState -> currentState = newState }
                    )

                    is EditorState.Wizard -> {
                        // TODO: Implement project wizard
                    }

                    is EditorState.Project -> InProject(
                        directory = state.projectPath,
                        colorScheme = colorScheme,
                    )

                    is EditorState.Settings -> {
                        // TODO: Implement settings
                    }
                }
            }
        }
    }
}
