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
import com.example.data.local.PrayerSyncMetadataEntity
import com.example.data.local.PrayerSyncOutcome
import com.example.data.local.QazoPrayerEntity
import com.example.data.local.QazoPrayerRepository
import com.example.data.local.SajdaBackupManager
import com.example.data.local.SajdaDatabase
import com.example.data.remote.RemoteUzbekistanPrayerTimesService
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
    val cachedCalendarDaysCount: Int = 0,
    val monthlyCalendarDays: List<PrayerCalendarDayEntity> = emptyList(),
    val isCalendarSyncing: Boolean = false,
    val calendarSyncStatusMessage: com.example.domain.model.LText? = null,
    val backupStatusMessage: com.example.domain.model.LText? = null,
    val tahoratAutoPlayActive: Boolean = false,
    val activeTahoratStepId: String? = null,
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
        context = application,
        dao = database.prayerCalendarDao(),
        syncMetadataDao = database.prayerSyncMetadataDao()
    )
    private val qazoRepo = QazoPrayerRepository(database.qazoPrayerDao())
    private val backupManager = SajdaBackupManager(prefsRepo, historyRepo, qazoRepo, database)
    private val getPrayerTimesUseCase = GetPrayerTimesUseCase()
    private val workScheduler = PrayerWorkScheduler(application)
    private val compassSensor = QiblaCompassSensor(application)
    private var _audioCoach: PrayerAudioCoach? = null
    private val ttsReadinessFlow = MutableStateFlow(com.example.audio.TtsReadinessInfo())

    private fun getOrCreateAudioCoach(): PrayerAudioCoach {
        return _audioCoach ?: PrayerAudioCoach(getApplication()).also { coach ->
            _audioCoach = coach
            viewModelScope.launch {
                coach.ttsReadiness.collect { readiness ->
                    ttsReadinessFlow.value = readiness
                }
            }
        }
    }

    // Minute-aligned clock tick (prevents waking up MainUiState and recomposing all screens every 1s)
    private val scheduleTickFlow = MutableStateFlow(System.currentTimeMillis())
    private val weekOffsetFlow = MutableStateFlow(0)
    private val calendarSyncingFlow = MutableStateFlow(false)
    private val calendarSyncStatusFlow = MutableStateFlow<com.example.domain.model.LText?>(null)
    private val backupStatusMessageFlow = MutableStateFlow<com.example.domain.model.LText?>(null)
    private val tahoratAutoPlayFlow = MutableStateFlow<Pair<Boolean, String?>>(false to null)
    private var tahoratAutoPlayJob: kotlinx.coroutines.Job? = null

    private val weeklySummaryFlow = weekOffsetFlow.flatMapLatest { offset ->
        val (startKey, endKey) = WeeklyPrayerAnalytics.weekBounds(weekOffset = offset)
        historyRepo.observeDaysInRange(startKey, endKey).map { records ->
            WeeklyPrayerAnalytics.buildSummary(
                records = records,
                weekOffset = offset
            )
        }
    }

    private val currentDayKeyFlow = scheduleTickFlow
        .map { PrayerHistoryRepository.formatDayKey(java.util.Date(it)) }
        .distinctUntilChanged()

    private val calendarPairFlow = combine(
        prefsRepo.settingsFlow.map { it.selectedCityId }.distinctUntilChanged(),
        currentDayKeyFlow
    ) { cityId, dayKey -> cityId to dayKey }
        .flatMapLatest { (cityId, _) ->
            val nowMs = System.currentTimeMillis()
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tashkent")).apply {
                timeInMillis = nowMs
            }
            val year = cal.get(java.util.Calendar.YEAR)
            val month = cal.get(java.util.Calendar.MONTH) + 1

            val syncMetaFlow = combine(
                calendarRepo.observeLatestSyncMillis(cityId),
                calendarRepo.observeSyncMetadata(cityId),
                calendarRepo.observeCachedDayCount(cityId),
                calendarSyncingFlow,
                calendarSyncStatusFlow
            ) { latestSync, metadata, cachedCount, syncing, statusMsg ->
                SyncMetaBundle(
                    latestSyncMillis = maxOf(latestSync ?: 0L, metadata?.lastSyncedAtMillis ?: 0L),
                    metadata = metadata,
                    cachedDaysCount = cachedCount,
                    isSyncing = syncing,
                    statusMessage = statusMsg
                )
            }

            combine(
                calendarRepo.observeTodayAndTomorrow(cityId, nowMs),
                calendarRepo.observeMonthSchedule(cityId, year, month),
                syncMetaFlow
            ) { pair, monthDays, meta ->
                CalendarBundle(
                    todayDay = pair.first,
                    tomorrowDay = pair.second,
                    monthlyDays = monthDays,
                    latestSyncMillis = meta.latestSyncMillis,
                    cachedDaysCount = meta.cachedDaysCount,
                    metadata = meta.metadata,
                    isSyncing = meta.isSyncing,
                    syncStatusMessage = meta.statusMessage
                )
            }
        }

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

    private val auxFlow = combine(
        backupStatusMessageFlow,
        ttsReadinessFlow,
        tahoratAutoPlayFlow
    ) { backupMsg, ttsReadiness, tahoratPair ->
        Triple(backupMsg, ttsReadiness, tahoratPair)
    }

    val uiState: StateFlow<MainUiState> = combine(
        prefsRepo.settingsFlow,
        databaseFlows,
        scheduleTickFlow,
        auxFlow
    ) { settings, dbBundle, nowMillis, aux ->
        val (backupMsg, ttsReadiness, tahoratPair) = aux
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
            audioStatus = _audioCoach?.status?.value ?: AudioCoachStatus.Idle,
            ttsReadiness = ttsReadiness,
            calendarSource = dbBundle.calendarBundle.todayDay?.source
                ?: dbBundle.calendarBundle.metadata?.provider
                ?: "uz_taqvim_365",
            lastCalendarSyncMillis = dbBundle.calendarBundle.latestSyncMillis,
            cachedCalendarDaysCount = dbBundle.calendarBundle.cachedDaysCount,
            monthlyCalendarDays = dbBundle.calendarBundle.monthlyDays,
            isCalendarSyncing = dbBundle.calendarBundle.isSyncing,
            calendarSyncStatusMessage = dbBundle.calendarBundle.syncStatusMessage,
            backupStatusMessage = backupMsg,
            tahoratAutoPlayActive = tahoratPair.first,
            activeTahoratStepId = tahoratPair.second,
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
            val initialSettings = prefsRepo.settingsFlow.first()
            val nowYear = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tashkent"))
                .get(java.util.Calendar.YEAR)

            // 1. Active-city-first fast seed (365 rows, ~15ms) so HomeScreen renders immediately
            calendarRepo.ensureCitySeededFast(initialSettings.selectedCityId, nowYear)
            rescheduleAlarms()

            // 2. Background network sync & deferred full-country seeding (never blocks cold start)
            launch(kotlinx.coroutines.Dispatchers.IO) {
                calendarRepo.syncFromNetworkIfConnected(initialSettings.selectedCityId)
                delay(1500L)
                calendarRepo.ensureSeededFromAssets()
                calendarRepo.ensureYearSeededOffline(2027)
                if (nowYear >= 2027) {
                    calendarRepo.ensureYearSeededOffline(nowYear)
                    calendarRepo.ensureYearSeededOffline(nowYear + 1)
                }
            }
        }
        // Minute-aligned schedule refresh (handles prayer transitions & midnight rollover without 1Hz UI churn)
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                scheduleTickFlow.value = now
                val msUntilNextMinute = (60_000L - (now % 60_000L)).coerceIn(1_000L, 60_000L)
                delay(msUntilNextMinute)
            }
        }
    }

    fun syncPrayerCalendarNow() {
        viewModelScope.launch {
            if (calendarSyncingFlow.value) return@launch
            calendarSyncingFlow.value = true
            calendarSyncStatusFlow.value = null
            try {
                val settings = prefsRepo.settingsFlow.first()
                val outcome = calendarRepo.syncCityPrayerTimes(settings.selectedCityId)
                calendarSyncStatusFlow.value = when (outcome) {
                    is PrayerSyncOutcome.Synced -> {
                        val providerLabel = when (outcome.provider) {
                            RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API -> "Islom.uz"
                            RemoteUzbekistanPrayerTimesService.PROVIDER_ALADHAN -> "Aladhan"
                            else -> outcome.provider
                        }
                        com.example.domain.model.lText(
                            "Yangilandi ($providerLabel): ${outcome.fetchedDaysCount} kun yuklandi • ${outcome.totalCachedDays} kun oflayn Room bazasida",
                            "Янгиланди ($providerLabel): ${outcome.fetchedDaysCount} кун юкланди • ${outcome.totalCachedDays} кун офлайн Room базасида"
                        )
                    }
                    is PrayerSyncOutcome.OfflineCacheReady -> {
                        com.example.domain.model.lText(
                            "Oflayn rejim faol — ${outcome.totalCachedDays} kunlik jadval mahalliy Room bazasidan ishlamoqda",
                            "Офлайн режим фаол — ${outcome.totalCachedDays} кунлик жадвал маҳаллий Room базасидан ишламоқда"
                        )
                    }
                }
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
                val resolver = getApplication<Application>().contentResolver
                val pfd = runCatching { resolver.openFileDescriptor(uri, "r") }.getOrNull()
                val size = pfd?.use { it.statSize } ?: -1L
                if (size > SajdaBackupManager.MAX_BACKUP_BYTES) {
                    return@launch backupStatusMessageFlow.let {
                        it.value = com.example.domain.model.lText(
                            "Tiklash xatosi: Zaxira fayli hajmi 2 MB dan oshmasligi kerak",
                            "Тиклаш хатоси: Захира файли ҳажми 2 МБ дан ошмаслиги керак"
                        )
                    }
                }
                val raw = resolver.openInputStream(uri)?.use { stream ->
                    SajdaBackupManager.readBoundedUtf8(stream)
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

    fun setAutoBoostPrayerVolume(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoBoostPrayerVolume(enabled) }
    }

    fun setAcousticChimeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAcousticChimeEnabled(enabled) }
    }

    fun setOledNightMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setOledNightMode(enabled) }
    }

    fun setReminderSoundMode(mode: com.example.data.preferences.ReminderSoundMode) {
        viewModelScope.launch {
            prefsRepo.setReminderSoundMode(mode)
            rescheduleAlarms()
        }
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
        stopTahoratAutoPlay()
        val settings = uiState.value.settings
        getOrCreateAudioCoach().speak(
            text = text,
            gender = settings.gender,
            preferOfflineMp3 = settings.preferOfflineMp3,
            speed = settings.voiceSpeed,
            audioAssetId = audioAssetId
        )
    }

    fun startTahoratAutoPlay(steps: List<com.example.domain.model.PurificationGuideItem>) {
        if (steps.isEmpty()) return
        tahoratAutoPlayJob?.cancel()
        tahoratAutoPlayJob = viewModelScope.launch {
            val settings = uiState.value.settings
            val coach = getOrCreateAudioCoach()
            try {
                for (step in steps) {
                    tahoratAutoPlayFlow.value = true to step.id
                    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                        coach.speak(
                            text = step.ttsPrompt,
                            gender = settings.gender,
                            preferOfflineMp3 = settings.preferOfflineMp3,
                            speed = settings.voiceSpeed,
                            audioAssetId = "",
                            onCompleted = {
                                if (cont.isActive) cont.resume(Unit) {}
                            }
                        )
                    }
                    delay(700L)
                }
            } finally {
                tahoratAutoPlayFlow.value = false to null
            }
        }
    }

    fun stopTahoratAutoPlay() {
        tahoratAutoPlayJob?.cancel()
        tahoratAutoPlayJob = null
        tahoratAutoPlayFlow.value = false to null
        _audioCoach?.stop()
    }

    fun stopSampleAudio() {
        stopTahoratAutoPlay()
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
        _audioCoach?.release()
        _audioCoach = null
    }

    private data class SyncMetaBundle(
        val latestSyncMillis: Long,
        val metadata: PrayerSyncMetadataEntity?,
        val cachedDaysCount: Int,
        val isSyncing: Boolean,
        val statusMessage: com.example.domain.model.LText?
    )

    private data class CalendarBundle(
        val todayDay: PrayerCalendarDayEntity?,
        val tomorrowDay: PrayerCalendarDayEntity?,
        val monthlyDays: List<PrayerCalendarDayEntity>,
        val latestSyncMillis: Long,
        val cachedDaysCount: Int,
        val metadata: PrayerSyncMetadataEntity?,
        val isSyncing: Boolean,
        val syncStatusMessage: com.example.domain.model.LText?
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
