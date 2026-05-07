package com.yelloistaken.discordwearos.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.yelloistaken.discordwearos.data.models.Channel
import com.yelloistaken.discordwearos.data.models.Message
import com.yelloistaken.discordwearos.ui.theme.DiscordBlurple
import com.yelloistaken.discordwearos.ui.theme.DiscordDark
import com.yelloistaken.discordwearos.ui.theme.DiscordDarker
import com.yelloistaken.discordwearos.ui.theme.DiscordGray
import com.yelloistaken.discordwearos.ui.theme.DiscordWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MessageScreen(
    channel: Channel,
    messages: List<Message>,
    currentUserId: String?,
    isLoading: Boolean,
    error: String?,
    onSendMessage: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    var prevNewestId by remember { mutableStateOf<String?>(null) }
    val newestId = messages.lastOrNull()?.id

    LaunchedEffect(newestId) {
        if (messages.isNotEmpty() && newestId != prevNewestId) {
            listState.scrollToItem(messages.size - 1)
            prevNewestId = newestId
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ChannelHeader(channel = channel, onLoadMore = onLoadMore)
            }

            when {
                isLoading && messages.isEmpty() -> item {
                    CircularProgressIndicator(
                        indicatorColor = DiscordBlurple,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                !error.isNullOrBlank() -> item {
                    Text(
                        text = error,
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                messages.isEmpty() -> item {
                    Text(
                        text = "No messages yet",
                        fontSize = 12.sp,
                        color = DiscordGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isSelf = message.author.id == currentUserId
                    )
                }
            }

            item {
                SendBar(channelId = channel.id, onSend = onSendMessage)
            }
        }
    }
}

@Composable
private fun ChannelHeader(channel: Channel, onLoadMore: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "# ${channel.displayName}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DiscordWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        CompactButton(
            onClick = onLoadMore,
            colors = ButtonDefaults.secondaryButtonColors(),
            modifier = Modifier.size(32.dp)
        ) {
            Text("↑", fontSize = 12.sp)
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isSelf: Boolean) {
    val bubbleColor = if (isSelf) DiscordBlurple else DiscordDark
    val alignment = if (isSelf) Alignment.End else Alignment.Start
    val textAlign = if (isSelf) TextAlign.End else TextAlign.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalAlignment = alignment
    ) {
        if (!isSelf) {
            Text(
                text = message.author.displayName,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = DiscordBlurple,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = if (isSelf) 12.dp else 2.dp,
                        topEnd = if (isSelf) 2.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val displayText = when {
                message.content.isNotBlank() -> message.content
                message.attachments.isNotEmpty() -> "[${message.attachments.size} attachment(s)]"
                else -> "[message]"
            }
            Text(
                text = displayText,
                fontSize = 12.sp,
                color = DiscordWhite,
                textAlign = textAlign
            )
        }

        Text(
            text = formatTimestamp(message.timestamp),
            fontSize = 8.sp,
            color = DiscordGray,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun SendBar(channelId: String, onSend: (String) -> Unit) {
    val context = LocalContext.current

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) {
                onSend(text)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your message")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    try { voiceLauncher.launch(intent) } catch (_: Exception) { }
                }
            },
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = DiscordBlurple),
            shape = CircleShape
        ) {
            Text("🎤", fontSize = 16.sp)
        }

        Button(
            onClick = { onSend("👍") },
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.secondaryButtonColors(),
            shape = CircleShape
        ) {
            Text("👍", fontSize = 14.sp)
        }

        Button(
            onClick = { onSend("✅") },
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.secondaryButtonColors(),
            shape = CircleShape
        ) {
            Text("✅", fontSize = 14.sp)
        }

        Button(
            onClick = { onSend("Ok") },
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.secondaryButtonColors(),
            shape = CircleShape
        ) {
            Text("Ok", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTimestamp(timestamp: String): String = try {
    val instant = Instant.parse(timestamp)
    timeFormatter.format(instant.atZone(ZoneId.systemDefault()))
} catch (e: Exception) {
    ""
}
