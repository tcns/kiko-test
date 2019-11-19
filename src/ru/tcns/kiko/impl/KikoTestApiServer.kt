package ru.tcns.kiko.impl

import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.delete
import io.ktor.locations.get
import io.ktor.locations.put
import io.ktor.routing.Route
import ru.tcns.kiko.api.TimeSlot
import ru.tcns.kiko.api.TimeSlotByTime
import ru.tcns.kiko.api.TimeSlotRequest
import ru.tcns.kiko.api.TimeSlotStatus
import test.kiko.ru.tcns.kiko.api.checkRequest
import test.kiko.ru.tcns.kiko.api.httpException
import test.kiko.ru.tcns.kiko.api.respond
import test.kiko.ru.tcns.kiko.mock.*


@KtorExperimentalLocationsAPI
fun Route.timeslot(dbMock: DbMock) {

    get<TimeSlotRequest> { request ->
        checkRequest(request.from >= 0) { "Invalid from date" }
        checkRequest(request.to >= 0) { "Invalid to date" }

        var slots =
            if (request.tenantIds.isBlank() && request.freeOnly) {
                freeSlots(nextWeekStart(), nextWeekEnd())
            } else {
                var filtered = dbMock.findAllTimeSlots()
                    .filter { it.date in request.from..request.to }
                if (request.tenantIds.isNotBlank()) {
                    filtered = filtered.filter { request.tenantIds.split(",").contains(it.tenantId) }
                }
                filtered

            }

        respond(
            result = slots
                .drop(request.page * request.pageSize)
                .take(request.pageSize),
            customHeaders = headersOf("X-total-pages", (slots.size / request.pageSize).toString())
        )
    }

    /**
     * get timeslot by time request
     *
     * @param time timeslot time
     * @param tenantId new tenant id
     *
     * @return search results matching criteria
     */

    get<TimeSlotByTime> { request ->
        val adjustedTime = request.time.adjustTimeSlot()
        val adjustedTimeSec = request.time.adjustTimeSlotSecs()
        checkRequest(adjustedTime.inAvailableRange()) { INVALID_TIME_MESSAGE }
        respond(dbMock.findTimeSlot(adjustedTimeSec) ?: TimeSlot(date = adjustedTimeSec))
    }

    /**
     * reserve timeslot by tenant
     *
     * @param time timeslot time
     * @param tenantId new tenant id
     *
     * @return returned timeslot
     */
    put<TimeSlotByTime> { request ->
        val adjustedTimeSec = request.time.adjustTimeSlotSecs()
        checkRequest(request.time.adjustTimeSlot().inAvailableRange()) { INVALID_TIME_MESSAGE }
        val timeSlot = dbMock.findTimeSlot(adjustedTimeSec)

        if (request.tenantId == CURRENT_TENANT_ID && isTaken(timeSlot)) {
            val slot =
                TimeSlot(date = adjustedTimeSec, status = TimeSlotStatus.ACCEPTED, tenantId = timeSlot!!.tenantId)
            respond(dbMock.createTimeSlot(slot))
            return@put
        }

        if (isTaken(timeSlot)) {
            httpException(
                HttpStatusCode.Forbidden,
                "Time Slot already taken"
            )
        }

        val slot = TimeSlot(date = adjustedTimeSec, status = TimeSlotStatus.RESERVED, tenantId = request.tenantId)
        respond(dbMock.createTimeSlot(slot))
    }

    /**
     * cancel reservation by tenant id
     *
     * @param time timeslot time
     * @param tenantId new tenant or current tenant id. If current then it will be blocked
     *
     * @return returned timeslot
     */

    delete<TimeSlotByTime> { request ->
        val adjustedTime = request.time.adjustTimeSlot()
        val adjustedTimeSec = request.time.adjustTimeSlotSecs()
        checkRequest(adjustedTime.inAvailableRange()) { INVALID_TIME_MESSAGE }

        if (request.tenantId == CURRENT_TENANT_ID) {
            //block timeslot but save tenant id for further unblocking if it will be needed
            val tenantId = dbMock.findTimeSlot(adjustedTimeSec)?.tenantId
            dbMock.createTimeSlot(
                TimeSlot(
                    date = adjustedTimeSec,
                    status = TimeSlotStatus.BLOCKED,
                    tenantId = tenantId
                )
            )
            respond(null)
            return@delete
        }

        val timeSlot = dbMock.findTimeSlot(adjustedTimeSec)

        if (timeSlot == null || timeSlot.tenantId != request.tenantId) httpException(
            HttpStatusCode.Forbidden,
            "Editing of this time slot is unavailable"
        )

        dbMock.deleteTimeSlot(request.time)
        respond(null)
    }

}

private fun isTaken(timeSlot: TimeSlot?) =
    timeSlot != null && timeSlot.status != TimeSlotStatus.VACANT


const val INVALID_TIME_MESSAGE = "Time is not in available range $START_HOUR hour until $END_HOUR"