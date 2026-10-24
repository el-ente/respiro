package io.github.elente.respiro.adb

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File

/**
 * A shell on the TV Respiro runs on, through the TV's own network ADB. The first connection shows Android's "Allow
 * debugging?" prompt for Respiro's key; after "Always allow" it connects silently.
 */
object SelfAdb {
    private const val HOST = "localhost"
    private const val PORT = 5555
    private const val CONNECT_TIMEOUT_MS = 5_000

    /** Long enough to read and accept "Allow debugging?"; past it the app shows what to do instead of hanging. */
    private const val SOCKET_TIMEOUT_MS = 30_000

    /** One connection for a series of commands, closed when [block] returns. */
    fun <T> session(context: Context, block: (Shell) -> T): T =
        Dadb.create(HOST, PORT, keyPair(context), CONNECT_TIMEOUT_MS, SOCKET_TIMEOUT_MS).use { adb ->
            block(Shell { command -> adb.shell(command).allOutput.trim() })
        }

    /** Respiro's own ADB key, made on first use and kept in the app's private files. */
    private fun keyPair(context: Context): AdbKeyPair {
        val private = File(context.filesDir, "adbkey")
        val public = File(context.filesDir, "adbkey.pub")
        if (!private.exists() || !public.exists()) AdbKeyPair.generate(private, public)
        return AdbKeyPair.read(private, public)
    }
}
