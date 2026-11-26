package app.caravan.data

import android.content.Context
import app.caravan.domain.Goods
import app.caravan.domain.Guild
import app.caravan.domain.House
import app.caravan.domain.Contracts
import app.caravan.domain.Contract
import app.caravan.domain.Shop
import app.caravan.domain.Curios
import app.caravan.domain.Folk
import app.caravan.domain.Encounters
import app.caravan.domain.Events
import app.caravan.domain.Merchants
import app.caravan.domain.Risk
import app.caravan.domain.Shops
import app.caravan.domain.Tutorial
import app.caravan.domain.Weather
import app.caravan.domain.Kind
import app.caravan.domain.Market
import app.caravan.domain.Metro
import app.caravan.domain.Station
import app.caravan.domain.Orders
import app.caravan.domain.Quarters
import app.caravan.domain.Rand
import app.caravan.domain.Ride
import app.caravan.domain.dayOf
import app.caravan.domain.hash
import app.caravan.domain.hourOf
import app.caravan.domain.today
import app.caravan.domain.tr
import app.caravan.trip.Fix
import app.caravan.trip.Geo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** What the ride is doing right now; never saved. */
data class Live(
    val fix: Fix? = null,
    /** m/s, smoothed. */
    val speed: Double = 0.0,
    val movingSince: Long = 0,
    val stillSince: Long = 0,
    val loading: Boolean = false,
    /** When the phone last gave any position at all, however rough (the clock's time). */
    val heardAt: Long = 0,
    /** When a rough position (too coarse to count metres) last showed the ride going on. */
    val stirAt: Long = 0,
) {
    /** The driver's screen rests while the cart moves faster than a walk. */
    val moving get() = speed > 3.5
}

/** Something to say: a toast in the app, a line of the voice. */
class News(val text: String, val speak: Boolean = false)

/**
 * The whole game: one state, saved as JSON after every change, and the rules that change it. Writes go
 * through one lock, so the GPS, the market and the trip's end never step on each other.
 */
class Game(private val app: Context, val scope: CoroutineScope) {
    private val file = File(app.filesDir, "caravan.json")
    private val lock = Mutex()
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    val live = MutableStateFlow(Live())
    val news = MutableSharedFlow<News>(extraBufferCapacity = 16)
    /** The finished trip whose ledger page should open, once. */
    val finished = MutableStateFlow<Trip?>(null)
    private var lastFetch = 0L
    private val classifying = HashSet<String>()
    /** When each quarter's kind was last asked, so a stand-in is re-asked every three hours at most. */
    private val tried = HashMap<String, Long>()

    val now: State get() = _state.value

    fun load() {
        var s = runCatching { stateOf(JSONObject(file.readText())) }.getOrNull() ?: State()
        // 1.0.1: the map server's counts were read wrong, so every kind was a stand-in; ask all of them again
        if ("fix.kinds" !in s.told) s = s.copy(quarters = s.quarters.mapValues { (_, q) -> if (q.kind != null) q.copy(guessed = true) else q }, told = s.told + "fix.kinds")
        _state.value = s
    }

    private fun save(s: State) {
        val text = s.json().toString()
        scope.launch(Dispatchers.IO) {
            runCatching {
                val tmp = File(file.parentFile, "caravan.json.tmp")
                tmp.writeText(text); tmp.renameTo(file) || run { file.writeText(text); true }
            }
        }
    }

    /** Applies [f] under the lock and saves. Also starts a new trading day when the date changed. */
    suspend fun edit(f: (State) -> State): State = lock.withLock {
        var s = _state.value
        val d = today()
        if (s.day != d) s = s.copy(day = d, traded = emptyMap())
        s = upkeep(f(s))
        _state.value = s
        save(s)
        s
    }

    fun write(f: (State) -> State) { scope.launch { edit(f) } }

    /** Stalls sell, late contracts lapse (their deposit is gone). */
    private fun upkeep(s0: State): State {
        var s = Shops.tick(s0)
        val t = System.currentTimeMillis()
        val late = s.contracts.filter { it.taken && t > it.until && Contracts.progress(s, it) < it.n }
        if (late.isNotEmpty()) s = s.copy(contracts = s.contracts - late.toSet())
        // a quarter one keeps coming back to gets a resident with errands (#16)
        s = Folk.settle(s)
        // a full set of curios gives its gift once (#17); all sixty, the keeper's title
        for (ctx in Curios.CONTEXTS) if ("set:${ctx.id}" !in s.told && Curios.complete(s, ctx.id)) {
            s = s.copy(gold = s.gold + Curios.SET_GOLD, renown = s.renown + Curios.SET_RENOWN, told = s.told + "set:${ctx.id}")
            news.tryEmit(News(tr("Набор «${ctx.ru}» собран: +${Curios.SET_GOLD} монет, +${Curios.SET_RENOWN} славы и ${Curios.perk(ctx.id)}",
                "The «${ctx.en}» set is complete: +${Curios.SET_GOLD} coins, +${Curios.SET_RENOWN} renown, and ${Curios.perk(ctx.id)}")))
        }
        if ("set:all" !in s.told && Curios.allFound(s)) {
            s = s.copy(gold = s.gold + Curios.ALL_GOLD, renown = s.renown + Curios.ALL_RENOWN, told = s.told + "set:all")
            news.tryEmit(News(tr("Все 60 диковин собраны! Вы – ${Curios.keeper}: +${Curios.ALL_GOLD} монет и +${Curios.ALL_RENOWN} славы",
                "All 60 curios found! You are the ${Curios.keeper}: +${Curios.ALL_GOLD} coins and +${Curios.ALL_RENOWN} renown")))
        }
        if (!Tutorial.finished(s) && s.onboarded) {
            val (next, paid) = Tutorial.advance(s)
            if (next.tut != s.tut) news.tryEmit(News(if (Tutorial.finished(next)) tr("Первые шаги пройдены – теперь вы настоящий купец!", "First steps done – you're a real merchant now!")
                else tr("Шаг пройден" + (if (paid > 0) ": +$paid монет" else ""), "Step done" + (if (paid > 0) ": +$paid coins" else ""))))
            s = next
        }
        return s
    }

    // ---------- the city ----------

    /** Loads the quarters around [lat],[lon] and sets home there. False without the network or with no quarters. */
    /**
     * Sets home here and loads the first ring of quarters. True when the map answered; without it home is still
     * set and the quarters come later by themselves (see [grow]).
     */
    suspend fun settle(lat: Double, lon: Double): Boolean {
        live.value = live.value.copy(loading = true)
        // the start waits 15 s at most: a slow map server is left to the background
        val qs = kotlinx.coroutines.withTimeoutOrNull(15_000) { Quarters.around(lat, lon, Quarters.RINGS.first()) }
        live.value = live.value.copy(loading = false)
        val home = qs?.let { Quarters.at(it, lat, lon) ?: it.minByOrNull { q -> Geo.distance(lat, lon, q.lat, q.lon) } }
        edit { s ->
            val merged = s.quarters.toMutableMap()
            qs?.forEach { q -> if (q.id !in merged) merged[q.id] = q }
            s.copy(homeLat = lat, homeLon = lon, quarters = merged, here = home?.id ?: s.here,
                reach = if (!qs.isNullOrEmpty()) maxOf(s.reach, Quarters.RINGS.first()) else s.reach)
        }
        if (home != null) enter(home.id, null)
        lastGrow = 0
        grow(lat, lon)
        return !qs.isNullOrEmpty()
    }

    private var growing = false
    private var lastGrow = 0L

    /**
     * Quarters load ring by ring: a small circle first (enough to start), then wider ones in the background.
     * A busy map server just stops the climb; it is tried again later (on the next start or position).
     */
    fun grow(lat: Double, lon: Double) {
        val t = System.currentTimeMillis()
        if (growing || t - lastGrow < 2 * 60_000L || lat == 0.0) return
        growing = true; lastGrow = t
        scope.launch {
            for (r in Quarters.RINGS) {
                if (now.reach >= r) continue
                val qs = Quarters.around(lat, lon, r) ?: break
                if (qs.isEmpty() && now.quarters.isEmpty() && r == Quarters.RINGS.last()) break
                edit { s -> s.copy(quarters = s.quarters + qs.filter { q -> q.id !in s.quarters && s.quarters.values.none { Geo.distance(it.lat, it.lon, q.lat, q.lon) < 600 } }.associateBy { it.id }, reach = maxOf(s.reach, r)) }
                // the first ring may have found the quarter we stand in
                if (now.here.isEmpty()) live.value.fix?.let { f -> Quarters.at(now.quarters.values, f.lat, f.lon) }?.let { q -> edit { it.copy(here = q.id) }; classify(q.id) }
            }
            growing = false
        }
    }

    private var spawning = false
    private var lastSpawn = 0L

    /** A ride far from every quarter (woods, fields, the town's edge) opens a new quarter where it is. */
    private fun spawn(lat: Double, lon: Double) {
        val t = System.currentTimeMillis()
        if (spawning || t - lastSpawn < 90_000) return
        spawning = true; lastSpawn = t
        scope.launch {
            try {
                val q = Quarters.spot(lat, lon, now.quarters.values.map { it.ru }.toSet())
                edit { s -> if (s.quarters.values.any { Geo.distance(it.lat, it.lon, q.lat, q.lon) < Quarters.GAP * 0.8 }) s else s.copy(quarters = s.quarters + (q.id to q)) }
            } finally { spawning = false }
        }
    }

    /** Away from every known quarter (another town): loads the new ones, at most every ten minutes. */
    private fun fetchAround(lat: Double, lon: Double) {
        val t = System.currentTimeMillis()
        if (t - lastFetch < 10 * 60_000) return
        lastFetch = t
        scope.launch {
            val qs = Quarters.around(lat, lon, 8000) ?: return@launch
            edit { s -> s.copy(quarters = s.quarters + qs.filter { q -> q.id !in s.quarters && s.quarters.values.none { Geo.distance(it.lat, it.lon, q.lat, q.lon) < 600 } }.associateBy { it.id }) }
        }
    }

    /** The current quarter, if still unknown, is asked about again (a restart may have cut the first ask). */
    fun ensureKnown() {
        val s = now
        if (s.onboarded && s.reach < Quarters.RINGS.last()) grow(s.homeLat, s.homeLon)
        s.quarters[s.here]?.takeIf { it.kind == null || (it.guessed && System.currentTimeMillis() - (tried[it.id] ?: 0) > 3 * 3600_000L) }?.let { classify(it.id) }
        s.trip?.quarters?.distinct()?.forEach { id -> if (s.quarters[id]?.kind == null) classify(id) }
    }

    /** Learns what a quarter is, once. */
    private fun classify(id: String) {
        if (!classifying.add(id)) return
        scope.launch {
            val q = now.quarters[id] ?: return@launch
            val real = Quarters.classify(q)
            val (a, b) = real ?: Quarters.guess(q)
            // a stand-in never replaces a kind already there. A real answer replaces a stand-in only while the
            // quarter hasn't been ridden through: a quarter seen as «waterfront» in the morning mustn't come back
            // as «gardens» in the evening (#13) – goods, contracts and memories hang on its kind.
            // a kind the merchant picked by hand (#14) is never overruled
            val seen = q.kind != null && (q.visits > 0 || q.changes > 0)
            if (real != null && seen) edit { s -> s.quarters[id]?.let { s.copy(quarters = s.quarters + (id to it.copy(guessed = false))) } ?: s }
            else if (real != null || q.kind == null) edit { s -> s.quarters[id]?.let { s.copy(quarters = s.quarters + (id to it.copy(kind = a, second = b, guessed = real == null))) } ?: s }
            if (real != null && !seen && q.kind != null && q.kind != a) news.tryEmit(News(tr("Картографы уточнили: ${q.ru} – это «${a.ru}»", "The cartographers checked: ${q.en} is «${a.en}»")))
            tried[id] = System.currentTimeMillis()
            classifying.remove(id)
        }
    }

    /** The merchant is in quarter [id] now; [trip] counts it once. */
    private suspend fun enter(id: String, trip: Trip?) {
        val s0 = now
        val q = s0.quarters[id] ?: return
        val fresh = q.visits == 0
        if (q.kind == null || (q.guessed && System.currentTimeMillis() - (tried[id] ?: 0) > 3 * 3600_000L)) classify(id)
        val counted = trip != null && id in trip.quarters
        edit { s ->
            val qq = s.quarters[id] ?: return@edit s
            val upd = if (counted && qq.visits > 0) qq else qq.copy(visits = qq.visits + 1, firstDay = if (qq.firstDay == 0L) today() else qq.firstDay)
            var t = s.trip
            if (t != null && t.quarters.lastOrNull() != id) t = t.copy(quarters = t.quarters + id, fresh = if (fresh && id !in t.fresh) t.fresh + id else t.fresh)
            // a letter for a resident is delivered by riding in (#16)
            val folk = s.folk.map { e -> if (e.since > 0 && !e.arrived && e.target == id && Folk.step(e)?.ask == Folk.Ask.Letter) e.copy(arrived = true) else e }
            s.copy(quarters = s.quarters + (id to upd), here = id, trip = t, renown = s.renown + if (fresh) 10 else 0, folk = folk)
        }
        if (trip != null) news.tryEmit(News(tr(
            if (fresh) "Новый квартал: ${q.ru}" else "Въезжаем: ${q.ru}",
            if (fresh) "A new quarter: ${q.en}" else "Entering ${q.en}"), speak = true))
    }

    // ---------- the ride ----------

    suspend fun startTrip(ride: Ride) {
        val t = System.currentTimeMillis()
        edit { s -> if (s.trip != null) s else s.copy(trip = Trip(start = t, ride = ride.id, quarters = listOfNotNull(s.here.takeIf { it.isNotEmpty() })), ride = ride.id) }
        live.value = Live(movingSince = 0, stillSince = t)
    }

    /** A position while a trip is on (and also between trips, to know the current quarter). */
    suspend fun onFix(f: Fix) {
        val prev = live.value
        // smoothed speed: the satellites' doppler if any, else from the last fix
        val raw = f.speed?.toDouble() ?: prev.fix?.let { p ->
            val dt = (f.time - p.time) / 1000.0
            if (dt > 0.5) Geo.distance(p.lat, p.lon, f.lat, f.lon) / dt else null
        } ?: prev.speed
        val speed = if (prev.fix == null) raw else prev.speed * 0.5 + raw * 0.5
        val moving = speed > 3.5
        // a rough position (a bus deep in the city, a train, a phone in a bag) can't count metres, but a big enough
        // jump from the last one still says the ride goes on, and keeps it from ending by itself
        val heard = System.currentTimeMillis()
        val stir = prev.fix?.let { p -> f.accuracy <= 500 && Geo.distance(p.lat, p.lon, f.lat, f.lon) > maxOf(150.0, (f.accuracy + p.accuracy).toDouble()) } == true
        live.value = prev.copy(fix = f, speed = speed, heardAt = heard, stirAt = if (stir) heard else prev.stirAt,
            movingSince = if (moving) (prev.movingSince.takeIf { it > 0 } ?: f.time) else 0,
            stillSince = if (speed < 1.2) (prev.stillSince.takeIf { it > 0 } ?: f.time) else 0)

        val s = now
        val q = Quarters.at(s.quarters.values, f.lat, f.lon)
        // beyond every known quarter: «beyond the city gates» until the new place's quarters load
        if (q == null && s.quarters.isNotEmpty() && f.accuracy < 300) {
            fetchAround(f.lat, f.lon)
            if (s.here.isNotEmpty()) edit { it.copy(here = "") }
        }
        val trip = s.trip
        // long empty stretches get quarters of their own, so a ride through woods and fields still crosses several
        if (trip != null && s.quarters.isNotEmpty() && (f.accuracy <= 60 || f.simulated) &&
            s.quarters.values.none { Geo.distance(it.lat, it.lon, f.lat, f.lon) < Quarters.GAP }) spawn(f.lat, f.lon)
        if (trip != null && q == null && s.quarters.isNotEmpty() && f.accuracy < 300 && !trip.outside) edit { st -> st.trip?.let { st.copy(trip = it.copy(outside = true)) } ?: st }
        if (trip == null) {
            if (q != null && q.id != s.here) edit { it.copy(here = q.id) }
            ensureKnown()
            return
        }
        if (f.accuracy > 60 && !f.simulated) return
        var meters = 0.0
        var underground = 0
        var jam = 0
        if (trip.lastAt > 0) {
            val d = Geo.distance(trip.lastLat, trip.lastLon, f.lat, f.lon)
            val dt = (f.time - trip.lastAt) / 1000.0
            // the underground: no satellites for minutes, then up somewhere else. By metro any jump is, however quick
            if (if (Ride.of(trip.ride) == Ride.METRO) d > 300 else dt > 150 && d > 800) underground = 1
            // a city ride: noise under 25 m (or twice the fix's own error) is left; a car can't jump faster than 60 m/s;
            // a phone lying still by the satellites' own word hasn't gone anywhere, however the dot drifts
            val lying = f.speed != null && f.speed < 0.5 && d < 150
            if (d >= maxOf(25.0, f.accuracy * 2.0) && !lying && (dt <= 0 || d / dt < 60)) meters = d
        }
        // standing still mid-ride in a car or a bus, between fixes; it turns into a jam only when the ride moves on
        val step = prev.fix?.let { (f.time - it.time) / 1000.0 } ?: 0.0
        if (speed < 1.5 && step in 0.0..60.0 && trip.meters > 300 && Ride.of(trip.ride) != Ride.METRO) jam = step.toInt()
        val moved = meters > 0 || trip.lastAt == 0L
        val hour = hourOf(f.time)
        edit { st ->
            val t = st.trip ?: return@edit st
            st.copy(trip = t.copy(
                meters = t.meters + meters,
                // moving again: the wait was a jam if it was short; twenty minutes and more was a stop, not traffic
                jamSec = t.jamSec + if (moving && t.jamWait <= JAM_MAX) t.jamWait else 0,
                jamWait = if (moving) 0 else t.jamWait + jam,
                underground = t.underground + underground,
                night = t.night || hour >= minOf(House.nightFrom(st), if (Events.of().id == "stars") 19 else 21) || hour < 5,
                lastLat = if (moved) f.lat else t.lastLat, lastLon = if (moved) f.lon else t.lastLon, lastAt = if (moved) f.time else t.lastAt,
                firstLat = if (t.lastAt == 0L) f.lat else t.firstLat, firstLon = if (t.lastAt == 0L) f.lon else t.firstLon,
            ))
        }
        if (q != null && (trip.quarters.lastOrNull() != q.id)) enter(q.id, now.trip)
    }

    private val JAM_MAX = 20 * 60

    /** True when a trip has stood still long enough to be over by itself. */
    fun idle(nowMs: Long = System.currentTimeMillis()): Boolean {
        val t = now.trip ?: return false
        val l = live.value
        return Idle.over(nowMs, t.start, t.lastAt, l.stirAt, l.heardAt, Ride.of(t.ride) == Ride.METRO)
    }

    // ---------- the metro: stations instead of the GPS ----------

    /** One load at a time: a second asker (the picker while the ride's own load is under way) waits for it. */
    private val stationsLock = Mutex()

    /** How loading the stations went: loaded (the list may be empty – no metro), nowhere to load around, no answer. */
    enum class StationsLoad { OK, NO_PLACE, NO_NET }

    /**
     * The city's stations, loaded once around home (again after a month, or a day when none were found). Without
     * a home: around the phone, or [lat], [lon], or where the ride began.
     */
    suspend fun stations(lat: Double? = null, lon: Double? = null): StationsLoad = stationsLock.withLock {
        val s = now
        val t = System.currentTimeMillis()
        if (s.stationsAt > 0 && t - s.stationsAt < 30 * 86_400_000L) return@withLock StationsLoad.OK
        val at = (s.homeLat to s.homeLon).takeIf { it.first != 0.0 }
            ?: live.value.fix?.let { it.lat to it.lon }
            ?: (if (lat != null && lon != null) lat to lon else null)
            ?: s.trip?.takeIf { it.lastAt > 0 }?.let { it.firstLat to it.firstLon }
            ?: return@withLock StationsLoad.NO_PLACE
        val got = Metro.around(at.first, at.second) ?: return@withLock StationsLoad.NO_NET
        edit { it.copy(stations = got, stationsAt = if (got.isEmpty()) t - 29 * 86_400_000L else t) }
        StationsLoad.OK
    }

    /** The station a metro ride began at, when known. */
    fun metroStart(): Station? = now.trip?.takeIf { it.lastAt > 0 }?.let { Metro.nearest(now.stations, it.firstLat, it.firstLon, 50.0) }

    /** A position near a station becomes the station itself. */
    private fun snap(lat: Double, lon: Double): Pair<Double, Double> =
        Metro.nearest(now.stations, lat, lon)?.let { it.lat to it.lon } ?: (lat to lon)

    /**
     * Where a metro ride began: a station picked by hand, or the position found at the entrance ([auto]; it never
     * overrides a start already known). The quarter the ride began in becomes the entrance's.
     */
    suspend fun metroFrom(lat: Double, lon: Double, auto: Boolean = false) {
        val t = now.trip ?: return
        if (Ride.of(t.ride) != Ride.METRO || (auto && t.lastAt > 0)) return
        val (la, lo) = snap(lat, lon)
        // nothing was ridden yet: the quarter seen before setting off (or a wrong entrance) gives way to the station's
        val known = Quarters.at(now.quarters.values, la, lo) != null
        edit { s -> s.trip?.let { tt -> s.copy(trip = tt.copy(quarters = if (known) emptyList() else tt.quarters, lastAt = 0, lastLat = 0.0, lastLon = 0.0)) } ?: s }
        // timed at the start, so the way to the exit is never too fast to count
        onFix(Fix(la, lo, 5f, t.start, speed = 0f, simulated = true))
    }

    /** The exit of a metro ride: one position (or a station picked), then the ride is over. */
    suspend fun metroEnd(lat: Double, lon: Double): Trip? {
        val (la, lo) = snap(lat, lon)
        if (now.trip != null) onFix(Fix(la, lo, 5f, System.currentTimeMillis(), speed = 0f, simulated = true))
        return endTrip()
    }

    /** «Arrived»: the ledger page is written, the steward sells, the route grows. */
    suspend fun endTrip(): Trip? {
        var done: Trip? = null
        val wx = now.trip?.let { tr -> if (tr.lastAt > 0) Weather.now(tr.lastLat, tr.lastLon) else "" }.orEmpty()
        edit { s0 ->
            val t0 = s0.trip?.copy(weather = wx) ?: return@edit s0
            var s = s0
            val end = maxOf(t0.lastAt, t0.start + 1000)
            var t = t0.copy(end = if (t0.lastAt > 0) end else System.currentTimeMillis())
            var renown = 0
            val passed = t.quarters.distinct().mapNotNull { s.quarters[it] }
            // a find by the roadside, on rides of 2 km and more
            val finds = if (t.km >= House.findKm(s)) (if (t.km >= 8 && (House.has(s, "shed") || Curios.complete(s, "road"))) 2 else 1) else 0
            for (fi in 0 until finds) if (s.pack.size < Market.capacity(s)) {
                val r = Rand(hash("find:${t.start}:$fi"))
                val src = passed.filter { it.kind != null }.ifEmpty { passed }
                val q = src.getOrNull(r.int(src.size.coerceAtLeast(1)))
                val k = q?.kind ?: Kind.entries[r.int(Kind.entries.size)]
                val g = Goods.of(k)[r.int(3)]
                if (q != null) {
                    s = s.copy(pack = s.pack + Lot(s.nextId, g.id, q.id, 0, t.end), nextId = s.nextId + 1)
                    t = t.copy(find = g.id)
                }
            }
            // the steward sells what was marked, each where it fetched most on the way
            val deals = ArrayList<Deal>()
            var pack = s.pack
            var gold = s.gold
            var traded = s.traded
            for (lot in s.pack.filter { it.auto }) {
                val best = passed.filter { it.id != lot.origin }.maxByOrNull { Market.price(lot, it, s.copy(traded = traded)) } ?: continue
                val p = Market.price(lot, best, s.copy(traded = traded))
                if (p <= lot.paid) continue
                pack = pack.filter { it.id != lot.id }
                gold += p
                traded = traded + ("s:${best.id}:${lot.good}" to (traded["s:${best.id}:${lot.good}"] ?: 0) + 1)
                deals += Deal(lot.good, best.id, p, t.end, buy = false, origin = lot.origin, paid = lot.paid)
                renown += 1
            }
            s = s.copy(pack = pack, gold = gold, traded = traded, deals = (s.deals + deals).takeLast(600))
            // fragile bales may break in a shaken cart
            val shake = Risk.breakChance(t, s)
            if (shake > 0) {
                val r = Rand(hash("break:${t.start}"))
                val broke = s.pack.filter { it.good in Risk.fragile && r.next() < shake }.take(1)
                if (broke.isNotEmpty()) { s = s.copy(pack = s.pack - broke.toSet()); t = t.copy(broken = broke.map { it.good }) }
            }
            // a curio of the road
            Curios.roll(s, t)?.let { c -> s = s.copy(curios = s.curios + c.id); t = t.copy(curio = c.id); renown += 5 }
            // a meeting on the road, now and then
            if (Tutorial.finished(s)) Encounters.roll(s, t.start)?.let { e ->
                val d = today()
                s = s.copy(encounter = "$e:${t.start}", encDay = d, encCount = (if (s.encDay == d) s.encCount else 0) + 1)
            }
            // the route between where it began and where it ended
            var routes = s.routes
            if (t.from.isNotEmpty() && t.to.isNotEmpty() && t.from != t.to) {
                val key = Route.key(t.from, t.to)
                val r = routes[key] ?: Route(minOf(t.from, t.to), maxOf(t.from, t.to))
                val r2 = r.copy(trips = r.trips + 1)
                if (Guild.level(r2) > Guild.level(r) || r.trips == 0) renown += 5
                if ("fountain" in r.built) renown += 10
                routes = routes + (key to r2)
            }
            renown += passed.size * 2 + (t.km / 3).toInt().coerceAtMost(20)
            if (Curios.complete(s, "under") && (t.underground > 0 || Ride.of(t.ride) == Ride.METRO)) renown += 5
            // a ride after dark, or along a watchtower's road, opens the night market until morning
            val tower = routes[Route.key(t.from, t.to)]?.built?.contains("tower") == true
            val night = if (t.night || tower) nextMorning(t.end) else s.nightUntil
            t = t.copy(renown = renown + t.fresh.size * 10)
            done = t
            val keep = t.km >= 0.3 || t.quarters.distinct().size > 1
            s.copy(trip = null, trips = if (keep) (listOf(t) + s.trips).take(400) else s.trips, routes = routes,
                renown = s.renown + renown, nightUntil = maxOf(s.nightUntil, if (keep) night else 0))
        }
        val t = done
        if (t != null && (t.km >= 0.3 || t.quarters.distinct().size > 1)) finished.value = t
        live.value = Live(fix = live.value.fix)
        return t
    }

    private fun nextMorning(at: Long): Long {
        val z = ZoneId.systemDefault()
        val d = java.time.Instant.ofEpochMilli(at).atZone(z)
        val day = if (d.hour < 6) d.toLocalDate() else d.toLocalDate().plusDays(1)
        return day.atTime(LocalTime.of(6, 0)).atZone(z).toInstant().toEpochMilli()
    }

    // ---------- trade ----------

    /** Trading is closed to a driver while the cart moves. */
    fun tradeLocked(): Boolean {
        val s = now
        val t = s.trip ?: return false
        return Ride.of(t.ride).driver && live.value.moving
    }

    suspend fun buy(quarter: String, good: String): Boolean {
        if (tradeLocked()) return false
        var ok = false
        edit { s ->
            val q = s.quarters[quarter] ?: return@edit s
            if (s.pack.size >= Market.capacity(s)) return@edit s
            val o = Market.sells(q, s).firstOrNull { it.good.id == good } ?: return@edit s
            if (o.stock <= 0 || s.gold < o.price) return@edit s
            ok = true
            val k = "b:$quarter:$good"
            val t = System.currentTimeMillis()
            s.copy(gold = s.gold - o.price, pack = s.pack + Lot(s.nextId, good, quarter, o.price, t), nextId = s.nextId + 1,
                traded = s.traded + (k to (s.traded[k] ?: 0) + 1), deals = (s.deals + Deal(good, quarter, o.price, t, buy = true, origin = quarter)).takeLast(600))
        }
        return ok
    }

    suspend fun buyNight(good: String): Boolean {
        if (tradeLocked()) return false
        var ok = false
        edit { s ->
            if (!Market.nightOpen(s) || s.pack.size >= Market.capacity(s)) return@edit s
            val o = Market.night(s).firstOrNull { it.good.id == good } ?: return@edit s
            if (o.stock <= 0 || s.gold < o.price) return@edit s
            ok = true
            val k = "b:night:$good"
            val t = System.currentTimeMillis()
            s.copy(gold = s.gold - o.price, pack = s.pack + Lot(s.nextId, good, s.here, o.price, t), nextId = s.nextId + 1,
                traded = s.traded + (k to 1), deals = (s.deals + Deal(good, "night", o.price, t, buy = true, origin = s.here)).takeLast(600))
        }
        return ok
    }

    /** Sells [lot] in [quarter]; the price paid, or 0. */
    suspend fun sell(quarter: String, lotId: Long): Int {
        if (tradeLocked()) return 0
        var got = 0
        edit { s ->
            val q = s.quarters[quarter] ?: return@edit s
            val lot = s.pack.firstOrNull { it.id == lotId } ?: return@edit s
            val p = Market.price(lot, q, s) * (100 + s.luck) / 100
            got = p
            val k = "s:$quarter:${lot.good}"
            val profit = p - lot.paid
            s.copy(gold = s.gold + p, luck = 0, pack = s.pack.filter { it.id != lotId }, traded = s.traded + (k to (s.traded[k] ?: 0) + 1),
                renown = s.renown + (if (profit >= 20) 2 else if (profit > 0) 1 else 0),
                deals = (s.deals + Deal(lot.good, quarter, p, System.currentTimeMillis(), buy = false, origin = lot.origin, paid = lot.paid)).takeLast(600))
        }
        return got
    }

    fun toggleAuto(lotId: Long) = write { s -> s.copy(pack = s.pack.map { if (it.id == lotId) it.copy(auto = !it.auto) else it }) }

    // ---------- the guild ----------

    suspend fun claim(day: Long, index: Int): Boolean {
        var ok = false
        edit { s ->
            val o = Orders.of(s, day).firstOrNull { it.index == index } ?: return@edit s
            val key = Orders.key(day, o)
            if (key in s.orders || !o.done(s, day)) return@edit s
            ok = true
            s.copy(orders = s.orders + key, renown = s.renown + o.renown * House.orderMul(s), gold = s.gold + o.gold)
        }
        return ok
    }

    suspend fun upgradePack(): Boolean {
        var ok = false
        edit { s ->
            val n = s.packLevel + 1
            if (n > Market.packSizes.lastIndex || s.gold < Market.packPrices[n]) return@edit s
            ok = true
            s.copy(gold = s.gold - Market.packPrices[n], packLevel = n)
        }
        return ok
    }

    suspend fun build(routeKey: String, id: String): Boolean {
        var ok = false
        edit { s ->
            val r = s.routes[routeKey] ?: return@edit s
            val b = Guild.byId[id] ?: return@edit s
            if (id in r.built || Guild.level(r) < b.needs || s.gold < b.price) return@edit s
            ok = true
            s.copy(gold = s.gold - b.price, renown = s.renown + b.price / 50, routes = s.routes + (routeKey to r.copy(built = r.built + id)))
        }
        return ok
    }

    /**
     * The merchant names the quarter's kind (#14): the map may call a whole district «gardens» for its lawns.
     * Its second kind stays unless it is the new one; the first change is free.
     */
    suspend fun rekind(id: String, k: Kind): Boolean {
        var ok = false
        edit { s ->
            val q = s.quarters[id] ?: return@edit s
            val price = Quarters.rekindPrice(q)
            if (q.kind == null || q.kind == k || s.gold < price) return@edit s
            ok = true
            val second = q.second?.takeIf { it != k } ?: q.kind
            s.copy(gold = s.gold - price, quarters = s.quarters + (id to q.copy(kind = k, second = second, guessed = false, changes = q.changes + 1)))
        }
        return ok
    }

    // ---------- residents (#16) ----------

    /** Takes the resident's current request; a letter gets its quarter now. */
    suspend fun takeErrand(quarter: String): Boolean {
        var ok = false
        edit { s ->
            val e = Folk.at(s, quarter) ?: return@edit s
            if (e.since > 0 || Folk.done(e) || Folk.taken(s) >= Folk.MAX_TAKEN) return@edit s
            var e2 = e.copy(since = System.currentTimeMillis(), arrived = false)
            if (Folk.step(e)?.ask == Folk.Ask.Letter) {
                val target = Folk.pickTarget(s, e2)
                if (target.isEmpty()) return@edit s
                e2 = e2.copy(target = target)
            }
            ok = true
            s.copy(folk = s.folk.map { if (it.quarter == quarter) e2 else it })
        }
        return ok
    }

    /** Hands the request over: goods leave the pack, the reward comes, the next request waits to be taken. */
    suspend fun claimErrand(quarter: String): Boolean {
        var ok = false
        edit { s ->
            val e = Folk.at(s, quarter) ?: return@edit s
            if (!Folk.ready(s, e)) return@edit s
            val give = Folk.fitting(s, e).map { it.id }.toSet()
            val i = e.step
            ok = true
            val next = e.copy(step = i + 1, since = 0, arrived = false, target = "")
            if (Folk.done(next)) news.tryEmit(News(tr("${Folk.of(e).ru}: «${Folk.of(e).thanksRu}»", "${Folk.of(e).en}: «${Folk.of(e).thanksEn}»")))
            s.copy(pack = s.pack.filter { it.id !in give }, gold = s.gold + Folk.gold[i], renown = s.renown + Folk.renown[i],
                folk = s.folk.map { if (it.quarter == quarter) next else it })
        }
        return ok
    }

    /** Lets a route grow over (#15): it goes with what is built on it; riding it again starts a new track. */
    fun abandon(routeKey: String) = write { s -> s.copy(routes = s.routes - routeKey) }

    // ---------- merchants ----------

    /** Talking to a merchant takes their current request (once). */
    fun talk(id: String) = write { s -> if (s.mSince[id] != null || Merchants.byId[id]?.let { Merchants.friend(s, it) } != false) s else s.copy(mSince = s.mSince + (id to System.currentTimeMillis())) }

    suspend fun claimMerchant(id: String): Boolean {
        var ok = false
        edit { s ->
            val m = Merchants.byId[id] ?: return@edit s
            if (!Merchants.ready(s, m)) return@edit s
            val st = m.steps[Merchants.step(s, m)]
            ok = true
            val next = Merchants.step(s, m) + 1
            val last = next >= m.steps.size
            // the last request also gives a curio of their kind, if one is left to find
            val gift = if (last) Curios.of(m.kind.id).firstOrNull { it.id !in s.curios } else null
            s.copy(gold = s.gold + st.gold, renown = s.renown + st.renown, mStep = s.mStep + (id to next),
                mSince = if (last) s.mSince - id else s.mSince + (id to System.currentTimeMillis()),
                curios = if (gift != null) s.curios + gift.id else s.curios)
        }
        return ok
    }

    // ---------- contracts ----------

    suspend fun takeContract(c: Contract): Boolean {
        var ok = false
        edit { s ->
            if (s.gold < c.deposit || s.contracts.any { it.id == c.id } || s.contracts.count { it.taken } >= 3) return@edit s
            ok = true
            s.copy(gold = s.gold - c.deposit, contracts = s.contracts + c.copy(since = System.currentTimeMillis()))
        }
        return ok
    }

    suspend fun claimContract(id: Long): Boolean {
        var ok = false
        edit { s ->
            val c = s.contracts.firstOrNull { it.id == id } ?: return@edit s
            if (Contracts.progress(s, c) < c.n) return@edit s
            ok = true
            s.copy(gold = s.gold + c.deposit + c.reward, renown = s.renown + 20, contracts = s.contracts - c)
        }
        return ok
    }

    // ---------- stalls in other quarters ----------

    suspend fun openShop(quarter: String): Boolean {
        var ok = false
        edit { s ->
            val price = Shops.price(s) ?: return@edit s
            if (s.gold < price || s.shops.any { it.quarter == quarter } || s.quarters[quarter]?.known != true) return@edit s
            ok = true
            s.copy(gold = s.gold - price, shops = s.shops + Shop(quarter, tick = System.currentTimeMillis()), renown = s.renown + 15)
        }
        return ok
    }

    /** Leaves a bale at the stall; only while standing in its quarter. */
    fun stock(quarter: String, lotId: Long) = write { s ->
        val sh = s.shops.firstOrNull { it.quarter == quarter } ?: return@write s
        val lot = s.pack.firstOrNull { it.id == lotId } ?: return@write s
        if (s.here != quarter || sh.stock.size >= Shops.SLOTS) return@write s
        val now = System.currentTimeMillis()
        s.copy(pack = s.pack - lot, shops = s.shops.map { if (it.quarter == quarter) it.copy(stock = it.stock + lot, tick = if (it.stock.isEmpty()) now else it.tick) else it })
    }

    fun collect(quarter: String) = write { s ->
        val sh = s.shops.firstOrNull { it.quarter == quarter } ?: return@write s
        if (s.here != quarter || sh.till == 0) return@write s
        s.copy(gold = s.gold + sh.till, shops = s.shops.map { if (it.quarter == quarter) it.copy(till = 0) else it })
    }

    // ---------- road meetings ----------

    /** The answer to the waiting road meeting; returns a line to show. */
    suspend fun resolve(choice: Int): String {
        var line = ""
        edit { s0 ->
            val id = s0.encounter.substringBefore(':')
            val seed = s0.encounter.substringAfter(':').toLongOrNull() ?: 0L
            var s = s0.copy(encounter = "")
            val r = Rand(hash("res:$seed"))
            line = when (id to choice) {
                "pedlar" to 0 -> {
                    val give = s.pack.minByOrNull { it.paid }
                    if (give == null) tr("Вам нечего отдать – торговец пожал плечами.", "You had nothing to give – the pedlar shrugged.")
                    else {
                        val g = Goods.rare[r.int(Goods.rare.size)]
                        s = s.copy(pack = s.pack - give + Lot(s.nextId, g.id, s.here, 0, System.currentTimeMillis()), nextId = s.nextId + 1)
                        tr("В свёртке – ${g.ru.lowercase()}!", "In the bundle: ${g.en.lowercase()}!")
                    }
                }
                "traveller" to 0 -> if (s.gold >= 10) { s = s.copy(gold = s.gold - 10, renown = s.renown + 15); tr("Путник обещал рассказать о вас всему городу. +15 славы.", "The traveller promised to tell the whole city about you. +15 renown.") } else tr("Денег на угощение не нашлось.", "No coins for food.")
                "dice" to 0 -> when {
                    s.gold < 30 -> tr("Ставить нечего.", "Nothing to stake.")
                    r.next() < 0.5 -> { s = s.copy(gold = s.gold + 30); tr("Выигрыш! +30 монет.", "You won! +30 coins.") }
                    else -> { s = s.copy(gold = s.gold - 30); tr("Кости легли не так. −30 монет.", "The dice fell wrong. −30 coins.") }
                }
                "cartographer" to 0 -> if (s.gold < 40) tr("Не хватило монет.", "Not enough coins.") else {
                    val here = s.quarters[s.here]
                    val unknown = s.quarters.values.filter { it.kind == null }.sortedBy { q -> here?.let { Geo.distance(it.lat, it.lon, q.lat, q.lon) } ?: 0.0 }.take(2)
                    unknown.forEach { classify(it.id) }
                    s = s.copy(gold = s.gold - 40)
                    if (unknown.isEmpty()) tr("Картограф развёл руками: вы и так всё знаете.", "The cartographer spread his hands: you know it all already.")
                    else tr("Картограф отметил на карте: ", "The cartographer marked on your map: ") + unknown.joinToString(", ") { it.name }
                }
                "lostbale" to 0 -> { s = s.copy(gold = s.gold + 25, renown = s.renown + 20); tr("Хозяин нашёлся и отблагодарил: +25 монет, +20 славы.", "The owner turned up and thanked you: +25 coins, +20 renown.") }
                "lostbale" to 1 -> if (s.pack.size >= Market.capacity(s)) tr("Во вьюке нет места.", "No room in the pack.") else {
                    val g = Goods.all.filter { !it.rare }.let { it[r.int(it.size)] }
                    s = s.copy(pack = s.pack + Lot(s.nextId, g.id, s.here, 0, System.currentTimeMillis()), nextId = s.nextId + 1)
                    tr("В тюке – ${g.ru.lowercase()}.", "In the bale: ${g.en.lowercase()}.")
                }
                "musician" to 0 -> if (s.gold < 5) tr("Монетки не нашлось.", "No coin to spare.") else {
                    s = s.copy(gold = s.gold - 5, renown = s.renown + 3)
                    Market.rumours(s).firstOrNull()?.let { tr("Он пел: ", "He sang: ") + it.text } ?: tr("Песня была красивая.", "It was a lovely song.")
                }
                "fortune" to 0 -> if (s.gold < 20) tr("Не хватило монет.", "Not enough coins.") else { s = s.copy(gold = s.gold - 20, luck = 25); tr("«Следующая сделка принесёт удачу»: ближайшая продажа – на 25% дороже.", "«Your next deal will be lucky»: the next sale pays 25% more.") }
                "toll" to 0 -> if (s.gold < 15) tr("Стражник пропустил даром – вид у вас усталый.", "The guard let you pass free – you looked tired.") else { s = s.copy(gold = s.gold - 15, renown = s.renown + 5); tr("Пошлина уплачена. Стража запомнит честного купца.", "Toll paid. The guards will remember an honest merchant.") }
                else -> tr("Вы поехали дальше.", "You rode on.")
            }
            s
        }
        return line
    }

    /** Builds a part of the trading house. */
    suspend fun buildHouse(id: String): Boolean {
        var ok = false
        edit { s ->
            val p = House.byId[id] ?: return@edit s
            if (!House.canBuild(s, p)) return@edit s
            ok = true
            s.copy(gold = s.gold - p.price, house = s.house + id, renown = s.renown + p.renown + if (id == "treasury") 500 else 0)
        }
        return ok
    }

    /** The garden's daily gift: a good of the home quarter's kind, free. Null if not ready or no room. */
    suspend fun takeGift(): String? {
        var got: String? = null
        edit { s ->
            if (!House.giftReady(s) || s.pack.size >= Market.capacity(s)) return@edit s
            val home = Quarters.at(s.quarters.values, s.homeLat, s.homeLon) ?: return@edit s
            val k = home.kind ?: Quarters.guess(home).first
            val g = Goods.of(k)[Rand(hash("gift:${s.day}")).int(3)]
            got = g.id
            s.copy(giftDay = s.day, pack = s.pack + Lot(s.nextId, g.id, home.id, 0, System.currentTimeMillis()), nextId = s.nextId + 1)
        }
        return got
    }

    suspend fun told(key: String) = edit { it.copy(told = it.told + key) }

    /** Wipes the game (settings → start over). */
    suspend fun reset() = edit { State(theme = it.theme, voice = it.voice) }

    fun dayOfToday() = dayOf(System.currentTimeMillis())

    suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

    @Suppress("unused") private fun epochDay(d: LocalDate) = d.toEpochDay()
}

/**
 * When a ride ends by itself. Its end is the last place it moved to, so waiting longer costs nothing but a little
 * battery, while ending too early loses the rest of the ride (a long jam, a train, the GPS gone quiet).
 */
object Idle {
    /** The phone says the ride stands: a jam up to twenty minutes is still part of it (see the jams' own limit). */
    const val STILL = 20 * 60_000L
    /** No positions at all: the satellites are lost (a train, a tunnel, a bag), not the ride. */
    const val SILENT = 45 * 60_000L
    /** This long without any position is silence. */
    const val QUIET = 3 * 60_000L
    /** Under the ground there is nothing to tell standing from riding: a metro ride waits for «Arrived». */
    const val METRO = 4 * 3600_000L

    fun over(now: Long, start: Long, movedAt: Long, stirAt: Long, heardAt: Long, metro: Boolean): Boolean {
        val since = maxOf(if (movedAt > 0) movedAt else start, stirAt)
        val limit = when {
            metro -> METRO
            now - maxOf(heardAt, start) > QUIET -> SILENT
            else -> STILL
        }
        return now - since > limit
    }
}
