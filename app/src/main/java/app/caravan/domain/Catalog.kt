package app.caravan.domain

/**
 * What a quarter of the city is, read from what OpenStreetMap has in it. The order matches the crests
 * (crest_<id>) and the goods below: each kind makes three goods.
 */
enum class Kind(val id: String, val ru: String, val en: String, val ruOf: String, val enOf: String) {
    GARDEN("garden", "Сады", "Gardens", "садовый квартал", "a garden quarter"),
    WATER("water", "Набережная", "Riverside", "квартал у воды", "a riverside quarter"),
    OLD("old", "Старый город", "Old town", "старый квартал", "an old quarter"),
    SCHOLAR("scholar", "Учёный квартал", "Scholars", "учёный квартал", "a scholars' quarter"),
    FORGE("forge", "Кузни", "Forges", "квартал кузниц", "a forge quarter"),
    BAZAAR("bazaar", "Базар", "Bazaar", "базарный квартал", "a bazaar quarter"),
    GATE("gate", "Ворота", "Gates", "квартал ворот", "a gate quarter"),
    HOMES("homes", "Слобода", "Homesteads", "жилая слобода", "a homestead quarter");

    val title get() = tr(ru, en)
    val crest get() = "crest_$id"

    /** The two kinds whose goods sell dearest here. */
    val wants: List<Kind> get() = when (this) {
        GARDEN -> listOf(FORGE, SCHOLAR)
        WATER -> listOf(GARDEN, HOMES)
        OLD -> listOf(SCHOLAR, BAZAAR)
        SCHOLAR -> listOf(OLD, GATE)
        FORGE -> listOf(GARDEN, WATER)
        BAZAAR -> listOf(GATE, WATER)
        GATE -> listOf(BAZAAR, FORGE)
        HOMES -> listOf(GARDEN, BAZAAR)
    }

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
}

/** A trade good. [base] is its fair price in coins; [rare] ones come only from the night market. */
class Good(val id: String, val kind: Kind?, val base: Int, val ru: String, val en: String, val rare: Boolean = false) {
    val name get() = tr(ru, en)
    val pic get() = if (rare) "rare_$id" else "good_$id"
}

object Goods {
    val all = listOf(
        Good("honey", Kind.GARDEN, 24, "Лунный мёд", "Moon honey"),
        Good("herbs", Kind.GARDEN, 14, "Целебные травы", "Healing herbs"),
        Good("resin", Kind.GARDEN, 18, "Смола древня", "Old-tree resin"),
        Good("amber", Kind.WATER, 30, "Речной янтарь", "River amber"),
        Good("fish", Kind.WATER, 12, "Туманная рыба", "Mist fish"),
        Good("pearl", Kind.WATER, 40, "Жемчуг заводи", "Backwater pearl"),
        Good("glass", Kind.OLD, 34, "Витражное стекло", "Stained glass"),
        Good("coins", Kind.OLD, 28, "Старинные монеты", "Old coins"),
        Good("candles", Kind.OLD, 12, "Восковые свечи", "Wax candles"),
        Good("scroll", Kind.SCHOLAR, 26, "Свитки", "Scrolls"),
        Good("ink", Kind.SCHOLAR, 32, "Звёздные чернила", "Star ink"),
        Good("quills", Kind.SCHOLAR, 10, "Перья для письма", "Writing quills"),
        Good("cogs", Kind.FORGE, 26, "Гномьи шестерни", "Dwarf cogs"),
        Good("iron", Kind.FORGE, 16, "Кованое железо", "Wrought iron"),
        Good("copper", Kind.FORGE, 20, "Медная проволока", "Copper wire"),
        Good("spice", Kind.BAZAAR, 22, "Пряности", "Spices"),
        Good("silk", Kind.BAZAAR, 38, "Бирюзовый шёлк", "Turquoise silk"),
        Good("rose", Kind.BAZAAR, 30, "Розовое масло", "Rose oil"),
        Good("tea", Kind.GATE, 20, "Заморский чай", "Faraway tea"),
        Good("maps", Kind.GATE, 28, "Карты дальних стран", "Maps of far lands"),
        Good("salt", Kind.GATE, 14, "Соль с караванов", "Caravan salt"),
        Good("bread", Kind.HOMES, 8, "Слободской каравай", "Homestead loaf"),
        Good("yarn", Kind.HOMES, 12, "Крашеная пряжа", "Dyed yarn"),
        Good("jug", Kind.HOMES, 18, "Расписной кувшин", "Painted jug"),
        // the night market's
        Good("pepper", null, 70, "Драконий перец", "Dragon pepper", rare = true),
        Good("starsalt", null, 80, "Звёздная соль", "Star salt", rare = true),
        Good("dust", null, 90, "Пыльца фей", "Fairy dust", rare = true),
        Good("clock", null, 110, "Часы-компас", "Clock compass", rare = true),
        Good("moonlight", null, 130, "Лунный свет в клетке", "Caged moonlight", rare = true),
        Good("musicbox", null, 150, "Шкатулка-яйцо", "Egg music box", rare = true),
    )
    val byId = all.associateBy { it.id }
    operator fun get(id: String) = byId.getValue(id)
    fun of(kind: Kind) = all.filter { it.kind == kind }
    val rare = all.filter { it.rare }

    /** Which kinds pay most for a rare good. */
    fun rareWanted(g: Good): List<Kind> = when (g.id) {
        "pepper" -> listOf(Kind.BAZAAR, Kind.HOMES)
        "starsalt" -> listOf(Kind.WATER, Kind.GATE)
        "dust" -> listOf(Kind.GARDEN, Kind.OLD)
        "clock" -> listOf(Kind.FORGE, Kind.GATE)
        "moonlight" -> listOf(Kind.SCHOLAR, Kind.OLD)
        else -> listOf(Kind.BAZAAR, Kind.SCHOLAR)
    }
}

/** How the merchant travels this time. Only the car's driver is kept off the screen while moving. */
enum class Ride(val id: String, val ru: String, val en: String, val driver: Boolean) {
    CAR("car", "Своя повозка", "My own cart", true),
    BUS("bus", "Омнибус", "Omnibus", false),
    TRAM("tram", "Трамвай", "Tram", false),
    METRO("metro", "Гномьи ходы", "Dwarf tunnels", false),
    PASSENGER("caravan", "Пассажиром", "As a passenger", false);

    val title get() = tr(ru, en)
    val sub get() = when (this) {
        CAR -> tr("за рулём – экран отдыхает", "driving – the screen rests")
        BUS -> tr("автобус, маршрутка", "bus, minibus")
        TRAM -> tr("трамвай, троллейбус", "tram, trolleybus")
        METRO -> tr("метро: от входа до выхода", "underground: in to out")
        PASSENGER -> tr("в чужой машине, в такси", "someone else's car, a taxi")
    }
    val pic get() = "ride_${if (this == PASSENGER) "caravan" else id}"

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: CAR }
}
