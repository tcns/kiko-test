package test.kiko

import test.kiko.ru.tcns.kiko.mock.adjustTimeSlot
import test.kiko.ru.tcns.kiko.mock.adjustTimeSlotSecs
import test.kiko.ru.tcns.kiko.mock.freeSlots
import test.kiko.ru.tcns.kiko.mock.nextWeekStart
import java.time.LocalDateTime
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitTests {

    @Test
    fun `it correctly adjusts time slot`() {
        val time = 1574111615

        val expect = LocalDateTime.of(2019, Month.NOVEMBER, 18, 21, 0, 0)
        assertEquals(expect, time.adjustTimeSlot())
        assertEquals(1574110800, time.adjustTimeSlotSecs())
    }

    @Test
    fun `it correctly returns free slots`() {
        val freeSlots = freeSlots(
            nextWeekStart(),
            nextWeekStart().plusMinutes(59)
        )
        assertEquals(3, freeSlots.size)
    }
}