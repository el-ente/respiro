package io.github.elente.respiro.debloat

import org.junit.Assert.assertEquals
import org.junit.Test

class ClearCachesTest {
    @Test
    fun `asks Android to trim every cache`() {
        val commands = mutableListOf<String>()
        Storage.clearCaches(io.github.elente.respiro.adb.Shell { commands += it; "" })
        assertEquals(listOf("pm trim-caches 999G"), commands)
    }
}
