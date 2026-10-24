package io.github.elente.respiro.adb

import java.net.ConnectException
import java.net.SocketTimeoutException

/** Respiro couldn't connect to, or wasn't authorised on, the TV's debugging: before any command ran. */
class ConnectionFailed(cause: Throwable) : Exception(cause.message, cause)

/** Why Respiro couldn't use the TV's debugging, in terms of what the user can do about it. */
enum class ConnectionProblem {
    /** Nothing listens on the port: USB or network debugging is off. */
    DEBUGGING_OFF,

    /** Connected but never authorised: the prompt was ignored or declined, or a stuck one is hiding it. */
    NOT_ALLOWED,

    /** Connecting failed some other way: the details are shown as they are. */
    OTHER,

    /** The connection worked but dropped, or a command failed, while Respiro was working. */
    DURING_WORK;

    companion object {
        fun of(error: Throwable): ConnectionProblem = when {
            error !is ConnectionFailed -> DURING_WORK
            rootCause(error) is ConnectException -> DEBUGGING_OFF
            rootCause(error) is SocketTimeoutException -> NOT_ALLOWED
            else -> OTHER
        }

        private fun rootCause(error: Throwable): Throwable = generateSequence(error) { it.cause }.last()
    }
}
