package test.kiko.ru.tcns.kiko.mock

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.*

fun nextWeekStart(): LocalDateTime = LocalDateTime.now()
    .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    .withMinute(0)
    .withSecond(0)
    .withHour(START_HOUR)

fun nextWeekEnd(): LocalDateTime = nextWeekStart()
    .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
    .withHour(END_HOUR)

fun Int.adjustTimeSlot(): LocalDateTime {
    val localDateTime = LocalDateTime.from(Date(this.toLong() * 1000).toInstant().atZone(ZoneOffset.UTC)).withSecond(0)
    val minute = localDateTime.minute - localDateTime.minute % DURATION_MINUTES
    return localDateTime.withMinute(minute)
}

fun Int.adjustTimeSlotSecs() = this.adjustTimeSlot().toInstant(ZoneOffset.UTC).epochSecond.toInt()

//since tomorrow between 8 and 20
fun LocalDateTime.inAvailableRange() = this.hour in START_HOUR until END_HOUR
        && this.isAfter(LocalDateTime.now().plusDays(1).withHour(7))