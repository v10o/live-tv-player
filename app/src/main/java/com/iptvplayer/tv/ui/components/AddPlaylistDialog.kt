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
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAddXtream: (name: String, url: String, username: String, password: String) -> Unit,
    onAddM3u: (name: String, url: String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    success: String? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Xtream fields
    var xtreamName by remember { mutableStateOf("") }
    var xtreamUrl by remember { mutableStateOf("") }
    var xtreamUsername by remember { mutableStateOf("") }
    var xtreamPassword by remember { mutableStateOf("") }

    // M3U fields
    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .width(520.dp)
                .heightIn(max = 600.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NovaColors.Surface)
        ) {
            // Decorative gradient
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NovaColors.Primary.copy(alpha = 0.1f),
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Column {
                    Text(
                        text = "Add Playlist",
                        color = NovaColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Connect to your IPTV service",
                        color = NovaColors.TextMuted,
                        fontSize = 14.sp
                    )
                }

                // Tab selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabButton(
                        label = "Xtream Codes",
                        icon = "🌐",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    TabButton(
                        label = "M3U URL",
                        icon = "📋",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }

                if (selectedTab == 0) {
                    // Xtream Codes form
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField(
                            value = xtreamName,
                            onValueChange = { xtreamName = it },
                            label = "Name",
                            placeholder = "My IPTV Service"
                        )
                        InputField(
                            value = xtreamUrl,
                            onValueChange = { xtreamUrl = it },
                            label = "Server URL",
                            placeholder = "http://example.com:8080"
                        )
                        InputField(
                            value = xtreamUsername,
                            onValueChange = { xtreamUsername = it },
                            label = "Username",
                            placeholder = "Enter username"
                        )
                        InputField(
                            value = xtreamPassword,
                            onValueChange = { xtreamPassword = it },
                            label = "Password",
                            placeholder = "Enter password"
                        )
                    }
                } else {
                    // M3U form
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField(
                            value = m3uName,
                            onValueChange = { m3uName = it },
                            label = "Name",
                            placeholder = "My Playlist"
                        )
                        InputField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = "M3U URL",
                            placeholder = "http://example.com/playlist.m3u"
                        )
                    }
                }

                // Status feedback
                if (isLoading || error != null || success != null) {
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
                                fontSize = 20.sp
                            )
                            Text(
                                text = when {
                                    success != null -> success
                                    error != null -> error
                                    else -> "Connecting to server..."
                                },
                                color = when {
                                    success != null -> NovaColors.Primary
                                    error != null -> NovaColors.Secondary
                                    else -> NovaColors.TextSecondary
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DialogButton(
                        label = "Cancel",
                        isPrimary = false,
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    DialogButton(
                        label = if (isLoading) "Adding..." else "Add Playlist",
                        isPrimary = true,
                        enabled = !isLoading && success == null,
                        onClick = {
                            if (selectedTab == 0) {
                                if (xtreamName.isNotBlank() && xtreamUrl.isNotBlank() &&
                                    xtreamUsername.isNotBlank() && xtreamPassword.isNotBlank()
                                ) {
                                    onAddXtream(xtreamName, xtreamUrl, xtreamUsername, xtreamPassword)
                                }
                            } else {
                                if (m3uName.isNotBlank() && m3uUrl.isNotBlank()) {
                                    onAddM3u(m3uName, m3uUrl)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> Brush.linearGradient(
                        colors = listOf(NovaColors.Primary, NovaColors.PrimaryDark)
                    )
                    isFocused -> Brush.linearGradient(
                        colors = listOf(NovaColors.SurfaceVariant, NovaColors.SurfaceVariant)
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(NovaColors.Background, NovaColors.Background)
                    )
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = label,
                color = if (isSelected) NovaColors.OnPrimary else NovaColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun InputField(
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
                    if (isFocused) {
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    NovaColors.Primary.copy(alpha = 0.1f),
                                    NovaColors.Background
                                )
                            )
                        )
                    } else Modifier
                )
                .padding(12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = NovaColors.TextPrimary,
                    fontSize = 14.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = NovaColors.TextMuted,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    isPrimary: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !enabled -> Brush.linearGradient(
                        colors = listOf(NovaColors.SurfaceVariant.copy(alpha = 0.5f), NovaColors.SurfaceVariant.copy(alpha = 0.5f))
                    )
                    isPrimary -> Brush.linearGradient(
                        colors = listOf(NovaColors.Primary, NovaColors.PrimaryDark)
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(NovaColors.SurfaceVariant, NovaColors.SurfaceVariant)
                    )
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> NovaColors.TextMuted
                isPrimary -> NovaColors.OnPrimary
                else -> NovaColors.TextSecondary
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
