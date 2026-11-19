package com.example.data.local

import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SajdaBackupPayload(
    val schemaVersion: Int = 1,
    val appId: String = "sajda_ai",
    val exportedAtMillis: Long = 0L,
    val settings: BackupSettings = BackupSettings(),
    val qazo: BackupQazo = BackupQazo(),
    val dailyProgress: List<BackupDailyDay> = emptyList()
)

@Serializable
data class BackupSettings(
    val genderId: String = Gender.MALE.id,
    val scriptId: String = TextScript.LATIN.id,
    val selectedCityId: String = "fargona",
    val preferOfflineMp3: Boolean = true,
    val voiceSpeed: Float = 1.0f,
    val darkRoomLuminanceThreshold: Float = 28f,
    val seatedPrayerMode: Boolean = false,
    val largeTextMode: Boolean = false,
    val lowPowerCameraMode: Boolean = false,
    val autoBoostPrayerVolume: Boolean = true,
    val acousticChimeEnabled: Boolean = true,
    val alarmMap: Map<String, Boolean> = emptyMap()
)

@Serializable
data class BackupQazo(
    val bomdod: Int = 0,
    val peshin: Int = 0,
    val asr: Int = 0,
    val shom: Int = 0,
    val xufton: Int = 0,
    val vitr: Int = 0,
    val updatedAtMillis: Long = 0L
)

@Serializable
data class BackupDailyDay(
    val dayKey: String,
    val bomdod: Boolean = false,
    val peshin: Boolean = false,
    val asr: Boolean = false,
    val shom: Boolean = false,
    val xufton: Boolean = false,
    val updatedAtMillis: Long = 0L
)

sealed interface BackupRestoreResult {
    data class Success(
        val daysRestored: Int,
        val qazoTotal: Int
    ) : BackupRestoreResult

    data class Failure(val reason: String) : BackupRestoreResult
}

/**
 * Offline JSON Backup & Restore manager using Storage Access Framework (no storage permissions needed).
 */
class SajdaBackupManager(
    private val prefsRepo: UserPreferencesRepository,
    private val historyRepo: PrayerHistoryRepository,
    private val qazoRepo: QazoPrayerRepository
) {
    companion object {
        const val MAX_BACKUP_BYTES = 2 * 1024 * 1024L // 2 MB
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToJson(nowMillis: Long = System.currentTimeMillis()): String {
        val settings = prefsRepo.settingsFlow.first()
        val qazo = qazoRepo.getSnapshot()
        val days = historyRepo.getAllDailyProgress()

        val payload = SajdaBackupPayload(
            schemaVersion = 1,
            appId = "sajda_ai",
            exportedAtMillis = nowMillis,
            settings = BackupSettings(
                genderId = settings.gender.id,
                scriptId = settings.script.id,
                selectedCityId = settings.selectedCityId,
                preferOfflineMp3 = settings.preferOfflineMp3,
                voiceSpeed = settings.voiceSpeed,
                darkRoomLuminanceThreshold = settings.darkRoomLuminanceThreshold,
                seatedPrayerMode = settings.seatedPrayerMode,
                largeTextMode = settings.largeTextMode,
                lowPowerCameraMode = settings.lowPowerCameraMode,
                autoBoostPrayerVolume = settings.autoBoostPrayerVolume,
                acousticChimeEnabled = settings.acousticChimeEnabled,
                alarmMap = settings.alarmMap.mapKeys { it.key.id }
            ),
            qazo = BackupQazo(
                bomdod = qazo.bomdodCount,
                peshin = qazo.peshinCount,
                asr = qazo.asrCount,
                shom = qazo.shomCount,
                xufton = qazo.xuftonCount,
                vitr = qazo.vitrCount,
                updatedAtMillis = qazo.updatedAtMillis
            ),
            dailyProgress = days.map { day ->
                BackupDailyDay(
                    dayKey = day.dayKey,
                    bomdod = day.bomdodCompleted,
                    peshin = day.peshinCompleted,
                    asr = day.asrCompleted,
                    shom = day.shomCompleted,
                    xufton = day.xuftonCompleted,
                    updatedAtMillis = day.updatedAtMillis
                )
            }
        )
        return json.encodeToString(payload)
    }

    suspend fun importFromJson(rawJson: String): BackupRestoreResult {
        if (rawJson.isBlank()) {
            return BackupRestoreResult.Failure("Fayl bo‘sh")
        }
        if (rawJson.length > MAX_BACKUP_BYTES) {
            return BackupRestoreResult.Failure("Zaxira fayli hajmi 2 MB dan oshmasligi kerak")
        }
        val payload = runCatching {
            json.decodeFromString<SajdaBackupPayload>(rawJson)
        }.getOrElse { err ->
            return BackupRestoreResult.Failure(err.message ?: "JSON formati noto‘g‘ri")
        }

        if (payload.appId != "sajda_ai" || payload.schemaVersion !in 1..5) {
            return BackupRestoreResult.Failure("Noto‘g‘ri zaxira fayli")
        }

        val validCity = UzbekistanCities.findById(payload.settings.selectedCityId).id
        val restoredAlarms = PrayerType.entries.associateWith { type ->
            payload.settings.alarmMap[type.id] ?: type.isPrayable
        }
        val restoredSettings = UserSettings(
            onboardingCompleted = true,
            gender = Gender.fromId(payload.settings.genderId),
            script = TextScript.fromId(payload.settings.scriptId),
            selectedCityId = validCity,
            preferOfflineMp3 = payload.settings.preferOfflineMp3,
            voiceSpeed = payload.settings.voiceSpeed.coerceIn(0.75f, 1.35f),
            darkRoomLuminanceThreshold = payload.settings.darkRoomLuminanceThreshold.coerceIn(12f, 55f),
            seatedPrayerMode = payload.settings.seatedPrayerMode,
            largeTextMode = payload.settings.largeTextMode,
            lowPowerCameraMode = payload.settings.lowPowerCameraMode,
            autoBoostPrayerVolume = payload.settings.autoBoostPrayerVolume,
            acousticChimeEnabled = payload.settings.acousticChimeEnabled,
            alarmMap = restoredAlarms
        )
        prefsRepo.restoreSettings(restoredSettings)

        val now = System.currentTimeMillis()
        val restoredQazo = QazoPrayerEntity(
            id = 1,
            bomdodCount = payload.qazo.bomdod.coerceAtLeast(0),
            peshinCount = payload.qazo.peshin.coerceAtLeast(0),
            asrCount = payload.qazo.asr.coerceAtLeast(0),
            shomCount = payload.qazo.shom.coerceAtLeast(0),
            xuftonCount = payload.qazo.xufton.coerceAtLeast(0),
            vitrCount = payload.qazo.vitr.coerceAtLeast(0),
            updatedAtMillis = if (payload.qazo.updatedAtMillis > 0L) payload.qazo.updatedAtMillis else now
        )
        qazoRepo.restoreSnapshot(restoredQazo)

        val validDayRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
        val restoredDays = payload.dailyProgress
            .filter { validDayRegex.matches(it.dayKey) }
            .map { day ->
                DailyPrayerProgressEntity(
                    dayKey = day.dayKey,
                    bomdodCompleted = day.bomdod,
                    peshinCompleted = day.peshin,
                    asrCompleted = day.asr,
                    shomCompleted = day.shom,
                    xuftonCompleted = day.xufton,
                    updatedAtMillis = if (day.updatedAtMillis > 0L) day.updatedAtMillis else now
                )
            }
        historyRepo.restoreDailyProgress(restoredDays)

        return BackupRestoreResult.Success(
            daysRestored = restoredDays.size,
            qazoTotal = restoredQazo.totalCount()
        )
    }
}
