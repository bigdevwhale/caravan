package app.caravan.domain

import app.caravan.data.State

/**
 * The merchant's own trading house in the home quarter: the long goal gold goes to. Ten parts, each seen on
 * the plot once built and each helping trade for good; the treasury, last, crowns the house.
 */
object House {
    /** [x] centre and [y] baseline as shares of the plot picture, [w] width share; drawn back to front by [y]. */
    class Part(val id: String, val price: Int, val ru: String, val en: String, val effRu: String, val effEn: String,
               val x: Float, val y: Float, val w: Float) {
        val title get() = tr(ru, en)
        val effect get() = tr(effRu, effEn)
        val pic get() = "hp_$id"
        val renown get() = price / 40
    }

    val parts = listOf(
        Part("gate", 400, "Ворота с гербом", "Crested gate", "местные сплетни: ещё один слух каждый день", "local gossip: one more rumour a day", 0.50f, 0.99f, 0.32f),
        Part("storeroom", 700, "Кладовая", "Storeroom", "+2 места во вьюке", "+2 places in the pack", 0.25f, 0.72f, 0.28f),
        Part("garden", 1100, "Сад", "Garden", "раз в день дарит товар вашего квартала", "gives a good of your quarter once a day", 0.25f, 0.95f, 0.26f),
        Part("fountain", 1600, "Фонтан во дворе", "Courtyard fountain", "+5% ко всем продажам", "+5% to every sale", 0.50f, 0.80f, 0.19f),
        Part("shed", 2200, "Каретный сарай", "Cart shed", "находки на обочине уже с 1 км, на долгих поездках – две", "roadside finds from 1 km, two on long rides", 0.75f, 0.72f, 0.28f),
        Part("hall", 3200, "Палаты", "The hall", "+3 места во вьюке", "+3 places in the pack", 0.50f, 0.60f, 0.44f),
        Part("tower", 3600, "Голубятня", "Pigeon tower", "ночной рынок открывают и вечерние поездки, с 19:00", "evening rides from 7 pm open the night market too", 0.19f, 0.57f, 0.13f),
        Part("pavilion", 4800, "Купол", "Domed pavilion", "+10% ко всем продажам", "+10% to every sale", 0.80f, 0.57f, 0.21f),
        Part("banner", 6000, "Знамя гильдии", "Guild banner", "двойная слава за поручения", "double renown for orders", 0.40f, 0.74f, 0.12f),
        Part("treasury", 8000, "Сокровищница", "Treasury", "венец дома: +500 славы и звание «Торговый дом»", "the crown of the house: +500 renown and the title «Trading house»", 0.76f, 0.95f, 0.22f),
    )
    val byId = parts.associateBy { it.id }

    fun has(s: State, id: String) = id in s.house
    fun canBuild(s: State, p: Part) = !has(s, p.id) && s.gold >= p.price && (p.id != "treasury" || parts.all { it.id == "treasury" || has(s, it.id) })
    fun done(s: State) = parts.all { has(s, it.id) }

    fun slots(s: State) = (if (has(s, "storeroom")) 2 else 0) + (if (has(s, "hall")) 3 else 0)
    fun sale(s: State) = 1.0 + (if (has(s, "fountain")) 0.05 else 0.0) + (if (has(s, "pavilion")) 0.10 else 0.0)
    fun rumours(s: State) = if (has(s, "gate")) 1 else 0
    fun findKm(s: State) = if (has(s, "shed")) 1.0 else 2.0
    fun nightFrom(s: State) = if (has(s, "tower")) 19 else 21
    fun orderMul(s: State) = if (has(s, "banner")) 2 else 1
    fun giftReady(s: State, day: Long = today()) = has(s, "garden") && s.giftDay != day
}
