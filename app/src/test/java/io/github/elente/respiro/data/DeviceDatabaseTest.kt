package io.github.elente.respiro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every file in the device database, checked the way a pull request adding a TV must pass. */
class DeviceDatabaseTest {
    private val root = File("src/main/assets/devices")
    private val jsonFiles = root.walk().filter { it.extension == "json" }.toList()
    private val database = DeviceDatabase.parse(jsonFiles.map { it.readText() })

    @Test
    fun `every file parses and its id matches its path`() {
        assertTrue("no database files found in $root", jsonFiles.isNotEmpty())
        jsonFiles.forEach { file ->
            val path = file.relativeTo(root).path.removeSuffix(".json")
            val id = database.files.single { candidate -> path == candidate.id || path == "models/" + candidate.id }.id
            assertNotNull("$path has no file with a matching id", id)
        }
    }

    @Test
    fun `every extends points to an existing file`() {
        val ids = database.files.map { it.id }.toSet()
        database.files.forEach { file ->
            file.extends.forEach { assertTrue("${file.id} extends missing $it", it in ids) }
        }
    }

    @Test
    fun `no file disables or asks about a protected core package`() {
        database.files.forEach { file ->
            (file.disable.map { it.pkg } + file.ask.map { it.pkg }).forEach {
                assertFalse("${file.id} lists protected $it", ProtectedPackages.isProtected(it))
            }
        }
    }

    @Test
    fun `no file names a package twice`() {
        database.files.forEach { file ->
            val repeated = file.packages.groupBy { it }.filterValues { it.size > 1 }.keys
            assertTrue("${file.id} repeats $repeated", repeated.isEmpty())
        }
    }

    @Test
    fun `every model says how it was tested`() {
        database.files.filter { it.isModel }.forEach { model ->
            assertNotNull("${model.id} has no tested block", model.tested)
        }
    }

    @Test
    fun `matches the TCL and the Flow box, and nothing for an unknown TV`() {
        val tcl = mapOf("ro.product.manufacturer" to "TCL", "ro.product.device" to "C06", "ro.product.name" to "C06_2K_GB")
        val flow = mapOf(
            "ro.product.manufacturer" to "Sagemcom",
            "ro.product.model" to "FlowSoundBox",
            "ro.product.device" to "m393gena_teco",
        )
        assertEquals("tcl/c06-2k-gb", database.match(tcl)?.id)
        assertEquals("sagemcom/flow-soundbox", database.match(flow)?.id)
        assertNull(database.match(mapOf("ro.product.manufacturer" to "Acme")))
    }

    @Test
    fun `the plan merges layers and never disables what a layer keeps`() {
        val tcl = database.files.single { it.id == "tcl/c06-2k-gb" }
        val plan = database.plan(tcl)
        val disabled = plan.disable.map { it.pkg }
        assertTrue("com.tcl.dashboard" in disabled)
        assertTrue("com.google.android.feedback" in disabled)
        assertTrue(disabled.none { it in plan.keep })
        assertTrue("com.tcl.tv" in plan.keep)
    }

    @Test
    fun `an unknown TV gets only the common layer`() {
        val plan = database.plan(null)
        assertEquals(setOf("com.google.android.feedback", "com.google.android.tvrecommendations"), plan.disable.map { it.pkg }.toSet())
    }
}
