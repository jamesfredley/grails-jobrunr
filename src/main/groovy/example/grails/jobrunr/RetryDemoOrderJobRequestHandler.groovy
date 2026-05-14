package example.grails.jobrunr

import example.grails.Order
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.lambdas.JobRequestHandler
import org.springframework.stereotype.Component

import java.util.concurrent.ThreadLocalRandom

/**
 * Demo handler that randomly fails ~67% of the time to showcase JobRunr's
 * automatic retry with exponential backoff. Lives in src/main/groovy as a
 * Spring @Component (not in grails-app/services/) so the demo-only failure
 * mode does not pollute the production OrderProcessingService.
 */
@Slf4j
@Component
class RetryDemoOrderJobRequestHandler implements JobRequestHandler<RetryDemoOrderJobRequest> {

    @Override
    @Job(name = 'Process order (retry demo)', retries = 5, labels = ['order-processing', 'retry-demo'])
    @Transactional
    void run(RetryDemoOrderJobRequest request) throws Exception {
        log.info('Processing order #{} (retry demo)', request.orderId)

        Order order = Order.get(request.orderId)
        if (!order) {
            throw new IllegalArgumentException("Order not found: ${request.orderId}")
        }

        if (ThreadLocalRandom.current().nextInt(3) == 0) {
            order.status = 'PROCESSING'
            order.save(flush: true)
            sleep(1000)
            order.status = 'SHIPPED'
            order.save(flush: true)
            log.info('Order #{} processed successfully (retry demo)', request.orderId)
        } else {
            throw new RuntimeException(
                "Simulated processing failure for order #${request.orderId} - JobRunr will retry this automatically"
            )
        }
    }
}
