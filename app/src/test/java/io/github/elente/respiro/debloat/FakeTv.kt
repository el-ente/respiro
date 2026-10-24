package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell

/** A TV for tests: tracks which packages are off and answers the commands Respiro sends. */
class FakeTv(
    installed: Set<String>,
    disabled: Set<String> = emptySet(),
    private val wifiDump: String = "Wi-Fi is enabled",
    private val refuses: Set<String> = emptySet(),
    private val failedChecks: Int = 0,
    private val preinstalled: Set<String> = installed,
    private val home: String = "io.github.elente.daiji",
    private val secureSettings: Map<String, String> = emptyMap(),
) : Shell {
    private val installed = installed.toMutableSet()
    val disabled = disabled.toMutableSet()
    val commands = mutableListOf<String>()
    private var checksToFail = failedChecks

    override fun run(command: String): String {
        commands += command
        val pkg = command.substringAfterLast(' ')
        return when {
            command == "pm list packages" -> installed.joinToString("\n") { "package:$it" }
            command == "pm list packages -s" -> preinstalled.joinToString("\n") { "package:$it" }
            command == "pm list packages -3" -> (installed - preinstalled).joinToString("\n") { "package:$it" }
            command.startsWith("cmd package query-activities") ->
                "  ActivityInfo:\n    packageName=$home\n  ActivityInfo:\n    packageName=com.android.tv.settings"
            command.startsWith("settings get secure ") -> secureSettings[command.substringAfterLast(' ')] ?: "null"
            command.startsWith("pm uninstall ") && !command.startsWith("pm uninstall-system-updates") && pkg in installed -> {
                installed -= pkg
                "Success"
            }
            command == "pm list packages -d" && checksToFail > 0 -> "".also { checksToFail-- }
            command == "pm list packages -d" -> disabled.joinToString("\n") { "package:$it" }
            command == "dumpsys wifi" -> wifiDump
            command.startsWith("pm disable-user --user 0 ") && pkg in installed && pkg !in refuses -> {
                disabled += pkg
                "Package $pkg new state: disabled-user"
            }
            command.startsWith("pm uninstall-system-updates ") || command.startsWith("pm clear --user 0 ") -> "Success"
            command.startsWith("pm enable --user 0 ") && pkg in installed -> {
                disabled -= pkg
                "Package $pkg new state: enabled"
            }
            else -> "Error"
        }
    }
}
