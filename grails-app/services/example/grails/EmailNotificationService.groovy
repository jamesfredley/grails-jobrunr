package example.grails

import example.grails.jobrunr.SendConfirmationRequest
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.context.JobContext
import org.jobrunr.jobs.context.JobDashboardLogger
import org.jobrunr.jobs.context.JobDashboardProgressBar
import org.jobrunr.jobs.lambdas.JobRequestHandler
import org.jobrunr.server.runner.ThreadLocalJobContext

/**
 * Demonstrates delayed/scheduled jobs, JobContext for progress reporting and dashboard logging.
 * Implements JobRequestHandler so it can be invoked via the JobRequest pattern from Groovy.
 */
@Slf4j
@GrailsCompileStatic
@Transactional
class EmailNotificationService implements JobRequestHandler<SendConfirmationRequest> {

    @Override
    @Job(name = 'Send order confirmation')
    void run(SendConfirmationRequest request) throws Exception {
        // JobRequestHandler.jobContext() is @Deprecated in JobRunr 8.x.
        // Use ThreadLocalJobContext.getJobContext() directly.
        JobContext context = ThreadLocalJobContext.jobContext
        JobDashboardLogger jobLogger = context.logger()
        JobDashboardProgressBar progressBar = context.progressBar(4)
        Long orderId = request.orderId

        jobLogger.info("Starting email notification for order #${orderId}")

        Order order = Order.get(orderId)
        if (!order) {
            jobLogger.warn("Order #${orderId} not found, skipping notification")
            return
        }

        jobLogger.info("Preparing email content for ${order.customerEmail}")
        sleep(1000)
        progressBar.incrementSucceeded()

        jobLogger.info('Rendering HTML template')
        sleep(500)
        progressBar.incrementSucceeded()

        jobLogger.info('Connecting to mail server (simulated)')
        sleep(500)
        progressBar.incrementSucceeded()

        jobLogger.info("Sending confirmation email to ${order.customerEmail}")
        sleep(1000)
        progressBar.incrementSucceeded()

        log.info('Order confirmation email sent to {} for order #{}', order.customerEmail, orderId)
    }
}
