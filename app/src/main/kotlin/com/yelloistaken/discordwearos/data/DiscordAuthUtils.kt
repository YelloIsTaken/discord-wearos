package com.yelloistaken.discordwearos.data

fun resolveAuthToken(token: String, isBot: Boolean): String = when {
    !isBot -> token
    token.startsWith("Bot ") || token.startsWith("Bearer ") -> token
    else -> "Bot $token"
}
