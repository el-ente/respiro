package io.github.elente.respiro.debloat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UninstallerTest {
    private val self = "io.github.elente.respiro"
    private val tv = FakeTv(
        installed = setOf("com.stremio.one", "com.tcl.tv", self, "io.github.elente.daiji"),
        preinstalled = setOf("com.tcl.tv"),
    )

    @Test
    fun `offers only apps the user installed, minus Respiro and the home screen`() {
        assertEquals(setOf("com.stremio.one"), Uninstaller(tv, self).removable())
    }

    @Test
    fun `never offers the keyboard or an accessibility service in use`() {
        val tv = FakeTv(
            installed = setOf("com.stremio.one", "com.keys.helper", "com.board.ime"),
            preinstalled = emptySet(),
            secureSettings = mapOf(
                "enabled_accessibility_services" to "com.urbandroid.lux/.Twilight:com.keys.helper/.KeyService",
                "default_input_method" to "com.board.ime/.Ime",
            ),
        )
        assertEquals(setOf("com.stremio.one"), Uninstaller(tv, self).removable())
    }

    @Test
    fun `offers nothing when the home screens can't be read`() {
        val tv = io.github.elente.respiro.adb.Shell { command -> if (command == "pm list packages -3") "package:com.stremio.one" else "" }
        assertTrue(Uninstaller(tv, self).removable().isEmpty())
    }

    @Test
    fun `uninstalls a user app`() {
        assertTrue(Uninstaller(tv, self).uninstall("com.stremio.one"))
        assertTrue(tv.commands.contains("pm uninstall com.stremio.one"))
    }

    @Test
    fun `refuses preinstalled apps, Respiro, Home and malformed names`() {
        val uninstaller = Uninstaller(tv, self)
        listOf("com.tcl.tv", self, "io.github.elente.daiji", "x;reboot").forEach { assertFalse(it, uninstaller.uninstall(it)) }
        assertTrue(tv.commands.none { it.startsWith("pm uninstall ") })
    }
}
