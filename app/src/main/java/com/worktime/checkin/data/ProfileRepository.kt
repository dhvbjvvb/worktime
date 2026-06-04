package com.worktime.checkin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.nio.charset.Charset

data class ProfileSettings(
    val nickname: String = DEFAULT_NICKNAME,
    val avatarUri: String? = null
)

class ProfileRepository(context: Context) {
    private val dataStore = context.applicationContext.appDataStore

    val settings: Flow<ProfileSettings> = dataStore.data.map { preferences ->
        ProfileSettings(
            nickname = preferences[Keys.nickname].normalizedNickname(),
            avatarUri = preferences[Keys.avatarUri]
        )
    }

    suspend fun setNickname(nickname: String) {
        dataStore.edit { preferences ->
            preferences[Keys.nickname] = nickname.ifBlank { DEFAULT_NICKNAME }
        }
    }

    suspend fun setAvatarUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(Keys.avatarUri)
            } else {
                preferences[Keys.avatarUri] = uri
            }
        }
    }

    private object Keys {
        val nickname = stringPreferencesKey("profile_nickname")
        val avatarUri = stringPreferencesKey("profile_avatar_uri")
    }
}

private const val DEFAULT_NICKNAME = "用户昵称"

private fun String?.normalizedNickname(): String {
    return when (this) {
        null, "", legacyMojibake(DEFAULT_NICKNAME) -> DEFAULT_NICKNAME
        else -> this
    }
}

private val LegacyMojibakeCharset = Charset.forName("GB18030")

private fun legacyMojibake(text: String): String {
    return String(text.toByteArray(Charsets.UTF_8), LegacyMojibakeCharset)
}
