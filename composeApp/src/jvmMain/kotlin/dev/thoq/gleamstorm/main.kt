package dev.thoq.gleamstorm

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.thoq.gleamstorm.plugin.EditorPluginManager
import dev.thoq.gleamstorm.utils.logger.Logger
import kotlinx.coroutines.runBlocking

fun shutdown() = runBlocking {
    Logger.info("shutdown", "Setting down GleamStorm...")

    EditorPluginManager().shutdown()
    Logger.shutdown()
}

fun launch() = application {
    Logger.init()
    Logger.info("bootstrap", "Starting GleamStorm...")
    Logger.debug("bootstrap", "Running on: Java ${System.getProperty("java.version")}")

    Window(
        onCloseRequest = ::exitApplication,
        title = "GleamStorm",
    ) {
        App()
    }
}

fun main() = runBlocking {
    Runtime.getRuntime().addShutdownHook(Thread {
        Thread.currentThread().name = "ShutdownHook"
        shutdown()
    })

    launch()
}
