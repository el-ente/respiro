package io.github.elente.respiro.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GetpropTest {
    @Test
    fun `parses key value lines and skips the rest`() {
        val output = "[ro.product.device]: [C06]\n[ro.build.display.id]: [AR2101 release-keys]\nnoise\n[empty]: []"
        assertEquals(
            mapOf("ro.product.device" to "C06", "ro.build.display.id" to "AR2101 release-keys", "empty" to ""),
            Getprop.parse(output),
        )
    }
}
