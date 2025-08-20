package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.thoq.gleamstorm.utils.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File

data class FileTreeNode(
    val file: File,
    val depth: Int = 0,
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val hasLoadedChildren: Boolean = false,
)

sealed class FileTreeState {
    object Loading : FileTreeState()
    data class Loaded(val nodes: List<FileTreeNode>) : FileTreeState()
    data class Error(val message: String) : FileTreeState()
}

class FileTreeManager {
    private val loadingFolders = mutableSetOf<String>()

    suspend fun buildFileTree(
        rootPath: String,
        expandedFolders: Set<String>,
        loadedFolders: Set<String> = emptySet(),
    ): List<FileTreeNode> = withContext(Dispatchers.IO) {
        fun buildTreeRecursive(directory: File, depth: Int = 0): List<FileTreeNode> {
            Thread.currentThread().name = "file-tree-build-service"

            val result = mutableListOf<FileTreeNode>()

            try {
                if(!directory.canRead()) {
                    Logger.warning("file-tree", "Cannot read directory: ${directory.absolutePath}")
                    return result
                }

                val allFiles = directory.listFiles()
                val children = allFiles?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

                children?.forEach { child ->
                    val childPath = child.absolutePath
                    val isExpanded = expandedFolders.contains(childPath)
                    val isLoading = loadingFolders.contains(childPath)
                    val hasLoadedChildren = loadedFolders.contains(childPath)

                    result.add(
                        FileTreeNode(
                            file = child,
                            depth = depth,
                            isExpanded = isExpanded,
                            isLoading = isLoading,
                            hasLoadedChildren = hasLoadedChildren
                        )
                    )

                    if(child.isDirectory && isExpanded && hasLoadedChildren && !isLoading) result.addAll(
                        buildTreeRecursive(child, depth + 1)
                    )
                }
            } catch(ex: Exception) {
                Logger.error("file-tree", "Error reading directory: ${directory.absolutePath}")
                Logger.error("file-tree", ex.stackTraceToString())
            }

            return result
        }

        buildTreeRecursive(File(rootPath))
    }

    fun setFolderLoading(folderPath: String, isLoading: Boolean) {
        if(isLoading) loadingFolders.add(folderPath) else loadingFolders.remove(folderPath)
    }
}

@Composable
fun LoadingIndicator(depth: Int) {
    Row(
        modifier = Modifier.padding(start = (depth * 20).dp), verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp), strokeWidth = 2.dp
        )
        Text(
            text = "Opening...",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
@Preview
fun FileTree(
    directory: String,
    colorScheme: ColorScheme = darkColorScheme(),
    onFileClick: (File) -> Unit = {},
) {
    val expandedFolders = remember { mutableStateOf(setOf<String>()) }
    val loadedFolders = remember { mutableStateOf(setOf<String>()) }
    val fileTreeState = remember { mutableStateOf<FileTreeState>(FileTreeState.Loading) }
    val fileTreeManager = remember { FileTreeManager() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(directory) {
        try {
            fileTreeState.value = FileTreeState.Loading
            val nodes = fileTreeManager.buildFileTree(directory, expandedFolders.value, loadedFolders.value)
            fileTreeState.value = FileTreeState.Loaded(nodes)
        } catch(e: Exception) {
            fileTreeState.value = FileTreeState.Error("Failed to load directory: ${e.message}")
        }
    }

    LaunchedEffect(expandedFolders.value, loadedFolders.value) {
        Thread.currentThread().name = "file-tree-refresh-service"
        if(fileTreeState.value is FileTreeState.Loaded) {
            try {
                val nodes = fileTreeManager.buildFileTree(directory, expandedFolders.value, loadedFolders.value)
                fileTreeState.value = FileTreeState.Loaded(nodes)
            } catch(e: Exception) {
                fileTreeState.value = FileTreeState.Error("Failed to refresh directory: ${e.message}")
                Logger.error("file-tree", "Failed to refresh directory: ${e.message}")
                Logger.error("file-tree", e.stackTraceToString())
            }
        }
    }

    val onFolderToggle: (File) -> Unit = { folder ->
        val folderPath = folder.absolutePath

        if(expandedFolders.value.contains(folderPath)) {
            expandedFolders.value = expandedFolders.value - folderPath
        } else {
            expandedFolders.value = expandedFolders.value + folderPath

            if(!loadedFolders.value.contains(folderPath)) {
                fileTreeManager.setFolderLoading(folderPath, true)

                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            if(folder.canRead()) folder.listFiles()
                        }

                        loadedFolders.value = loadedFolders.value + folderPath
                        fileTreeManager.setFolderLoading(folderPath, false)
                    } catch(ex: Exception) {
                        Logger.error("file-tree", "Error loading folder: $folderPath - ${ex.message}")
                        fileTreeManager.setFolderLoading(folderPath, false)
                        loadedFolders.value = loadedFolders.value + folderPath
                    }
                }
            }
        }
    }

    MaterialTheme(colorScheme) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            when(val state = fileTreeState.value) {
                is FileTreeState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(0)
                        }
                    }
                }

                is FileTreeState.Error -> {
                    item {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is FileTreeState.Loaded -> {
                    items(state.nodes) { node ->
                        if(node.file.isDirectory && node.isExpanded && node.isLoading) {
                            LoadingIndicator(node.depth + 1)
                        } else {
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
        }
    }
}
