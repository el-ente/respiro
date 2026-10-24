package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell

/** Free space on the TV's data partition, from `dumpsys diskstats` ("Data-Free: 704644K / 2708200K total"). */
object Storage {
    private val dataFree = Regex("""Data-Free: (\d+)K""")

    fun freeKb(shell: Shell): Long? = parseFreeKb(shell.run("dumpsys diskstats"))

    fun parseFreeKb(dump: String): Long? = dataFree.find(dump)?.groupValues?.get(1)?.toLongOrNull()
}
