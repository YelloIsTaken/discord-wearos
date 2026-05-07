package com.yelloistaken.discordwearos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.yelloistaken.discordwearos.data.models.DiscordUser
import com.yelloistaken.discordwearos.data.models.Guild
import com.yelloistaken.discordwearos.ui.theme.DiscordBlurple
import com.yelloistaken.discordwearos.ui.theme.DiscordDark
import com.yelloistaken.discordwearos.ui.theme.DiscordGray
import com.yelloistaken.discordwearos.ui.theme.DiscordWhite

@Composable
fun GuildListScreen(
    currentUser: DiscordUser?,
    guilds: List<Guild>,
    isLoading: Boolean,
    error: String?,
    gatewayConnected: Boolean,
    onGuildSelected: (Guild) -> Unit,
    onLogout: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                HeaderRow(
                    username = currentUser?.displayName ?: "Discord",
                    isConnected = gatewayConnected
                )
            }

            when {
                isLoading && guilds.isEmpty() -> item {
                    CircularProgressIndicator(
                        indicatorColor = DiscordBlurple,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                !error.isNullOrBlank() -> item {
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                guilds.isEmpty() -> item {
                    Text(
                        text = "No servers found",
                        fontSize = 12.sp,
                        color = DiscordGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> items(guilds) { guild ->
                    GuildChip(guild = guild, onClick = { onGuildSelected(guild) })
                }
            }

            item {
                Chip(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    colors = ChipDefaults.secondaryChipColors(),
                    label = { Text("Sign Out", fontSize = 12.sp, color = DiscordGray) }
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(username: String, isConnected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = username,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DiscordWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (isConnected) " ●" else " ○",
            fontSize = 10.sp,
            color = if (isConnected) androidx.compose.ui.graphics.Color(0xFF23A55A)
            else DiscordGray,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun GuildChip(guild: Guild, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = ChipDefaults.chipColors(
            backgroundColor = DiscordDark,
            contentColor = DiscordWhite
        ),
        label = {
            Text(
                text = guild.name,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
    )
}
