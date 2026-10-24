package io.github.elente.respiro.data

import android.content.pm.PackageManager

/** How Respiro shows a package: its app name, and what the database says about it. */
data class NamedPackage(val pkg: String, val label: String, val reason: String?)

class AppNames(private val packageManager: PackageManager, private val database: DeviceDatabase) {
    fun describe(pkg: String) = NamedPackage(pkg, label(pkg), database.reasonFor(pkg))

    /** The app's own name, also for disabled apps; the package name when it has none. */
    @Suppress("DEPRECATION")
    private fun label(pkg: String): String = runCatching {
        val info = packageManager.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS)
        info.loadLabel(packageManager).toString()
    }.getOrDefault(pkg)
}
