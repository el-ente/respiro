package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell
import io.github.elente.respiro.data.DebloatPlan

/** The packages on this TV, which are already switched off, and which came with it. */
data class TvPackages(val installed: Set<String>, val disabled: Set<String>, val preinstalled: Set<String> = installed) {
    fun isActive(pkg: String): Boolean = pkg in installed && pkg !in disabled

    /** Installed by the user: Respiro uninstalls these instead of switching them off. */
    fun isUserApp(pkg: String): Boolean = pkg in installed && pkg !in preinstalled

    companion object {
        fun read(shell: Shell) = TvPackages(
            installed = parse(shell.run("pm list packages")),
            disabled = parse(shell.run("pm list packages -d")),
            preinstalled = parse(shell.run("pm list packages -s")),
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
