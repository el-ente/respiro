package io.github.elente.respiro.debloat

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageSizesTest {
    @Test
    fun `adds code and data per package from diskstats`() {
        val dump = """
            Package Names: ["com.a","com.b"]
            App Sizes: [1000,2000]
            App Data Sizes: [10,20]
            Cache Sizes: [1,2]
        """.trimIndent()
        assertEquals(mapOf("com.a" to 1010L, "com.b" to 2020L), Storage.parseAppSizes(dump))
    }
}
