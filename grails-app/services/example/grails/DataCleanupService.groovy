package example.grails

import example.grails.jobrunr.CleanupJobRequest
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.lambdas.JobRequestHandler

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Demonstrates programmatic recurring job registration (done in BootStrap.groovy).
 * Implements JobRequestHandler so it can be invoked via the JobRequest pattern from Groovy.
 *
 * Not annotated with @GrailsCompileStatic because it uses GORM's where {} DetachedCriteria
 * DSL, which is not statically compilable.
 */
@Slf4j
@Transactional
class DataCleanupService implements JobRequestHandler<CleanupJobRequest> {

    @Override
    @Job(name = 'Data cleanup', retries = 2, labels = ['maintenance', 'cleanup'])
    void run(CleanupJobRequest request) throws Exception {
        switch (request.type) {
            case 'audit-logs':       cleanupOldAuditLogs(); break
            case 'cancelled-orders': cleanupCancelledOrders(); break
        }
    }

    void cleanupOldAuditLogs() {
        log.info('=== Starting Audit Log Cleanup ===')

        // 30-day duration cutoff (not local-calendar 30 days - DST-safe).
        Date cutoffDate = Date.from(Instant.now().minus(30, ChronoUnit.DAYS))

        // DetachedCriteria.deleteAll() returns Number; widen to long to avoid
        // truncation on tables that ever exceed Integer.MAX_VALUE rows.
        long deletedCount = AuditLog.where {
            dateCreated < cutoffDate
        }.deleteAll() as long

        log.info('Deleted {} audit log entries older than 30 days', deletedCount)
        log.info('=== Audit Log Cleanup Complete ===')
    }

    void cleanupCancelledOrders() {
        log.info('=== Starting Cancelled Order Cleanup ===')

        List<Order> cancelledOrders = Order.findAllByStatus('CANCELLED')
        int count = cancelledOrders.size()

        cancelledOrders.each { Order order ->
            log.info('Archiving cancelled order: {}', order.orderNumber)
            // In a real app, you'd archive to a separate table or external storage
        }

        log.info('Processed {} cancelled orders for archival', count)
        log.info('=== Cancelled Order Cleanup Complete ===')
    }
}
