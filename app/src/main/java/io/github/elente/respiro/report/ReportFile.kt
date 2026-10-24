package io.github.elente.respiro.report

import io.github.elente.respiro.adb.Shell
import java.io.IOException
import java.util.Base64

/**
 * Writes a text file where people can reach it (Download/), through the TV's shell: the app itself has no storage
 * permission. Sent in small base64 pieces, since one shell command can't carry a long text.
 */
object ReportFile {
    private const val CHUNK = 2048
    private const val OK = "ok"

    fun write(shell: Shell, path: String, text: String) {
        val encoded = Base64.getEncoder().encodeToString(text.toByteArray())
        shell.run("rm -f '$path'")
        encoded.chunked(CHUNK).forEach { piece ->
            if (shell.run("echo '$piece' | base64 -d >> '$path' && echo $OK") != OK) throw IOException("Couldn't write $path")
        }
    }
}
