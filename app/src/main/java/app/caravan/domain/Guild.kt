package app.caravan.domain

import app.caravan.data.Route
import app.caravan.data.State
import app.caravan.data.Trip

/** The merchants' guild: ranks for renown, routes that grow with use, buildings bought on them. */
object Guild {
    class Rank(val at: Int, val ru: String, val en: String) { val title get() = tr(ru, en) }

    val ranks = listOf(
        Rank(0, "Разносчик", "Pedlar"),
        Rank(60, "Лоточник", "Stallkeeper"),
        Rank(180, "Купец третьей гильдии", "Merchant of the third guild"),
        Rank(450, "Купец второй гильдии", "Merchant of the second guild"),
        Rank(1000, "Купец первой гильдии", "Merchant of the first guild"),
        Rank(2000, "Именитый купец", "Eminent merchant"),
        Rank(4000, "Торговый князь", "Merchant prince"),
    )

    fun rank(renown: Int) = ranks.last { renown >= it.at }
    fun next(renown: Int) = ranks.firstOrNull { it.at > renown }

    class Level(val at: Int, val ru: String, val en: String) { val title get() = tr(ru, en) }

    val levels = listOf(
        Level(1, "Тропа", "Track"), Level(3, "Дорога", "Road"), Level(7, "Торговый путь", "Trade route"),
        Level(15, "Большой тракт", "High road"), Level(30, "Шёлковый путь", "Silk road"),
    )

    fun level(r: Route) = levels.indexOfLast { r.trips >= it.at }.coerceAtLeast(0)

    class Building(val id: String, val needs: Int, val price: Int, val ru: String, val en: String, val effRu: String, val effEn: String) {
        val title get() = tr(ru, en)
        val effect get() = tr(effRu, effEn)
        val pic get() = "bld_$id"
    }

    val buildings = listOf(
        Building("stall", 1, 300, "Лавка", "Stall", "по две лишних штуки товара на обоих концах пути", "two more of each good at both ends"),
        Building("well", 1, 450, "Колодец", "Well", "+8% к ценам продажи на обоих концах", "+8% to sale prices at both ends"),
        Building("inn", 2, 900, "Постоялый двор", "Inn", "ещё один слух каждый день", "one more rumour every day"),
        Building("tower", 2, 1200, "Сторожевая башня", "Watchtower", "поездка по пути открывает ночной рынок", "a trip on it opens the night market"),
        Building("warehouse", 3, 1800, "Склад", "Warehouse", "+3 места во вьюке", "+3 places in the pack"),
        Building("fountain", 3, 2600, "Фонтан", "Fountain", "+10 славы за каждую поездку по пути", "+10 renown for every trip on it"),
    )
    val byId = buildings.associateBy { it.id }

    private fun built(s: State, id: String) = s.routes.values.filter { id in it.built }

    fun extraSlots(s: State) = built(s, "warehouse").size * 3
    fun inns(s: State) = built(s, "inn").size
    fun stallAt(s: State, q: String) = built(s, "stall").any { it.a == q || it.b == q }
    fun priceBonus(s: State, q: String) = if (built(s, "well").any { it.a == q || it.b == q }) 1.08 else 1.0

    fun routeOf(s: State, t: Trip): Route? {
        if (t.from.isEmpty() || t.to.isEmpty() || t.from == t.to) return null
        return s.routes[Route.key(t.from, t.to)]
    }
}

/** The day's three small orders; they never burn with a penalty, they're just new tomorrow. */
class Order(val index: Int, val ru: String, val en: String, val renown: Int, val gold: Int, val done: (State, Long) -> Boolean) {
    val text get() = tr(ru, en)
}

object Orders {
    private fun tripsOn(s: State, day: Long) = s.trips.filter { dayOf(it.start) == day }
    private fun dealsOn(s: State, day: Long) = s.deals.filter { dayOf(it.at) == day }

    fun of(s: State, day: Long = today()): List<Order> {
        val r = Rand(hash("orders:$day"))
        val kinds = Kind.entries
        val a = kinds[r.int(kinds.size)]
        var b = a.wants[r.int(2)]
        if (b == a) b = Kind.BAZAAR
        val all = listOf(
            Order(0, "Продать товар из квартала «${a.ru}» в квартале «${b.ru}»",
                "Sell a good from ${a.en} in ${b.en}", 15, 20) { st, d ->
                dealsOn(st, d).any { !it.buy && Goods.byId[it.good]?.kind == a && st.quarters[it.quarter]?.kind == b }
            },
            Order(1, "Побывать в квартале, где ещё не были", "Visit a quarter you've never been to", 20, 15) { st, d ->
                (tripsOn(st, d) + listOfNotNull(st.trip)).any { it.fresh.isNotEmpty() }
            },
            Order(2, "Проехать через три квартала за одну поездку", "Pass through three quarters in one trip", 12, 15) { st, d ->
                (tripsOn(st, d) + listOfNotNull(st.trip)).any { it.quarters.distinct().size >= 3 }
            },
            Order(3, "Выручить 100 монет за день", "Take 100 coins in a day", 12, 0) { st, d ->
                dealsOn(st, d).filter { !it.buy }.sumOf { it.price } >= 100
            },
            Order(4, "Продать товар, увезённый за 4 км от места покупки", "Sell a good carried 4 km from where it was bought", 15, 20) { st, d ->
                dealsOn(st, d).any { dl ->
                    if (dl.buy) return@any false
                    val o = st.quarters[dl.origin]; val q = st.quarters[dl.quarter]
                    o != null && q != null && app.caravan.trip.Geo.distance(o.lat, o.lon, q.lat, q.lon) >= 4000
                }
            },
            Order(5, "Успеть к слуху: продать то, чего ждут", "Catch a rumour: sell what they're waiting for", 20, 25) { st, d ->
                val rs = Market.rumours(st, d)
                dealsOn(st, d).any { dl -> !dl.buy && rs.any { it.quarter.id == dl.quarter && it.good.id == dl.good } }
            },
            Order(6, "Купить по одному товару в двух разных кварталах", "Buy a good in two different quarters", 10, 10) { st, d ->
                dealsOn(st, d).filter { it.buy && it.quarter != "night" }.map { it.quarter }.distinct().size >= 2
            },
        )
        return all.shuffled(kotlin.random.Random(r.int(1 shl 20))).take(3)
    }

    fun key(day: Long, o: Order) = "$day:${o.index}"
}
