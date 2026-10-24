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

    private val packageName = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$")

    /** A real package name: dotted segments of letters, digits and underscores, nothing a shell could read as more. */
    fun isValidName(pkg: String): Boolean = packageName.matches(pkg)

    /** Core packages, and anything that isn't a clean package name, since it ends up in a shell command. */
    fun isProtected(pkg: String): Boolean =
        !isValidName(pkg) ||
            pkg in exact ||
            pkg.startsWith(OVERLAY_MODULES) ||
            (pkg.startsWith(PROVIDERS) && pkg !in optionalProviders) ||
            coreModules.any { pkg == "com.android.$it" || pkg == "com.google.android.$it" }
}
