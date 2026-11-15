package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.PrayerWorkScheduler
import com.example.audio.AudioCoachStatus
import com.example.audio.PrayerAudioCoach
import com.example.data.local.DailyPrayerProgressEntity
import com.example.data.local.PrayerHistoryEntity
import com.example.data.local.PrayerHistoryRepository
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.PrayerScheduleSummary
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import com.example.domain.model.WeeklyPrayerAnalytics
import com.example.domain.model.WeeklyPrayerSummary
import com.example.sensor.QiblaCompassSensor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: UserSettings = UserSettings(),
    val schedule: PrayerScheduleSummary? = null,
    val todayProgress: DailyPrayerProgressEntity? = null,
    val dailyProgressHistory: List<DailyPrayerProgressEntity> = emptyList(),
    val weeklySummary: WeeklyPrayerSummary? = null,
    val history: List<PrayerHistoryEntity> = emptyList(),
    val compassAzimuth: Float = 0f,
    val compassAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasCompassSensor: Boolean = true,
    val audioStatus: AudioCoachStatus = AudioCoachStatus.Idle,
    val isReady: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = UserPreferencesRepository(application)
    private val historyRepo = SajdaDatabase.getInstance(application).let { db ->
        PrayerHistoryRepository(db.prayerHistoryDao(), db.dailyPrayerProgressDao())
    }
    private val getPrayerTimesUseCase = GetPrayerTimesUseCase()
    private val workScheduler = PrayerWorkScheduler(application)
    private val compassSensor = QiblaCompassSensor(application)
    private val audioCoach = PrayerAudioCoach(application)

    private val tickerFlow = MutableStateFlow(System.currentTimeMillis())
    private val weekOffsetFlow = MutableStateFlow(0)

    private val weeklySummaryFlow = weekOffsetFlow.flatMapLatest { offset ->
        val (startKey, endKey) = WeeklyPrayerAnalytics.weekBounds(weekOffset = offset)
        historyRepo.observeDaysInRange(startKey, endKey).map { records ->
            WeeklyPrayerAnalytics.buildSummary(
                records = records,
                weekOffset = offset
            )
        }
    }

    private val databaseFlows = combine(
        historyRepo.observeTodayProgress(),
        historyRepo.recentDailyProgress,
        historyRepo.recentHistory,
        weeklySummaryFlow
    ) { todayProgress, dailyHistory, sessionHistory, weeklySummary ->
        DatabaseBundle(
            todayProgress = todayProgress,
            dailyProgressHistory = dailyHistory,
            sessionHistory = sessionHistory,
            weeklySummary = weeklySummary
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        prefsRepo.settingsFlow,
        databaseFlows,
        tickerFlow,
        compassSensor.readingsFlow()
    ) { settings, dbBundle, nowMillis, compassReading ->
        val city = UzbekistanCities.findById(settings.selectedCityId)
        val schedule = getPrayerTimesUseCase.calculateForToday(
            city = city,
            completedToday = dbBundle.todayProgress.completedTypes(),
            alarmMap = settings.alarmMap,
            nowMillis = nowMillis
        )
        val declination = GeomagneticField(
            city.latitude.toFloat(),
            city.longitude.toFloat(),
            400f,
            nowMillis
        ).declination
        val trueAzimuth = ((compassReading.magneticAzimuthDegrees + declination) % 360f + 360f) % 360f

        MainUiState(
            settings = settings,
            schedule = schedule,
            todayProgress = dbBundle.todayProgress,
            dailyProgressHistory = dbBundle.dailyProgressHistory,
            weeklySummary = dbBundle.weeklySummary,
            history = dbBundle.sessionHistory,
            compassAzimuth = trueAzimuth,
            compassAccuracy = compassReading.accuracy,
            hasCompassSensor = compassReading.hasSensor,
            audioStatus = audioCoach.status.value,
            isReady = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            val initialSettings = prefsRepo.settingsFlow.first()
            workScheduler.syncAllPrayers(initialSettings)
        }
        viewModelScope.launch {
            while (true) {
                tickerFlow.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    fun shiftWeek(delta: Int) {
        val next = (weekOffsetFlow.value + delta).coerceAtMost(0)
        weekOffsetFlow.value = next
    }

    fun resetToCurrentWeek() {
        weekOffsetFlow.value = 0
    }

    fun completeOnboarding(gender: Gender, script: TextScript, cityId: String) {
        viewModelScope.launch {
            prefsRepo.completeOnboarding(gender, script, cityId)
            rescheduleAlarms()
        }
    }

    fun setGender(gender: Gender) {
        viewModelScope.launch { prefsRepo.setGender(gender) }
    }

    fun setScript(script: TextScript) {
        viewModelScope.launch {
            prefsRepo.setScript(script)
            rescheduleAlarms()
        }
    }

    fun setCityId(cityId: String) {
        viewModelScope.launch {
            prefsRepo.setCityId(cityId)
            rescheduleAlarms()
        }
    }

    fun setUseGeminiTts(enabled: Boolean) {
        if (enabled) {
            PrayerAudioCoach.clearSessionBlock()
        }
        viewModelScope.launch { prefsRepo.setUseGeminiTts(enabled) }
    }

    fun setVoiceSpeed(speed: Float) {
        viewModelScope.launch { prefsRepo.setVoiceSpeed(speed) }
    }

    fun setDarkRoomThreshold(threshold: Float) {
        viewModelScope.launch { prefsRepo.setDarkRoomThreshold(threshold) }
    }

    fun togglePrayerCompleted(type: PrayerType) {
        if (!type.isPrayable) return
        viewModelScope.launch {
            historyRepo.togglePrayerCompletedToday(type)
        }
    }

    fun togglePrayerCompletedForDay(dayKey: String, type: PrayerType) {
        if (!type.isPrayable) return
        viewModelScope.launch {
            historyRepo.togglePrayerCompletedForDay(dayKey, type)
        }
    }

    fun togglePrayerAlarm(type: PrayerType) {
        if (!type.isPrayable) return
        viewModelScope.launch {
            val current = uiState.value.settings.alarmMap[type] ?: type.isPrayable
            val next = !current
            prefsRepo.setPrayerAlarmEnabled(type, next)
            val updatedSettings = prefsRepo.settingsFlow.first()
            if (next) {
                workScheduler.scheduleNextOccurrence(type, updatedSettings)
            } else {
                workScheduler.cancelPrayer(type)
            }
        }
    }

    fun speakSample(text: String) {
        val settings = uiState.value.settings
        audioCoach.speak(
            text = text,
            gender = settings.gender,
            useGeminiTts = settings.useGeminiTts,
            speed = settings.voiceSpeed
        )
    }

    fun stopSampleAudio() {
        audioCoach.stop()
    }

    @SuppressLint("MissingPermission")
    fun detectNearestCityFromCoarseLocation(): String? {
        val lm = getApplication<Application>()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        val location = providers.firstNotNullOfOrNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        } ?: return null

        val nearest = UzbekistanCities.findNearest(location.latitude, location.longitude)
        setCityId(nearest.id)
        return nearest.name.get(uiState.value.settings.script)
    }

    private suspend fun rescheduleAlarms() {
        val latestSettings = prefsRepo.settingsFlow.first()
        workScheduler.syncAllPrayers(latestSettings)
    }

    override fun onCleared() {
        super.onCleared()
        audioCoach.release()
    }

    private data class DatabaseBundle(
        val todayProgress: DailyPrayerProgressEntity,
        val dailyProgressHistory: List<DailyPrayerProgressEntity>,
        val sessionHistory: List<PrayerHistoryEntity>,
        val weeklySummary: WeeklyPrayerSummary
    )
}
