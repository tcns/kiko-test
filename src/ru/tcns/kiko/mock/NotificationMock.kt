package test.kiko.ru.tcns.kiko.mock

import org.slf4j.LoggerFactory
import ru.tcns.kiko.api.TimeSlot

class NotificationMock {
    val LOG = LoggerFactory.getLogger(javaClass)
    fun notifyTenant(timeSlot: TimeSlot) {
        LOG.info("Notifying ${timeSlot.tenantId}")
    }
}