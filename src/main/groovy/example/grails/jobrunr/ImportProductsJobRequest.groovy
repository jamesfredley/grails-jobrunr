package example.grails.jobrunr

import org.jobrunr.jobs.lambdas.JobRequest

/**
 * Demonstrates the JobRequest pattern — an alternative to lambdas for job serialization.
 * JobRequest objects are simple serializable POJOs that describe WHAT to do.
 * The corresponding JobRequestHandler describes HOW to do it.
 */
class ImportProductsJobRequest implements JobRequest {

    String sourceUrl
    int batchSize

    /** Required no-arg constructor for deserialization */
    ImportProductsJobRequest() {}

    ImportProductsJobRequest(String sourceUrl, int batchSize) {
        this.sourceUrl = sourceUrl
        this.batchSize = batchSize
    }

    @Override
    Class<ImportProductsJobRequestHandler> getJobRequestHandler() {
        return ImportProductsJobRequestHandler
    }
}
