package com.example

import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerTrack
import com.example.domain.model.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerEngineUnitTest {

    @Test
    fun qiblaBearing_forUzbekistanCities_isSouthwest() {
        val fargona = UzbekistanCities.findById("fargona")
        val bearing = UzbekistanCities.calculateQiblaBearing(fargona.latitude, fargona.longitude)
        assertTrue("Expected Fargona Qibla ~240-248 deg, was $bearing", bearing in 235.0..250.0)

        val toshkent = UzbekistanCities.findById("toshkent")
        val tBearing = UzbekistanCities.calculateQiblaBearing(toshkent.latitude, toshkent.longitude)
        assertTrue("Expected Toshkent Qibla ~235-248 deg, was $tBearing", tBearing in 232.0..248.0)
    }

    @Test
    fun prayerTimes_areChronologicallyOrdered() {
        val useCase = GetPrayerTimesUseCase()
        val city = UzbekistanCities.findById("toshkent")
        val summary = useCase.calculateForToday(
            city = city,
            completedToday = emptySet(),
            alarmMap = emptyMap()
        )
        assertEquals(6, summary.slots.size)
        val mins = summary.slots.map { it.hour * 60 + it.minute }
        for (i in 0 until mins.lastIndex) {
            assertTrue("Expected slot $i < ${i + 1}: $mins", mins[i] < mins[i + 1])
        }
        assertEquals(PrayerType.BOMDOD, summary.slots.first().type)
        assertEquals(PrayerType.XUFTON, summary.slots.last().type)
    }

    @Test
    fun prayerSequenceEngine_buildsCorrectRakatsAndGenderHints() {
        val engine = PrayerSequenceEngine()
        val peshinSteps = engine.buildSteps(PrayerTrack.PESHIN_FARZ, Gender.MALE)
        assertEquals(4, peshinSteps.last().rakatNumber)
        assertEquals(PrayerPose.SALAM, peshinSteps.last().expectedPose)
        assertTrue(peshinSteps.any { it.coachInstruction.latin.contains("kindik") })

        val femaleSteps = engine.buildSteps(PrayerTrack.BOMDOD_SUNNAT, Gender.FEMALE)
        assertEquals(2, femaleSteps.last().rakatNumber)
        assertTrue(femaleSteps.any { it.coachInstruction.latin.contains("ko‘krak") })

        val vitrSteps = engine.buildSteps(PrayerTrack.VITR_VOJIB, Gender.MALE)
        assertTrue(vitrSteps.any { it.recitationTitle.latin.contains("QUNUT") })
    }
}
