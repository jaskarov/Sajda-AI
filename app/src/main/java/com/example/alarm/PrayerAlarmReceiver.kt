package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.PrayerCalendarRepository
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = PrayerNotificationHelper.CHANNEL_ID
        const val EXTRA_PRAYER_TYPE_ID = "extra_prayer_type_id"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_SCRIPT_ID = "extra_script_id"
        const val EXTRA_SOUND_MODE_ID = "extra_sound_mode_id"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val typeId = intent.getStringExtra(EXTRA_PRAYER_TYPE_ID)
        val prayerType = PrayerType.fromId(typeId)
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: prayerType.title.latin
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: "O‘zbekiston"
        val script = TextScript.fromId(intent.getStringExtra(EXTRA_SCRIPT_ID))
        val soundMode = com.example.data.preferences.ReminderSoundMode.fromId(
            intent.getStringExtra(EXTRA_SOUND_MODE_ID)
        )
        val notificationId = intent.getIntExtra(EXTRA_REQUEST_CODE, 101)

        PrayerNotificationHelper.showPrayerNotification(
            context = context,
            notificationId = notificationId,
            prayerName = prayerName,
            prayerTime = prayerTime,
            cityName = cityName,
            script = script,
            soundMode = soundMode
        )

        if (prayerType.isPrayable) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appContext = context.applicationContext
                    val prefs = UserPreferencesRepository(appContext)
                    val settings = prefs.settingsFlow.first()
                    val calendarRepo = PrayerCalendarRepository(
                        appContext,
                        SajdaDatabase.getInstance(appContext).prayerCalendarDao()
                    )
                    val (todayDay, tomorrowDay) = calendarRepo.getTodayAndTomorrow(settings.selectedCityId)
                    PrayerWorkScheduler(appContext).scheduleNextOccurrence(
                        prayerType = prayerType,
                        settings = settings,
                        todayCalendarDay = todayDay,
                        tomorrowCalendarDay = tomorrowDay
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
