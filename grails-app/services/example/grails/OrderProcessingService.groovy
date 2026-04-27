package example.grails

import example.grails.jobrunr.OrderJobRequest
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.lambdas.JobRequestHandler

/**
 * Demonstrates fire-and-forget jobs with @Job annotation for naming, retries, and labels.
 * Implements JobRequestHandler so it can be invoked via the JobRequest pattern from Groovy.
 */
@Slf4j
@Transactional
class OrderProcessingService implements JobRequestHandler<OrderJobRequest> {

    @Override
    @Job(name = "Process order", retries = 5, labels = ["order-processing"])
    void run(OrderJobRequest request) throws Exception {
        if (request.simulateFailure) {
            processOrderWithPossibleFailure(request.orderId)
        } else {
            processOrder(request.orderId)
        }
    }

    void processOrder(Long orderId) {
        log.info("Starting to process order #{}", orderId)

        Order order = Order.get(orderId)
        if (!order) {
            throw new IllegalArgumentException("Order not found: ${orderId}")
        }

        // Simulate processing steps
        order.status = 'PROCESSING'
        order.save(flush: true)
        log.info("Order #{} marked as PROCESSING", orderId)

        sleep(2000) // Simulate payment verification

        order.status = 'SHIPPED'
        order.save(flush: true)
        log.info("Order #{} has been shipped", orderId)
    }

    void processOrderWithPossibleFailure(Long orderId) {
        log.info("Processing order #{} (failure demo)", orderId)

        Order order = Order.get(orderId)
        if (!order) {
            throw new IllegalArgumentException("Order not found: ${orderId}")
        }

        // Randomly fail to demonstrate retry behavior
        if (new Random().nextInt(3) == 0) {
            order.status = 'PROCESSING'
            order.save(flush: true)
            sleep(1000)
            order.status = 'SHIPPED'
            order.save(flush: true)
            log.info("Order #{} processed successfully", orderId)
        } else {
            throw new RuntimeException("Simulated processing failure for order #${orderId} — JobRunr will retry this automatically")
        }
    }
}
