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
 * Common fields (always visible): Title, Draft, Tags, Categories, Description.
 * Advanced fields (collapsible): Date, Slug, Cover Image.
 *
 * Every field change calls [onChanged] immediately — no "Done" button needed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrontMatterTab(
    frontMatter: HugoFrontMatter,
    onChanged: (HugoFrontMatter) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local mutable state mirrors the incoming frontMatter so changes are instant in the UI
    var title by remember(frontMatter.title) { mutableStateOf(frontMatter.title) }
    var draft by remember(frontMatter.draft) { mutableStateOf(frontMatter.draft) }
    var description by remember(frontMatter.description) { mutableStateOf(frontMatter.description) }
    var date by remember(frontMatter.date) { mutableStateOf(frontMatter.date) }
    var slug by remember(frontMatter.slug) { mutableStateOf(frontMatter.slug) }
    var coverImage by remember(frontMatter.coverImage) { mutableStateOf(frontMatter.coverImage) }
    val tags = remember(frontMatter.tags) { mutableStateListOf(*frontMatter.tags.toTypedArray()) }
    val categories = remember(frontMatter.categories) { mutableStateListOf(*frontMatter.categories.toTypedArray()) }
    var newTag by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Helper to emit the current state after any change
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
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; emit() },
            label = { Text(stringResource(R.string.title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
            Switch(
                checked = draft,
                onCheckedChange = { draft = it; emit() },
            )
        }

        // ── Description ────────────────────────────────────────────────────
        OutlinedTextField(
            value = description,
            onValueChange = { description = it; emit() },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        // ── Tags ───────────────────────────────────────────────────────────
        Text(stringResource(R.string.tags), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            tags.toList().forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(tag) },
                    trailingIcon = {
                        IconButton(
                            onClick = { tags.remove(tag); emit() },
                            modifier = Modifier.size(18.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), modifier = Modifier.size(14.dp))
                        }
                    },
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text(stringResource(R.string.add_tag)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newTag.isNotBlank()) { tags.add(newTag.trim()); newTag = ""; emit() }
                    focusManager.clearFocus()
                }),
            )
            IconButton(onClick = {
                if (newTag.isNotBlank()) { tags.add(newTag.trim()); newTag = ""; emit() }
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_tag))
            }
        }

        // ── Categories ─────────────────────────────────────────────────────
        Text(stringResource(R.string.categories), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            categories.toList().forEach { cat ->
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(cat) },
                    trailingIcon = {
                        IconButton(
                            onClick = { categories.remove(cat); emit() },
                            modifier = Modifier.size(18.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), modifier = Modifier.size(14.dp))
                        }
                    },
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newCategory,
                onValueChange = { newCategory = it },
                label = { Text(stringResource(R.string.add_category)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newCategory.isNotBlank()) { categories.add(newCategory.trim()); newCategory = ""; emit() }
                    focusManager.clearFocus()
                }),
            )
            IconButton(onClick = {
                if (newCategory.isNotBlank()) { categories.add(newCategory.trim()); newCategory = ""; emit() }
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_category))
            }
        }

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

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it; emit() },
                    label = { Text(stringResource(R.string.date_iso_label)) },
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

                // Slug
                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it; emit() },
                    label = { Text(stringResource(R.string.slug_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                // Cover image
                OutlinedTextField(
                    value = coverImage,
                    onValueChange = { coverImage = it; emit() },
                    label = { Text(stringResource(R.string.cover_image_path)) },
                    placeholder = { Text(stringResource(R.string.cover_image_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    supportingText = {
                        Text(
                            stringResource(R.string.cover_image_supporting),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}
