package io.github.elente.respiro.report

/**
 * What a contributor needs to add an unknown TV to the database: a model file to edit, plus the TV's preinstalled
 * and disabled packages. Only the properties below go in; serial numbers, apps the user installed, Wi-Fi names and
 * addresses never do.
 */
object DeviceReport {
    private val properties = listOf(
        "ro.product.manufacturer",
        "ro.product.brand",
        "ro.product.model",
        "ro.product.device",
        "ro.product.name",
        "ro.build.display.id",
        "ro.build.version.release",
        "ro.build.version.sdk",
        "ro.build.version.security_patch",
        "ro.board.platform",
        "ro.product.cpu.abilist",
        "ro.config.low_ram",
    )

    /** Download/respiro-<device>.txt, with the device name reduced to safe characters. */
    fun path(props: Map<String, String>) = "/sdcard/Download/respiro-${slug(props["ro.product.device"] ?: "tv")}.txt"

    fun build(props: Map<String, String>, preinstalled: Set<String>, disabled: Set<String>, switchedOff: List<String>): String {
        val manufacturer = props["ro.product.manufacturer"].orEmpty()
        val device = props["ro.product.device"].orEmpty()
        return buildString {
            appendLine("# Respiro device report")
            appendLine("# Edit the model file, test it on the TV with a reboot, and open a pull request.")
            appendLine()
            appendLine("## Model file draft (app/src/main/assets/devices/models/${slug(manufacturer)}/${slug(device)}.json)")
            appendLine(modelDraft(props, switchedOff))
            appendLine()
            appendLine("## Properties")
            properties.forEach { key -> props[key]?.let { appendLine("$key=$it") } }
            appendLine()
            appendLine("## Preinstalled packages (disabled ones marked)")
            preinstalled.sorted().forEach { appendLine(if (it in disabled) "$it  [disabled]" else it) }
        }
    }

    private fun modelDraft(props: Map<String, String>, switchedOff: List<String>): String {
        val manufacturer = props["ro.product.manufacturer"].orEmpty()
        val device = props["ro.product.device"].orEmpty()
        val match = listOf("ro.product.manufacturer", "ro.product.device", "ro.product.name")
            .mapNotNull { key -> props[key]?.let { "    \"$key\": \"$it\"" } }
            .joinToString(",\n")
        val disable = switchedOff.joinToString(",\n") { "    { \"package\": \"$it\", \"group\": \"TODO\", \"reason\": \"TODO\" }" }
        return """
            |{
            |  "id": "${slug(manufacturer)}/${slug(device)}",
            |  "name": "${props["ro.product.brand"].orEmpty()} ${props["ro.product.model"].orEmpty()}",
            |  "match": {
            |$match
            |  },
            |  "firmware": { "android": "${props["ro.build.version.release"].orEmpty()}", "sdk": ${props["ro.build.version.sdk"] ?: 0}, "build": "${props["ro.build.display.id"].orEmpty()}" },
            |  "extends": ["common/google-tv"],
            |  "disable": [
            |$disable
            |  ],
            |  "tested": { "date": "TODO", "reboot": false, "notes": "TODO" }
            |}
        """.trimMargin()
    }

    private fun slug(text: String) = text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
