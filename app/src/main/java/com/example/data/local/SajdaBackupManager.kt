package com.example.data.local

import androidx.room.withTransaction
import com.example.data.preferences.ReminderSoundMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SajdaBackupPayload(
    val schemaVersion: Int = 2,
    val appId: String = "sajda_ai",
    val exportedAtMillis: Long = 0L,
    val settings: BackupSettings = BackupSettings(),
    val qazo: BackupQazo = BackupQazo(),
    val dailyProgress: List<BackupDailyDay> = emptyList(),
    val sessionHistory: List<BackupPrayerSession> = emptyList()
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
    val oledNightMode: Boolean = false,
    val reminderSoundModeId: String = ReminderSoundMode.TAKBIR.id,
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

@Serializable
data class BackupPrayerSession(
    val prayerTypeId: String,
    val trackId: String,
    val rakatCount: Int,
    val coachMode: String,
    val cityId: String,
    val dayKey: String,
    val durationSeconds: Int,
    val completedAtMillis: Long
)

sealed interface BackupRestoreResult {
    data class Success(
        val daysRestored: Int,
        val qazoTotal: Int,
        val sessionsRestored: Int = 0
    ) : BackupRestoreResult

    data class Failure(val reason: String) : BackupRestoreResult
}

/**
 * Offline JSON Backup & Restore manager using Storage Access Framework (no storage permissions needed).
 * Enforces a strict 2 MB payload cap and atomic Room transaction restore.
 */
class SajdaBackupManager(
    private val prefsRepo: UserPreferencesRepository,
    private val historyRepo: PrayerHistoryRepository,
    private val qazoRepo: QazoPrayerRepository,
    private val database: SajdaDatabase? = null
) {
    companion object {
        const val MAX_BACKUP_BYTES: Long = 2 * 1024 * 1024L // 2 MB
        const val MAX_DAILY_PROGRESS_RECORDS: Int = 5_000
        const val MAX_SESSION_HISTORY_RECORDS: Int = 500

        /**
         * Reads UTF-8 text from [inputStream] safely in 8 KB chunks up to [maxBytes],
         * throwing [IllegalArgumentException] immediately if the stream exceeds the cap
         * (prevents OOM / DoS on malformed or oversized files).
         */
        fun readBoundedUtf8(
            inputStream: InputStream,
            maxBytes: Long = MAX_BACKUP_BYTES
        ): String {
            val buffer = ByteArray(8 * 1024)
            val out = ByteArrayOutputStream()
            var total = 0L
            while (true) {
                val read = inputStream.read(buffer)
                if (read == -1) break
                total += read
                if (total > maxBytes) {
                    throw IllegalArgumentException("Zaxira fayli hajmi 2 MB dan oshmasligi kerak")
                }
                out.write(buffer, 0, read)
            }
            return out.toString(Charsets.UTF_8.name())
        }
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
        val sessions = historyRepo.getAllSessionHistory()

        val payload = SajdaBackupPayload(
            schemaVersion = 2,
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
                oledNightMode = settings.oledNightMode,
                reminderSoundModeId = settings.reminderSoundMode.id,
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
            },
            sessionHistory = sessions.take(MAX_SESSION_HISTORY_RECORDS).map { s ->
                BackupPrayerSession(
                    prayerTypeId = s.prayerTypeId,
                    trackId = s.trackId,
                    rakatCount = s.rakatCount,
                    coachMode = s.coachMode,
                    cityId = s.cityId,
                    dayKey = s.dayKey,
                    durationSeconds = s.durationSeconds,
                    completedAtMillis = s.completedAtMillis
                )
            }
        )
        return json.encodeToString(payload)
    }

    suspend fun importFromJson(rawJson: String): BackupRestoreResult {
        if (rawJson.isBlank()) {
            return BackupRestoreResult.Failure("Fayl bo‘sh")
        }
        if (rawJson.toByteArray(Charsets.UTF_8).size.toLong() > MAX_BACKUP_BYTES) {
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
            oledNightMode = payload.settings.oledNightMode,
            reminderSoundMode = ReminderSoundMode.fromId(payload.settings.reminderSoundModeId),
            alarmMap = restoredAlarms
        )

        val now = System.currentTimeMillis()
        val restoredQazo = QazoPrayerEntity(
            id = 1,
            bomdodCount = payload.qazo.bomdod.coerceIn(0, 100_000),
            peshinCount = payload.qazo.peshin.coerceIn(0, 100_000),
            asrCount = payload.qazo.asr.coerceIn(0, 100_000),
            shomCount = payload.qazo.shom.coerceIn(0, 100_000),
            xuftonCount = payload.qazo.xufton.coerceIn(0, 100_000),
            vitrCount = payload.qazo.vitr.coerceIn(0, 100_000),
            updatedAtMillis = if (payload.qazo.updatedAtMillis > 0L) payload.qazo.updatedAtMillis else now
        )

        val validDayRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
        val restoredDays = payload.dailyProgress
            .asSequence()
            .filter { validDayRegex.matches(it.dayKey) }
            .take(MAX_DAILY_PROGRESS_RECORDS)
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
            .toList()

        val restoredSessions = payload.sessionHistory
            .asSequence()
            .filter { validDayRegex.matches(it.dayKey) && it.rakatCount in 1..20 }
            .take(MAX_SESSION_HISTORY_RECORDS)
            .map { s ->
                PrayerHistoryEntity(
                    prayerTypeId = s.prayerTypeId,
                    trackId = s.trackId,
                    rakatCount = s.rakatCount,
                    coachMode = s.coachMode,
                    cityId = UzbekistanCities.findById(s.cityId).id,
                    dayKey = s.dayKey,
                    durationSeconds = s.durationSeconds.coerceIn(0, 7200),
                    completedAtMillis = if (s.completedAtMillis > 0L) s.completedAtMillis else now
                )
            }
            .toList()

        // Apply Room writes atomically inside a transaction when database handle is provided
        if (database != null) {
            database.withTransaction {
                qazoRepo.restoreSnapshot(restoredQazo)
                historyRepo.restoreDailyProgress(restoredDays)
                historyRepo.restoreSessionHistory(restoredSessions)
            }
        } else {
            qazoRepo.restoreSnapshot(restoredQazo)
            historyRepo.restoreDailyProgress(restoredDays)
            historyRepo.restoreSessionHistory(restoredSessions)
        }
        prefsRepo.restoreSettings(restoredSettings)

        return BackupRestoreResult.Success(
            daysRestored = restoredDays.size,
            qazoTotal = restoredQazo.totalCount(),
            sessionsRestored = restoredSessions.size
        )
    }
}
