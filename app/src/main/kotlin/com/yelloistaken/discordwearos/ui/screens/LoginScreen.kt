package com.yelloistaken.discordwearos.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.yelloistaken.discordwearos.ui.theme.DiscordBlurple
import com.yelloistaken.discordwearos.ui.theme.DiscordGray

@Composable
fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    onLogin: (String) -> Unit
) {
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val token = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                // voice recognition adds spaces in tokens — remove them
                ?.replace(" ", "")
            if (!token.isNullOrBlank()) {
                onLogin(token)
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

            item {
                Text(
                    text = "Speak or type your bot token to sign in",
                    fontSize = 11.sp,
                    color = DiscordGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (!error.isNullOrBlank()) {
                item {
                    Text(
                        text = error,
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
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your bot token")
                                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                            }
                            voiceLauncher.launch(intent)
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
                    text = "Token stored locally on device only",
                    fontSize = 10.sp,
                    color = DiscordGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}
