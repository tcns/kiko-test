package test.kiko.ru.tcns.kiko.impl

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import test.kiko.ru.tcns.kiko.Configuration
import test.kiko.ru.tcns.kiko.mock.DbMock
import test.kiko.ru.tcns.kiko.mock.NotificationMock
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.coroutines.CoroutineContext

class NotificationWorker : CoroutineScope {
    val LOG = LoggerFactory.getLogger(javaClass)
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    fun cancel() {
        job.cancel()
    }

    fun schedule(dbMock: DbMock, configuration: Configuration) = launch {
        val notifier = NotificationMock()
        while (true) {
            delay(configuration.schedulerCheckSec.toLong() * 1000)
            val tenants = dbMock.findTimeSlotBetween(
                LocalDateTime.now().plusHours(12).toEpochSecond(ZoneOffset.UTC).toInt(),
                LocalDateTime.now().plusHours(24).toEpochSecond(ZoneOffset.UTC).toInt()
            )
            if (tenants.isEmpty()) {
                LOG.info("No tenants to notify")
            }
            tenants.forEach { notifier.notifyTenant(it) }

        }
    }
}

class DeletionWorker : CoroutineScope {
    val LOG = LoggerFactory.getLogger(javaClass)
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    fun cancel() {
        job.cancel()
    }

    fun schedule(dbMock: DbMock, configuration: Configuration) = launch {

        while (true) {
            delay(configuration.deletionCheckSec.toLong() * 1000)
            LOG.info("Starting deletion...")
            dbMock.clearBefore(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt())
        }
    }
}