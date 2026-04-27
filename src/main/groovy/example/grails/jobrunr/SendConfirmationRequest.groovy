package example.grails.jobrunr

import example.grails.EmailNotificationService
import org.jobrunr.jobs.lambdas.JobRequest

class SendConfirmationRequest implements JobRequest {

    Long orderId

    SendConfirmationRequest() {}

    SendConfirmationRequest(Long orderId) {
        this.orderId = orderId
    }

    @Override
    Class<EmailNotificationService> getJobRequestHandler() {
        return EmailNotificationService
    }
}
