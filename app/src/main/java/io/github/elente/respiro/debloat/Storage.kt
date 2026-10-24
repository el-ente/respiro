package io.github.elente.respiro.debloat

import io.github.elente.respiro.adb.Shell
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/** Free space on the TV's data partition, from `dumpsys diskstats` ("Data-Free: 704644K / 2708200K total"). */
object Storage {
    private val dataFree = Regex("""Data-Free: (\d+)K""")

    fun freeKb(shell: Shell): Long? = parseFreeKb(shell.run("dumpsys diskstats"))

    /**
     * Asks Android to clear every app's cache: `pm trim-caches` with a target larger than the disk frees all it can.
     * Caches are rebuilt by apps as needed; no data or settings are lost.
     */
    fun clearCaches(shell: Shell) {
        shell.run("pm trim-caches $TRIM_EVERYTHING")
    }

    private const val TRIM_EVERYTHING = "999G"

    fun parseFreeKb(dump: String): Long? = dataFree.find(dump)?.groupValues?.get(1)?.toLongOrNull()

    /** Bytes each app takes, its code plus its data (cache included), from the same dump. */
    fun appSizes(shell: Shell): Map<String, Long> = parseAppSizes(shell.run("dumpsys diskstats"))

    fun parseAppSizes(dump: String): Map<String, Long> {
        val names = array(dump, "Package Names") ?: return emptyMap()
        val code = array(dump, "App Sizes").orEmpty()
        val data = array(dump, "App Data Sizes").orEmpty()
        return names.mapIndexed { index, name ->
            name.jsonPrimitive.content to number(code, index) + number(data, index)
        }.toMap()
    }

    private fun array(dump: String, label: String): JsonArray? = dump.lineSequence()
        .firstOrNull { it.startsWith("$label: ") }
        ?.let { runCatching { Json.parseToJsonElement(it.removePrefix("$label: ")) as JsonArray }.getOrNull() }

    private fun number(values: List<kotlinx.serialization.json.JsonElement>, index: Int): Long =
        values.getOrNull(index)?.jsonPrimitive?.long ?: 0
}
