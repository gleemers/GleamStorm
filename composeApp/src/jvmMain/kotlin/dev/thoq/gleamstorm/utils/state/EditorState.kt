package dev.thoq.gleamstorm.utils.state

sealed class EditorState {
    object Home : EditorState()
    object Wizard : EditorState()
    data class Project(val projectPath: String) : EditorState()
    object Settings : EditorState()
}
