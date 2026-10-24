package io.github.elente.respiro.report

import io.github.elente.respiro.adb.Shell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class DeviceReportTest {
    private val props = mapOf(
        "ro.product.manufacturer" to "Acme",
        "ro.product.brand" to "ACME",
        "ro.product.model" to "Smart TV 55",
        "ro.product.device" to "ac55",
        "ro.build.version.release" to "11",
        "ro.build.version.sdk" to "30",
        "ro.build.display.id" to "R1",
        "ro.serialno" to "SECRET123",
        "persist.sys.wifi.ssid" to "HomeNet",
    )

    @Test
    fun `draft matches the TV and lists what was switched off`() {
        val report = DeviceReport.build(props, setOf("com.acme.ads", "com.acme.tv"), setOf("com.acme.ads"), listOf("com.acme.ads"))
        assertTrue("\"id\": \"acme/ac55\"" in report)
        assertTrue("\"ro.product.device\": \"ac55\"" in report)
        assertTrue("{ \"package\": \"com.acme.ads\", \"group\": \"TODO\"" in report)
        assertTrue("com.acme.ads  [disabled]" in report)
    }

    @Test
    fun `never includes anything that identifies one person's TV`() {
        val report = DeviceReport.build(props, emptySet(), emptySet(), emptyList())
        assertFalse("SECRET123" in report)
        assertFalse("HomeNet" in report)
    }

    @Test
    fun `writes the file in base64 pieces that decode to the text`() {
        val commands = mutableListOf<String>()
        val text = "x".repeat(5000)
        ReportFile.write(Shell { commands += it; "" }, "/sdcard/Download/r.txt", text)
        assertEquals("rm -f '/sdcard/Download/r.txt'", commands.first())
        val pieces = commands.drop(1).map { it.substringAfter("echo '").substringBefore("'") }
        assertTrue(pieces.size > 1)
        assertEquals(text, String(Base64.getDecoder().decode(pieces.joinToString(""))))
    }
}
