package io.github.elente.respiro.adb

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class ConnectionProblemTest {
    @Test
    fun `refused means debugging is off`() {
        assertEquals(ConnectionProblem.DEBUGGING_OFF, ConnectionProblem.of(ConnectException("Connection refused")))
    }

    @Test
    fun `a timeout, even wrapped, means it wasn't allowed`() {
        assertEquals(ConnectionProblem.NOT_ALLOWED, ConnectionProblem.of(IOException("auth", SocketTimeoutException())))
    }

    @Test
    fun `anything else is other`() {
        assertEquals(ConnectionProblem.OTHER, ConnectionProblem.of(IllegalStateException("x")))
    }
}
