package example.grails.jobrunr

import example.grails.OrderProcessingService
import org.jobrunr.jobs.lambdas.JobRequest

class OrderJobRequest implements JobRequest {

    Long orderId
    boolean simulateFailure = false

    OrderJobRequest() {}

    OrderJobRequest(Long orderId) {
        this.orderId = orderId
    }

    OrderJobRequest(Long orderId, boolean simulateFailure) {
        this.orderId = orderId
        this.simulateFailure = simulateFailure
    }

    @Override
    Class<OrderProcessingService> getJobRequestHandler() {
        return OrderProcessingService
    }
}
