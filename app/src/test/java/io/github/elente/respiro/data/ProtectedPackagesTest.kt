package io.github.elente.respiro.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedPackagesTest {
    @Test
    fun `core packages are protected`() {
        listOf(
            "android",
            "com.android.systemui",
            "com.android.tv.settings",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.overlay.modules.ext.services",
            "com.android.providers.settings",
        ).forEach { assertTrue(it, ProtectedPackages.isProtected(it)) }
    }

    @Test
    fun `anything that isn't a clean package name is protected`() {
        listOf("com.android.systemui ", "com.android.systemui;reboot", "", "nodots", "com.tcl.dashboard\nreboot")
            .forEach { assertTrue("'$it'", ProtectedPackages.isProtected(it)) }
    }

    @Test
    fun `optional providers and ordinary apps are not`() {
        listOf(
            "com.android.providers.calendar",
            "com.android.providers.contacts",
            "com.google.android.feedback",
            "com.tcl.dashboard",
        ).forEach { assertFalse(it, ProtectedPackages.isProtected(it)) }
    }
}
