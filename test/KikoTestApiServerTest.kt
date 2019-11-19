package test.kiko

import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI
import ru.tcns.kiko.api.TimeSlot
import ru.tcns.kiko.api.TimeSlotStatus
import test.kiko.ru.tcns.kiko.api.Json
import test.kiko.ru.tcns.kiko.mock.CURRENT_TENANT_ID
import test.kiko.ru.tcns.kiko.mock.DbMock
import kotlin.test.Test
import kotlin.test.assertEquals


@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
class RoutesTest {
    /**
     * @see KikoTestApiServer.getTimeslot
     */
    @Test
    fun `it correctly returns answer on simple get request`() {
        withTestApplication {
            val dbMock = DbMock()
            addTestData(dbMock)
            handleRequest(HttpMethod.Get, "/timeslot?tenantIds=t1,t2").apply {
                assertEquals(2, getContentList().size)
                assertEquals(HttpStatusCode.OK, response.status())
            }
            handleRequest(HttpMethod.Get, "/timeslot?to=-1").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }


    /**
     * @see KikoTestApiServer.getTimeslotId
     */
    @Test
    fun `it correctly returns specific timeslot on request by time or throw error`() {
        withTestApplication {
            handleRequest(HttpMethod.Get, "/timeslot/${slot3 + 100}").apply {
                assertEquals("t3", getContent().tenantId)
                assertEquals(HttpStatusCode.OK, response.status())
            }

            handleRequest(HttpMethod.Get, "/timeslot/${slot4 + 25 * 60}").apply {
                assertEquals(TimeSlotStatus.VACANT, getContent().status)
                assertEquals(HttpStatusCode.OK, response.status())
            }

            handleRequest(HttpMethod.Get, "/timeslot/999").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    /**
     * @see KikoTestApiServer.putTimeslotId
     */
    @Test
    fun `it throws errors on unavailable slots`() {
        withTestApplication {
            handleRequest(HttpMethod.Put, "/timeslot/${slot3 + 100}?tenantId=t4").apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
            handleRequest(HttpMethod.Put, "/timeslot/999?tenantId=t4").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun `it correctly reserves specific timeslot`() {
        withTestApplication {
            handleRequest(HttpMethod.Put, "/timeslot/${slot4 + 50 * 60}?tenantId=t4").apply {
                assertEquals(TimeSlotStatus.RESERVED, getContent().status)
                assertEquals(HttpStatusCode.OK, response.status())
            }

            handleRequest(HttpMethod.Put, "/timeslot/${slot4 + 50 * 60}?tenantId=$CURRENT_TENANT_ID").apply {
                assertEquals(TimeSlotStatus.ACCEPTED, getContent().status)
                assertEquals(HttpStatusCode.OK, response.status())
            }

            handleRequest(HttpMethod.Get, "/timeslot?tenantIds=t4").apply {
                assertEquals(2, getContentList().size)
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    /**
     * @see KikoTestApiServer.deleteTimeslotId
     */
    @Test
    fun `it handles deletion routine`() {
        withTestApplication {
            val slot = slot4 + 50 * 60

            //reserve slot
            handleRequest(HttpMethod.Put, "/timeslot/$slot?tenantId=t4")

            //block slot
            handleRequest(HttpMethod.Delete, "/timeslot/$slot?tenantId=$CURRENT_TENANT_ID").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            //forbidden
            handleRequest(HttpMethod.Put, "/timeslot/$slot?tenantId=t4").apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }

            //unblock
            handleRequest(HttpMethod.Put, "/timeslot/$slot?tenantId=$CURRENT_TENANT_ID").apply {
                assertEquals(TimeSlotStatus.ACCEPTED, getContent().status)
                assertEquals(HttpStatusCode.OK, response.status())
            }

            //slot is accepted
            handleRequest(HttpMethod.Get, "/timeslot/$slot").apply {
                assertEquals("t4", getContent().tenantId)
                assertEquals(TimeSlotStatus.ACCEPTED, getContent().status)
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun `it deletes own slot`() {
        withTestApplication {

            //delete slot
            handleRequest(HttpMethod.Delete, "/timeslot/$slot2?tenantId=t2").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            //reserve slot
            handleRequest(HttpMethod.Put, "/timeslot/$slot2?tenantId=t3").apply {
                assertEquals("t3", getContent().tenantId)
                assertEquals(TimeSlotStatus.RESERVED, getContent().status)
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }


    @Test
    fun `it prevents of deleting another slot`() {
        withTestApplication {
            //delete slot
            handleRequest(HttpMethod.Delete, "/timeslot/$slot3?tenantId=t2").apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }

            //reserve slot
            handleRequest(HttpMethod.Get, "/timeslot/$slot3").apply {
                assertEquals("t3", getContent().tenantId)
                assertEquals(TimeSlotStatus.RESERVED, getContent().status)
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }


    @KtorExperimentalLocationsAPI
    @KtorExperimentalAPI
    fun <R> withTestApplication(test: TestApplicationEngine.() -> R): R {
        return withApplication(createTestEnvironment()) {
            (environment.config as MapApplicationConfig).apply {
                put("scheduler.notification", "10")
                put("scheduler.deletion", "10")
            }
            val dbMock = DbMock()
            dbMock.clear()
            addTestData(dbMock)
            application.module(dbMock)
            test()
        }
    }

    fun TestApplicationRequest.setBodyJson(value: Any?) = setBody(Json.stringify(value))

    @Suppress("UNCHECKED_CAST")
    private fun TestApplicationCall.getContentList() =
        Json.parse(response.content!!, List::class.java) as List<TimeSlot>

    private fun TestApplicationCall.getContent() =
        Json.parse(response.content!!, TimeSlot::class.java)

}
