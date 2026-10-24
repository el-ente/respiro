package io.github.elente.respiro.data

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** How Respiro shows a package: its app name, and what the database says about it. */
data class NamedPackage(val pkg: String, val label: String, val reason: String?)

class AppNames(private val packageManager: PackageManager, private val database: DeviceDatabase) {
    fun describe(pkg: String) = NamedPackage(pkg, label(pkg), database.reasonFor(pkg))

    /** The app's icon at [sizePx], for lists people scan by sight; null when it has none. */
    fun icon(pkg: String, sizePx: Int): ImageBitmap? = runCatching {
        packageManager.getApplicationIcon(pkg).toBitmap(sizePx, sizePx).asImageBitmap()
    }.getOrNull()

    /** The app's own name, also for disabled apps; the package name when it has none. */
    @Suppress("DEPRECATION")
    private fun label(pkg: String): String = runCatching {
        val info = packageManager.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS)
        info.loadLabel(packageManager).toString()
    }.getOrDefault(pkg)
}
