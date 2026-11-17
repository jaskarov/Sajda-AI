package com.example.domain.engine

import com.example.domain.model.PrayerScheduleSummary
import com.example.domain.model.PrayerTimeSlot
import com.example.domain.model.PrayerType
import com.example.domain.model.UzbekCity
import com.example.domain.model.lText
import java.util.Calendar
import java.util.Locale
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Offline Hanafi astronomical prayer time calculator tuned for Uzbekistan (UTC+5).
 */
class GetPrayerTimesUseCase {

    fun calculateForToday(
        city: UzbekCity,
        completedToday: Set<PrayerType>,
        alarmMap: Map<PrayerType, Boolean>,
        nowMillis: Long = System.currentTimeMillis(),
        todayCalendarDay: com.example.data.local.PrayerCalendarDayEntity? = null,
        tomorrowCalendarDay: com.example.data.local.PrayerCalendarDayEntity? = null
    ): PrayerScheduleSummary {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val nowSecondsOfDay =
            cal.get(Calendar.HOUR_OF_DAY) * 3600L +
                cal.get(Calendar.MINUTE) * 60L +
                cal.get(Calendar.SECOND)

        val rawMinutes = if (todayCalendarDay != null) {
            linkedMapOf(
                PrayerType.BOMDOD to parseHhMmToMinutes(todayCalendarDay.bomdod),
                PrayerType.QUYOSH to parseHhMmToMinutes(todayCalendarDay.quyosh),
                PrayerType.PESHIN to parseHhMmToMinutes(todayCalendarDay.peshin),
                PrayerType.ASR to parseHhMmToMinutes(todayCalendarDay.asr),
                PrayerType.SHOM to parseHhMmToMinutes(todayCalendarDay.shom),
                PrayerType.XUFTON to parseHhMmToMinutes(todayCalendarDay.xufton)
            )
        } else {
            val times = computeTimesHours(
                year = year,
                month = month,
                day = day,
                latitude = city.latitude,
                longitude = city.longitude,
                timeZoneHours = 5.0
            )

            linkedMapOf(
                PrayerType.BOMDOD to toMinutes(times[PrayerType.BOMDOD]!!) + city.fajrOffsetMin,
                PrayerType.QUYOSH to toMinutes(times[PrayerType.QUYOSH]!!),
                PrayerType.PESHIN to toMinutes(times[PrayerType.PESHIN]!!),
                PrayerType.ASR to toMinutes(times[PrayerType.ASR]!!),
                PrayerType.SHOM to toMinutes(times[PrayerType.SHOM]!!) + city.maghribOffsetMin,
                PrayerType.XUFTON to toMinutes(times[PrayerType.XUFTON]!!)
            )
        }

        val nextEntryToday = rawMinutes.entries.firstOrNull { (_, mins) ->
            mins * 60L > nowSecondsOfDay
        }
        val nextPrayer = nextEntryToday?.key ?: PrayerType.BOMDOD

        val currentPrayer = rawMinutes.entries
            .lastOrNull { (type, mins) -> type.isPrayable && mins * 60L <= nowSecondsOfDay }?.key
            ?: PrayerType.XUFTON

        val (nextTargetTotalMins, countdownSeconds) = if (nextEntryToday != null) {
            val targetSec = nextEntryToday.value * 60L
            nextEntryToday.value to (targetSec - nowSecondsOfDay)
        } else {
            // After Xufton: read tomorrow's Bomdod from Room calendar or calculate it
            val tomorrowBomdodMins = if (tomorrowCalendarDay != null) {
                parseHhMmToMinutes(tomorrowCalendarDay.bomdod)
            } else {
                val tomorrowCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
                val tomorrowTimes = computeTimesHours(
                    year = tomorrowCal.get(Calendar.YEAR),
                    month = tomorrowCal.get(Calendar.MONTH) + 1,
                    day = tomorrowCal.get(Calendar.DAY_OF_MONTH),
                    latitude = city.latitude,
                    longitude = city.longitude,
                    timeZoneHours = 5.0
                )
                toMinutes(tomorrowTimes[PrayerType.BOMDOD]!!) + city.fajrOffsetMin
            }
            val remaining = (24L * 3600L - nowSecondsOfDay) + tomorrowBomdodMins * 60L
            tomorrowBomdodMins to remaining
        }

        val nextPrayerTimeFormatted = String.format(
            Locale.US,
            "%02d:%02d",
            (nextTargetTotalMins / 60) % 24,
            nextTargetTotalMins % 60
        )

        val slots = rawMinutes.entries.map { (type, totalMins) ->
            val h = (totalMins / 60) % 24
            val m = totalMins % 60
            PrayerTimeSlot(
                type = type,
                timeFormatted = String.format(Locale.US, "%02d:%02d", h, m),
                hour = h,
                minute = m,
                isPassed = totalMins * 60L <= nowSecondsOfDay && type != currentPrayer,
                isCurrent = type == currentPrayer,
                isCompletedToday = completedToday.contains(type),
                alarmEnabled = alarmMap[type] ?: type.isPrayable
            )
        }

        val qibla = UzbekistanCities.calculateQiblaBearing(city.latitude, city.longitude)
        val dateLabel = formatUzbekDate(cal)

        return PrayerScheduleSummary(
            city = city,
            nextPrayer = nextPrayer,
            nextPrayerTimeFormatted = nextPrayerTimeFormatted,
            currentPrayer = currentPrayer,
            countdownSeconds = countdownSeconds,
            slots = slots,
            hijriLabel = dateLabel,
            qiblaBearingDegrees = qibla
        )
    }

    private fun computeTimesHours(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZoneHours: Double
    ): Map<PrayerType, Double> {
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val d = jd - 2451545.0

        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val decl = asinDeg(sinDeg(e) * sinDeg(l))
        val eqOfTime = q / 15.0 - fixHour(ra)

        val dhuhr = 12.0 + timeZoneHours - longitude / 15.0 - eqOfTime + (2.0 / 60.0)
        val sunrise = dhuhr - sunHourAngle(0.833, latitude, decl) / 15.0
        val sunset = dhuhr + sunHourAngle(0.833, latitude, decl) / 15.0

        // Uzbekistan Muslim Board standard: Fajr 15.0°, Isha 15.0°, Hanafi Asr (shadow = 2)
        val fajr = dhuhr - sunHourAngle(15.0, latitude, decl) / 15.0
        val isha = dhuhr + sunHourAngle(15.0, latitude, decl) / 15.0
        val asr = dhuhr + asrHourAngle(2.0, latitude, decl) / 15.0

        return mapOf(
            PrayerType.BOMDOD to fajr,
            PrayerType.QUYOSH to sunrise,
            PrayerType.PESHIN to dhuhr,
            PrayerType.ASR to asr,
            PrayerType.SHOM to sunset + (2.0 / 60.0),
            PrayerType.XUFTON to isha
        )
    }

    private fun sunHourAngle(angle: Double, lat: Double, decl: Double): Double {
        val cosH = (-sinDeg(angle) - sinDeg(lat) * sinDeg(decl)) /
            (cosDeg(lat) * cosDeg(decl))
        val clamped = cosH.coerceIn(-0.999, 0.999)
        return Math.toDegrees(acos(clamped))
    }

    private fun asrHourAngle(factor: Double, lat: Double, decl: Double): Double {
        val diff = kotlin.math.abs(lat - decl)
        val targetAlt = Math.toDegrees(atan(1.0 / (factor + tan(Math.toRadians(diff)))))
        val cosH = (sinDeg(targetAlt) - sinDeg(lat) * sinDeg(decl)) /
            (cosDeg(lat) * cosDeg(decl))
        val clamped = cosH.coerceIn(-0.999, 0.999)
        return Math.toDegrees(acos(clamped))
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun toMinutes(hours: Double): Int {
        val normalized = fixHour(hours)
        return (normalized * 60.0).roundToInt().coerceIn(0, 23 * 60 + 59)
    }

    private fun parseHhMmToMinutes(hhMm: String): Int {
        val parts = hhMm.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (h * 60 + m).coerceIn(0, 23 * 60 + 59)
    }

    private fun fixAngle(a: Double): Double {
        var res = a % 360.0
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h % 24.0
        if (res < 0) res += 24.0
        return res
    }

    private fun sinDeg(d: Double) = sin(Math.toRadians(d))
    private fun cosDeg(d: Double) = cos(Math.toRadians(d))
    private fun asinDeg(x: Double) = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun atan2Deg(y: Double, x: Double) = Math.toDegrees(atan2(y, x))

    private fun formatUzbekDate(cal: Calendar): com.example.domain.model.LText {
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val monthsLat = listOf(
            "Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
            "Iyul", "Avgust", "Sentabr", "Oktabr", "Noyabr", "Dekabr"
        )
        val monthsCyr = listOf(
            "Январ", "Феврал", "Март", "Апрел", "Май", "Июн",
            "Июл", "Август", "Сентабр", "Октабр", "Ноябр", "Декабр"
        )
        return lText(
            "$day-${monthsLat[monthIdx]}, $year",
            "$day-${monthsCyr[monthIdx]}, $year"
        )
    }
}
