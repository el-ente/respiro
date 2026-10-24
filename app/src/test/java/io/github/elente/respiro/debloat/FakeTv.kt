package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell

/** A TV for tests: tracks which packages are off and answers the commands Respiro sends. */
class FakeTv(
    installed: Set<String>,
    disabled: Set<String> = emptySet(),
    private val wifiDump: String = "",
    private val refuses: Set<String> = emptySet(),
) : Shell {
    private val installed = installed.toMutableSet()
    val disabled = disabled.toMutableSet()
    val commands = mutableListOf<String>()

    override fun run(command: String): String {
        commands += command
        val pkg = command.substringAfterLast(' ')
        return when {
            command == "pm list packages" -> installed.joinToString("\n") { "package:$it" }
            command == "pm list packages -d" -> disabled.joinToString("\n") { "package:$it" }
            command == "dumpsys wifi" -> wifiDump
            command.startsWith("pm disable-user --user 0 ") && pkg in installed && pkg !in refuses -> {
                disabled += pkg
                "Package $pkg new state: disabled-user"
            }
            command.startsWith("pm enable --user 0 ") && pkg in installed -> {
                disabled -= pkg
                "Package $pkg new state: enabled"
            }
            else -> "Error"
        }
    }
}
