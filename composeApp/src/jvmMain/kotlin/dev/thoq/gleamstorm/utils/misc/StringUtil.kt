package dev.thoq.gleamstorm.utils.misc

object StringUtil {
    fun String.stripAnsi(): String {
        val ansiPattern = Regex("\u001B(?:[@-Z\\\\-_]|\\[[0-?]*[ -/]*[@-~])")

        return this.replace(ansiPattern, "")
    }
}