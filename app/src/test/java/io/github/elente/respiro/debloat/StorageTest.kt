package io.github.elente.respiro.debloat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StorageTest {
    @Test
    fun `reads free space from diskstats`() {
        assertEquals(704644L, Storage.parseFreeKb("Data-Free: 704644K / 2708200K total = 26% free"))
        assertNull(Storage.parseFreeKb("nothing"))
    }
}
