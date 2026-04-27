package example.grails.jobrunr

import example.grails.DataCleanupService
import org.jobrunr.jobs.lambdas.JobRequest

class CleanupJobRequest implements JobRequest {

    String type

    CleanupJobRequest() {}

    CleanupJobRequest(String type) {
        this.type = type
    }

    @Override
    Class<DataCleanupService> getJobRequestHandler() {
        return DataCleanupService
    }
}
