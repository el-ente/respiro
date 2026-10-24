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

    /** Android refused, or the package was still on when checked after its batch. Undo still lists it, to be safe. */
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
            val batchOutcomes = batch.mapIndexed { index, pkg ->
                disableOne(pkg, wifiOwners).also { onProgress(outcomes.size + index + 1, packages.size) }
            }
            outcomes += confirm(batchOutcomes)
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
        // Recorded before the command: if the connection drops after pm ran, undo still knows about it.
        log.add(pkg)
        if (!shell.run("pm disable-user --user 0 $pkg").contains("new state: disabled-user")) {
            log.remove(pkg)
            return Outcome.Skipped(pkg, SkipReason.REFUSED)
        }
        return Outcome.Disabled(pkg)
    }

    /**
     * Checks one batch against the TV's disabled list. A package reported off but missing from it becomes a refusal,
     * but stays in the undo record: turning on something already on does no harm, losing track of something off does.
     * An empty list means the check itself failed, so nothing is changed.
     */
    private fun confirm(batch: List<Outcome>): List<Outcome> {
        val disabled = TvPackages.parse(shell.run("pm list packages -d"))
        if (disabled.isEmpty()) return batch
        return batch.map { if (it is Outcome.Disabled && it.pkg !in disabled) Outcome.Skipped(it.pkg, SkipReason.REFUSED) else it }
    }

    /**
     * Packages (or shared users) that created a saved Wi-Fi network, from `cname=` in the Wi-Fi dump. An empty dump
     * means the check failed, and switching off blind could take the TV offline, so nothing is done.
     */
    private fun wifiOwners(): Set<String> {
        val dump = shell.run("dumpsys wifi")
        check(dump.isNotBlank()) { "Couldn't read who owns the saved Wi-Fi networks" }
        return cname.findAll(dump).map { it.groupValues[1] }.toSet()
    }

    private companion object {
        const val BATCH_SIZE = 5
        val cname = Regex("""cname=([A-Za-z0-9_.]+)""")
    }
}
