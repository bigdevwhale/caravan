package app.caravan.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.caravan.MainActivity
import app.caravan.container
import app.caravan.domain.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Three pages: the city as a capital of quarters, how trade works, and finding the merchant's own quarter. */
@Composable
fun Onboarding() {
    var page by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Pic(if (page == 1) "prologue_trade" else "prologue_city", Modifier.fillMaxSize(), ContentScale.Crop)
        }
        Column(
            Modifier.fillMaxWidth().padding(14.dp).framed().padding(20.dp).navigationBarsPadding(),
        ) {
            when (page) {
                0 -> {
                    Text(tr("Ваш город – волшебная столица", "Your city is a magic capital"), style = T.title)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Каждый его квартал – свой рынок: в садах – лунный мёд, у реки – янтарь, в кузнях – гномьи шестерни. Вы – странствующий купец.",
                        "Each of its quarters is a market: moon honey in the gardens, amber by the river, dwarf cogs in the forges. You are a travelling merchant."), style = T.body)
                }
                1 -> {
                    Text(tr("Купить здесь – продать там", "Buy here, sell there"), style = T.title)
                    Spacer(Modifier.height(8.dp))
                    Line("ic_bundle", tr("Ездите как обычно: на машине, автобусе, трамвае, метро. Каждая поездка – путь каравана.", "Ride as you always do: by car, bus, tram or underground. Every ride is a caravan's journey."))
                    Line("ic_scales", tr("Товар дороже там, где его ждут, и чем дальше вы его увезли.", "A good fetches more where it's wanted, and the farther you carried it."))
                    Line("ic_lantern", tr("За рулём ничего нажимать не нужно: экран отдыхает, всё ждёт на стоянке.", "Nothing to tap while driving: the screen rests, everything waits for the stop."))
                }
                else -> {
                    Text(tr("Найдём ваш квартал", "Let's find your quarter"), style = T.title)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Кварталы берутся из OpenStreetMap вокруг вас. Местоположение нужно, чтобы знать, где вы торгуете и куда едете; оно не уходит с телефона.",
                        "The quarters come from OpenStreetMap around you. Location tells where you trade and where you ride; it never leaves the phone."), style = T.body)
                    if (busy) stage?.let { Spacer(Modifier.height(8.dp)); Text(it, style = T.smallB.copy(color = C.acc)) }
                    err?.let { Spacer(Modifier.height(8.dp)); Text(it, style = T.small.copy(color = C.bad)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == page) C.acc else C.line)) }
                }
                if (busy) CircularProgressIndicator(Modifier.size(28.dp), color = C.acc, strokeWidth = 3.dp)
                else Btn(if (page < 2) tr("Дальше", "Next") else tr("Найти мой квартал", "Find my quarter")) {
                    if (page < 2) { page++; return@Btn }
                    val want = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION); add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    MainActivity.ask(want) {
                        if (!c.feed.permitted()) { err = tr("Без местоположения кварталы не найти.", "Without location the quarters can't be found."); return@ask }
                        busy = true; err = null; stage = tr("Ищем вас…", "Finding you…")
                        scope.launch {
                            c.feed.acquire()
                            var f = c.game.live.value.fix ?: c.feed.last()
                            var waited = 0
                            while (f == null && waited < 40) { delay(500); waited++; f = c.game.live.value.fix }
                            c.feed.release()
                            if (f == null) { busy = false; err = tr("Не удалось определить место. Выйдите ближе к окну и попробуйте ещё раз.", "Couldn't get a position. Move closer to a window and try again."); return@launch }
                            stage = tr("Загружаем кварталы города…", "Loading the city's quarters…")
                            val ok = c.game.settle(f.lat, f.lon)
                            busy = false
                            if (!ok) { err = tr("Не удалось загрузить кварталы: нет сети или сервер карт занят. Попробуйте ещё раз.", "Couldn't load the quarters: no network or the map server is busy. Try again."); return@launch }
                            c.game.edit { it.copy(onboarded = true) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.statusBarsPadding())
    }
}

@Composable
private fun Line(pic: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Pic(pic, Modifier.size(34.dp))
        Spacer(Modifier.size(10.dp))
        Text(text, style = T.body)
    }
}
