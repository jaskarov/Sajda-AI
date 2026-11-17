package com.example.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.UzbekCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

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

@Serializable
private data class AladhanCalendarResponse(
    val code: Int = 0,
    val data: List<AladhanDayData> = emptyList()
)

@Serializable
private data class AladhanDayData(
    val timings: AladhanTimings,
    val date: AladhanDateContainer
)

@Serializable
private data class AladhanTimings(
    val Fajr: String,
    val Sunrise: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)

@Serializable
private data class AladhanDateContainer(
    val gregorian: AladhanGregorian
)

@Serializable
private data class AladhanGregorian(
    val day: String,
    val year: String,
    val month: AladhanMonth
)

@Serializable
private data class AladhanMonth(
    val number: Int
)

class PrayerCalendarRepository(
    private val context: Context,
    private val dao: PrayerCalendarDao
) {
    companion object {
        private const val TAG = "PrayerCalendarRepo"
        private const val ASSET_FILE_NAME = "uzbekistan_prayer_calendar_365.json"
        private const val EXPECTED_MIN_ROWS = 365 * 14
        private val HH_MM_REGEX = Regex("(\\d{2}:\\d{2})")
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val seedMutex = Mutex()

    suspend fun ensureSeededFromAssets() = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            if (dao.countAll() >= EXPECTED_MIN_ROWS) return@withLock
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

    fun observeTodayAndTomorrow(
        cityId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Flow<Pair<PrayerCalendarDayEntity?, PrayerCalendarDayEntity?>> {
        val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
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

    fun observeLatestSyncMillis(cityId: String): Flow<Long?> = dao.observeLatestSyncMillis(cityId)

    suspend fun getTodayAndTomorrow(
        cityId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<PrayerCalendarDayEntity?, PrayerCalendarDayEntity?> {
        ensureSeededFromAssets()
        val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val today = dao.getDay(
            cityId = cityId,
            year = todayCal.get(Calendar.YEAR),
            month = todayCal.get(Calendar.MONTH) + 1,
            day = todayCal.get(Calendar.DAY_OF_MONTH)
        )
        val tomorrow = dao.getDay(
            cityId = cityId,
            year = tomorrowCal.get(Calendar.YEAR),
            month = tomorrowCal.get(Calendar.MONTH) + 1,
            day = tomorrowCal.get(Calendar.DAY_OF_MONTH)
        )
        return today to tomorrow
    }

    suspend fun syncFromNetworkIfConnected(
        cityId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        ensureSeededFromAssets()
        if (!isNetworkConnected()) return@withContext false

        val city = UzbekistanCities.findById(cityId)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val nextYear = nextCal.get(Calendar.YEAR)
        val nextMonth = nextCal.get(Calendar.MONTH) + 1

        val currentOk = fetchAndStoreMonth(city, currentYear, currentMonth, nowMillis)
        val nextOk = fetchAndStoreMonth(city, nextYear, nextMonth, nowMillis)
        currentOk || nextOk
    }

    private suspend fun fetchAndStoreMonth(
        city: UzbekCity,
        year: Int,
        month: Int,
        syncTimeMillis: Long
    ): Boolean {
        return runCatching {
            val urlStr = "https://api.aladhan.com/v1/calendar/$year/$month" +
                "?latitude=${city.latitude}&longitude=${city.longitude}" +
                "&method=99&methodSettings=15,null,15&school=1" +
                "&tune=0,0,0,2,0,3,0,0,0&timezonestring=Asia/Tashkent"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching false
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = json.decodeFromString<AladhanCalendarResponse>(body)
                if (parsed.data.isEmpty()) return@runCatching false

                val rows = parsed.data.mapNotNull { dayData ->
                    val d = dayData.date.gregorian.day.toIntOrNull() ?: return@mapNotNull null
                    val m = dayData.date.gregorian.month.number
                    val y = dayData.date.gregorian.year.toIntOrNull() ?: year
                    PrayerCalendarDayEntity(
                        cityId = city.id,
                        year = y,
                        month = m,
                        day = d,
                        bomdod = extractHhMm(dayData.timings.Fajr),
                        quyosh = extractHhMm(dayData.timings.Sunrise),
                        peshin = extractHhMm(dayData.timings.Dhuhr),
                        asr = extractHhMm(dayData.timings.Asr),
                        shom = extractHhMm(dayData.timings.Maghrib),
                        xufton = extractHhMm(dayData.timings.Isha),
                        source = "aladhan_online",
                        syncedAtMillis = syncTimeMillis
                    )
                }
                if (rows.isNotEmpty()) {
                    dao.insertAll(rows)
                    true
                } else {
                    false
                }
            } finally {
                conn.disconnect()
            }
        }.onFailure { err ->
            Log.w(TAG, "Online calendar sync failed for ${city.id} $year-$month", err)
        }.getOrDefault(false)
    }

    private fun extractHhMm(raw: String): String {
        return HH_MM_REGEX.find(raw)?.groupValues?.getOrNull(1) ?: "00:00"
    }

    private fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
