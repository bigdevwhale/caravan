package app.caravan.domain

import app.caravan.data.Lot
import app.caravan.data.Quarter
import app.caravan.data.State

/** The week's event in the city: it changes what pays, so weekdays don't all look alike. */
class CityEvent(val id: String, val ru: String, val en: String, val textRu: String, val textEn: String, val pic: String) {
    val title get() = tr(ru, en)
    val text get() = tr(textRu, textEn)

    /** Multiplier on what [q] pays for good [g]. */
    fun sell(g: Good, q: Quarter): Double = when (id) {
        "drought" -> if (g.kind == Kind.GARDEN) 1.6 else 1.0
        "fire" -> if (q.kind == Kind.FORGE) 1.5 else 1.0
        "scholars" -> if (g.kind == Kind.SCHOLAR) 1.5 else 1.0
        "weddings" -> if (q.kind == Kind.HOMES) 1.4 else 1.0
        "flood" -> if (g.kind == Kind.WATER && q.kind in listOf(Kind.OLD, Kind.HOMES)) 1.4 else 1.0
        "silk" -> if (g.kind == Kind.BAZAAR && q.kind != Kind.BAZAAR) 1.35 else 1.0
        else -> 1.0
    }

    /** Multiplier on what quarters of kind [k] ask for their own goods. */
    fun buy(k: Kind?): Double = when (id) {
        "flood" -> if (k == Kind.WATER) 0.7 else 1.0
        "caravan" -> if (k == Kind.GATE) 0.75 else 1.0
        "drought" -> if (k == Kind.GARDEN) 1.3 else 1.0
        else -> 1.0
    }
}

object Events {
    val all = listOf(
        CityEvent("drought", "Засуха в садах", "Drought in the gardens", "Садовые товары везде на 60% дороже, а сами сады продают их втридорога.", "Garden goods fetch 60% more everywhere, and the gardens themselves ask a lot.", "crest_garden"),
        CityEvent("flood", "Половодье", "The river is high", "Товары набережной дёшевы у воды, а старый город и слободы платят за них на 40% больше.", "River goods are cheap by the water, and the old town and homesteads pay 40% more for them.", "crest_water"),
        CityEvent("fire", "Праздник огня", "Festival of fire", "Кузни гуляют и платят на 50% больше за любой товар.", "The forges celebrate and pay 50% more for anything.", "crest_forge"),
        CityEvent("scholars", "Съезд учёных", "Scholars' congress", "Свитки, чернила и перья везде на 50% дороже.", "Scrolls, ink and quills fetch 50% more everywhere.", "crest_scholar"),
        CityEvent("caravan", "Караван с юга", "A caravan from the south", "Ворота продают свои товары на четверть дешевле, а на ночном рынке – четыре редкости.", "The gates sell their goods a quarter cheaper, and the night market has four rarities.", "crest_gate"),
        CityEvent("weddings", "Свадебная неделя", "Wedding week", "Слободы платят на 40% больше за всё.", "The homesteads pay 40% more for everything.", "crest_homes"),
        CityEvent("silk", "Мода на шёлк", "Silk is in fashion", "Товары базара за его пределами на 35% дороже.", "Bazaar goods fetch 35% more outside the bazaar.", "crest_bazaar"),
        CityEvent("stars", "Звездопад", "Starfall week", "Ночной рынок открывается после любой поездки с 19:00.", "The night market opens after any ride from 7 pm.", "crest_night"),
    )

    /** One event per week: every eight weeks each event comes once, in a shuffled order, never twice running. */
    fun of(day: Long = today()): CityEvent {
        val week = Math.floorDiv(weekStart(day), 7L)
        return all[cycle(Math.floorDiv(week, 8L))[Math.floorMod(week, 8L).toInt()]]
    }

    private fun order(c: Long) = all.indices.shuffled(kotlin.random.Random(hash("events:$c"))).toMutableList()

    private fun cycle(c: Long): List<Int> {
        val o = order(c)
        if (o.first() == order(c - 1).last()) { val t = o[0]; o[0] = o[1]; o[1] = t }
        return o
    }
}

/** Goods that don't keep, and goods that break. */
object Risk {
    val perishable = setOf("fish", "bread", "herbs")
    val fragile = setOf("glass", "jug", "pearl")

    /** Price share left after [days] in the pack: perishables lose a fifth a day after the first, down to 40%. */
    fun freshness(lot: Lot, now: Long = System.currentTimeMillis()): Double {
        if (lot.good !in perishable) return 1.0
        val days = ((now - lot.at) / 86_400_000L).toInt()
        return (1.0 - 0.2 * (days - 0).coerceAtLeast(0)).coerceAtLeast(0.4).let { if (days < 1) 1.0 else it }
    }

    /** The chance one fragile bale breaks on this ride: jams and long rides shake the cart. */
    fun breakChance(t: app.caravan.data.Trip, s: State): Double {
        var p = when { t.jamSec > 300 -> 0.15; t.km > 5 -> 0.07; else -> 0.0 }
        if (House.has(s, "shed")) p /= 2
        if (Curios.complete(s, "weather")) p /= 2
        return p
    }
}

/** A guild contract: deliver [n] of [good] to a quarter of kind [where] before [until]; [deposit] is lost if late. */
data class Contract(val id: Long, val good: String, val n: Int, val where: Kind, val until: Long, val deposit: Int, val reward: Int, val since: Long = 0, val done: Int = 0) {
    val taken get() = since > 0
}

object Contracts {
    /** Two offers a day, each taking two days. */
    fun offers(s: State, day: Long = today()): List<Contract> {
        val r = Rand(hash("contract:$day"))
        val goods = Goods.all.filter { !it.rare }
        return (0 until 2).map { i ->
            val g = goods[r.int(goods.size)]
            val where = (Kind.entries - g.kind!!).let { it[r.int(it.size)] }
            val n = 2 + r.int(3)
            val dep = (g.base * n * 0.6).toInt()
            val until = java.time.LocalDate.ofEpochDay(day + 1).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
            Contract(day * 10 + i, g.id, n, where, until, dep, (g.base * n * 1.4).toInt() + 40)
        }.filter { o -> s.contracts.none { it.id == o.id } }
    }

    fun progress(s: State, c: Contract) = s.deals.count { !it.buy && it.at >= c.since && it.good == c.good && s.quarters[it.quarter]?.kind == c.where }
}

/** A short meeting on the road after a ride, with a choice. */
class Encounter(val id: String, val ru: String, val en: String, val textRu: String, val textEn: String, val pic: String, val options: List<Pair<String, String>>) {
    val title get() = tr(ru, en)
    val text get() = tr(textRu, textEn)
    fun option(i: Int) = options[i].let { tr(it.first, it.second) }
}

object Encounters {
    val all = listOf(
        Encounter("pedlar", "Бродячий торговец", "A wandering pedlar", "У моста торговец предлагает обмен: любой ваш тюк на его загадочный свёрток.", "By the bridge a pedlar offers a swap: any of your bales for his mysterious bundle.", "ic_bundle",
            listOf("Меняюсь" to "Swap", "Проехать мимо" to "Ride on")),
        Encounter("traveller", "Усталый путник", "A tired traveller", "Путник просит подвезти его до ближайшего квартала и угостить хлебом.", "A traveller asks for a ride to the next quarter and a bite of bread.", "ic_sign",
            listOf("Подвезти (10 монет на угощение)" to "Give a ride (10 coins for food)", "Нет места" to "No room")),
        Encounter("dice", "Игрок в кости", "A dice player", "У трактира зазывают сыграть: ставка 30 монет, выигрыш – вдвое.", "Outside an inn they call you to play: a 30-coin stake, double if you win.", "ic_purse",
            listOf("Сыграть" to "Play", "Не сегодня" to "Not today")),
        Encounter("cartographer", "Старый картограф", "An old cartographer", "За 40 монет картограф расскажет, чем торгуют два неизведанных квартала.", "For 40 coins the cartographer will tell what two unexplored quarters trade.", "ic_map",
            listOf("Заплатить 40" to "Pay 40", "Сам узнаю" to "I'll find out myself")),
        Encounter("lostbale", "Потерянный тюк", "A lost bale", "На обочине лежит чей-то тюк. На нём бирка с адресом.", "A bale lies by the roadside, with an address tag on it.", "ic_bundle",
            listOf("Вернуть хозяину" to "Return it to the owner", "Забрать себе" to "Keep it")),
        Encounter("musician", "Уличный музыкант", "A street musician", "Музыкант поёт про то, где сегодня дорого платят. Бросить монетку?", "A musician sings of where they pay well today. Toss a coin?", "ic_lantern",
            listOf("Бросить 5 монет" to "Toss 5 coins", "Послушать и уйти" to "Listen and go")),
        Encounter("fortune", "Гадалка", "A fortune-teller", "Гадалка предлагает за 20 монет погадать на удачу в торговле.", "A fortune-teller offers to read your trading luck for 20 coins.", "ic_seal",
            listOf("Погадать" to "Have it read", "Не верю в гадания" to "I don't believe in it")),
        Encounter("toll", "Мостовой сбор", "A bridge toll", "Стражник у моста просит пошлину – 15 монет. Можно поехать в объезд, но там пыльно.", "A bridge guard asks for a 15-coin toll. You could take the detour, but it's dusty.", "ic_key",
            listOf("Заплатить" to "Pay", "В объезд" to "Detour")),
    )
    val byId = all.associateBy { it.id }

    /** After a ride: two meetings a day at most, about two rides in five. */
    fun roll(s: State, start: Long, day: Long = today()): String? {
        val count = if (s.encDay == day) s.encCount else 0
        if (count >= 2) return null
        val r = Rand(hash("enc:$start"))
        if (r.next() > 0.4) return null
        return all[r.int(all.size)].id
    }
}

/** Your stall in another quarter: it sells what you leave there, one bale every four hours, for 90% of the local price. */
data class Shop(val quarter: String, val stock: List<Lot> = emptyList(), val till: Int = 0, val tick: Long = 0)

object Shops {
    const val EVERY = 4 * 3600_000L
    const val SLOTS = 5
    val prices = listOf(600, 1500, 3200)

    fun price(s: State) = prices.getOrNull(s.shops.size)

    /** Sells from every stall what the hours since the last look allow. */
    fun tick(s: State, now: Long = System.currentTimeMillis()): State {
        if (s.shops.none { it.stock.isNotEmpty() }) return s
        val shops = s.shops.map { sh ->
            if (sh.stock.isEmpty()) return@map sh.copy(tick = now)
            val q = s.quarters[sh.quarter] ?: return@map sh
            var n = ((now - sh.tick) / EVERY).toInt().coerceAtMost(sh.stock.size)
            var stock = sh.stock; var till = sh.till
            while (n-- > 0) {
                val lot = stock.maxBy { Market.price(it, q, s) }
                till += (Market.price(lot, q, s) * 0.9).toInt()
                stock = stock - lot
            }
            sh.copy(stock = stock, till = till, tick = if (stock.isEmpty()) now else sh.tick + ((now - sh.tick) / EVERY) * EVERY)
        }
        return s.copy(shops = shops)
    }
}
