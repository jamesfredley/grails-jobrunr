package example.grails.jobrunr

import org.jobrunr.jobs.lambdas.JobRequest

/**
 * Demo-only request that intentionally fails ~67% of the time so the JobRunr
 * dashboard's retry behaviour is observable. Routed to its own handler to keep
 * {@link OrderJobRequest} clean of demo concerns.
 */
class RetryDemoOrderJobRequest implements JobRequest {

    Long orderId

    RetryDemoOrderJobRequest() {}

    RetryDemoOrderJobRequest(Long orderId) {
        this.orderId = orderId
    }

    @Override
    Class<RetryDemoOrderJobRequestHandler> getJobRequestHandler() {
        return RetryDemoOrderJobRequestHandler
    }
}
