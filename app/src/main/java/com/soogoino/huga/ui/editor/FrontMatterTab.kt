package com.soogoino.huga.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.soogoino.huga.R
import com.soogoino.huga.data.model.HugoFrontMatter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Full-screen Front Matter editing tab.
 *
 * Common fields (always visible): Title, Date, Draft, Description, Tags, Categories.
 * Advanced fields (collapsible): Slug, Cover Image, Keywords.
 *
 * All fields use a uniform layout: standalone section label (titleSmall) above the input widget.
 * Every field change calls [onChanged] immediately — no "Done" button needed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrontMatterTab(
    frontMatter: HugoFrontMatter,
    onChanged: (HugoFrontMatter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title       by remember(frontMatter.title)       { mutableStateOf(frontMatter.title) }
    var draft       by remember(frontMatter.draft)       { mutableStateOf(frontMatter.draft) }
    var description by remember(frontMatter.description) { mutableStateOf(frontMatter.description) }
    var date        by remember(frontMatter.date)        { mutableStateOf(frontMatter.date) }
    var slug        by remember(frontMatter.slug)        { mutableStateOf(frontMatter.slug) }
    var coverImage  by remember(frontMatter.coverImage)  { mutableStateOf(frontMatter.coverImage) }
    val tags       = remember(frontMatter.tags)       { mutableStateListOf(*frontMatter.tags.toTypedArray()) }
    val categories = remember(frontMatter.categories) { mutableStateListOf(*frontMatter.categories.toTypedArray()) }
    val keywords   = remember(frontMatter.keywords)   { mutableStateListOf(*frontMatter.keywords.toTypedArray()) }
    var newTag      by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var newKeyword  by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun emit() {
        onChanged(
            frontMatter.copy(
                title = title,
                draft = draft,
                description = description,
                date = date,
                slug = slug,
                coverImage = coverImage,
                tags = tags.toList(),
                categories = categories.toList(),
                keywords = keywords.toList(),
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Title ──────────────────────────────────────────────────────────
        FmFieldLabel(R.string.title)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; emit() },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        // ── Date ───────────────────────────────────────────────────────────
        FmFieldLabel(R.string.date)
        OutlinedTextField(
            value = date,
            onValueChange = { date = it; emit() },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.date_iso_placeholder)) },
            trailingIcon = {
                TextButton(onClick = {
                    date = Instant.now()
                        .atOffset(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    emit()
                }) { Text(stringResource(R.string.now)) }
            },
        )

        // ── Draft toggle ───────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.draft), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.draft_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = draft, onCheckedChange = { draft = it; emit() })
        }

        // ── Description ────────────────────────────────────────────────────
        FmFieldLabel(R.string.description)
        OutlinedTextField(
            value = description,
            onValueChange = { description = it; emit() },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        // ── Tags ───────────────────────────────────────────────────────────
        FmFieldLabel(R.string.tags)
        FmChipInput(
            value = newTag,
            onValueChange = { newTag = it },
            onAdd = { if (newTag.isNotBlank()) { tags.add(newTag.trim()); newTag = ""; emit() } },
            onDone = {
                if (newTag.isNotBlank()) { tags.add(newTag.trim()); newTag = ""; emit() }
                focusManager.clearFocus()
            },
            addContentDescription = stringResource(R.string.add_tag),
        )
        FmChipRow(items = tags.toList(), onRemove = { tags.remove(it); emit() })

        // ── Categories ─────────────────────────────────────────────────────
        FmFieldLabel(R.string.categories)
        FmChipInput(
            value = newCategory,
            onValueChange = { newCategory = it },
            onAdd = { if (newCategory.isNotBlank()) { categories.add(newCategory.trim()); newCategory = ""; emit() } },
            onDone = {
                if (newCategory.isNotBlank()) { categories.add(newCategory.trim()); newCategory = ""; emit() }
                focusManager.clearFocus()
            },
            addContentDescription = stringResource(R.string.add_category),
        )
        FmChipRow(items = categories.toList(), onRemove = { categories.remove(it); emit() })

        // ── Advanced section (collapsible) ─────────────────────────────────
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvanced = !showAdvanced }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.advanced), style = MaterialTheme.typography.titleSmall)
            Icon(
                imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (showAdvanced) stringResource(R.string.collapse) else stringResource(R.string.expand),
            )
        }

        AnimatedVisibility(visible = showAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Slug
                FmFieldLabel(R.string.slug_optional)
                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it; emit() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                // Cover image
                FmFieldLabel(R.string.cover_image_path)
                OutlinedTextField(
                    value = coverImage,
                    onValueChange = { coverImage = it; emit() },
                    placeholder = { Text(stringResource(R.string.cover_image_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                Text(
                    stringResource(R.string.cover_image_supporting),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.offset(y = (-8).dp),
                )

                // Keywords
                FmFieldLabel(R.string.keywords)
                FmChipInput(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    onAdd = { if (newKeyword.isNotBlank()) { keywords.add(newKeyword.trim()); newKeyword = ""; emit() } },
                    onDone = {
                        if (newKeyword.isNotBlank()) { keywords.add(newKeyword.trim()); newKeyword = ""; emit() }
                        focusManager.clearFocus()
                    },
                    addContentDescription = stringResource(R.string.add_keyword),
                )
                FmChipRow(items = keywords.toList(), onRemove = { keywords.remove(it); emit() })
            }
        }
    }
}

// ─── Shared primitives ────────────────────────────────────────────────────────

@Composable
private fun FmFieldLabel(labelRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun FmChipInput(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDone: () -> Unit,
    addContentDescription: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = addContentDescription)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FmChipRow(
    items: List<String>,
    onRemove: (String) -> Unit,
) {
    if (items.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-8).dp),
    ) {
        items.forEach { item ->
            InputChip(
                selected = false,
                onClick = {},
                label = { Text(item) },
                trailingIcon = {
                    IconButton(
                        onClick = { onRemove(item) },
                        modifier = Modifier.size(18.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = item,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                },
            )
        }
    }
}
