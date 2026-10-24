package io.github.elente.respiro.data

/**
 * Android's core packages. Without them a TV may not boot again (an original Mi TV Stick never did), so no database
 * file can disable them: this list lives in code and has no override.
 */
object ProtectedPackages {
    private val exact = setOf(
        "android",
        "com.android.systemui",
        "com.android.shell",
        "com.android.phone",
        "com.android.settings",
        "com.android.tv.settings",
    )

    /** Core modules shipped under either prefix, e.g. com.android.permissioncontroller and com.google.android.… */
    private val coreModules = setOf("permissioncontroller", "packageinstaller", "ext.services", "ext.shared", "modulemetadata")

    private const val PROVIDERS = "com.android.providers."
    private val optionalProviders = setOf("calendar", "contacts", "userdictionary", "blockednumber").map { PROVIDERS + it }.toSet()

    private const val OVERLAY_MODULES = "com.google.android.overlay.modules"

    fun isProtected(pkg: String): Boolean =
        pkg in exact ||
            pkg.startsWith(OVERLAY_MODULES) ||
            (pkg.startsWith(PROVIDERS) && pkg !in optionalProviders) ||
            coreModules.any { pkg == "com.android.$it" || pkg == "com.google.android.$it" }
}
