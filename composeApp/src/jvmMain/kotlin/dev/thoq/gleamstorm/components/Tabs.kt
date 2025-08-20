package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorTabs(
    openFiles: List<String>,
    activeFile: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(colorScheme.surfaceVariant)
    ) {
        openFiles.forEach { file ->
            val isActive = file == activeFile
            Box(
                modifier = Modifier
                    .clip(shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                    .background(if (isActive) colorScheme.surface else colorScheme.surfaceVariant)
                    .clickable { onTabClick(file) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.split("/").last(),
                        color = if (isActive) colorScheme.onBackground else colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        tint = if (isActive) colorScheme.onBackground else colorScheme.onSurface,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onTabClose(file) }
                    )
                }
            }
        }
    }
}
