package app.caravan.data

import app.caravan.domain.Kind
import org.json.JSONArray
import org.json.JSONObject

/** A quarter of the real city: a named place from OpenStreetMap; its kind is learnt on the first visit. */
data class Quarter(
    val id: String,
    val ru: String,
    val en: String,
    val lat: Double,
    val lon: Double,
    val kind: Kind? = null,
    val second: Kind? = null,
    val visits: Int = 0,
    val firstDay: Long = 0,
    /** The kind is a stand-in: the map server didn't answer, it will be asked again. */
    val guessed: Boolean = false,
) {
    val name get() = app.caravan.domain.tr(ru, en)
    /** Its kind is known: visited, or told by the cartographer. */
    val known get() = kind != null
}

/** A bale in the pack: a good bought in quarter [origin] for [paid]. [auto]: the steward may sell it on the way. */
data class Lot(val id: Long, val good: String, val origin: String, val paid: Int, val at: Long, val auto: Boolean = false)

/** One sale or purchase. */
data class Deal(val good: String, val quarter: String, val price: Int, val at: Long, val buy: Boolean, val origin: String = "", val paid: Int = 0)

/** A ride through the city, from the first fix to «arrived». */
data class Trip(
    val start: Long,
    val end: Long = 0,
    val ride: String = "car",
    val meters: Double = 0.0,
    val quarters: List<String> = emptyList(),
    val fresh: List<String> = emptyList(),
    val jamSec: Int = 0,
    /** Standing still right now, seconds: it becomes a jam only if the ride moves on soon. */
    val jamWait: Int = 0,
    val night: Boolean = false,
    val underground: Int = 0,
    val renown: Int = 0,
    val find: String = "",
    val curio: String = "",
    val broken: List<String> = emptyList(),
    val weather: String = "",
    val outside: Boolean = false,
    val lastLat: Double = 0.0,
    val lastLon: Double = 0.0,
    val lastAt: Long = 0,
) {
    val from get() = quarters.firstOrNull().orEmpty()
    val to get() = quarters.lastOrNull().orEmpty()
    val km get() = meters / 1000
}

/** A road between two quarters that the merchant keeps taking; buildings on it help trade. */
data class Route(val a: String, val b: String, val trips: Int = 0, val built: Set<String> = emptySet()) {
    val key get() = Route.key(a, b)
    companion object { fun key(a: String, b: String) = if (a < b) "$a|$b" else "$b|$a" }
}

data class State(
    val onboarded: Boolean = false,
    val homeLat: Double = 0.0,
    val homeLon: Double = 0.0,
    val quarters: Map<String, Quarter> = emptyMap(),
    val here: String = "",
    val gold: Int = 120,
    val renown: Int = 0,
    val packLevel: Int = 0,
    val pack: List<Lot> = emptyList(),
    val trips: List<Trip> = emptyList(),
    val trip: Trip? = null,
    /** Every sale and purchase, newest last (the last 600). */
    val deals: List<Deal> = emptyList(),
    val routes: Map<String, Route> = emptyMap(),
    /** Today's trade, so stalls run out and prices sag: "b:quarter:good" bought, "s:quarter:good" sold. */
    val day: Long = 0,
    val traded: Map<String, Int> = emptyMap(),
    /** Daily orders done: "day:index". */
    val orders: Set<String> = emptySet(),
    val nightUntil: Long = 0,
    val told: Set<String> = emptySet(),
    val theme: String = "",
    val voice: Boolean = false,
    val ride: String = "car",
    val nextId: Long = 1,
    /** Parts of the trading house built. */
    val house: Set<String> = emptySet(),
    /** The day the garden last gave its gift. */
    val giftDay: Long = 0,
    val curios: Set<String> = emptySet(),
    /** Each merchant's request number and when it was taken. */
    val mStep: Map<String, Int> = emptyMap(),
    val mSince: Map<String, Long> = emptyMap(),
    val contracts: List<app.caravan.domain.Contract> = emptyList(),
    /** A road meeting waiting for a choice ("id:tripStart"), and today's count. */
    val encounter: String = "",
    val encDay: Long = 0,
    val encCount: Int = 0,
    val shops: List<app.caravan.domain.Shop> = emptyList(),
    /** Fortune told: the next sale pays this much more (once). */
    val luck: Int = 0,
    /** First steps paid for so far. */
    val tut: Int = 0,
    /** The car's Bluetooth device that announces a ride: address and name. */
    val btAddr: String = "",
    val btName: String = "",
    /** How far around home the quarters are loaded, metres. */
    val reach: Int = 0,
) {
    fun quarter(id: String) = quarters[id]
}

// ---- JSON, by hand: small, readable and stable across versions ----

private fun JSONObject.str(k: String) = optString(k, "")
private fun <T> JSONArray?.list(f: (JSONObject) -> T): List<T> = if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }
private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }

fun Quarter.json() = JSONObject().put("id", id).put("ru", ru).put("en", en).put("lat", lat).put("lon", lon)
    .put("kind", kind?.id ?: "").put("second", second?.id ?: "").put("visits", visits).put("firstDay", firstDay).put("guessed", guessed)

fun quarterOf(o: JSONObject) = Quarter(o.str("id"), o.str("ru"), o.str("en"), o.getDouble("lat"), o.getDouble("lon"),
    Kind.of(o.str("kind")), Kind.of(o.str("second")), o.optInt("visits"), o.optLong("firstDay"), o.optBoolean("guessed"))

fun Lot.json() = JSONObject().put("id", id).put("good", good).put("origin", origin).put("paid", paid).put("at", at).put("auto", auto)
fun lotOf(o: JSONObject) = Lot(o.getLong("id"), o.str("good"), o.str("origin"), o.optInt("paid"), o.optLong("at"), o.optBoolean("auto"))

fun Deal.json() = JSONObject().put("good", good).put("quarter", quarter).put("price", price).put("at", at).put("buy", buy).put("origin", origin).put("paid", paid)
fun dealOf(o: JSONObject) = Deal(o.str("good"), o.str("quarter"), o.optInt("price"), o.optLong("at"), o.optBoolean("buy"), o.str("origin"), o.optInt("paid"))

fun Trip.json(): JSONObject = JSONObject().put("start", start).put("end", end).put("ride", ride).put("meters", meters)
    .put("quarters", JSONArray(quarters)).put("fresh", JSONArray(fresh)).put("jamSec", jamSec).put("jamWait", jamWait).put("night", night).put("underground", underground)
    .put("renown", renown).put("find", find).put("curio", curio).put("broken", JSONArray(broken)).put("weather", weather).put("outside", outside)
    .put("lastLat", lastLat).put("lastLon", lastLon).put("lastAt", lastAt)

fun tripOf(o: JSONObject) = Trip(o.getLong("start"), o.optLong("end"), o.optString("ride", "car"), o.optDouble("meters", 0.0),
    o.optJSONArray("quarters").strings(), o.optJSONArray("fresh").strings(), o.optInt("jamSec"), o.optInt("jamWait"), o.optBoolean("night"), o.optInt("underground"),
    o.optInt("renown"), o.str("find"), o.str("curio"), o.optJSONArray("broken").strings(), o.str("weather"), o.optBoolean("outside"), o.optDouble("lastLat", 0.0), o.optDouble("lastLon", 0.0), o.optLong("lastAt"))

fun Route.json() = JSONObject().put("a", a).put("b", b).put("trips", trips).put("built", JSONArray(built.toList()))
fun routeOf(o: JSONObject) = Route(o.str("a"), o.str("b"), o.optInt("trips"), o.optJSONArray("built").strings().toSet())

fun State.json(): JSONObject = JSONObject()
    .put("v", 1).put("onboarded", onboarded).put("homeLat", homeLat).put("homeLon", homeLon)
    .put("quarters", JSONArray(quarters.values.map { it.json() })).put("here", here)
    .put("gold", gold).put("renown", renown).put("packLevel", packLevel)
    .put("pack", JSONArray(pack.map { it.json() })).put("trips", JSONArray(trips.map { it.json() }))
    .put("trip", trip?.json() ?: JSONObject.NULL).put("deals", JSONArray(deals.map { it.json() }))
    .put("routes", JSONArray(routes.values.map { it.json() }))
    .put("day", day).put("traded", JSONObject(traded as Map<*, *>)).put("orders", JSONArray(orders.toList()))
    .put("nightUntil", nightUntil).put("told", JSONArray(told.toList())).put("theme", theme).put("voice", voice).put("ride", ride).put("nextId", nextId)
    .put("house", JSONArray(house.toList())).put("giftDay", giftDay)
    .put("curios", JSONArray(curios.toList())).put("mStep", JSONObject(mStep as Map<*, *>)).put("mSince", JSONObject(mSince as Map<*, *>))
    .put("contracts", JSONArray(contracts.map { it.json() })).put("encounter", encounter).put("encDay", encDay).put("encCount", encCount)
    .put("shops", JSONArray(shops.map { it.json() })).put("luck", luck).put("tut", tut).put("btAddr", btAddr).put("btName", btName).put("reach", reach).put("placesV", 2)

fun stateOf(o: JSONObject): State {
    val traded = o.optJSONObject("traded")?.let { t -> t.keys().asSequence().associateWith { t.getInt(it) } }.orEmpty()
    return State(
        onboarded = o.optBoolean("onboarded"), homeLat = o.optDouble("homeLat", 0.0), homeLon = o.optDouble("homeLon", 0.0),
        quarters = o.optJSONArray("quarters").list(::quarterOf).associateBy { it.id }, here = o.str("here"),
        gold = o.optInt("gold", 120), renown = o.optInt("renown"), packLevel = o.optInt("packLevel"),
        pack = o.optJSONArray("pack").list(::lotOf), trips = o.optJSONArray("trips").list(::tripOf),
        trip = o.optJSONObject("trip")?.let(::tripOf), deals = o.optJSONArray("deals").list(::dealOf),
        routes = o.optJSONArray("routes").list(::routeOf).associateBy { it.key },
        day = o.optLong("day"), traded = traded, orders = o.optJSONArray("orders").strings().toSet(),
        nightUntil = o.optLong("nightUntil"), told = o.optJSONArray("told").strings().toSet(), theme = o.str("theme"),
        voice = o.optBoolean("voice"), ride = o.optString("ride", "car"), nextId = o.optLong("nextId", 1),
        house = o.optJSONArray("house").strings().toSet(), giftDay = o.optLong("giftDay"),
        curios = o.optJSONArray("curios").strings().toSet(),
        mStep = o.optJSONObject("mStep")?.let { m -> m.keys().asSequence().associateWith { m.getInt(it) } }.orEmpty(),
        mSince = o.optJSONObject("mSince")?.let { m -> m.keys().asSequence().associateWith { m.getLong(it) } }.orEmpty(),
        contracts = o.optJSONArray("contracts").list(::contractOf), encounter = o.str("encounter"), encDay = o.optLong("encDay"), encCount = o.optInt("encCount"),
        shops = o.optJSONArray("shops").list(::shopOf), luck = o.optInt("luck"), tut = o.optInt("tut"), btAddr = o.str("btAddr"), btName = o.str("btName"), reach = if (o.optInt("placesV", 1) < 2 || (o.optJSONArray("quarters")?.length() ?: 0) == 0) 0 else o.optInt("reach", if (o.optJSONArray("quarters")?.length() ?: 0 > 0) 9000 else 0),
    )
}

fun app.caravan.domain.Contract.json() = JSONObject().put("id", id).put("good", good).put("n", n).put("where", where.id).put("until", until)
    .put("deposit", deposit).put("reward", reward).put("since", since).put("done", done)
fun contractOf(o: JSONObject) = app.caravan.domain.Contract(o.getLong("id"), o.str("good"), o.optInt("n"), Kind.of(o.str("where")) ?: Kind.BAZAAR,
    o.optLong("until"), o.optInt("deposit"), o.optInt("reward"), o.optLong("since"), o.optInt("done"))

fun app.caravan.domain.Shop.json() = JSONObject().put("quarter", quarter).put("stock", JSONArray(stock.map { it.json() })).put("till", till).put("tick", tick)
fun shopOf(o: JSONObject) = app.caravan.domain.Shop(o.str("quarter"), o.optJSONArray("stock").list(::lotOf), o.optInt("till"), o.optLong("tick"))
