package app.caravan.domain

import app.caravan.data.State

/**
 * The book of curios: 60 small wonders found on rides, five for each of twelve kinds of road. A ride offers
 * the curios of what it went through – a garden quarter, the dwarf tunnels, a night, a long road, rain.
 * Order matches tools/curios.py and the art (cur_<id>).
 */
object Curios {
    class Context(val id: String, val ru: String, val en: String, val hintRu: String, val hintEn: String) {
        val title get() = tr(ru, en)
        val hint get() = tr(hintRu, hintEn)
    }

    class Curio(val id: String, val ru: String, val en: String, val context: Context) {
        val name get() = tr(ru, en)
        val pic get() = "cur_$id"
    }

    val CONTEXTS = listOf(
        Context("garden", "Сады", "Gardens", "поездка через садовый квартал", "a ride through a garden quarter"),
        Context("water", "Набережная", "Riverside", "поездка через квартал у воды", "a ride through a riverside quarter"),
        Context("old", "Старый город", "Old town", "поездка через старый квартал", "a ride through an old quarter"),
        Context("scholar", "Учёный квартал", "Scholars", "поездка через учёный квартал", "a ride through a scholars' quarter"),
        Context("forge", "Кузни", "Forges", "поездка через квартал кузниц", "a ride through a forge quarter"),
        Context("bazaar", "Базар", "Bazaar", "поездка через базарный квартал", "a ride through a bazaar quarter"),
        Context("gate", "Ворота", "Gates", "поездка через квартал ворот", "a ride through a gate quarter"),
        Context("homes", "Слобода", "Homesteads", "поездка через слободу", "a ride through a homestead quarter"),
        Context("night", "Ночь", "Night", "поездка после 21:00 или до 5 утра", "a ride after 9 pm or before 5 am"),
        Context("under", "Гномьи ходы", "Dwarf tunnels", "поездка на метро или через квартал кузниц в темноте", "a ride on the underground, or through a forge quarter after dark"),
        Context("road", "Дальняя дорога", "The long road", "поездка от 15 км или за городом", "a ride of 15 km or beyond the city"),
        Context("weather", "Непогода", "Weather", "поездка в дождь, снег или туман", "a ride in rain, snow or fog"),
    )
    private val ctx = CONTEXTS.associateBy { it.id }

    private val raw = listOf(
        "bud" to "Вечный бутон|Eternal bud", "acornbox" to "Жёлудь-шкатулка|Acorn box", "peacock" to "Павлинье перо|Peacock feather",
        "seeds" to "Семена ночной лилии|Night lily seeds", "comb" to "Соты в янтаре|Honeycomb in amber",
        "shell" to "Раковина с шумом моря|Shell with the sea in it", "bottle" to "Письмо в бутылке|Letter in a bottle", "boatcompass" to "Компас лодочника|Boatman's compass",
        "pearls" to "Жемчужная нить|String of pearls", "oar" to "Резное весло|Carved oar",
        "doorkey" to "Ключ от неведомой двери|Key to an unknown door", "bell" to "Бронзовый колокольчик|Bronze bell", "tile" to "Изразец с тюльпаном|Tulip tile",
        "stamp" to "Старинная печать|Old seal stamp", "mosaic" to "Осколок мозаики|Mosaic fragment",
        "astrolabe" to "Астролябия|Astrolabe", "starchart" to "Звёздная карта|Star chart", "specs" to "Очки звездочёта|Stargazer's spectacles",
        "globeink" to "Чернильница-глобус|Globe inkwell", "endless" to "Книга без конца|The endless book",
        "horseshoe" to "Золотая подкова|Golden horseshoe", "windkey" to "Заводной ключ|Wind-up key", "cogbrooch" to "Брошь из шестерёнок|Cog brooch",
        "dagger" to "Кинжал в ножнах|Jewelled dagger", "anvil" to "Наковальня-малютка|Tiny anvil",
        "lamp" to "Лампа джинна|Genie's lamp", "carpet" to "Лоскут ковра|Carpet swatch", "perfume" to "Флакон духов|Perfume flask",
        "beads" to "Бирюзовые бусы|Turquoise beads", "dallah" to "Медный кофейник|Copper coffee pot",
        "ticket" to "Билет на дальний караван|Far caravan ticket", "foreigncoin" to "Монета чужой страны|Foreign coin", "staff" to "Дорожный посох|Walking staff",
        "saddlebag" to "Седельная сумка|Saddlebag", "pass" to "Подорожная грамота|Travel pass",
        "whistle" to "Глиняная свистулька|Clay whistle", "scarf" to "Вышитый платок|Embroidered scarf", "spoon" to "Расписная ложка|Painted spoon",
        "yarnball" to "Клубок волшебной пряжи|Ball of magic yarn", "gingerbread" to "Печатный пряник|Gingerbread",
        "moonshard" to "Осколок луны|Moon shard", "starjar" to "Звезда в банке|Star in a jar", "nightflower" to "Ночной цветок|Night flower",
        "candle" to "Негаснущая свеча|Undying candle", "dreamcatcher" to "Ловец снов|Dreamcatcher",
        "minerlamp" to "Гномий фонарь|Dwarf lamp", "amethyst" to "Друза аметиста|Amethyst cluster", "pickaxe" to "Кирка-малютка|Tiny pickaxe",
        "nugget" to "Самородок|Gold nugget", "tunnelmap" to "Карта подземных ходов|Map of the tunnels",
        "milestone" to "Верстовой камень|Milestone", "wheel" to "Колесо кибитки|Cart wheel", "roaddust" to "Пыль дальних дорог|Dust of far roads",
        "pocketglobe" to "Карманный глобус|Pocket globe", "windrose" to "Роза ветров|Wind rose",
        "raindrop" to "Капля дождя в стекле|Raindrop in glass", "snowflake" to "Нетающая снежинка|Snowflake that never melts", "parasol" to "Бумажный зонтик|Paper parasol",
        "prism" to "Радужная призма|Rainbow prism", "cloudbottle" to "Облако в бутылке|Cloud in a bottle",
    )
    val all: List<Curio> = raw.mapIndexed { i, (id, n) -> Curio(id, n.substringBefore('|'), n.substringAfter('|'), CONTEXTS[i / 5]) }
    val byId = all.associateBy { it.id }
    fun of(c: String) = all.filter { it.context.id == c }

    // ---- full sets (#17): a gift once, and a small lasting favour of the road ----

    const val SET_GOLD = 250
    const val SET_RENOWN = 60
    const val ALL_GOLD = 1000
    const val ALL_RENOWN = 300

    /** What a full set of [c] gives for good. */
    fun perk(c: String): String = when (c) {
        "night" -> tr("на ночном рынке – на одну редкость больше", "one more rarity at the night market")
        "under" -> tr("+5 славы за каждую поездку гномьими ходами", "+5 renown for every ride through the dwarf tunnels")
        "road" -> tr("на поездках от 8 км – две находки на обочине", "two roadside finds on rides of 8 km and more")
        "weather" -> tr("хрупкий товар бьётся вдвое реже", "fragile goods break half as often")
        else -> Kind.of(c)?.let { tr("товары квартала «${it.ru}» продаются на 5% дороже", "goods of «${it.en}» quarters sell 5% dearer") }.orEmpty()
    }

    fun complete(s: State, c: String) = of(c).all { it.id in s.curios }
    fun allFound(s: State) = all.all { it.id in s.curios }

    /** Sale bonus on [g] from full sets of its kind. */
    fun saleBonus(s: State, g: Good) = if (g.kind != null && complete(s, g.kind.id)) 1.05 else 1.0

    /** The keeper of curios: the title for all sixty. */
    val keeper get() = tr("Хранитель диковин", "Keeper of curios")

    /** The contexts a finished ride went through. */
    fun contexts(s: State, t: app.caravan.data.Trip): Set<String> = buildSet {
        t.quarters.distinct().mapNotNull { s.quarters[it]?.kind }.forEach { add(it.id) }
        if (t.night) add("night")
        // gnomes come up in the forges at night, so a city with no underground has its tunnels too
        if (t.underground > 0 || Ride.of(t.ride) == Ride.METRO || (t.night && t.quarters.any { s.quarters[it]?.kind == Kind.FORGE })) add("under")
        if (t.km >= 15 || t.outside) add("road")
        if (t.weather.isNotEmpty()) add("weather")
    }

    /**
     * Maybe a curio for this ride: about one ride in three, more on rides through many kinds of places. Unfound ones
     * first; once a context is complete its curios don't repeat.
     */
    fun roll(s: State, t: app.caravan.data.Trip): Curio? {
        val cs = contexts(s, t)
        if (cs.isEmpty() || t.km < 0.5) return null
        val r = Rand(hash("curio:${t.start}"))
        val chance = 0.25 + 0.05 * cs.size
        if (r.next() > chance) return null
        val pool = all.filter { it.context.id in cs && it.id !in s.curios }
        return pool.getOrNull(r.int(pool.size.coerceAtLeast(1)))
    }
}
