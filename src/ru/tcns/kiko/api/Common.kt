package test.kiko.ru.tcns.kiko.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.utils.wrapHeaders
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext

class HttpException(val code: HttpStatusCode, val description: String = code.description) :
    RuntimeException(description)

fun httpException(code: HttpStatusCode, message: String = code.description): Nothing =
    throw HttpException(code, message)

fun httpException(code: Int, message: String = "Error $code"): Nothing =
    throw HttpException(HttpStatusCode(code, message))

fun <T> T.checkRequest(cond: Boolean, callback: () -> String) {
    if (!cond) httpException(HttpStatusCode.BadRequest, callback())
}

suspend fun PipelineContext<Unit, ApplicationCall>.respond(
    result: Any?, status: HttpStatusCode = HttpStatusCode.OK,
    customHeaders: Headers = Headers.Empty
) {
    customHeaders.forEach {k,v ->
        v.forEach {
            call.response.header(k,it)
        }
    }
    call.respondText(result?.let { Json.stringify(result) } ?: "", ContentType.Application.Json, status)
}

object Json {
    @PublishedApi
    internal val objectMapper = jacksonObjectMapper()

    fun <T> convert(value: Any?, clazz: Class<T>): T = objectMapper.convertValue(value, clazz)
    fun <T> parse(str: String, clazz: Class<T>): T = objectMapper.readValue(str, clazz)
    fun <T> stringify(value: T): String = objectMapper.writeValueAsString(value)
}