package example.grails.jobrunr

import example.grails.OrderProcessingService
import org.jobrunr.jobs.lambdas.JobRequest

/**
 * Fire-and-forget order processing request. The retry-failure demo lives in a
 * separate {@link RetryDemoOrderJobRequest} so this class stays free of
 * demo-only flags.
 */
class OrderJobRequest implements JobRequest {

    Long orderId

    OrderJobRequest() {}

    OrderJobRequest(Long orderId) {
        this.orderId = orderId
    }

    @Override
    Class<OrderProcessingService> getJobRequestHandler() {
        return OrderProcessingService
    }
}
