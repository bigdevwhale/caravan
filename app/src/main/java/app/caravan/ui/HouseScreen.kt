package app.caravan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.caravan.container
import app.caravan.data.State
import app.caravan.domain.Goods
import app.caravan.domain.House
import app.caravan.domain.fmt
import app.caravan.domain.tr
import kotlinx.coroutines.launch

/** The trading house on its plot: built parts stand where they belong, empty places wait with their price. */
@Composable
fun HouseScreen(s: State, open: (Over) -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val built = House.parts.count { House.has(s, it.id) }
    Box(Modifier.fillMaxSize().background(C.bg)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val plotId = drawableId("house_plot")
            if (plotId != 0) {
                val painter = painterResource(plotId)
                val ratio = painter.intrinsicSize.width / painter.intrinsicSize.height
                // the plot fills the screen, cropped at the sides, its ground kept at the bottom
                val imgW: Dp = maxOf(maxWidth, maxHeight * ratio)
                val imgH: Dp = imgW / ratio
                val left: Dp = (maxWidth - imgW) / 2
                val top: Dp = maxHeight - imgH
                Image(painter, null, Modifier.requiredSize(imgW, imgH).offset(x = left + imgW / 2 - maxWidth / 2, y = top + imgH / 2 - maxHeight / 2), contentScale = ContentScale.FillBounds)
                for (p in House.parts.sortedBy { it.y }) {
                    val cx = left + imgW * p.x
                    val base = top + imgH * p.y
                    val id = drawableId(p.pic)
                    if (id == 0) continue
                    val pp = painterResource(id)
                    val w = imgW * p.w
                    val h = w * (pp.intrinsicSize.height / pp.intrinsicSize.width)
                    // built parts in full colour; the rest as a pale ghost of what will stand there
                    Image(pp, p.title, Modifier.offset(x = cx - w / 2, y = base - h).size(w, h).alpha(if (House.has(s, p.id)) 1f else 0.22f)
                        .clickable { open(Over.HousePart(p.id)) })
                }
                for (p in House.parts.sortedBy { it.y }) {
                    val cx = left + imgW * p.x
                    val base = top + imgH * p.y
                    if (!House.has(s, p.id)) {
                        // an empty place: a stake with the price
                        val can = House.canBuild(s, p)
                        Column(Modifier.offset(x = cx - 34.dp, y = base - imgH * p.w * 0.45f - 30.dp).width(68.dp).clickable { open(Over.HousePart(p.id)) },
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(if (can) C.acc else C.paper.copy(alpha = 0.85f))
                                .border(1.5.dp, C.gold, CircleShape), contentAlignment = Alignment.Center) {
                                Text("+", style = T.title.copy(color = if (can) C.accInk else C.mute))
                            }
                            Text(fmt(p.price), style = T.tiny.copy(color = C.ink), modifier = Modifier.padding(top = 2.dp)
                                .clip(RoundedCornerShape(6.dp)).background(C.paper.copy(alpha = 0.85f)).padding(horizontal = 5.dp))
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("ic_purse", fmt(s.gold))
                Spacer(Modifier.weight(1f))
                HelpBtn()
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.align(Alignment.CenterHorizontally).framed(16.dp).padding(horizontal = 18.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (House.done(s)) tr("Торговый дом", "Trading house") else tr("Ваш торговый дом", "Your trading house"), style = T.title)
                Text(tr("построено $built из ${House.parts.size}", "$built of ${House.parts.size} built"), style = T.small)
                Meter(built / House.parts.size.toFloat(), Modifier.padding(top = 4.dp).width(180.dp))
            }
        }
        if (House.giftReady(s)) {
            Row(Modifier.align(Alignment.BottomCenter).padding(12.dp).framed(14.dp).clickable {
                scope.launch {
                    val g = c.game.takeGift()
                    c.game.news.tryEmit(app.caravan.data.News(g?.let { tr("Сад подарил: ${Goods[it].ru}", "The garden gave: ${Goods[it].en}") }
                        ?: tr("Во вьюке нет места для подарка", "No room in the pack for the gift")))
                }
            }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic("hp_garden", Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Text(tr("Сад принёс подарок – забрать", "The garden has a gift – take it"), style = T.bodyB)
            }
        }
    }
}

/** One part of the house: what it gives, and the button to build it. */
@Composable
fun HousePartSheet(s: State, id: String, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val p = House.byId[id] ?: return onDismiss()
    val has = House.has(s, id)
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)).background(C.paper2), contentAlignment = Alignment.Center) {
                Pic(p.pic, Modifier.size(84.dp), alpha = if (has) 1f else 0.55f)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.title, style = T.title)
                Text(p.effect, style = T.body)
                Text(tr("+${p.renown} славы при постройке", "+${p.renown} renown when built"), style = T.small)
            }
        }
        Spacer(Modifier.height(14.dp))
        when {
            has -> Text(tr("Уже стоит.", "Already built."), style = T.bodyB.copy(color = C.ok))
            p.id == "treasury" && !House.parts.all { it.id == "treasury" || House.has(s, it.id) } ->
                Text(tr("Сокровищницу ставят последней – когда готово всё остальное.", "The treasury comes last – once everything else stands."), style = T.body)
            else -> {
                Btn(tr("Построить за ${fmt(p.price)}", "Build for ${fmt(p.price)}"), Modifier.fillMaxWidth(), enabled = s.gold >= p.price) {
                    scope.launch { if (c.game.buildHouse(id)) onDismiss() }
                }
                if (s.gold < p.price) Text(tr("Не хватает ${fmt(p.price - s.gold)} монет", "${fmt(p.price - s.gold)} coins short"), style = T.small, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

