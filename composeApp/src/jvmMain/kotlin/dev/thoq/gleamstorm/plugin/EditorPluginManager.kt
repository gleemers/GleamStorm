@file:Suppress("unused")

package dev.thoq.gleamstorm.plugin

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import dev.thoq.gleamstorm.utils.logger.Logger

class EditorPluginManager {
    private val plugins = mutableListOf<IEditorPlugin>()
    private val fileExtensionPlugins = mutableMapOf<String, IEditorPlugin>()

    fun addPlugin(plugin: IEditorPlugin): EditorPluginManager {
        plugins.add(plugin)
        Logger.info("plugins", "Loaded plugin: ${plugin.name}")

        return this
    }

    fun getPluginForFile(fileName: String): IEditorPlugin? {
        val extension = fileName.substringAfterLast(".", "")

        Logger.debug("plugins", "foundExtension: $extension,")
        Logger.debug("plugins", "getPluginForFile: $fileName, $extension")

        fileExtensionPlugins[extension]?.let { return it }

        val compatiblePlugin = plugins.find { it.isCompatible(extension) }

        compatiblePlugin?.let { fileExtensionPlugins[extension] = it }

        return compatiblePlugin
    }

    fun getAllPlugins(): List<IEditorPlugin> = plugins.toList()

    fun hasPluginForFile(fileName: String): Boolean {
        return getPluginForFile(fileName) != null
    }

    @Composable
    fun renderEditorWithPlugin(
        fileName: String,
        text: MutableState<String>,
        placeholder: String,
        colorScheme: ColorScheme,
        modifier: Modifier,
        fallbackRenderer: @Composable (MutableState<String>, String, ColorScheme, Modifier) -> Unit,
    ) {
        val plugin = getPluginForFile(fileName)

        if(plugin != null) {
            plugin.renderEditor(text, placeholder, colorScheme, modifier)
        } else {
            fallbackRenderer(text, placeholder, colorScheme, modifier)
        }
    }

    fun shutdown() {
        Logger.debug("plugins", "Unloading plugins...")

        for(plugin in plugins) {
            Logger.debug("plugins", "Forcefully unloading plugin: ${plugin.name}")
            plugin.onShutdown()
        }
    }
}
