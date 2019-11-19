package test.kiko

import ru.tcns.kiko.api.TimeSlot
import ru.tcns.kiko.api.TimeSlotStatus
import test.kiko.ru.tcns.kiko.mock.DbMock
import test.kiko.ru.tcns.kiko.mock.adjustTimeSlotSecs
import test.kiko.ru.tcns.kiko.mock.nextWeekStart
import java.time.ZoneOffset

val slot1 = getSlot(0)
val slot2 = getSlot(30)
val slot3 = getSlot(60)
val slot4 = getSlot(88)

fun addTestData(dbMock: DbMock) {
    dbMock.createTimeSlot(
        TimeSlot(
            date = slot1,
            status = TimeSlotStatus.RESERVED,
            tenantId = "t1"
        )
    )
    dbMock.createTimeSlot(
        TimeSlot(
            date = slot2,
            status = TimeSlotStatus.RESERVED,
            tenantId = "t2"
        )
    )

    dbMock.createTimeSlot(
        TimeSlot(
            date = slot3,
            status = TimeSlotStatus.RESERVED,
            tenantId = "t3"
        )
    )

    dbMock.createTimeSlot(
        TimeSlot(
            date = slot4,
            status = TimeSlotStatus.RESERVED,
            tenantId = "t4"
        )
    )
}

private fun getSlot(delay: Long) =
    nextWeekStart().plusMinutes(delay).toEpochSecond(ZoneOffset.UTC).toInt().adjustTimeSlotSecs()