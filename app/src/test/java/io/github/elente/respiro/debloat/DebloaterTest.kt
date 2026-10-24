package io.github.elente.respiro.debloat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DebloaterTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun log() = ChangeLog(folder.newFile())

    @Test
    fun `disables each package with its own command and records it`() {
        val tv = FakeTv(installed = setOf("com.tcl.a", "com.tcl.b"))
        val log = log()
        val outcomes = Debloater(tv, log).disable(listOf("com.tcl.a", "com.tcl.b"))
        assertEquals(listOf(Outcome.Disabled("com.tcl.a"), Outcome.Disabled("com.tcl.b")), outcomes)
        assertEquals(listOf("com.tcl.a", "com.tcl.b"), log.all())
        assertTrue("pm disable-user --user 0 com.tcl.a" in tv.commands)
        assertEquals(setOf("com.tcl.a", "com.tcl.b"), tv.disabled)
    }

    @Test
    fun `removes updates and data before disabling, so the disabled state is set last`() {
        val tv = FakeTv(installed = setOf("com.tcl.a"))
        Debloater(tv, log()).disable(listOf("com.tcl.a"))
        val sequence = tv.commands.filter { it.endsWith(" com.tcl.a") }
        assertEquals(
            listOf("pm uninstall-system-updates com.tcl.a", "pm clear --user 0 com.tcl.a", "pm disable-user --user 0 com.tcl.a"),
            sequence,
        )
    }

    @Test
    fun `never touches a protected package or a malformed name`() {
        val tv = FakeTv(installed = setOf("com.android.systemui"))
        val outcomes = Debloater(tv, log()).disable(listOf("com.android.systemui", "x;reboot"))
        assertTrue(outcomes.all { it is Outcome.Skipped && it.reason == SkipReason.PROTECTED })
        assertTrue(tv.commands.none { it.startsWith("pm disable-user") || it.startsWith("pm clear") || it.startsWith("pm uninstall") })
    }

    @Test
    fun `skips the package that owns the saved Wi-Fi network`() {
        val wizard = "com.google.android.tungsten.setupwraith"
        val tv = FakeTv(installed = setOf(wizard), wifiDump = " cuid=10027 cname=$wizard luid=10027")
        val outcomes = Debloater(tv, log()).disable(listOf(wizard))
        assertEquals(listOf(Outcome.Skipped(wizard, SkipReason.OWNS_WIFI)), outcomes)
        assertFalse(wizard in tv.disabled)
    }

    @Test
    fun `a refusal is reported and not recorded`() {
        val tv = FakeTv(installed = setOf("com.tcl.a"), refuses = setOf("com.tcl.a"))
        val log = log()
        val outcomes = Debloater(tv, log).disable(listOf("com.tcl.a"))
        assertEquals(listOf(Outcome.Skipped("com.tcl.a", SkipReason.REFUSED)), outcomes)
        assertTrue(log.all().isEmpty())
    }

    @Test
    fun `a failed check keeps the undo record`() {
        val tv = FakeTv(installed = setOf("com.tcl.a"), failedChecks = 1)
        val log = log()
        val outcomes = Debloater(tv, log).disable(listOf("com.tcl.a"))
        assertEquals(listOf(Outcome.Disabled("com.tcl.a")), outcomes)
        assertEquals(listOf("com.tcl.a"), log.all())
    }

    @Test(expected = IllegalStateException::class)
    fun `an unreadable Wi-Fi dump stops before any change`() {
        val tv = FakeTv(installed = setOf("com.tcl.a"), wifiDump = "")
        try {
            Debloater(tv, log()).disable(listOf("com.tcl.a"))
        } finally {
            assertTrue(tv.disabled.isEmpty())
        }
    }

    @Test
    fun `checks the TV after every batch of five`() {
        val packages = (1..7).map { "com.tcl.p$it" }
        val tv = FakeTv(installed = packages.toSet())
        Debloater(tv, log()).disable(packages)
        assertEquals(2, tv.commands.count { it == "pm list packages -d" })
    }

    @Test
    fun `enabling turns a package back on and drops it from the record`() {
        val tv = FakeTv(installed = setOf("com.tcl.a"))
        val log = log()
        val debloater = Debloater(tv, log)
        debloater.disable(listOf("com.tcl.a"))
        assertTrue(debloater.enable("com.tcl.a"))
        assertTrue(log.all().isEmpty())
        assertFalse("com.tcl.a" in tv.disabled)
    }
}
