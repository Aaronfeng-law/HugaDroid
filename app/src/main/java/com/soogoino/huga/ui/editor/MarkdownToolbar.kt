package com.soogoino.huga.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.soogoino.huga.R

// ─── Action Model ─────────────────────────────────────────────────────────────

sealed class MarkdownAction {
    object Bold : MarkdownAction()
    object Italic : MarkdownAction()
    object Strikethrough : MarkdownAction()
    object InlineCode : MarkdownAction()
    object Heading : MarkdownAction()
    object BulletList : MarkdownAction()
    object NumberList : MarkdownAction()
    object BlockQuote : MarkdownAction()
    object Link : MarkdownAction()
    object HorizontalRule : MarkdownAction()
}

// ─── Pure transformation function ────────────────────────────────────────────

/**
 * Applies a [MarkdownAction] to [tfv] and returns the updated [TextFieldValue]
 * with correct cursor/selection. This is a pure function with no side effects.
 */
fun applyMarkdownAction(action: MarkdownAction, tfv: TextFieldValue): TextFieldValue =
    when (action) {
        is MarkdownAction.Bold          -> wrapSelection(tfv, "**", "**", "bold")
        is MarkdownAction.Italic        -> wrapSelection(tfv, "*", "*", "italic")
        is MarkdownAction.Strikethrough -> wrapSelection(tfv, "~~", "~~", "text")
        is MarkdownAction.InlineCode    -> wrapSelection(tfv, "`", "`", "code")
        is MarkdownAction.Heading       -> cycleHeading(tfv)
        is MarkdownAction.BulletList    -> toggleLinePrefix(tfv, "- ")
        is MarkdownAction.NumberList    -> toggleLinePrefix(tfv, "1. ")
        is MarkdownAction.BlockQuote    -> toggleLinePrefix(tfv, "> ")
        is MarkdownAction.Link          -> applyLink(tfv)
        is MarkdownAction.HorizontalRule -> applyHorizontalRule(tfv)
    }

// ─── Transform helpers ────────────────────────────────────────────────────────

/**
 * Wraps the selected text with [prefix] + [suffix].
 * - If text is selected: wraps it. If already wrapped, toggles off.
 * - If no selection: inserts [prefix]+[placeholder]+[suffix] and selects the placeholder.
 */
private fun wrapSelection(
    tfv: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String,
): TextFieldValue {
    val text = tfv.text
    val selStart = tfv.selection.min
    val selEnd = tfv.selection.max
    val selected = text.substring(selStart, selEnd)

    return if (selected.isNotEmpty()) {
        // Toggle: remove wrap if already applied
        if (selected.startsWith(prefix) && selected.endsWith(suffix) &&
            selected.length > prefix.length + suffix.length
        ) {
            val inner = selected.removePrefix(prefix).removeSuffix(suffix)
            val newText = text.substring(0, selStart) + inner + text.substring(selEnd)
            TextFieldValue(newText, TextRange(selStart + inner.length))
        } else {
            val newText = text.substring(0, selStart) + prefix + selected + suffix + text.substring(selEnd)
            TextFieldValue(newText, TextRange(selStart + prefix.length + selected.length + suffix.length))
        }
    } else {
        // Insert placeholder and select it so user can immediately type
        val insert = prefix + placeholder + suffix
        val newText = text.substring(0, selStart) + insert + text.substring(selStart)
        val selectStart = selStart + prefix.length
        val selectEnd = selectStart + placeholder.length
        TextFieldValue(newText, TextRange(selectStart, selectEnd))
    }
}

/**
 * Cycles heading level on the current line: none → H1 → H2 → H3 → none.
 */
private fun cycleHeading(tfv: TextFieldValue): TextFieldValue {
    val text = tfv.text
    val cursor = tfv.selection.start
    val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
    val line = text.substring(lineStart, lineEnd)

    val (newLine, cursorDelta) = when {
        line.startsWith("### ") -> Pair(line.removePrefix("### "), -4)
        line.startsWith("## ")  -> Pair("### " + line.removePrefix("## "), 1)
        line.startsWith("# ")   -> Pair("## " + line.removePrefix("# "), 1)
        else                    -> Pair("# $line", 2)
    }

    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val newCursor = (cursor + cursorDelta).coerceIn(lineStart, lineStart + newLine.length)
    return TextFieldValue(newText, TextRange(newCursor))
}

/**
 * Toggles a [prefix] (e.g. "- ", "> ") at the start of the current line.
 */
private fun toggleLinePrefix(tfv: TextFieldValue, prefix: String): TextFieldValue {
    val text = tfv.text
    val cursor = tfv.selection.start
    val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
    val line = text.substring(lineStart, lineEnd)

    val newLine = if (line.startsWith(prefix)) line.removePrefix(prefix) else prefix + line
    val delta = newLine.length - line.length
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val newCursor = (cursor + delta).coerceAtLeast(lineStart)
    return TextFieldValue(newText, TextRange(newCursor))
}

/**
 * Inserts a markdown link.
 * - With selection: `[selected text](url)` — selects "url" for immediate typing.
 * - Without selection: `[](url)` — cursor lands inside `[]`.
 */
private fun applyLink(tfv: TextFieldValue): TextFieldValue {
    val text = tfv.text
    val selStart = tfv.selection.min
    val selEnd = tfv.selection.max
    val selected = text.substring(selStart, selEnd)

    return if (selected.isNotEmpty()) {
        val insert = "[$selected](url)"
        val newText = text.substring(0, selStart) + insert + text.substring(selEnd)
        val urlStart = selStart + 1 + selected.length + 2  // after "[$selected]("
        TextFieldValue(newText, TextRange(urlStart, urlStart + 3))
    } else {
        val insert = "[](url)"
        val newText = text.substring(0, selStart) + insert + text.substring(selStart)
        TextFieldValue(newText, TextRange(selStart + 1))  // cursor inside []
    }
}

/**
 * Inserts a horizontal rule `---` on a new block line.
 */
private fun applyHorizontalRule(tfv: TextFieldValue): TextFieldValue {
    val text = tfv.text
    val cursor = tfv.selection.start
    val insert = "\n\n---\n\n"
    val newText = text.substring(0, cursor) + insert + text.substring(cursor)
    return TextFieldValue(newText, TextRange(cursor + insert.length))
}

// ─── Toolbar UI ───────────────────────────────────────────────────────────────

private data class ToolbarItem(
    val action: MarkdownAction,
    val icon: ImageVector,
    val labelRes: Int,
)

private val toolbarItems = listOf(
    ToolbarItem(MarkdownAction.Bold,          Icons.Filled.FormatBold,          R.string.fmt_bold),
    ToolbarItem(MarkdownAction.Italic,        Icons.Filled.FormatItalic,        R.string.fmt_italic),
    ToolbarItem(MarkdownAction.Strikethrough, Icons.Filled.FormatStrikethrough, R.string.fmt_strikethrough),
    ToolbarItem(MarkdownAction.InlineCode,    Icons.Filled.Code,                R.string.fmt_inline_code),
    ToolbarItem(MarkdownAction.Heading,       Icons.Filled.Title,               R.string.fmt_heading),
    ToolbarItem(MarkdownAction.BulletList,    Icons.Filled.FormatListBulleted,     R.string.fmt_bullet_list),
    ToolbarItem(MarkdownAction.NumberList,    Icons.Filled.FormatListNumbered,  R.string.fmt_number_list),
    ToolbarItem(MarkdownAction.BlockQuote,    Icons.Filled.FormatQuote,         R.string.fmt_block_quote),
    ToolbarItem(MarkdownAction.Link,          Icons.Filled.Link,                R.string.fmt_link),
    ToolbarItem(MarkdownAction.HorizontalRule, Icons.Filled.HorizontalRule,     R.string.fmt_hr),
)

@Composable
fun MarkdownToolbar(
    onAction: (MarkdownAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(toolbarItems) { item ->
                IconButton(
                    onClick = { onAction(item.action) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelRes),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
