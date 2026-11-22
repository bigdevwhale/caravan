package app.caravan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.caravan.container
import app.caravan.data.Route
import app.caravan.data.State
import app.caravan.domain.Guild
import app.caravan.domain.Orders
import app.caravan.domain.Ride
import app.caravan.domain.dayOf
import app.caravan.domain.fmt
import app.caravan.domain.fmt1
import app.caravan.domain.relDay
import app.caravan.domain.today
import app.caravan.domain.tr
import kotlinx.coroutines.launch

@Composable
private fun Banner(pic: String, title: String, sub: String, action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().height(150.dp)) {
        Pic(pic, Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(C.bg.copy(alpha = 0.95f), C.bg.copy(alpha = 0.6f), Color.Transparent))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(title, style = T.big)
            Text(sub, style = T.small.copy(color = C.ink))
        }
        if (action != null) Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(10.dp)) { action() }
    }
}

/** The ledger: totals and a page for every trip. */
@Composable
fun Ledger(s: State, open: (Over) -> Unit) {
    val sold = s.deals.filter { !it.buy }
    val profit = sold.sumOf { it.price - it.paid }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Banner("banner_ledger", tr("Гроссбух", "The ledger"), tr("Поездки, сделки и прибыль", "Trips, deals and profit")) { HelpBtn() }
            Row(Modifier.fillMaxWidth().padding(12.dp).framed().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Fig("${s.trips.size}", tr("поездок", "trips"))
                Fig(fmt1(s.trips.sumOf { it.km }), tr("км", "km"))
                Fig("${s.quarters.values.count { it.known }}", tr("кварталов", "quarters"))
                Fig((if (profit >= 0) "+" else "") + fmt(profit), tr("прибыль", "profit"))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).framed().clickable { open(Over.Curios) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic("cur_lamp", Modifier.size(44.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Книга диковин", "The book of curios"), style = T.h3)
                    Text(tr("найдено ${s.curios.size} из ${app.caravan.domain.Curios.all.size} – их приносят поездки", "${s.curios.size} of ${app.caravan.domain.Curios.all.size} found – rides bring them"), style = T.small)
                }
                Text("›", style = T.title.copy(color = C.mute))
            }
            Spacer(Modifier.height(6.dp))
        }
        if (s.trips.isEmpty()) item {
            Text(tr("Пока пусто. Нажмите «В путь» на воротах перед следующей поездкой – и здесь появится первая страница.",
                "Empty so far. Press «Set off» at the gate before your next ride and the first page appears here."), style = T.body, modifier = Modifier.padding(20.dp))
        }
        items(s.trips, key = { it.start }) { t ->
            val deals = s.deals.filter { it.at in t.start..(t.end + 60_000) }
            val take = deals.filter { !it.buy }.sumOf { it.price }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).framed(12.dp, fill = C.paper).clickable { open(Over.TripPage(t.start)) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Pic(Ride.of(t.ride).pic, Modifier.size(width = 56.dp, height = 40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(s.quarters[t.from]?.name, s.quarters[t.to]?.name.takeIf { t.to != t.from }).joinToString(" → ").ifEmpty { tr("Поездка", "A trip") },
                        style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(relDay(dayOf(t.start)).replaceFirstChar { it.uppercase() } + " · " + fmt1(t.km) + tr(" км", " km") +
                        (if (t.fresh.isNotEmpty()) tr(" · новых: ${t.fresh.size}", " · new: ${t.fresh.size}") else ""), style = T.small)
                }
                if (take > 0) Text("+" + fmt(take), style = T.bodyB.copy(color = C.ok))
                else if (t.find.isNotEmpty()) Pic(app.caravan.domain.Goods[t.find].pic, Modifier.size(28.dp))
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun Fig(v: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(v, style = T.num); Text(label, style = T.tiny) }
}

/** The guild: rank, the day's orders, the routes and what is built on them. */
@Composable
fun GuildScreen(s: State, open: (Over) -> Unit) {
    val rank = Guild.rank(s.renown)
    val next = Guild.next(s.renown)
    val day = today()
    val orders = Orders.of(s, day)
    val routes = s.routes.values.sortedByDescending { it.trips }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Banner("banner_guild", tr("Гильдия", "The guild"), rank.title) {
                Row { HelpBtn(); Spacer(Modifier.width(8.dp)); Box(Modifier.clip(CircleShape).background(C.paper.copy(alpha = 0.9f)).clickable { open(Over.Settings) }.padding(8.dp)) {
                    Icon(Icons.Outlined.Settings, null, tint = C.ink)
                } }
            }
            Column(Modifier.fillMaxWidth().padding(12.dp).framed().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pic("ic_seal", Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Слава: ${fmt(s.renown)}", "Renown: ${fmt(s.renown)}"), style = T.h3)
                        Text(next?.let { tr("до звания «${it.ru}» – ${fmt(it.at - s.renown)}", "${fmt(it.at - s.renown)} to «${it.en}»") } ?: tr("Высшее звание гильдии", "The guild's highest rank"), style = T.small)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Meter(next?.let { (s.renown - rank.at).toFloat() / (it.at - rank.at) } ?: 1f)
                Spacer(Modifier.height(6.dp))
                Text(tr("Слава – за новые кварталы, поездки, выгодные сделки, поручения и постройки.", "Renown comes from new quarters, trips, good deals, orders and buildings."), style = T.tiny)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).framed().clickable { open(Over.Day) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic("ic_scroll", Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Поручения и слухи", "Orders and rumours"), style = T.h3)
                    Text(tr("Сегодня выполнено ${orders.count { Orders.key(day, it) in s.orders }} из ${orders.size}", "Done today: ${orders.count { Orders.key(day, it) in s.orders }} of ${orders.size}"), style = T.small)
                }
                Text("›", style = T.title.copy(color = C.mute))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).framed().clickable { open(Over.Merchants) }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Pic("mer_bazaar", Modifier.size(48.dp))
                    Text(tr("Купцы", "Merchants"), style = T.h3)
                    Text(tr("друзей: ", "friends: ") + app.caravan.domain.Merchants.all.count { app.caravan.domain.Merchants.friend(s, it) } + "/8", style = T.tiny)
                }
                if (app.caravan.domain.Tutorial.finished(s)) Column(Modifier.weight(1f).framed().clickable { open(Over.Contracts) }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Pic("ic_scroll", Modifier.size(48.dp))
                    Text(tr("Контракты", "Contracts"), style = T.h3)
                    val n = s.contracts.count { it.taken }
                    // «2/3» read as «there should be three» (#11): taken, and how many more the guild gives
                    Text(if (n == 0) tr("можно взять до трёх", "up to three at once") else if (n >= 3) tr("взято 3 – больше нельзя", "3 taken – the most") else tr("взято $n · можно ещё ${3 - n}", "$n taken · ${3 - n} more possible"), style = T.tiny)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(tr("Торговые пути", "Trade routes"), style = T.title, modifier = Modifier.padding(horizontal = 16.dp))
            Text(tr("Путь появляется, когда вы едете из одного квартала в другой. Чем чаще – тем он крепче, и на нём можно строить.",
                "A route appears when you ride from one quarter to another. The more often, the stronger it grows, and you can build on it."), style = T.small, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(6.dp))
        }
        if (routes.isEmpty()) item { Text(tr("Пока ни одного пути.", "No routes yet."), style = T.body, modifier = Modifier.padding(16.dp)) }
        items(routes, key = { it.key }) { r -> RouteRow(s, r) { open(Over.RoutePage(r.key)) } }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun RouteRow(s: State, r: Route, onClick: () -> Unit) {
    val lv = Guild.level(r)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).framed(12.dp).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Pic(s.quarters[r.a]?.kind?.crest ?: "crest_night", Modifier.size(30.dp))
        Pic(s.quarters[r.b]?.kind?.crest ?: "crest_night", Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("${s.quarters[r.a]?.name} – ${s.quarters[r.b]?.name}", style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Guild.levels[lv].title + tr(" · поездок ${r.trips}", " · ${r.trips} trips"), style = T.small)
        }
        r.built.forEach { Pic(Guild.byId[it]?.pic ?: "", Modifier.size(26.dp)) }
    }
}

/** A route's page: its level and the buildings to put on it. */
@Composable
fun RouteSheet(s: State, key: String, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val r = s.routes[key] ?: return onDismiss()
    val lv = Guild.level(r)
    Sheet(onDismiss) {
        Title("${s.quarters[r.a]?.name} – ${s.quarters[r.b]?.name}", Guild.levels[lv].title + tr(" · поездок ${r.trips}", " · ${r.trips} trips"))
        Guild.levels.getOrNull(lv + 1)?.let { n ->
            Spacer(Modifier.height(6.dp))
            Meter((r.trips - Guild.levels[lv].at).toFloat() / (n.at - Guild.levels[lv].at))
            Text(tr("до «${n.ru}» – ещё ${n.at - r.trips} поездок", "${n.at - r.trips} more trips to «${n.en}»"), style = T.tiny)
        }
        Ornament()
        Guild.buildings.forEach { b ->
            val has = b.id in r.built
            GoodRow(b.pic, b.title, b.effect + if (!has && lv < b.needs) tr(" · нужен «${Guild.levels[b.needs].ru}»", " · needs «${Guild.levels[b.needs].en}»") else "") {
                when {
                    has -> Text(tr("стоит", "built"), style = T.smallB.copy(color = C.ok))
                    else -> Btn(fmt(b.price), small = true, enabled = lv >= b.needs && s.gold >= b.price) { scope.launch { c.game.build(key, b.id) } }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(tr("Постройки – главное, на что уходит золото: оно не должно лежать мёртвым грузом.", "Buildings are where gold goes: it shouldn't lie idle."), style = T.tiny)
    }
}
