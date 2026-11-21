package app.caravan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.caravan.container
import app.caravan.data.State
import app.caravan.domain.Contracts
import app.caravan.domain.Curios
import app.caravan.domain.Encounters
import app.caravan.domain.Events
import app.caravan.domain.Goods
import app.caravan.domain.Market
import app.caravan.domain.Merchants
import app.caravan.domain.Shops
import app.caravan.domain.fmt
import app.caravan.domain.tr
import kotlinx.coroutines.launch

/** A merchant: their words, the current request and its progress. */
@Composable
fun MerchantSheet(s: State, id: String, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val m = Merchants.byId[id] ?: return onDismiss()
    LaunchedEffect(id) { if (Merchants.met(s, m)) c.game.talk(id) }
    val step = Merchants.step(s, m)
    val friend = Merchants.friend(s, m)
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(m.pic, Modifier.size(96.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, style = T.title)
                Text(m.role.replaceFirstChar { it.uppercase() } + " · " + m.kind.title.lowercase(), style = T.small)
                Spacer(Modifier.height(6.dp))
                Text("«" + (if (friend) m.bye else m.hello) + "»", style = T.body.copy(fontStyle = FontStyle.Italic))
            }
        }
        Ornament()
        if (!Merchants.met(s, m)) {
            Text(tr("Встретите, когда побываете в квартале вида «${m.kind.ru}».", "You'll meet them in a «${m.kind.en}» quarter."), style = T.body)
            return@Sheet
        }
        // the chain: four lozenges, done ones filled
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            m.steps.indices.forEach { i ->
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(if (i < step) C.gold else C.line))
            }
            Spacer(Modifier.width(6.dp))
            Text(if (friend) tr("друг", "a friend") else tr("просьба ${step + 1} из ${m.steps.size}", "request ${step + 1} of ${m.steps.size}"), style = T.smallB)
        }
        Spacer(Modifier.height(10.dp))
        if (friend) {
            Text(tr("Теперь в кварталах вида «${m.kind.ru}» для вас лишний товар каждого вида и цена на 10% ниже.",
                "Now «${m.kind.en}» quarters keep one more of each good for you, 10% cheaper."), style = T.body)
            return@Sheet
        }
        val st = m.steps[step]
        val (a, b) = Merchants.progress(s, m)
        Text(st.text, style = T.bodyB)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.paper2).padding(10.dp)) {
            Text(tr("Как выполнить", "How to do it"), style = T.smallB.copy(color = C.acc))
            Text(Merchants.how(s, st), style = T.small.copy(color = C.ink))
            Text(tr("Считаются только сделки и поездки после разговора с купцом.", "Only deals and rides after talking to the merchant count."), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Meter(a.toFloat() / b)
        Text("${minOf(a, b)} / $b · " + tr("награда ${st.gold} монет и ${st.renown} славы", "reward ${st.gold} coins and ${st.renown} renown") +
            if (step == m.steps.lastIndex) tr(" + диковина", " + a curio") else "", style = T.small)
        Spacer(Modifier.height(12.dp))
        Btn(if (a >= b) tr("Отдать заказ", "Hand it over") else tr("Принято – в путь", "Got it – off I go"), Modifier.fillMaxWidth(), enabled = true) {
            if (a >= b) scope.launch { c.game.claimMerchant(id) } else onDismiss()
        }
    }
}

/** All eight merchants, met or not. */
@Composable
fun MerchantsSheet(s: State, onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) {
        Title(tr("Купцы города", "The city's merchants"), tr("По одному в каждом виде квартала", "One in each kind of quarter"))
        Spacer(Modifier.height(8.dp))
        Merchants.all.forEach { m ->
            val met = Merchants.met(s, m)
            GoodRow(m.pic, if (met) m.name + " · " + m.role else "?", when {
                !met -> tr("ждёт в квартале вида «${m.kind.ru}»", "waits in a «${m.kind.en}» quarter")
                Merchants.friend(s, m) -> tr("друг – скидка в его кварталах", "a friend – discounts in their quarters")
                Merchants.ready(s, m) -> tr("заказ готов – отдайте!", "the order is ready – hand it over!")
                else -> tr("просьба ${Merchants.step(s, m) + 1} из ${m.steps.size}", "request ${Merchants.step(s, m) + 1} of ${m.steps.size}")
            }, Modifier.clickable { onOpen(m.id) }) {
                if (Merchants.ready(s, m)) Text("!", style = T.title.copy(color = C.acc))
            }
        }
    }
}

/** Guild contracts: today's offers and the ones taken. */
@Composable
fun ContractsSheet(s: State, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    Sheet(onDismiss) {
        Title(tr("Контракты гильдии", "Guild contracts"), tr("Залог возвращается с наградой, если успеете к сроку. Опоздаете – залог пропадёт.", "The deposit comes back with the reward if you're in time. Late – the deposit is lost."))
        Spacer(Modifier.height(8.dp))
        val taken = s.contracts.filter { it.taken }
        var open by remember { mutableStateOf<Long?>(null) }
        if (taken.isNotEmpty()) Text(tr("Взятые", "Taken"), style = T.h3)
        taken.forEach { k ->
            val g = Goods[k.good]
            val p = Contracts.progress(s, k)
            GoodRow(g.pic, tr("${k.n} × ${g.ru} → «${k.where.ru}»", "${k.n} × ${g.en} → «${k.where.en}»"),
                tr("сдано $p из ${k.n} · до ", "delivered $p of ${k.n} · until ") + app.caravan.domain.fmtDay(app.caravan.domain.dayOf(k.until)),
                Modifier.clickable { open = if (open == k.id) null else k.id }, lines = 4) {
                if (p >= k.n) Btn("+" + fmt(k.deposit + k.reward), small = true) { scope.launch { c.game.claimContract(k.id) } }
            }
            if (open == k.id) ContractHow(s, k, taken = true)
        }
        Ornament()
        Text(tr("Предложения на сегодня", "Today's offers"), style = T.h3)
        Contracts.offers(s).forEach { k ->
            val g = Goods[k.good]
            GoodRow(g.pic, tr("${k.n} × ${g.ru} → «${k.where.ru}»", "${k.n} × ${g.en} → «${k.where.en}»"),
                tr("залог ${k.deposit} · награда ${k.reward} · 2 дня", "deposit ${k.deposit} · reward ${k.reward} · 2 days"),
                Modifier.clickable { open = if (open == k.id) null else k.id }, lines = 4) {
                Btn(tr("Взять", "Take"), small = true, enabled = s.gold >= k.deposit && taken.size < 3) { scope.launch { c.game.takeContract(k) } }
            }
            if (open == k.id) ContractHow(s, k, taken = false)
        }
        if (taken.size >= 3) Text(tr("Больше трёх контрактов сразу гильдия не даёт.", "The guild gives no more than three at once."), style = T.small)
        Text(tr("Тапните контракт – что нужно сделать.", "Tap a contract to see what to do."), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A contract in plain words: where to buy, where to sell, by when, and what comes back. */
@Composable
private fun ContractHow(s: State, k: app.caravan.domain.Contract, taken: Boolean) {
    val g = Goods[k.good]
    val here = s.quarters[s.here]
    fun nearest(kind: app.caravan.domain.Kind?) = s.quarters.values.filter { it.kind == kind }
        .minByOrNull { q -> here?.let { app.caravan.trip.Geo.distance(it.lat, it.lon, q.lat, q.lon) } ?: 0.0 }
    val buyAt = nearest(g.kind)
    val sellAt = nearest(k.where)
    val gk = g.kind
    val buy = if (gk == null) tr("Добудьте ${k.n} × ${g.ru.lowercase()}.", "Get ${k.n} × ${g.en.lowercase()}.")
        else tr("1. Купите ${k.n} × ${g.ru.lowercase()} – его продают в кварталах вида «${gk.ru}»", "1. Buy ${k.n} × ${g.en.lowercase()} – it is sold in «${gk.en}» quarters") +
            (buyAt?.let { tr(", ближе всего – ${it.ru}", ", nearest: ${it.en}") } ?: tr(" (такой вы ещё не нашли)", " (you haven't found one yet)")) + "."
    val sell = tr("2. Отвезите и продайте их в любом квартале вида «${k.where.ru}»", "2. Carry them to any «${k.where.en}» quarter and sell them there") +
        (sellAt?.let { tr(", например в ${it.ru}", ", for example ${it.en}") } ?: tr(" (такой вы ещё не нашли)", " (you haven't found one yet)")) +
        tr(" до ${app.caravan.domain.fmtDay(app.caravan.domain.dayOf(k.until))}, конца дня.", " by the end of ${app.caravan.domain.fmtDay(app.caravan.domain.dayOf(k.until))}.")
    val money = tr("3. Залог ${k.deposit} вернётся вместе с наградой ${k.reward}. Не успеете – залог пропадёт.", "3. The ${k.deposit} deposit comes back with a ${k.reward} reward. Too late – the deposit is lost.")
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(10.dp)).background(C.paper2).padding(10.dp)) {
        Text(tr("Как выполнить", "How to do it"), style = T.smallB.copy(color = C.acc))
        Text(buy, style = T.small.copy(color = C.ink))
        Text(sell, style = T.small.copy(color = C.ink))
        Text(money, style = T.small.copy(color = C.ink))
        if (taken) Text(tr("Считаются продажи после того, как вы взяли контракт.", "Only sales after you took the contract count."), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The book of curios: 60 cells by kind of road, found ones in colour. */
@Composable
fun CuriosSheet(s: State, onDismiss: () -> Unit) {
    var pick by remember { mutableStateOf<Curios.Curio?>(null) }
    Sheet(onDismiss) {
        Title(tr("Книга диковин", "The book of curios"), tr("Найдено ${s.curios.size} из ${Curios.all.size}", "Found ${s.curios.size} of ${Curios.all.size}"))
        pick?.let { p ->
            val has = p.id in s.curios
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).framed(12.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(p.pic, Modifier.size(56.dp), alpha = if (has) 1f else 0.15f)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(if (has) p.name else "?", style = T.bodyB)
                    Text(tr("Где искать: ", "Where to look: ") + p.context.hint, style = T.small)
                }
            }
        }
        Curios.CONTEXTS.forEach { ctx ->
            Spacer(Modifier.height(8.dp))
            Text(ctx.title + " · " + Curios.of(ctx.id).count { it.id in s.curios } + "/5", style = T.smallB)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Curios.of(ctx.id).forEach { cu ->
                    val has = cu.id in s.curios
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(C.paper2)
                        .border(1.dp, if (pick == cu) C.acc else C.line, RoundedCornerShape(10.dp)).clickable { pick = cu }, contentAlignment = Alignment.Center) {
                        Pic(cu.pic, Modifier.size(48.dp), alpha = if (has) 1f else 0.12f)
                    }
                }
            }
        }
    }
}

/** The stall in the quarter you're in: open one, leave bales, take the till. */
@Composable
fun ShopSheet(s: State, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val q = s.quarters[s.here] ?: return onDismiss()
    val sh = s.shops.firstOrNull { it.quarter == q.id }
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic("bld_stall", Modifier.size(56.dp))
            Spacer(Modifier.width(10.dp))
            Title(tr("Лавка: ", "Stall: ") + q.name, tr("Торгует, пока вас нет: по тюку в 4 часа, за 90% местной цены", "Trades while you're away: a bale every 4 hours, for 90% of the local price"))
        }
        Spacer(Modifier.height(10.dp))
        if (sh == null) {
            val price = Shops.price(s)
            when {
                q.kind == null -> Text(tr("Сначала узнайте, что это за квартал.", "First learn what quarter this is."), style = T.body)
                price == null -> Text(tr("У вас уже три лавки – больше гильдия не позволяет.", "You have three stalls already – the guild allows no more."), style = T.body)
                else -> {
                    Text(tr("Лучше всего лавка в квартале, который дорого платит за ваши товары – и куда вы и так ездите.", "Best is a quarter that pays well for your goods – and one you ride to anyway."), style = T.body)
                    Spacer(Modifier.height(10.dp))
                    Btn(tr("Открыть лавку за ${fmt(price)}", "Open a stall for ${fmt(price)}"), Modifier.fillMaxWidth(), enabled = s.gold >= price) { scope.launch { c.game.openShop(q.id) } }
                }
            }
            return@Sheet
        }
        if (sh.till > 0) {
            Btn(tr("Забрать выручку: ${fmt(sh.till)}", "Take the till: ${fmt(sh.till)}"), Modifier.fillMaxWidth(), color = C.gold, ink = C.ink) { c.game.collect(q.id) }
            Spacer(Modifier.height(8.dp))
        }
        Text(tr("На прилавке ${sh.stock.size} из ${Shops.SLOTS}", "On the counter: ${sh.stock.size} of ${Shops.SLOTS}"), style = T.h3)
        sh.stock.forEach { lot -> GoodRow(Goods[lot.good].pic, Goods[lot.good].name, tr("продастся примерно за ${(Market.price(lot, q, s) * 0.9).toInt()}", "will sell for about ${(Market.price(lot, q, s) * 0.9).toInt()}")) {} }
        if (sh.stock.size < Shops.SLOTS && s.pack.isNotEmpty()) {
            Ornament()
            Text(tr("Оставить из вьюка", "Leave from the pack"), style = T.h3)
            s.pack.forEach { lot ->
                GoodRow(Goods[lot.good].pic, Goods[lot.good].name, tr("здесь ≈ ${(Market.price(lot, q, s) * 0.9).toInt()}", "here ≈ ${(Market.price(lot, q, s) * 0.9).toInt()}")) {
                    Btn(tr("На прилавок", "To counter"), small = true, color = C.teal) { c.game.stock(q.id, lot.id) }
                }
            }
        }
    }
}

/** A meeting on the road after a ride. */
@Composable
fun EncounterSheet(s: State, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val e = Encounters.byId[s.encounter.substringBefore(':')]
    var result by remember { mutableStateOf<String?>(null) }
    if (e == null && result == null) return onDismiss()
    Sheet({ if (result != null) onDismiss() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(e?.pic ?: "ic_sign", Modifier.size(52.dp))
            Spacer(Modifier.width(10.dp))
            Title(e?.title ?: tr("В пути", "On the road"), tr("Встреча в дороге", "A meeting on the road"))
        }
        Spacer(Modifier.height(10.dp))
        val r = result
        if (r != null) {
            Text(r, style = T.body)
            Spacer(Modifier.height(12.dp))
            Btn(tr("Дальше", "Onward"), Modifier.fillMaxWidth(), onClick = onDismiss)
            return@Sheet
        }
        e ?: return@Sheet
        Text(e.text, style = T.body)
        Spacer(Modifier.height(12.dp))
        e.options.indices.forEach { i ->
            Btn(e.option(i), Modifier.fillMaxWidth().padding(vertical = 4.dp), color = if (i == 0) C.acc else C.paper2, ink = if (i == 0) C.accInk else C.ink) {
                scope.launch { result = c.game.resolve(i) }
            }
        }
    }
}

/** The week's event, as a card. */
@Composable
fun EventCard(modifier: Modifier = Modifier) {
    val e = Events.of()
    Row(modifier.fillMaxWidth().framed(12.dp, fill = C.goldSoft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Pic(e.pic, Modifier.size(40.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(tr("Неделя: ", "This week: ") + e.title, style = T.bodyB)
            Text(e.text, style = T.small.copy(color = C.ink), textAlign = TextAlign.Start)
        }
    }
}

