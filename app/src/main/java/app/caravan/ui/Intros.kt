package app.caravan.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.caravan.domain.tr

/** A first-visit introduction: what a screen is for and how to use it. */
class Intro(val id: String, val pic: String, val title: String, val lines: List<Pair<String, String>>)

/** The introduction for what is on screen now, or null. */
fun introFor(tab: Int, over: Over?, trip: Boolean): Intro? = when (over) {
    Over.Market -> Intro("i.market", "ic_scales", tr("Рынок квартала", "The quarter's market"), listOf(
        "ic_bundle" to tr("«Купить»: квартал дёшево продаёт свои товары – по нескольку штук в день.", "«Buy»: a quarter sells its own goods cheap – a few a day."),
        "ic_purse" to tr("«Продать»: цена выше там, где товар ждут, и чем дальше вы его увезли. Внизу строки – прибыль или убыток.", "«Sell»: the price is higher where the good is wanted and the farther you carried it. Below – profit or loss."),
        "ic_hourglass" to tr("Каждая продажа одного товара в одном квартале сбивает цену – лучше развозить по разным.", "Each sale of the same good in one quarter lowers the price – better spread them around."),
        "ic_lantern" to tr("После ночной поездки появится вкладка «Ночной» с редкими товарами.", "After a night ride a «Night» tab appears with rare goods.")))
    Over.Pack -> Intro("i.pack", "ic_bundle", tr("Вьюк", "The pack"), listOf(
        "ic_bundle" to tr("Всё, что вы везёте: откуда товар и за сколько куплен.", "Everything you carry: where it's from and what you paid."),
        "ic_key" to tr("Переключатель «приказчик»: он сам продаст товар в поездке – там, где по пути дадут больше всего, и только с прибылью. Удобно за рулём.", "The «steward» switch: he sells the good on a trip – where it fetches most on the way, and only at a profit. Handy when driving."),
        "ride_chest" to tr("Внизу – больше места за золото.", "Below – more room for gold.")))
    Over.Rides -> Intro("i.rides", "ic_sign", tr("В путь", "Setting off"), listOf(
        "ride_car" to tr("Выберите, как едете. За рулём экран отдыхает: всё важное – после остановки.", "Pick how you ride. Driving, the screen rests: everything waits for the stop."),
        "ride_bus" to tr("В автобусе, трамвае, метро или пассажиром можно торговать прямо в пути.", "On a bus, tram, underground or as a passenger you can trade on the way."),
        "ic_hourglass" to tr("Поездка закончится сама, если постоять на месте 20 минут, или по «Приехали».", "The trip ends by itself after 20 still minutes, or with «Arrived».")))
    Over.Day -> Intro("i.day", "ic_scroll", tr("Поручения и слухи", "Orders and rumours"), listOf(
        "ic_scroll" to tr("Три поручения гильдии в день. Выполнили – заберите славу и монеты.", "Three guild orders a day. Done – claim renown and coins."),
        "ic_purse" to tr("Слух: сегодня этот квартал платит вдвое за названный товар.", "A rumour: today this quarter pays double for the named good."),
        "ic_tent" to tr("По выходным в одном из кварталов ярмарка – все цены выше.", "At weekends a fair comes to one quarter – all prices higher.")))
    is Over.Quarter -> Intro("i.quarter", "ic_map", tr("Страница квартала", "A quarter's page"), listOf(
        "crest_garden" to tr("Герб и вид квартала: сады, набережная, кузни, базар…", "The crest and kind: gardens, riverside, forges, bazaar…"),
        "ic_scales" to tr("«Делает» – что здесь дёшево купить. «Ждёт» – чьи товары здесь дороже всего.", "«Makes» – what's cheap here. «Wants» – whose goods fetch most here.")))
    is Over.TripPage -> Intro("i.trip", "ic_ledger", tr("Страница гроссбуха", "A ledger page"), listOf(
        "ic_sign" to tr("Каждая поездка записывается: путь, кварталы, слава, находка на обочине.", "Every ride is written down: the way, quarters, renown, a roadside find."),
        "ic_hourglass" to tr("Пробки не наказываются – просто отмечаются.", "Jams aren't punished – just noted.")))
    is Over.HousePart -> null
    is Over.Merchant -> Intro("i.merchant", "mer_bazaar", tr("Купец", "A merchant"), listOf(
        "ic_scroll" to tr("У каждого купца – четыре просьбы с маленькой историей. Взялись – и считаются только сделки после этого.", "Each merchant has four requests with a little story. Once taken, only deals after that count."),
        "ic_purse" to tr("Выполнили – отдайте заказ: монеты и слава, а за последнюю просьбу ещё и диковина.", "Done – hand it over: coins and renown, and a curio for the last one."),
        "ic_seal" to tr("Друг-купец даёт скидку и лишний товар во всех кварталах своего вида.", "A merchant friend gives a discount and an extra good in all quarters of their kind.")))
    Over.Merchants -> null
    Over.Encounter -> null
    Over.Contracts -> Intro("i.contracts", "ic_scroll", tr("Контракты", "Contracts"), listOf(
        "ic_purse" to tr("Берёте контракт – вносите залог. Довезли нужный товар в нужный вид квартала за 2 дня – залог возвращается с наградой.", "Take a contract – pay a deposit. Deliver the good to the right kind of quarter in 2 days – the deposit comes back with a reward."),
        "ic_hourglass" to tr("Не успели – залог пропадает. Это риск, на который идёте сами.", "Too late – the deposit is lost. A risk you choose yourself.")))
    Over.Curios -> Intro("i.curios", "cur_lamp", tr("Книга диковин", "The book of curios"), listOf(
        "cur_moonshard" to tr("Шестьдесят диковин, по пять на каждый вид дороги: кварталы, ночь, метро, дальняя дорога, непогода.", "Sixty curios, five for each kind of road: quarters, night, underground, long road, weather."),
        "ic_sign" to tr("Примерно каждая третья поездка что-то приносит. Тапните пустую клетку – подскажет, где искать.", "About one ride in three brings one. Tap an empty cell for a hint where to look.")))
    Over.Shop -> Intro("i.shop", "bld_stall", tr("Своя лавка", "Your stall"), listOf(
        "bld_stall" to tr("Лавку открывают в чужом квартале. Оставьте на прилавке товар – она продаёт по тюку в 4 часа, пока вас нет.", "Open a stall in another quarter. Leave goods on the counter – it sells a bale every 4 hours while you're away."),
        "ic_purse" to tr("Выручку и новый товар – только когда сами приедете в этот квартал.", "The till and new stock – only when you come to that quarter yourself.")))
    is Over.RoutePage -> Intro("i.route", "bld_inn", tr("Торговый путь", "A trade route"), listOf(
        "ic_sign" to tr("Путь крепнет с каждой поездкой между этими кварталами: тропа → дорога → торговый путь → тракт.", "A route grows with every ride between these quarters: track → road → trade route → high road."),
        "bld_well" to tr("На крепком пути можно строить: каждая постройка даёт свою пользу навсегда.", "On a strong route you can build: each building helps for good.")))
    Over.Settings -> null
    null -> when (tab) {
        0 -> if (trip) Intro("i.ontrip", "ride_car", tr("Караван в пути", "The caravan on its way"), listOf(
            "ic_map" to tr("Кварталы засчитываются сами, когда вы через них проезжаете. Новые – +10 славы.", "Quarters count by themselves as you pass. New ones give +10 renown."),
            "ic_lantern" to tr("За рулём в движении экран показывает только дорогу – не отвлекайтесь.", "Driving in motion, the screen shows only the road – keep your eyes on it."),
            "ic_sign" to tr("Можно убрать телефон: поездка идёт и с выключенным экраном.", "You can pocket the phone: the trip goes on with the screen off.")))
        else Intro("i.gate", "ic_purse", tr("Ворота квартала", "The quarter's gate"), listOf(
            "crest_bazaar" to tr("Вверху – квартал, где вы сейчас. Тапните герб – что он делает и чего ждёт.", "At the top – the quarter you're in. Tap the crest – what it makes and wants."),
            "ic_bundle" to tr("Слева вверху – монеты и слава, справа – вьюк: тапните, чтобы открыть.", "Top left – coins and renown, top right – the pack: tap to open it."),
            "ic_scales" to tr("«Рынок» – купить и продать здесь. «В путь» – перед поездкой на машине, автобусе или метро.", "«Market» – buy and sell here. «Set off» – before a ride by car, bus or underground."),
            "ic_scroll" to tr("Строка внизу – поручения гильдии и слухи дня.", "The line below – guild orders and the day's rumours.")))
        1 -> Intro("i.house", "tab_house", tr("Торговый дом", "The trading house"), listOf(
            "ic_purse" to tr("Сюда уходит заработанное: на участке в вашем квартале можно построить десять частей дома.", "This is where earnings go: ten parts of a house to build on your plot."),
            "tab_build" to tr("Тапните «+» на пустом месте – что даёт постройка и сколько стоит.", "Tap «+» on an empty place – what it gives and what it costs."),
            "hp_treasury" to tr("Каждая часть навсегда помогает торговле, а сокровищница в конце венчает дом.", "Each part helps trade for good, and the treasury at the end crowns the house.")))
        2 -> Intro("i.map", "ic_map", tr("Карта кварталов", "The map of quarters"), listOf(
            "ic_map" to tr("Город поделён на кварталы. Изведанные – в цвете с гербом, остальные бледные.", "The city is cut into quarters. Explored ones in colour with a crest, the rest pale."),
            "ic_key" to tr("Чтобы изведать квартал, просто проедьте через него в поездке.", "To explore a quarter, just ride through it on a trip."),
            "crest_water" to tr("Тапните квартал – откроется его страница.", "Tap a quarter to open its page.")))
        3 -> Intro("i.ledger", "ic_ledger", tr("Гроссбух", "The ledger"), listOf(
            "ic_ledger" to tr("Итоги вверху и по странице на каждую поездку. Тапните – подробности и сделки.", "Totals at the top and a page per trip. Tap for details and deals.")))
        else -> Intro("i.guild", "ic_seal", tr("Гильдия", "The guild"), listOf(
            "ic_seal" to tr("Слава поднимает звание: от разносчика до торгового князя.", "Renown raises your rank: from pedlar to merchant prince."),
            "ic_scroll" to tr("Поручения и слухи – здесь же.", "Orders and rumours are here too."),
            "bld_stall" to tr("Торговые пути: тапните путь, чтобы строить на нём лавки, колодцы, склады.", "Trade routes: tap one to build stalls, wells, warehouses on it.")))
    }
    else -> null
}

@Composable
fun IntroSheet(i: Intro, onDismiss: () -> Unit) {
    // a compact card near the top, lightly dimmed: the screen it explains stays in view below it
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val shown = androidx.compose.runtime.remember { androidx.compose.animation.core.MutableTransitionState(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown.targetState = true }
    Box(Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(shown, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x33000000)).tap(onDismiss))
        }
        androidx.compose.animation.AnimatedVisibility(shown, Modifier.align(Alignment.TopCenter),
            enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { -it }) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
                .framed(16.dp).tap { }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                IntroBody(i, onDismiss)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.IntroBody(i: Intro, onDismiss: () -> Unit) {
    run {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(i.pic, Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Text(i.title, style = T.h3, modifier = Modifier.weight(1f))
            Text(tr("Понятно", "Got it"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
        }
        Spacer(Modifier.height(4.dp))
        i.lines.forEach { (pic, text) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(pic, Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(text, style = T.small.copy(color = C.ink))
            }
        }
    }
}

/** «?» on a screen opens its introduction again. */
object Help { var open by mutableStateOf(false) }

@Composable
fun HelpBtn(modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).clip(CircleShape).background(C.paper.copy(alpha = 0.92f)).border(1.dp, C.gold.copy(alpha = 0.7f), CircleShape)
        .clickable { Help.open = true }, contentAlignment = Alignment.Center) {
        Text("?", style = T.h3)
    }
}
