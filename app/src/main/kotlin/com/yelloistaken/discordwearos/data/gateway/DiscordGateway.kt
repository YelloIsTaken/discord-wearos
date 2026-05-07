package com.yelloistaken.discordwearos.data.gateway

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.yelloistaken.discordwearos.data.models.Channel
import com.yelloistaken.discordwearos.data.models.DiscordUser
import com.yelloistaken.discordwearos.data.models.GatewayEvent
import com.yelloistaken.discordwearos.data.models.GuildCreateData
import com.yelloistaken.discordwearos.data.models.HelloData
import com.yelloistaken.discordwearos.data.models.Message
import com.yelloistaken.discordwearos.data.models.ReadyData
import com.yelloistaken.discordwearos.data.models.Guild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val TAG = "DiscordGateway"
private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"

// Gateway opcodes
private const val OP_DISPATCH = 0
private const val OP_HEARTBEAT = 1
private const val OP_IDENTIFY = 2
private const val OP_RESUME = 6
private const val OP_RECONNECT = 7
private const val OP_INVALID_SESSION = 9
private const val OP_HELLO = 10
private const val OP_HEARTBEAT_ACK = 11

// Gateway intents: GUILDS | GUILD_MESSAGES | MESSAGE_CONTENT | DIRECT_MESSAGES | DIRECT_MESSAGE_REACTIONS
private const val GATEWAY_INTENTS = 1 or 512 or 32768 or 4096 or 8192

class DiscordGateway(private val token: String, private val isBot: Boolean) {

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var lastSequence: Int? = null
    private var sessionId: String? = null
    private var resumeUrl: String? = null
    private var heartbeatAckReceived = true

    private val _events = MutableSharedFlow<GatewayEvent>(
        replay = 0,
        extraBufferCapacity = 128
    )
    val events: SharedFlow<GatewayEvent> = _events

    fun connect() {
        val url = resumeUrl ?: GATEWAY_URL
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, Listener())
        Log.d(TAG, "Connecting to $url")
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "Logout")
        webSocket = null
        sessionId = null
        resumeUrl = null
        lastSequence = null
        Log.d(TAG, "Disconnected")
    }

    fun updatePresence(status: String = "online") {
        val payload = """{"op":3,"d":{"since":null,"activities":[],"status":"$status","afk":false}}"""
        webSocket?.send(payload)
    }

    private inner class Listener : WebSocketListener() {

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleRawMessage(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code $reason")
            val shouldResume = code != 4004 && code != 4010 && code != 4011 && code != 4014
            scope.launch { _events.emit(GatewayEvent.Reconnect(shouldResume)) }
        }
    }

    private fun handleRawMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val op = json.get("op")?.asInt ?: return
            val s = json.get("s")?.takeIf { !it.isJsonNull }?.asInt
            val t = json.get("t")?.takeIf { !it.isJsonNull }?.asString
            val d = json.get("d")

            if (s != null) lastSequence = s

            when (op) {
                OP_HELLO -> handleHello(d.asJsonObject)
                OP_DISPATCH -> handleDispatch(t, d)
                OP_HEARTBEAT -> sendHeartbeat()
                OP_HEARTBEAT_ACK -> heartbeatAckReceived = true
                OP_RECONNECT -> reconnect(canResume = true)
                OP_INVALID_SESSION -> {
                    val resumable = d?.asBoolean ?: false
                    reconnect(canResume = resumable)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }

    private fun handleHello(data: JsonObject) {
        val hello = gson.fromJson(data, HelloData::class.java)
        startHeartbeat(hello.heartbeatInterval)

        if (sessionId != null && lastSequence != null) {
            sendResume()
        } else {
            sendIdentify()
        }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatAckReceived = true
        heartbeatJob = scope.launch {
            delay((intervalMs * Random.nextFloat()).toLong())
            while (isActive) {
                if (!heartbeatAckReceived) {
                    Log.w(TAG, "Heartbeat ACK not received, reconnecting")
                    reconnect(canResume = true)
                    break
                }
                heartbeatAckReceived = false
                sendHeartbeat()
                delay(intervalMs)
            }
        }
    }

    private fun sendHeartbeat() {
        val seq = lastSequence?.toString() ?: "null"
        webSocket?.send("""{"op":$OP_HEARTBEAT,"d":$seq}""")
    }

    private fun resolveAuthToken(): String = when {
        !isBot -> token
        token.startsWith("Bot ") || token.startsWith("Bearer ") -> token
        else -> "Bot $token"
    }

    private fun sendIdentify() {
        val authToken = resolveAuthToken()
        val payload = if (isBot) {
            """{"op":$OP_IDENTIFY,"d":{"token":"$authToken","properties":{"os":"android","browser":"discord-wearos","device":"wear-os"},"intents":$GATEWAY_INTENTS,"compress":false,"large_threshold":50}}"""
        } else {
            // User account: omit intents (bots-only field); mimic the Android client
            """{"op":$OP_IDENTIFY,"d":{"token":"$authToken","properties":{"os":"android","browser":"Discord Android","device":"android"},"compress":false}}"""
        }
        webSocket?.send(payload)
        Log.d(TAG, "Sent IDENTIFY (isBot=$isBot)")
    }

    private fun sendResume() {
        val authToken = resolveAuthToken()
        val payload = """
            {
              "op": $OP_RESUME,
              "d": {
                "token": "$authToken",
                "session_id": "$sessionId",
                "seq": $lastSequence
              }
            }
        """.trimIndent()
        webSocket?.send(payload)
        Log.d(TAG, "Sent RESUME")
    }

    private fun handleDispatch(eventType: String?, data: com.google.gson.JsonElement?) {
        if (data == null || data.isJsonNull) return
        try {
            when (eventType) {
                "READY" -> {
                    val ready = gson.fromJson(data, ReadyData::class.java)
                    sessionId = ready.sessionId
                    resumeUrl = ready.resumeGatewayUrl
                    scope.launch { _events.emit(GatewayEvent.Ready(ready)) }
                }
                "GUILD_CREATE" -> {
                    val obj = data.asJsonObject
                    val guildData = gson.fromJson(obj, GuildCreateData::class.java)
                    val guild = Guild(
                        id = guildData.id,
                        name = guildData.name,
                        icon = guildData.icon
                    )
                    val channels = guildData.channels
                        .filter { it.isText }
                        .sortedBy { it.position }
                    scope.launch {
                        _events.emit(GatewayEvent.GuildCreated(guild, channels))
                    }
                }
                "MESSAGE_CREATE" -> {
                    val message = gson.fromJson(data, Message::class.java)
                    scope.launch { _events.emit(GatewayEvent.MessageCreated(message)) }
                }
                "MESSAGE_UPDATE" -> {
                    val message = gson.fromJson(data, Message::class.java)
                    scope.launch { _events.emit(GatewayEvent.MessageUpdated(message)) }
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling dispatch $eventType", e)
        }
    }

    private fun reconnect(canResume: Boolean) {
        if (!canResume) {
            sessionId = null
            lastSequence = null
        }
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "Reconnecting")
        webSocket = null
        scheduleReconnectWithDelay(2_000)
    }

    private fun scheduleReconnect() {
        scheduleReconnectWithDelay(5_000)
    }

    private fun scheduleReconnectWithDelay(delayMs: Long) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMs)
            reconnectJob = null
            connect()
        }
    }
}
