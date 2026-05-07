package com.yelloistaken.discordwearos

import android.app.Application
import com.yelloistaken.discordwearos.notifications.createNotificationChannels

class DiscordApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
    }
}
