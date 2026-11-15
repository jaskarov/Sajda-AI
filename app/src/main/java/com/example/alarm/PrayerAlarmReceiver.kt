package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = PrayerNotificationHelper.CHANNEL_ID
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Namoz"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: "O'zbekiston"
        val notificationId = intent.getIntExtra(EXTRA_REQUEST_CODE, 101)

        PrayerNotificationHelper.showPrayerNotification(
            context = context,
            notificationId = notificationId,
            prayerName = prayerName,
            prayerTime = prayerTime,
            cityName = cityName
        )
    }
}
