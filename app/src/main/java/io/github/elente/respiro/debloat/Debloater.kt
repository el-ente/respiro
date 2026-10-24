package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell
import io.github.elente.respiro.data.ProtectedPackages

/** What happened to one package. */
sealed interface Outcome {
    val pkg: String

    data class Disabled(override val pkg: String) : Outcome

    data class Skipped(override val pkg: String, val reason: SkipReason) : Outcome
}

enum class SkipReason {
    /** A core package, or not a clean package name. */
    PROTECTED,

    /** It owns a saved Wi-Fi network; Android deletes the network when it's disabled, taking the TV offline. */
    OWNS_WIFI,

    /** Android refused, or the package was still on when checked after its batch. */
    REFUSED,
}

/**
 * Switches packages off with `pm disable-user`, one command each, in small batches checked against the TV after each
 * batch, recording every change so it can be undone. Never uninstalls.
 */
class Debloater(private val shell: Shell, private val log: ChangeLog) {

    fun disable(packages: List<String>, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<Outcome> {
        val wifiOwners = wifiOwners()
        val outcomes = mutableListOf<Outcome>()
        packages.chunked(BATCH_SIZE).forEach { batch ->
            batch.forEach { pkg ->
                outcomes += disableOne(pkg, wifiOwners)
                onProgress(outcomes.size, packages.size)
            }
            confirmBatch(outcomes)
        }
        return outcomes
    }

    /** Turns a package back on; true when Android confirms it. */
    fun enable(pkg: String): Boolean {
        if (!ProtectedPackages.isValidName(pkg)) return false
        val enabled = shell.run("pm enable --user 0 $pkg").contains("new state: enabled")
        if (enabled) log.remove(pkg)
        return enabled
    }

    private fun disableOne(pkg: String, wifiOwners: Set<String>): Outcome {
        if (ProtectedPackages.isProtected(pkg)) return Outcome.Skipped(pkg, SkipReason.PROTECTED)
        if (pkg in wifiOwners) return Outcome.Skipped(pkg, SkipReason.OWNS_WIFI)
        if (!shell.run("pm disable-user --user 0 $pkg").contains("new state: disabled-user")) {
            return Outcome.Skipped(pkg, SkipReason.REFUSED)
        }
        log.add(pkg)
        return Outcome.Disabled(pkg)
    }

    /** Anything reported off but not in the TV's disabled list becomes a refusal, so the record matches the TV. */
    private fun confirmBatch(outcomes: MutableList<Outcome>) {
        val disabled = TvPackages.parse(shell.run("pm list packages -d"))
        outcomes.replaceAll { outcome ->
            if (outcome is Outcome.Disabled && outcome.pkg !in disabled) {
                log.remove(outcome.pkg)
                Outcome.Skipped(outcome.pkg, SkipReason.REFUSED)
            } else {
                outcome
            }
        }
    }

    /** Packages (or shared users) that created a saved Wi-Fi network, from `cname=` in the Wi-Fi dump. */
    private fun wifiOwners(): Set<String> =
        cname.findAll(shell.run("dumpsys wifi")).map { it.groupValues[1] }.toSet()

    private companion object {
        const val BATCH_SIZE = 5
        val cname = Regex("""cname=([A-Za-z0-9_.]+)""")
    }
}
