package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.domain.model.PrayerType

/**
 * Legacy AlarmManager cleanup helper so old alarms are canceled when migrated to WorkManager.
 */
class PrayerAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun cancelSlot(type: PrayerType) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.ordinal + 100,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
