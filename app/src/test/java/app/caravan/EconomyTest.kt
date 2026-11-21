package app.caravan

import app.caravan.data.Lot
import app.caravan.data.Quarter
import app.caravan.data.State
import app.caravan.data.json
import app.caravan.data.stateOf
import app.caravan.domain.Goods
import app.caravan.domain.Kind
import app.caravan.domain.Market
import app.caravan.domain.Orders
import app.caravan.domain.Voronoi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class EconomyTest {
    private val garden = Quarter("1", "Сады", "Gardens", 55.75, 37.60, Kind.GARDEN, Kind.HOMES, visits = 5, firstDay = 1)
    private val forge = Quarter("2", "Кузни", "Forges", 55.78, 37.66, Kind.FORGE, Kind.GATE, visits = 5, firstDay = 1)
    private val bazaar = Quarter("3", "Базар", "Bazaar", 55.74, 37.62, Kind.BAZAAR, Kind.OLD, visits = 1, firstDay = 1)
    private val s = State(quarters = listOf(garden, forge, bazaar).associateBy { it.id }, day = 102)

    @Test fun honeySellsDearerInForgesThanAtHome() {
        val lot = Lot(1, "honey", garden.id, 17, 0)
        val home = Market.price(lot, garden, s, 102)
        val away = Market.price(lot, forge, s, 102)
        assertTrue("home $home away $away", away > home * 1.8)
        assertTrue(away > lot.paid)
    }

    @Test fun repeatedSalesSag() {
        val lot = Lot(1, "honey", garden.id, 17, 0)
        val first = Market.price(lot, forge, s, 102)
        val third = Market.price(lot, forge, s.copy(traded = mapOf("s:2:honey" to 2)), 102)
        assertTrue(third < first * 0.8)
    }

    @Test fun stallsRunOut() {
        val o = Market.sells(garden, s.copy(traded = mapOf("b:1:honey" to Market.STOCK)), 102).first { it.good.id == "honey" }
        assertEquals(0, o.stock)
        assertEquals(4, Market.sells(garden, s, 102).size)
    }

    @Test fun stateRoundTrips() {
        val t = s.copy(pack = listOf(Lot(3, "silk", "3", 25, 9, auto = true)), traded = mapOf("b:1:honey" to 2), orders = setOf("100:2"))
        val back = stateOf(t.json())
        assertEquals(t.pack, back.pack)
        assertEquals(t.traded, back.traded)
        assertEquals(t.quarters, back.quarters)
        assertEquals(t.orders, back.orders)
    }

    @Test fun voronoiCellsContainTheirPlace() {
        val cells = Voronoi.cells(s.quarters.values)
        assertEquals(3, cells.size)
        cells.values.forEach { assertTrue(it.size >= 3) }
    }

    @Test fun ordersAreThreeAndStable() {
        val a = Orders.of(s, 200); val b = Orders.of(s, 200)
        assertEquals(3, a.size)
        assertEquals(a.map { it.index }, b.map { it.index })
    }

    @Test fun everyGoodHasAKindOrIsRare() {
        Goods.all.forEach { assertTrue(it.rare || it.kind != null) }
        Kind.entries.forEach { assertEquals(3, Goods.of(it).size) }
    }
}

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class WorldTest {
    @Test fun sixtyUniqueCurios() {
        val ids = app.caravan.domain.Curios.all.map { it.id }
        assertEquals(60, ids.size); assertEquals(60, ids.toSet().size)
        app.caravan.domain.Curios.CONTEXTS.forEach { assertEquals(5, app.caravan.domain.Curios.of(it.id).size) }
    }

    @Test fun eightMerchantsFourSteps() {
        assertEquals(8, app.caravan.domain.Merchants.all.size)
        app.caravan.domain.Merchants.all.forEach { assertEquals(4, it.steps.size) }
    }

    @Test fun eventsDontRepeatWeekToWeek() {
        for (w in 0 until 60) {
            val d = 20000L + w * 7
            assertTrue(app.caravan.domain.Events.of(d).id != app.caravan.domain.Events.of(d - 7).id)
        }
    }

    @Test fun perishablesGoOff() {
        val day = 86_400_000L
        val fresh = app.caravan.domain.Risk.freshness(Lot(1, "fish", "1", 8, 0), now = day / 2)
        val old = app.caravan.domain.Risk.freshness(Lot(1, "fish", "1", 8, 0), now = day * 3)
        assertEquals(1.0, fresh, 0.0); assertTrue(old < 0.6)
        assertEquals(1.0, app.caravan.domain.Risk.freshness(Lot(1, "silk", "1", 8, 0), now = day * 9), 0.0)
    }

    @Test fun stallSellsEveryFourHours() {
        val q = Quarter("1", "A", "A", 55.0, 37.0, Kind.FORGE, Kind.GATE, 3, 1)
        val sh = app.caravan.domain.Shop("1", listOf(Lot(1, "honey", "2", 10, 0), Lot(2, "herbs", "2", 10, 0), Lot(3, "resin", "2", 10, 0)), 0, 0)
        val s = State(quarters = mapOf("1" to q, "2" to q.copy(id = "2", kind = Kind.GARDEN, lat = 55.05)), shops = listOf(sh))
        val after = app.caravan.domain.Shops.tick(s, now = app.caravan.domain.Shops.EVERY * 2 + 10)
        assertEquals(1, after.shops[0].stock.size)
        assertTrue(after.shops[0].till > 0)
    }

    @Test fun twoContractOffers() {
        assertEquals(2, app.caravan.domain.Contracts.offers(State(), 20500).size)
    }
}
