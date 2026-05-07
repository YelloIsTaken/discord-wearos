package com.yelloistaken.discordwearos.data.models

import com.google.gson.annotations.SerializedName

data class DiscordUser(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String?,
    val bot: Boolean = false,
    @SerializedName("global_name") val globalName: String?
) {
    val displayName: String get() = globalName ?: username
}

data class Guild(
    val id: String,
    val name: String,
    val icon: String?,
    val owner: Boolean = false
)

data class Channel(
    val id: String,
    val name: String?,
    val type: Int,
    @SerializedName("guild_id") val guildId: String?,
    val topic: String?,
    val position: Int = 0,
    @SerializedName("parent_id") val parentId: String?
) {
    val isText: Boolean get() = type == 0 || type == 5 || type == 15
    val isDm: Boolean get() = type == 1 || type == 3
    val displayName: String get() = name ?: "Unknown"
}

data class Message(
    val id: String,
    @SerializedName("channel_id") val channelId: String,
    val content: String,
    val author: DiscordUser,
    val timestamp: String,
    @SerializedName("edited_timestamp") val editedTimestamp: String?,
    val attachments: List<Attachment> = emptyList(),
    val type: Int = 0
) {
    val isDefault: Boolean get() = type == 0 || type == 19
}

data class Attachment(
    val id: String,
    val filename: String,
    val url: String,
    @SerializedName("content_type") val contentType: String?,
    val size: Int
)

data class SendMessageRequest(val content: String)

data class GatewayPayload(
    val op: Int,
    val d: Any?,
    val s: Int?,
    val t: String?
)

data class HelloData(
    @SerializedName("heartbeat_interval") val heartbeatInterval: Long
)

data class ReadyData(
    val v: Int,
    val user: DiscordUser,
    val guilds: List<UnavailableGuild>,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("resume_gateway_url") val resumeGatewayUrl: String
)

data class UnavailableGuild(
    val id: String,
    val unavailable: Boolean = true
)

data class GuildCreateData(
    val id: String,
    val name: String,
    val icon: String?,
    val channels: List<Channel>
)

sealed class GatewayEvent {
    data class Ready(val data: ReadyData) : GatewayEvent()
    data class GuildCreated(val guild: Guild, val channels: List<Channel>) : GatewayEvent()
    data class MessageCreated(val message: Message) : GatewayEvent()
    data class MessageUpdated(val message: Message) : GatewayEvent()
    data class Reconnect(val canResume: Boolean) : GatewayEvent()
    object Disconnected : GatewayEvent()
}
