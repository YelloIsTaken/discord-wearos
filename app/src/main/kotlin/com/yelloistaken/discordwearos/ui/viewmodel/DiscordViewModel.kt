package com.yelloistaken.discordwearos.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yelloistaken.discordwearos.data.local.TokenManager
import com.yelloistaken.discordwearos.data.models.Channel
import com.yelloistaken.discordwearos.data.models.DiscordUser
import com.yelloistaken.discordwearos.data.models.GatewayEvent
import com.yelloistaken.discordwearos.data.models.Guild
import com.yelloistaken.discordwearos.data.models.Message
import com.yelloistaken.discordwearos.data.repository.DiscordRepository
import com.yelloistaken.discordwearos.data.repository.Result
import com.yelloistaken.discordwearos.service.ACTION_START
import com.yelloistaken.discordwearos.service.ACTION_STOP
import com.yelloistaken.discordwearos.service.GatewayEventBus
import com.yelloistaken.discordwearos.service.GatewayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: DiscordUser? = null,
    val guilds: List<Guild> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val messages: List<Message> = emptyList(),
    val selectedGuild: Guild? = null,
    val selectedChannel: Channel? = null,
    val error: String? = null,
    val gatewayConnected: Boolean = false,
    val pendingReply: PendingReply? = null
)

data class PendingReply(val channelId: String, val text: String)

class DiscordViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private var repository: DiscordRepository? = null

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedToken = tokenManager.tokenFlow.firstOrNull()
            if (!savedToken.isNullOrBlank()) {
                initializeWithToken(savedToken)
            }
        }
        collectGatewayEvents()
    }

    private fun collectGatewayEvents() {
        viewModelScope.launch {
            GatewayEventBus.events.collect { event ->
                handleGatewayEvent(event)
            }
        }
    }

    private fun handleGatewayEvent(event: GatewayEvent) {
        when (event) {
            is GatewayEvent.Ready -> {
                _uiState.update { state ->
                    state.copy(
                        currentUser = event.data.user,
                        gatewayConnected = true,
                        isLoading = false
                    )
                }
                viewModelScope.launch { tokenManager.saveUserInfo(event.data.user.id, event.data.user.displayName) }
            }
            is GatewayEvent.GuildCreated -> {
                _uiState.update { state ->
                    val existing = state.guilds.any { it.id == event.guild.id }
                    val updatedGuilds = if (existing) state.guilds else state.guilds + event.guild
                    state.copy(guilds = updatedGuilds)
                }
            }
            is GatewayEvent.MessageCreated -> {
                _uiState.update { state ->
                    if (event.message.channelId != state.selectedChannel?.id) return@update state
                    val exists = state.messages.any { it.id == event.message.id }
                    if (exists) state else state.copy(messages = state.messages + event.message)
                }
            }
            is GatewayEvent.MessageUpdated -> {
                _uiState.update { state ->
                    val idx = state.messages.indexOfFirst { it.id == event.message.id }
                    if (idx == -1) state
                    else state.copy(messages = state.messages.toMutableList().also { it[idx] = event.message })
                }
            }
            is GatewayEvent.Reconnect -> {
                _uiState.update { it.copy(gatewayConnected = false) }
            }
            GatewayEvent.Disconnected -> {
                _uiState.update { it.copy(gatewayConnected = false) }
            }
        }
    }

    fun login(token: String) {
        if (token.isBlank()) {
            _uiState.update { it.copy(error = "Token cannot be empty") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val repo = DiscordRepository(token)
            when (val result = repo.getCurrentUser()) {
                is Result.Success -> {
                    tokenManager.saveToken(token)
                    tokenManager.saveUserInfo(result.data.id, result.data.displayName)
                    repository = repo
                    _uiState.update { state ->
                        state.copy(
                            isLoggedIn = true,
                            currentUser = result.data,
                            isLoading = false
                        )
                    }
                    startGatewayService()
                    loadGuilds()
                }
                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = if (result.code == 401) "Invalid token" else result.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun initializeWithToken(token: String) {
        _uiState.update { it.copy(isLoading = true) }
        val repo = DiscordRepository(token)
        when (val result = repo.getCurrentUser()) {
            is Result.Success -> {
                repository = repo
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = true,
                        currentUser = result.data,
                        isLoading = false
                    )
                }
                startGatewayService(token)
                loadGuilds()
            }
            is Result.Error -> {
                tokenManager.clearAll()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadGuilds() {
        val repo = repository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repo.getGuilds()) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(guilds = result.data, isLoading = false)
                }
                is Result.Error -> _uiState.update { state ->
                    state.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun selectGuild(guild: Guild) {
        _uiState.update { it.copy(selectedGuild = guild, channels = emptyList()) }
        loadChannels(guild.id)
    }

    fun loadChannels(guildId: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repo.getChannels(guildId)) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(channels = result.data, isLoading = false)
                }
                is Result.Error -> _uiState.update { state ->
                    state.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun selectChannel(channel: Channel) {
        _uiState.update { it.copy(selectedChannel = channel, messages = emptyList()) }
        loadMessages(channel.id)
    }

    fun loadMessages(channelId: String, before: String? = null) {
        val repo = repository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = before == null, error = null) }
            when (val result = repo.getMessages(channelId, before = before)) {
                is Result.Success -> _uiState.update { state ->
                    val messages = if (before != null) {
                        result.data + state.messages
                    } else {
                        result.data
                    }
                    state.copy(messages = messages, isLoading = false)
                }
                is Result.Error -> _uiState.update { state ->
                    state.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun sendMessage(channelId: String, content: String) {
        if (content.isBlank()) return
        val repo = repository ?: return
        viewModelScope.launch {
            when (val result = repo.sendMessage(channelId, content)) {
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                is Result.Success -> {}
            }
        }
    }

    fun handlePendingReply(channelId: String, text: String) {
        sendMessage(channelId, text)
    }

    fun clearPendingReply() {
        _uiState.update { it.copy(pendingReply = null) }
    }

    fun logout() {
        viewModelScope.launch {
            stopGatewayService()
            tokenManager.clearAll()
            repository = null
            _uiState.value = AppUiState()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun startGatewayService() {
        val context = getApplication<Application>()
        val intent = Intent(context, GatewayService::class.java).apply {
            action = ACTION_START
        }
        context.startForegroundService(intent)
    }

    private fun stopGatewayService() {
        val context = getApplication<Application>()
        val intent = Intent(context, GatewayService::class.java).apply {
            action = ACTION_STOP
        }
        context.startService(intent)
    }
}
