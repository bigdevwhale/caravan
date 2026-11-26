package app.caravan.domain

import app.caravan.data.Errand
import app.caravan.data.State
import app.caravan.trip.Geo

/**
 * The city's residents (#16): twelve people, one in each of the first quarters the merchant keeps coming back to
 * (three visits). Each has a little story in three requests – a letter to carry, goods to bring, a rarity from the
 * night market – with no deadline: long errands, unlike the guild's contracts. A finished story goes into the
 * book of thanks.
 */
object Folk {
    const val VISITS = 3
    const val MAX_TAKEN = 3
    val gold = listOf(50, 90, 160)
    val renown = listOf(10, 20, 40)

    sealed class Ask {
        /** Carry a letter to another quarter (picked when the request is taken). */
        object Letter : Ask()
        /** Bring [n] of [good] here. */
        class Bring(val good: String, val n: Int) : Ask()
        /** Bring a good bought at least [km] away from here. */
        class Far(val km: Int = 5) : Ask()
    }

    /** One request: its words ({q} – the letter's quarter) and what it asks. */
    class Step(val ru: String, val en: String, val ask: Ask)

    class Resident(val ru: String, val en: String, val roleRu: String, val roleEn: String, val helloRu: String, val helloEn: String,
                   val thanksRu: String, val thanksEn: String, val steps: List<Step>) {
        val name get() = tr(ru, en)
        val role get() = tr(roleRu, roleEn)
        val hello get() = tr(helloRu, helloEn)
        val thanks get() = tr(thanksRu, thanksEn)
    }

    private fun letter(ru: String, en: String) = Step(ru, en, Ask.Letter)
    private fun bring(good: String, n: Int, ru: String, en: String) = Step(ru, en, Ask.Bring(good, n))
    private fun far(ru: String, en: String) = Step(ru, en, Ask.Far())

    val all = listOf(
        Resident("Ширин", "Shirin", "вышивальщица", "the embroiderer",
            "Купец! Глаза у меня уже не те, а работы полон дом. Поможешь?", "Merchant! My eyes aren't what they were, and the house is full of work. Will you help?",
            "Я вышила твой караван на платке – пусть бережёт тебя в дороге.", "I've embroidered your caravan on a kerchief – may it keep you on the road.",
            listOf(
                letter("Отвези письмо сестре в квартал {q}. Почтальон теряет уже десятое.", "Take a letter to my sister in {q}. The postman has lost ten already."),
                bring("yarn", 3, "Нитки кончились, а заказ горит. Привези три мотка крашеной пряжи.", "I'm out of thread and the order is due. Bring three skeins of dyed yarn."),
                bring("dust", 1, "Хочу, чтобы узор светился в темноте. Говорят, для этого нужна пыльца фей с ночного рынка.", "I want the pattern to glow in the dark. They say it takes fairy dust from the night market."),
            )),
        Resident("Хасан", "Hasan", "отец трёх дочерей", "a father of three daughters",
            "Еду на ярмарку не я, а мои просьбы. Дочки всё заказали – выручишь?", "It's my wishes going to the fair, not me. My daughters have ordered it all – will you help?",
            "Все три довольны, а младшая говорит, что ты – добрый волшебник.", "All three are happy, and the youngest says you're a kind wizard.",
            listOf(
                bring("silk", 2, "Старшая просит шёлку на платье. Привези два отреза.", "The eldest wants silk for a dress. Bring two lengths."),
                bring("rose", 2, "Средняя – розового масла. Два флакона, не меньше.", "The middle one wants rose oil. Two flasks, no fewer."),
                bring("moonlight", 1, "А младшая просит аленький цветочек. Такого нет нигде, но на ночном рынке бывает лунный свет в клетке – она согласна и на него.", "And the youngest asks for a little scarlet flower. There's none anywhere, but the night market sometimes has caged moonlight – she'll take that."),
            )),
        Resident("Касым", "Kasym", "сторож моста", "the bridge keeper",
            "Сорок лет стерегу мост, а дальше него не бывал. Расскажешь, что там?", "Forty years I've kept this bridge and never been past it. Will you tell me what's out there?",
            "Теперь я знаю, что город больше моего моста. Спасибо, что был моими глазами.", "Now I know the city is bigger than my bridge. Thank you for being my eyes.",
            listOf(
                letter("Отнеси весточку старому другу в {q}. Мы не виделись с молодости.", "Take word to an old friend in {q}. We haven't met since we were young."),
                far("Привези что-нибудь издалека – купленное не ближе 5 км отсюда. Хочу знать, чем пахнут другие кварталы.", "Bring me something from afar – bought at least 5 km from here. I want to know how other quarters smell."),
                letter("Друг ответил! Отвези ему моё письмо – он теперь живёт у внука, в квартале {q}.", "My friend wrote back! Take him my letter – he lives with his grandson now, in {q}."),
            )),
        Resident("Лейла", "Leila", "девочка с воздушным змеем", "a girl with a kite",
            "Ты на повозке? Значит, ты быстрее моего змея! Мне очень нужна помощь.", "You have a cart? Then you're faster than my kite! I really need help.",
            "Шкатулка играет каждый вечер. Я назвала её в твою честь!", "The box plays every evening. I named it after you!",
            listOf(
                letter("Отвези бабушке в {q} мой рисунок. Там нарисован ты!", "Take my drawing to granny in {q}. You're in it!"),
                bring("candles", 2, "Скоро праздник фонарей, а у нас ни одной свечки. Привези две?", "The lantern festival is coming and we have no candles. Bring two?"),
                bring("musicbox", 1, "Я мечтаю о шкатулке-яйце с ночного рынка. Коплю уже целый год, но у меня только три монетки…", "I dream of an egg music box from the night market. I've saved for a year, but I only have three coins…"),
            )),
        Resident("Юсуф", "Yusuf", "лекарь", "the healer",
            "Больных много, а снадобий мало. Купец, без тебя мне не справиться.", "Many patients, few remedies. Merchant, I can't manage without you.",
            "Ты вылечил полквартала, даже не заходя в дома. Двери лекарни для тебя открыты всегда.", "You've healed half the quarter without entering a house. My door is always open to you.",
            listOf(
                bring("herbs", 3, "Привези три связки целебных трав – зима близко.", "Bring three bundles of healing herbs – winter is near."),
                far("Нужна редкая трава, что растёт далеко. Привези любой товар, купленный не ближе 5 км отсюда, – там и спрошу.", "I need a rare herb that grows far away. Bring any good bought at least 5 km from here – I'll ask its seller."),
                bring("starsalt", 1, "Последнее: звёздная соль с ночного рынка. Ею лечат тоску.", "The last: star salt from the night market. It cures melancholy."),
            )),
        Resident("Мариам", "Mariam", "хозяйка чайханы", "the teahouse keeper",
            "Открываю второй зал, а чая и пряностей – на донышке. Выручай!", "I'm opening a second room and the tea and spice are nearly gone. Help me out!",
            "Твоя пиала стоит на полке – наливаю в неё только тебе.", "Your bowl stands on the shelf – I pour from it only for you.",
            listOf(
                bring("tea", 3, "Привези три тюка заморского чая.", "Bring three bales of faraway tea."),
                bring("spice", 2, "К плову нужны пряности – два мешочка.", "The pilaf needs spice – two pouches."),
                letter("Отвези приглашение на открытие в квартал {q} – там живут мои лучшие гости.", "Take an invitation to the opening to {q} – my best guests live there."),
            )),
        Resident("Рустам", "Rustam", "ученик часовщика", "a clockmaker's apprentice",
            "Мастер уехал, а часы на башне встали. Если не починю – меня выгонят!", "My master's away and the tower clock has stopped. If I don't fix it, I'm out!",
            "Часы идут, мастер вернулся и ничего не заметил. Тссс!", "The clock runs, the master's back and noticed nothing. Shhh!",
            listOf(
                bring("cogs", 2, "Нужны две гномьи шестерни – старые стёрлись.", "I need two dwarf cogs – the old ones are worn."),
                bring("copper", 2, "И медная проволока для боя – две катушки.", "And copper wire for the chime – two coils."),
                bring("clock", 1, "Без часов-компаса стрелки не выставить. Их продают только ночью.", "I can't set the hands without a clock compass. They're sold only at night."),
            )),
        Resident("Зейнаб", "Zeinab", "сказительница", "the storyteller",
            "Я знаю тысячу сказок, но ни одной – про тебя. Поможешь сочинить?", "I know a thousand tales but none about you. Will you help me make one?",
            "Теперь у города есть сказка о купце. Её рассказывают детям на ночь.", "Now the city has a tale of the merchant. It's told to children at bedtime.",
            listOf(
                letter("Отвези в квартал {q} начало моей новой сказки – пусть читают и ждут продолжения.", "Take the beginning of my new tale to {q} – let them read and wait for more."),
                bring("ink", 2, "Чтобы записать сказку, нужны звёздные чернила. Две склянки.", "To write the tale I need star ink. Two vials."),
                bring("moonlight", 1, "Конец сказки должен светиться. Привези лунный свет в клетке.", "The end of the tale must shine. Bring caged moonlight."),
            )),
        Resident("Ахмед", "Ahmed", "рыбак без лодки", "a fisherman without a boat",
            "Лодку унесло в половодье. Строю новую – подсобишь?", "The flood took my boat. I'm building a new one – lend a hand?",
            "Лодка на воде! Первая рыба – твоя, и катаю тебя бесплатно до старости.", "The boat's on the water! The first fish is yours, and free rides until we're old.",
            listOf(
                bring("iron", 2, "На гвозди нужно кованое железо – два бруска.", "I need wrought iron for nails – two bars."),
                bring("resin", 2, "Швы смолят смолой древня. Два кувшина.", "The seams are sealed with old-tree resin. Two jars."),
                letter("Отвези в квартал {q} весть: лодка готова, пусть приходят кататься.", "Take word to {q}: the boat is ready, come for a ride."),
            )),
        Resident("Нигора", "Nigora", "садовница на крыше", "the rooftop gardener",
            "У меня сад на крыше, и он мал! Нужны горшки и семена издалека.", "I have a garden on the roof, and it's too small! I need pots and seeds from afar.",
            "На крыше цветёт всё, что ты привёз. Приходи пить чай среди роз.", "Everything you brought blooms on the roof. Come for tea among the roses.",
            listOf(
                bring("jug", 2, "Привези два расписных кувшина – под цветы.", "Bring two painted jugs – for the flowers."),
                far("Семена нужны из дальних кварталов: привези товар, купленный не ближе 5 км отсюда.", "The seeds must come from far quarters: bring a good bought at least 5 km from here."),
                bring("dust", 1, "Чтобы цвело и зимой, нужна пыльца фей.", "To bloom in winter too, it needs fairy dust."),
            )),
        Resident("Бахтияр", "Bakhtiyar", "картограф-самоучка", "a self-taught mapmaker",
            "Рисую карту города, но дальше трёх улиц не хожу – ноги. Сходишь за меня?", "I'm drawing a map of the city but can't walk past three streets – my legs. Will you go for me?",
            "Карта готова, и твой путь на ней – золотой нитью.", "The map is done, and your route is on it in golden thread.",
            listOf(
                letter("Отвези мой вопрос в квартал {q}: какие там улицы? Пусть ответят на обороте.", "Take my question to {q}: what are its streets like? Let them answer on the back."),
                letter("Теперь – в квартал {q}. Там, говорят, есть улица, которой нет на картах.", "Now to {q}. They say there's a street there that's on no map."),
                bring("maps", 2, "Последний штрих: две карты дальних стран – сверить края.", "The last touch: two maps of far lands, to check the edges."),
            )),
        Resident("Гульчехра", "Gulchehra", "бабушка у окна", "the grandmother at the window",
            "Внук уехал в другой квартал и не пишет. Сердце болит.", "My grandson moved to another quarter and doesn't write. My heart aches.",
            "Внук приезжает по воскресеньям. Это ты его привёз – я знаю.", "My grandson visits on Sundays. You brought him back – I know it.",
            listOf(
                letter("Отвези внуку письмо, он в квартале {q}.", "Take my grandson a letter, he's in {q}."),
                bring("yarn", 2, "Свяжу ему носки. Привези два мотка пряжи.", "I'll knit him socks. Bring two skeins of yarn."),
                letter("Носки готовы. Отвези их в {q} – он переехал, вот новый адрес.", "The socks are ready. Take them to {q} – he's moved, here's the new address."),
            )),
    )

    fun of(e: Errand) = all[e.who]
    fun step(e: Errand) = of(e).steps.getOrNull(e.step)
    fun done(e: Errand) = e.step >= of(e).steps.size
    fun at(s: State, quarter: String) = s.folk.firstOrNull { it.quarter == quarter }
    fun taken(s: State) = s.folk.count { it.since > 0 && !done(it) }

    /** The request's words with its quarter's name in. */
    fun text(s: State, e: Errand): String {
        val st = step(e) ?: return ""
        // the letter's quarter is known before the request is taken, so its words always name it
        val q = s.quarters[if (e.since > 0) e.target else pickTarget(s, e)]?.name.orEmpty()
        return tr(st.ru, st.en).replace("{q}", "«$q»")
    }

    /** A quarter visited often enough gets the next resident without a home, while any are left. */
    fun settle(s: State): State {
        val free = all.indices.filter { i -> s.folk.none { it.who == i } }
        if (free.isEmpty()) return s
        val ready = s.quarters.values.filter { it.known && it.visits >= VISITS && s.folk.none { e -> e.quarter == it.id } }.sortedBy { it.firstDay * 1000 + hash(it.id).mod(1000) }
        if (ready.isEmpty()) return s
        val add = ready.zip(free.shuffled(kotlin.random.Random(hash("folk:" + s.homeLat + ":" + s.homeLon)))).map { (q, i) -> Errand(q.id, i) }
        return s.copy(folk = s.folk + add)
    }

    /** Where a letter goes: another quarter 2–8 km away, any one if none is; never the last one again. */
    fun pickTarget(s: State, e: Errand): String {
        val home = s.quarters[e.quarter] ?: return ""
        val others = s.quarters.values.filter { it.id != home.id && it.id != e.target }.sortedBy { it.id }
        if (others.isEmpty()) return ""
        val near = others.filter { Geo.distance(home.lat, home.lon, it.lat, it.lon) in 2000.0..8000.0 }.ifEmpty { others }
        return near[Rand(hash("letter:${e.quarter}:${e.step}")).int(near.size)].id
    }

    /** The bales in the pack that would do for this request. */
    fun fitting(s: State, e: Errand): List<app.caravan.data.Lot> {
        val home = s.quarters[e.quarter] ?: return emptyList()
        return when (val a = step(e)?.ask) {
            is Ask.Bring -> s.pack.filter { it.good == a.good }.take(a.n)
            is Ask.Far -> s.pack.filter { l -> s.quarters[l.origin]?.let { Geo.distance(it.lat, it.lon, home.lat, home.lon) >= a.km * 1000 } == true }.take(1)
            else -> emptyList()
        }
    }

    /** (done so far, needed) for a taken request. */
    fun progress(s: State, e: Errand): Pair<Int, Int> = when (val a = step(e)?.ask) {
        Ask.Letter -> (if (e.arrived) 1 else 0) to 1
        is Ask.Bring -> fitting(s, e).size to a.n
        is Ask.Far -> fitting(s, e).size to 1
        null -> 1 to 1
    }

    /** Ready to hand over: a letter delivered (from anywhere), goods only in the resident's own quarter. */
    fun ready(s: State, e: Errand): Boolean {
        if (e.since == 0L || done(e)) return false
        val (a, b) = progress(s, e)
        return a >= b && (step(e)?.ask == Ask.Letter || s.here == e.quarter)
    }

    /** Plain words on how to do it. */
    fun how(s: State, e: Errand): String {
        val home = s.quarters[e.quarter]
        return when (val a = step(e)?.ask) {
            Ask.Letter -> s.quarters[e.target]?.let { q ->
                val km = home?.let { Geo.distance(it.lat, it.lon, q.lat, q.lon) / 1000 } ?: 0.0
                tr("Нажмите «В путь» и заедьте в квартал «${q.ru}» (${fmt1(km)} км отсюда). Письмо отдадут само, а отчитаться можно откуда угодно.",
                    "Press «Set off» and ride into «${q.en}» (${fmt1(km)} km from here). The letter delivers itself; report back from anywhere.")
            }.orEmpty()
            is Ask.Bring -> Goods[a.good].let { g ->
                if (g.rare) tr("«${g.ru}» бывает только на ночном рынке – он открывается после поездки в темноте. Купите и привезите сюда.",
                    "«${g.en}» is found only at the night market – it opens after a ride in the dark. Buy it and bring it here.")
                else tr("«${g.ru}» продают кварталы вида «${g.kind!!.ru}». Купите ${a.n} шт. и привезите сюда, в «${home?.ru}».",
                    "«${g.en}» is sold in «${g.kind!!.en}» quarters. Buy ${a.n} and bring them here, to «${home?.en}».")
            }
            is Ask.Far -> tr("Купите любой товар в квартале не ближе ${a.km} км отсюда и привезите сюда, в «${home?.ru}».",
                "Buy any good in a quarter at least ${a.km} km from here and bring it here, to «${home?.en}».")
            null -> ""
        }
    }

    /** The picture of the current request. */
    fun pic(e: Errand): String = when (val a = step(e)?.ask) {
        Ask.Letter -> "ic_scroll"
        is Ask.Bring -> Goods[a.good].pic
        is Ask.Far -> "ic_map"
        null -> "ic_seal"
    }
}
