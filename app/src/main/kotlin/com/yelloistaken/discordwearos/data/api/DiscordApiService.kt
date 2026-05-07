package com.yelloistaken.discordwearos.data.api

import com.yelloistaken.discordwearos.data.models.Channel
import com.yelloistaken.discordwearos.data.models.DiscordUser
import com.yelloistaken.discordwearos.data.models.Guild
import com.yelloistaken.discordwearos.data.models.Message
import com.yelloistaken.discordwearos.data.models.SendMessageRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface DiscordApiService {

    @GET("users/@me")
    suspend fun getCurrentUser(): Response<DiscordUser>

    @GET("users/@me/guilds")
    suspend fun getGuilds(): Response<List<Guild>>

    @GET("guilds/{guild_id}/channels")
    suspend fun getChannels(@Path("guild_id") guildId: String): Response<List<Channel>>

    @GET("channels/{channel_id}/messages")
    suspend fun getMessages(
        @Path("channel_id") channelId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<List<Message>>

    @POST("channels/{channel_id}/messages")
    suspend fun sendMessage(
        @Path("channel_id") channelId: String,
        @Body body: SendMessageRequest
    ): Response<Message>

    @GET("users/@me/channels")
    suspend fun getDmChannels(): Response<List<Channel>>
}

object DiscordApiFactory {

    private const val BASE_URL = "https://discord.com/api/v10/"

    fun create(token: String, isBot: Boolean): DiscordApiService {
        val authToken = when {
            !isBot -> token                                           // user account: raw token
            token.startsWith("Bot ") || token.startsWith("Bearer ") -> token
            else -> "Bot $token"
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", authToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "DiscordWearOS (WearOS, 1.0)")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiscordApiService::class.java)
    }
}
