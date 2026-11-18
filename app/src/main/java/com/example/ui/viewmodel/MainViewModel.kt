package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.PrayerWorkScheduler
import com.example.audio.AudioCoachStatus
import com.example.audio.PrayerAudioCoach
import com.example.data.local.BackupRestoreResult
import com.example.data.local.DailyPrayerProgressEntity
import com.example.data.local.PrayerCalendarDayEntity
import com.example.data.local.PrayerCalendarRepository
import com.example.data.local.PrayerHistoryEntity
import com.example.data.local.PrayerHistoryRepository
import com.example.data.local.QazoPrayerEntity
import com.example.data.local.QazoPrayerRepository
import com.example.data.local.SajdaBackupManager
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.PrayerScheduleSummary
import com.example.domain.model.PrayerTrack
import com.example.domain.model.PrayerType
import com.example.domain.model.QazoPrayerType
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompassUiState(
    val compassAzimuth: Float = 0f,
    val compassAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasCompassSensor: Boolean = true,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val tiltDegrees: Float = 0f,
    val isLevel: Boolean = true
)

data class MainUiState(
    val settings: UserSettings = UserSettings(),
    val schedule: PrayerScheduleSummary? = null,
    val quickTracks: List<PrayerTrack> = emptyList(),
    val todayProgress: DailyPrayerProgressEntity? = null,
    val dailyProgressHistory: List<DailyPrayerProgressEntity> = emptyList(),
    val weeklySummary: WeeklyPrayerSummary? = null,
    val qazoProgress: QazoPrayerEntity = QazoPrayerEntity(),
    val history: List<PrayerHistoryEntity> = emptyList(),
    val compassAzimuth: Float = 0f,
    val compassAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasCompassSensor: Boolean = true,
    val audioStatus: AudioCoachStatus = AudioCoachStatus.Idle,
    val ttsReadiness: com.example.audio.TtsReadinessInfo = com.example.audio.TtsReadinessInfo(),
    val calendarSource: String = "uz_taqvim_365",
    val lastCalendarSyncMillis: Long = 0L,
    val isCalendarSyncing: Boolean = false,
    val backupStatusMessage: com.example.domain.model.LText? = null,
    val isReady: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SajdaDatabase.getInstance(application)
    private val prefsRepo = UserPreferencesRepository(application)
    private val historyRepo = PrayerHistoryRepository(
        database.prayerHistoryDao(),
        database.dailyPrayerProgressDao()
    )
    private val calendarRepo = PrayerCalendarRepository(
        application,
        database.prayerCalendarDao()
    )
    private val qazoRepo = QazoPrayerRepository(database.qazoPrayerDao())
    private val backupManager = SajdaBackupManager(prefsRepo, historyRepo, qazoRepo)
    private val getPrayerTimesUseCase = GetPrayerTimesUseCase()
    private val workScheduler = PrayerWorkScheduler(application)
    private val compassSensor = QiblaCompassSensor(application)
    private val audioCoach = PrayerAudioCoach(application)

    private val tickerFlow = MutableStateFlow(System.currentTimeMillis())
    private val weekOffsetFlow = MutableStateFlow(0)
    private val calendarSyncingFlow = MutableStateFlow(false)
    private val backupStatusMessageFlow = MutableStateFlow<com.example.domain.model.LText?>(null)

    private val weeklySummaryFlow = weekOffsetFlow.flatMapLatest { offset ->
        val (startKey, endKey) = WeeklyPrayerAnalytics.weekBounds(weekOffset = offset)
        historyRepo.observeDaysInRange(startKey, endKey).map { records ->
            WeeklyPrayerAnalytics.buildSummary(
                records = records,
                weekOffset = offset
            )
        }
    }

    private val calendarPairFlow = prefsRepo.settingsFlow
        .map { it.selectedCityId }
        .distinctUntilChanged()
        .flatMapLatest { cityId ->
            combine(
                calendarRepo.observeTodayAndTomorrow(cityId),
                calendarRepo.observeLatestSyncMillis(cityId),
                calendarSyncingFlow
            ) { pair, latestSync, syncing ->
                CalendarBundle(
                    todayDay = pair.first,
                    tomorrowDay = pair.second,
                    latestSyncMillis = latestSync ?: 0L,
                    isSyncing = syncing
                )
            }
        }

    private val currentDayKeyFlow = tickerFlow
        .map { PrayerHistoryRepository.formatDayKey(java.util.Date(it)) }
        .distinctUntilChanged()

    private val progressAndStatsFlow = combine(
        historyRepo.observeTodayProgress(currentDayKeyFlow),
        historyRepo.recentDailyProgress,
        historyRepo.recentHistory,
        weeklySummaryFlow,
        qazoRepo.qazoFlow
    ) { todayProgress, dailyHistory, sessionHistory, weeklySummary, qazoProgress ->
        StatsBundle(
            todayProgress = todayProgress,
            dailyProgressHistory = dailyHistory,
            sessionHistory = sessionHistory,
            weeklySummary = weeklySummary,
            qazoProgress = qazoProgress
        )
    }

    private val databaseFlows = combine(
        progressAndStatsFlow,
        calendarPairFlow
    ) { stats, calendarBundle ->
        DatabaseBundle(
            todayProgress = stats.todayProgress,
            dailyProgressHistory = stats.dailyProgressHistory,
            sessionHistory = stats.sessionHistory,
            weeklySummary = stats.weeklySummary,
            qazoProgress = stats.qazoProgress,
            calendarBundle = calendarBundle
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        prefsRepo.settingsFlow,
        databaseFlows,
        tickerFlow,
        backupStatusMessageFlow,
        audioCoach.ttsReadiness
    ) { settings, dbBundle, nowMillis, backupMsg, ttsReadiness ->
        val city = UzbekistanCities.findById(settings.selectedCityId)
        val schedule = getPrayerTimesUseCase.calculateForToday(
            city = city,
            completedToday = dbBundle.todayProgress.completedTypes(),
            alarmMap = settings.alarmMap,
            nowMillis = nowMillis,
            todayCalendarDay = dbBundle.calendarBundle.todayDay,
            tomorrowCalendarDay = dbBundle.calendarBundle.tomorrowDay
        )

        MainUiState(
            settings = settings,
            schedule = schedule,
            quickTracks = PrayerTrack.tracksForPrayer(schedule.currentPrayer),
            todayProgress = dbBundle.todayProgress,
            dailyProgressHistory = dbBundle.dailyProgressHistory,
            weeklySummary = dbBundle.weeklySummary,
            qazoProgress = dbBundle.qazoProgress,
            history = dbBundle.sessionHistory,
            compassAzimuth = 0f,
            compassAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
            hasCompassSensor = true,
            audioStatus = audioCoach.status.value,
            ttsReadiness = ttsReadiness,
            calendarSource = dbBundle.calendarBundle.todayDay?.source ?: "uz_taqvim_365",
            lastCalendarSyncMillis = dbBundle.calendarBundle.latestSyncMillis,
            isCalendarSyncing = dbBundle.calendarBundle.isSyncing,
            backupStatusMessage = backupMsg,
            isReady = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainUiState()
    )

    val compassUiState: StateFlow<CompassUiState> = prefsRepo.settingsFlow
        .map { it.selectedCityId }
        .distinctUntilChanged()
        .flatMapLatest { cityId ->
            val city = UzbekistanCities.findById(cityId)
            val declination = GeomagneticField(
                city.latitude.toFloat(),
                city.longitude.toFloat(),
                400f,
                System.currentTimeMillis()
            ).declination
            compassSensor.readingsFlow().map { reading ->
                val trueAzimuth = ((reading.magneticAzimuthDegrees + declination) % 360f + 360f) % 360f
                CompassUiState(
                    compassAzimuth = trueAzimuth,
                    compassAccuracy = reading.accuracy,
                    hasCompassSensor = reading.hasSensor,
                    tiltX = reading.tiltX,
                    tiltY = reading.tiltY,
                    tiltDegrees = reading.tiltDegrees,
                    isLevel = reading.isLevel
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CompassUiState()
        )

    init {
        viewModelScope.launch {
            calendarRepo.ensureSeededFromAssets()
            calendarRepo.ensureYearSeededOffline(2027)
            val nowYear = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tashkent"))
                .get(java.util.Calendar.YEAR)
            if (nowYear >= 2027) {
                calendarRepo.ensureYearSeededOffline(nowYear)
                calendarRepo.ensureYearSeededOffline(nowYear + 1)
            }
            val initialSettings = prefsRepo.settingsFlow.first()
            calendarRepo.syncFromNetworkIfConnected(initialSettings.selectedCityId)
            rescheduleAlarms()
        }
        viewModelScope.launch {
            while (true) {
                tickerFlow.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    fun syncPrayerCalendarNow() {
        viewModelScope.launch {
            if (calendarSyncingFlow.value) return@launch
            calendarSyncingFlow.value = true
            try {
                val settings = prefsRepo.settingsFlow.first()
                calendarRepo.syncFromNetworkIfConnected(settings.selectedCityId)
                rescheduleAlarms()
            } finally {
                calendarSyncingFlow.value = false
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val jsonText = backupManager.exportToJson()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonText.toByteArray(Charsets.UTF_8))
                } ?: error("Faylga yozib bo‘lmadi")
            }.onSuccess {
                backupStatusMessageFlow.value = com.example.domain.model.lText(
                    "Zaxira nusxa muvaffaqiyatli saqlandi",
                    "Захира нусха муваффақиятли сақланди"
                )
            }.onFailure { err ->
                backupStatusMessageFlow.value = com.example.domain.model.lText(
                    "Saqlashda xatolik: ${err.localizedMessage ?: "noma'lum"}",
                    "Сақлашда хатолик: ${err.localizedMessage ?: "номаълум"}"
                )
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = runCatching {
                val raw = getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                backupManager.importFromJson(raw)
            }.getOrElse { err ->
                BackupRestoreResult.Failure(err.localizedMessage ?: "O‘qib bo‘lmadi")
            }

            when (result) {
                is BackupRestoreResult.Success -> {
                    rescheduleAlarms()
                    backupStatusMessageFlow.value = com.example.domain.model.lText(
                        "Tiklandi: ${result.daysRestored} kun tarix, ${result.qazoTotal} ta qazo",
                        "Тикланди: ${result.daysRestored} кун тарих, ${result.qazoTotal} та қазо"
                    )
                }
                is BackupRestoreResult.Failure -> {
                    backupStatusMessageFlow.value = com.example.domain.model.lText(
                        "Tiklash xatosi: ${result.reason}",
                        "Тиклаш хатоси: ${result.reason}"
                    )
                }
            }
        }
    }

    fun clearBackupMessage() {
        backupStatusMessageFlow.value = null
    }

    fun shiftWeek(delta: Int) {
        val next = (weekOffsetFlow.value + delta).coerceAtMost(0)
        weekOffsetFlow.value = next
    }

    fun resetToCurrentWeek() {
        weekOffsetFlow.value = 0
    }

    fun completeOnboarding(
        gender: Gender,
        script: TextScript,
        cityId: String,
        seatedPrayerMode: Boolean = false,
        largeTextMode: Boolean = false
    ) {
        viewModelScope.launch {
            prefsRepo.completeOnboarding(gender, script, cityId, seatedPrayerMode, largeTextMode)
            calendarRepo.syncFromNetworkIfConnected(cityId)
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
            calendarRepo.syncFromNetworkIfConnected(cityId)
            rescheduleAlarms()
        }
    }

    fun setPreferOfflineMp3(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setPreferOfflineMp3(enabled) }
    }

    fun setVoiceSpeed(speed: Float) {
        viewModelScope.launch { prefsRepo.setVoiceSpeed(speed) }
    }

    fun setDarkRoomThreshold(threshold: Float) {
        viewModelScope.launch { prefsRepo.setDarkRoomThreshold(threshold) }
    }

    fun setSeatedPrayerMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSeatedPrayerMode(enabled) }
    }

    fun setLargeTextMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setLargeTextMode(enabled) }
    }

    fun setLowPowerCameraMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setLowPowerCameraMode(enabled) }
    }

    fun incrementQazo(type: QazoPrayerType) {
        viewModelScope.launch { qazoRepo.increment(type) }
    }

    fun decrementQazo(type: QazoPrayerType) {
        viewModelScope.launch { qazoRepo.decrement(type) }
    }

    fun addQazoYears(years: Int) {
        viewModelScope.launch { qazoRepo.addBulkForYears(years) }
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
                val (todayDay, tomorrowDay) = calendarRepo.getTodayAndTomorrow(updatedSettings.selectedCityId)
                workScheduler.scheduleNextOccurrence(
                    prayerType = type,
                    settings = updatedSettings,
                    todayCalendarDay = todayDay,
                    tomorrowCalendarDay = tomorrowDay
                )
            } else {
                workScheduler.cancelPrayer(type)
            }
        }
    }

    fun speakSample(text: String, audioAssetId: String = "") {
        val settings = uiState.value.settings
        audioCoach.speak(
            text = text,
            gender = settings.gender,
            preferOfflineMp3 = settings.preferOfflineMp3,
            speed = settings.voiceSpeed,
            audioAssetId = audioAssetId
        )
    }

    fun stopSampleAudio() {
        audioCoach.stop()
    }

    @SuppressLint("MissingPermission")
    fun detectNearestCityFromCoarseLocation(): String? {
        val lm = getApplication<Application>()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            providers.add(LocationManager.FUSED_PROVIDER)
        }
        providers.addAll(listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ))

        var location: Location? = providers.firstNotNullOfOrNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }

        // If no cached location is available, actively request single location with 1.8s timeout
        if (location == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val latch = java.util.concurrent.CountDownLatch(1)
            val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
            try {
                val activeProvider = if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else null

                if (activeProvider != null) {
                    lm.getCurrentLocation(activeProvider, null, executor) { loc ->
                        if (loc != null) {
                            location = loc
                        }
                        latch.countDown()
                    }
                    latch.await(1800, java.util.concurrent.TimeUnit.MILLISECONDS)
                }
            } catch (_: Exception) {
            } finally {
                executor.shutdown()
            }
        }

        val loc = location ?: return null
        val nearest = UzbekistanCities.findNearest(loc.latitude, loc.longitude)
        setCityId(nearest.id)
        return nearest.name.get(uiState.value.settings.script)
    }

    private suspend fun rescheduleAlarms() {
        val latestSettings = prefsRepo.settingsFlow.first()
        val (todayDay, tomorrowDay) = calendarRepo.getTodayAndTomorrow(latestSettings.selectedCityId)
        workScheduler.syncAllPrayers(
            settings = latestSettings,
            todayCalendarDay = todayDay,
            tomorrowCalendarDay = tomorrowDay
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioCoach.release()
    }

    private data class CalendarBundle(
        val todayDay: PrayerCalendarDayEntity?,
        val tomorrowDay: PrayerCalendarDayEntity?,
        val latestSyncMillis: Long,
        val isSyncing: Boolean
    )

    private data class StatsBundle(
        val todayProgress: DailyPrayerProgressEntity,
        val dailyProgressHistory: List<DailyPrayerProgressEntity>,
        val sessionHistory: List<PrayerHistoryEntity>,
        val weeklySummary: WeeklyPrayerSummary,
        val qazoProgress: QazoPrayerEntity
    )

    private data class DatabaseBundle(
        val todayProgress: DailyPrayerProgressEntity,
        val dailyProgressHistory: List<DailyPrayerProgressEntity>,
        val sessionHistory: List<PrayerHistoryEntity>,
        val weeklySummary: WeeklyPrayerSummary,
        val qazoProgress: QazoPrayerEntity,
        val calendarBundle: CalendarBundle
    )
}
