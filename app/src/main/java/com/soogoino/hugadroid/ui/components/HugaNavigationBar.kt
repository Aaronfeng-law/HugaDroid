package com.soogoino.hugadroid.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.soogoino.hugadroid.R

enum class HugaTab { HOME, POSTS, FILES, SETTINGS }

@Composable
fun HugaNavigationBar(
    selected: HugaTab,
    onHome: () -> Unit,
    onPosts: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == HugaTab.HOME,
            onClick = onHome,
            icon = {
                Icon(
                    if (selected == HugaTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.home)) },
        )
        NavigationBarItem(
            selected = selected == HugaTab.POSTS,
            onClick = onPosts,
            icon = {
                Icon(
                    if (selected == HugaTab.POSTS) Icons.Filled.Article else Icons.Outlined.Article,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.posts)) },
        )
        NavigationBarItem(
            selected = selected == HugaTab.FILES,
            onClick = onFiles,
            icon = {
                Icon(
                    if (selected == HugaTab.FILES) Icons.Filled.FolderOpen else Icons.Outlined.FolderOpen,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.files)) },
        )
        NavigationBarItem(
            selected = selected == HugaTab.SETTINGS,
            onClick = onSettings,
            icon = {
                Icon(
                    if (selected == HugaTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.settings)) },
        )
    }
}
