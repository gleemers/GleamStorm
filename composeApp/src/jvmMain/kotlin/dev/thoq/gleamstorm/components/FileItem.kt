package dev.thoq.gleamstorm.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import java.io.File

@Composable
fun FileItem(
    file: File,
    depth: Int = 0,
    isExpanded: Boolean = false,
    onFileClick: (File) -> Unit = {},
    onFolderToggle: (File) -> Unit = {},
    colorScheme: ColorScheme = darkColorScheme(),
) {
    MaterialTheme(colorScheme) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if(file.isDirectory) {
                        onFolderToggle(file)
                    } else {
                        onFileClick(file)
                    }
                }
                .padding(
                    start = 8.dp + (depth * 16.dp),
                    top = 4.dp,
                    end = 8.dp,
                    bottom = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    file.isDirectory && isExpanded -> Icons.Default.FolderOpen
                    file.isDirectory -> Icons.Default.Folder
                    else -> Icons.Default.Description
                },
                contentDescription = if(file.isDirectory) "Directory" else "File",
                tint = if(file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
