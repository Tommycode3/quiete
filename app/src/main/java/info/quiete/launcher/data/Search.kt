package info.quiete.launcher.data

/**
 * Ranked fuzzy matching over app labels.
 *
 * The ranking is what makes search feel instant rather than merely fast: typing
 * "tel" must put *Telegram* above *Hotel Booking*, and "wa" must put *WhatsApp*
 * above *Wallet* only because of usage — so prefix beats word-start beats
 * scattered-subsequence, and ties break on the shorter label.
 */
object Search {

    private const val PREFIX = 1_000_000
    private const val WORD_START = 100_000
    private const val SUBSEQUENCE = 1_000

    fun rank(apps: List<AppEntry>, rawQuery: String): List<AppEntry> {
        val query = rawQuery.trim().uppercase()
        if (query.isEmpty()) return apps

        return apps
            .mapNotNull { app -> score(app.sortKey, query)?.let { app to it } }
            .sortedWith(
                compareByDescending<Pair<AppEntry, Int>> { it.second }
                    .thenBy { it.first.label.length }
                    .thenBy { it.first.sortKey },
            )
            .map { it.first }
    }

    /** Higher is better. `null` means no match at all. */
    private fun score(label: String, query: String): Int? {
        if (label.startsWith(query)) return PREFIX + (100 - label.length).coerceAtLeast(0)

        // Match at the start of any word: "app store" matched by "st".
        var word = 0
        while (word < label.length) {
            if (label.startsWith(query, word)) return WORD_START + (100 - word).coerceAtLeast(0)
            val next = label.indexOf(' ', word)
            if (next < 0) break
            word = next + 1
        }

        // Scattered subsequence: "wsp" matches "WhatSaPp". Reward adjacency so
        // tightly-clustered matches outrank characters found across the label.
        var qi = 0
        var streak = 0
        var points = 0
        var previous = -2
        for ((li, ch) in label.withIndex()) {
            if (qi < query.length && ch == query[qi]) {
                streak = if (li == previous + 1) streak + 1 else 0
                points += 1 + streak * 2
                previous = li
                qi++
            }
        }
        return if (qi == query.length) SUBSEQUENCE + points else null
    }
}
