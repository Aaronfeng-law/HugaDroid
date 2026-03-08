package com.soogoino.huga.ui.sync

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soogoino.huga.R
import com.soogoino.huga.git.CommitEntry
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCommitDialog by remember { mutableStateOf(false) }
    var showCancelSyncDialog by remember { mutableStateOf(false) }

    // Intercept back press while pull/push is running
    BackHandler(enabled = uiState.isSyncing) { showCancelSyncDialog = true }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SyncEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.git_sync)) },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.isSyncing) showCancelSyncDialog = true else onNavigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!uiState.isRepoSetup) {
                        TextButton(onClick = onNavigateToSetup) { Text(stringResource(R.string.set_up)) }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Progress bar
            item {
                AnimatedVisibility(uiState.isSyncing) {
                    Column {
                        LinearProgressIndicator(
                            progress = { if (uiState.syncProgress > 0) uiState.syncProgress / 100f else 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (uiState.syncTask.isNotBlank()) {
                            Text(uiState.syncTask, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // Action buttons
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.actions), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = viewModel::pull,
                                enabled = !uiState.isSyncing && uiState.isRepoSetup,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.pull))
                            }
                            Button(
                                onClick = { showCommitDialog = true },
                                enabled = !uiState.isSyncing && uiState.isRepoSetup && uiState.pendingChanges.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Upload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.commit_and_push))
                            }
                        }
                        // Status-loading indicator: shown while git status is scanned in background.
                        // Explains why the Commit & Push button is gray before the check completes.
                        AnimatedVisibility(uiState.isLoadingStatus && !uiState.isSyncing) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    stringResource(R.string.checking_status),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // "No local changes" — only shown once status check is done and list is empty
            if (!uiState.isLoadingStatus && !uiState.isSyncing &&
                uiState.pendingChanges.isEmpty() && uiState.isRepoSetup
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.no_local_changes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            // Pending changes
            if (uiState.pendingChanges.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ChangeHistory, null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.pending_changes, uiState.pendingChanges.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(Modifier.height(8.dp))
                            uiState.pendingChanges.take(10).forEach { change ->
                                Text(change, style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (uiState.pendingChanges.size > 10) {
                                Text(stringResource(R.string.and_more, uiState.pendingChanges.size - 10),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            // Ahead count
            if (uiState.aheadCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ArrowUpward, null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.commits_ahead, uiState.aheadCount),
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }

            // Commit log
            if (uiState.commitLog.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.recent_commits), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp))
                }
                items(uiState.commitLog, key = { it.hash }) { commit ->
                    CommitRow(commit)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showCommitDialog) {
        CommitMessageDialog(
            onDismiss = { showCommitDialog = false },
            onConfirm = { msg ->
                showCommitDialog = false
                viewModel.commitAndPush(msg)
            }
        )
    }

    // ── Cancel-sync confirmation dialog ────────────────────────────────────────
    if (showCancelSyncDialog) {
        AlertDialog(
            onDismissRequest = { showCancelSyncDialog = false },
            title = { Text(stringResource(R.string.sync_in_progress_title)) },
            text = { Text(stringResource(R.string.sync_in_progress_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelSyncDialog = false
                        viewModel.cancelSync()
                        onNavigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.cancel_and_go_back)) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelSyncDialog = false }) {
                    Text(stringResource(R.string.keep_waiting))
                }
            },
        )
    }
}

@Composable
private fun CommitRow(commit: CommitEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                commit.shortHash,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(commit.message, style = MaterialTheme.typography.bodyMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Text(
                "${commit.authorName} · ${formatEpoch(commit.time)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun CommitMessageDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    val autoCommitText = stringResource(R.string.auto_commit, java.time.Instant.now().toString())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.commit_and_push)) },
        text = {
            Column {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.message)) },
                    placeholder = { Text(stringResource(R.string.commit_message_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                // ROB-14: inform user what will be used when left blank
                if (message.isBlank()) {
                    Text(
                        text = stringResource(R.string.commit_message_auto_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(message.ifBlank { autoCommitText }) }) {
                Text(stringResource(R.string.commit_and_push))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun formatEpoch(epochSec: Long): String = runCatching {
    val ldt = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSec), ZoneId.systemDefault())
    ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}.getOrDefault("")
