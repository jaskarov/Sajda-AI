package com.example.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.ReminderSoundMode
import com.example.domain.model.TextScript
import com.example.domain.model.lText

object PrayerNotificationHelper {

    const val CHANNEL_ID = "sajda_prayer_alarms"
    const val CHANNEL_ID_TAKBIR = "sajda_prayer_alarms_takbir"
    const val CHANNEL_ID_VIBRATE = "sajda_prayer_alarms_vibrate"

    private val GENTLE_VIBRATION_PATTERN = longArrayOf(0L, 350L, 200L, 350L)

    fun channelIdFor(soundMode: ReminderSoundMode): String = when (soundMode) {
        ReminderSoundMode.TAKBIR -> CHANNEL_ID_TAKBIR
        ReminderSoundMode.DEFAULT_TONE -> CHANNEL_ID
        ReminderSoundMode.VIBRATE_ONLY -> CHANNEL_ID_VIBRATE
    }

    fun takbirSoundUri(context: Context): Uri = Uri.parse(
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.takbir}"
    )

    fun ensureChannel(
        context: Context,
        script: TextScript = TextScript.LATIN,
        soundMode: ReminderSoundMode = ReminderSoundMode.TAKBIR
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channelId = channelIdFor(soundMode)
            val channelName = when (soundMode) {
                ReminderSoundMode.TAKBIR -> lText(
                    "Namoz vaqtlari (Qori Takbiri)",
                    "Намоз вақтлари (Қори Такбири)"
                ).get(script)
                ReminderSoundMode.DEFAULT_TONE -> lText(
                    "Namoz vaqtlari eslatmasi",
                    "Намоз вақтлари эслатмаси"
                ).get(script)
                ReminderSoundMode.VIBRATE_ONLY -> lText(
                    "Namoz vaqtlari (Faqat tebranish)",
                    "Намоз вақтлари (Фақат тебраниш)"
                ).get(script)
            }
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = lText(
                    "O‘zbekiston taqvimi bo‘yicha besh vaqt namoz bildirishnomalari",
                    "Ўзбекистон тақвими бўйича беш вақт намоз билдиришномалари"
                ).get(script)
                enableVibration(true)
                vibrationPattern = GENTLE_VIBRATION_PATTERN
                when (soundMode) {
                    ReminderSoundMode.TAKBIR -> {
                        val attrs = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        setSound(takbirSoundUri(context), attrs)
                    }
                    ReminderSoundMode.DEFAULT_TONE -> {
                        // Uses system default notification sound
                    }
                    ReminderSoundMode.VIBRATE_ONLY -> {
                        setSound(null, null)
                    }
                }
            }
            manager.createNotificationChannel(channel)
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
        script: TextScript = TextScript.LATIN,
        soundMode: ReminderSoundMode = ReminderSoundMode.TAKBIR
    ): Boolean {
        ensureChannel(context, script, soundMode)
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

        val builder = NotificationCompat.Builder(context, channelIdFor(soundMode))
            .setSmallIcon(R.drawable.ic_notification_prayer)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(GENTLE_VIBRATION_PATTERN)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        when (soundMode) {
            ReminderSoundMode.TAKBIR -> builder.setSound(takbirSoundUri(context))
            ReminderSoundMode.DEFAULT_TONE -> builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            ReminderSoundMode.VIBRATE_ONLY -> builder.setSilent(false).setSound(null)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        return true
    }
}
