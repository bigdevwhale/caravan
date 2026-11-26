package app.caravan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import app.caravan.container
import app.caravan.data.State
import app.caravan.domain.tr
import kotlinx.coroutines.delay

/** Which sheet is open over the tabs. */
sealed interface Over {
    data object Market : Over
    data object Pack : Over
    data object Rides : Over
    data object Day : Over
    data object Settings : Over
    class Quarter(val id: String) : Over
    class TripPage(val start: Long) : Over
    class RoutePage(val key: String) : Over
    class HousePart(val id: String) : Over
    class Merchant(val id: String) : Over
    data object Merchants : Over
    data object Contracts : Over
    data object Curios : Over
    data object Shop : Over
    data object Encounter : Over
    class Folk(val quarter: String) : Over
    data object FolkList : Over
    /** By metro: the entrance or the exit station. */
    class Station(val exit: Boolean) : Over
}

@Composable
fun Root(s: State) {
    val c = LocalContext.current.container
    var tab by remember { mutableIntStateOf(app.caravan.data.Demo.tab.coerceAtLeast(0)) }
    // screenshot builds: the capture script names the sheet to open
    var over by remember { mutableStateOf<Over?>(app.caravan.data.Demo.open?.let { o ->
        when {
            o == "market" -> Over.Market
            o == "pack" -> Over.Pack
            o == "rides" -> Over.Rides
            o == "day" -> Over.Day
            o == "curios" -> Over.Curios
            o == "contracts" -> Over.Contracts
            o == "shop" -> Over.Shop
            o == "merchants" -> Over.Merchants
            o == "folk" -> Over.FolkList
            o.startsWith("folk:") -> Over.Folk(o.substringAfter(':'))
            o.startsWith("merchant:") -> Over.Merchant(o.substringAfter(':'))
            o.startsWith("quarter:") -> Over.Quarter(o.substringAfter(':'))
            o.startsWith("part:") -> Over.HousePart(o.substringAfter(':'))
            o == "trip" -> s.trips.firstOrNull()?.let { Over.TripPage(it.start) }
            else -> null
        }
    }) }
    val finished by c.game.finished.collectAsState()
    val live by c.game.live.collectAsState()
    val locked = s.trip?.let { app.caravan.domain.Ride.of(it.ride).driver } == true && live.moving
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.game.news.collect { toast = it.text } }
    // the metro's stations are loaded ahead, while there is a network and time: underground there may be neither
    LaunchedEffect(s.homeLat) { if (s.homeLat != 0.0) c.game.stations() }
    LaunchedEffect(toast) { if (toast != null) { delay(2600); toast = null } }
    LaunchedEffect(finished) { finished?.let { over = Over.TripPage(it.start); c.game.finished.value = null } }
    // a road meeting waits until the ledger page is closed and the driver has stopped
    LaunchedEffect(over, s.encounter, locked) { if (over == null && s.encounter.isNotEmpty() && !locked) over = Over.Encounter }
    BackHandler(tab != 0 && over == null) { tab = 0 }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> Home(s) { over = it }
                    1 -> HouseScreen(s) { over = it }
                    2 -> CityMap(s) { over = it }
                    3 -> Ledger(s) { over = it }
                    else -> GuildScreen(s) { over = it }
                }
            }
            // a driver in motion sees only the road card; the tabs wait for the stop
            if (!locked) Tabs(tab) { tab = it }
        }
        toast?.let {
            Text(it, style = T.smallB.copy(color = C.accInk), textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(12.dp)).background(C.indigo).padding(horizontal = 14.dp, vertical = 8.dp))
        }
        when (val o = over) {
            null -> {}
            Over.Market -> MarketSheet(s) { over = null }
            Over.Pack -> PackSheet(s) { over = null }
            Over.Rides -> RidesSheet(s) { over = null }
            Over.Day -> DaySheet(s, onQuarter = { over = Over.Quarter(it) }) { over = null }
            Over.Settings -> SettingsSheet(s) { over = null }
            is Over.Quarter -> QuarterSheet(s, o.id, onFolk = { over = Over.Folk(it) }) { over = null }
            is Over.Folk -> FolkSheet(s, o.quarter) { over = null }
            Over.FolkList -> FolkListSheet(s, onOpen = { over = Over.Folk(it) }) { over = null }
            is Over.TripPage -> TripSheet(s, o.start) { over = null }
            is Over.RoutePage -> RouteSheet(s, o.key) { over = null }
            is Over.HousePart -> HousePartSheet(s, o.id) { over = null }
            is Over.Merchant -> MerchantSheet(s, o.id) { over = null }
            Over.Merchants -> MerchantsSheet(s, onOpen = { over = Over.Merchant(it) }) { over = null }
            Over.Contracts -> ContractsSheet(s) { over = null }
            Over.Curios -> CuriosSheet(s) { over = null }
            Over.Shop -> ShopSheet(s) { over = null }
            Over.Encounter -> EncounterSheet(s) { over = null }
            is Over.Station -> StationSheet(s, o.exit) { over = null }
        }
        // each screen introduces itself once; «?» brings it back
        if (!locked) introFor(tab, over, s.trip != null)?.let { i ->
            if (Help.open || i.id !in s.told) IntroSheet(i) { Help.open = false; c.game.write { it.copy(told = it.told + i.id) } }
        }
    }
}

@Composable
private fun Tabs(sel: Int, onSel: (Int) -> Unit) {
    val items = listOf("tab_gate" to tr("Ворота", "Gate"), "tab_house" to tr("Дом", "House"), "tab_map" to tr("Карта", "Map"), "tab_ledger" to tr("Гроссбух", "Ledger"), "tab_guild" to tr("Гильдия", "Guild"))
    val band = if (C.dark) Color(0xFF0F1426) else Color(0xFF283A6E)
    val gold = Color(0xFFE3AD48)
    Box(Modifier.fillMaxWidth().background(band).navigationBarsPadding()) {
        // a gold border of little arches along the top, like a tiled frieze
        Box(Modifier.fillMaxWidth().height(10.dp).drawBehind {
            val w = 14.dp.toPx(); var x = 0f
            drawRect(gold, size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
            while (x < size.width) {
                drawArc(gold, 0f, 180f, false, topLeft = Offset(x, -w / 2 + 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(w, w), style = Stroke(1.2.dp.toPx()))
                drawCircle(gold, 1.6.dp.toPx(), Offset(x + w / 2, w / 2 + 3.dp.toPx()))
                x += w
            }
        })
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(66.dp)) {
            items.forEachIndexed { i, (pic, label) ->
                val on = i == sel
                Column(Modifier.weight(1f).fillMaxSize().clickable { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(width = 54.dp, height = 40.dp)
                        .then(if (on) Modifier.clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp, bottomStart = 8.dp, bottomEnd = 8.dp)).background(Color(0xFFFBF3E1)).border(1.5.dp, gold, RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp, bottomStart = 8.dp, bottomEnd = 8.dp)) else Modifier),
                        contentAlignment = Alignment.Center) {
                        Pic(pic, Modifier.size(if (on) 34.dp else 32.dp), alpha = if (on) 1f else 0.72f)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(label, style = T.tiny.copy(color = if (on) gold else Color(0xFFCFC6B4), fontFamily = Phil, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 12.sp))
                }
            }
        }
    }
}
