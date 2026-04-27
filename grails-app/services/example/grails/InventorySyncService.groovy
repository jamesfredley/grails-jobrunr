package example.grails

import example.grails.jobrunr.SyncProductRequest
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.lambdas.JobRequestHandler

/**
 * Demonstrates bulk job enqueueing — the controller enqueues one sync job per product.
 * Implements JobRequestHandler so it can be invoked via the JobRequest pattern from Groovy.
 */
@Slf4j
@Transactional
class InventorySyncService implements JobRequestHandler<SyncProductRequest> {

    @Override
    @Job(name = "Sync inventory", retries = 3, labels = ["inventory", "sync"])
    void run(SyncProductRequest request) throws Exception {
        syncProduct(request.productId)
    }

    void syncProduct(Long productId) {
        log.info("Syncing inventory for product #{}", productId)

        Product product = Product.get(productId)
        if (!product) {
            log.warn("Product #{} not found, skipping sync", productId)
            return
        }

        // Simulate external inventory API call
        sleep(500)
        int externalStock = new Random().nextInt(100) + 1
        int oldStock = product.stockQuantity

        product.stockQuantity = externalStock
        product.save(flush: true)

        log.info("Product '{}' stock updated: {} -> {}", product.name, oldStock, externalStock)
    }
}
