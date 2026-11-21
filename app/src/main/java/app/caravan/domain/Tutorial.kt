package app.caravan.domain

import app.caravan.data.State

/**
 * The first steps: six small things in the order a new merchant needs them, each paid for. Until they are
 * done the gate shows the next one instead of the day's orders, and the deeper parts of the game (stalls,
 * contracts, road meetings) stay out of the way.
 */
object Tutorial {
    class Step(val ru: String, val en: String, val howRu: String, val howEn: String, val gold: Int, val done: (State) -> Boolean) {
        val text get() = tr(ru, en)
        val how get() = tr(howRu, howEn)
    }

    val steps = listOf(
        Step("Купите товар", "Buy a good", "Нажмите «Рынок» внизу и купите что-нибудь на вкладке «Купить».", "Press «Market» below and buy something on the «Buy» tab.", 20) { s -> s.deals.any { it.buy } || s.pack.isNotEmpty() },
        Step("Отправьтесь в путь", "Set off", "Перед обычной поездкой – на машине, автобусе, метро – нажмите «В путь» и выберите, как едете.", "Before an ordinary ride – car, bus, underground – press «Set off» and pick how you ride.", 20) { s -> s.trip != null || s.trips.isNotEmpty() },
        Step("Доедьте до другого квартала", "Reach another quarter", "Просто езжайте. Кварталы засчитаются сами, поездка закончится по «Приехали» или сама.", "Just ride. Quarters count by themselves; the trip ends with «Arrived» or by itself.", 30) { s -> s.trips.any { it.quarters.distinct().size >= 2 } },
        Step("Продайте с прибылью", "Sell at a profit", "В другом квартале откройте «Рынок» → «Продать». Зелёная цифра под ценой – ваша прибыль.", "In another quarter open «Market» → «Sell». The green number under the price is your profit.", 40) { s -> s.deals.any { !it.buy && it.price > it.paid } },
        Step("Поговорите с купцом", "Talk to a merchant", "Купец стоит у ворот слева. Тапните его – он даст первую просьбу.", "A merchant stands by the gate on the left. Tap them for a first request.", 30) { s -> s.mSince.isNotEmpty() || s.mStep.isNotEmpty() },
        Step("Постройте ворота дома", "Build the house gate", "Вкладка «Дом» → «+» с ценой 400. Дом – то, ради чего копится золото.", "The «House» tab → «+» priced 400. The house is what gold is saved for.", 0) { s -> s.house.isNotEmpty() },
    )

    fun finished(s: State) = s.tut >= steps.size
    fun current(s: State) = steps.getOrNull(s.tut)

    /** Pays for every step done in a row; returns the new state and the gold just paid. */
    fun advance(s0: State): Pair<State, Int> {
        var s = s0; var paid = 0
        while (s.tut < steps.size && steps[s.tut].done(s)) {
            paid += steps[s.tut].gold
            s = s.copy(tut = s.tut + 1, gold = s.gold + steps[s.tut].gold, renown = s.renown + 5)
        }
        return s to paid
    }
}
