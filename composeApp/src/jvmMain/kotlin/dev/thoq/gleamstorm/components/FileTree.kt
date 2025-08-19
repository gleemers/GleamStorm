package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.thoq.gleamstorm.utils.logger.Logger
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File

data class FileTreeNode(
    val file: File,
    val depth: Int = 0,
    val isExpanded: Boolean = false,
)

fun buildFileTree(rootPath: String, expandedFolders: Set<String>): List<FileTreeNode> {
    fun buildTreeRecursive(directory: File, depth: Int = 0): List<FileTreeNode> {
        val result = mutableListOf<FileTreeNode>()

        try {
            val children = directory.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

            children?.forEach { child ->
                val isExpanded = expandedFolders.contains(child.absolutePath)
                result.add(FileTreeNode(child, depth, isExpanded))

                if(child.isDirectory && isExpanded)
                    result.addAll(buildTreeRecursive(child, depth + 1))
            }
        } catch(e: Exception) {
            Logger.error("file-tree", "Error reading directory: ${directory.absolutePath}")
            Logger.error("file-tree", e.stackTraceToString())
        }

        return result
    }

    return buildTreeRecursive(File(rootPath))
}

@Composable
@Preview
fun FileTree(
    directory: String = ".",
    colorScheme: ColorScheme = darkColorScheme(),
    onFileClick: (File) -> Unit = {},
) {
    val expandedFolders = remember { mutableStateOf(setOf<String>()) }

    val fileTree = remember(directory, expandedFolders.value) {
        buildFileTree(directory, expandedFolders.value)
    }

    val onFolderToggle: (File) -> Unit = { folder ->
        val folderPath = folder.absolutePath
        expandedFolders.value = if(expandedFolders.value.contains(folderPath)) {
            expandedFolders.value - folderPath
        } else {
            expandedFolders.value + folderPath
        }
    }

    MaterialTheme(colorScheme) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(fileTree) { node ->
                FileItem(
                    file = node.file,
                    depth = node.depth,
                    isExpanded = node.isExpanded,
                    onFileClick = onFileClick,
                    onFolderToggle = onFolderToggle,
                    colorScheme = colorScheme
                )
            }
        }
    }
}
