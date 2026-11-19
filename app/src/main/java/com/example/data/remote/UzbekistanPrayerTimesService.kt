package com.example.data.remote

import android.util.Log
import com.example.data.local.PrayerCalendarDayEntity
import com.example.domain.model.UzbekCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit API definition for fetching Uzbekistan monthly prayer times.
 * Supports primary Uzbekistan regional endpoint (islomapi.uz) and
 * Aladhan Hanafi (15°/15°, Asia/Tashkent) fallback endpoint.
 */
interface UzbekistanPrayerTimesApi {

    @GET("https://islomapi.uz/api/monthly")
    suspend fun fetchIslomApiMonthly(
        @Query("region") region: String,
        @Query("month") month: Int
    ): Response<ResponseBody>

    @GET("https://api.aladhan.com/v1/calendar/{year}/{month}")
    suspend fun fetchAladhanMonthly(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 99,
        @Query("methodSettings") methodSettings: String = "15,null,15",
        @Query("school") school: Int = 1,
        @Query("tune") tune: String = "0,0,0,2,0,3,0,0,0",
        @Query("timezonestring") timezone: String = "Asia/Tashkent"
    ): Response<ResponseBody>
}

sealed interface PrayerTimeFetchResult {
    data class Success(
        val days: List<PrayerCalendarDayEntity>,
        val provider: String
    ) : PrayerTimeFetchResult

    data class Failure(
        val reason: String
    ) : PrayerTimeFetchResult
}

interface UzbekistanPrayerTimesService {
    suspend fun fetchMonthlyPrayerTimes(
        city: UzbekCity,
        year: Int,
        month: Int,
        syncTimeMillis: Long = System.currentTimeMillis()
    ): PrayerTimeFetchResult
}

@Serializable
internal data class IslomApiDayDto(
    val region: String = "",
    val date: String = "",
    val month: Int = 0,
    val day: Int = 0,
    val times: IslomApiTimesDto
)

@Serializable
internal data class IslomApiTimesDto(
    @SerialName("tong_saharlik") val tongSaharlik: String = "",
    val quyosh: String = "",
    val peshin: String = "",
    val asr: String = "",
    @SerialName("shom_iftor") val shomIftor: String = "",
    val hufton: String = ""
)

@Serializable
internal data class AladhanCalendarResponseDto(
    val code: Int = 0,
    val data: List<AladhanDayDataDto> = emptyList()
)

@Serializable
internal data class AladhanDayDataDto(
    val timings: AladhanTimingsDto,
    val date: AladhanDateContainerDto
)

@Serializable
internal data class AladhanTimingsDto(
    val Fajr: String = "",
    val Sunrise: String = "",
    val Dhuhr: String = "",
    val Asr: String = "",
    val Maghrib: String = "",
    val Isha: String = ""
)

@Serializable
internal data class AladhanDateContainerDto(
    val gregorian: AladhanGregorianDto
)

@Serializable
internal data class AladhanGregorianDto(
    val day: String = "",
    val year: String = "",
    val month: AladhanMonthDto
)

@Serializable
internal data class AladhanMonthDto(
    val number: Int = 0
)

class RemoteUzbekistanPrayerTimesService(
    private val api: UzbekistanPrayerTimesApi,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : UzbekistanPrayerTimesService {

    companion object {
        private const val TAG = "UzPrayerTimesService"
        const val PROVIDER_ISLOM_API = "islomapi_uz"
        const val PROVIDER_ALADHAN = "aladhan_online"

        private val HH_MM_REGEX = Regex("(\\d{1,2}:\\d{2})")
        private val ISO_DATE_REGEX = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})")

        fun create(): RemoteUzbekistanPrayerTimesService {
            val client = OkHttpClient.Builder()
                .connectTimeout(7, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://islomapi.uz/")
                .client(client)
                .build()

            return RemoteUzbekistanPrayerTimesService(
                api = retrofit.create(UzbekistanPrayerTimesApi::class.java)
            )
        }

        fun islomApiRegionName(cityId: String): String = when (cityId) {
            "toshkent" -> "Toshkent"
            "fargona" -> "Farg'ona"
            "andijon" -> "Andijon"
            "namangan" -> "Namangan"
            "qoqon" -> "Qo'qon"
            "samarqand" -> "Samarqand"
            "buxoro" -> "Buxoro"
            "qarshi" -> "Qarshi"
            "termiz" -> "Termiz"
            "navoiy" -> "Navoiy"
            "jizzax" -> "Jizzax"
            "guliston" -> "Guliston"
            "urganch" -> "Urganch"
            "nukus" -> "Nukus"
            else -> "Toshkent"
        }

        internal fun normalizeHhMm(raw: String): String {
            val match = HH_MM_REGEX.find(raw.trim())?.groupValues?.getOrNull(1) ?: return "00:00"
            val parts = match.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            return "%02d:%02d".format(h, m)
        }
    }

    override suspend fun fetchMonthlyPrayerTimes(
        city: UzbekCity,
        year: Int,
        month: Int,
        syncTimeMillis: Long
    ): PrayerTimeFetchResult = withContext(Dispatchers.IO) {
        // 1. Try primary Uzbekistan regional API (islomapi.uz)
        val primaryError = runCatching {
            val region = islomApiRegionName(city.id)
            val response = api.fetchIslomApiMonthly(region = region, month = month)
            if (response.isSuccessful) {
                val body = response.body()?.string().orEmpty()
                val parsed = parseIslomApiMonthly(
                    rawJson = body,
                    cityId = city.id,
                    requestedYear = year,
                    requestedMonth = month,
                    syncTimeMillis = syncTimeMillis
                )
                if (parsed.isNotEmpty()) {
                    return@withContext PrayerTimeFetchResult.Success(
                        days = parsed,
                        provider = PROVIDER_ISLOM_API
                    )
                }
                "islomapi.uz returned empty or mismatched year data"
            } else {
                "islomapi.uz HTTP ${response.code()}"
            }
        }.fold(
            onSuccess = { it },
            onFailure = { err ->
                Log.w(TAG, "Primary Uzbekistan API failed for ${city.id} $year-$month", err)
                err.localizedMessage ?: "islomapi.uz network error"
            }
        )

        // 2. Automatic fallback to Aladhan API tuned for Uzbekistan Muslim Board (Hanafi 15°/15°)
        val fallbackResult = runCatching {
            val response = api.fetchAladhanMonthly(
                year = year,
                month = month,
                latitude = city.latitude,
                longitude = city.longitude
            )
            if (response.isSuccessful) {
                val body = response.body()?.string().orEmpty()
                val parsed = parseAladhanMonthly(
                    rawJson = body,
                    cityId = city.id,
                    fallbackYear = year,
                    syncTimeMillis = syncTimeMillis
                )
                if (parsed.isNotEmpty()) {
                    PrayerTimeFetchResult.Success(
                        days = parsed,
                        provider = PROVIDER_ALADHAN
                    )
                } else {
                    PrayerTimeFetchResult.Failure("Aladhan API returned empty list ($primaryError)")
                }
            } else {
                PrayerTimeFetchResult.Failure("Aladhan HTTP ${response.code()} ($primaryError)")
            }
        }.getOrElse { err ->
            Log.w(TAG, "Aladhan fallback failed for ${city.id} $year-$month", err)
            PrayerTimeFetchResult.Failure(
                "${err.localizedMessage ?: "Aladhan network error"} ($primaryError)"
            )
        }

        fallbackResult
    }

    internal fun parseIslomApiMonthly(
        rawJson: String,
        cityId: String,
        requestedYear: Int,
        requestedMonth: Int,
        syncTimeMillis: Long
    ): List<PrayerCalendarDayEntity> {
        if (rawJson.isBlank()) return emptyList()
        val items = json.decodeFromString<List<IslomApiDayDto>>(rawJson)
        if (items.isEmpty()) return emptyList()

        return items.mapNotNull { item ->
            val dateMatch = ISO_DATE_REGEX.find(item.date.trim())
            val parsedYear = dateMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: requestedYear
            val parsedMonth = dateMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
                ?: item.month.takeIf { it in 1..12 }
                ?: requestedMonth
            val parsedDay = dateMatch?.groupValues?.getOrNull(3)?.toIntOrNull()
                ?: item.day.takeIf { it in 1..31 }
                ?: return@mapNotNull null

            // Guard against stale year responses when requesting a different year
            if (parsedYear != requestedYear || parsedMonth != requestedMonth) {
                return@mapNotNull null
            }

            PrayerCalendarDayEntity(
                cityId = cityId,
                year = parsedYear,
                month = parsedMonth,
                day = parsedDay,
                bomdod = normalizeHhMm(item.times.tongSaharlik),
                quyosh = normalizeHhMm(item.times.quyosh),
                peshin = normalizeHhMm(item.times.peshin),
                asr = normalizeHhMm(item.times.asr),
                shom = normalizeHhMm(item.times.shomIftor),
                xufton = normalizeHhMm(item.times.hufton),
                source = PROVIDER_ISLOM_API,
                syncedAtMillis = syncTimeMillis
            )
        }
    }

    internal fun parseAladhanMonthly(
        rawJson: String,
        cityId: String,
        fallbackYear: Int,
        syncTimeMillis: Long
    ): List<PrayerCalendarDayEntity> {
        if (rawJson.isBlank()) return emptyList()
        val parsed = json.decodeFromString<AladhanCalendarResponseDto>(rawJson)
        if (parsed.data.isEmpty()) return emptyList()

        return parsed.data.mapNotNull { dayData ->
            val d = dayData.date.gregorian.day.toIntOrNull() ?: return@mapNotNull null
            val m = dayData.date.gregorian.month.number.takeIf { it in 1..12 } ?: return@mapNotNull null
            val y = dayData.date.gregorian.year.toIntOrNull() ?: fallbackYear
            PrayerCalendarDayEntity(
                cityId = cityId,
                year = y,
                month = m,
                day = d,
                bomdod = normalizeHhMm(dayData.timings.Fajr),
                quyosh = normalizeHhMm(dayData.timings.Sunrise),
                peshin = normalizeHhMm(dayData.timings.Dhuhr),
                asr = normalizeHhMm(dayData.timings.Asr),
                shom = normalizeHhMm(dayData.timings.Maghrib),
                xufton = normalizeHhMm(dayData.timings.Isha),
                source = PROVIDER_ALADHAN,
                syncedAtMillis = syncTimeMillis
            )
        }
    }
}
