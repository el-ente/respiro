package io.github.elente.respiro.data

import kotlinx.serialization.json.Json

/** What Respiro would do on one TV, after merging the model's layers. */
data class DebloatPlan(
    val model: DeviceFile?,
    val disable: List<DisableEntry>,
    val ask: List<AskEntry>,
    val keep: Set<String>,
    val neverStart: Set<String>,
)

/** The device database, parsed. Files are addressed by id, e.g. "vendors/tcl" or "models/tcl/c06-2k-gb". */
class DeviceDatabase(files: List<DeviceFile>) {
    private val byId: Map<String, DeviceFile> = files.associateBy { it.id }

    init {
        val repeated = files.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(repeated.isEmpty()) { "Device files share an id: $repeated" }
    }

    val files: Collection<DeviceFile> get() = byId.values

    /** The tested model whose every match property equals the TV's; the most specific wins. Null for an unknown TV. */
    fun match(props: Map<String, String>): DeviceFile? = byId.values
        .filter { it.isModel && it.match.all { (key, value) -> props[key] == value } }
        .maxByOrNull { it.match.size }

    /** For an unknown TV only the common layer applies: it holds what is safe on any Android TV with Google services. */
    fun plan(model: DeviceFile?): DebloatPlan {
        val layers = model?.let(::layersOf) ?: listOfNotNull(byId[COMMON_LAYER])
        val keep = layers.flatMap { layer -> layer.keep.map { it.pkg } }.toSet()
        val allowed = { pkg: String -> pkg !in keep && !ProtectedPackages.isProtected(pkg) }
        val ask = layers.flatMap { it.ask }.distinctBy { it.pkg }.filter { allowed(it.pkg) }
        val askFirst = ask.map { it.pkg }.toSet()
        return DebloatPlan(
            model = model,
            // A package one layer disables and another asks about is asked about: never switch off what someone may use.
            disable = layers.flatMap { it.disable }.distinctBy { it.pkg }.filter { allowed(it.pkg) && it.pkg !in askFirst },
            ask = ask,
            keep = keep,
            neverStart = layers.flatMap { it.neverStart }.toSet(),
        )
    }

    /** The files a model extends, in order, then the model itself. */
    fun layersOf(model: DeviceFile): List<DeviceFile> = model.extends.mapNotNull { byId[it] } + model

    companion object {
        const val COMMON_LAYER = "common/google-tv"

        private val json = Json { ignoreUnknownKeys = false }

        fun parse(texts: List<String>): DeviceDatabase = DeviceDatabase(texts.map { json.decodeFromString<DeviceFile>(it) })
    }
}
