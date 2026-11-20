package com.example.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.remote.PrayerTimeFetchResult
import com.example.data.remote.RemoteUzbekistanPrayerTimesService
import com.example.data.remote.UzbekistanPrayerTimesService
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.UzbekCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.TimeZone

@Serializable
private data class BundledCalendarPayload(
    val year: Int,
    val source: String,
    val cities: Map<String, List<BundledCalendarDay>>
)

@Serializable
private data class BundledCalendarDay(
    val m: Int,
    val d: Int,
    val b: String,
    val q: String,
    val p: String,
    val a: String,
    val s: String,
    val x: String
)

sealed interface PrayerSyncOutcome {
    data class Synced(
        val cityId: String,
        val provider: String,
        val fetchedDaysCount: Int,
        val totalCachedDays: Int
    ) : PrayerSyncOutcome

    data class OfflineCacheReady(
        val cityId: String,
        val totalCachedDays: Int,
        val reason: String
    ) : PrayerSyncOutcome
}

class PrayerCalendarRepository(
    private val context: Context,
    private val dao: PrayerCalendarDao,
    private val syncMetadataDao: PrayerSyncMetadataDao? = null,
    private val remoteService: UzbekistanPrayerTimesService = RemoteUzbekistanPrayerTimesService.create(),
    private val isOnlineProvider: (() -> Boolean)? = null
) {
    companion object {
        private const val TAG = "PrayerCalendarRepo"
        private const val ASSET_FILE_NAME = "uzbekistan_prayer_calendar_365.json"
        private const val EXPECTED_MIN_ROWS = 365 * 14
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val seedMutex = Mutex()
    private val solarUseCase = GetPrayerTimesUseCase()

    /**
     * Fast cold-start seeding: populates ONLY the active city for [year] (365 rows, ~10-15ms)
     * so HomeScreen renders immediately without waiting for all 14 cities (10,220+ rows).
     */
    suspend fun ensureCitySeededFast(
        cityId: String,
        year: Int = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).get(Calendar.YEAR)
    ) = withContext(Dispatchers.IO) {
        val city = UzbekistanCities.findById(cityId)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply {
            set(year, Calendar.JANUARY, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
        if (dao.countForCityYear(city.id, year) >= daysInYear) return@withContext

        seedMutex.withLock {
            if (dao.countForCityYear(city.id, year) >= daysInYear) return@withLock
            if (year == 2026) {
                val seededFromAsset = runCatching {
                    val rawJson = context.assets.open(ASSET_FILE_NAME)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    val payload = json.decodeFromString<BundledCalendarPayload>(rawJson)
                    val cityDays = payload.cities[city.id].orEmpty()
                    if (cityDays.isNotEmpty()) {
                        val entities = cityDays.map { entry ->
                            PrayerCalendarDayEntity(
                                cityId = city.id,
                                year = payload.year,
                                month = entry.m,
                                day = entry.d,
                                bomdod = entry.b,
                                quyosh = entry.q,
                                peshin = entry.p,
                                asr = entry.a,
                                shom = entry.s,
                                xufton = entry.x,
                                source = payload.source,
                                syncedAtMillis = 0L
                            )
                        }
                        dao.insertAll(entities)
                        true
                    } else {
                        false
                    }
                }.getOrDefault(false)
                if (seededFromAsset) return@withLock
            }

            val generated = ArrayList<PrayerCalendarDayEntity>(daysInYear)
            for (dayOfYear in 1..daysInYear) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.DAY_OF_YEAR, dayOfYear)
                val month = cal.get(Calendar.MONTH) + 1
                val day = cal.get(Calendar.DAY_OF_MONTH)
                generated += solarUseCase.computeCalendarDay(
                    city = city,
                    year = year,
                    month = month,
                    day = day,
                    source = "uz_taqvim_365_${year}_offline",
                    syncedAtMillis = 0L
                )
            }
            if (generated.isNotEmpty()) {
                dao.insertAll(generated)
            }
        }
    }

    suspend fun ensureSeededFromAssets() = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            if (dao.countForYear(2026) < EXPECTED_MIN_ROWS) {
                runCatching {
                    val rawJson = context.assets.open(ASSET_FILE_NAME)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    val payload = json.decodeFromString<BundledCalendarPayload>(rawJson)
                    val entities = ArrayList<PrayerCalendarDayEntity>(EXPECTED_MIN_ROWS)
                    payload.cities.forEach { (cityId, days) ->
                        days.forEach { entry ->
                            entities += PrayerCalendarDayEntity(
                                cityId = cityId,
                                year = payload.year,
                                month = entry.m,
                                day = entry.d,
                                bomdod = entry.b,
                                quyosh = entry.q,
                                peshin = entry.p,
                                asr = entry.a,
                                shom = entry.s,
                                xufton = entry.x,
                                source = payload.source,
                                syncedAtMillis = 0L
                            )
                        }
                    }
                    if (entities.isNotEmpty()) {
                        dao.insertAll(entities)
                    }
                }.onFailure { err ->
                    Log.w(TAG, "Failed to seed 365-day prayer calendar JSON", err)
                }
            }
        }
    }

    suspend fun ensureYearSeededOffline(year: Int) = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            ensureYearSeededOfflineLocked(year)
        }
    }

    private suspend fun ensureYearSeededOfflineLocked(year: Int) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply {
            set(year, Calendar.JANUARY, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
        val expectedRows = daysInYear * UzbekistanCities.all.size
        if (dao.countForYear(year) >= expectedRows) return

        val generated = ArrayList<PrayerCalendarDayEntity>(expectedRows)
        for (dayOfYear in 1..daysInYear) {
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.DAY_OF_YEAR, dayOfYear)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            UzbekistanCities.all.forEach { city ->
                generated += solarUseCase.computeCalendarDay(
                    city = city,
                    year = year,
                    month = month,
                    day = day,
                    source = "uz_taqvim_365_${year}_offline",
                    syncedAtMillis = 0L
                )
            }
        }
        if (generated.isNotEmpty()) {
            dao.insertAll(generated)
        }
    }

    fun observeTodayAndTomorrow(
        cityId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Flow<Pair<PrayerCalendarDayEntity?, PrayerCalendarDayEntity?>> {
        val todayCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply { timeInMillis = nowMillis }
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        val todayFlow = dao.observeDay(
            cityId = cityId,
            year = todayCal.get(Calendar.YEAR),
            month = todayCal.get(Calendar.MONTH) + 1,
            day = todayCal.get(Calendar.DAY_OF_MONTH)
        )
        val tomorrowFlow = dao.observeDay(
            cityId = cityId,
            year = tomorrowCal.get(Calendar.YEAR),
            month = tomorrowCal.get(Calendar.MONTH) + 1,
            day = tomorrowCal.get(Calendar.DAY_OF_MONTH)
        )
        return combine(todayFlow, tomorrowFlow) { today, tomorrow ->
            today to tomorrow
        }
    }

    fun observeMonthSchedule(
        cityId: String,
        year: Int,
        month: Int
    ): Flow<List<PrayerCalendarDayEntity>> = dao.observeMonth(cityId, year, month)

    fun observeCachedDayCount(cityId: String): Flow<Int> = dao.observeCachedDayCount(cityId)

    fun observeLatestSyncMillis(cityId: String): Flow<Long?> = dao.observeLatestSyncMillis(cityId)

    fun observeSyncMetadata(cityId: String): Flow<PrayerSyncMetadataEntity?> =
        syncMetadataDao?.observeMetadata(cityId) ?: flowOf(null)

    suspend fun getMonthSchedule(
        cityId: String,
        year: Int,
        month: Int
    ): List<PrayerCalendarDayEntity> = withContext(Dispatchers.IO) {
        ensureSeededFromAssets()
        if (year != 2026) ensureYearSeededOffline(year)
        dao.getMonth(cityId, year, month)
    }

    suspend fun getTodayAndTomorrow(
        cityId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<PrayerCalendarDayEntity?, PrayerCalendarDayEntity?> {
        ensureSeededFromAssets()
        val todayCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply { timeInMillis = nowMillis }
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val todayYear = todayCal.get(Calendar.YEAR)
        val tomorrowYear = tomorrowCal.get(Calendar.YEAR)
        if (todayYear != 2026) ensureYearSeededOffline(todayYear)
        if (tomorrowYear != 2026 && tomorrowYear != todayYear) ensureYearSeededOffline(tomorrowYear)

        val today = dao.getDay(
            cityId = cityId,
            year = todayYear,
            month = todayCal.get(Calendar.MONTH) + 1,
            day = todayCal.get(Calendar.DAY_OF_MONTH)
        )
        val tomorrow = dao.getDay(
            cityId = cityId,
            year = tomorrowYear,
            month = tomorrowCal.get(Calendar.MONTH) + 1,
            day = tomorrowCal.get(Calendar.DAY_OF_MONTH)
        )
        return today to tomorrow
    }

    suspend fun syncFromNetworkIfConnected(
        cityId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return syncCityPrayerTimes(cityId, nowMillis) is PrayerSyncOutcome.Synced
    }

    suspend fun syncCityPrayerTimes(
        cityId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): PrayerSyncOutcome = withContext(Dispatchers.IO) {
        ensureSeededFromAssets()
        val city = UzbekistanCities.findById(cityId)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply { timeInMillis = nowMillis }
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        if (currentYear != 2026) {
            ensureYearSeededOffline(currentYear)
        }

        if (!isNetworkConnected()) {
            val cached = dao.countForCity(city.id)
            recordSyncMetadata(
                city = city,
                provider = "uz_taqvim_365",
                syncedAtMillis = syncMetadataDao?.getMetadata(city.id)?.lastSyncedAtMillis ?: 0L,
                cachedDays = cached,
                success = false,
                error = "offline"
            )
            return@withContext PrayerSyncOutcome.OfflineCacheReady(
                cityId = city.id,
                totalCachedDays = cached,
                reason = "offline"
            )
        }

        val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val nextYear = nextCal.get(Calendar.YEAR)
        val nextMonth = nextCal.get(Calendar.MONTH) + 1

        val currentResult = remoteService.fetchMonthlyPrayerTimes(city, currentYear, currentMonth, nowMillis)
        val nextResult = remoteService.fetchMonthlyPrayerTimes(city, nextYear, nextMonth, nowMillis)

        val fetchedRows = mutableListOf<PrayerCalendarDayEntity>()
        var activeProvider: String? = null
        val errors = mutableListOf<String>()

        when (currentResult) {
            is PrayerTimeFetchResult.Success -> {
                fetchedRows += currentResult.days
                activeProvider = currentResult.provider
            }
            is PrayerTimeFetchResult.Failure -> errors += currentResult.reason
        }

        when (nextResult) {
            is PrayerTimeFetchResult.Success -> {
                fetchedRows += nextResult.days
                if (activeProvider == null) activeProvider = nextResult.provider
            }
            is PrayerTimeFetchResult.Failure -> errors += nextResult.reason
        }

        if (fetchedRows.isNotEmpty()) {
            dao.insertAll(fetchedRows)
            val totalCached = dao.countForCity(city.id)
            val provider = activeProvider ?: RemoteUzbekistanPrayerTimesService.PROVIDER_ALADHAN
            recordSyncMetadata(
                city = city,
                provider = provider,
                syncedAtMillis = nowMillis,
                cachedDays = totalCached,
                success = true,
                error = null
            )
            PrayerSyncOutcome.Synced(
                cityId = city.id,
                provider = provider,
                fetchedDaysCount = fetchedRows.size,
                totalCachedDays = totalCached
            )
        } else {
            val totalCached = dao.countForCity(city.id)
            val reason = errors.joinToString("; ").ifBlank { "network_error" }
            val existingMeta = syncMetadataDao?.getMetadata(city.id)
            recordSyncMetadata(
                city = city,
                provider = existingMeta?.provider ?: "uz_taqvim_365",
                syncedAtMillis = existingMeta?.lastSyncedAtMillis ?: 0L,
                cachedDays = totalCached,
                success = false,
                error = reason
            )
            PrayerSyncOutcome.OfflineCacheReady(
                cityId = city.id,
                totalCachedDays = totalCached,
                reason = reason
            )
        }
    }

    private suspend fun recordSyncMetadata(
        city: UzbekCity,
        provider: String,
        syncedAtMillis: Long,
        cachedDays: Int,
        success: Boolean,
        error: String?
    ) {
        syncMetadataDao?.upsert(
            PrayerSyncMetadataEntity(
                cityId = city.id,
                cityName = city.name.latin,
                provider = provider,
                lastSyncedAtMillis = syncedAtMillis,
                cachedDaysCount = cachedDays,
                lastSyncSuccess = success,
                lastError = error
            )
        )
    }

    private fun isNetworkConnected(): Boolean {
        isOnlineProvider?.let { return it() }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
