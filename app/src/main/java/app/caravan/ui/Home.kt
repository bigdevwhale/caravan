package app.caravan.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.caravan.BuildConfig
import app.caravan.container
import app.caravan.data.State
import app.caravan.domain.Guild
import app.caravan.domain.Market
import app.caravan.domain.Orders
import app.caravan.domain.Ride
import app.caravan.domain.fmt
import app.caravan.domain.fmt1
import app.caravan.domain.tr
import app.caravan.trip.TripService
import java.time.LocalTime

/** The gate of the quarter the merchant is in: one screen – the scene, the purse, the market and the road. */
@Composable
fun Home(s: State, open: (Over) -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val live by c.game.live.collectAsState()
    val trip = s.trip
    val ride = Ride.of(trip?.ride ?: s.ride)
    if (trip != null && ride.driver && live.moving) return RoadCard(s)

    val q = s.quarters[s.here]
    val hour = LocalTime.now().hour
    val night = !BuildConfig.SHOTS && (hour >= 20 || hour < 6)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Pic(if (night) "gate_night" else "gate", Modifier.fillMaxSize(), ContentScale.Crop)
            // the cart on the square, swaying a little when on the way
            val sway = rememberInfiniteTransition(label = "sway")
            val dy by sway.animateFloat(0f, if (trip != null && !BuildConfig.SHOTS) 3f else 0f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dy")
            Pic(ride.pic, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).size(150.dp).offset(y = dy.dp))
            // the quarter's merchant stands by the gate
            q?.kind?.let { k -> app.caravan.domain.Merchants.byId[k.id] }?.let { m ->
                val ready = app.caravan.domain.Merchants.ready(s, m)
                val fresh = s.mSince[m.id] == null && !app.caravan.domain.Merchants.friend(s, m)
                Box(Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 8.dp).tap { open(Over.Merchant(m.id)) }) {
                    Pic(m.pic, Modifier.size(150.dp))
                    if (ready || fresh) Text(if (ready) "!" else "…", style = T.title.copy(color = C.accInk),
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(C.acc).padding(horizontal = 11.dp, vertical = 1.dp))
                }
            }
            // a stall here, or the chance to open one
            if (q?.kind != null && app.caravan.domain.Tutorial.finished(s)) {
                val sh = s.shops.firstOrNull { it.quarter == q.id }
                Pill("bld_stall", if (sh == null) tr("Лавка", "Stall") else if (sh.till > 0) "+" + fmt(sh.till) else "${sh.stock.size}/${app.caravan.domain.Shops.SLOTS}",
                    Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 16.dp)) { open(Over.Shop) }
            }
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent))).statusBarsPadding().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("ic_purse", fmt(s.gold))
                    Spacer(Modifier.width(8.dp))
                    Pill("ic_seal", fmt(s.renown))
                    Spacer(Modifier.weight(1f))
                    Pill("ic_bundle", "${s.pack.size}/${Market.capacity(s)}") { open(Over.Pack) }
                    Spacer(Modifier.width(8.dp))
                    HelpBtn()
                }
                Spacer(Modifier.height(12.dp))
                Plaque(s, Modifier.align(Alignment.CenterHorizontally)) { q?.let { open(Over.Quarter(it.id)) } }
            }
        }
        Column(Modifier.fillMaxWidth().padding(12.dp).framed().padding(14.dp)) {
            if (trip != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pic(ride.pic, Modifier.size(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("В пути: ${fmt1(trip.km)} км · кварталов ${trip.quarters.distinct().size}", "On the way: ${fmt1(trip.km)} km · ${trip.quarters.distinct().size} quarters"), style = T.bodyB, modifier = Modifier.weight(1f))
                }
                Text(if (ride.driver) tr("Пока едете, экран отдыхает. Остановитесь – и рынок откроется.", "While you drive the screen rests. Stop and the market opens.")
                else tr("Пассажиром можно торговать прямо в пути.", "As a passenger you can trade on the way."), style = T.small)
            } else if (!app.caravan.domain.Tutorial.finished(s)) {
                FirstSteps(s)
            } else {
                DayLine(s) { open(Over.Day) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(tr("Рынок", "Market"), Modifier.weight(1f), color = C.indigo, ink = Color(0xFFFFF8EC), icon = Icons.Outlined.Storefront, enabled = q != null) { open(Over.Market) }
                if (trip == null) Btn(tr("В путь", "Set off"), Modifier.weight(1f), icon = Icons.Outlined.Route) { open(Over.Rides) }
                else Btn(tr("Приехали", "Arrived"), Modifier.weight(1f), icon = Icons.Outlined.Flag) { TripService.stop(ctx) }
            }
        }
    }
}

/** The quarter's crest and name over the arch. */
@Composable
private fun Plaque(s: State, modifier: Modifier, onClick: () -> Unit) {
    val q = s.quarters[s.here]
    Row(modifier.framed(16.dp).tap(onClick).padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Pic(q?.kind?.crest ?: "crest_night", Modifier.size(52.dp), alpha = if (q?.kind == null) 0.45f else 1f)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(q?.name ?: if (s.quarters.isEmpty()) tr("Кварталы грузятся", "Loading quarters") else tr("За городскими воротами", "Beyond the city gates"), style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(when {
                q == null && s.quarters.isEmpty() -> tr("Сервер карт занят – попробуем ещё раз сами", "The map server is busy – we'll try again by ourselves")
                q == null -> tr("Здесь нет известных кварталов", "No known quarters here")
                q.kind == null -> tr("Квартал изучается…", "Learning the quarter…")
                else -> q.kind.title + (q.second?.let { " · " + it.title.lowercase() } ?: "")
            }, style = T.small)
        }
    }
}

/** One line of the day: the orders done and the first rumour. */
@Composable
private fun DayLine(s: State, onClick: () -> Unit) {
    val day = app.caravan.domain.today()
    val orders = Orders.of(s, day)
    val done = orders.count { Orders.key(day, it) in s.orders }
    val rumour = Market.rumours(s, day).firstOrNull()
    Row(Modifier.fillMaxWidth().tap(onClick), verticalAlignment = Alignment.CenterVertically) {
        Pic("ic_scroll", Modifier.size(40.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Поручения гильдии: $done из ${orders.size}", "Guild orders: $done of ${orders.size}"), style = T.bodyB)
            Text(rumour?.text ?: (if (Market.nightOpen(s)) tr("Ночной рынок открыт до утра", "The night market is open till morning")
                else tr("Слухи появятся, когда узнаете пару кварталов", "Rumours come once you know a couple of quarters")), style = T.small, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", style = T.title.copy(color = C.mute))
    }
}

/** For a driver in motion: big, calm, nothing to tap. */
@Composable
private fun RoadCard(s: State) {
    val t = s.trip ?: return
    val q = s.quarters[s.here]
    Box(Modifier.fillMaxSize().background(Color(0xFF141A2E)).statusBarsPadding().padding(24.dp)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Pic(q?.kind?.crest ?: "crest_night", Modifier.size(130.dp))
            Spacer(Modifier.height(16.dp))
            Text(q?.name ?: tr("В дороге", "On the road"), style = T.big.copy(color = Color(0xFFF0E4CC)), textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Text(fmt1(t.km), style = T.huge.copy(color = Color(0xFFE3AD48)))
            Text(tr("км в пути · кварталов ${t.quarters.distinct().size}", "km on the way · ${t.quarters.distinct().size} quarters"), style = T.body.copy(color = Color(0xFFA8A08E)))
        }
        Text(tr("Смотрите на дорогу. Рынок, находки и гроссбух ждут на стоянке.", "Eyes on the road. The market, finds and the ledger wait for the stop."),
            style = T.body.copy(color = Color(0xFFA8A08E)), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}

/** The next of the six first steps, in place of the day's line until all are done. */
@Composable
private fun FirstSteps(s: State) {
    val st = app.caravan.domain.Tutorial.current(s) ?: return
    val n = app.caravan.domain.Tutorial.steps.size
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Первые шаги · ${s.tut + 1} из $n", "First steps · ${s.tut + 1} of $n"), style = T.tiny.copy(color = C.acc), modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(n) { i -> Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(if (i < s.tut) C.gold else if (i == s.tut) C.acc else C.line)) }
            }
        }
        Text(st.text + if (st.gold > 0) tr(" · +${st.gold} монет", " · +${st.gold} coins") else "", style = T.bodyB)
        Text(st.how, style = T.small)
    }
}
