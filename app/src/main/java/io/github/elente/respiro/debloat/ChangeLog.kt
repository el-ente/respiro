package io.github.elente.respiro.debloat

import java.io.File

/** Every package Respiro switched off and hasn't turned back on, one per line, so any of it can be undone. */
class ChangeLog(private val file: File) {
    @Synchronized
    fun all(): List<String> = if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()

    @Synchronized
    fun add(pkg: String) {
        if (pkg !in all()) file.appendText("$pkg\n")
    }

    @Synchronized
    fun remove(pkg: String) = file.writeText(all().filter { it != pkg }.joinToString("") { "$it\n" })
}
