# Discord WearOS

A Discord client for Wear OS 3+ with real-time messaging, notifications, and voice input.

## Features

- **Server & channel browsing** — scroll through your guilds and text channels
- **Real-time messages** — Discord Gateway WebSocket delivers messages instantly
- **Send messages** — voice dictation (🎤), quick emoji reactions, or short preset replies
- **Wear OS notifications** — pop-up notifications for new messages with inline voice reply
- **Persistent background connection** — foreground service keeps the gateway alive
- **Auto-reconnect** — session resume on network drops

## Setup

### Prerequisites

- Android Studio Hedgehog or later
- A device or emulator running **Wear OS 3.0+** (API 30+)
- A Discord **Bot Token** (create at [discord.com/developers](https://discord.com/developers/applications))

### Bot configuration

1. Create an application at the Discord Developer Portal
2. Under **Bot**, enable these **Privileged Gateway Intents**:
   - Server Members Intent
   - Message Content Intent
3. Invite the bot to your servers with at minimum `Read Messages` + `Send Messages` permissions
4. Copy the bot token

### Building

```bash
# Generate Gradle wrapper (only needed once)
gradle wrapper --gradle-version 8.9

# Build debug APK
./gradlew assembleDebug

# Install on connected Wear OS device / emulator
./gradlew installDebug
```

### First launch

1. Open the app on your watch
2. Tap **🎤 Enter Token** and speak your bot token
   - Voice recognition strips spaces, so speaking character-by-character works well
3. The app connects to the Discord Gateway and loads your servers

## Architecture

```text
data/
  models/       — Discord data classes (Guild, Channel, Message, Gateway events)
  api/          — Retrofit REST service (messages, channels, guilds)
  gateway/      — OkHttp WebSocket Discord Gateway v10
  local/        — EncryptedSharedPreferences token storage
  repository/   — Coordinates REST calls
service/
  GatewayService  — Foreground service hosting the WebSocket
  GatewayEventBus — SharedFlow bridge between service and ViewModel
notifications/
  NotificationHelper        — Message notifications with RemoteInput voice reply
  NotificationActionReceiver — Handles reply/dismiss BroadcastReceiver
ui/
  viewmodel/DiscordViewModel — Single ViewModel managing all state
  screens/
    LoginScreen      — Voice-input token entry
    GuildListScreen  — Server list with gateway status indicator
    ChannelListScreen — Text channel list
    MessageScreen    — Message bubbles + voice/emoji send bar
  theme/DiscordWearTheme   — Discord colour palette for Wear OS Compose
```

## Navigation

Swipe right at any screen to go back (standard Wear OS dismiss gesture).

```text
Loading → [Login] → Guilds → Channels → Messages
```

## Notes

- Bot tokens only. Using raw user tokens is against Discord's ToS.
- `MESSAGE_CONTENT` is a privileged intent — enable it in the Developer Portal or message content will be empty.
- Notifications fire for every incoming `MESSAGE_CREATE` event while the service runs; filter by channel if desired.
