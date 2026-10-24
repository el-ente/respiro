package io.github.elente.respiro

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import kotlin.concurrent.thread

/** Spike: proves Respiro can reach its own TV's ADB and read what a debloat needs. */
class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply {
            textSize = 20f
            setPadding(PADDING, PADDING, PADDING, PADDING)
            text = "Connecting to this TV's ADB…\nIf Android asks to allow debugging, choose Always allow."
        }
        setContentView(status)
        thread { probe() }
    }

    private fun probe() {
        val report = runCatching {
            PROBES.joinToString("\n") { (label, command) -> "$label: ${SelfAdb.shell(this, command)}" }
        }.getOrElse { "Couldn't connect: ${it.javaClass.simpleName}: ${it.message}" }
        runOnUiThread { status.text = report }
    }

    private companion object {
        const val PADDING = 48
        val PROBES = listOf(
            "Manufacturer" to "getprop ro.product.manufacturer",
            "Model" to "getprop ro.product.model",
            "Device" to "getprop ro.product.device",
            "Build" to "getprop ro.build.display.id",
            "Shell user" to "id -un",
            "Disabled packages" to "pm list packages -d | wc -l",
        )
    }
}
