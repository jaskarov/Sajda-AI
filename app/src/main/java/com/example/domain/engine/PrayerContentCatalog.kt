package com.example.domain.engine

import com.example.domain.model.PostureGuideItem
import com.example.domain.model.PrayerPose
import com.example.domain.model.SurahLibraryItem
import com.example.domain.model.lText

object PrayerContentCatalog {

    val surahs: List<SurahLibraryItem> = listOf(
        SurahLibraryItem(
            id = "sano",
            title = lText("Sano duosi", "Сано дуоси"),
            subtitle = lText("Takbiri tahrima ortidan o‘qiladi", "Такбири таҳрима ортидан ўқилади"),
            arabicText = "سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ وَتَبَارَكَ اسْمُكَ وَتَعَالَى جَدُّكَ وَلَا إِلَهَ غَيْرُكَ",
            translitLatin = "Subhanakallohumma va bihamdika, va tabarokasmuka, va ta'ala jadduka, va la ilaha g‘oyruk.",
            translitCyrillic = "Субҳанакаллоҳумма ва биҳамдика, ва табарокасмука, ва таъала жаддука, ва ла илаҳа ғойрук.",
            meaning = lText(
                "Allohim, Sen poksan va hamd Sengadir. Isming muborak, ulug‘liging yuksakdir. Sendan o‘zga iloh yo‘q.",
                "Аллоҳим, Сен поксан ва ҳамд Сенгадир. Исминг муборак, улуғлигинг юксакдир. Сендан ўзга илоҳ йўқ."
            ),
            ttsPrompt = "Subhanakallohumma va bihamdika, va tabarokasmuka, va ta'ala jadduka, va la ilaha g'oyruk"
        ),
        SurahLibraryItem(
            id = "fotiha",
            title = lText("Fotiha surasi", "Фотиҳа сураси"),
            subtitle = lText("Har bir rakatda o‘qilishi farz", "Ҳар бир ракатда ўқилиши фарз"),
            arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۝ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝ الرَّحْمَٰنِ الرَّحِيمِ ۝ مَالِكِ يَوْمِ الدِّينِ ۝ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۝ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۝ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
            translitLatin = "Bismillahir Rohmanir Rohiym. Alhamdulillahi Robbil ‘alamiyn. Ar-Rohmanir Rohiym. Maliki yavmiddiyn. Iyyaka na‘budu va iyyaka nasta‘iyn. Ihdinas-sirotol mustaqiym. Sirotollaziyna an‘amta ‘alayhim g‘oyril mag‘zubi ‘alayhim valazzolliyn.",
            translitCyrillic = "Бисмиллаҳир Роҳманир Роҳийм. Алҳамдулиллаҳи Роббил ъаламийн. Ар-Роҳманир Роҳийм. Малики явмиддийн. Ийяка наъбуду ва ийяка настаъийн. Иҳдинас-сиротол мустақийм. Сиротоллазийна анъамта ъалайҳим ғойрил мағзуби ъалайҳим валаззоллийн.",
            meaning = lText(
                "Mehribon va rahmli Alloh nomi bilan. Barcha hamd olamlar Robbi Allohga xosdir...",
                "Меҳрибон ва раҳмли Аллоҳ номи билан. Барча ҳамд оламлар Робби Аллоҳга хосдир..."
            ),
            ttsPrompt = "Bismillahir Rohmanir Rohiym. Alhamdulillahi Robbil alamiyn. Ar-Rohmanir Rohiym. Maliki yavmiddiyn. Iyyaka na'budu va iyyaka nasta'iyn. Ihdinas sirotol mustaqiym. Sirotollaziyna an'amta alayhim g'oyril mag'zubi alayhim valazzolliyn. Amiyn."
        ),
        SurahLibraryItem(
            id = "kavsar",
            title = lText("Kavsar surasi", "Кавсар сураси"),
            subtitle = lText("Qur'ondagi eng qisqa zam sura", "Қуръондаги энг қисқа зам сура"),
            arabicText = "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ۝ فَصَلِّ لِرَبِّكَ وَانْحَرْ ۝ إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ",
            translitLatin = "Inna a‘toynakal Kavsar. Fasolli lirobbika vanhar. Inna shani’aka huval abtar.",
            translitCyrillic = "Инна аътойнакал Кавсар. Фасолли лироббика ванҳар. Инна шаниъака ҳувал абтар.",
            meaning = lText(
                "Albatta, Biz senga Kavsarni ato etdik. Bas, Robbing uchun namoz o‘qi va qurbonlik qil.",
                "Албатта, Биз сенга Кавсарни ато этдик. Бас, Роббинг учун намоз ўқи ва қурбонлик қил."
            ),
            ttsPrompt = "Bismillahir Rohmanir Rohiym. Inna a'toynakal Kavsar. Fasolli lirobbika vanhar. Inna shani'aka huval abtar."
        ),
        SurahLibraryItem(
            id = "ixlos",
            title = lText("Ixlos surasi", "Ихлос сураси"),
            subtitle = lText("Tavhid surasi (zam sura)", "Тавҳид сураси (зам сура)"),
            arabicText = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ",
            translitLatin = "Qul huvallohu ahad. Allohus-somad. Lam yalid va lam yulad. Va lam yakullahu kufuvan ahad.",
            translitCyrillic = "Қул ҳуваллоҳу аҳад. Аллоҳус-сомад. Лам ялид ва лам юлад. Ва лам якуллаҳу куфуван аҳад.",
            meaning = lText(
                "Ayt: U Alloh yagonadir. Alloh behojatdir. U tug‘magan va tug‘ilmagandir. Va Unga hech kim teng emasdir.",
                "Айт: У Аллоҳ ягонадир. Аллоҳ беҳожатдир. У туғмаган ва туғилмагандир. Ва Унга ҳеч ким тенг эмасдир."
            ),
            ttsPrompt = "Bismillahir Rohmanir Rohiym. Qul huvallohu ahad. Allohus somad. Lam yalid va lam yulad. Va lam yakullahu kufuvan ahad."
        ),
        SurahLibraryItem(
            id = "falaq",
            title = lText("Falaq surasi", "Фалақ сураси"),
            subtitle = lText("Panoh so‘rash surasi", "Паноҳ сўраш сураси"),
            arabicText = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ",
            translitLatin = "Qul a‘uzu birobbil falaq. Min sharri ma xolaq. Va min sharri g‘osiqin iza vaqob. Va min sharrin-naffasati fil uqod. Va min sharri hasidin iza hasad.",
            translitCyrillic = "Қул аъузу бироббил фалақ. Мин шарри ма холақ. Ва мин шарри ғосиқин иза вақоб. Ва мин шаррин-наффасати фил уқод. Ва мин шарри ҳасидин иза ҳасад.",
            meaning = lText(
                "Tong Robbidan yaratgan narsalarining yomonligidan panoh so‘rayman...",
                "Тонг Роббидан яратган нарсаларининг ёмонлигидан паноҳ сўрайман..."
            ),
            ttsPrompt = "Qul a'uzu birobbil falaq. Min sharri ma xolaq. Va min sharri g'osiqin iza vaqob. Va min sharrin naffasati fil uqod. Va min sharri hasidin iza hasad."
        ),
        SurahLibraryItem(
            id = "attahiyat",
            title = lText("Attahiyat (Tashahhud)", "Аттаҳият (Ташаҳҳуд)"),
            subtitle = lText("Qa'dada o‘qiladi", "Қаъдада ўқилади"),
            arabicText = "التَّحِيَّاتُ لِلَّهِ وَالصَّلَوَاتُ وَالطَّيِّبَاتُ السَّلَامُ عَلَيْكَ أَيُّهَا النَّبِيُّ وَرَحْمَةُ اللَّهِ وَبَرَكَاتُهُ",
            translitLatin = "Attahiyyatu lillahi vas-solavatu vat-toyyibat. Assalamu ‘alayka ayyuhan-nabiyyu va rohmatullohi va barokatuh. Assalamu ‘alayna va ‘ala ‘ibadillahis-solihiyn. Ashhadu alla ilaha illallohu va ashhadu anna Muhammadan ‘abduhu va rosuluh.",
            translitCyrillic = "Аттаҳийяту лиллаҳи вас-солавату ват-тоййибат. Ассаламу ъалайка айюҳан-набийю ва роҳматуллоҳи ва барокатуҳ. Ассаламу ъалайна ва ъала ъибадиллаҳис-солиҳийн. Ашҳаду алла илаҳа иллаллоҳу ва ашҳаду анна Муҳаммадан ъабдуҳу ва росулуҳ.",
            meaning = lText(
                "Barcha maqtovlar, salovatlar va pok amallar Alloh uchundir...",
                "Барча мақтовлар, саловатлар ва пок амаллар Аллоҳ учундир..."
            ),
            ttsPrompt = "Attahiyyatu lillahi vas solavatu vat toyyibat. Assalamu alayka ayyuhan nabiyyu va rohmatullohi va barokatuh. Assalamu alayna va ala ibadillahis solihiyn. Ashhadu alla ilaha illallohu va ashhadu anna Muhammadan abduhu va rosuluh."
        ),
        SurahLibraryItem(
            id = "salovat",
            title = lText("Salovat (Allohumma solli)", "Саловат (Аллоҳумма солли)"),
            subtitle = lText("Oxirgi qa'dada o‘qiladi", "Охирги қаъдада ўқилади"),
            arabicText = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ",
            translitLatin = "Allohumma solli ‘ala Muhammadin va ‘ala ali Muhammad, kama sollayta ‘ala Ibrohiyma va ‘ala ali Ibrohiym, innaka hamiydum-majiyd. Allohumma barik ‘ala Muhammadin va ‘ala ali Muhammad...",
            translitCyrillic = "Аллоҳумма солли ъала Муҳаммадин ва ъала али Муҳаммад, кама соллайта ъала Иброҳийма ва ъала али Иброҳийм, иннака ҳамийдум-мажийд. Аллоҳумма барик ъала Муҳаммадин ва ъала али Муҳаммад...",
            meaning = lText(
                "Allohim, Ibrohim va uning oilasiga rahmat yo‘llaganing kabi Muhammad va u zotning oilasiga ham rahmat va baraka yog‘dir.",
                "Аллоҳим, Иброҳим ва унинг оиласига раҳмат йўллаганинг каби Муҳаммад ва у зотнинг оиласига ҳам раҳмат ва барака ёғдир."
            ),
            ttsPrompt = "Allohumma solli ala Muhammadin va ala ali Muhammad, kama sollayta ala Ibrohiyma va ala ali Ibrohiym, innaka hamiydum majiyd."
        ),
        SurahLibraryItem(
            id = "robbana",
            title = lText("Robbana duosi", "Роббана дуоси"),
            subtitle = lText("Salom berishdan oldin o‘qiladi", "Салом беришдан олдин ўқилади"),
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            translitLatin = "Robbana atina fid-dunya hasanatan va fil axiroti hasanatan va qina ‘azaban-nar.",
            translitCyrillic = "Роббана атина фид-дуня ҳасанатан ва фил ахироти ҳасанатан ва қина ъазабан-нар.",
            meaning = lText(
                "Robbimiz, bizga bu dunyoda ham yaxshilik ber, oxiratda ham yaxshilik ber va bizni do‘zax azobidan saqla.",
                "Роббимиз, бизга бу дунёда ҳам яхшилик бер, охиратда ҳам яхшилик бер ва бизни дўзах азобидан сақла."
            ),
            ttsPrompt = "Robbana atina fid dunya hasanatan va fil axiroti hasanatan va qina azaban nar."
        )
    )

    val postureGuides: List<PostureGuideItem> = listOf(
        PostureGuideItem(
            id = "guide_takbir",
            pose = PrayerPose.TAKBIR,
            title = lText("Takbiri tahrima", "Такбири таҳрима"),
            angleSpec = "θ_torso: 170°–180° • Qo‘llar yuqorida",
            description = lText(
                "Qiblaga qarab tik turiladi, niyat qilinib qo‘llar ko‘tarilgan holda «Allohu akbar» deyiladi.",
                "Қиблага қараб тик турилади, ният қилиниб қўллар кўтарилган ҳолда «Аллоҳу акбар» дейилади."
            ),
            maleNote = lText(
                "Bosh barmoqlar quloq yumshog‘iga tekkiziladi.",
                "Бош бармоқлар қулоқ юмшоғига теккизилади."
            ),
            femaleNote = lText(
                "Qo‘llar yelka barobarida ko‘tariladi.",
                "Қўллар елка баробарида кўтарилади."
            ),
            ttsPrompt = "Takbiri tahrima uchun tik turing, niyat qiling va qo'llaringizni ko'tarib Allohu akbar deng."
        ),
        PostureGuideItem(
            id = "guide_qiyom",
            pose = PrayerPose.QIYOM,
            title = lText("Qiyom (Tik turish)", "Қиём (Тик туриш)"),
            angleSpec = "θ_torso: 165°–185° • θ_knee > 165°",
            description = lText(
                "Gavda tik, tizzalar bukilmagan holatda Fotiha va zam suralar o‘qiladi. Nigoh sajda o‘rnida bo‘ladi.",
                "Гавда тик, тиззалар букилмаган ҳолатда Фотиҳа ва зам суралар ўқилади. Нигоҳ сажда ўрнида бўлади."
            ),
            maleNote = lText(
                "O‘ng qo‘l chap qo‘l ustida kindik ostida bog‘lanadi.",
                "Ўнг қўл чап қўл устида киндик остида боғланади."
            ),
            femaleNote = lText(
                "O‘ng qo‘l chap qo‘l ustida ko‘krak ustiga qo‘yiladi.",
                "Ўнг қўл чап қўл устида кўкрак устига қўйилади."
            ),
            ttsPrompt = "Qiyomda gavdangizni tik tuting va nigohingizni sajda o'rniga qarating."
        ),
        PostureGuideItem(
            id = "guide_ruku",
            pose = PrayerPose.RUKU,
            title = lText("Ruku (Egilish)", "Руку (Эгилиш)"),
            angleSpec = "θ_torso: 75°–115° • θ_knee > 160°",
            description = lText(
                "Orqa tekis holatda beldan 90° atrofida egilib, qo‘llar tizzaga qo‘yiladi va uch marta «Subhana Robbiyal ‘Aziym» aytiladi.",
                "Орқа текис ҳолатда белдан 90° атрофида эгилиб, қўллар тиззага қўйилади ва уч марта «Субҳана Роббиял Ъазийм» айтилади."
            ),
            maleNote = lText(
                "Barmoqlar orasi ochiq holda tizzani mahkam tutadi, bel tekis bo‘ladi.",
                "Бармоқлар ораси очиқ ҳолда тиззани маҳкам тутади, бел текис бўлади."
            ),
            femaleNote = lText(
                "Biroz kamroq egilinadi, barmoqlar jips holda tizza ustiga qo‘yiladi.",
                "Бироз камроқ эгилинади, бармоқлар жипс ҳолда тизза устига қўйилади."
            ),
            ttsPrompt = "Rukuga egiling, belingizni tekis tuting va uch marta Subhana Robbiyal Aziym deng."
        ),
        PostureGuideItem(
            id = "guide_sajda",
            pose = PrayerPose.SAJDA,
            title = lText("Sajda", "Сажда"),
            angleSpec = "H_head < 15% • θ_knee < 60°",
            description = lText(
                "Peshona va burun joynamozga qo‘yilib, uch marta «Subhana Robbiyal A‘la» aytiladi.",
                "Пешона ва бурун жойнамозга қўйилиб, уч марта «Субҳана Роббиял Аъла» айтилади."
            ),
            maleNote = lText(
                "Tirsaklar yerga tegmaydi va biqindan uzoqroq tutiladi.",
                "Тирсаклар ерга тегмайди ва биқиндан узоқроқ тутилади."
            ),
            femaleNote = lText(
                "Qo‘llar va gavda ixcham, yerga yaqinroq tutiladi.",
                "Қўллар ва гавда ихчам, ерга яқинроқ тутилади."
            ),
            ttsPrompt = "Sajdaga boring, peshonangizni yerga qo'yib uch marta Subhana Robbiyal A'la deng."
        ),
        PostureGuideItem(
            id = "guide_qada",
            pose = PrayerPose.QADA,
            title = lText("Qa'da (O‘tirish)", "Қаъда (Ўтириш)"),
            angleSpec = "θ_torso: 75°–105° • θ_knee < 50°",
            description = lText(
                "Gavda tik holda tizzalar ustida o‘tirilib Attahiyat, oxirgi qa'dada esa Salovat va Robbana o‘qiladi.",
                "Гавда тик ҳолда тиззалар устида ўтирилиб Аттаҳият, охирги қаъдада эса Саловат ва Роббана ўқилади."
            ),
            maleNote = lText(
                "Chap oyoq yotqizilib ustiga o‘tiriladi, o‘ng oyoq barmoqlari qiblaga qaratiladi.",
                "Чап оёқ ётқизилиб устига ўтирилади, ўнг оёқ бармоқлари қиблага қаратилади."
            ),
            femaleNote = lText(
                "Ikki oyoq o‘ng tomonga chiqarilib ixcham o‘tiriladi.",
                "Икки оёқ ўнг томонга чиқарилиб ихчам ўтирилади."
            ),
            ttsPrompt = "Qa'dada xotirjam o'tiring va Attahiyatni o'qing."
        )
    )
}
