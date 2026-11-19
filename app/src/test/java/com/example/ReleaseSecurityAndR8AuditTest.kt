package com.example

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BackupRestoreResult
import com.example.data.local.PrayerHistoryRepository
import com.example.data.local.QazoPrayerRepository
import com.example.data.local.SajdaBackupManager
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseSecurityAndR8AuditTest {

    @Test
    fun manifestSecurity_doesNotDeclareUseExactAlarmAndEnforcesCleartextBlock() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val manifestXml = if (manifestFile.exists()) manifestFile.readText() else File("app/src/main/AndroidManifest.xml").readText()

        assertFalse(
            "USE_EXACT_ALARM is prohibited by Google Play Store policy for non-clock apps",
            manifestXml.contains("android.permission.USE_EXACT_ALARM")
        )
        assertTrue(
            "SCHEDULE_EXACT_ALARM must remain declared for prayer azan scheduling",
            manifestXml.contains("android.permission.SCHEDULE_EXACT_ALARM")
        )
        assertTrue(
            "Cleartext traffic must be explicitly denied",
            manifestXml.contains("android:usesCleartextTraffic=\"false\"")
        )
        assertTrue(
            "Network security config must be referenced",
            manifestXml.contains("android:networkSecurityConfig=\"@xml/network_security_config\"")
        )
        assertTrue(
            "Boot receiver must handle TIME_SET to reschedule alarms after clock adjustments",
            manifestXml.contains("android.intent.action.TIME_SET")
        )
        assertTrue(
            "Boot receiver must handle TIMEZONE_CHANGED",
            manifestXml.contains("android.intent.action.TIMEZONE_CHANGED")
        )
        assertTrue(
            "Boot receiver must handle MY_PACKAGE_REPLACED after Play Store updates",
            manifestXml.contains("android.intent.action.MY_PACKAGE_REPLACED")
        )
    }

    @Test
    fun proguardR8Rules_protectsDataRemoteDtosAndRetrofit() {
        val rulesFile = File("proguard-rules.pro")
        val rulesText = if (rulesFile.exists()) rulesFile.readText() else File("app/proguard-rules.pro").readText()

        assertTrue(
            "ProGuard rules must keep data.remote DTOs to prevent R8 from breaking IslomAPI / Aladhan sync",
            rulesText.contains("com.example.data.remote.**")
        )
        assertTrue(
            "ProGuard rules must protect Retrofit interface methods",
            rulesText.contains("@retrofit2.http.* <methods>;")
        )
    }

    @Test
    fun backupManager_rejectsPayloadExceedingTwoMegabytes() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, SajdaDatabase::class.java).build()
        val prefsRepo = UserPreferencesRepository(app)
        val historyRepo = PrayerHistoryRepository(db.prayerHistoryDao(), db.dailyPrayerProgressDao())
        val qazoRepo = QazoPrayerRepository(db.qazoPrayerDao())
        val manager = SajdaBackupManager(prefsRepo, historyRepo, qazoRepo)

        // Generate synthetic payload larger than 2 MB
        val oversizePayload = "{\"appId\":\"sajda_ai\",\"data\":\"" + "A".repeat(2 * 1024 * 1024 + 100) + "\"}"
        val result = manager.importFromJson(oversizePayload)

        assertTrue("Payload > 2MB must be rejected", result is BackupRestoreResult.Failure)
        val failure = result as BackupRestoreResult.Failure
        assertTrue(failure.reason.contains("2 MB"))
        db.close()
    }

    @Test
    fun roomDatabase_migrationFiveToSix_createsIndexesOnPrayerHistory() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, SajdaDatabase::class.java)
            .addMigrations(
                SajdaDatabase.MIGRATION_1_2,
                SajdaDatabase.MIGRATION_2_3,
                SajdaDatabase.MIGRATION_3_4,
                SajdaDatabase.MIGRATION_4_5,
                SajdaDatabase.MIGRATION_5_6
            )
            .build()

        val openHelper = db.openHelper.writableDatabase
        val cursor = openHelper.query("PRAGMA index_list(`prayer_history`)")
        val indexNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            indexNames.add(name)
        }
        cursor.close()

        assertTrue(
            "index_prayer_history_completedAtMillis must exist",
            indexNames.any { it.contains("completedAtMillis") }
        )
        assertTrue(
            "index_prayer_history_dayKey must exist",
            indexNames.any { it.contains("dayKey") }
        )
        db.close()
    }
}
