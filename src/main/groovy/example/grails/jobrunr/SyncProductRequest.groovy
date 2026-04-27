package example.grails.jobrunr

import example.grails.InventorySyncService
import org.jobrunr.jobs.lambdas.JobRequest

class SyncProductRequest implements JobRequest {

    Long productId

    SyncProductRequest() {}

    SyncProductRequest(Long productId) {
        this.productId = productId
    }

    @Override
    Class<InventorySyncService> getJobRequestHandler() {
        return InventorySyncService
    }
}
