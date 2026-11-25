package app.caravan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.caravan.Manifests
import app.caravan.container
import app.caravan.currentLanguage
import app.caravan.data.Lot
import app.caravan.data.State
import app.caravan.domain.Goods
import app.caravan.domain.Market
import app.caravan.domain.Orders
import app.caravan.domain.Ride
import app.caravan.domain.dur
import app.caravan.domain.fmt
import app.caravan.domain.fmt1
import app.caravan.domain.fmtDay
import app.caravan.domain.dayOf
import app.caravan.domain.today
import app.caravan.domain.tr
import app.caravan.setLanguage
import app.caravan.trip.Geo
import app.caravan.trip.TripService
import kotlinx.coroutines.launch

@Composable
fun GoodRow(pic: String, title: String, sub: String, modifier: Modifier = Modifier, lines: Int = 2, right: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(C.paper2), contentAlignment = Alignment.Center) { Pic(pic, Modifier.size(44.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bodyB, maxLines = lines, overflow = TextOverflow.Ellipsis)
            Text(sub, style = T.small, maxLines = lines + 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        right()
    }
}

/** The taken contract still waiting for [good], if any (#10): such goods stand out at the market. */
private fun contractFor(s: State, good: String) = s.contracts.firstOrNull { it.taken && it.good == good && app.caravan.domain.Contracts.progress(s, it) < it.n }

/** A gold wash behind a row a contract asks for. */
private fun Modifier.contracted() = this.clip(RoundedCornerShape(12.dp)).background(C.gold.copy(alpha = .16f)).padding(horizontal = 6.dp)

@Composable
private fun Tabs2(items: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.paper2).padding(3.dp)) {
        items.forEachIndexed { i, t ->
            Text(t, style = T.smallB.copy(color = if (i == sel) C.accInk else C.ink), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (i == sel) C.acc else Color.Transparent).clickable { onSel(i) }.padding(vertical = 9.dp))
        }
    }
}

/** The market of the quarter the merchant is in: buy its goods, sell from the pack; at night the lantern market too. */
@Composable
fun MarketSheet(s: State, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val live by c.game.live.collectAsState()
    val q = s.quarters[s.here]
    val night = Market.nightOpen(s)
    var tab by remember { mutableIntStateOf(if (s.pack.isNotEmpty()) 1 else 0) }
    val locked = s.trip?.let { Ride.of(it.ride).driver } == true && live.moving
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(q?.kind?.crest ?: "crest_night", Modifier.size(48.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Рынок: ", "Market: ") + (q?.name ?: "—"), style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(q?.kind?.title ?: tr("квартал ещё изучается", "the quarter is being learnt"), style = T.small)
            }
            Coins(s.gold)
            Spacer(Modifier.width(8.dp))
            HelpBtn()
        }
        Spacer(Modifier.height(12.dp))
        if (locked) {
            Text(tr("Торговля – на стоянке. Остановитесь, и рынок откроется.", "Trade waits for the stop. Pull over and the market opens."), style = T.body)
            return@Sheet
        }
        if (q == null) { Text(tr("Вы вне известных кварталов.", "You're outside the known quarters."), style = T.body); return@Sheet }
        val tabs = listOf(tr("Купить", "Buy"), tr("Продать (${s.pack.size})", "Sell (${s.pack.size})")) + if (night) listOf(tr("Ночной", "Night")) else emptyList()
        Tabs2(tabs, tab.coerceAtMost(tabs.lastIndex)) { tab = it }
        Spacer(Modifier.height(8.dp))
        val full = s.pack.size >= Market.capacity(s)
        when (tab) {
            0 -> {
                val offers = Market.sells(q, s)
                if (offers.isEmpty()) Text(tr("Лавки откроются, как только станет ясно, что это за квартал.", "The stalls open once it's clear what quarter this is."), style = T.body)
                offers.forEach { o ->
                    val k = contractFor(s, o.good.id)
                    GoodRow(o.good.pic, o.good.name, (k?.let { tr("📜 контракт: ${it.n} → «${it.where.ru}» · ", "📜 contract: ${it.n} → «${it.where.en}» · ") } ?: "") +
                        (if (o.note.isNotEmpty()) o.note + " · " else "") + if (o.stock > 0) tr("осталось ${o.stock} · обычная цена ${o.good.base}", "${o.stock} left · fair price ${o.good.base}") else tr("на сегодня распродано", "sold out today"),
                        if (k != null) Modifier.contracted() else Modifier) {
                        Btn(fmt(o.price), small = true, enabled = o.stock > 0 && s.gold >= o.price && !full, color = C.teal) {
                            scope.launch { c.game.buy(q.id, o.good.id) }
                        }
                    }
                }
                if (full) Text(tr("Вьюк полон – сначала продайте что-нибудь.", "The pack is full – sell something first."), style = T.small.copy(color = C.bad))
                Spacer(Modifier.height(6.dp))
                Text(tr("Дороже всего здесь берут: ", "Pays most here for: ") + q.kind?.wants?.joinToString(", ") { it.title.lowercase() }.orEmpty() +
                    (Market.craves(q)?.let { tr(", а особенно – ${it.ru.lowercase()}", ", above all ${it.en.lowercase()}") } ?: ""), style = T.small)
            }
            1 -> {
                if (s.pack.isEmpty()) Text(tr("Вьюк пуст. Купите товар в одном квартале и увезите в другой.", "The pack is empty. Buy in one quarter and carry it to another."), style = T.body)
                s.pack.sortedByDescending { Market.price(it, q, s) - it.paid }.forEach { lot ->
                    val g = Goods[lot.good]
                    val p = Market.price(lot, q, s)
                    val from = s.quarters[lot.origin]
                    val gain = p - lot.paid
                    val k = contractFor(s, lot.good)
                    val here = k != null && k.where == q.kind
                    GoodRow(g.pic, g.name, (k?.let { if (here) tr("📜 сдаётся здесь по контракту · ", "📜 counts for a contract here · ") else tr("📜 контракт: везти в «${it.where.ru}» · ", "📜 contract: take to «${it.where.en}» · ") } ?: "") +
                        (from?.let { tr("из ${it.ru}", "from ${it.en}") } ?: "") + " · " +
                        (if (lot.paid == 0) tr("находка", "a find") else tr("куплено за ${lot.paid}", "bought for ${lot.paid}")) + riskTag(lot),
                        if (k != null) Modifier.contracted() else Modifier, lines = 3) {
                        Column(horizontalAlignment = Alignment.End) {
                            Btn(fmt(p), small = true, color = if (gain > 0) C.acc else C.mute) { scope.launch { c.game.sell(q.id, lot.id) } }
                            Text((if (gain >= 0) "+" else "") + fmt(gain), style = T.tiny.copy(color = if (gain > 0) C.ok else C.bad))
                        }
                    }
                }
            }
            else -> {
                Text(tr("Под фонарями до утра – три редких товара. Каждый дороже всего в двух видах кварталов.", "Under the lanterns till morning – three rare goods. Each fetches most in two kinds of quarters."), style = T.small)
                Market.night(s).forEach { o ->
                    GoodRow(o.good.pic, o.good.name, tr("ценят: ", "valued in: ") + Goods.rareWanted(o.good).joinToString(", ") { it.title.lowercase() }) {
                        Btn(fmt(o.price), small = true, enabled = o.stock > 0 && s.gold >= o.price && !full, color = C.indigo, ink = Color(0xFFFFF8EC)) {
                            scope.launch { c.game.buyNight(o.good.id) }
                        }
                    }
                }
            }
        }
    }
}

/** The pack: every bale, where it's from and whether the steward may sell it on the way. */
@Composable
fun PackSheet(s: State, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    Sheet(onDismiss) {
        Title(tr("Вьюк", "The pack"), tr("${s.pack.size} из ${Market.capacity(s)} мест", "${s.pack.size} of ${Market.capacity(s)} places"))
        Spacer(Modifier.height(6.dp))
        Text(tr("Отметьте товар для приказчика – он продаст его в поездке, в том квартале по пути, где дадут больше всего (и только с прибылью).",
            "Mark goods for the steward – he'll sell them on a trip, in the quarter on the way that pays most (and only at a profit)."), style = T.small)
        Spacer(Modifier.height(8.dp))
        if (s.pack.isEmpty()) Text(tr("Пусто.", "Empty."), style = T.body)
        s.pack.forEach { lot: Lot ->
            val g = Goods[lot.good]
            val from = s.quarters[lot.origin]
            GoodRow(g.pic, g.name, (from?.name ?: "") + " · " + (if (lot.paid == 0) tr("находка", "a find") else tr("за ${lot.paid}", "for ${lot.paid}")) + riskTag(lot)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(lot.auto, { c.game.toggleAuto(lot.id) }, colors = SwitchDefaults.colors(checkedTrackColor = C.acc))
                    Text(tr("приказчик", "steward"), style = T.tiny)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val n = s.packLevel + 1
        if (n <= Market.packSizes.lastIndex) {
            Ornament()
            GoodRow("ride_${if (n >= 3) "caravan" else "chest"}", tr("Больше места: ${Market.packSizes[n]}", "More room: ${Market.packSizes[n]}"),
                tr("Новые тюки и короба для каравана", "New bales and chests for the caravan")) {
                Btn(fmt(Market.packPrices[n]), small = true, enabled = s.gold >= Market.packPrices[n]) { scope.launch { c.game.upgradePack() } }
            }
        }
    }
}

/** How the merchant rides this time. */
@Composable
fun RidesSheet(s: State, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    Sheet(onDismiss) {
        Title(tr("Как едем?", "How do we ride?"), tr("Поездка закончится сама, когда вы постоите на месте 20 минут. В метро GPS не нужен: хватит станций входа и выхода.",
            "The trip ends by itself after 20 still minutes. The metro needs no GPS: the stations in and out are enough."))
        Spacer(Modifier.height(10.dp))
        Ride.entries.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp))
                .background(if (r.id == s.ride) C.goldSoft else C.paper2)
                .border(1.dp, if (r.id == s.ride) C.gold else Color.Transparent, RoundedCornerShape(14.dp))
                .clickable {
                    Manifests.askLocation { if (c.feed.permitted()) { TripService.start(ctx, r); onDismiss() } }
                }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(r.pic, Modifier.size(width = 72.dp, height = 48.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = T.h3)
                    Text(r.sub, style = T.small)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Голос в пути", "Voice on the way"), style = T.bodyB)
                Text(tr("Скажет, в какой квартал вы въехали", "Says which quarter you enter"), style = T.small)
            }
            Switch(s.voice, { v -> c.game.write { it.copy(voice = v) } }, colors = SwitchDefaults.colors(checkedTrackColor = C.acc))
        }
        Ornament()
        CarPicker(s)
    }
}

/** The day: guild orders and the rumours. */
@Composable
fun DaySheet(s: State, onQuarter: (String) -> Unit, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val day = today()
    Sheet(onDismiss) {
        EventCard()
        Spacer(Modifier.height(12.dp))
        Title(tr("Поручения на сегодня", "Today's orders"), tr("Не сгорают со штрафом – завтра будут новые.", "No penalty – tomorrow brings new ones."))
        Spacer(Modifier.height(8.dp))
        Orders.of(s, day).forEach { o ->
            val claimed = Orders.key(day, o) in s.orders
            val done = o.done(s, day)
            GoodRow(if (claimed) "ic_seal" else "ic_scroll", o.text, tr("+${o.renown} славы", "+${o.renown} renown") + if (o.gold > 0) tr(" · +${o.gold} монет", " · +${o.gold} coins") else "", lines = 4) {
                when {
                    claimed -> Text(tr("готово", "done"), style = T.smallB.copy(color = C.ok))
                    done -> Btn(tr("Забрать", "Claim"), small = true) { scope.launch { c.game.claim(day, o.index) } }
                    else -> Text(tr("ещё нет", "not yet"), style = T.tiny)
                }
            }
        }
        Ornament()
        Text(tr("Слухи", "Rumours"), style = T.h3)
        Text(tr("Сегодня там платят вдвое за названный товар.", "Today they pay double there for the named good."), style = T.small)
        Spacer(Modifier.height(4.dp))
        val rs = Market.rumours(s, day)
        if (rs.isEmpty()) Text(tr("Слухи пойдут, когда вы узнаете хотя бы два квартала.", "Rumours start once you know at least two quarters."), style = T.body)
        rs.forEach { r ->
            GoodRow(r.good.pic, r.text, r.quarter.kind?.title.orEmpty(), Modifier.clickable { onQuarter(r.quarter.id) }, lines = 4) { Pic(r.quarter.kind?.crest ?: "crest_night", Modifier.size(36.dp)) }
        }
        Market.fairAt(s, day)?.let { f ->
            Ornament()
            GoodRow("ic_tent", tr("Ярмарка: ${f.ru}", "Fair: ${f.en}"), tr("По выходным – продажа на 30% дороже", "At weekends – sales 30% dearer")) {}
        }
        if (Market.nightOpen(s)) GoodRow("ic_lantern", tr("Ночной рынок открыт", "The night market is open"), tr("До шести утра – на любом рынке, вкладка «Ночной»", "Till six in the morning – in any market, the «Night» tab")) {}
    }
}

/** A quarter's page: what it is, what it makes, what it wants. */
@Composable
fun QuarterSheet(s: State, id: String, onDismiss: () -> Unit) {
    val q = s.quarters[id] ?: return onDismiss()
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(q.kind?.crest ?: "crest_night", Modifier.size(72.dp), alpha = if (q.known) 1f else 0.4f)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(q.name, style = T.title)
                Text(if (q.known) (q.kind!!.title + (q.second?.let { " · " + it.title.lowercase() } ?: "")) else tr("Неизведанный квартал", "An unexplored quarter"), style = T.small)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (!q.known) {
            Text(tr("Побывайте здесь – и станет ясно, чем торгует квартал. За первое посещение – 10 славы, а новые кварталы платят дороже.",
                "Come here and you'll learn what the quarter trades. The first visit gives 10 renown, and new quarters pay more."), style = T.body)
            val home = s.quarters[s.here]
            if (home != null) Text(tr("Отсюда около ${fmt1(Geo.distance(home.lat, home.lon, q.lat, q.lon) / 1000)} км", "About ${fmt1(Geo.distance(home.lat, home.lon, q.lat, q.lon) / 1000)} km from here"), style = T.small)
            return@Sheet
        }
        run {
            val c = LocalContext.current.container
            val off = Market.hidden(s, q.id)
            Text(if (off) tr("Не попадает в слухи · вернуть", "Left out of rumours · bring back") else tr("Не показывать в слухах", "Leave out of rumours"),
                style = T.smallB.copy(color = if (off) C.acc else C.mute),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { c.game.write { st -> st.copy(told = if (off) st.told - "hide:${q.id}" else st.told + "hide:${q.id}") } }.padding(vertical = 6.dp))
            Spacer(Modifier.height(6.dp))
        }
        Text(tr("Делает", "Makes"), style = T.h3)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp).horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
            Market.sells(q, s).forEach { o -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                Pic(o.good.pic, Modifier.size(52.dp)); Text(o.good.name, style = T.tiny, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (o.note.isNotEmpty()) Text(o.note, style = T.tiny.copy(color = C.acc), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            } }
        }
        Text(tr("Ждёт", "Wants"), style = T.h3)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            q.kind!!.wants.forEach { k -> Row(verticalAlignment = Alignment.CenterVertically) { Pic(k.crest, Modifier.size(32.dp)); Spacer(Modifier.width(4.dp)); Text(k.title, style = T.smallB) } }
        }
        Market.craves(q)?.let { g -> Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(g.pic, Modifier.size(32.dp)); Spacer(Modifier.width(4.dp)); Text(tr("Особенно – ${g.ru.lowercase()}: за него платят больше", "Above all ${g.en.lowercase()}: it pays extra"), style = T.small)
        } }
        if (q.guessed) Text(tr("Вид предварительный: сервер карт не ответил, уточним при следующем визите.", "The kind is provisional: the map server didn't answer; it will be checked on the next visit."), style = T.small.copy(color = C.acc))
        if (s.pack.isNotEmpty() && q.id != s.here) {
            Ornament()
            Text(tr("Сколько здесь дадут за ваш вьюк", "What your pack would fetch here"), style = T.h3)
            s.pack.sortedByDescending { Market.price(it, q, s) - it.paid }.take(6).forEach { lot ->
                val p = Market.price(lot, q, s)
                GoodRow(Goods[lot.good].pic, Goods[lot.good].name, tr("куплено за ${lot.paid}", "bought for ${lot.paid}")) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(fmt(p), style = T.bodyB)
                        Text((if (p - lot.paid >= 0) "+" else "") + fmt(p - lot.paid), style = T.tiny.copy(color = if (p > lot.paid) C.ok else C.bad))
                    }
                }
            }
            Text(tr("Цены на сегодня; к вашему приезду могут чуть сдвинуться.", "Today's prices; they may shift a little by the time you arrive."), style = T.tiny)
        }
        if (q.visits > 0) Text(tr("Побывали ${q.visits} раз · впервые ${fmtDay(q.firstDay)}", "Visited ${q.visits} times · first on ${fmtDay(q.firstDay)}"), style = T.small)
    }
}

/** A page of the ledger: one trip. */
@Composable
fun TripSheet(s: State, start: Long, onDismiss: () -> Unit) {
    val t = s.trips.firstOrNull { it.start == start } ?: return onDismiss()
    val deals = s.deals.filter { it.at in t.start..(t.end + 60_000) }
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(Ride.of(t.ride).pic, Modifier.size(width = 80.dp, height = 56.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Страница гроссбуха", "A ledger page"), style = T.title)
                Text(fmtDay(dayOf(t.start)) + " · " + Ride.of(t.ride).title, style = T.small)
            }
        }
        Ornament()
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Stat(fmt1(t.km), tr("км", "km")); Stat("${t.quarters.distinct().size}", tr("кварталов", "quarters")); Stat("+${t.renown}", tr("славы", "renown"))
        }
        Spacer(Modifier.height(8.dp))
        Text(t.quarters.mapNotNull { s.quarters[it]?.name }.joinToString(" → "), style = T.body)
        if (t.fresh.isNotEmpty()) Line("ic_key", tr("Новые кварталы: ", "New quarters: ") + t.fresh.mapNotNull { s.quarters[it]?.name }.joinToString(", "))
        if (t.curio.isNotEmpty()) app.caravan.domain.Curios.byId[t.curio]?.let { cu -> Line(cu.pic, tr("Диковина: ", "A curio: ") + cu.name) }
        if (t.broken.isNotEmpty()) Line(Goods[t.broken.first()].pic, tr("Разбилось в тряске: ", "Broke in the jolting: ") + Goods[t.broken.first()].name.lowercase())
        if (t.weather.isNotEmpty()) Line("cur_raindrop", tr("Погода в пути: ", "Weather on the way: ") + app.caravan.domain.Weather.title(t.weather))
        if (t.find.isNotEmpty()) Line(Goods[t.find].pic, tr("Находка на обочине: ", "Found by the roadside: ") + Goods[t.find].name)
        if (t.jamSec >= 120) Line("ic_hourglass", tr("В заторах: ${dur(t.jamSec)} – терпение купца тоже товар", "In jams: ${dur(t.jamSec)} – a merchant's patience is a good too"))
        if (t.underground > 0) Line("ride_metro", tr("Гномьими ходами: ${t.underground}", "Through dwarf tunnels: ${t.underground}"))
        if (t.night) Line("ic_lantern", tr("Ночная поездка – открыт ночной рынок", "A night ride – the night market is open"))
        app.caravan.domain.Guild.routeOf(s, t)?.let { r ->
            Line("ic_sign", tr("Путь ${s.quarters[r.a]?.ru} – ${s.quarters[r.b]?.ru}: ", "Route ${s.quarters[r.a]?.en} – ${s.quarters[r.b]?.en}: ") +
                app.caravan.domain.Guild.levels[app.caravan.domain.Guild.level(r)].title.lowercase() + tr(", поездок ${r.trips}", ", ${r.trips} trips"))
        }
        if (deals.isNotEmpty()) {
            Ornament()
            deals.forEach { d ->
                GoodRow(Goods[d.good].pic, Goods[d.good].name, (if (d.buy) tr("куплено", "bought") else tr("продано", "sold")) + " · " + (s.quarters[d.quarter]?.name ?: tr("ночной рынок", "night market"))) {
                    Text((if (d.buy) "−" else "+") + fmt(d.price), style = T.bodyB.copy(color = if (d.buy) C.mute else C.ok))
                }
            }
        }
    }
}

@Composable
private fun Stat(v: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(v, style = T.big); Text(label, style = T.small) }
}

@Composable
private fun Line(pic: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Pic(pic, Modifier.size(32.dp)); Spacer(Modifier.width(8.dp)); Text(text, style = T.body)
    }
}

@Composable
fun SettingsSheet(s: State, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var sure by remember { mutableIntStateOf(0) }
    Sheet(onDismiss) {
        Title(tr("Настройки", "Settings"))
        Spacer(Modifier.height(10.dp))
        Text(tr("Тема", "Theme"), style = T.h3)
        Tabs2(listOf(tr("Как в системе", "System"), tr("Светлая", "Light"), tr("Тёмная", "Dark")), listOf("", "light", "dark").indexOf(s.theme).coerceAtLeast(0)) { i ->
            c.game.write { it.copy(theme = listOf("", "light", "dark")[i]) }
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("Язык", "Language"), style = T.h3)
        val lang = currentLanguage()
        Tabs2(listOf(tr("Как в системе", "System"), "Русский", "English"), listOf("", "ru", "en").indexOf(lang).coerceAtLeast(0)) { i -> setLanguage(listOf("", "ru", "en")[i]) }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Голос в пути", "Voice on the way"), style = T.bodyB, modifier = Modifier.weight(1f))
            Switch(s.voice, { v -> c.game.write { it.copy(voice = v) } }, colors = SwitchDefaults.colors(checkedTrackColor = C.acc))
        }
        Spacer(Modifier.height(12.dp))
        CarPicker(s)
        Spacer(Modifier.height(12.dp))
        Btn(tr("Обновить кварталы вокруг", "Reload the quarters around"), Modifier.fillMaxWidth(), color = C.paper2, ink = C.ink, small = true) {
            scope.launch {
                val f = c.game.live.value.fix ?: c.feed.last()
                if (f != null) c.game.settle(f.lat, f.lon)
            }
        }
        Spacer(Modifier.height(8.dp))
        Btn(if (sure == 0) tr("Начать заново", "Start over") else tr("Точно? Всё будет стёрто", "Sure? Everything will be erased"), Modifier.fillMaxWidth(), color = if (sure == 0) C.paper2 else C.bad, ink = if (sure == 0) C.bad else C.accInk, small = true) {
            if (sure == 0) sure = 1 else scope.launch { c.game.reset(); onDismiss() }
        }
        Spacer(Modifier.height(10.dp))
        Text(tr("Кварталы и карта – © участники OpenStreetMap. Караван ничего не отправляет: всё хранится на телефоне.",
            "Quarters and map – © OpenStreetMap contributors. Caravan sends nothing anywhere: everything stays on the phone."), style = T.tiny)
    }
}

/** «портится» / «хрупкое» after a bale's line. */
fun riskTag(lot: app.caravan.data.Lot): String = when (lot.good) {
    in app.caravan.domain.Risk.perishable -> app.caravan.domain.Risk.freshness(lot).let { f -> if (f < 1.0) tr(" · подпортилось (${(f * 100).toInt()}%)", " · going off (${(f * 100).toInt()}%)") else tr(" · портится", " · perishable") }
    in app.caravan.domain.Risk.fragile -> tr(" · хрупкое", " · fragile")
    else -> ""
}

/** Which paired Bluetooth device is the car: when it connects, Caravan offers the ride; when it goes, the ride ends. */
@Composable
fun CarPicker(s: State) {
    val ctx = LocalContext.current
    val c = ctx.container
    var list by remember { androidx.compose.runtime.mutableStateOf<List<Pair<String, String>>?>(null) }
    Text(tr("Машина по Bluetooth", "The car by Bluetooth"), style = T.h3)
    Text(tr("Когда телефон подключится к машине, придёт уведомление «Караван готов» – одно нажатие, и поездка идёт. Заглушили мотор – поездка закончилась сама.",
        "When the phone connects to the car a «Caravan is ready» notification comes – one tap and the ride is on. Engine off – the ride ends by itself."), style = T.small)
    Spacer(Modifier.height(6.dp))
    if (s.btAddr.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic("ride_car", Modifier.size(width = 54.dp, height = 36.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.btName, style = T.bodyB, modifier = Modifier.weight(1f))
            Ghost(tr("Убрать", "Remove"), color = C.bad) { c.game.write { it.copy(btAddr = "", btName = "") } }
        }
    } else if (list == null) {
        Btn(tr("Выбрать машину", "Choose the car"), Modifier.fillMaxWidth(), color = C.paper2, ink = C.ink, small = true) {
            val ask = if (android.os.Build.VERSION.SDK_INT >= 31) listOf(android.Manifest.permission.BLUETOOTH_CONNECT) else emptyList()
            app.caravan.MainActivity.ask(ask) { list = app.caravan.trip.Auto.paired(ctx) }
        }
    } else {
        if (list!!.isEmpty()) Text(tr("Нет сопряжённых устройств (или Bluetooth не разрешён). Сначала подключите телефон к машине в настройках Bluetooth.",
            "No paired devices (or Bluetooth isn't allowed). Pair the phone with the car in Bluetooth settings first."), style = T.small.copy(color = C.bad))
        list!!.forEach { (addr, name) ->
            Text(name, style = T.bodyB, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { c.game.write { it.copy(btAddr = addr, btName = name) }; list = null }.padding(vertical = 10.dp, horizontal = 6.dp))
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(tr("Для автобуса и метро: добавьте плитку «Караван: в путь» в шторку быстрых настроек – старт одним касанием, не открывая приложение.",
        "For bus and underground: add the «Caravan: set off» tile to quick settings – one touch to start, without opening the app."), style = T.tiny)
}
