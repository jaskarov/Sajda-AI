package com.example.domain.model

enum class Gender(val id: String, val label: LText) {
    MALE("male", lText("Erkak", "Эркак")),
    FEMALE("female", lText("Ayol", "Аёл"));

    companion object {
        fun fromId(id: String?): Gender =
            entries.firstOrNull { it.id == id } ?: MALE
    }
}

enum class PrayerType(
    val id: String,
    val title: LText,
    val isPrayable: Boolean = true
) {
    BOMDOD("bomdod", lText("Bomdod", "Бомдод")),
    QUYOSH("quyosh", lText("Quyosh", "Қуёш"), isPrayable = false),
    PESHIN("peshin", lText("Peshin", "Пешин")),
    ASR("asr", lText("Asr", "Аср")),
    SHOM("shom", lText("Shom", "Шом")),
    XUFTON("xufton", lText("Xufton", "Хуфтон"));

    companion object {
        fun fromId(id: String?): PrayerType =
            entries.firstOrNull { it.id == id } ?: PESHIN
    }
}

enum class PrayerTrack(
    val id: String,
    val parentPrayer: PrayerType,
    val rakatCount: Int,
    val title: LText,
    val subtitle: LText
) {
    BOMDOD_SUNNAT(
        "bomdod_sunnat",
        PrayerType.BOMDOD,
        2,
        lText("Bomdod (2 rakat sunnat)", "Бомдод (2 ракат суннат)"),
        lText("2 rakat sunnat • Tonggi ibodat", "2 ракат суннат • Тонгги ибодат")
    ),
    BOMDOD_FARZ(
        "bomdod_farz",
        PrayerType.BOMDOD,
        2,
        lText("Bomdod (2 rakat farz)", "Бомдод (2 ракат фарз)"),
        lText("2 rakat farz • Tonggi farz", "2 ракат фарз • Тонгги фарз")
    ),
    PESHIN_SUNNAT_1(
        "peshin_sunnat_1",
        PrayerType.PESHIN,
        4,
        lText("Peshin (4 rakat sunnat)", "Пешин (4 ракат суннат)"),
        lText("4 rakat avvalgi sunnat", "4 ракат аввалги суннат")
    ),
    PESHIN_FARZ(
        "peshin_farz",
        PrayerType.PESHIN,
        4,
        lText("Peshin (4 rakat farz)", "Пешин (4 ракат фарз)"),
        lText("4 rakat farz • Kunduzgi farz", "4 ракат фарз • Кундузги фарз")
    ),
    PESHIN_SUNNAT_2(
        "peshin_sunnat_2",
        PrayerType.PESHIN,
        2,
        lText("Peshin (2 rakat sunnat)", "Пешин (2 ракат суннат)"),
        lText("2 rakat oxirgi sunnat", "2 ракат охирги суннат")
    ),
    ASR_FARZ(
        "asr_farz",
        PrayerType.ASR,
        4,
        lText("Asr (4 rakat farz)", "Аср (4 ракат фарз)"),
        lText("4 rakat farz • Asr namozi", "4 ракат фарз • Аср намози")
    ),
    SHOM_FARZ(
        "shom_farz",
        PrayerType.SHOM,
        3,
        lText("Shom (3 rakat farz)", "Шом (3 ракат фарз)"),
        lText("3 rakat farz • Quyosh botgach", "3 ракат фарз • Қуёш ботгач")
    ),
    SHOM_SUNNAT(
        "shom_sunnat",
        PrayerType.SHOM,
        2,
        lText("Shom (2 rakat sunnat)", "Шом (2 ракат суннат)"),
        lText("2 rakat sunnat • Shomdan keyin", "2 ракат суннат • Шомдан кейин")
    ),
    XUFTON_FARZ(
        "xufton_farz",
        PrayerType.XUFTON,
        4,
        lText("Xufton (4 rakat farz)", "Хуфтон (4 ракат фарз)"),
        lText("4 rakat farz • Kechki farz", "4 ракат фарз • Кечки фарз")
    ),
    XUFTON_SUNNAT(
        "xufton_sunnat",
        PrayerType.XUFTON,
        2,
        lText("Xufton (2 rakat sunnat)", "Хуфтон (2 ракат суннат)"),
        lText("2 rakat sunnat • Xuftondan keyin", "2 ракат суннат • Хуфтондан кейин")
    ),
    VITR_VOJIB(
        "vitr_vojib",
        PrayerType.XUFTON,
        3,
        lText("Vitr (3 rakat)", "Витр (3 ракат)"),
        lText("3 rakat vojib • Qunut duosi bilan", "3 ракат вожиб • Қунут дуоси билан")
    );

    fun nextContinuationTrack(): PrayerTrack? = when (this) {
        BOMDOD_SUNNAT -> BOMDOD_FARZ
        PESHIN_SUNNAT_1 -> PESHIN_FARZ
        PESHIN_FARZ -> PESHIN_SUNNAT_2
        SHOM_FARZ -> SHOM_SUNNAT
        XUFTON_FARZ -> XUFTON_SUNNAT
        XUFTON_SUNNAT -> VITR_VOJIB
        else -> null
    }

    companion object {
        fun fromId(id: String?): PrayerTrack =
            entries.firstOrNull { it.id == id } ?: PESHIN_FARZ

        fun defaultForPrayer(prayerType: PrayerType): PrayerTrack = when (prayerType) {
            PrayerType.BOMDOD -> BOMDOD_SUNNAT
            PrayerType.QUYOSH, PrayerType.PESHIN -> PESHIN_SUNNAT_1
            PrayerType.ASR -> ASR_FARZ
            PrayerType.SHOM -> SHOM_FARZ
            PrayerType.XUFTON -> XUFTON_FARZ
        }

        fun tracksForPrayer(prayerType: PrayerType): List<PrayerTrack> = when (prayerType) {
            PrayerType.BOMDOD -> listOf(BOMDOD_SUNNAT, BOMDOD_FARZ)
            PrayerType.QUYOSH, PrayerType.PESHIN -> listOf(PESHIN_SUNNAT_1, PESHIN_FARZ, PESHIN_SUNNAT_2)
            PrayerType.ASR -> listOf(ASR_FARZ)
            PrayerType.SHOM -> listOf(SHOM_FARZ, SHOM_SUNNAT)
            PrayerType.XUFTON -> listOf(XUFTON_FARZ, XUFTON_SUNNAT, VITR_VOJIB)
        }
    }
}

enum class QazoPrayerType(
    val id: String,
    val title: LText,
    val rakatSummary: LText
) {
    BOMDOD("bomdod", lText("Bomdod", "Бомдод"), lText("2 rakat farz", "2 ракат фарз")),
    PESHIN("peshin", lText("Peshin", "Пешин"), lText("4 rakat farz", "4 ракат фарз")),
    ASR("asr", lText("Asr", "Аср"), lText("4 rakat farz", "4 ракат фарз")),
    SHOM("shom", lText("Shom", "Шом"), lText("3 rakat farz", "3 ракат фарз")),
    XUFTON("xufton", lText("Xufton", "Хуфтон"), lText("4 rakat farz", "4 ракат фарз")),
    VITR("vitr", lText("Vitr", "Витр"), lText("3 rakat vojib", "3 ракат вожиб"));

    fun toPrayerTrack(): PrayerTrack = when (this) {
        BOMDOD -> PrayerTrack.BOMDOD_FARZ
        PESHIN -> PrayerTrack.PESHIN_FARZ
        ASR -> PrayerTrack.ASR_FARZ
        SHOM -> PrayerTrack.SHOM_FARZ
        XUFTON -> PrayerTrack.XUFTON_FARZ
        VITR -> PrayerTrack.VITR_VOJIB
    }

    companion object {
        fun fromId(id: String?): QazoPrayerType? =
            entries.firstOrNull { it.id == id }
    }
}

enum class PrayerPose(
    val title: LText,
    val emoji: String
) {
    UNKNOWN(lText("Aniqlanmoqda", "Аниқланмоқда"), "🔍"),
    TAKBIR(lText("TAKBIR", "ТАКБИР"), "🙌"),
    QIYOM(lText("QIYOM", "ҚИЁМ"), "🧍"),
    RUKU(lText("RUKU", "РУКУ"), "🙇"),
    QAVMA(lText("QAVMA", "ҚАВМА"), "🧍"),
    SAJDA(lText("SAJDA", "САЖДА"), "🤲"),
    JALSA(lText("JALSA", "ЖАЛСА"), "🧎"),
    QADA(lText("QADA", "ҚАЪДА"), "🧎"),
    SALAM(lText("SALOM", "САЛОМ"), "✨")
}

enum class CoachMode {
    CAMERA_AI,
    AUDIO_ONLY
}

enum class FallbackReason {
    NONE,
    CAMERA_PERMISSION_DENIED,
    LOW_LIGHT_DARK_ROOM,
    USER_SELECTED
}

data class UzbekCity(
    val id: String,
    val name: LText,
    val region: LText,
    val latitude: Double,
    val longitude: Double,
    val fajrOffsetMin: Int = 0,
    val maghribOffsetMin: Int = 0
)

data class PrayerTimeSlot(
    val type: PrayerType,
    val timeFormatted: String,
    val hour: Int,
    val minute: Int,
    val isPassed: Boolean,
    val isCurrent: Boolean,
    val isCompletedToday: Boolean,
    val alarmEnabled: Boolean
)

data class PrayerScheduleSummary(
    val city: UzbekCity,
    val nextPrayer: PrayerType,
    val nextPrayerTimeFormatted: String,
    val currentPrayer: PrayerType,
    val countdownSeconds: Long,
    val slots: List<PrayerTimeSlot>,
    val hijriLabel: LText,
    val qiblaBearingDegrees: Double
)

data class PrayerStep(
    val index: Int,
    val rakatNumber: Int,
    val totalRakats: Int,
    val expectedPose: PrayerPose,
    val stepTitle: LText,
    val recitationTitle: LText,
    val arabicText: String,
    val recitationLatin: String,
    val recitationCyrillic: String,
    val meaning: LText,
    val coachInstruction: LText,
    val ttsPrompt: String,
    val minHoldDurationMs: Long,
    val requiresPoseTransition: Boolean = true,
    val audioAssetId: String = "",
    val postRecitationCue: LText? = null
) {
    fun getWords(script: TextScript): List<String> {
        val text = when (script) {
            TextScript.LATIN -> recitationLatin
            TextScript.CYRILLIC -> recitationCyrillic
        }
        return text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }
}

data class NormalizedLandmark(
    val type: Int,
    val x: Float,
    val y: Float,
    val inFrameLikelihood: Float
)

data class PoseAnalysisSnapshot(
    val confirmedPose: PrayerPose = PrayerPose.UNKNOWN,
    val rawPose: PrayerPose = PrayerPose.UNKNOWN,
    val torsoAngle: Float = 0f,
    val kneeAngle: Float = 0f,
    val headHeightRatio: Float = 0f,
    val isFullBodyVisible: Boolean = false,
    val calibrationHint: LText = lText(
        "To'liq bo'yingiz ko'rinishi uchun telefondan 1.5-2 metr orqada turing",
        "Тўлиқ бўйингиз кўриниши учун телефондан 1.5-2 метр орқада туринг"
    ),
    val averageLuminance: Float = 128f,
    val isLowLight: Boolean = false,
    val landmarks: List<NormalizedLandmark> = emptyList(),
    val imageWidth: Int = 480,
    val imageHeight: Int = 640,
    val isFrontCamera: Boolean = true,
    val headYawDirection: Int = 0
)

data class SurahLibraryItem(
    val id: String,
    val title: LText,
    val subtitle: LText,
    val arabicText: String,
    val translitLatin: String,
    val translitCyrillic: String,
    val meaning: LText,
    val ttsPrompt: String,
    val audioAssetId: String = id
)

data class PostureGuideItem(
    val id: String,
    val pose: PrayerPose,
    val title: LText,
    val angleSpec: String,
    val description: LText,
    val femaleNote: LText,
    val maleNote: LText,
    val ttsPrompt: String
)

enum class PurificationCategory(val id: String, val title: LText) {
    WUDU("wudu", lText("Tahorat tartibi (8 qadam)", "Таҳорат тартиби (8 қадам)")),
    WUDU_BREAKERS("wudu_breakers", lText("Tahoratni buzadigan holatlar", "Таҳоратни бузадиган ҳолатлар")),
    GHUSL_TAYAMMUM("ghusl_tayammum", lText("G‘usl va Tayammum", "Ғусл ва Таяммум"))
}

data class PurificationGuideItem(
    val id: String,
    val category: PurificationCategory,
    val stepNumber: Int = 0,
    val isFarz: Boolean = false,
    val title: LText,
    val ruleBadge: LText,
    val description: LText,
    val ttsPrompt: String
)
