package com.soogoino.huga.ui.util

/**
 * CJK-aware word / character count.
 *
 * Rules:
 *  - Each CJK character (Han, Hiragana, Katakana, Hangul, etc.) counts as 1 unit.
 *  - Remaining non-CJK text is split on whitespace; each non-blank token counts as 1 unit.
 *
 * Estimated reading time assumes:
 *  - CJK readers: ~500 characters / min
 *  - English / other: ~200 words / min
 *  Mixed content uses a weighted average.
 */

private val CJK_REGEX = Regex(
    "[\u4E00-\u9FFF"   +   // CJK Unified Ideographs
    "\u3400-\u4DBF"    +   // CJK Extension A
    "\uF900-\uFAFF"    +   // CJK Compatibility Ideographs
    "\u3040-\u309F"    +   // Hiragana
    "\u30A0-\u30FF"    +   // Katakana
    "\uAC00-\uD7AF"    +   // Hangul Syllables
    "\u1100-\u11FF"    +   // Hangul Jamo
    "\u3130-\u318F"    +   // Hangul Compatibility Jamo
    "]"
)

/**
 * Returns total word/character count with CJK awareness.
 */
fun countWords(text: String): Int {
    val cjkCount = CJK_REGEX.findAll(text).count()
    val nonCjkText = CJK_REGEX.replace(text, " ")
    val latinCount = nonCjkText.split(Regex("\\s+")).count { it.isNotBlank() }
    return cjkCount + latinCount
}

/**
 * Returns estimated reading time in minutes (minimum 1).
 * CJK: 500 chars/min, Latin: 200 words/min.
 */
fun estimatedMinRead(text: String): Int {
    val cjkCount = CJK_REGEX.findAll(text).count()
    val nonCjkText = CJK_REGEX.replace(text, " ")
    val latinCount = nonCjkText.split(Regex("\\s+")).count { it.isNotBlank() }
    if (cjkCount == 0 && latinCount == 0) return 1
    val minutes = cjkCount / 500.0 + latinCount / 200.0
    return minutes.coerceAtLeast(1.0).toInt()
}
