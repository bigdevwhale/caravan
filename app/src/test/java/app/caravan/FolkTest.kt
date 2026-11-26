package app.caravan

import app.caravan.data.Errand
import app.caravan.data.Lot
import app.caravan.data.Quarter
import app.caravan.data.State
import app.caravan.data.json
import app.caravan.data.stateOf
import app.caravan.domain.Curios
import app.caravan.domain.Folk
import app.caravan.domain.Goods
import app.caravan.domain.Kind
import app.caravan.domain.Market
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class FolkTest {
    private val home = Quarter("1", "Сады", "Gardens", 55.75, 37.60, Kind.GARDEN, Kind.HOMES, visits = 5, firstDay = 1)
    private val far = Quarter("2", "Кузни", "Forges", 55.78, 37.66, Kind.FORGE, Kind.GATE, visits = 1, firstDay = 2)
    private val near = Quarter("3", "Базар", "Bazaar", 55.751, 37.601, Kind.BAZAAR, Kind.OLD, visits = 3, firstDay = 3, changes = 1)
    private val s = State(quarters = listOf(home, far, near).associateBy { it.id }, here = "1")

    @Test fun residentsSettleInOftenVisitedQuarters() {
        val t = Folk.settle(s)
        assertEquals(setOf("1", "3"), t.folk.map { it.quarter }.toSet())
        assertEquals(2, t.folk.map { it.who }.distinct().size)
        // settling again adds nobody
        assertEquals(t.folk, Folk.settle(t).folk)
    }

    @Test fun letterGoesFarAndCountsFromAnywhere() {
        val letterWho = Folk.all.indexOfFirst { it.steps[0].ask == Folk.Ask.Letter }
        val e = Errand("1", letterWho, since = 1)
        assertEquals("2", Folk.pickTarget(s, e))
        val sent = e.copy(target = "2")
        assertFalse(Folk.ready(s.copy(folk = listOf(sent)), sent))
        val arrived = sent.copy(arrived = true)
        assertTrue(Folk.ready(s.copy(folk = listOf(arrived), here = "3"), arrived))
    }

    @Test fun untakenLetterNamesTheQuarterItWillGoTo() {
        val who = Folk.all.indexOfFirst { it.steps[2].ask == Folk.Ask.Letter }
        // the previous letter's quarter is left in target after a claim: the next untaken one must not name it
        val e = Errand("1", who, step = 2, target = "3")
        val name = s.quarters[Folk.pickTarget(s, e)]!!.name
        assertTrue(Folk.text(s, e), Folk.text(s, e).contains(name))
    }

    @Test fun goodsOnlyInTheResidentsQuarter() {
        val who = Folk.all.indexOfFirst { (it.steps[0].ask as? Folk.Ask.Bring)?.good == "silk" }
        val e = Errand("1", who, since = 1)
        val pack = listOf(Lot(1, "silk", "3", 30, 0), Lot(2, "silk", "3", 30, 0), Lot(3, "honey", "1", 10, 0))
        assertTrue(Folk.ready(s.copy(pack = pack, folk = listOf(e)), e))
        assertFalse(Folk.ready(s.copy(pack = pack, folk = listOf(e), here = "2"), e))
        assertFalse(Folk.ready(s.copy(pack = pack.drop(1), folk = listOf(e)), e))
    }

    @Test fun folkAndChangesSurviveJson() {
        val t = Folk.settle(s).let { it.copy(folk = it.folk.map { e -> e.copy(since = 5, target = "2", arrived = true, step = 1) }) }
        val back = stateOf(t.json())
        assertEquals(t.folk, back.folk)
        assertEquals(1, back.quarters["3"]!!.changes)
    }

    @Test fun fullSetSellsDearer() {
        val lot = Lot(1, "honey", "1", 17, 0)
        val before = Market.price(lot, far, s, 102)
        val after = Market.price(lot, far, s.copy(curios = Curios.of("garden").map { it.id }.toSet()), 102)
        assertTrue("$before → $after", after > before)
        assertEquals(1.0, Curios.saleBonus(s, Goods["iron"]), 0.0)
    }
}
