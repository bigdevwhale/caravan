package app.caravan.domain

import app.caravan.data.Lot
import app.caravan.data.Quarter
import app.caravan.data.State
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt

/** One line of a quarter's market: what it sells (with stock) or what it pays for a good. */
class Offer(val good: Good, val price: Int, val stock: Int = 0, val note: String = "")

/** A rumour of the day: [quarter] pays double for [good] today. */
class Rumour(val quarter: Quarter, val good: Good) {
    val text: String get() {
        val r = Rand(hash(quarter.id + good.id))
        val ru = listOf("в ${quarter.ru} свадьба – ищут", "в ${quarter.ru} праздник – нужно", "на ${quarter.ru} ждут гостей – скупают", "в ${quarter.ru} кончилось")
        val en = listOf("a wedding in ${quarter.en} – they want", "a feast in ${quarter.en} – they need", "${quarter.en} expects guests – buying up", "${quarter.en} ran out of")
        val i = r.int(ru.size)
        return tr("${ru[i].cap()}: ${good.ru.lowercase()}", "${en[i].cap()}: ${good.en.lowercase()}")
    }
}

/**
 * Prices. A quarter sells the three goods of its kind cheap (and one of its second kind), a few a day, and
 * each quarter has its own face: one good it is famous for (cheaper), one good it craves (paid
 * extra) and a daily import from a third kind of quarter. It
 * pays for the rest: more for the kinds it wants, for goods carried from far across the city, in quarters
 * the merchant rarely visits and for the day's rumours. Each sale of the same good in a quarter the same day
 * lowers its price, so a loop between two stalls soon stops paying.
 */
object Market {
    const val STOCK = 3
    val packSizes = listOf(6, 9, 12, 16, 20, 25)
    val packPrices = listOf(0, 250, 700, 1600, 3000, 5000)

    fun capacity(s: State) = packSizes[s.packLevel.coerceIn(0, packSizes.lastIndex)] + Guild.extraSlots(s) + House.slots(s)

    private fun noise(day: Long, q: String, g: String) = 0.9 + Rand(hash("$day:$q:$g")).next() * 0.2

    /** The good of its own kind this quarter is famous for: it sells it cheaper. */
    fun famous(q: Quarter): Good? = q.kind?.let { Goods.of(it)[Rand(hash("famous:" + q.id)).int(3)] }

    /** The one good this quarter craves above all, from the kinds it wants: it pays extra for it. */
    fun craves(q: Quarter): Good? = q.kind?.let { k -> k.wants.flatMap { Goods.of(it) }.let { it[Rand(hash("craves:" + q.id)).int(it.size)] } }

    /** Today's import: a good of a kind this quarter neither makes nor wants, brought in for the day. */
    fun imported(q: Quarter, day: Long = today()): Good? {
        val k = q.kind ?: return null
        val pool = Goods.all.filter { !it.rare && it.kind != k && it.kind != q.second && it.kind !in k.wants }
        return pool.getOrNull(Rand(hash("import:$day:${q.id}")).int(pool.size))
    }

    fun sells(q: Quarter, s: State, day: Long = today()): List<Offer> {
        val k = q.kind ?: return emptyList()
        val mine = Goods.of(k) + listOfNotNull(q.second?.takeIf { it != k }?.let { Goods.of(it)[Rand(hash(q.id)).int(3)] }, imported(q, day))
        val star = famous(q)
        val brought = imported(q, day)
        val extra = (if (Guild.stallAt(s, q.id)) 2 else 0) + (if (Merchants.friendOf(s, k)) 1 else 0) - (if (Events.of(day).id == "drought" && k == Kind.GARDEN) 2 else 0)
        return mine.distinctBy { it.id }.map { g ->
            val import = g == brought
            val stock = when { import -> 1; g == star -> STOCK + extra; else -> STOCK + extra }
            val left = stock - (s.traded["b:${q.id}:${g.id}"] ?: 0)
            val friend = if (Merchants.friendOf(s, k) && !import) 0.9 else 1.0
            val f = when { import -> 1.15; g == star -> 0.6; else -> 0.7 }
            val p = (g.base * f * noise(day, q.id, g.id) * (if (import) 1.0 else Events.of(day).buy(k)) * friend).roundToInt().coerceAtLeast(3)
            Offer(g, p, left.coerceAtLeast(0), when { import -> tr("привоз дня", "today's import"); g == star -> tr("фирменный", "speciality"); else -> "" })
        }
    }

    /** What [q] pays for [lot] now. */
    fun price(lot: Lot, q: Quarter, s: State, day: Long = today()): Int {
        val g = Goods[lot.good]
        val k = q.kind ?: return (g.base * 0.6).roundToInt()
        var f = 1.0
        if (g.rare) {
            if (k in Goods.rareWanted(g)) f += 0.8
        } else when {
            g.kind == k -> f = 0.55
            g.kind in k.wants -> f += 0.6
            g.kind == q.second -> f -= 0.15
            else -> f += 0.15
        }
        if (g == craves(q)) f += 0.2
        // bought here and sold back here: the stall takes it back at a loss
        if (lot.origin == q.id && lot.paid > 0) f = min(f, 0.6)
        // carried across the city
        val origin = s.quarters[lot.origin]
        if (origin != null && origin.id != q.id) f += min(0.6, app.caravan.trip.Geo.distance(origin.lat, origin.lon, q.lat, q.lon) / 1000 * 0.06)
        // rarely visited quarters pay for novelty
        f += when { q.visits <= 1 -> 0.3; q.visits <= 4 -> 0.15; else -> 0.0 }
        if (rumours(s, day).any { it.quarter.id == q.id && it.good.id == g.id }) f += 1.0
        if (fairAt(s, day)?.id == q.id) f += 0.3
        f *= Guild.priceBonus(s, q.id) * House.sale(s) * Curios.saleBonus(s, g) * Events.of(day).sell(g, q) * Risk.freshness(lot)
        val sold = s.traded["s:${q.id}:${g.id}"] ?: 0
        f *= Math.pow(0.88, sold.toDouble())
        return (g.base * f * noise(day, q.id, g.id)).roundToInt().coerceAtLeast(1)
    }

    /** A quarter left out of the rumours (#12). */
    fun hidden(s: State, id: String) = "hide:$id" in s.told

    /** Two rumours a day among the quarters already known (one more for each inn). */
    fun rumours(s: State, day: Long = today()): List<Rumour> {
        // quarters one never goes back to can be left out (#12)
        val known = s.quarters.values.filter { it.known && !hidden(s, it.id) }.sortedBy { it.id }
        if (known.size < 2) return emptyList()
        val r = Rand(hash("rumour:$day"))
        val n = 2 + Guild.inns(s) + House.rumours(s)
        val goods = Goods.all.filter { !it.rare }
        return (0 until n).map { known[r.int(known.size)] to goods[r.int(goods.size)] }
            .filter { (q, g) -> g.kind != q.kind }.distinctBy { it.first.id }.map { Rumour(it.first, it.second) }
    }

    /** Saturdays and Sundays a fair comes to one known garden quarter (or any known quarter). */
    fun fairAt(s: State, day: Long = today()): Quarter? {
        val d = LocalDate.ofEpochDay(day).dayOfWeek
        if (d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY) return null
        val known = s.quarters.values.filter { it.known }.sortedBy { it.id }
        val pool = known.filter { it.kind == Kind.GARDEN }.ifEmpty { known }
        if (pool.isEmpty()) return null
        return pool[Rand(hash("fair:${weekStart(day)}")).int(pool.size)]
    }

    fun nightOpen(s: State, now: Long = System.currentTimeMillis()) = now < s.nightUntil

    /** The night market: three rare goods, dear, one of each. */
    fun night(s: State, day: Long = today()): List<Offer> {
        val r = Rand(hash("night:$day"))
        return Goods.rare.shuffled(kotlin.random.Random(r.int(1 shl 20))).take((if (Events.of(day).id == "caravan") 4 else 3) + (if (Curios.complete(s, "night")) 1 else 0)).map { g ->
            Offer(g, (g.base * (0.8 + r.next() * 0.2)).roundToInt(), 1 - (s.traded["b:night:${g.id}"] ?: 0).coerceAtMost(1))
        }
    }
}
