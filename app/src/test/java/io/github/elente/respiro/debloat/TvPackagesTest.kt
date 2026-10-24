package io.github.elente.respiro.debloat

import io.github.elente.respiro.data.AskEntry
import io.github.elente.respiro.data.DebloatPlan
import io.github.elente.respiro.data.DisableEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TvPackagesTest {
    @Test
    fun `the plan keeps only what is installed and still on`() {
        val tv = TvPackages.read(FakeTv(installed = setOf("com.a", "com.b", "com.c"), disabled = setOf("com.b")))
        val plan = DebloatPlan(
            model = null,
            disable = listOf(DisableEntry("com.a", "ads", ""), DisableEntry("com.b", "ads", ""), DisableEntry("com.gone", "ads", "")),
            ask = listOf(AskEntry("com.c", "streaming", "C"), AskEntry("com.gone2", "streaming", "")),
            keep = emptySet(),
            neverStart = emptySet(),
        ).forTv(tv)
        assertEquals(listOf("com.a"), plan.disable.map { it.pkg })
        assertEquals(listOf("com.c"), plan.ask.map { it.pkg })
    }
}
