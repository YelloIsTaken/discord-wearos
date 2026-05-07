package com.yelloistaken.discordwearos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.yelloistaken.discordwearos.data.models.Channel
import com.yelloistaken.discordwearos.data.models.Guild
import com.yelloistaken.discordwearos.ui.theme.DiscordBlurple
import com.yelloistaken.discordwearos.ui.theme.DiscordDark
import com.yelloistaken.discordwearos.ui.theme.DiscordGray
import com.yelloistaken.discordwearos.ui.theme.DiscordWhite

@Composable
fun ChannelListScreen(
    guild: Guild,
    channels: List<Channel>,
    isLoading: Boolean,
    error: String?,
    onChannelSelected: (Channel) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = guild.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DiscordWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            when {
                isLoading && channels.isEmpty() -> item {
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
                channels.isEmpty() -> item {
                    Text(
                        text = "No text channels",
                        fontSize = 12.sp,
                        color = DiscordGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> items(channels) { channel ->
                    ChannelChip(channel = channel, onClick = { onChannelSelected(channel) })
                }
            }
        }
    }
}

@Composable
private fun ChannelChip(channel: Channel, onClick: () -> Unit) {
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
                text = "# ${channel.displayName}",
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        secondaryLabel = channel.topic?.takeIf { it.isNotBlank() }?.let { topic ->
            {
                Text(
                    text = topic,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = DiscordGray
                )
            }
        }
    )
}
