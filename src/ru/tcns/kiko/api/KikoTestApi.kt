package ru.tcns.kiko.api

import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import test.kiko.ru.tcns.kiko.mock.DURATION_MINUTES

/**
 * Kiko test api
 * 
 * Kiko test api. Because of lack of authorization. Current tenant id will be landlord
 */

    /**
     * filter timeslots
     * 
     * @param from from filtering date in unix second
     * @param to to filtering date in unix second
     * @param tenantIds filter by tenantids comma separated
     * @param freeOnly return only available timeslots. Ignored when tenantIds provided
     * @param page page of pagination response
     * @param pageSize page of pagination response
     * 
     * @return search results matching criteria
     */
    @KtorExperimentalLocationsAPI
    @Location("/timeslot")
    data class TimeSlotRequest(
        val from: Int = 0,
        val to: Int = 1974106617,
        val tenantIds: String = "",
        val freeOnly: Boolean = false,
        val page: Int = 0,
        val pageSize: Int = 20
    )

    /**
     * get timeslot by time request
     * 
     * @param time timeslot time
     * @param tenantId new tenant id
     *
     * @return search results matching criteria
     */
    @KtorExperimentalLocationsAPI
    @Location("/timeslot/{time}")
    data class TimeSlotByTime(val time: Int = 0, val tenantId: String = "")




enum class TimeSlotStatus {
    BLOCKED, VACANT, RESERVED, ACCEPTED
}


data class TimeSlot(
    val date: Int = 0,
    val duration: Int = DURATION_MINUTES,
    val status: TimeSlotStatus = TimeSlotStatus.VACANT,
    val tenantId: String? = null
)

