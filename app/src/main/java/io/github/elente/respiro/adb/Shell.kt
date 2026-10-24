package io.github.elente.respiro.adb

/** Runs one shell command on the TV and returns its output. Tests use a fake one. */
fun interface Shell {
    fun run(command: String): String
}
