package com.example.domain.model

import com.example.data.local.DailyPrayerProgressEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class WeeklyChartMode {
    BY_PRAYER,
    BY_DAY
}

data class PrayerBarMetric(
    val prayerType: PrayerType,
    val completedDays: Int,
    val totalDays: Int,
    val percentage: Float,
    val dayStatuses: List<Boolean>
) {
    val roundedPercent: Int get() = percentage.roundToInt()
}

data class DailyBarMetric(
    val dayKey: String,
    val shortDayLabel: LText,
    val fullDayLabel: LText,
    val shortDate: String,
    val completedCount: Int,
    val totalPrayers: Int,
    val percentage: Float,
    val isToday: Boolean,
    val isFuture: Boolean,
    val completedTypes: Set<PrayerType>
) {
    val roundedPercent: Int get() = percentage.roundToInt()
}

data class WeeklyPrayerSummary(
    val weekOffset: Int,
    val startDayKey: String,
    val endDayKey: String,
    val rangeLabel: LText,
    val prayerBars: List<PrayerBarMetric>,
    val dailyBars: List<DailyBarMetric>,
    val overallPercentage: Float,
    val totalCompleted: Int,
    val totalPossible: Int,
    val perfectDaysCount: Int,
    val bestPrayer: PrayerType?
) {
    val overallRoundedPercent: Int get() = overallPercentage.roundToInt()
}

object WeeklyPrayerAnalytics {

    val fiveDailyPrayers: List<PrayerType> = listOf(
        PrayerType.BOMDOD,
        PrayerType.PESHIN,
        PrayerType.ASR,
        PrayerType.SHOM,
        PrayerType.XUFTON
    )

    private val shortWeekDayLabels = listOf(
        lText("Du", "Ду"),
        lText("Se", "Се"),
        lText("Ch", "Чо"),
        lText("Pa", "Па"),
        lText("Ju", "Жу"),
        lText("Sh", "Ша"),
        lText("Ya", "Як")
    )

    private val fullWeekDayLabels = listOf(
        lText("Dushanba", "Душанба"),
        lText("Seshanba", "Сешанба"),
        lText("Chorshanba", "Чоршанба"),
        lText("Payshanba", "Пайшанба"),
        lText("Juma", "Жума"),
        lText("Shanba", "Шанба"),
        lText("Yakshanba", "Якшанба")
    )

    private val monthNamesLat = listOf(
        "Yan", "Fev", "Mar", "Apr", "May", "Iyn",
        "Iyl", "Avg", "Sen", "Okt", "Noy", "Dek"
    )
    private val monthNamesCyr = listOf(
        "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
        "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
    )

    fun weekBounds(
        weekOffset: Int = 0,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): Pair<String, String> {
        val monday = mondayOfWeek(weekOffset, referenceTimeMillis)
        val sunday = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(monday.time) to fmt.format(sunday.time)
    }

    fun buildSummary(
        records: List<DailyPrayerProgressEntity>,
        weekOffset: Int = 0,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): WeeklyPrayerSummary {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortDateFmt = SimpleDateFormat("dd.MM", Locale.US)

        val todayCal = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayKey = keyFmt.format(todayCal.time)

        val monday = mondayOfWeek(weekOffset, referenceTimeMillis)
        val byDay = records.associateBy { it.dayKey }

        val dailyBars = mutableListOf<DailyBarMetric>()
        val dayCalendars = mutableListOf<Calendar>()

        for (i in 0 until 7) {
            val dayCal = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            dayCalendars.add(dayCal)
            val dayKey = keyFmt.format(dayCal.time)
            val entity = byDay[dayKey]
            val completedTypes = entity?.completedTypes() ?: emptySet()
            val completedCount = completedTypes.size
            val pct = (completedCount / 5f) * 100f
            val isFuture = dayCal.timeInMillis > todayCal.timeInMillis

            dailyBars.add(
                DailyBarMetric(
                    dayKey = dayKey,
                    shortDayLabel = shortWeekDayLabels[i],
                    fullDayLabel = fullWeekDayLabels[i],
                    shortDate = shortDateFmt.format(dayCal.time),
                    completedCount = completedCount,
                    totalPrayers = 5,
                    percentage = pct,
                    isToday = dayKey == todayKey,
                    isFuture = isFuture,
                    completedTypes = completedTypes
                )
            )
        }

        val prayerBars = fiveDailyPrayers.map { type ->
            val statuses = dailyBars.map { day -> day.completedTypes.contains(type) }
            val completedDays = statuses.count { it }
            val pct = (completedDays / 7f) * 100f
            PrayerBarMetric(
                prayerType = type,
                completedDays = completedDays,
                totalDays = 7,
                percentage = pct,
                dayStatuses = statuses
            )
        }

        val totalCompleted = dailyBars.sumOf { it.completedCount }
        val totalPossible = 35
        val overallPct = (totalCompleted.toFloat() / totalPossible.toFloat()) * 100f
        val perfectDaysCount = dailyBars.count { it.completedCount == 5 }
        val bestPrayer = prayerBars
            .filter { it.completedDays > 0 }
            .maxByOrNull { it.completedDays }
            ?.prayerType

        val firstCal = dayCalendars.first()
        val lastCal = dayCalendars.last()
        val rangeLabel = formatRangeLabel(firstCal, lastCal)

        return WeeklyPrayerSummary(
            weekOffset = weekOffset,
            startDayKey = dailyBars.first().dayKey,
            endDayKey = dailyBars.last().dayKey,
            rangeLabel = rangeLabel,
            prayerBars = prayerBars,
            dailyBars = dailyBars,
            overallPercentage = overallPct,
            totalCompleted = totalCompleted,
            totalPossible = totalPossible,
            perfectDaysCount = perfectDaysCount,
            bestPrayer = bestPrayer
        )
    }

    private fun mondayOfWeek(weekOffset: Int, referenceTimeMillis: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val offsetFromMonday = when (dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            add(Calendar.DAY_OF_MONTH, -offsetFromMonday + (weekOffset * 7))
        }
    }

    private fun formatRangeLabel(start: Calendar, end: Calendar): LText {
        val d1 = start.get(Calendar.DAY_OF_MONTH)
        val m1 = start.get(Calendar.MONTH)
        val d2 = end.get(Calendar.DAY_OF_MONTH)
        val m2 = end.get(Calendar.MONTH)

        return if (m1 == m2) {
            lText(
                "$d1–$d2 ${monthNamesLat[m2]}",
                "$d1–$d2 ${monthNamesCyr[m2]}"
            )
        } else {
            lText(
                "$d1 ${monthNamesLat[m1]} – $d2 ${monthNamesLat[m2]}",
                "$d1 ${monthNamesCyr[m1]} – $d2 ${monthNamesCyr[m2]}"
            )
        }
    }
}
