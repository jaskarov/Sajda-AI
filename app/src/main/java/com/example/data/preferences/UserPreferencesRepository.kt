package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.Gender
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sajda_prefs")

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val gender: Gender = Gender.MALE,
    val script: TextScript = TextScript.LATIN,
    val selectedCityId: String = "fargona",
    val preferOfflineMp3: Boolean = true,
    val voiceSpeed: Float = 1.0f,
    val darkRoomLuminanceThreshold: Float = 28f,
    val alarmMap: Map<PrayerType, Boolean> = PrayerType.entries.associateWith { it.isPrayable }
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val GENDER = stringPreferencesKey("gender")
        val SCRIPT = stringPreferencesKey("script")
        val CITY_ID = stringPreferencesKey("city_id")
        val PREFER_OFFLINE_MP3 = booleanPreferencesKey("prefer_offline_mp3")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val DARK_THRESHOLD = floatPreferencesKey("dark_threshold")
        fun alarmKey(type: PrayerType) = booleanPreferencesKey("alarm_${type.id}")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val alarms = PrayerType.entries.associateWith { type ->
            prefs[Keys.alarmKey(type)] ?: type.isPrayable
        }
        UserSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            gender = Gender.fromId(prefs[Keys.GENDER]),
            script = TextScript.fromId(prefs[Keys.SCRIPT]),
            selectedCityId = prefs[Keys.CITY_ID] ?: "fargona",
            preferOfflineMp3 = prefs[Keys.PREFER_OFFLINE_MP3] ?: true,
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
            darkRoomLuminanceThreshold = prefs[Keys.DARK_THRESHOLD] ?: 28f,
            alarmMap = alarms
        )
    }

    suspend fun completeOnboarding(gender: Gender, script: TextScript, cityId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GENDER] = gender.id
            prefs[Keys.SCRIPT] = script.id
            prefs[Keys.CITY_ID] = cityId
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setGender(gender: Gender) {
        context.dataStore.edit { it[Keys.GENDER] = gender.id }
    }

    suspend fun setScript(script: TextScript) {
        context.dataStore.edit { it[Keys.SCRIPT] = script.id }
    }

    suspend fun setCityId(cityId: String) {
        context.dataStore.edit { it[Keys.CITY_ID] = cityId }
    }

    suspend fun setPreferOfflineMp3(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PREFER_OFFLINE_MP3] = enabled }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.VOICE_SPEED] = speed.coerceIn(0.75f, 1.35f) }
    }

    suspend fun setDarkRoomThreshold(threshold: Float) {
        context.dataStore.edit { it[Keys.DARK_THRESHOLD] = threshold.coerceIn(12f, 60f) }
    }

    suspend fun setPrayerAlarmEnabled(type: PrayerType, enabled: Boolean) {
        context.dataStore.edit { it[Keys.alarmKey(type)] = enabled }
    }
}
