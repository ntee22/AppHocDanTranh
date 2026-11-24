package vn.hocdantranh.guzheng.tuner

/**
 * One string of a 21-string guzheng tuned to D. Port of iOS `GString`.
 *
 * @property isGreen the A strings, coloured green on a real guzheng as position markers.
 */
data class GuzhengString(
    val id: Int,
    val note: String,
    val label: String,
    val frequency: Double,
    val isGreen: Boolean,
)

/**
 * Hz table and labels copied verbatim from iOS `GUZHENG_STRINGS` — the two apps must agree
 * on every value. String 1 is the highest. U+0332 (combining low line) underlines the
 * lower-octave labels, as in Chinese cipher notation.
 */
val GUZHENG_STRINGS: List<GuzhengString> = listOf(
    GuzhengString(1, "D", "D3", 1172.5, false),
    GuzhengString(2, "B", "B2", 985.5, false),
    GuzhengString(3, "A", "A2", 889.2, true),
    GuzhengString(4, "F♯", "F♯2", 737.6, false),
    GuzhengString(5, "E", "E2", 662.8, false),
    GuzhengString(6, "D", "D2", 585.4, false),
    GuzhengString(7, "B", "B1", 489.5, false),
    GuzhengString(8, "A", "A1", 437.9, true),
    GuzhengString(9, "F♯", "F♯1", 368.1, false),
    GuzhengString(10, "E", "E1", 330.4, false),
    GuzhengString(11, "D", "D", 293.8, false),
    GuzhengString(12, "B", "B", 246.2, false),
    GuzhengString(13, "A", "A", 218.4, true),
    GuzhengString(14, "F♯", "F♯", 184.7, false),
    GuzhengString(15, "E", "E", 164.4, false),
    GuzhengString(16, "D", "D̲", 146.2, false),
    GuzhengString(17, "B", "B̲", 123.0, false),
    GuzhengString(18, "A", "A̲", 109.5, true),
    GuzhengString(19, "F♯", "F̲♯", 92.3, false),
    GuzhengString(20, "E", "E̲", 81.9, false),
    GuzhengString(21, "D", "D̲̲", 73.1, false),
)

fun guzhengString(id: Int): GuzhengString = GUZHENG_STRINGS.first { it.id == id }
