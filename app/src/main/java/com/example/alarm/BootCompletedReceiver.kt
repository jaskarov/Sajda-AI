package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val prefs = UserPreferencesRepository(appContext)
                val settings = prefs.settingsFlow.first()
                PrayerWorkScheduler(appContext).syncAllPrayers(settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
