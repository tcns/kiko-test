package test.kiko.ru.tcns.kiko.mock

import ru.tcns.kiko.api.TimeSlot
import ru.tcns.kiko.api.TimeSlotStatus
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.HashMap

const val START_HOUR = 8
const val END_HOUR = 20
const val DURATION_MINUTES = 20
const val CURRENT_TENANT_ID = "landlord"


class DbMock {

    @Synchronized
    fun createTimeSlot(timeSlot: TimeSlot): TimeSlot {
        timeSlots[timeSlot.date] = timeSlot
        return timeSlot
    }

    fun findTimeSlot(date: Int) = timeSlots[date]

    fun findTimeSlotBetween(from: Int, to: Int) = timeSlots.filter { it.key in from..to }.values

    @Synchronized
    fun deleteTimeSlot(date: Int) = timeSlots.remove(date)

    fun contains(date: Int) = timeSlots.containsKey(date)

    fun clear() = timeSlots.clear()

    @Synchronized
    fun clearBefore(date: Int) {
        val future = timeSlots.filter { it.key >= date }
        timeSlots.clear()
        timeSlots.putAll(future)
    }

    private val timeSlots: MutableMap<Int, TimeSlot> = HashMap()
}


fun freeSlots(from: LocalDateTime, to: LocalDateTime): MutableList<TimeSlot> {
    val fromSecond = from.toInstant(ZoneOffset.UTC).epochSecond.toInt().adjustTimeSlotSecs()
    val toSecond = to.toInstant(ZoneOffset.UTC).epochSecond.toInt().adjustTimeSlotSecs()
    val dbMock = DbMock()
    val freeSlots = mutableListOf<TimeSlot>()
    for (i in fromSecond..toSecond step DURATION_MINUTES * 60) {
        val localDateTime = LocalDateTime.from(Date(i.toLong() * 1000).toInstant().atZone(ZoneOffset.UTC))
        if (localDateTime.inAvailableRange()) {
            if (!dbMock.contains(i)) {
                freeSlots.add(
                    TimeSlot(
                        status = TimeSlotStatus.VACANT,
                        date = i,
                        tenantId = null,
                        duration = DURATION_MINUTES
                    )
                )
            }
        }
    }
    return freeSlots

}