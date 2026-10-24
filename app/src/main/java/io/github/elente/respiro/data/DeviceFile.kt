package io.github.elente.respiro.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One file of the device database: a common layer, a vendor layer or a tested model. See assets/devices/README.md. */
@Serializable
data class DeviceFile(
    val id: String,
    val name: String,
    val match: Map<String, String> = emptyMap(),
    val firmware: Firmware? = null,
    val extends: List<String> = emptyList(),
    val disable: List<DisableEntry> = emptyList(),
    val ask: List<AskEntry> = emptyList(),
    val keep: List<KeepEntry> = emptyList(),
    @SerialName("never_start") val neverStart: List<String> = emptyList(),
    val quirks: Quirks? = null,
    val tested: Tested? = null,
) {
    /** Models say how to recognise a TV; common and vendor layers don't. */
    val isModel: Boolean get() = match.isNotEmpty()

    /** Every package this file names, to check a file never lists one twice. */
    val packages: List<String> get() = disable.map { it.pkg } + ask.map { it.pkg } + keep.map { it.pkg }
}

@Serializable
data class Firmware(val android: String, val sdk: Int, val build: String)

@Serializable
data class DisableEntry(@SerialName("package") val pkg: String, val group: String, val reason: String)

@Serializable
data class AskEntry(@SerialName("package") val pkg: String, val category: String, val label: String)

@Serializable
data class KeepEntry(@SerialName("package") val pkg: String, val reason: String)

@Serializable
data class Quirks(
    @SerialName("reenabled_at_boot") val reenabledAtBoot: List<String> = emptyList(),
    @SerialName("remote_keys") val remoteKeys: List<RemoteKey> = emptyList(),
    val notes: List<String> = emptyList(),
)

@Serializable
data class RemoteKey(val scancode: Int, val button: String)

@Serializable
data class Tested(val date: String, val reboot: Boolean, val notes: String = "")
