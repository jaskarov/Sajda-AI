package com.example.domain.engine

import com.example.domain.model.Gender
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerStep
import com.example.domain.model.PrayerTrack
import com.example.domain.model.lText

class PrayerSequenceEngine {

    fun buildSteps(track: PrayerTrack, gender: Gender): List<PrayerStep> {
        val steps = mutableListOf<PrayerStep>()
        var idx = 0

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
                    meaning = lText(
                        "Alloh buyukdir — namozga kirish takbiri.",
                        "Аллоҳ буюкдир — намозга кириш такбири."
                    ),
                    coachInstruction = lText(takbirHintLat, takbirHintCyr),
                    ttsPrompt = "Niyat qiling va qo'llaringizni ko'tarib: Allohu akbar",
                    minHoldDurationMs = 4000L,
                    audioAssetId = "takbir"
                )

                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = 1,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.QIYOM,
                    stepTitle = lText("Qiyom • Sano", "Қиём • Сано"),
                    recitationTitle = lText("SANO DUOSI", "САНО ДУОСИ"),
                    arabicText = PrayerContentCatalog.surahs[0].arabicText,
                    recitationLatin = PrayerContentCatalog.surahs[0].translitLatin,
                    recitationCyrillic = PrayerContentCatalog.surahs[0].translitCyrillic,
                    meaning = PrayerContentCatalog.surahs[0].meaning,
                    coachInstruction = lText(
                        "$qiyomHandsHintLat va Sano duosini o‘qing",
                        "$qiyomHandsHintCyr ва Сано дуосини ўқинг"
                    ),
                    ttsPrompt = "Qo'llaringizni bog'lang. ${PrayerContentCatalog.surahs[0].ttsPrompt}",
                    minHoldDurationMs = 7000L,
                    audioAssetId = PrayerContentCatalog.surahs[0].audioAssetId,
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
                arabicText = PrayerContentCatalog.surahs[1].arabicText,
                recitationLatin = PrayerContentCatalog.surahs[1].translitLatin,
                recitationCyrillic = PrayerContentCatalog.surahs[1].translitCyrillic,
                meaning = PrayerContentCatalog.surahs[1].meaning,
                coachInstruction = lText(
                    if (hasZamSura) "Fotiha surasidan so‘ng zam sura o‘qiladi"
                    else "Sura tugagach, rukuga egiling",
                    if (hasZamSura) "Фотиҳа сурасидан сўнг зам сура ўқилади"
                    else "Сура тугагач, рукуга эгилинг"
                ),
                ttsPrompt = PrayerContentCatalog.surahs[1].ttsPrompt,
                minHoldDurationMs = 15000L,
                requiresPoseTransition = rakat > 1,
                audioAssetId = PrayerContentCatalog.surahs[1].audioAssetId,
                postRecitationCue = if (hasZamSura) {
                    lText("Endi zam surani o‘qing", "Энди зам сурани ўқинг")
                } else {
                    lText("Allohu akbar, rukuga boring", "Аллоҳу акбар, рукуга боринг")
                }
            )

            // Zam sura in rakats 1 & 2, or all rakats for sunnat/vitr
            if (hasZamSura) {
                val zamSura = when (rakat) {
                    1 -> PrayerContentCatalog.surahs[2] // Kavsar
                    2 -> PrayerContentCatalog.surahs[3] // Ixlos
                    else -> PrayerContentCatalog.surahs[4] // Falaq
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
                        if (isVitr && rakat == 3) "Qunut takbirini aytib Qunut duosini o‘qing"
                        else "Sura tugagach, «Allohu akbar» deb rukuga egiling",
                        if (isVitr && rakat == 3) "Қунут такбирини айтиб Қунут дуосини ўқинг"
                        else "Сура тугагач, «Аллоҳу акбар» деб рукуга эгилинг"
                    ),
                    ttsPrompt = zamSura.ttsPrompt,
                    minHoldDurationMs = 9000L,
                    requiresPoseTransition = false,
                    audioAssetId = zamSura.audioAssetId,
                    postRecitationCue = if (isVitr && rakat == 3) {
                        lText("Allohu akbar, Qunut duosini o‘qing", "Аллоҳу акбар, Қунут дуосини ўқинг")
                    } else {
                        lText("Allohu akbar, rukuga boring", "Аллоҳу акбар, рукуга боринг")
                    }
                )
            }

            if (isVitr && rakat == 3) {
                steps += PrayerStep(
                    index = idx++,
                    rakatNumber = 3,
                    totalRakats = totalRakats,
                    expectedPose = PrayerPose.QIYOM,
                    stepTitle = lText("3-Rakat • Qunut duosi", "3-Ракат • Қунут дуоси"),
                    recitationTitle = lText("QUNUT DUOSI", "ҚУНУТ ДУОСИ"),
                    arabicText = "اللَّهُمَّ إِنَّا نَسْتَعِينُكَ وَنَسْتَغْفِرُكَ وَنُؤْمِنُ بِكَ وَنَتَوَكَّلُ عَلَيْكَ",
                    recitationLatin = "Allohumma inna nasta‘iynuka va nastag‘firuk, va nu’minu bika va natavakkalu ‘alayk, va nusniy ‘alaykal xoyr...",
                    recitationCyrillic = "Аллоҳумма инна настаъийнука ва настағфирук, ва нуъмину бика ва натаваккалу ъалайк, ва нусний ъалайкал хойр...",
                    meaning = lText(
                        "Allohim, Sendan madad va mag‘firat so‘raymiz, Senga iymon keltirib tavakkal qilamiz.",
                        "Аллоҳим, Сендан мадад ва мағфират сўраймиз, Сенга иймон келтириб таваккал қиламиз."
                    ),
                    coachInstruction = lText(
                        "Qunut duosi tugagach rukuga egiling",
                        "Қунут дуоси тугагач рукуга эгилинг"
                    ),
                    ttsPrompt = "Allohu akbar. Allohumma inna nasta'iynuka va nastag'firuk, va nu'minu bika va natavakkalu alayk.",
                    minHoldDurationMs = 10000L,
                    requiresPoseTransition = false,
                    audioAssetId = "qunut",
                    postRecitationCue = lText("Allohu akbar, rukuga boring", "Аллоҳу акбар, рукуга боринг")
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
                    arabicText = PrayerContentCatalog.surahs[5].arabicText,
                    recitationLatin = PrayerContentCatalog.surahs[5].translitLatin,
                    recitationCyrillic = PrayerContentCatalog.surahs[5].translitCyrillic,
                    meaning = PrayerContentCatalog.surahs[5].meaning,
                    coachInstruction = lText(
                        "Attahiyat tugagach, 3-rakatga tik turing",
                        "Аттаҳият тугагач, 3-ракатга тик туринг"
                    ),
                    ttsPrompt = "Allohu akbar. ${PrayerContentCatalog.surahs[5].ttsPrompt}",
                    minHoldDurationMs = 12000L,
                    audioAssetId = PrayerContentCatalog.surahs[5].audioAssetId,
                    postRecitationCue = lText(
                        "Allohu akbar, navbatdagi rakatga turing",
                        "Аллоҳу акбар, навбатдаги ракатга туринг"
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
                    arabicText = PrayerContentCatalog.surahs[5].arabicText,
                    recitationLatin = PrayerContentCatalog.surahs[5].translitLatin,
                    recitationCyrillic = PrayerContentCatalog.surahs[5].translitCyrillic,
                    meaning = PrayerContentCatalog.surahs[5].meaning,
                    coachInstruction = lText(
                        "O‘tirgan holda Attahiyatni o‘qing",
                        "Ўтирган ҳолда Аттаҳиятни ўқинг"
                    ),
                    ttsPrompt = "Allohu akbar. ${PrayerContentCatalog.surahs[5].ttsPrompt}",
                    minHoldDurationMs = 12000L,
                    audioAssetId = PrayerContentCatalog.surahs[5].audioAssetId,
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
                    arabicText = "${PrayerContentCatalog.surahs[6].arabicText}\n${PrayerContentCatalog.surahs[7].arabicText}",
                    recitationLatin = "${PrayerContentCatalog.surahs[6].translitLatin} ${PrayerContentCatalog.surahs[7].translitLatin}",
                    recitationCyrillic = "${PrayerContentCatalog.surahs[6].translitCyrillic} ${PrayerContentCatalog.surahs[7].translitCyrillic}",
                    meaning = PrayerContentCatalog.surahs[7].meaning,
                    coachInstruction = lText(
                        "Salovat va Robbana duosidan so‘ng salom bering",
                        "Саловат ва Роббана дуосидан сўнг салом беринг"
                    ),
                    ttsPrompt = "${PrayerContentCatalog.surahs[6].ttsPrompt} ${PrayerContentCatalog.surahs[7].ttsPrompt}",
                    minHoldDurationMs = 12000L,
                    requiresPoseTransition = false,
                    audioAssetId = "salovat,robbana",
                    postRecitationCue = lText(
                        "O‘ng va chap yelkaga salom bering",
                        "Ўнг ва чап елкага салом беринг"
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
                    ttsPrompt = "O'ng va chap yelkangizga qarab: Assalamu alaykum va rohmatulloh. Namozingiz qabul bo'lsin.",
                    minHoldDurationMs = 6000L,
                    requiresPoseTransition = false,
                    audioAssetId = "salam"
                )
            }
        }
        return steps
    }
}
