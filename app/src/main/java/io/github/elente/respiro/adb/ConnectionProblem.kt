package io.github.elente.respiro.adb

import java.net.ConnectException
import java.net.SocketTimeoutException

/** Why Respiro couldn't use the TV's debugging, in terms of what the user can do about it. */
enum class ConnectionProblem {
    /** Nothing listens on the port: USB or network debugging is off. */
    DEBUGGING_OFF,

    /** Connected but never authorised: the prompt was ignored or declined, or a stuck one is hiding it. */
    NOT_ALLOWED,

    /** Anything else: the details are shown as they are. */
    OTHER;

    companion object {
        fun of(error: Throwable): ConnectionProblem = when (rootCause(error)) {
            is ConnectException -> DEBUGGING_OFF
            is SocketTimeoutException -> NOT_ALLOWED
            else -> OTHER
        }

        private fun rootCause(error: Throwable): Throwable = generateSequence(error) { it.cause }.last()
    }
}
