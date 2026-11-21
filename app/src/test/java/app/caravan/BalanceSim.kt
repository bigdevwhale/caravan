package app.caravan

import app.caravan.data.Lot
import app.caravan.data.Quarter
import app.caravan.data.State
import app.caravan.domain.House
import app.caravan.domain.Kind
import app.caravan.domain.Market
import org.junit.Test

/**
 * Not a check but a measuring stick: how fast gold comes to a plain commuter (home ↔ work, weekdays only)
 * and to a rover who also crosses the city at weekends, and on which day each purchase becomes affordable.
 * Run with `./gradlew testDebugUnitTest --tests "*BalanceSim*" -i` and read the printed table.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class BalanceSim {
    private fun q(id: String, k: Kind, k2: Kind, lat: Double) = Quarter(id, id, id, lat, 30.0, k, k2, visits = 9, firstDay = 1)

    /** Buys the best bales for [to] at [from], returns the state after buying. */
    private fun buy(s0: State, from: Quarter, to: Quarter, day: Long): State {
        var s = s0
        while (s.pack.size < Market.capacity(s)) {
            val best = Market.sells(from, s, day).filter { it.stock > 0 && it.price <= s.gold }
                .maxByOrNull { Market.price(Lot(0, it.good.id, from.id, it.price, System.currentTimeMillis()), to, s, day) - it.price } ?: break
            val gain = Market.price(Lot(0, best.good.id, from.id, best.price, System.currentTimeMillis()), to, s, day) - best.price
            if (gain <= 0) break
            val k = "b:${from.id}:${best.good.id}"
            s = s.copy(gold = s.gold - best.price, pack = s.pack + Lot(s.nextId, best.good.id, from.id, best.price, System.currentTimeMillis()), nextId = s.nextId + 1,
                traded = s.traded + (k to (s.traded[k] ?: 0) + 1))
        }
        return s
    }

    private fun sell(s0: State, at: Quarter, day: Long): State {
        var s = s0
        for (lot in s0.pack) {
            val p = Market.price(lot, at, s, day)
            if (p <= lot.paid) continue
            val k = "s:${at.id}:${lot.good}"
            s = s.copy(gold = s.gold + p, pack = s.pack - lot, traded = s.traded + (k to (s.traded[k] ?: 0) + 1))
        }
        return s
    }

    private fun run(name: String, rover: Boolean) {
        val home = q("home", Kind.WATER, Kind.HOMES, 60.00)
        val work = q("work", Kind.FORGE, Kind.GATE, 60.045)
        val far = listOf(q("far1", Kind.GARDEN, Kind.OLD, 60.09), q("far2", Kind.BAZAAR, Kind.SCHOLAR, 59.93))
        var s = State(quarters = (listOf(home, work) + far).associateBy { it.id })
        val log = ArrayList<String>()
        var income = 0
        for (d in 0 until 240) {
            val day = 20800L + d
            s = s.copy(day = day, traded = emptyMap())
            val before = s.gold + s.pack.sumOf { it.paid }
            val weekend = java.time.LocalDate.ofEpochDay(day).dayOfWeek.value >= 6
            if (!weekend) {
                s = buy(s, home, work, day); s = sell(s, work, day)
                s = buy(s, work, home, day); s = sell(s, home, day)
                s = s.copy(gold = s.gold + 25) // a guild order or two
            } else if (rover) {
                val f = far[d % 2]
                s = buy(s, home, f, day); s = sell(s, f, day)
                s = buy(s, f, home, day); s = sell(s, home, day)
            }
            income += s.gold + s.pack.sumOf { it.paid } - before
            // spend: the cheapest thing that fits, pack first
            while (true) {
                val n = s.packLevel + 1
                val pack = Market.packPrices.getOrNull(n)
                val part = House.parts.filter { House.canBuild(s.copy(gold = Int.MAX_VALUE), it) }.minByOrNull { it.price }
                when {
                    pack != null && (part == null || pack <= part.price) && s.gold >= pack + 60 -> { s = s.copy(gold = s.gold - pack, packLevel = n); log += "day ${d + 1}: pack ${Market.packSizes[n]}" }
                    part != null && s.gold >= part.price + 60 -> { s = s.copy(gold = s.gold - part.price, house = s.house + part.id); log += "day ${d + 1}: ${part.id} (${part.price})" }
                    else -> break
                }
            }
            if (d == 6 || d == 29 || d == 89) log += "   -- after ${d + 1} days: ${income / (d + 1)} a day on average"
            if (House.done(s)) { log += "HOUSE DONE on day ${d + 1}"; break }
        }
        println("=== $name ===\n" + log.joinToString("\n"))
    }

    @Test fun commuter() = run("commuter, weekdays only", false)
    @Test fun rover() = run("rover, weekends across the city", true)
}
