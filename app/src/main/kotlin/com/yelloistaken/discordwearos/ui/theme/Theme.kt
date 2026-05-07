package com.yelloistaken.discordwearos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors

val DiscordBlurple = Color(0xFF5865F2)
val DiscordDark = Color(0xFF2B2D31)
val DiscordDarker = Color(0xFF1E1F22)
val DiscordGreen = Color(0xFF23A55A)
val DiscordRed = Color(0xFFDA373C)
val DiscordWhite = Color(0xFFDBDBDB)
val DiscordGray = Color(0xFF80848E)

private val DiscordColors = Colors(
    primary = DiscordBlurple,
    primaryVariant = Color(0xFF4752C4),
    secondary = DiscordGreen,
    secondaryVariant = Color(0xFF1A7A42),
    background = DiscordDarker,
    surface = DiscordDark,
    error = DiscordRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DiscordWhite,
    onSurface = DiscordWhite,
    onError = Color.White
)

@Composable
fun DiscordWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DiscordColors,
        content = content
    )
}
