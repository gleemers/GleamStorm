@file:Suppress("unused")

package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale.getDefault

@Composable
fun DirectoryPicker(
    title: String,
    colorScheme: ColorScheme = darkColorScheme(),
    onDirectorySelected: (File?) -> Unit,
    initialPath: File = File(System.getProperty("user.home")),
) {
    var currentPath by remember { mutableStateOf(initialPath) }
    var filesAndFolders by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(currentPath) {
        val contents = currentPath.listFiles()?.toList()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase(getDefault()) }
            ?: emptyList()
        filesAndFolders = contents
    }

    AlertDialog(
        onDismissRequest = { onDirectorySelected(null) },
        title = { Text(title, color = colorScheme.onSurface) },
        backgroundColor = colorScheme.surface,
        text = {
            Column {
                Text(
                    text = currentPath.absolutePath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surface)
                        .heightIn(max = 300.dp)
                ) {
                    if(currentPath.parentFile != null) {
                        item {
                            ListItem(
                                headlineContent = { Text("..") },
                                leadingContent = {
                                    Icon(Icons.Default.DoubleArrow, contentDescription = "Go Up")
                                },
                                modifier = Modifier.clickable { currentPath = currentPath.parentFile }
                            )
                        }
                    }

                    items(filesAndFolders) { file ->
                        ListItem(
                            headlineContent = { Text(file.name, color = colorScheme.onSurface) },
                            leadingContent = {
                                Icon(Icons.Default.Folder, contentDescription = "Folder")
                            },
                            modifier = Modifier
                                .clickable { currentPath = file }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDirectorySelected(currentPath) }) {
                Text("Select")
            }
        },
        dismissButton = {
            Button(onClick = { onDirectorySelected(null) }) {
                Text("Cancel")
            }
        }
    )
}
