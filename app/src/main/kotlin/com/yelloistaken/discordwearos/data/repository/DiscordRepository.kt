package com.yelloistaken.discordwearos.data.repository

import com.yelloistaken.discordwearos.data.api.DiscordApiFactory
import com.yelloistaken.discordwearos.data.api.DiscordApiService
import com.yelloistaken.discordwearos.data.models.Channel
import com.yelloistaken.discordwearos.data.models.DiscordUser
import com.yelloistaken.discordwearos.data.models.Guild
import com.yelloistaken.discordwearos.data.models.Message

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
}

class DiscordRepository(token: String) {

    private val api: DiscordApiService = DiscordApiFactory.create(token)

    suspend fun getCurrentUser(): Result<DiscordUser> = try {
        val response = api.getCurrentUser()
        if (response.isSuccessful) {
            Result.Success(response.body()!!)
        } else {
            Result.Error("Auth failed: ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    suspend fun getGuilds(): Result<List<Guild>> = try {
        val response = api.getGuilds()
        if (response.isSuccessful) {
            Result.Success(response.body() ?: emptyList())
        } else {
            Result.Error("Failed to load servers: ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    suspend fun getChannels(guildId: String): Result<List<Channel>> = try {
        val response = api.getChannels(guildId)
        if (response.isSuccessful) {
            val channels = (response.body() ?: emptyList())
                .filter { it.isText }
                .sortedBy { it.position }
            Result.Success(channels)
        } else {
            Result.Error("Failed to load channels: ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    suspend fun getMessages(
        channelId: String,
        limit: Int = 50,
        before: String? = null
    ): Result<List<Message>> = try {
        val response = api.getMessages(channelId, limit, before)
        if (response.isSuccessful) {
            val messages = (response.body() ?: emptyList()).reversed()
            Result.Success(messages)
        } else {
            Result.Error("Failed to load messages: ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    suspend fun sendMessage(channelId: String, content: String): Result<Message> = try {
        val response = api.sendMessage(
            channelId,
            com.yelloistaken.discordwearos.data.models.SendMessageRequest(content)
        )
        if (response.isSuccessful) {
            Result.Success(response.body()!!)
        } else {
            Result.Error("Failed to send: ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    suspend fun getDmChannels(): Result<List<Channel>> = try {
        val response = api.getDmChannels()
        if (response.isSuccessful) {
            Result.Success(response.body() ?: emptyList())
        } else {
            Result.Error("Failed to load DMs: ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }
}
