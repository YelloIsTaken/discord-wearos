package com.yelloistaken.discordwearos.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "discord_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private const val ENCRYPTED_PREFS_FILE = "discord_token_prefs"
        private const val TOKEN_KEY_SP = "discord_token"
        private const val IS_BOT_KEY_SP = "is_bot_token"
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val tokenFlow: Flow<String?> = flow {
        emit(withContext(Dispatchers.IO) { encryptedPrefs.getString(TOKEN_KEY_SP, null) })
    }

    val isBotFlow: Flow<Boolean> = flow {
        emit(withContext(Dispatchers.IO) {
            try { encryptedPrefs.getBoolean(IS_BOT_KEY_SP, false) } catch (e: Exception) { false }
        })
    }

    val usernameFlow: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[USERNAME_KEY] }

    suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(TOKEN_KEY_SP, token).apply()
    }

    suspend fun saveIsBot(isBot: Boolean) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putBoolean(IS_BOT_KEY_SP, isBot).apply()
    }

    suspend fun saveUserInfo(userId: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
            prefs[USERNAME_KEY] = username
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().clear().apply()
        context.dataStore.edit { it.clear() }
    }

    suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        try { encryptedPrefs.getString(TOKEN_KEY_SP, null) } catch (e: Exception) { null }
    }

    suspend fun getIsBot(): Boolean = withContext(Dispatchers.IO) {
        try { encryptedPrefs.getBoolean(IS_BOT_KEY_SP, false) } catch (e: Exception) { false }
    }
}
