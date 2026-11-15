package com.example.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.TextScript
import com.example.domain.model.lText

object PrayerNotificationHelper {

    const val CHANNEL_ID = "sajda_prayer_alarms"

    fun ensureChannel(context: Context, script: TextScript = TextScript.LATIN) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                lText("Namoz vaqtlari eslatmasi", "Намоз вақтлари эслатмаси").get(script),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = lText(
                    "O‘zbekiston taqvimi bo‘yicha besh vaqt namoz bildirishnomalari",
                    "Ўзбекистон тақвими бўйича беш вақт намоз билдиришномалари"
                ).get(script)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showPrayerNotification(
        context: Context,
        notificationId: Int,
        prayerName: String,
        prayerTime: String,
        cityName: String,
        script: TextScript = TextScript.LATIN
    ): Boolean {
        ensureChannel(context, script)
        if (!hasNotificationPermission(context)) return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = lText(
            "$prayerName vaqti kirdi ($prayerTime)",
            "$prayerName вақти кирди ($prayerTime)"
        ).get(script)
        val body = lText(
            "$cityName • AI-ustoz bilan namozni boshlash uchun bosing",
            "$cityName • AI-устоз билан намозни бошлаш учун босинг"
        ).get(script)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }
}
