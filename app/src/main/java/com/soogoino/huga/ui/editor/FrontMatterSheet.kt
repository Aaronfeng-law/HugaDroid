package com.soogoino.huga.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.soogoino.huga.data.model.HugoFrontMatter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FrontMatterSheet(
    frontMatter: HugoFrontMatter,
    onDismiss: () -> Unit,
    onConfirm: (HugoFrontMatter) -> Unit,
) {
    var title by remember { mutableStateOf(frontMatter.title) }
    var date by remember { mutableStateOf(frontMatter.date) }
    var draft by remember { mutableStateOf(frontMatter.draft) }
    var description by remember { mutableStateOf(frontMatter.description) }
    var slug by remember { mutableStateOf(frontMatter.slug) }
    var coverImage by remember { mutableStateOf(frontMatter.coverImage) }
    val tags = remember { mutableStateListOf(*frontMatter.tags.toTypedArray()) }
    val categories = remember { mutableStateListOf(*frontMatter.categories.toTypedArray()) }
    var newTag by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Front Matter", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    onConfirm(
                        HugoFrontMatter(
                            title = title,
                            date = date,
                            draft = draft,
                            description = description,
                            slug = slug,
                            tags = tags.toList(),
                            categories = categories.toList(),
                            coverImage = coverImage,
                            extra = frontMatter.extra,
                            weight = frontMatter.weight,
                        )
                    )
                }) { Text("Done") }
            }

            HorizontalDivider()

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            // Date
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (ISO-8601)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. 2026-03-06T12:00:00+08:00") },
                trailingIcon = {
                    TextButton(onClick = {
                        date = Instant.now().atOffset(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    }) { Text("Now") }
                },
            )

            // Slug
            OutlinedTextField(
                value = slug,
                onValueChange = { slug = it },
                label = { Text("Slug (optional override)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            // Draft toggle
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Draft", style = MaterialTheme.typography.titleSmall)
                    Text("Draft posts are not published", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = draft, onCheckedChange = { draft = it })
            }

            // Tags
            Text("Tags", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("Add tag") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newTag.isNotBlank()) { tags.add(newTag.trim()); newTag = "" }
                        focusManager.clearFocus()
                    }),
                )
                IconButton(onClick = {
                    if (newTag.isNotBlank()) { tags.add(newTag.trim()); newTag = "" }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add tag")
                }
            }
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(tag) },
                            trailingIcon = {
                                IconButton(onClick = { tags.remove(tag) }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            },
                        )
                    }
                }
            }

            // Categories
            Text("Categories", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    label = { Text("Add category") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newCategory.isNotBlank()) { categories.add(newCategory.trim()); newCategory = "" }
                        focusManager.clearFocus()
                    }),
                )
                IconButton(onClick = {
                    if (newCategory.isNotBlank()) { categories.add(newCategory.trim()); newCategory = "" }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add category")
                }
            }
            if (categories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(cat) },
                            trailingIcon = {
                                IconButton(onClick = { categories.remove(cat) }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            },
                        )
                    }
                }
            }

            // Cover image
            HorizontalDivider()
            Text("Cover Image", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = coverImage,
                onValueChange = { coverImage = it },
                label = { Text("Path") },
                placeholder = { Text("e.g. cover.jpg or /images/cover.jpg") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                supportingText = { Text("Relative to bundle dir (PAGE_BUNDLE) or absolute from site root", style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}
