package app.caravan.domain

import app.caravan.data.State
import app.caravan.trip.Geo

/**
 * Eight merchants, one for each kind of quarter. You meet one in the first quarter of their kind; each has a
 * chain of four requests with a little story, and the last makes them a friend: their quarters then sell
 * one more of each good, 10% cheaper.
 */
object Merchants {
    /** What a request asks, counted from the moment it was given ([since]). */
    sealed class Goal {
        /** Sell [n] goods of [what] kind in quarters of kind [where]. */
        class Deliver(val what: Kind, val where: Kind, val n: Int) : Goal()
        /** Sell [n] of good [good] anywhere but its own kind. */
        class Sell(val good: String, val n: Int) : Goal()
        class NewQuarters(val n: Int) : Goal()
        object NightRide : Goal()
        object Underground : Goal()
        class Far(val km: Int) : Goal()
        class Profit(val n: Int) : Goal()
        class CurioOf(val context: String) : Goal()
        class Trips(val n: Int) : Goal()
    }

    class Step(val ru: String, val en: String, val goal: Goal, val gold: Int, val renown: Int) {
        val text get() = tr(ru, en)
    }

    class Merchant(val kind: Kind, val ru: String, val en: String, val roleRu: String, val roleEn: String,
                   val helloRu: String, val helloEn: String, val byeRu: String, val byeEn: String, val steps: List<Step>) {
        val id get() = kind.id
        val name get() = tr(ru, en)
        val role get() = tr(roleRu, roleEn)
        val hello get() = tr(helloRu, helloEn)
        val bye get() = tr(byeRu, byeEn)
        val pic get() = "mer_${kind.id}"
    }

    private fun s(ru: String, en: String, g: Goal, gold: Int, renown: Int) = Step(ru, en, g, gold, renown)

    val all = listOf(
        Merchant(Kind.GARDEN, "Гюльнар", "Gulnar", "садовница", "the gardener",
            "Сады шепчут, что ты купец с добрыми руками. Поможешь старухе?", "The gardens whisper you're a merchant with kind hands. Will you help an old woman?",
            "Теперь мои сады – твои сады. Бери мёд дешевле, сынок.", "My gardens are your gardens now. Take the honey cheaper.",
            listOf(
                s("Кузнецы просили трав для ожогов. Отвези им два моих товара.", "The smiths asked for herbs for their burns. Take them two of my goods.", Goal.Deliver(Kind.GARDEN, Kind.FORGE, 2), 60, 15),
                s("Говорят, в городе есть кварталы, где я не бывала. Расскажи мне о двух новых.", "They say there are quarters I've never seen. Tell me of two new ones.", Goal.NewQuarters(2), 70, 20),
                s("Ночная лилия цветёт только в темноте. Проедь по городу ночью – посмотри, распустилась ли.", "The night lily blooms only in the dark. Ride through the city at night and see if it opened.", Goal.NightRide, 90, 25),
                s("Последнее: найди диковину садов. Хочу знать, что они прячут.", "The last: find a curio of the gardens. I want to know what they hide.", Goal.CurioOf("garden"), 150, 40),
            )),
        Merchant(Kind.WATER, "Ильяс", "Ilyas", "лодочник", "the boatman",
            "Эй, купец! Река приносит всё, но не всё уносит. Есть дело.", "Hey, merchant! The river brings everything but doesn't take it all away. I've a job.",
            "Ты свой на этой воде. Янтарь тебе – по-дружески.", "You belong on this water. Amber for you – as a friend.",
            listOf(
                s("В слободе ждут свежую рыбу к празднику. Довези два товара с реки.", "The homesteads want fresh fish for a feast. Bring two goods of the river.", Goal.Deliver(Kind.WATER, Kind.HOMES, 2), 60, 15),
                s("Хочу проверить, далеко ли ходят мои товары. Продай что-нибудь за 5 км от места покупки.", "I want to see how far my goods travel. Sell something 5 km from where it was bought.", Goal.Far(5), 80, 20),
                s("Под рекой, говорят, гномы роют ходы. Прокатись по ним.", "They say dwarves dig tunnels under the river. Ride through them.", Goal.Underground, 90, 25),
                s("Река вернула мне жемчуг. Продай три жемчужины – и мы квиты.", "The river gave me back pearls. Sell three of them and we're square.", Goal.Sell("pearl", 3), 160, 40),
            )),
        Merchant(Kind.OLD, "Мирза", "Mirza", "хранитель ключей", "the keeper of keys",
            "У каждой двери этого города свой ключ. У тебя, купец, ключа нет. Пока.", "Every door in this city has its key. You, merchant, have none. Yet.",
            "Держи ключ от моих лавок. Старина продаётся тебе дешевле.", "Here's the key to my stalls. Antiquities sell to you cheaper.",
            listOf(
                s("Учёным нужны мои монеты для летописи. Отвези им два товара старого города.", "The scholars need my coins for their chronicle. Take them two old-town goods.", Goal.Deliver(Kind.OLD, Kind.SCHOLAR, 2), 60, 15),
                s("Старый город любит терпеливых. Соверши пять поездок.", "The old town loves the patient. Make five rides.", Goal.Trips(5), 70, 20),
                s("Выручи 300 монет – покажи, что ты не простой разносчик.", "Take 300 coins – show you're no mere pedlar.", Goal.Profit(300), 100, 25),
                s("Найди диковину старого города. Ключ к ней – в дороге.", "Find a curio of the old town. Its key is on the road.", Goal.CurioOf("old"), 150, 40),
            )),
        Merchant(Kind.SCHOLAR, "Низами", "Nizami", "звездочёт", "the stargazer",
            "Звёзды сказали, что ты придёшь. Правда, они не сказали, когда.", "The stars said you'd come. They didn't say when, though.",
            "Звёзды благоволят тебе. Свитки и чернила – со скидкой.", "The stars favour you. Scrolls and ink at a discount.",
            listOf(
                s("Хранителю старины нужны свитки. Отвези два моих товара в старый город.", "The keeper of the old town needs scrolls. Take two of my goods there.", Goal.Deliver(Kind.SCHOLAR, Kind.OLD, 2), 60, 15),
                s("Мне нужны наблюдения ночного неба. Проедь по городу ночью.", "I need observations of the night sky. Ride through the city at night.", Goal.NightRide, 80, 20),
                s("Нанеси на мою карту три новых квартала.", "Put three new quarters on my map.", Goal.NewQuarters(3), 100, 25),
                s("Звёздные чернила кончаются. Продай три флакона там, где их ждут.", "The star ink is running out. Sell three bottles where they're wanted.", Goal.Sell("ink", 3), 160, 40),
            )),
        Merchant(Kind.FORGE, "Тимур", "Timur", "кузнец", "the blacksmith",
            "Без железа ни одна телега не поедет. Без купцов – ни одно железо.", "No cart rolls without iron. No iron moves without merchants.",
            "Ты крепкий, как мой лучший клинок. Железо – тебе первому и дешевле.", "You're as tough as my best blade. Iron goes to you first, and cheaper.",
            listOf(
                s("Садам нужны новые лопаты. Отвези туда два товара кузниц.", "The gardens need new spades. Take two forge goods there.", Goal.Deliver(Kind.FORGE, Kind.GARDEN, 2), 60, 15),
                s("Хорошая сталь закаляется в дороге. Продай товар за 6 км от места покупки.", "Good steel is tempered on the road. Sell a good 6 km from where you bought it.", Goal.Far(6), 80, 20),
                s("Гномы из подземных ходов – мои старые друзья. Навести их.", "The dwarves of the tunnels are old friends of mine. Visit them.", Goal.Underground, 90, 25),
                s("Продай три гномьи шестерни – пусть весь город заводится.", "Sell three dwarf cogs – let the whole city tick.", Goal.Sell("cogs", 3), 160, 40),
            )),
        Merchant(Kind.BAZAAR, "Зарина", "Zarina", "купчиха", "the merchant",
            "Хочешь торговать на моём базаре? Сначала покажи, чего стоишь.", "Want to trade at my bazaar? Show me what you're worth first.",
            "Отныне ты торгуешь как свой. Шёлк и пряности – по цене для своих.", "From now on you trade as one of us. Silk and spice at insiders' prices.",
            listOf(
                s("Воротам нужны пряности для караванов. Отвези им два товара базара.", "The gates need spice for the caravans. Take them two bazaar goods.", Goal.Deliver(Kind.BAZAAR, Kind.GATE, 2), 60, 15),
                s("Выручи 200 монет за день-другой.", "Take 200 coins in a day or two.", Goal.Profit(200), 80, 20),
                s("Хороший купец знает город. Побывай в двух новых кварталах.", "A good merchant knows the city. Visit two new quarters.", Goal.NewQuarters(2), 90, 25),
                s("Продай три отреза бирюзового шёлка – и я назову тебя компаньоном.", "Sell three lengths of turquoise silk and I'll call you a partner.", Goal.Sell("silk", 3), 160, 40),
            )),
        Merchant(Kind.GATE, "Азат", "Azat", "караванщик", "the caravan leader",
            "Я видел сто городов. Твой – сто первый. Покажешь мне его дороги?", "I've seen a hundred cities. Yours is the hundred and first. Will you show me its roads?",
            "Ты настоящий караванщик. Заморское – тебе дешевле.", "You're a true caravaneer. Faraway goods – cheaper for you.",
            listOf(
                s("Базару нужен заморский чай. Отвези туда два товара ворот.", "The bazaar needs faraway tea. Take two gate goods there.", Goal.Deliver(Kind.GATE, Kind.BAZAAR, 2), 60, 15),
                s("Дорога зовёт. Соверши пять поездок.", "The road calls. Make five rides.", Goal.Trips(5), 70, 20),
                s("Унеси товар подальше: продай за 8 км от места покупки.", "Carry a good far: sell it 8 km from where you bought it.", Goal.Far(8), 100, 25),
                s("Найди диковину дальней дороги – как настоящий караванщик.", "Find a curio of the long road – like a true caravaneer.", Goal.CurioOf("road"), 160, 40),
            )),
        Merchant(Kind.HOMES, "Фатима", "Fatima", "пекарь", "the baker",
            "Заходи, пока каравай горячий! А потом – поможешь мне?", "Come in while the loaf is warm! And then – will you help me?",
            "Ты нам как родной. Каравай и пряжа – по-соседски.", "You're family to us now. Bread and yarn at neighbours' prices.",
            listOf(
                s("На набережной давно не ели свежего хлеба. Отвези два моих товара.", "The riverside hasn't had fresh bread in ages. Take two of my goods there.", Goal.Deliver(Kind.HOMES, Kind.WATER, 2), 60, 15),
                s("Хлеб черствеет, а друзья ждут. Соверши три поездки.", "Bread goes stale and friends are waiting. Make three rides.", Goal.Trips(3), 60, 15),
                s("Продай три каравая, пока горячие.", "Sell three loaves while they're warm.", Goal.Sell("bread", 3), 90, 25),
                s("Говорят, клубок волшебной пряжи находят в слободах. Найди диковину слободы.", "They say a ball of magic yarn turns up in the homesteads. Find a homestead curio.", Goal.CurioOf("homes"), 150, 40),
            )),
    )
    val byId = all.associateBy { it.id }

    /** Met: a quarter of their kind has been visited. */
    fun met(s: State, m: Merchant) = s.quarters.values.any { it.known && it.kind == m.kind }
    fun step(s: State, m: Merchant) = s.mStep[m.id] ?: 0
    fun friend(s: State, m: Merchant) = step(s, m) >= m.steps.size
    fun friendOf(s: State, k: Kind?) = k != null && byId[k.id]?.let { friend(s, it) } == true

    /** Progress of the current request: (done so far, needed). */
    fun progress(s: State, m: Merchant): Pair<Int, Int> {
        val st = m.steps.getOrNull(step(s, m)) ?: return 1 to 1
        val since = s.mSince[m.id] ?: Long.MAX_VALUE
        val sales = s.deals.filter { !it.buy && it.at >= since }
        val trips = s.trips.filter { it.start >= since }
        return when (val g = st.goal) {
            is Goal.Deliver -> sales.count { Goods.byId[it.good]?.kind == g.what && s.quarters[it.quarter]?.kind == g.where } to g.n
            is Goal.Sell -> sales.count { it.good == g.good && s.quarters[it.quarter]?.kind != Goods[g.good].kind } to g.n
            is Goal.NewQuarters -> trips.sumOf { it.fresh.size } to g.n
            Goal.NightRide -> trips.count { it.night }.coerceAtMost(1) to 1
            Goal.Underground -> trips.count { it.underground > 0 || Ride.of(it.ride) == Ride.METRO }.coerceAtMost(1) to 1
            is Goal.Far -> sales.count { d ->
                val o = s.quarters[d.origin]; val q = s.quarters[d.quarter]
                o != null && q != null && Geo.distance(o.lat, o.lon, q.lat, q.lon) >= g.km * 1000
            }.coerceAtMost(1) to 1
            is Goal.Profit -> sales.sumOf { (it.price - it.paid).coerceAtLeast(0) } to g.n
            is Goal.CurioOf -> trips.count { t -> t.curio.isNotEmpty() && Curios.byId[t.curio]?.context?.id == g.context }.coerceAtMost(1) to 1
            is Goal.Trips -> trips.size to g.n
        }
    }

    /** The nearest known quarter of kind [k] from where the merchant is, as «Name (2,8 km)», or a hint to explore. */
    private fun nearest(s: State, k: Kind): String {
        val here = s.quarters[s.here]
        val q = s.quarters.values.filter { it.kind == k }.minByOrNull { here?.let { h -> Geo.distance(h.lat, h.lon, it.lat, it.lon) } ?: 0.0 }
            ?: return tr("такой квартал ещё не найден – проезжайте через бледные кварталы на карте", "none found yet – ride through the pale quarters on the map")
        if (q.id == s.here) return tr("${q.ru} – вы здесь", "${q.en} – you're here")
        val km = here?.let { Geo.distance(it.lat, it.lon, q.lat, q.lon) / 1000 } ?: 0.0
        return tr("ближайший – ${q.ru} (${fmt1(km)} км)", "nearest: ${q.en} (${fmt1(km)} km)")
    }

    private fun goods(k: Kind) = Goods.of(k).joinToString(", ") { it.name.lowercase() }

    /** Plain steps for the current request: what to buy, where, and where to take it. */
    fun how(s: State, st: Step): String = when (val g = st.goal) {
        is Goal.Deliver -> tr(
            "1) Купите ${g.n} ${plural(g.n.toLong(), "товар", "товара", "товаров")} квартала «${g.what.ru}» (${goods(g.what)}) – ${nearest(s, g.what)}.\n2) Продайте их на рынке квартала «${g.where.ru}» – ${nearest(s, g.where)}.",
            "1) Buy ${g.n} goods of a «${g.what.en}» quarter (${goods(g.what)}) – ${nearest(s, g.what)}.\n2) Sell them at the market of a «${g.where.en}» quarter – ${nearest(s, g.where)}.")
        is Goal.Sell -> Goods[g.good].let { gd -> tr(
            "1) Купите «${gd.ru}» в квартале «${gd.kind!!.ru}» – ${nearest(s, gd.kind)}.\n2) Продайте ${g.n} шт. в любом квартале другого вида – дороже всего там, где их ждут.",
            "1) Buy «${gd.en}» in a «${gd.kind!!.en}» quarter – ${nearest(s, gd.kind)}.\n2) Sell ${g.n} in any quarter of another kind – dearest where it's wanted.") }
        is Goal.NewQuarters -> tr("Нажмите «В путь» и проедьте через ${g.n} ${plural(g.n.toLong(), "квартал", "квартала", "кварталов")}, где ещё не были (на карте они бледные).", "Press «Set off» and ride through ${g.n} quarters you've never been to (pale on the map).")
        Goal.NightRide -> tr("Нажмите «В путь» и прокатитесь после 21:00 или до 5 утра.", "Press «Set off» and ride after 9 pm or before 5 am.")
        Goal.Underground -> tr("Нажмите «В путь», выберите «Гномьи ходы» и проедьте на метро.", "Press «Set off», pick «Dwarf tunnels» and ride the underground.")
        is Goal.Far -> tr("Купите товар в одном квартале и продайте в квартале не ближе ${g.km} км от него.", "Buy a good in one quarter and sell it in a quarter at least ${g.km} km away.")
        is Goal.Profit -> tr("Наторгуйте ${g.n} монет прибыли: покупайте дёшево, продавайте там, где дороже.", "Make ${g.n} coins of profit: buy cheap, sell where it's dearer.")
        is Goal.CurioOf -> Curios.CONTEXTS.first { it.id == g.context }.let { c -> tr("Ездите – диковины находятся сами. Нужна диковина вида «${c.ru}»: ${c.hintRu}.", "Ride – curios turn up by themselves. You need one of «${c.en}»: ${c.hintEn}.") }
        is Goal.Trips -> tr("Совершите ${g.n} ${plural(g.n.toLong(), "поездку", "поездки", "поездок")} с кнопкой «В путь».", "Make ${g.n} rides with «Set off».")
    }

    fun ready(s: State, m: Merchant): Boolean {
        if (!met(s, m) || friend(s, m) || s.mSince[m.id] == null) return false
        val (a, b) = progress(s, m); return a >= b
    }
}
