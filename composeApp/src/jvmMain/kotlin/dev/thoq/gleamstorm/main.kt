package dev.thoq.gleamstorm

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.thoq.gleamstorm.plugin.EditorPluginManager
import dev.thoq.gleamstorm.utils.logger.Logger
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun shutdown(): Nothing = runBlocking {
    Logger.info("shutdown", "Shutting down GleamStorm...")

    EditorPluginManager().shutdown()
    Logger.shutdown()

    exitProcess(status = 0)
}

fun launch(args: Array<String>) = application {
    val launchDir: String = if(args.isEmpty()) "." else args[0]

    Logger.init()
    Logger.info("bootstrap", "Starting GleamStorm...")
    Logger.debug("bootstrap", "Running on: Java ${System.getProperty("java.version")}")

    Window(
        onCloseRequest = ::shutdown,
        title = "GleamStorm",
    ) {
        App(dir = launchDir, colorScheme = darkColorScheme())
    }
}

fun main(args: Array<String>) = runBlocking {
    launch(args = args)
}
