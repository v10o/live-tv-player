package com.iptvplayer.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.*
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.model.PlaylistType
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun PlaylistOptionsDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onEdit: (name: String, serverUrl: String?, username: String?, password: String?, m3uUrl: String?) -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onLiveTV: (() -> Unit)? = null,
    onMovies: (() -> Unit)? = null,
    onSeries: (() -> Unit)? = null,
    isLoading: Boolean = false,
    error: String? = null,
    success: String? = null
) {
    var showEditMode by remember { mutableStateOf(false) }

    // Edit fields
    var name by remember { mutableStateOf(playlist.name) }
    var serverUrl by remember { mutableStateOf(playlist.serverUrl ?: "") }
    var username by remember { mutableStateOf(playlist.username ?: "") }
    var password by remember { mutableStateOf(playlist.password ?: "") }
    var m3uUrl by remember { mutableStateOf(playlist.m3uUrl ?: "") }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .width(480.dp)
                .heightIn(max = 550.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NovaColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (playlist.type == PlaylistType.XTREAM) "🌐" else "📋",
                        fontSize = 28.sp
                    )
                    Column {
                        Text(
                            text = if (showEditMode) "Edit Playlist" else playlist.name,
                            color = NovaColors.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${playlist.channelsCount} channels • ${playlist.type.name}",
                            color = NovaColors.TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                // Status feedback
                if (isLoading || error != null || success != null) {
                    StatusBox(isLoading, error, success)
                }

                if (showEditMode) {
                    // Edit Form
                    EditForm(
                        playlist = playlist,
                        name = name,
                        onNameChange = { name = it },
                        serverUrl = serverUrl,
                        onServerUrlChange = { serverUrl = it },
                        username = username,
                        onUsernameChange = { username = it },
                        password = password,
                        onPasswordChange = { password = it },
                        m3uUrl = m3uUrl,
                        onM3uUrlChange = { m3uUrl = it }
                    )

                    // Edit Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OptionButton(
                            label = "Cancel",
                            icon = "←",
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) { showEditMode = false }

                        OptionButton(
                            label = "Save",
                            icon = "💾",
                            isPrimary = true,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading && name.isNotBlank()
                        ) {
                            onEdit(
                                name,
                                serverUrl.ifBlank { null },
                                username.ifBlank { null },
                                password.ifBlank { null },
                                m3uUrl.ifBlank { null }
                            )
                        }
                    }
                } else {
                    // Navigation Options (Xtream only)
                    if (playlist.type == PlaylistType.XTREAM) {
                        Text(
                            text = "Open Content",
                            color = NovaColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        onLiveTV?.let {
                            OptionButton(
                                label = "Live TV",
                                icon = "📺",
                                isPrimary = true,
                                enabled = !isLoading
                            ) { it(); onDismiss() }
                        }

                        onMovies?.let {
                            OptionButton(
                                label = "Movies",
                                icon = "🎬",
                                enabled = !isLoading
                            ) { it(); onDismiss() }
                        }

                        onSeries?.let {
                            OptionButton(
                                label = "TV Series",
                                icon = "📚",
                                enabled = !isLoading
                            ) { it(); onDismiss() }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Manage",
                            color = NovaColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Options Menu
                    OptionButton(
                        label = "Edit Playlist",
                        icon = "✏️",
                        enabled = !isLoading
                    ) { showEditMode = true }

                    OptionButton(
                        label = "Refresh Channels",
                        icon = "🔄",
                        enabled = !isLoading
                    ) { onRefresh() }

                    OptionButton(
                        label = "Delete Playlist",
                        icon = "🗑️",
                        isDestructive = true,
                        enabled = !isLoading
                    ) { onDelete() }

                    Spacer(modifier = Modifier.height(8.dp))

                    OptionButton(
                        label = "Close",
                        icon = "✕",
                        enabled = !isLoading
                    ) { onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun EditForm(
    playlist: Playlist,
    name: String,
    onNameChange: (String) -> Unit,
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    m3uUrl: String,
    onM3uUrlChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditField(
            value = name,
            onValueChange = onNameChange,
            label = "Name",
            placeholder = "Playlist name"
        )

        if (playlist.type == PlaylistType.XTREAM) {
            EditField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                label = "Server URL",
                placeholder = "http://example.com:8080"
            )
            EditField(
                value = username,
                onValueChange = onUsernameChange,
                label = "Username",
                placeholder = "Enter username"
            )
            EditField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                placeholder = "Enter password"
            )
        } else {
            EditField(
                value = m3uUrl,
                onValueChange = onM3uUrlChange,
                label = "M3U URL",
                placeholder = "http://example.com/playlist.m3u"
            )
        }
    }
}

@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = NovaColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NovaColors.Background)
                .then(
                    if (isFocused) Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(NovaColors.Primary.copy(alpha = 0.1f), NovaColors.Background)
                        )
                    ) else Modifier
                )
                .padding(12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = NovaColors.TextPrimary, fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, color = NovaColors.TextMuted, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusBox(isLoading: Boolean, error: String?, success: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    success != null -> NovaColors.Primary.copy(alpha = 0.15f)
                    error != null -> NovaColors.Secondary.copy(alpha = 0.15f)
                    else -> NovaColors.SurfaceVariant
                }
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when {
                    success != null -> "✅"
                    error != null -> "❌"
                    else -> "⏳"
                },
                fontSize = 18.sp
            )
            Text(
                text = when {
                    success != null -> success
                    error != null -> error
                    else -> "Loading..."
                },
                color = when {
                    success != null -> NovaColors.Primary
                    error != null -> NovaColors.Secondary
                    else -> NovaColors.TextSecondary
                },
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun OptionButton(
    label: String,
    icon: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        !enabled -> NovaColors.SurfaceVariant.copy(alpha = 0.5f)
        isPrimary -> NovaColors.Primary
        isDestructive && isFocused -> NovaColors.Secondary
        isFocused -> NovaColors.SurfaceVariant
        else -> NovaColors.Background
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 18.sp)
        Text(
            text = label,
            color = when {
                !enabled -> NovaColors.TextMuted
                isPrimary -> NovaColors.OnPrimary
                isDestructive -> if (isFocused) NovaColors.OnPrimary else NovaColors.Secondary
                else -> NovaColors.TextPrimary
            },
            fontSize = 15.sp,
            fontWeight = if (isPrimary || (isDestructive && isFocused)) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
