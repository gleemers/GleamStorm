package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope

@Composable
fun FrameWindowScope.CustomTitleBar(
    title: String = "",
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
    colorScheme: ColorScheme = darkColorScheme(),
) {
    WindowDraggableArea {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(colorScheme.surfaceContainerLow)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WindowControlButton(
                    onClick = onMinimize,
                    color = Color(0x00000000)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Minimize",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                WindowControlButton(
                    onClick = onMaximize,
                    color = Color(0x00000000)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Maximize",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                WindowControlButton(
                    onClick = onClose,
                    color = Color(0x00000000)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    color: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp, 28.dp)
            .clickable { onClick() }
            .background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
