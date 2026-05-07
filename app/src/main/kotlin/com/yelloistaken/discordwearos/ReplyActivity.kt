package com.yelloistaken.discordwearos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import com.yelloistaken.discordwearos.notifications.EXTRA_CHANNEL_ID
import com.yelloistaken.discordwearos.notifications.EXTRA_VOICE_REPLY

class ReplyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = RemoteInput.getResultsFromIntent(intent)
        val replyText = bundle?.getCharSequence(EXTRA_VOICE_REPLY)?.toString()
        val channelId = intent?.getStringExtra(EXTRA_CHANNEL_ID)

        if (!replyText.isNullOrBlank() && !channelId.isNullOrBlank()) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_CHANNEL_ID, channelId)
                    putExtra(EXTRA_VOICE_REPLY, replyText)
                }
            )
        }
        finish()
    }
}
