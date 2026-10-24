package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell
import java.io.File

/** Saves the TV's state before the first change: packages, what was already off, and the settings. */
object Backup {
    private val sections = listOf(
        "packages" to "pm list packages",
        "disabled" to "pm list packages -d",
        "settings global" to "settings list global",
        "settings secure" to "settings list secure",
    )

    fun save(shell: Shell, dir: File, now: Long): File {
        dir.mkdirs()
        val file = File(dir, "backup-$now.txt")
        file.writeText(sections.joinToString("\n") { (title, command) -> "## $title\n${shell.run(command)}\n" })
        return file
    }
}
