package app.caravan.data

import android.content.Context
import app.caravan.trip.Fix
import org.json.JSONObject

/**
 * Screenshot builds (-Pdemo) start from a ready game in central Moscow (assets/demo.json, made by tools/demo.py).
 * Its times are written around a fixed «base» moment and moved to now here, so the demo never looks stale.
 */
object Demo {
    /** What the demo opens on: a tab and a sheet, from the capture script's intent. */
    @Volatile var tab = -1
    @Volatile var open: String? = null

    suspend fun seedIfEmpty(c: Context, g: Game) {
        if (g.now.onboarded) return
        val text = runCatching { c.assets.open("demo.json").bufferedReader().use { it.readText() } }.getOrNull() ?: return
        val o = JSONObject(text)
        val d = System.currentTimeMillis() - o.optLong("base", System.currentTimeMillis())
        val days = Math.floorDiv(d, 86_400_000L)
        val s = stateOf(o)
        fun t(x: Long) = if (x > 0) x + d else x
        val moved = s.copy(
            trips = s.trips.map { it.copy(start = t(it.start), end = t(it.end), lastAt = t(it.lastAt)) },
            deals = s.deals.map { it.copy(at = t(it.at)) },
            pack = s.pack.map { it.copy(at = t(it.at)) },
            mSince = s.mSince.mapValues { t(it.value) },
            nightUntil = t(s.nightUntil),
            contracts = s.contracts.map { it.copy(until = t(it.until), since = t(it.since), id = it.id + days * 10) },
            shops = s.shops.map { sh -> sh.copy(tick = t(sh.tick), stock = sh.stock.map { it.copy(at = t(it.at)) }) },
            quarters = s.quarters.mapValues { (_, q) -> if (q.firstDay > 0) q.copy(firstDay = q.firstDay + days) else q },
            orders = s.orders.map { k -> (k.substringBefore(':').toLong() + days).toString() + ":" + k.substringAfter(':') }.toSet(),
        )
        g.edit { moved.copy(day = it.day) }
        // the cart stands at the gate of the quarter the demo is in
        moved.quarters[moved.here]?.let { q -> g.live.value = g.live.value.copy(fix = Fix(q.lat - 0.002, q.lon + 0.003, 8f, System.currentTimeMillis())) }
    }
}
