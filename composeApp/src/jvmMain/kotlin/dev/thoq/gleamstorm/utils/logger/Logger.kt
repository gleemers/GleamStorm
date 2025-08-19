@file:Suppress("unused")

package dev.thoq.gleamstorm.utils.logger

import dev.thoq.gleamstorm.utils.misc.StringUtil.stripAnsi
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.util.*

object Logger {
    const val RESET = "\u001B[0m"

    private val logDir = File("./logs")
    private val theDate = getDate().replace(" ", "-")
    private var writer: BufferedWriter? = null

    fun getDate(): String = Date.from(Date().toInstant()).toString()

    fun getThread(): String = Thread.currentThread().name.padEnd(10)

    fun formatLevel(level: LogLevel): String = level.toString().padEnd(7)

    fun getColor(level: LogLevel): String = when(level) {
        LogLevel.Info -> "\u001B[35m"
        LogLevel.Warning -> "\u001B[33m"
        LogLevel.Error -> "\u001B[31m"
        LogLevel.Debug -> "\u001B[34m"
    }

    fun init() {
        if(!logDir.exists())
            logDir.mkdirs()

        if(writer == null) {
            try {
                writer = File("$logDir/$theDate-GleamStorm.log").bufferedWriter()
            } catch(ex: Exception) {
                println("Failed to initialize log writer: ${ex.message}")
            }
        }
    }

    fun log(message: String, logLevel: LogLevel) {
        val formattedMessage =
            "$theDate | ${getThread()} | ${getColor(logLevel)}${formatLevel(logLevel)}$RESET | $message"

        println(formattedMessage)

        try {
            writer?.let {
                it.write(formattedMessage.stripAnsi())
                it.newLine()
                it.flush()
            }
        } catch(ex: FileNotFoundException) {
            warning("logger", "Could not write to log file: ${ex.message}")
        } catch(ex: Exception) {
            warning("logger", "Error writing to log file: ${ex.message}")
            ex.printStackTrace()
        }
    }

    fun info(task: String = "", message: String) = log("[$task] $message", LogLevel.Info)

    fun warning(task: String = "", message: String) = log("[$task] $message", LogLevel.Warning)

    fun error(task: String = "", message: String) = log("[$task] $message", LogLevel.Error)

    fun debug(task: String = "", message: String) = log("[$task] $message", LogLevel.Debug)

    fun shutdown() {
        info("shutdown", "Shutting down logger...")
        writer?.close()
        writer = null
    }
}