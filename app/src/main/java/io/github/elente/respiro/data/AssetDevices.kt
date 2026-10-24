package io.github.elente.respiro.data

import android.content.res.AssetManager

/** Reads every JSON file under assets/devices into the database. */
object AssetDevices {
    private const val ROOT = "devices"

    fun load(assets: AssetManager): DeviceDatabase = DeviceDatabase.parse(jsonPaths(assets, ROOT).map { path ->
        assets.open(path).bufferedReader().use { it.readText() }
    })

    private fun jsonPaths(assets: AssetManager, dir: String): List<String> =
        assets.list(dir).orEmpty().flatMap { name ->
            val path = "$dir/$name"
            when {
                name.endsWith(".json") -> listOf(path)
                name.contains('.') -> emptyList()
                else -> jsonPaths(assets, path)
            }
        }
}
