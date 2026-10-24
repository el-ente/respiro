package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell
import io.github.elente.respiro.data.ProtectedPackages

/**
 * Removes apps the user installed, one at a time and only when they pick it. Preinstalled apps are never offered:
 * those are only ever switched off. Respiro itself and the app that is Home are never offered either.
 */
class Uninstaller(private val shell: Shell, private val self: String) {

    /** The apps the user may remove: installed by them, not Respiro, not the current home screen. */
    fun removable(): Set<String> {
        val home = shell.run("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME")
            .lineSequence().last().substringBefore('/')
        return TvPackages.parse(shell.run("pm list packages -3"))
            .filter { it != self && it != home && ProtectedPackages.isValidName(it) }
            .toSet()
    }

    /** True when Android removed it. Checks again that the app is still removable right before running the command. */
    fun uninstall(pkg: String): Boolean {
        if (pkg !in removable()) return false
        return shell.run("pm uninstall $pkg").trim() == SUCCESS
    }

    private companion object {
        const val SUCCESS = "Success"
    }
}
