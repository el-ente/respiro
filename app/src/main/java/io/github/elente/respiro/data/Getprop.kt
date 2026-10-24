package io.github.elente.respiro.data

/** Parses `getprop` output, one "[key]: [value]" per line, into a map. */
object Getprop {
    private val line = Regex("""^\[(.+?)]: \[(.*)]$""")

    fun parse(output: String): Map<String, String> = output.lineSequence()
        .mapNotNull { line.matchEntire(it.trim())?.destructured }
        .associate { (key, value) -> key to value }
}
