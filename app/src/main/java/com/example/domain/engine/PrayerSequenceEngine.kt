package com.example.domain.engine

import com.example.domain.model.Gender
import com.example.domain.model.LText
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerStep
import com.example.domain.model.PrayerTrack
import com.example.domain.model.QazoPrayerType
import com.example.domain.model.lText

class PrayerSequenceEngine {

    fun buildNiyatText(track: PrayerTrack, qazoType: QazoPrayerType? = null): LText {
        if (qazoType != null) {
            val kindLat = if (qazoType == QazoPrayerType.VITR) "3 rakat vojibini" else "${track.rakatCount} rakat farzini"
            val kindCyr = if (qazoType == QazoPrayerType.VITR) "3 ракат вожибини" else "${track.rakatCount} ракат фарзини"
            return lText(
                "Niyat: Alloh rizoligi uchun zimmamdagi qazo ${qazoType.title.latin} namozining $kindLat o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун зиммамдаги қазо ${qazoType.title.cyrillic} намозининг $kindCyr ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
        }
        return when (track) {
            PrayerTrack.BOMDOD_SUNNAT -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Bomdod namozining 2 rakat sunnatini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Бомдод намозининг 2 ракат суннатини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.BOMDOD_FARZ -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Bomdod namozining 2 rakat farzini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Бомдод намозининг 2 ракат фарзини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.PESHIN_SUNNAT_1 -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Peshin namozining 4 rakat avvalgi sunnatini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Пешин намозининг 4 ракат аввалги суннатини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.PESHIN_FARZ -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Peshin namozining 4 rakat farzini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Пешин намозининг 4 ракат фарзини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.PESHIN_SUNNAT_2 -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Peshin namozining 2 rakat oxirgi sunnatini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Пешин намозининг 2 ракат охирги суннатини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.ASR_FARZ -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Asr namozining 4 rakat farzini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Аср намозининг 4 ракат фарзини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.SHOM_FARZ -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Shom namozining 3 rakat farzini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Шом намозининг 3 ракат фарзини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.SHOM_SUNNAT -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Shom namozining 2 rakat sunnatini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Шом намозининг 2 ракат суннатини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.XUFTON_FARZ -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Xufton namozining 4 rakat farzini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Хуфтон намозининг 4 ракат фарзини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.XUFTON_SUNNAT -> lText(
                "Niyat: Alloh rizoligi uchun bugungi Xufton namozining 2 rakat sunnatini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги Хуфтон намозининг 2 ракат суннатини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
            PrayerTrack.VITR_VOJIB -> lText(
                "Niyat: Alloh rizoligi uchun bugungi 3 rakat Vitr vojib namozini o‘qishni niyat qildim, yuzimni qiblaga qaratdim.",
                "Ният: Аллоҳ ризолиги учун бугунги 3 ракат Витр вожиб намозини ўқишни ният қилдим, юзимни қиблага қаратдим."
            )
        }
    }

    fun buildSteps(
        track: PrayerTrack,
        gender: Gender,
        qazoType: QazoPrayerType? = null
    ): List<PrayerStep> {
        val steps = mutableListOf<PrayerStep>()
        var idx = 0

        val sano = PrayerContentCatalog.findSurahById("sano")
        val fotiha = PrayerContentCatalog.findSurahById("fotiha")
        val kavsar = PrayerContentCatalog.findSurahById("kavsar")
        val ixlos = PrayerContentCatalog.findSurahById("ixlos")
        val falaq = PrayerContentCatalog.findSurahById("falaq")
        val attahiyat = PrayerContentCatalog.findSurahById("attahiyat")
        val salovat = PrayerContentCatalog.findSurahById("salovat")
        val robbana = PrayerContentCatalog.findSurahById("robbana")
        val qunut = PrayerContentCatalog.findSurahById("qunut")

        val qiyomHandsHintLat = if (gender == Gender.MALE) {
            "O‘ng qo‘lni chap qo‘l ustiga qo‘yib kindik ostida bog‘lang"
        } else {
            "O‘ng qo‘lni chap qo‘l ustiga qo‘yib ko‘krak ustiga qo‘ying"
        }
        val qiyomHandsHintCyr = if (gender == Gender.MALE) {
            "Ўнг қўлни чап қўл устига қўйиб киндик остида боғланг"
        } else {
            "Ўнг қўлни чап қўл устига қўйиб кўкрак устига қўйинг"
        }

        val takbirHintLat = if (gender == Gender.MALE) {
            "Qo‘llaringizni quloq yumshog‘igacha ko‘tarib «Allohu akbar» deng"
        } else {
            "Qo‘llaringizni yelka barobarigacha ko‘tarib «Allohu akbar» deng"
        }
        val takbirHintCyr = if (gender == Gender.MALE) {
            "Қўлларингизни қулоқ юмшоғигача кўтариб «Аллоҳу акбар» денг"
        } else {
            "Қўлларингизни елка баробаригача кўтариб «Аллоҳу акбар» денг"
        }

        val niyat = buildNiyatText(track, qazoType)
        val totalRakats = track.rakatCount
        val isFarz = track == PrayerTrack.BOMDOD_FARZ ||
            track == PrayerTrack.PESHIN_FARZ ||
            track == PrayerTrack.ASR_FARZ ||
            track == PrayerTrack.SHOM_FARZ ||
            track == PrayerTrack.XUFTON_FARZ
        val isVitr = track == PrayerTrack.VITR_VOJIB

        for (rakat in 1..totalRakats) {
            if (rakat == 1) {
                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = 1,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.TAKBIR,
                    stepTitle = lText("Takbiri tahrima", "Такбири таҳрима"),
                    recitationTitle = lText("NIYAT VA TAKBIR", "НИЯТ ВА ТАКБИР"),
                    arabicText = "اللَّهُ أَكْبَرُ",
                    recitationLatin = "Allohu akbar.",
                    recitationCyrillic = "Аллоҳу акбар.",
                    meaning = niyat,
                    coachInstruction = lText(takbirHintLat, takbirHintCyr),
                    ttsPrompt = "${niyat.latin} Allohu akbar.",
                    minHoldDurationMs = 4500L,
                    audioAssetId = "takbir"
                )

                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = 1,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.QIYOM,
                    stepTitle = lText("Qiyom • Sano", "Қиём • Сано"),
                    recitationTitle = lText("SANO DUOSI", "САНО ДУОСИ"),
                    arabicText = sano.arabicText,
                    recitationLatin = sano.translitLatin,
                    recitationCyrillic = sano.translitCyrillic,
                    meaning = sano.meaning,
                    coachInstruction = lText(
                        "$qiyomHandsHintLat va Sano duosini o‘qing",
                        "$qiyomHandsHintCyr ва Сано дуосини ўқинг"
                    ),
                    ttsPrompt = sano.ttsPrompt,
                    minHoldDurationMs = 7000L,
                    audioAssetId = sano.audioAssetId,
                    postRecitationCue = lText("Endi Fotiha surasini o‘qing", "Энди Фотиҳа сурасини ўқинг")
                )
            }

            val hasZamSura = rakat <= 2 || !isFarz

            // Fotiha in every rakat
            steps += PrayerStep(
                index = idx++,
                rakatNumber = rakat,
                totalRakats = totalRakats,
                expectedPose = PrayerPose.QIYOM,
                stepTitle = lText("$rakat-Rakat • Qiyom", "$rakat-Ракат • Қиём"),
                recitationTitle = lText("FOTIHA SURASI", "ФОТИҲА СУРАСИ"),
                arabicText = fotiha.arabicText,
                recitationLatin = fotiha.translitLatin,
                recitationCyrillic = fotiha.translitCyrillic,
                meaning = fotiha.meaning,
                coachInstruction = lText(
                    if (hasZamSura) "Fotiha surasidan so‘ng zam sura o‘qiladi"
                    else "Sura tugagach, rukuga egiling",
                    if (hasZamSura) "Фотиҳа сурасидан сўнг зам сура ўқилади"
                    else "Сура тугагач, рукуга эгилинг"
                ),
                ttsPrompt = if (rakat > 1) {
                    "Allohu akbar. ${fotiha.ttsPrompt}"
                } else {
                    fotiha.ttsPrompt
                },
                minHoldDurationMs = 15000L,
                requiresPoseTransition = rakat > 1,
                audioAssetId = if (rakat > 1) {
                    "takbir,${fotiha.audioAssetId}"
                } else {
                    fotiha.audioAssetId
                },
                postRecitationCue = if (hasZamSura) {
                    lText("Endi zam surani o‘qing", "Энди зам сурани ўқинг")
                } else {
                    lText("Endi rukuga egiling", "Энди рукуга эгилинг")
                }
            )

            // Zam sura in rakats 1 & 2, or all rakats for sunnat/vitr
            if (hasZamSura) {
                val zamSura = when (rakat) {
                    1 -> kavsar
                    2 -> ixlos
                    else -> falaq
                }
                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = rakat,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.QIYOM,
                    stepTitle = lText("$rakat-Rakat • Zam sura", "$rakat-Ракат • Зам сура"),
                    recitationTitle = lText(
                        zamSura.title.latin.uppercase(),
                        zamSura.title.cyrillic.uppercase()
                    ),
                    arabicText = zamSura.arabicText,
                    recitationLatin = zamSura.translitLatin,
                    recitationCyrillic = zamSura.translitCyrillic,
                    meaning = zamSura.meaning,
                    coachInstruction = lText(
                        when {
                            isVitr && rakat == 3 -> "Sura tugagach, Qunut takbiri uchun qo‘llaringizni ko‘taring"
                            rakat == 4 -> "4-rakat sunnat zam surasi (Falaq yoki Nos o‘qish mumkin). So‘ng rukuga egiling"
                            else -> "Sura tugagach, rukuga egiling"
                        },
                        when {
                            isVitr && rakat == 3 -> "Сура тугагач, Қунут такбири учун қўлларингизни кўтаринг"
                            rakat == 4 -> "4-ракат суннат зам сураси (Фалақ ёки Нос ўқиш мумкин). Сўнг рукуга эгилинг"
                            else -> "Сура тугагач, рукуга эгилинг"
                        }
                    ),
                    ttsPrompt = zamSura.ttsPrompt,
                    minHoldDurationMs = 9000L,
                    requiresPoseTransition = false,
                    audioAssetId = zamSura.audioAssetId,
                    postRecitationCue = if (isVitr && rakat == 3) {
                        lText("Endi Qunut takbirini ayting", "Энди Қунут такбирини айтинг")
                    } else {
                        lText("Endi rukuga egiling", "Энди рукуга эгилинг")
                    }
                )
            }

            // Hanafi Vitr 3rd Rakat: Qunut Takbir (raise hands) followed by Qunut Duosi
            if (isVitr && rakat == 3) {
                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = 3,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.TAKBIR,
                    stepTitle = lText("3-Rakat • Qunut takbiri", "3-Ракат • Қунут такбири"),
                    recitationTitle = lText("QUNUT TAKBIRI", "ҚУНУТ ТАКБИРИ"),
                    arabicText = "اللَّهُ أَكْبَرُ",
                    recitationLatin = "Allohu akbar.",
                    recitationCyrillic = "Аллоҳу акбар.",
                    meaning = lText(
                        "Qunut duosidan oldin qo‘llarni qaytadan ko‘tarib aytiladigan vojib takbir.",
                        "Қунут дуосидан олдин қўлларни қайтадан кўтариб айтиладиган вожиб такбир."
                    ),
                    coachInstruction = lText(
                        "$takbirHintLat, so‘ng qo‘llarni bog‘lang",
                        "$takbirHintCyr, сўнг қўлларни боғланг"
                    ),
                    ttsPrompt = "Allohu akbar.",
                    minHoldDurationMs = 3500L,
                    requiresPoseTransition = true,
                    audioAssetId = "takbir"
                )

                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = 3,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.QIYOM,
                    stepTitle = lText("3-Rakat • Qunut duosi", "3-Ракат • Қунут дуоси"),
                    recitationTitle = lText("QUNUT DUOSI", "ҚУНУТ ДУОСИ"),
                    arabicText = qunut.arabicText,
                    recitationLatin = qunut.translitLatin,
                    recitationCyrillic = qunut.translitCyrillic,
                    meaning = qunut.meaning,
                    coachInstruction = lText(
                        "$qiyomHandsHintLat va Qunut duosini o‘qing",
                        "$qiyomHandsHintCyr ва Қунут дуосини ўқинг"
                    ),
                    ttsPrompt = qunut.ttsPrompt,
                    minHoldDurationMs = 15000L,
                    requiresPoseTransition = false,
                    audioAssetId = qunut.audioAssetId,
                    postRecitationCue = lText("Endi rukuga egiling", "Энди рукуга эгилинг")
                )
            }

            // Ruku
            steps += PrayerStep(
                index = idx++,
                rakatNumber = rakat,
                totalRakats = totalRakats,
                expectedPose = PrayerPose.RUKU,
                stepTitle = lText("$rakat-Rakat • Ruku", "$rakat-Ракат • Руку"),
                recitationTitle = lText("RUKU TASBEHI", "РУКУ ТАСБЕҲИ"),
                arabicText = "سُبْحَانَ رَبِّيَ الْعَظِيمِ (٣×)",
                recitationLatin = "Allohu akbar. Subhana Robbiyal ‘Aziym. Subhana Robbiyal ‘Aziym. Subhana Robbiyal ‘Aziym.",
                recitationCyrillic = "Аллоҳу акбар. Субҳана Роббиял Ъазийм. Субҳана Роббиял Ъазийм. Субҳана Роббиял Ъазийм.",
                meaning = lText(
                    "Ulug‘ Robbim barcha nuqsonlardan pokdir (3 marta).",
                    "Улуғ Роббим барча нуқсонлардан покдир (3 марта)."
                ),
                coachInstruction = lText(
                    "Belni tekis tutib egiling. Tasbehdan so‘ng tik turing",
                    "Белни текис тутиб эгилинг. Тасбеҳдан сўнг тик туринг"
                ),
                ttsPrompt = "Allohu akbar. Subhana Robbiyal Aziym. Subhana Robbiyal Aziym. Subhana Robbiyal Aziym.",
                minHoldDurationMs = 7000L,
                audioAssetId = "ruku"
            )

            // Qavma
            steps += PrayerStep(
                index = idx++,
                rakatNumber = rakat,
                totalRakats = totalRakats,
                expectedPose = PrayerPose.QAVMA,
                stepTitle = lText("$rakat-Rakat • Qavma", "$rakat-Ракат • Қавма"),
                recitationTitle = lText("RUKUDAN TURISH", "РУКУДАН ТУРИШ"),
                arabicText = "سَمِعَ اللَّهُ لِمَنْ حَمِدَهُ ۝ رَبَّنَا وَلَكَ الْحَمْدُ",
                recitationLatin = "Sami‘allohu liman hamidah. Robbana lakal hamd.",
                recitationCyrillic = "Самиъаллоҳу лиман ҳамидаҳ. Роббана лакал ҳамд.",
                meaning = lText(
                    "Alloh O‘ziga hamd aytuvchini eshitdi. Robbimiz, hamd Sengadir.",
                    "Аллоҳ Ўзига ҳамд айтувчини эшитди. Роббимиз, ҳамд Сенгадир."
                ),
                coachInstruction = lText(
                    "Tik turib rostlangach, «Allohu akbar» deb sajdaga boring",
                    "Тик туриб ростлангач, «Аллоҳу акбар» деб саждага боринг"
                ),
                ttsPrompt = "Sami'allohu liman hamidah. Robbana lakal hamd.",
                minHoldDurationMs = 5000L,
                audioAssetId = "qavma"
            )

            // 1st Sajda
            steps += PrayerStep(
                index = idx++,
                rakatNumber = rakat,
                totalRakats = totalRakats,
                expectedPose = PrayerPose.SAJDA,
                stepTitle = lText("$rakat-Rakat • 1-Sajda", "$rakat-Ракат • 1-Сажда"),
                recitationTitle = lText("SAJDA TASBEHI", "САЖДА ТАСБЕҲИ"),
                arabicText = "سُبْحَانَ رَبِّيَ الْأَعْلَى (٣×)",
                recitationLatin = "Allohu akbar. Subhana Robbiyal A‘la. Subhana Robbiyal A‘la. Subhana Robbiyal A‘la.",
                recitationCyrillic = "Аллоҳу акбар. Субҳана Роббиял Аъла. Субҳана Роббиял Аъла. Субҳана Роббиял Аъла.",
                meaning = lText(
                    "Eng oliy Robbim barcha nuqsonlardan pokdir (3 marta).",
                    "Энг олий Роббим барча нуқсонлардан покдир (3 марта)."
                ),
                coachInstruction = lText(
                    "Peshona va burunni joynamozga qo‘ying. So‘ng o‘tiring",
                    "Пешона ва бурунни жойнамозга қўйинг. Сўнг ўтиринг"
                ),
                ttsPrompt = "Allohu akbar. Subhana Robbiyal A'la. Subhana Robbiyal A'la. Subhana Robbiyal A'la.",
                minHoldDurationMs = 7000L,
                audioAssetId = "sajda"
            )

            // Jalsa
            steps += PrayerStep(
                index = idx++,
                rakatNumber = rakat,
                totalRakats = totalRakats,
                expectedPose = PrayerPose.JALSA,
                stepTitle = lText("$rakat-Rakat • Jalsa", "$rakat-Ракат • Жалса"),
                recitationTitle = lText("SAJDALAR ORASIDA O‘TIRISH", "САЖДАЛАР ОРАСИДА ЎТИРИШ"),
                arabicText = "اللَّهُ أَكْبَرُ ۝ رَبِّ اغْفِرْ لِي",
                recitationLatin = "Allohu akbar. Robbig‘firliy, Robbig‘firliy.",
                recitationCyrillic = "Аллоҳу акбар. Роббиғфирлий, Роббиғфирлий.",
                meaning = lText(
                    "Robbim, meni mag‘firat qilgin.",
                    "Роббим, мени мағфират қилгин."
                ),
                coachInstruction = lText(
                    "Biroz xotirjam o‘tirib, ikkinchi sajdaga boring",
                    "Бироз хотиржам ўтириб, иккинчи саждага боринг"
                ),
                ttsPrompt = "Allohu akbar. Robbig'firliy.",
                minHoldDurationMs = 4500L,
                audioAssetId = "jalsa"
            )

            // 2nd Sajda
            steps += PrayerStep(
                index = idx++,
                rakatNumber = rakat,
                totalRakats = totalRakats,
                expectedPose = PrayerPose.SAJDA,
                stepTitle = lText("$rakat-Rakat • 2-Sajda", "$rakat-Ракат • 2-Сажда"),
                recitationTitle = lText("2-SAJDA TASBEHI", "2-САЖДА ТАСБЕҲИ"),
                arabicText = "سُبْحَانَ رَبِّيَ الْأَعْلَى (٣×)",
                recitationLatin = "Allohu akbar. Subhana Robbiyal A‘la. Subhana Robbiyal A‘la. Subhana Robbiyal A‘la.",
                recitationCyrillic = "Аллоҳу акбар. Субҳана Роббиял Аъла. Субҳана Роббиял Аъла. Субҳана Роббиял Аъла.",
                meaning = lText(
                    "Eng oliy Robbim barcha nuqsonlardan pokdir (3 marta).",
                    "Энг олий Роббим барча нуқсонлардан покдир (3 марта)."
                ),
                coachInstruction = lText(
                    if (rakat == 2 || rakat == totalRakats) "Tasbehdan so‘ng qa'daga o‘tiring"
                    else "Tasbehdan so‘ng navbatdagi rakat uchun tik turing",
                    if (rakat == 2 || rakat == totalRakats) "Тасбеҳдан сўнг қаъдага ўтиринг"
                    else "Тасбеҳдан сўнг навбатдаги ракат учун тик туринг"
                ),
                ttsPrompt = "Allohu akbar. Subhana Robbiyal A'la. Subhana Robbiyal A'la. Subhana Robbiyal A'la.",
                minHoldDurationMs = 7000L,
                audioAssetId = "sajda"
            )

            // Qa'dai ula (2nd rakat when total > 2)
            if (rakat == 2 && totalRakats > 2) {
                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = 2,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.QADA,
                    stepTitle = lText("2-Rakat • Birinchi qa'da", "2-Ракат • Биринчи қаъда"),
                    recitationTitle = lText("ATTAHIYAT", "АТТАҲИЯТ"),
                    arabicText = attahiyat.arabicText,
                    recitationLatin = attahiyat.translitLatin,
                    recitationCyrillic = attahiyat.translitCyrillic,
                    meaning = attahiyat.meaning,
                    coachInstruction = lText(
                        "Attahiyat tugagach, 3-rakatga tik turing",
                        "Аттаҳият тугагач, 3-ракатга тик туринг"
                    ),
                    ttsPrompt = "Allohu akbar. ${attahiyat.ttsPrompt}",
                    minHoldDurationMs = 12000L,
                    audioAssetId = "takbir,${attahiyat.audioAssetId}",
                    postRecitationCue = lText(
                        "Endi 3-rakatga tik turing",
                        "Энди 3-ракатга тик туринг"
                    )
                )
            }

            // Final Qa'da
            if (rakat == totalRakats) {
                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = rakat,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.QADA,
                    stepTitle = lText("Oxirgi qa'da • Attahiyat", "Охирги қаъда • Аттаҳият"),
                    recitationTitle = lText("ATTAHIYAT", "АТТАҲИЯТ"),
                    arabicText = attahiyat.arabicText,
                    recitationLatin = attahiyat.translitLatin,
                    recitationCyrillic = attahiyat.translitCyrillic,
                    meaning = attahiyat.meaning,
                    coachInstruction = lText(
                        "O‘tirgan holda Attahiyatni o‘qing",
                        "Ўтирган ҳолда Аттаҳиятни ўқинг"
                    ),
                    ttsPrompt = "Allohu akbar. ${attahiyat.ttsPrompt}",
                    minHoldDurationMs = 12000L,
                    audioAssetId = "takbir,${attahiyat.audioAssetId}",
                    postRecitationCue = lText(
                        "Endi Salovat va Robbana duosini o‘qing",
                        "Энди Саловат ва Роббана дуосини ўқинг"
                    )
                )

                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = rakat,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.QADA,
                    stepTitle = lText("Oxirgi qa'da • Salovat va Duo", "Охирги қаъда • Саловат ва Дуо"),
                    recitationTitle = lText("SALOVAT VA ROBBANA", "САЛОВАТ ВА РОББАНА"),
                    arabicText = "${salovat.arabicText}\n${robbana.arabicText}",
                    recitationLatin = "${salovat.translitLatin} ${robbana.translitLatin}",
                    recitationCyrillic = "${salovat.translitCyrillic} ${robbana.translitCyrillic}",
                    meaning = robbana.meaning,
                    coachInstruction = lText(
                        "Salovat va Robbana duosidan so‘ng salom bering",
                        "Саловат ва Роббана дуосидан сўнг салом беринг"
                    ),
                    ttsPrompt = "${salovat.ttsPrompt} ${robbana.ttsPrompt}",
                    minHoldDurationMs = 14000L,
                    requiresPoseTransition = false,
                    audioAssetId = "salovat,robbana",
                    postRecitationCue = lText(
                        "Endi o‘ng va chap yelkaga salom bering",
                        "Энди ўнг ва чап елкага салом беринг"
                    )
                )

                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = rakat,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.SALAM,
                    stepTitle = lText("Salom berish", "Салом бериш"),
                    recitationTitle = lText("NAMOZNI YAKUNLASH", "НАМОЗНИ ЯКУНЛАШ"),
                    arabicText = "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللَّهِ",
                    recitationLatin = "Assalamu ‘alaykum va rohmatulloh (o‘ngga va chapga).",
                    recitationCyrillic = "Ассаламу ъалайкум ва роҳматуллоҳ (ўнгга ва чапга).",
                    meaning = lText(
                        "Sizlarga Allohning tinchligi va rahmati bo‘lsin.",
                        "Сизларга Аллоҳнинг тинчлиги ва раҳмати бўлсин."
                    ),
                    coachInstruction = lText(
                        "Avval o‘ng yelkangizga, so‘ng chap yelkangizga qarab salom bering",
                        "Аввал ўнг елкангизга, сўнг чап елкангизга қараб салом беринг"
                    ),
                    ttsPrompt = "Assalamu alaykum va rohmatulloh.",
                    minHoldDurationMs = 6000L,
                    requiresPoseTransition = false,
                    audioAssetId = "salam"
                )
            }
        }
        return steps
    }
}
