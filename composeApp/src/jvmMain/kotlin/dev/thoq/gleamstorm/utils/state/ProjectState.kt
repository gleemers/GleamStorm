package dev.thoq.gleamstorm.utils.state

import androidx.compose.runtime.mutableStateOf
import dev.thoq.gleamstorm.utils.logger.Logger
import java.io.File

class ProjectState() {
    lateinit var projectFolder: File
    val openFiles = mutableStateOf<List<File>>(emptyList())
    val activeFile = mutableStateOf<File?>(null)
    val editorText = mutableStateOf("")

    fun openFile(file: File) {
        if(!file.isDirectory) {
            if(!openFiles.value.contains(file)) {
                openFiles.value += file
            }
            activeFile.value = file
            loadFileContent(file)
        }
    }

    fun closeFile(file: File) {
        val currentIndex = openFiles.value.indexOf(file)
        openFiles.value -= file

        if(activeFile.value == file) {
            if(openFiles.value.isNotEmpty()) {
                val newIndex = if(currentIndex > 0) currentIndex - 1 else 0
                activeFile.value = openFiles.value[newIndex]
                loadFileContent(activeFile.value!!)
            } else {
                activeFile.value = null
                editorText.value = ""
            }
        }
    }

    fun switchTab(file: File) {
        activeFile.value = file
        loadFileContent(file)
    }

    private fun loadFileContent(file: File) {
        try {
            editorText.value = file.readText()
        } catch(e: Exception) {
            editorText.value = "Error reading file: ${e.message}"
        }
    }

    fun saveActiveFile() {
        activeFile.value?.let { file ->
            try {
                file.writeText(editorText.value)
                Logger.info("file-save", "Successfully saved ${file.absolutePath}")
            } catch(e: Exception) {
                Logger.error("file-save", "Failed to save ${file.absolutePath}: ${e.message}")
            }
        } ?: run {
            Logger.warning("file-save", "No file selected to save")
        }
    }
}
