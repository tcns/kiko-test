package ru.tcns.kiko.impl

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.delete
import io.ktor.locations.get
import io.ktor.locations.put
import io.ktor.routing.Route
import io.ktor.util.pipeline.PipelineContext
import ru.tcns.kiko.api.TimeSlot
import ru.tcns.kiko.api.TimeSlotByTime
import ru.tcns.kiko.api.TimeSlotRequest
import ru.tcns.kiko.api.TimeSlotStatus
import test.kiko.ru.tcns.kiko.api.HttpException
import test.kiko.ru.tcns.kiko.api.checkRequest
import test.kiko.ru.tcns.kiko.api.httpException
import test.kiko.ru.tcns.kiko.api.respond
import test.kiko.ru.tcns.kiko.mock.*


@KtorExperimentalLocationsAPI
fun Route.timeSlot(dbMock: DbMock, notificationMock: NotificationMock) {

    get<TimeSlotRequest> { request ->
        try {
            handleGet(request, dbMock)
        } catch (ex: HttpException) {
            respond(ex)
        }

    }

    /**
     * get timeSlot by time request
     *
     * @return search results matching criteria
     */

    get<TimeSlotByTime> { request ->
        try {
            handleGetByTime(request, dbMock)
        } catch (ex: HttpException) {
            respond(ex)
        }
    }

    /**
     * reserve timeSlot by tenant
     *
     * @return returned timeslot
     */
    put<TimeSlotByTime> { request ->
        try {
            handlePut(request, dbMock, notificationMock)
        } catch (ex: HttpException) {
            respond(ex)
        }
    }

    /**
     * cancel reservation by tenant id
     *
     * @return returned timeslot
     */

    delete<TimeSlotByTime> { request ->
        try {
            handleDelete(request, dbMock, notificationMock)
        } catch (ex: HttpException) {
            respond(ex)
        }

    }
}


@KtorExperimentalLocationsAPI
private suspend fun PipelineContext<Unit, ApplicationCall>.handleGet(
    request: TimeSlotRequest,
    dbMock: DbMock
) {
    checkRequest(request.from >= 0) { "Invalid from date" }
    checkRequest(request.to >= 0) { "Invalid to date" }

    val slots =
        if (request.freeOnly) freeSlots(nextWeekStart(), nextWeekEnd())
        else filterTimeSlots(dbMock, request)
    respond(
        result = slots
            .drop(request.page * request.pageSize)
            .take(request.pageSize),
        customHeaders = headersOf("X-total-pages", (slots.size / request.pageSize).toString())
    )
}

@KtorExperimentalLocationsAPI
private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetByTime(
    request: TimeSlotByTime,
    dbMock: DbMock
) {
    val adjustedTimeSec = request.time.adjustTimeSlotSecs()
    checkRequest(request.time.adjustTimeSlot().inAvailableRange()) { INVALID_TIME_MESSAGE }
    respond(dbMock.findTimeSlot(adjustedTimeSec) ?: TimeSlot(date = adjustedTimeSec))
}

@KtorExperimentalLocationsAPI
private suspend fun PipelineContext<Unit, ApplicationCall>.handlePut(
    request: TimeSlotByTime,
    dbMock: DbMock,
    notificationMock: NotificationMock
) {
    val adjustedTimeSec = request.time.adjustTimeSlotSecs()
    checkRequest(request.time.adjustTimeSlot().inAvailableRange()) { INVALID_TIME_MESSAGE }
    val timeSlot = dbMock.findTimeSlot(adjustedTimeSec)

    if (request.tenantId == CURRENT_TENANT_ID && isTaken(timeSlot)) {
        val slot = dbMock.createTimeSlot(
            TimeSlot(
                date = adjustedTimeSec,
                status = TimeSlotStatus.ACCEPTED,
                tenantId = timeSlot!!.tenantId
            )
        )
        notificationMock.notifyTenant(slot)
        respond(slot)
        return
    }

    if (isTaken(timeSlot))
        httpException(HttpStatusCode.Forbidden, "Time Slot already taken")

    val slot = TimeSlot(date = adjustedTimeSec, status = TimeSlotStatus.RESERVED, tenantId = request.tenantId)
    respond(dbMock.createTimeSlot(slot))
}

@KtorExperimentalLocationsAPI
private suspend fun PipelineContext<Unit, ApplicationCall>.handleDelete(
    request: TimeSlotByTime,
    dbMock: DbMock,
    notificationMock: NotificationMock
) {
    val adjustedTimeSec = request.time.adjustTimeSlotSecs()
    checkRequest(request.time.adjustTimeSlot().inAvailableRange()) { INVALID_TIME_MESSAGE }

    if (request.tenantId == CURRENT_TENANT_ID) {
        //block timeslot but save tenant id for further unblocking if it will be needed
        val tenantId = dbMock.findTimeSlot(adjustedTimeSec)?.tenantId


        val slot = dbMock.createTimeSlot(
            TimeSlot(
                date = adjustedTimeSec,
                status = TimeSlotStatus.BLOCKED,
                tenantId = tenantId
            )
        )
        if (tenantId != null) {
            notificationMock.notifyTenant(slot)
        }
        respond()
        return
    }

    val timeSlot = dbMock.findTimeSlot(adjustedTimeSec)

    if (timeSlot == null || timeSlot.tenantId != request.tenantId)
        httpException(HttpStatusCode.Forbidden, "Editing of this time slot is unavailable")

    dbMock.deleteTimeSlot(request.time)
    respond()
}


@KtorExperimentalLocationsAPI
private fun filterTimeSlots(dbMock: DbMock, request: TimeSlotRequest) =
    dbMock
        .findTimeSlotBetween(request.from, request.to).let {
            if (request.tenantIds.isNotBlank()) {
                it.filter { ts -> request.tenantIds.split(",").contains(ts.tenantId) }
            } else {
                it
            }
        }

private fun isTaken(timeSlot: TimeSlot?) =
    timeSlot != null && timeSlot.status != TimeSlotStatus.VACANT


const val INVALID_TIME_MESSAGE = "Time is not in available range $START_HOUR hour until $END_HOUR"