package example.grails.jobrunr

import example.grails.Product
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.context.JobContext
import org.jobrunr.jobs.context.JobDashboardLogger
import org.jobrunr.jobs.context.JobDashboardProgressBar
import org.jobrunr.jobs.lambdas.JobRequestHandler
import org.jobrunr.server.runner.ThreadLocalJobContext
import org.springframework.stereotype.Component

import java.util.concurrent.ThreadLocalRandom

/**
 * Handles ImportProductsJobRequest - demonstrates the JobRequest/Handler pattern
 * with progress bar and dashboard logging.
 */
@Slf4j
@Component
class ImportProductsJobRequestHandler implements JobRequestHandler<ImportProductsJobRequest> {

    @Override
    @Job(name = 'Import products', retries = 3, labels = ['import', 'products'])
    @Transactional
    void run(ImportProductsJobRequest jobRequest) throws Exception {
        // JobRequestHandler.jobContext() is @Deprecated in JobRunr 8.x.
        // Use ThreadLocalJobContext.getJobContext() directly.
        JobContext context = ThreadLocalJobContext.jobContext
        JobDashboardLogger jobLogger = context.logger()
        JobDashboardProgressBar progressBar = context.progressBar(jobRequest.batchSize)

        jobLogger.info("Starting product import from: ${jobRequest.sourceUrl}")
        jobLogger.info("Batch size: ${jobRequest.batchSize}")

        ThreadLocalRandom random = ThreadLocalRandom.current()
        (1..jobRequest.batchSize).each { int i ->
            String sku = "IMP-${System.currentTimeMillis()}-${i}"
            new Product(
                name:          "Imported Product ${i}",
                sku:           sku,
                price:         BigDecimal.valueOf(10 + random.nextInt(90)),
                stockQuantity: random.nextInt(200) + 1
            ).save(flush: true)

            jobLogger.info("Imported product: Imported Product ${i} (SKU: ${sku})")
            progressBar.incrementSucceeded()
            sleep(3000)
        }

        jobLogger.info("Product import complete: ${jobRequest.batchSize} products imported")
    }
}
