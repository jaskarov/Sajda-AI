package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.preferences.ReminderSoundMode
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript

/**
 * Schedules exact background Azan / prayer reminders using AlarmManager.setExactAndAllowWhileIdle
 * on Android 12+ when SCHEDULE_EXACT_ALARM is permitted, with automatic fallback to
 * setAndAllowWhileIdle when exact alarm permission has not been granted yet.
 */
class PrayerAlarmScheduler(private val context: Context) {

    companion object {
        private const val TAG = "PrayerAlarmScheduler"
        const val REQUEST_CODE_BASE = 100
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleExactPrayer(
        type: PrayerType,
        triggerMillis: Long,
        prayerName: String,
        prayerTime: String,
        cityName: String,
        script: TextScript,
        soundMode: ReminderSoundMode = ReminderSoundMode.TAKBIR
    ) {
        if (!type.isPrayable) return
        val pendingIntent = buildPendingIntent(
            type = type,
            prayerName = prayerName,
            prayerTime = prayerTime,
            cityName = cityName,
            script = script,
            soundMode = soundMode,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val scheduledExact = if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                true
            } catch (sec: SecurityException) {
                Log.w(TAG, "Exact alarm denied at runtime, falling back to setAndAllowWhileIdle", sec)
                false
            }
        } else {
            false
        }

        if (!scheduledExact) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun isSlotScheduled(type: PrayerType): Boolean {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + type.ordinal,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }

    fun cancelSlot(type: PrayerType) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + type.ordinal,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildPendingIntent(
        type: PrayerType,
        prayerName: String,
        prayerTime: String,
        cityName: String,
        script: TextScript,
        soundMode: ReminderSoundMode,
        flags: Int
    ): PendingIntent? {
        val requestCode = REQUEST_CODE_BASE + type.ordinal
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE_ID, type.id)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayerTime)
            putExtra(PrayerAlarmReceiver.EXTRA_CITY_NAME, cityName)
            putExtra(PrayerAlarmReceiver.EXTRA_SCRIPT_ID, script.id)
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_MODE_ID, soundMode.id)
            putExtra(PrayerAlarmReceiver.EXTRA_REQUEST_CODE, requestCode)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
