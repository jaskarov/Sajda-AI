package com.example.domain.engine

import com.example.domain.model.PostureGuideItem
import com.example.domain.model.PrayerPose
import com.example.domain.model.PurificationCategory
import com.example.domain.model.PurificationGuideItem
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
                "Mehribon va rahmli Alloh nomi bilan. Barcha hamd olamlar Robbi Allohga xosdir. U Mehribon va Rahmli, jazo kunining egasidir. Sengagina ibodat qilamiz va Sendangina yordam so‘raymiz. Bizni g‘azabga uchraganlar va adashganlarning emas, O‘zing ne’mat bergan zotlarning to‘g‘ri yo‘liga hidoyat qilgin.",
                "Меҳрибон ва раҳмли Аллоҳ номи билан. Барча ҳамд оламлар Робби Аллоҳга хосдир. У Меҳрибон ва Раҳмли, жазо кунининг эгасидир. Сенгагина ибодат қиламиз ва Сендангина ёрдам сўраймиз. Бизни ғазабга учраганлар ва адашганларнинг эмас, Ўзинг неъмат берган зотларнинг тўғри йўлига ҳидоят қилгин."
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
                "Albatta, Biz senga Kavsarni ato etdik. Bas, Robbing uchun namoz o‘qi va qurbonlik qil. Albatta, seni yomon ko‘ruvchining o‘zi naslsizdir.",
                "Албатта, Биз сенга Кавсарни ато этдик. Бас, Роббинг учун намоз ўқи ва қурбонлик қил. Албатта, сени ёмон кўрувчининг ўзи наслсиздир."
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
            subtitle = lText("Panoh so‘rash surasi (5 oyat)", "Паноҳ сўраш сураси (5 оят)"),
            arabicText = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِنْ شَرِّ مَا خَلَقَ ۝ وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",
            translitLatin = "Qul a‘uzu birobbil falaq. Min sharri ma xolaq. Va min sharri g‘osiqin iza vaqob. Va min sharrin-naffasati fil uqod. Va min sharri hasidin iza hasad.",
            translitCyrillic = "Қул аъузу бироббил фалақ. Мин шарри ма холақ. Ва мин шарри ғосиқин иза вақоб. Ва мин шаррин-наффасати фил уқод. Ва мин шарри ҳасидин иза ҳасад.",
            meaning = lText(
                "Ayt: Tong Robbidan yaratgan narsalarining yomonligidan, zulmatga cho‘mgan tunning yomonligidan, tugunlarga dam soluvchilarning yomonligidan va hasad qilgan paytdagi hasadchining yomonligidan panoh so‘rayman.",
                "Айт: Тонг Роббидан яратган нарсаларининг ёмонлигидан, зулматга чўмган туннинг ёмонлигидан, тугунларга дам солувчиларнинг ёмонлигидан ва ҳасад қилган пайтдаги ҳасадчининг ёмонлигидан паноҳ сўрайман."
            ),
            ttsPrompt = "Qul a'uzu birobbil falaq. Min sharri ma xolaq. Va min sharri g'osiqin iza vaqob. Va min sharrin naffasati fil uqod. Va min sharri hasidin iza hasad."
        ),
        SurahLibraryItem(
            id = "nas",
            title = lText("Nos surasi", "Нос сураси"),
            subtitle = lText("Qur'onning oxirgi surasi (6 oyat)", "Қуръоннинг охирги сураси (6 оят)"),
            arabicText = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",
            translitLatin = "Qul a‘uzu birobbin-nas. Malikin-nas. Ilahin-nas. Min sharril vasvasil xonnas. Allaziy yuvasvisu fiy sudurin-nas. Minal jinnati van-nas.",
            translitCyrillic = "Қул аъузу бироббин-нас. Маликин-нас. Илаҳин-нас. Мин шаррил васвасил хоннас. Аллазий ювасвису фий судурин-нас. Минал жиннати ван-нас.",
            meaning = lText(
                "Ayt: Insonlarning Robbidan, insonlarning Podshohidan, insonlarning Ilohidan — jinlardan va insonlardan bo‘lib, odamlarning dillariga vasvasa soluvchi yashirin vasvasachining yomonligidan panoh so‘rayman.",
                "Айт: Инсонларнинг Роббидан, инсонларнинг Подшоҳидан, инсонларнинг Илоҳидан — жинлардан ва инсонлардан бўлиб, одамларнинг дилларига васваса солувчи яширин васвасачининг ёмонлигидан паноҳ сўрайман."
            ),
            ttsPrompt = "Qul a'uzu birobbin nas. Malikin nas. Ilahin nas. Min sharril vasvasil xonnas. Allaziy yuvasvisu fiy sudurin nas. Minal jinnati van nas.",
            audioAssetId = "nas"
        ),
        SurahLibraryItem(
            id = "attahiyat",
            title = lText("Attahiyat (Tashahhud)", "Аттаҳият (Ташаҳҳуд)"),
            subtitle = lText("Qa'dada o‘qiladi", "Қаъдада ўқилади"),
            arabicText = "التَّحِيَّاتُ لِلَّهِ وَالصَّلَوَاتُ وَالطَّيِّبَاتُ السَّلَامُ عَلَيْكَ أَيُّهَا النَّبِيُّ وَرَحْمَةُ اللَّهِ وَبَرَكَاتُهُ السَّلَامُ عَلَيْنَا وَعَلَى عِبَادِ اللَّهِ الصَّالِحِينَ أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ",
            translitLatin = "Attahiyyatu lillahi vas-solavatu vat-toyyibat. Assalamu ‘alayka ayyuhan-nabiyyu va rohmatullohi va barokatuh. Assalamu ‘alayna va ‘ala ‘ibadillahis-solihiyn. Ashhadu alla ilaha illallohu va ashhadu anna Muhammadan ‘abduhu va rosuluh.",
            translitCyrillic = "Аттаҳийяту лиллаҳи вас-солавату ват-тоййибат. Ассаламу ъалайка айюҳан-набийю ва роҳматуллоҳи ва барокатуҳ. Ассаламу ъалайна ва ъала ъибадиллаҳис-солиҳийн. Ашҳаду алла илаҳа иллаллоҳу ва ашҳаду анна Муҳаммадан ъабдуҳу ва росулуҳ.",
            meaning = lText(
                "Barcha maqtovlar, salovatlar va pok amallar Alloh uchundir. Ey Payg‘ambar, sizga Allohning salomi, rahmati va barakoti bo‘lsin. Bizga va Allohning solih bandalariga ham salom bo‘lsin. Guvohlik beramanki, Allohdan o‘zga iloh yo‘q va guvohlik beramanki, Muhammad Uning bandasi va elchisidir.",
                "Барча мақтовлар, саловатлар ва пок амаллар Аллоҳ учундир. Эй Пайғамбар, сизга Аллоҳнинг саломи, раҳмати ва баракоти бўлсин. Бизга ва Аллоҳнинг солиҳ бандаларига ҳам салом бўлсин. Гувоҳлик бераманки, Аллоҳдан ўзга илоҳ йўқ ва гувоҳлик бераманки, Муҳаммад Унинг бандаси ва элчисидир."
            ),
            ttsPrompt = "Attahiyyatu lillahi vas solavatu vat toyyibat. Assalamu alayka ayyuhan nabiyyu va rohmatullohi va barokatuh. Assalamu alayna va ala ibadillahis solihiyn. Ashhadu alla ilaha illallohu va ashhadu anna Muhammadan abduhu va rosuluh."
        ),
        SurahLibraryItem(
            id = "salovat",
            title = lText("Salovat (Allohumma solli va barik)", "Саловат (Аллоҳумма солли ва барик)"),
            subtitle = lText("Oxirgi qa'dada to‘liq o‘qiladi", "Охирги қаъдада тўлиқ ўқилади"),
            arabicText = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ ۝ اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ",
            translitLatin = "Allohumma solli ‘ala Muhammadin va ‘ala ali Muhammad, kama sollayta ‘ala Ibrohiyma va ‘ala ali Ibrohiym, innaka hamiydum-majiyd. Allohumma barik ‘ala Muhammadin va ‘ala ali Muhammad, kama barokta ‘ala Ibrohiyma va ‘ala ali Ibrohiym, innaka hamiydum-majiyd.",
            translitCyrillic = "Аллоҳумма солли ъала Муҳаммадин ва ъала али Муҳаммад, кама соллайта ъала Иброҳийма ва ъала али Иброҳийм, иннака ҳамийдум-мажийд. Аллоҳумма барик ъала Муҳаммадин ва ъала али Муҳаммад, кама барокта ъала Иброҳийма ва ъала али Иброҳийм, иннака ҳамийдум-мажийд.",
            meaning = lText(
                "Allohim, Ibrohim va u zotning oilasiga rahmat yog‘dirganing kabi Muhammad va u zotning oilasiga ham rahmat yog‘dirgin. Albatta, Sen maqtovga loyiq va buyuksan. Allohim, Ibrohim va u zotning oilasiga baraka berganing kabi Muhammad va u zotning oilasiga ham baraka bergin. Albatta, Sen maqtovga loyiq va buyuksan.",
                "Аллоҳим, Иброҳим ва у зотнинг оиласига раҳмат ёғдирганинг каби Муҳаммад ва у зотнинг оиласига ҳам раҳмат ёғдиргин. Албатта, Сен мақтовга лойиқ ва буюксан. Аллоҳим, Иброҳим ва у зотнинг оиласига барака берганинг каби Муҳаммад ва у зотнинг оиласига ҳам барака бергин. Албатта, Сен мақтовга лойиқ ва буюксан."
            ),
            ttsPrompt = "Allohumma solli ala Muhammadin va ala ali Muhammad, kama sollayta ala Ibrohiyma va ala ali Ibrohiym, innaka hamiydum majiyd. Allohumma barik ala Muhammadin va ala ali Muhammad, kama barokta ala Ibrohiyma va ala ali Ibrohiym, innaka hamiydum majiyd."
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
        ),
        SurahLibraryItem(
            id = "qunut",
            title = lText("Qunut duosi", "Қунут дуоси"),
            subtitle = lText("Vitr namozining 3-rakatida o‘qiladi", "Витр намозининг 3-ракатида ўқилади"),
            arabicText = "اللَّهُمَّ إِنَّا نَسْتَعِينُكَ وَنَسْتَغْفِرُكَ وَنُؤْمِنُ بِكَ وَنَتَوَكَّلُ عَلَيْكَ وَنُثْنِي عَلَيْكَ الْخَيْرَ نَشْكُرُكَ وَلَا نَكْفُرُكَ وَنَخْلَعُ وَنَتْرُكُ مَنْ يَفْجُرُكَ ۝ اللَّهُمَّ إِيَّاكَ نَعْبُدُ وَلَكَ نُصَلِّي وَنَسْجُدُ وَإِلَيْكَ نَسْعَى وَنَحْفِدُ نَرْجُو رَحْمَتَكَ وَنَخْشَى عَذَابَكَ إِنَّ عَذَابَكَ بِالْكُفَّارِ مُلْحِقٌ",
            translitLatin = "Allohumma inna nasta‘iynuka va nastag‘firuk, va nu’minu bika va natavakkalu ‘alayk, va nusniy ‘alaykal xoyr, nashkuruka va la nakfuruk, va naxla‘u va natruku may-yafjuruk. Allohumma iyyaka na‘budu va laka nusolliy va nasjud, va ilayka nas‘a va nahfid, narju rohmataka va naxsha ‘azabak, inna ‘azabaka bil kuffari mulhiq.",
            translitCyrillic = "Аллоҳумма инна настаъийнука ва настағфирук, ва нуъмину бика ва натаваккалу ъалайк, ва нусний ъалайкал хойр, нашкурука ва ла накфурук, ва нахлаъу ва натруку май-яфжурук. Аллоҳумма ийяка наъбуду ва лака нусоллий ва насжуд, ва илайка насъа ва наҳфид, наржу роҳматака ва нахша ъазабак, инна ъазабака бил куффари мулҳиқ.",
            meaning = lText(
                "Allohim, Sendan madad va mag‘firat so‘raymiz, Senga iymon keltirib tavakkal qilamiz, Senga barcha yaxshilik bilan hamd aytamiz, Senga shukr qilib kufr keltirmaymiz va Senga osiylik qiluvchidan yuz o‘giramiz. Allohim, Sengagina ibodat qilamiz, Sen uchungina namoz o‘qiymiz va sajda qilamiz, Senga tomon intilamiz va shoshilamiz, rahmatingdan umidvor bo‘lib azobingdan qo‘rqamiz. Albatta, Sening azobing kofirlarga yetguvchidir.",
                "Аллоҳим, Сендан мадад ва мағфират сўраймиз, Сенга иймон келтириб таваккал қиламиз, Сенга барча яхшилик билан ҳамд айтамиз, Сенга шукр қилиб куфр келтирмаймиз ва Сенга осийлик қилувчидан юз ўгирамиз. Аллоҳим, Сенгагина ибодат қиламиз, Сен учунгина намоз ўқиймиз ва сажда қиламиз, Сенга томон интиламиз ва шошиламиз, раҳматингдан умидвор бўлиб азобингдан қўрқамиз. Албатта, Сенинг азобинг кофирларга етгувчидир."
            ),
            ttsPrompt = "Allohumma inna nasta'iynuka va nastag'firuk, va nu'minu bika va natavakkalu alayk, va nusniy alaykal xoyr, nashkuruka va la nakfuruk, va naxla'u va natruku may yafjuruk. Allohumma iyyaka na'budu va laka nusolliy va nasjud, va ilayka nas'a va nahfid, narju rohmataka va naxsha azabak, inna azabaka bil kuffari mulhiq.",
            audioAssetId = "qunut"
        )
    )

    fun findSurahById(id: String): SurahLibraryItem =
        surahs.first { it.id == id }

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

    val purificationGuides: List<PurificationGuideItem> = listOf(
        PurificationGuideItem(
            id = "wudu_step_1",
            category = PurificationCategory.WUDU,
            stepNumber = 1,
            isFarz = false,
            title = lText("Niyat va Bismillah", "Ният ва Бисмиллаҳ"),
            ruleBadge = lText("Sunnat", "Суннат"),
            description = lText(
                "Qalbda tahorat olish niyat qilinadi va «Bismillahir Rohmanir Rohiym» deb boshlanadi.",
                "Қалбда таҳорат олиш ният қилинади ва «Бисмиллаҳир Роҳманир Роҳийм» деб бошланади."
            ),
            ttsPrompt = "Tahoratni qalbda niyat qilib, Bismillahir Rohmanir Rohiym deb boshlang."
        ),
        PurificationGuideItem(
            id = "wudu_step_2",
            category = PurificationCategory.WUDU,
            stepNumber = 2,
            isFarz = false,
            title = lText("Qo‘llarni bilakkacha yuvish", "Қўлларни билаккача ювиш"),
            ruleBadge = lText("Sunnat • 3 marta", "Суннат • 3 марта"),
            description = lText(
                "Ikki qo‘l bilaklargacha, barmoqlar oralarini hilol qilib 3 marta yuviladi.",
                "Икки қўл билакларгача, бармоқлар ораларини ҳилол қилиб 3 марта ювилади."
            ),
            ttsPrompt = "Ikki qo'lni bilakkacha va barmoqlar orasini uch marta yaxshilab yuving."
        ),
        PurificationGuideItem(
            id = "wudu_step_3",
            category = PurificationCategory.WUDU,
            stepNumber = 3,
            isFarz = false,
            title = lText("Og‘iz va burunni chayish", "Оғиз ва бурунни чайиш"),
            ruleBadge = lText("Sunnat • 3 marta", "Суннат • 3 марта"),
            description = lText(
                "Avval o‘ng qo‘l bilan 3 marta og‘iz chayiladi (ro‘zador bo‘lmasa g‘arg‘ara), so‘ng 3 marta burunga suv olib chap qo‘l bilan qoqiladi.",
                "Аввал ўнг қўл билан 3 марта оғиз чайилади (рўзадор бўлмаса ғарғара), сўнг 3 марта бурунга сув олиб чап қўл билан қоқилади."
            ),
            ttsPrompt = "Uch marta og'zingizni chaying, so'ng uch marta burunga suv olib chap qo'l bilan qoqing."
        ),
        PurificationGuideItem(
            id = "wudu_step_4",
            category = PurificationCategory.WUDU,
            stepNumber = 4,
            isFarz = true,
            title = lText("Yuzni yuvish", "Юзни ювиш"),
            ruleBadge = lText("1-FARZ • 3 marta", "1-ФАРЗ • 3 марта"),
            description = lText(
                "Soch chiqqan joydan iyak ostigacha, bir quloq yumshog‘idan ikkinchisigacha bo‘lgan yuz qismi quruq joy qoldirmay 3 marta yuviladi.",
                "Соч чиққан жойдан ияк остигача, бир қулоқ юмшоғидан иккинчисигача бўлган юз қисми қуруқ жой қолдирмай 3 марта ювилади."
            ),
            ttsPrompt = "Birinchi farz: yuzingizni peshonadan iyak ostigacha va bir quloqdan ikkinchisigacha uch marta yuving."
        ),
        PurificationGuideItem(
            id = "wudu_step_5",
            category = PurificationCategory.WUDU,
            stepNumber = 5,
            isFarz = true,
            title = lText("Qo‘llarni tirsak bilan yuvish", "Қўлларни тирсак билан ювиш"),
            ruleBadge = lText("2-FARZ • 3 marta", "2-ФАРЗ • 3 марта"),
            description = lText(
                "Avval o‘ng qo‘l, keyin chap qo‘l barmoq uchlaridan tirsaklarni ham qo‘shib 3 martadan yuviladi.",
                "Аввал ўнг қўл, кейин чап қўл бармоқ учларидан тирсакларни ҳам қўшиб 3 мартадан ювилади."
            ),
            ttsPrompt = "Ikkinchi farz: avval o'ng qo'lni, so'ng chap qo'lni tirsaklari bilan qo'shib uch martadan yuving."
        ),
        PurificationGuideItem(
            id = "wudu_step_6",
            category = PurificationCategory.WUDU,
            stepNumber = 6,
            isFarz = true,
            title = lText("Boshga mas’h tortish", "Бошга масъҳ тортиш"),
            ruleBadge = lText("3-FARZ • 1 marta", "3-ФАРЗ • 1 марта"),
            description = lText(
                "Ho‘llangan kaftlar bilan boshning kamida to‘rtdan bir qismiga mas’h tortiladi; so‘ng ko‘rsatkich barmoq bilan quloq ichi, bosh barmoq bilan quloq orqasi va qo‘l orqasi bilan bo‘yin artiladi.",
                "Ҳўлланган кафтлар билан бошнинг камида тўртдан бир қисмига масъҳ тортилади; сўнг кўрсаткич бармоқ билан қулоқ ичи, бош бармоқ билан қулоқ орқаси ва қўл орқаси билан бўйин артилади."
            ),
            ttsPrompt = "Uchinchi farz: ho'l kaftlar bilan boshingizga, quloqlaringizga va bo'yningizga bir marta mas'h torting."
        ),
        PurificationGuideItem(
            id = "wudu_step_7",
            category = PurificationCategory.WUDU,
            stepNumber = 7,
            isFarz = true,
            title = lText("Oyoqlarni to‘piq bilan yuvish", "Оёқларни тўпиқ билан ювиш"),
            ruleBadge = lText("4-FARZ • 3 marta", "4-ФАРЗ • 3 марта"),
            description = lText(
                "Avval o‘ng oyoq, keyin chap oyoq barmoqlar orasini hilol qilib to‘piqlari bilan qo‘shib 3 martadan yuviladi.",
                "Аввал ўнг оёқ, кейин чап оёқ бармоқлар орасини ҳилол қилиб тўпиқлари билан қўшиб 3 мартадан ювилади."
            ),
            ttsPrompt = "To'rtinchi farz: avval o'ng oyoqni, so'ng chap oyoqni to'piqlari bilan qo'shib uch martadan yuving."
        ),
        PurificationGuideItem(
            id = "wudu_step_8",
            category = PurificationCategory.WUDU,
            stepNumber = 8,
            isFarz = false,
            title = lText("Tahorat duosi", "Таҳорат дуоси"),
            ruleBadge = lText("Sunnat", "Суннат"),
            description = lText(
                "Tahoratdan so‘ng Qiblaga qarab: «Ashhadu alla ilaha illallohu vahdahu la sharika lah va ashhadu anna Muhammadan ‘abduhu va rosuluh» deyiladi.",
                "Таҳоратдан сўнг Қиблага қараб: «Ашҳаду алла илаҳа иллаллоҳу ваҳдаҳу ла шарика лаҳ ва ашҳаду анна Муҳаммадан ъабдуҳу ва росулуҳ» дейилади."
            ),
            ttsPrompt = "Tahorat yakunida Qiblaga qarab shahodat kalimasini ayting."
        ),
        PurificationGuideItem(
            id = "wudu_breakers_summary",
            category = PurificationCategory.WUDU_BREAKERS,
            title = lText("Tahoratni sindiruvchi asosiy holatlar", "Таҳоратни синдирувчи асосий ҳолатлар"),
            ruleBadge = lText("Hanafiy fiqhi", "Ҳанафий фиқҳи"),
            description = lText(
                "1) Old va orqa yo‘llardan biror narsa yoki yel chiqishi;\n2) Badandan qon yoki yiring chiqib, chiqqan joyidan oqib ketishi;\n3) Og‘iz to‘lib qusish;\n4) Yotib yoki biror narsaga suyanib chuqur uxlash, hushdan ketish;\n5) Ruku va sajdali namozda yonidagilar eshitadigan darajada kulish.",
                "1) Олд ва орқа йўллардан бирор нарса ёки ел чиқиши;\n2) Бадандан қон ёки йиринг чиқиб, чиққан жойидан оқиб кетиши;\n3) Оғиз тўлиб қусиш;\n4) Ётиб ёки бирор нарсага суяниб чуқур ухлаш, ҳушдан кетиш;\n5) Руку ва саждали намозда ёнидагилар эшитадиган даражада кулиш."
            ),
            ttsPrompt = "Tahoratni buzadigan holatlar: tabiiy yo'ldan chiqindilar yoki yel chiqishi, qon oqishi, og'iz to'ldirib qusish, suyanib yoki yotib uxlash va namozda kulish."
        ),
        PurificationGuideItem(
            id = "ghusl_farz",
            category = PurificationCategory.GHUSL_TAYAMMUM,
            isFarz = true,
            title = lText("G‘usl (3 ta farz)", "Ғусл (3 та фарз)"),
            ruleBadge = lText("3 ta farz", "3 та фарз"),
            description = lText(
                "G‘uslning 3 ta farzi bor:\n1) Og‘izni suv bilan to‘liq chayish (g‘arg‘ara);\n2) Burunni achishguncha suv bilan yuvish;\n3) Butun badanning har bir qismiga quruq joy qoldirmay suv yetkazish.",
                "Ғуслнинг 3 та фарзи бор:\n1) Оғизни сув билан тўлиқ чайиш (ғарғара);\n2) Бурунни ачишгунча сув билан ювиш;\n3) Бутун баданнинг ҳар бир қисмига қуруқ жой қолдирмай сув етказиш."
            ),
            ttsPrompt = "G'uslning uchta farzi: og'izni g'arg'ara qilib chayish, burunga suv tortish va butun badanni quruq joy qoldirmay yuvish."
        ),
        PurificationGuideItem(
            id = "tayammum_farz",
            category = PurificationCategory.GHUSL_TAYAMMUM,
            isFarz = true,
            title = lText("Tayammum (2 ta farz)", "Таяммум (2 та фарз)"),
            ruleBadge = lText("Uzrli holatda", "Узрли ҳолатда"),
            description = lText(
                "Suv topilmaganda yoki suv ishlatish sog‘liqqa zarar yetkazganda qilinadi:\n1) Niyat qilish;\n2) Pok tuproq, qum yoki changli toshga ikki qo‘lni urib yuzga mas’h tortish, so‘ng yana urib qo‘llarni tirsaklari bilan mas’h qilish.",
                "Сув топилмаганда ёки сув ишлатиш соғлиққа зарар етказганда қилинади:\n1) Ният қилиш;\n2) Пок тупроқ, қум ёки чангли тошга икки қўлни уриб юзга масъҳ тортиш, сўнг яна уриб қўлларни тирсаклари билан масъҳ қилиш."
            ),
            ttsPrompt = "Suv bo'lmaganda yoki kasallik sababli tayammum qilinadi: avval niyat qilib pok tuproqqa kaftlarni urib yuzga, so'ng yana urib qo'llarga tirsakkacha mas'h tortiladi."
        )
    )
}
