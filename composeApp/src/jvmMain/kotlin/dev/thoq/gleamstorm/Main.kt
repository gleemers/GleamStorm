package dev.thoq.gleamstorm

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
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
    Thread.currentThread().name = "glee-event-loop"
    Logger.debug("bootstrap", "Running on: Java ${System.getProperty("java.version")}")

    val launchDir: String = if(args.isEmpty()) "" else args[0]
    val windowState = rememberWindowState(
        size = DpSize(width = 1100.dp, height = 700.dp), position = WindowPosition.Aligned(Alignment.Center)
    )

    Window(
        onCloseRequest = ::shutdown,
        title = "GleamStorm",
        resizable = true,
        state = windowState,
        undecorated = true
    ) {
        App(window = window, dir = launchDir, onExit = ::shutdown, colorScheme = darkColorScheme())
    }
}

fun main(args: Array<String>) = runBlocking {
    if(System.getProperty("os.name").lowercase().contains("mac")) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", "GleamStorm")
    }

    Logger.init()
    Logger.info("bootstrap", "Starting GleamStorm...")

    launch(args = args)
}
