package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.thoq.gleamstorm.utils.state.EditorState
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File

@Composable
@Preview
fun Home(
    colorScheme: ColorScheme = darkColorScheme(),
    onStateChange: (EditorState) -> Unit = {},
) {
    var selectedProject by remember { mutableStateOf<File?>(null) }
    var isPickingFile by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = colorScheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surfaceVariant)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to GleamStorm",
                style = MaterialTheme.typography.headlineLarge,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isPickingFile = true
                },
                modifier = Modifier.size(200.dp, 56.dp),
                enabled = !isPickingFile
            ) {
                if(isPickingFile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Project")
                }
            }

            selectedProject?.let { project ->
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Selected: ${project.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface
                )
                Text(
                    text = project.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if(isPickingFile) {
                DirectoryPicker(
                    title = "Select Project Directory",
                    colorScheme = colorScheme,
                    onDirectorySelected = { selected ->
                        selected?.let { file ->
                            selectedProject = file
                            onStateChange(EditorState.Project(file.absolutePath))
                        }
                        isPickingFile = false
                    }
                )
            }
        }
    }
}
