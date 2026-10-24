package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell
import io.github.elente.respiro.data.DebloatPlan

/** The packages on this TV, and which of them are already switched off. */
data class TvPackages(val installed: Set<String>, val disabled: Set<String>) {
    fun isActive(pkg: String): Boolean = pkg in installed && pkg !in disabled

    companion object {
        fun read(shell: Shell) = TvPackages(
            installed = parse(shell.run("pm list packages")),
            disabled = parse(shell.run("pm list packages -d")),
        )

        /** `pm list packages` prints one "package:name" per line. */
        fun parse(output: String): Set<String> = output.lineSequence()
            .map { it.trim().removePrefix("package:") }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}

/** Only what is installed and still on: nothing to do for the rest. */
fun DebloatPlan.forTv(tv: TvPackages): DebloatPlan = copy(
    disable = disable.filter { tv.isActive(it.pkg) },
    ask = ask.filter { tv.isActive(it.pkg) },
)
