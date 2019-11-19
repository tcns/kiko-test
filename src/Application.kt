package test.kiko

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.jackson.jackson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import ru.tcns.kiko.impl.timeSlot

import test.kiko.ru.tcns.kiko.Configuration
import test.kiko.ru.tcns.kiko.api.HttpException
import test.kiko.ru.tcns.kiko.impl.DeletionWorker
import test.kiko.ru.tcns.kiko.impl.NotificationWorker
import test.kiko.ru.tcns.kiko.mock.DbMock

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
fun Application.module(dbMock: DbMock = DbMock()) {

    val config = environment.config.config("scheduler")
    val schedulerConfig = Configuration(
        schedulerCheckSec = config.property("notification").getString().toInt(),
        deletionCheckSec = config.property("deletion").getString().toInt()
    )
    val nWorker = NotificationWorker()
    val dWorker = DeletionWorker()

    environment.monitor.subscribe(ApplicationStarted) {
        nWorker.schedule(dbMock, schedulerConfig)
        dWorker.schedule(dbMock, schedulerConfig)
    }
    environment.monitor.subscribe(ApplicationStopped) {
        nWorker.cancel()
        dWorker.cancel()
    }

    install(Locations)
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    routing {
        get("/") {
            call.respondText("Kiko Test", contentType = ContentType.Text.Plain)
        }

        install(StatusPages) {
            exception<HttpException> { cause ->
                call.respond(cause.code, cause.description)
            }
        }
        timeSlot(dbMock)
    }
}

