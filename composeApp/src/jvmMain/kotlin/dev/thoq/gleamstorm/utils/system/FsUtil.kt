package dev.thoq.gleamstorm.utils.system

import kotlinx.coroutines.coroutineScope
import java.io.File

object FsUtil {
    suspend fun walk(directory: String): List<String> = coroutineScope {
        val files = mutableListOf<String>()

        for(file in File(directory).listFiles()) {
            if(file.isDirectory) {
                files.addAll(walk(file.absolutePath))
            } else {
                files.add(file.absolutePath)
            }
        }

        return@coroutineScope files
    }
}