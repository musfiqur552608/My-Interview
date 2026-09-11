package com.freedu.myinterviews.util

/**
 * Total-compensation math + rough built-in market bands (annual USD).
 * Bands are indicative offline hints, not live market data — the UI labels them as such.
 */
object CompCalc {

    /** First plausible money figure in free text ("$150k", "150,000", "100 RSUs"). */
    fun parseMoney(text: String): Long {
        if (text.isBlank()) return 0
        val m = Regex("""\$?\s*([\d,]+(?:\.\d+)?)\s*([kKmM])?""").find(text) ?: return 0
        val num = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return 0
        val mult = when (m.groupValues[2].lowercase()) {
            "k" -> 1_000.0
            "m" -> 1_000_000.0
            else -> 1.0
        }
        return (num * mult).toLong()
    }

    /** Annualized total: base + bonus + equity grant spread over 4 years. */
    fun annualized(base: Long, bonus: Long, equityText: String): Long =
        base + bonus + parseMoney(equityText) / 4

    fun formatShort(v: Long): String = when {
        v <= 0 -> "—"
        v >= 1_000_000 -> "$${"%.1f".format(v / 1_000_000.0)}M"
        v >= 1_000 -> "$${v / 1_000}k"
        else -> "$$v"
    }

    /** role keyword → (p25, median, p75) base salary, USD. */
    private val MARKET = listOf(
        "android" to Triple(120_000L, 155_000L, 200_000L),
        "ios" to Triple(120_000L, 155_000L, 195_000L),
        "backend" to Triple(125_000L, 160_000L, 205_000L),
        "frontend" to Triple(110_000L, 145_000L, 185_000L),
        "fullstack" to Triple(115_000L, 150_000L, 190_000L),
        "full-stack" to Triple(115_000L, 150_000L, 190_000L),
        "data" to Triple(120_000L, 155_000L, 200_000L),
        "ml" to Triple(140_000L, 180_000L, 240_000L),
        "ai" to Triple(140_000L, 180_000L, 240_000L),
        "devops" to Triple(120_000L, 155_000L, 195_000L),
        "sre" to Triple(130_000L, 165_000L, 210_000L),
        "qa" to Triple(85_000L, 110_000L, 140_000L),
        "test" to Triple(85_000L, 110_000L, 140_000L),
        "manager" to Triple(150_000L, 185_000L, 230_000L),
        "lead" to Triple(150_000L, 185_000L, 230_000L),
        "senior" to Triple(140_000L, 175_000L, 220_000L),
        "junior" to Triple(80_000L, 105_000L, 130_000L),
        "intern" to Triple(40_000L, 60_000L, 90_000L),
        "staff" to Triple(170_000L, 210_000L, 270_000L),
        "principal" to Triple(185_000L, 230_000L, 300_000L)
    )

    fun marketHint(jobTitle: String): Triple<Long, Long, Long>? {
        val t = jobTitle.lowercase()
        return MARKET.firstOrNull { (k, _) -> k in t }?.second
    }
}
