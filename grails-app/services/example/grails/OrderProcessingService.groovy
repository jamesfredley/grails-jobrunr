package example.grails

import example.grails.jobrunr.OrderJobRequest
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.lambdas.JobRequestHandler

/**
 * Demonstrates fire-and-forget jobs with @Job annotation for naming, retries, and labels.
 * Implements JobRequestHandler so it can be invoked via the JobRequest pattern from Groovy.
 *
 * The retry-failure demo (which intentionally throws to exercise JobRunr's retry path)
 * lives in {@code RetryDemoOrderJobRequestHandler} so this service stays free of
 * demo-only branches.
 */
@Slf4j
@GrailsCompileStatic
@Transactional
class OrderProcessingService implements JobRequestHandler<OrderJobRequest> {

    @Override
    @Job(name = 'Process order', retries = 5, labels = ['order-processing'])
    void run(OrderJobRequest request) throws Exception {
        processOrder(request.orderId)
    }

    void processOrder(Long orderId) {
        log.info('Starting to process order #{}', orderId)

        Order order = Order.get(orderId)
        if (!order) {
            throw new IllegalArgumentException("Order not found: ${orderId}")
        }

        // Simulate processing steps
        order.status = 'PROCESSING'
        order.save(flush: true)
        log.info('Order #{} marked as PROCESSING', orderId)

        sleep(2000) // Simulate payment verification

        order.status = 'SHIPPED'
        order.save(flush: true)
        log.info('Order #{} has been shipped', orderId)
    }
}
