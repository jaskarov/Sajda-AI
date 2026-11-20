package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Gender
import com.example.domain.model.LText
import com.example.domain.model.PrayerPose
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.components.PrayerPostureIllustration
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.DeepHeroGradientEnd
import com.example.ui.theme.DeepHeroGradientStart
import com.example.ui.theme.NightGold
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.SaffronGold
import kotlinx.coroutines.launch

data class InteractivePrayerGuideStep(
    val id: String,
    val stepNumber: Int,
    val pose: PrayerPose,
    val title: LText,
    val ruleBadge: LText,
    val arabicText: String,
    val recitationLine: LText,
    val meaning: LText,
    val summary: LText,
    val maleNote: LText,
    val femaleNote: LText,
    val seatedNote: LText,
    val audioAssetId: String,
    val ttsPrompt: String
)

object InteractivePrayerGuideCatalog {
    val steps: List<InteractivePrayerGuideStep> = listOf(
        InteractivePrayerGuideStep(
            id = "step_takbir",
            stepNumber = 1,
            pose = PrayerPose.TAKBIR,
            title = lText("Niyat va Takbiri tahrima", "Ният ва Такбири таҳрима"),
            ruleBadge = lText("1-qadam • Farz", "1-қадам • Фарз"),
            arabicText = "اللَّهُ أَكْبَرُ",
            recitationLine = lText("Allohu akbar", "Аллоҳу акбар"),
            meaning = lText("Alloh eng buyukdir", "Аллоҳ энг буюкдир"),
            summary = lText(
                "Qiblaga yuzlanib qalbda niyat qilinadi va qo‘llar ko‘tarilib «Allohu akbar» aytiladi.",
                "Қиблага юзланиб қалбда ният қилинади ва қўллар кўтарилиб «Аллоҳу акбар» айтилади."
            ),
            maleNote = lText(
                "Erkaklar: bosh barmoqlar quloq yumshog‘iga tekkiziladi, kaftlar Qiblaga qaraydi.",
                "Эркаклар: бош бармоқлар қулоқ юмшоғига теккизилади, кафтлар Қиблага қарайди."
            ),
            femaleNote = lText(
                "Ayollar: qo‘llar yelka barobarida ko‘tariladi, kaftlar Qiblaga qaratiladi.",
                "Аёллар: қўллар елка баробарида кўтарилади, кафтлар Қиблага қаратилади."
            ),
            seatedNote = lText(
                "Stulda o‘qish: gavdani tik tutib o‘tirgan holda qo‘llar ko‘tarilib takbir aytiladi.",
                "Стулда ўқиш: гавдани тик тутиб ўтирган ҳолда қўллар кўтарилиб такбир айтилади."
            ),
            audioAssetId = "takbir",
            ttsPrompt = "Niyat qiling, qo'llaringizni ko'tarib Allohu akbar deng."
        ),
        InteractivePrayerGuideStep(
            id = "step_qiyom",
            stepNumber = 2,
            pose = PrayerPose.QIYOM,
            title = lText("Qiyom va Qiroat", "Қиём ва Қироат"),
            ruleBadge = lText("2-qadam • Farz", "2-қадам • Фарз"),
            arabicText = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
            recitationLine = lText(
                "Sano → Fotiha surasi → Zam sura (masalan, Ixlos)",
                "Сано → Фотиҳа сураси → Зам сура (масалан, Ихлос)"
            ),
            meaning = lText(
                "Barcha hamd olamlar Robbi Allohga xosdir...",
                "Барча ҳамд оламлар Робби Аллоҳга хосдир..."
            ),
            summary = lText(
                "Tik turib nigoh sajda qilinadigan joyga qaratiladi. 1-rakatda Sano, so‘ng Fotiha va zam sura o‘qiladi.",
                "Тик туриб нигоҳ сажда қилинадиган жойга қаратилади. 1-ракатда Сано, сўнг Фотиҳа ва зам сура ўқилади."
            ),
            maleNote = lText(
                "Erkaklar: o‘ng qo‘l chap qo‘l ustida kindik ostida bog‘lanadi.",
                "Эркаклар: ўнг қўл чап қўл устида киндик остида боғланади."
            ),
            femaleNote = lText(
                "Ayollar: o‘ng qo‘l chap qo‘l ustida ko‘krak ustiga qo‘yiladi.",
                "Аёллар: ўнг қўл чап қўл устида кўкрак устига қўйилади."
            ),
            seatedNote = lText(
                "Stulda o‘qish: stulda gavdani tik tutib o‘tirgan holda qiroat qilinadi.",
                "Стулда ўқиш: стулда гавдани тик тутиб ўтирган ҳолда қироат қилинади."
            ),
            audioAssetId = "fotiha",
            ttsPrompt = "Qiyomda tik turing va Fotiha surasini xotirjam o'qing."
        ),
        InteractivePrayerGuideStep(
            id = "step_ruku",
            stepNumber = 3,
            pose = PrayerPose.RUKU,
            title = lText("Ruku (Beldan egilish)", "Руку (Белдан эгилиш)"),
            ruleBadge = lText("3-qadam • Farz", "3-қадам • Фарз"),
            arabicText = "سُبْحَانَ رَبِّيَ الْعَظِيمِ",
            recitationLine = lText(
                "Subhana Robbiyal ‘Aziym (3 marta)",
                "Субҳана Роббиял Ъазийм (3 марта)"
            ),
            meaning = lText("Ulug‘ Robbim barcha nuqsonlardan pokdir", "Улуғ Роббим барча нуқсонлардан покдир"),
            summary = lText(
                "«Allohu akbar» deb beldan egilinadi, qo‘llar tizzaga qo‘yilib uch marta tasbeh aytiladi, so‘ng «Sami’allohu liman hamidah» deb tiklaniladi.",
                "«Аллоҳу акбар» деб белдан эгилинади, қўллар тиззага қўйилиб уч марта тасбеҳ айтилади, сўнг «Самиъаллоҳу лиман ҳамидаҳ» деб тикланилади."
            ),
            maleNote = lText(
                "Erkaklar: bel va bosh bir tekis 90° bo‘ladi, barmoqlar orasi ochiq holda tizzani tutadi.",
                "Эркаклар: бел ва бош бир текис 90° бўлади, бармоқлар ораси очиқ ҳолда тиззани тутади."
            ),
            femaleNote = lText(
                "Ayollar: biroz kamroq egilinadi, barmoqlar jips holda tizzalar ustiga qo‘yiladi.",
                "Аёллар: бироз камроқ эгилинади, бармоқлар жипс ҳолда тиззалар устига қўйилади."
            ),
            seatedNote = lText(
                "Stulda o‘qish: bosh va gavda oldinga yengil (15°–30°) egiladi.",
                "Стулда ўқиш: бош ва гавда олдинга енгил (15°–30°) эгилади."
            ),
            audioAssetId = "ruku",
            ttsPrompt = "Rukuga egilib uch marta Subhana Robbiyal Aziym deng."
        ),
        InteractivePrayerGuideStep(
            id = "step_sajda",
            stepNumber = 4,
            pose = PrayerPose.SAJDA,
            title = lText("Ikki marta Sajda", "Икки марта Сажда"),
            ruleBadge = lText("4-qadam • Farz", "4-қадам • Фарз"),
            arabicText = "سُبْحَانَ رَبِّيَ الْأَعْلَى",
            recitationLine = lText(
                "Subhana Robbiyal A‘la (3 marta)",
                "Субҳана Роббиял Аъла (3 марта)"
            ),
            meaning = lText("Eng oliy Robbim barcha nuqsonlardan pokdir", "Энг олий Роббим барча нуқсонлардан покдир"),
            summary = lText(
                "Peshona va burun joynamozga qo‘yilib 3 marta tasbeh aytiladi. So‘ng qisqa o‘tirib (Jalsa) ikkinchi sajda bajariladi.",
                "Пешона ва бурун жойнамозга қўйилиб 3 марта тасбеҳ айтилади. Сўнг қисқа ўтириб (Жалса) иккинчи сажда бажарилади."
            ),
            maleNote = lText(
                "Erkaklar: tirsaklar yerga tegmaydi va biqindan uzoqroq tutiladi.",
                "Эркаклар: тирсаклар ерга тегмайди ва биқиндан узоқроқ тутилади."
            ),
            femaleNote = lText(
                "Ayollar: qo‘llar va gavda yerga yaqinroq, ixcham holda saqlanadi.",
                "Аёллар: қўллар ва гавда ерга яқинроқ, ихчам ҳолда сақланади."
            ),
            seatedNote = lText(
                "Stulda o‘qish: sajda uchun ruku'ga qaraganda chuqurroq (45°–60°) egilinadi.",
                "Стулда ўқиш: сажда учун рукуъга қараганда чуқурроқ (45°–60°) эгилинади."
            ),
            audioAssetId = "sajda",
            ttsPrompt = "Sajdaga boring va uch marta Subhana Robbiyal A'la deng."
        ),
        InteractivePrayerGuideStep(
            id = "step_qada",
            stepNumber = 5,
            pose = PrayerPose.QADA,
            title = lText("Qa’da (O‘tirish)", "Қаъда (Ўтириш)"),
            ruleBadge = lText("5-qadam • Vojib / Farz", "5-қадам • Вожиб / Фарз"),
            arabicText = "التَّحِيَّاتُ لِلَّهِ وَالصَّلَوَاتُ وَالطَّيِّبَاتُ",
            recitationLine = lText(
                "Attahiyat • Oxirgi qa’dada: Salovat + Robbana",
                "Аттаҳият • Охирги қаъдада: Саловат + Роббана"
            ),
            meaning = lText(
                "Barcha maqtovlar, salovatlar va pok amallar Alloh uchundir...",
                "Барча мақтовлар, саловатлар ва пок амаллар Аллоҳ учундир..."
            ),
            summary = lText(
                "Har 2-rakat oxirida xotirjam o‘tirib Attahiyat o‘qiladi. Oxirgi rakatda Salovat va Robbana duosi qo‘shiladi.",
                "Ҳар 2-ракат охирида хотиржам ўтириб Аттаҳият ўқилади. Охирги ракатда Саловат ва Роббана дуоси қўшилади."
            ),
            maleNote = lText(
                "Erkaklar: chap oyoq yotqizilib ustiga o‘tiriladi, o‘ng oyoq barmoqlari Qiblaga qaratiladi.",
                "Эркаклар: чап оёқ ётқизилиб устига ўтирилади, ўнг оёқ бармоқлари Қиблага қаратилади."
            ),
            femaleNote = lText(
                "Ayollar: ikki oyoq o‘ng tomonga chiqarilib ixcham o‘tiriladi.",
                "Аёллар: икки оёқ ўнг томонга чиқарилиб ихчам ўтирилади."
            ),
            seatedNote = lText(
                "Stulda o‘qish: stulda tik va xotirjam o‘tirib, qo‘llar son ustiga qo‘yiladi.",
                "Стулда ўқиш: стулда тик ва хотиржам ўтириб, қўллар сон устига қўйилади."
            ),
            audioAssetId = "attahiyat",
            ttsPrompt = "Qa'dada xotirjam o'tiring va Attahiyatni o'qing."
        ),
        InteractivePrayerGuideStep(
            id = "step_salam",
            stepNumber = 6,
            pose = PrayerPose.SALAM,
            title = lText("Salom berib yakunlash", "Салом бериб якунлаш"),
            ruleBadge = lText("6-qadam • Yakun", "6-қадам • Якун"),
            arabicText = "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللَّهِ",
            recitationLine = lText(
                "Assalamu alaykum va rohmatulloh (o‘ngga va chapga)",
                "Ассаламу алайкум ва роҳматуллоҳ (ўнгга ва чапга)"
            ),
            meaning = lText(
                "Sizlarga Allohning tinchligi va rahmati bo‘lsin",
                "Сизларга Аллоҳнинг тинчлиги ва раҳмати бўлсин"
            ),
            summary = lText(
                "Avval o‘ng yelkaga, so‘ng chap yelkaga qarab salom beriladi. Shu bilan namoz mukammal yakunlanadi!",
                "Аввал ўнг елкага, сўнг чап елкага қараб салом берилади. Шу билан намоз мукаммал якунланади!"
            ),
            maleNote = lText(
                "Bosh o‘ng va chap yelkaga qarab xotirjam buriladi, gavda qimirlamaydi.",
                "Бош ўнг ва чап елкага қараб хотиржам бурилади, гавда қимирламайди."
            ),
            femaleNote = lText(
                "Bosh avval o‘ng, so‘ng chap yelkaga muloyim burilib salom beriladi.",
                "Бош аввал ўнг, сўнг чап елкага мулойим бурилиб салом берилади."
            ),
            seatedNote = lText(
                "Stulda o‘qish: o‘tirgan holda boshni avval o‘ngga, keyin chapga burib salom beriladi.",
                "Стулда ўқиш: ўтирган ҳолда бошни аввал ўнгга, кейин чапга буриб салом берилади."
            ),
            audioAssetId = "salam",
            ttsPrompt = "Avval o'ngga, so'ng chapga qarab Assalamu alaykum va rohmatulloh deb salom bering."
        )
    )
}

@Composable
fun PrayerGuideOnboardingScreen(
    initialGender: Gender = Gender.MALE,
    initialScript: TextScript = TextScript.LATIN,
    initialSeatedMode: Boolean = false,
    isFirstLaunch: Boolean = true,
    onPlayAudio: (prompt: String, audioAssetId: String) -> Unit = { _, _ -> },
    onPreferencesUpdated: (gender: Gender, script: TextScript, seatedMode: Boolean) -> Unit = { _, _, _ -> },
    onStartPractice: (() -> Unit)? = null,
    onFinish: (gender: Gender, script: TextScript, seatedMode: Boolean) -> Unit
) {
    var gender by remember(initialGender) { mutableStateOf(initialGender) }
    var script by remember(initialScript) { mutableStateOf(initialScript) }
    var seatedMode by remember(initialSeatedMode) { mutableStateOf(initialSeatedMode) }
    var masteredStepIds by remember { mutableStateOf(setOf<String>()) }

    val steps = InteractivePrayerGuideCatalog.steps
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    if (!isFirstLaunch) {
        BackHandler {
            onFinish(gender, script, seatedMode)
        }
    }

    val currentStep = steps[pagerState.currentPage]
    val isLastPage = pagerState.currentPage == steps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Header: Progress Title + Script Toggle + Skip / Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isFirstLaunch) {
                        IconButton(
                            onClick = { onFinish(gender, script, seatedMode) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("onboarding_guide_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = lText("Yopish", "Ёпиш").get(script)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = lText(
                                "NAMOZ TARTIBI • ${pagerState.currentPage + 1} / ${steps.size}",
                                "НАМОЗ ТАРТИБИ • ${pagerState.currentPage + 1} / ${steps.size}"
                            ).get(script),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lText(
                                "Bosqichma-bosqich qo‘llanma",
                                "Босқичма-босқич қўлланма"
                            ).get(script),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isFirstLaunch) {
                        TextButton(
                            onClick = { onFinish(gender, script, seatedMode) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("onboarding_guide_skip_button")
                        ) {
                            Text(
                                text = lText("O‘tkazish", "Ўтказиш").get(script),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Prominent Latin / Cyrillic Alphabet Selector right at the top of screen 1
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("guide_script_selector_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isLatin = script == TextScript.LATIN
                    Surface(
                        onClick = {
                            if (script != TextScript.LATIN) {
                                script = TextScript.LATIN
                                onPreferencesUpdated(gender, script, seatedMode)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLatin) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(if (!isLatin) "guide_script_toggle" else "guide_script_latin_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .then(
                                    if (!isLatin) Modifier.testTag("guide_script_latin_button")
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "O‘zbekcha (Lotin)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isLatin) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLatin) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (script != TextScript.CYRILLIC) {
                                script = TextScript.CYRILLIC
                                onPreferencesUpdated(gender, script, seatedMode)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isLatin) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(if (isLatin) "guide_script_toggle" else "guide_script_cyrillic_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .then(
                                    if (isLatin) Modifier.testTag("guide_script_cyrillic_button")
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ўзбекча (Кирилл)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (!isLatin) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isLatin) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Interactive Tappable Stepper (1..6) with Mastery Checkmarks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { idx, step ->
                    val isCurrent = idx == pagerState.currentPage
                    val isMastered = step.id in masteredStepIds
                    val pillColor by animateColorAsState(
                        targetValue = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isMastered -> PoseMatchedGreen
                            idx < pagerState.currentPage -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        label = "step_pill_color_$idx"
                    )
                    val height by animateDpAsState(
                        targetValue = if (isCurrent) 32.dp else 26.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "step_pill_h_$idx"
                    )

                    Box(
                        modifier = Modifier
                            .weight(if (isCurrent) 1.5f else 1f)
                            .height(height)
                            .clip(CircleShape)
                            .background(pillColor)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(idx) }
                            }
                            .testTag("guide_step_tab_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMastered && !isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = step.title.get(script),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = if (isCurrent) "${step.stepNumber}. ${step.pose.title.get(script)}" else "${step.stepNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent || isMastered || idx < pagerState.currentPage) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            // Live Interactive Mode Switcher (Erkak/Ayol & Tik/Stulda)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = gender == Gender.MALE,
                    onClick = {
                        gender = Gender.MALE
                        onPreferencesUpdated(gender, script, seatedMode)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Man, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(Gender.MALE.label.get(script), maxLines = 1) },
                    modifier = Modifier.testTag("guide_gender_male")
                )
                FilterChip(
                    selected = gender == Gender.FEMALE,
                    onClick = {
                        gender = Gender.FEMALE
                        onPreferencesUpdated(gender, script, seatedMode)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Woman, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(Gender.FEMALE.label.get(script), maxLines = 1) },
                    modifier = Modifier.testTag("guide_gender_female")
                )
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = seatedMode,
                    onClick = {
                        seatedMode = !seatedMode
                        onPreferencesUpdated(gender, script, seatedMode)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Accessible, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = {
                        Text(
                            text = lText("Stulda", "Стулда").get(script),
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("guide_mode_seated")
                )
            }

            // Swipeable Interactive Step Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 12.dp
            ) { page ->
                val step = steps[page]
                val isMastered = step.id in masteredStepIds
                PrayerGuideStepPage(
                    step = step,
                    gender = gender,
                    script = script,
                    seatedMode = seatedMode,
                    isMastered = isMastered,
                    onPlayAudio = { onPlayAudio(step.ttsPrompt, step.audioAssetId) },
                    onToggleMastered = {
                        masteredStepIds = if (isMastered) {
                            masteredStepIds - step.id
                        } else {
                            masteredStepIds + step.id
                        }
                    }
                )
            }

            // Sticky Bottom Navigation Controls
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(52.dp)
                                    .testTag("guide_prev_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(lText("Oldingi", "Олдинги").get(script))
                            }
                        }

                        if (!isLastPage) {
                            Button(
                                onClick = {
                                    masteredStepIds = masteredStepIds + currentStep.id
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                                    .testTag("guide_next_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = lText("Keyingi qadam", "Кейинги қадам").get(script),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    masteredStepIds = masteredStepIds + currentStep.id
                                    onFinish(gender, script, seatedMode)
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                                    .testTag("guide_finish_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PoseMatchedGreen,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (isFirstLaunch) {
                                        lText("Sozlashga o‘tish", "Созлашга ўтиш").get(script)
                                    } else {
                                        lText("Tushunarli, yopish", "Тушунарли, ёпиш").get(script)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    if (isLastPage && !isFirstLaunch && onStartPractice != null) {
                        FilledTonalButton(
                            onClick = onStartPractice,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("guide_start_practice_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = lText(
                                    "Hozir 2 rakat Bomdod bilan mashq qilish",
                                    "Ҳозир 2 ракат Бомдод билан машқ қилиш"
                                ).get(script),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerGuideStepPage(
    step: InteractivePrayerGuideStep,
    gender: Gender,
    script: TextScript,
    seatedMode: Boolean,
    isMastered: Boolean,
    onPlayAudio: () -> Unit,
    onToggleMastered: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Animated Posture Sanctuary Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(DeepHeroGradientStart, DeepHeroGradientEnd)
                    )
                )
        ) {
            PrayerPostureIllustration(
                pose = step.pose,
                gender = gender,
                seatedMode = seatedMode,
                modifier = Modifier.fillMaxSize()
            )

            // Top-left rule badge
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.38f),
                border = BorderStroke(1.dp, NightGold.copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = step.ruleBadge.get(script),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFE7B3),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            // Bottom-right Listen Audio pill (plays bundled offline Qori MP3)
            Button(
                onClick = onPlayAudio,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronGold,
                    contentColor = Color(0xFF1B1400)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .heightIn(min = 42.dp)
                    .testTag("guide_listen_${step.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = lText("Ovozli tinglash", "Овозли тинглаш").get(script),
                        tint = Color(0xFF1B1400),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = lText("Tinglash", "Тинглаш").get(script),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1400)
                    )
                }
            }
        }

        // Title & Recitation Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${step.pose.emoji} ${step.title.get(script)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = step.summary.get(script),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = step.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 25.sp,
                            lineHeight = 38.sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = step.recitationLine.get(script),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = step.meaning.get(script),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Dynamic Hanafi Fiqh Rule Card (updates live when Gender or Seated Mode toggles)
        val activeNote = when {
            seatedMode -> step.seatedNote.get(script)
            gender == Gender.FEMALE -> step.femaleNote.get(script)
            else -> step.maleNote.get(script)
        }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (seatedMode) {
                        lText("Stulda / Imo-ishora qoidasi", "Стулда / Имо-ишора қоидаси").get(script)
                    } else {
                        lText("Hanafiy fiqhiy eslatma", "Ҳанафий фиқҳий эслатма").get(script)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                AnimatedContent(targetState = activeNote, label = "fiqh_note_transition") { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.testTag("guide_active_fiqh_note_${step.id}")
                    )
                }
            }
        }

        // Interactive Mastery Check Chip
        Surface(
            onClick = onToggleMastered,
            shape = RoundedCornerShape(16.dp),
            color = if (isMastered) PoseMatchedGreen.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = if (isMastered) 1.5.dp else 1.dp,
                color = if (isMastered) PoseMatchedGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("guide_mark_understood_${step.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isMastered) PoseMatchedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isMastered) {
                        lText("Bu qadam tushunildi ✓", "Бу қадам тушунилди ✓").get(script)
                    } else {
                        lText("Bu qadamni tushundim deb belgilash", "Бу қадамни тушундим деб белгилаш").get(script)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMastered) PoseMatchedGreen else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}
