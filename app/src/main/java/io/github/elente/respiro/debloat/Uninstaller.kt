package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell
import io.github.elente.respiro.data.ProtectedPackages

/**
 * Removes apps the user installed, one at a time and only when they pick it. Preinstalled apps are never offered:
 * those are only ever switched off. Never offered either: Respiro, any app that can be Home, and the keyboard and
 * accessibility services in use, since the TV relies on them.
 */
class Uninstaller(private val shell: Shell, private val self: String) {

    /** The apps the user may remove. Empty when the home screens can't be read: better offer nothing than guess. */
    fun removable(): Set<String> {
        val homes = packageNames.findAll(
            shell.run("cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME"),
        ).map { it.groupValues[1] }.toSet()
        if (homes.isEmpty()) return emptySet()
        val inUse = homes + self + settingPackages("enabled_accessibility_services") + settingPackages("default_input_method")
        return TvPackages.parse(shell.run("pm list packages -3"))
            .filter { it !in inUse && ProtectedPackages.isValidName(it) }
            .toSet()
    }

    /** Packages named in a secure setting holding components ("pkg/.Class", several joined by ':'). */
    private fun settingPackages(key: String): Set<String> =
        shell.run("settings get secure $key").split(':').map { it.trim().substringBefore('/') }.filter { it.isNotEmpty() }.toSet()

    /** True when Android removed it. Checks again that the app is still removable right before running the command. */
    fun uninstall(pkg: String): Boolean {
        if (pkg !in removable()) return false
        return shell.run("pm uninstall $pkg").trim() == SUCCESS
    }

    private companion object {
        const val SUCCESS = "Success"
        val packageNames = Regex("packageName=([A-Za-z0-9_.]+)")
    }
}
