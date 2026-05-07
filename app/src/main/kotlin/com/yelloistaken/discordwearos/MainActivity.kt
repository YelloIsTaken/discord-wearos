package com.yelloistaken.discordwearos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.yelloistaken.discordwearos.notifications.EXTRA_CHANNEL_ID
import com.yelloistaken.discordwearos.notifications.EXTRA_VOICE_REPLY
import com.yelloistaken.discordwearos.ui.screens.ChannelListScreen
import com.yelloistaken.discordwearos.ui.screens.GuildListScreen
import com.yelloistaken.discordwearos.ui.screens.LoginScreen
import com.yelloistaken.discordwearos.ui.screens.MessageScreen
import com.yelloistaken.discordwearos.ui.theme.DiscordBlurple
import com.yelloistaken.discordwearos.ui.theme.DiscordWearTheme
import com.yelloistaken.discordwearos.ui.viewmodel.DiscordViewModel

private const val ROUTE_LOADING = "loading"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_GUILDS = "guilds"
private const val ROUTE_CHANNELS = "channels"
private const val ROUTE_MESSAGES = "messages"

class MainActivity : ComponentActivity() {

    private val viewModel: DiscordViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        handleIncomingIntent(intent)

        setContent {
            DiscordWearTheme {
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberSwipeDismissableNavController()

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = ROUTE_LOADING
                ) {
                    composable(ROUTE_LOADING) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(indicatorColor = DiscordBlurple)
                        }
                        LaunchedEffect(uiState.isLoading, uiState.isLoggedIn) {
                            if (!uiState.isLoading) {
                                val destination = if (uiState.isLoggedIn) ROUTE_GUILDS else ROUTE_LOGIN
                                navController.navigate(destination) {
                                    popUpTo(ROUTE_LOADING) { inclusive = true }
                                }
                            }
                        }
                    }

                    composable(ROUTE_LOGIN) {
                        LoginScreen(
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onLogin = { token ->
                                viewModel.clearError()
                                viewModel.login(token)
                            }
                        )
                        LaunchedEffect(uiState.isLoggedIn) {
                            if (uiState.isLoggedIn) {
                                navController.navigate(ROUTE_GUILDS) {
                                    popUpTo(ROUTE_LOGIN) { inclusive = true }
                                }
                            }
                        }
                    }

                    composable(ROUTE_GUILDS) {
                        GuildListScreen(
                            currentUser = uiState.currentUser,
                            guilds = uiState.guilds,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            gatewayConnected = uiState.gatewayConnected,
                            onGuildSelected = { guild ->
                                viewModel.selectGuild(guild)
                                navController.navigate(ROUTE_CHANNELS)
                            },
                            onLogout = {
                                viewModel.logout()
                                navController.navigate(ROUTE_LOGIN) {
                                    popUpTo(ROUTE_GUILDS) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(ROUTE_CHANNELS) {
                        val guild = uiState.selectedGuild
                        if (guild != null) {
                            ChannelListScreen(
                                guild = guild,
                                channels = uiState.channels,
                                isLoading = uiState.isLoading,
                                error = uiState.error,
                                onChannelSelected = { channel ->
                                    viewModel.selectChannel(channel)
                                    navController.navigate(ROUTE_MESSAGES)
                                }
                            )
                        }
                    }

                    composable(ROUTE_MESSAGES) {
                        val channel = uiState.selectedChannel
                        if (channel != null) {
                            MessageScreen(
                                channel = channel,
                                messages = uiState.messages,
                                currentUserId = uiState.currentUser?.id,
                                isLoading = uiState.isLoading,
                                error = uiState.error,
                                onSendMessage = { content ->
                                    viewModel.sendMessage(channel.id, content)
                                },
                                onLoadMore = {
                                    val oldest = uiState.messages.firstOrNull()?.id
                                    viewModel.loadMessages(channel.id, before = oldest)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        val channelId = intent?.getStringExtra(EXTRA_CHANNEL_ID) ?: return
        val replyText = intent.getStringExtra(EXTRA_VOICE_REPLY) ?: return
        viewModel.handlePendingReply(channelId, replyText)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
