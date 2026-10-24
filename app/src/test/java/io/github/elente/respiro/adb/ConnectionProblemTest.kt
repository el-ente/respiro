package io.github.elente.respiro.adb

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class ConnectionProblemTest {
    @Test
    fun `refused means debugging is off`() {
        assertEquals(ConnectionProblem.DEBUGGING_OFF, ConnectionProblem.of(ConnectionFailed(ConnectException("Connection refused"))))
    }

    @Test
    fun `a timeout, even wrapped, means it wasn't allowed`() {
        assertEquals(ConnectionProblem.NOT_ALLOWED, ConnectionProblem.of(ConnectionFailed(IOException("auth", SocketTimeoutException()))))
    }

    @Test
    fun `any other connection failure is other`() {
        assertEquals(ConnectionProblem.OTHER, ConnectionProblem.of(ConnectionFailed(IllegalStateException("x"))))
    }

    @Test
    fun `a timeout once working is not blamed on authorisation`() {
        assertEquals(ConnectionProblem.DURING_WORK, ConnectionProblem.of(SocketTimeoutException()))
    }
}
