package com.yelloistaken.discordwearos.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.yelloistaken.discordwearos.data.gateway.DiscordGateway
import com.yelloistaken.discordwearos.data.local.TokenManager
import com.yelloistaken.discordwearos.data.models.GatewayEvent
import com.yelloistaken.discordwearos.notifications.NOTIFICATION_ID_SERVICE
import com.yelloistaken.discordwearos.notifications.buildServiceNotification
import com.yelloistaken.discordwearos.notifications.showMessageNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private const val TAG = "GatewayService"

const val ACTION_START = "START"
const val ACTION_STOP = "STOP"

class GatewayService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var gateway: DiscordGateway? = null
    private var eventsJob: Job? = null
    private var notifIdCounter = 1000

    val gatewayEvents: SharedFlow<GatewayEvent>?
        get() = gateway?.events

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID_SERVICE, buildServiceNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                scope.launch {
                    val token = TokenManager(this@GatewayService).getToken()
                    if (token != null) {
                        startGateway(token)
                    } else {
                        Log.w(TAG, "No token found, cannot start gateway")
                        stopSelf()
                    }
                }
            }
            ACTION_STOP -> {
                stopGateway()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startGateway(token: String) {
        eventsJob?.cancel()
        eventsJob = null
        gateway?.disconnect()
        gateway = DiscordGateway(token).also { gw ->
            gw.connect()
            eventsJob = scope.launch {
                gw.events.collect { event ->
                    handleGatewayEvent(event)
                }
            }
        }
        GatewayEventBus.setGateway(gateway!!)
        Log.d(TAG, "Gateway started")
    }

    private fun stopGateway() {
        eventsJob?.cancel()
        eventsJob = null
        gateway?.disconnect()
        gateway = null
        GatewayEventBus.clearGateway()
        Log.d(TAG, "Gateway stopped")
    }

    private fun handleGatewayEvent(event: GatewayEvent) {
        when (event) {
            is GatewayEvent.MessageCreated -> {
                GatewayEventBus.emit(event)
                val notifId = notifIdCounter++
                showMessageNotification(this, event.message, notifId)
            }
            is GatewayEvent.Reconnect -> {
                Log.d(TAG, "Reconnect event, canResume=${event.canResume}")
                GatewayEventBus.emit(event)
            }
            is GatewayEvent.Disconnected -> {
                GatewayEventBus.emit(event)
            }
            else -> GatewayEventBus.emit(event)
        }
    }

    override fun onDestroy() {
        stopGateway()
        scope.cancel()
        super.onDestroy()
    }
}

object GatewayEventBus {
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<GatewayEvent>(
        replay = 0,
        extraBufferCapacity = 256
    )
    val events: SharedFlow<GatewayEvent> = _events

    private var gateway: DiscordGateway? = null

    fun setGateway(gw: DiscordGateway) {
        gateway = gw
    }

    fun clearGateway() {
        gateway = null
    }

    fun emit(event: GatewayEvent) {
        _events.tryEmit(event)
    }
}
