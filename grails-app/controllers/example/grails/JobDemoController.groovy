package example.grails

import example.grails.jobrunr.CleanupJobRequest
import example.grails.jobrunr.ImportProductsJobRequest
import example.grails.jobrunr.OrderJobRequest
import example.grails.jobrunr.RetryDemoOrderJobRequest
import example.grails.jobrunr.SendConfirmationRequest
import example.grails.jobrunr.SyncProductRequest
import grails.compiler.GrailsCompileStatic
import org.jobrunr.scheduling.JobRequestScheduler

import java.time.Instant

@GrailsCompileStatic
class JobDemoController {

    static allowedMethods = [
        fireAndForget:          'POST',
        fireAndForgetWithRetry: 'POST',
        scheduleDelayed:        'POST',
        importProducts:         'POST',
        bulkSync:               'POST',
        triggerCleanup:         'POST',
    ]

    // Grails autowires controller fields by name. JobRunr's Spring Boot auto-config
    // registers `jobRequestScheduler` as a bean, so no explicit @Autowired is needed -
    // and dropping it lets unit tests assign a Mock without provisioning a Spring context.
    JobRequestScheduler jobRequestScheduler

    def index() {
        [
            orders:    Order.list(max: 20, sort: 'dateCreated', order: 'desc'),
            products:  Product.list(max: 20, sort: 'name'),
            auditLogs: AuditLog.list(max: 20, sort: 'dateCreated', order: 'desc'),
        ]
    }

    /** Feature 1: Fire-and-forget job */
    def fireAndForget() {
        Order order = Order.findByStatus('PENDING')
        if (!order) {
            flash.message = 'No pending orders found. Seed data may have already been processed.'
            redirect(action: 'index')
            return
        }

        jobRequestScheduler.enqueue(new OrderJobRequest(order.id))

        flash.message = "Fire-and-forget job enqueued: Process Order #${order.id}. Check the dashboard at http://localhost:8000"
        redirect(action: 'index')
    }

    /** Feature 8: Fire-and-forget with possible failure (retry demo) */
    def fireAndForgetWithRetry() {
        Order order = Order.findByStatus('PENDING')
        if (!order) {
            flash.message = 'No pending orders found.'
            redirect(action: 'index')
            return
        }

        jobRequestScheduler.enqueue(new RetryDemoOrderJobRequest(order.id))

        flash.message = "Retry demo job enqueued for Order #${order.id}. This job randomly fails - watch retries in the dashboard!"
        redirect(action: 'index')
    }

    /** Feature 2: Delayed/scheduled job - runs 2 minutes from now */
    def scheduleDelayed() {
        // GORM dynamic finder: returns null on empty table.
        // (Avoid `Order.list(max: 1).first()` - .first() throws NoSuchElementException on empty list.)
        Order order = Order.first()
        if (!order) {
            flash.message = 'No orders found.'
            redirect(action: 'index')
            return
        }

        Instant runAt = Instant.now().plusSeconds(120)
        jobRequestScheduler.schedule(runAt, new SendConfirmationRequest(order.id))

        flash.message = "Scheduled job: Send confirmation for Order #${order.id} in 2 minutes. Watch it in the 'Scheduled' tab of the dashboard."
        redirect(action: 'index')
    }

    /** Feature 6: JobRequest pattern - enqueue via JobRequestScheduler */
    def importProducts() {
        jobRequestScheduler.enqueue(new ImportProductsJobRequest('https://example.com/products/catalog.csv', 5))

        flash.message = 'Product import job enqueued via JobRequest pattern (5 products). Watch progress in the dashboard!'
        redirect(action: 'index')
    }

    /** Feature 11: Bulk job enqueueing */
    def bulkSync() {
        List<Product> products = Product.list()
        if (!products) {
            flash.message = 'No products found to sync.'
            redirect(action: 'index')
            return
        }

        products.each { Product product ->
            jobRequestScheduler.enqueue(new SyncProductRequest(product.id))
        }

        flash.message = "Bulk sync enqueued: ${products.size()} inventory sync jobs. Check the dashboard to see them all!"
        redirect(action: 'index')
    }

    /** Feature 12: Trigger cleanup manually (also runs as programmatic recurring job) */
    def triggerCleanup() {
        jobRequestScheduler.enqueue(new CleanupJobRequest('audit-logs'))

        flash.message = "Cleanup job enqueued. This also runs as a recurring job (see 'Recurring Jobs' tab)."
        redirect(action: 'index')
    }
}
