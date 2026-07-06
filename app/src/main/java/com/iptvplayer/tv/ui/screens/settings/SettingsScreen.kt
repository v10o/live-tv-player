package com.iptvplayer.tv.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun SettingsScreen(
    onBackPress: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())

    var selectedPlayer by remember { mutableStateOf("ExoPlayer") }
    var bufferSize by remember { mutableStateOf("Medium") }
    var showGroupManager by remember { mutableStateOf(false) }

    BackHandler {
        if (showGroupManager) {
            showGroupManager = false
            viewModel.clearGroupSelection()
        } else {
            onBackPress()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovaColors.Surface)
                    .clickable {
                        if (showGroupManager) {
                            showGroupManager = false
                            viewModel.clearGroupSelection()
                        } else {
                            onBackPress()
                        }
                    }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NovaColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = if (showGroupManager) "Manage Groups" else "Settings",
                    color = NovaColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (showGroupManager) "Show/hide channel groups" else "Configure your Live TV experience",
                    color = NovaColors.TextMuted,
                    fontSize = 14.sp
                )
            }
        }

        if (showGroupManager) {
            GroupManagerContent(
                playlists = playlists,
                uiState = uiState,
                onPlaylistSelected = { viewModel.loadGroupsForPlaylist(it.id) },
                onToggleGroup = { viewModel.toggleGroup(it) },
                onSelectAll = { viewModel.selectAll() },
                onUnselectAll = { viewModel.unselectAll() }
            )
        } else {
            SettingsContent(
                selectedPlayer = selectedPlayer,
                bufferSize = bufferSize,
                onPlayerChange = { selectedPlayer = it },
                onBufferChange = { bufferSize = it },
                onManageGroups = { showGroupManager = true }
            )
        }
    }
}

@Composable
private fun SettingsContent(
    selectedPlayer: String,
    bufferSize: String,
    onPlayerChange: (String) -> Unit,
    onBufferChange: (String) -> Unit,
    onManageGroups: () -> Unit
) {
    TvLazyColumn(
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Channel Groups
        item {
            SectionTitle(icon = Icons.Default.LiveTv, title = "Live TV")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Folder,
                title = "Manage Channel Groups",
                subtitle = "Show or hide groups for each playlist",
                onClick = onManageGroups
            )
        }

        // Player Settings
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(icon = Icons.Default.PlayArrow, title = "Player")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Speed,
                title = "Buffer Size",
                subtitle = bufferSize,
                onClick = {
                    onBufferChange(
                        when (bufferSize) {
                            "Small" -> "Medium"
                            "Medium" -> "Large"
                            else -> "Small"
                        }
                    )
                }
            )
        }

        // Display Settings
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(icon = Icons.Default.DarkMode, title = "Display")
        }

        item {
            SettingsItem(
                icon = Icons.Default.AspectRatio,
                title = "Aspect Ratio",
                subtitle = "Auto",
                onClick = { }
            )
        }

        // About
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(icon = Icons.Default.Info, title = "About")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "1.0.0",
                onClick = { }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Description,
                title = "Licenses",
                subtitle = "Open source licenses",
                onClick = { }
            )
        }
    }
}

@Composable
private fun GroupManagerContent(
    playlists: List<Playlist>,
    uiState: SettingsUiState,
    onPlaylistSelected: (Playlist) -> Unit,
    onToggleGroup: (String) -> Unit,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
    ) {
        // Playlist selector (left side)
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(end = 24.dp)
        ) {
            Text(
                text = "Select Playlist",
                color = NovaColors.TextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TvLazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistSelectorItem(
                        playlist = playlist,
                        isSelected = uiState.selectedPlaylistId == playlist.id,
                        onClick = { onPlaylistSelected(playlist) }
                    )
                }
            }
        }

        // Groups list (right side)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (uiState.selectedPlaylistId != null) {
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        label = "Select All",
                        onClick = onSelectAll
                    )
                    ActionButton(
                        label = "Unselect All",
                        onClick = onUnselectAll
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    val visibleCount = uiState.allGroups.size - uiState.hiddenGroups.size
                    Text(
                        text = "$visibleCount / ${uiState.allGroups.size} visible",
                        color = NovaColors.TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                if (uiState.isLoadingGroups) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading groups...", color = NovaColors.TextMuted)
                    }
                } else if (uiState.allGroups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No groups found", color = NovaColors.TextMuted)
                    }
                } else {
                    TvLazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.allGroups) { group ->
                            GroupCheckboxItem(
                                group = group,
                                isChecked = !uiState.hiddenGroups.contains(group),
                                onToggle = { onToggleGroup(group) }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = NovaColors.TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select a playlist to manage groups",
                            color = NovaColors.TextMuted,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistSelectorItem(
    playlist: Playlist,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> NovaColors.Primary.copy(alpha = 0.15f)
                    isFocused -> NovaColors.SurfaceVariant
                    else -> NovaColors.Surface
                }
            )
            .then(
                if (isSelected || isFocused) {
                    Modifier.border(1.5.dp, NovaColors.Primary, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = if (isSelected) NovaColors.Primary else NovaColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = "${playlist.channelsCount} channels",
                color = NovaColors.TextMuted,
                fontSize = 12.sp
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = NovaColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GroupCheckboxItem(
    group: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) NovaColors.SurfaceVariant else Color.Transparent
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onToggle() }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (isChecked) NovaColors.Primary else NovaColors.TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = group,
            color = if (isChecked) NovaColors.TextPrimary else NovaColors.TextMuted,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) NovaColors.Primary else NovaColors.Surface
            )
            .then(
                if (isFocused) Modifier else Modifier.border(1.dp, NovaColors.Border, RoundedCornerShape(8.dp))
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (isFocused) Color.Black else NovaColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NovaColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            color = NovaColors.Primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFocused) {
                    Brush.linearGradient(
                        colors = listOf(
                            NovaColors.Primary.copy(alpha = 0.15f),
                            NovaColors.Surface
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(NovaColors.Surface, NovaColors.Surface)
                    )
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isFocused) NovaColors.Primary.copy(alpha = 0.2f)
                    else NovaColors.SurfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) NovaColors.Primary else NovaColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isFocused) NovaColors.TextPrimary else NovaColors.TextSecondary,
                fontSize = 16.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                color = NovaColors.TextMuted,
                fontSize = 13.sp
            )
        }

        // Arrow
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isFocused) NovaColors.Primary else NovaColors.TextMuted,
            modifier = Modifier.size(24.dp)
        )
    }
}
