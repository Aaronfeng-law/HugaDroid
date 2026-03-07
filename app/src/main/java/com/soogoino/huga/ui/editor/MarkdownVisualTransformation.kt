package com.soogoino.huga.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Lightweight Markdown syntax highlighting applied as a [VisualTransformation].
 * Highlights: headings, bold, italic, inline code, fenced code blocks, front matter, links.
 * Does NOT change the length of the text – uses identity offset mapping.
 */
class MarkdownVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = buildAnnotatedString {
            append(raw)
            // Apply span styles via regex
            RULES.forEach { (pattern, style) ->
                pattern.findAll(raw).forEach { match ->
                    addStyle(style, match.range.first, match.range.last + 1)
                }
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }

    companion object {
        // Colors – dark-mode friendly, but will be visible on light too
        private val colorHeading = Color(0xFF79C0FF)
        private val colorBold = Color(0xFFFFE57F)
        private val colorItalic = Color(0xFFCEA5FB)
        private val colorCode = Color(0xFF89DDFF)
        private val colorCodeBlock = Color(0xFF8B949E)
        private val colorFrontMatter = Color(0xFF56D364)
        private val colorLink = Color(0xFF58A6FF)
        private val colorComment = Color(0xFF8B949E)

        private val RULES: List<Pair<Regex, SpanStyle>> = listOf(
            // Front matter delimiter lines (--- or +++)
            Regex("""^(---|\+\+\+)\s*$""", setOf(RegexOption.MULTILINE)) to
                    SpanStyle(color = colorFrontMatter, fontWeight = FontWeight.Bold),

            // ATX Headings # through ######
            Regex("""^#{1,6} .+$""", setOf(RegexOption.MULTILINE)) to
                    SpanStyle(color = colorHeading, fontWeight = FontWeight.Bold),

            // Fenced code blocks ```
            Regex("""```[\s\S]*?```""") to SpanStyle(color = colorCodeBlock),

            // Inline code `...`
            Regex("""`[^`\n]+`""") to SpanStyle(color = colorCode, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),

            // Bold **text** or __text__
            Regex("""\*\*[^*\n]+\*\*|__[^_\n]+__""") to SpanStyle(fontWeight = FontWeight.Bold, color = colorBold),

            // Italic *text* or _text_
            Regex("""(?<!\*)\*(?!\*)[^*\n]+\*(?!\*)|(?<!_)_(?!_)[^_\n]+_(?!_)""") to
                    SpanStyle(fontStyle = FontStyle.Italic, color = colorItalic),

            // Links [text](url)
            Regex("""\[[^\]]+\]\([^)]+\)""") to SpanStyle(color = colorLink),

            // Image links ![alt](url)
            Regex("""!\[[^\]]*\]\([^)]+\)""") to SpanStyle(color = colorLink, fontStyle = FontStyle.Italic),

            // Hugo shortcodes {{< ... >}}
            Regex("""\{\{<.*?>\}\}""") to SpanStyle(color = colorFrontMatter),

            // Blockquote >
            Regex("""^> .+$""", setOf(RegexOption.MULTILINE)) to SpanStyle(color = colorComment),
        )
    }
}
