package com.yelloistaken.discordwearos.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.yelloistaken.discordwearos.ui.theme.DiscordBlurple
import com.yelloistaken.discordwearos.ui.theme.DiscordDark
import com.yelloistaken.discordwearos.ui.theme.DiscordGray
import com.yelloistaken.discordwearos.ui.theme.DiscordWhite

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    onLogin: (token: String, isBot: Boolean) -> Unit
) {
    val context = LocalContext.current
    var isBot by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val token = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                ?.replace(" ", "")
            if (!token.isNullOrBlank()) {
                voiceError = null
                onLogin(token, isBot)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Discord",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DiscordBlurple,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
                )
            }

            // Mode selector: Account vs Bot
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    ModeChip(
                        label = "Account",
                        selected = !isBot,
                        onClick = { isBot = false }
                    )
                    ModeChip(
                        label = "Bot",
                        selected = isBot,
                        onClick = { isBot = true }
                    )
                }
            }

            // Context-sensitive help text
            item {
                Text(
                    text = if (isBot) {
                        "Developer Portal → Bot → Copy Token"
                    } else {
                        "Browser: F12 → Network → any request → Authorization header"
                    },
                    fontSize = 11.sp,
                    color = DiscordGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            val displayError = error.takeIf { !it.isNullOrBlank() } ?: voiceError
            if (displayError != null) {
                item {
                    Text(
                        text = displayError,
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                if (isLoading) {
                    CircularProgressIndicator(
                        indicatorColor = DiscordBlurple,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            voiceError = null
                            val prompt = if (isBot) "Say your bot token" else "Say your account token"
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                            }
                            try {
                                voiceLauncher.launch(intent)
                            } catch (e: ActivityNotFoundException) {
                                Log.w(TAG, "No speech recognizer available", e)
                                voiceError = "Speech recognition not available"
                            } catch (e: SecurityException) {
                                Log.w(TAG, "Microphone permission denied", e)
                                voiceError = "Microphone permission denied"
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to launch voice input", e)
                                voiceError = "Could not open voice input"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DiscordBlurple)
                    ) {
                        Text(
                            text = "🎤  Enter Token",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Token stored encrypted on device only",
                    fontSize = 10.sp,
                    color = DiscordGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        colors = if (selected) {
            ChipDefaults.chipColors(
                backgroundColor = DiscordBlurple,
                contentColor = DiscordWhite
            )
        } else {
            ChipDefaults.chipColors(
                backgroundColor = DiscordDark,
                contentColor = DiscordGray
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    )
}
