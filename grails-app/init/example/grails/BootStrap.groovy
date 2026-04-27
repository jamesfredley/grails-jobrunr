package example.grails

import example.grails.jobrunr.CleanupJobRequest
import groovy.util.logging.Slf4j
import org.jobrunr.scheduling.JobRequestScheduler
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class BootStrap {

    @Autowired
    JobRequestScheduler jobRequestScheduler

    def init = { servletContext ->
        log.info("=== Bootstrapping Grails JobRunr Demo ===")

        seedProducts()
        seedOrders()
        registerProgrammaticRecurringJobs()

        log.info("=== Bootstrap Complete ===")
        log.info("Demo app:  http://localhost:8080")
        log.info("Dashboard: http://localhost:8000")
    }

    private void seedProducts() {
        if (Product.count() > 0) {
            log.info("Products already exist, skipping seed")
            return
        }

        log.info("Seeding products...")
        [
            [name: 'Wireless Mouse',       sku: 'ELEC-001', price: 29.99,  stock: 150],
            [name: 'Mechanical Keyboard',   sku: 'ELEC-002', price: 89.99,  stock: 75],
            [name: 'USB-C Hub',             sku: 'ELEC-003', price: 49.99,  stock: 200],
            [name: 'Monitor Stand',         sku: 'FURN-001', price: 39.99,  stock: 50],
            [name: 'Desk Lamp',             sku: 'FURN-002', price: 34.99,  stock: 120],
            [name: 'Webcam HD',             sku: 'ELEC-004', price: 59.99,  stock: 5],
            [name: 'Noise-Cancelling Headphones', sku: 'ELEC-005', price: 199.99, stock: 30],
            [name: 'Standing Desk Mat',     sku: 'FURN-003', price: 45.99,  stock: 8],
            [name: 'Cable Management Kit',  sku: 'ACC-001',  price: 14.99,  stock: 300],
            [name: 'Screen Cleaner',        sku: 'ACC-002',  price: 9.99,   stock: 3],
        ].each { data ->
            new Product(
                name: data.name,
                sku: data.sku,
                price: data.price as BigDecimal,
                stockQuantity: data.stock as Integer
            ).save(failOnError: true)
        }
        log.info("Seeded {} products", Product.count())
    }

    private void seedOrders() {
        if (Order.count() > 0) {
            log.info("Orders already exist, skipping seed")
            return
        }

        log.info("Seeding orders...")
        [
            [orderNumber: 'ORD-1001', email: 'alice@example.com',   amount: 119.98, status: 'PENDING'],
            [orderNumber: 'ORD-1002', email: 'bob@example.com',     amount: 89.99,  status: 'PENDING'],
            [orderNumber: 'ORD-1003', email: 'carol@example.com',   amount: 249.97, status: 'PENDING'],
            [orderNumber: 'ORD-1004', email: 'dave@example.com',    amount: 49.99,  status: 'PENDING'],
            [orderNumber: 'ORD-1005', email: 'eve@example.com',     amount: 34.99,  status: 'DELIVERED'],
            [orderNumber: 'ORD-1006', email: 'frank@example.com',   amount: 199.99, status: 'DELIVERED'],
            [orderNumber: 'ORD-1007', email: 'grace@example.com',   amount: 14.99,  status: 'CANCELLED'],
        ].each { data ->
            new Order(
                orderNumber: data.orderNumber,
                customerEmail: data.email,
                totalAmount: data.amount as BigDecimal,
                status: data.status
            ).save(failOnError: true)
        }
        log.info("Seeded {} orders", Order.count())
    }

    /**
     * Feature 12: Programmatic recurring job registration.
     * Uses JobRequestScheduler with JobRequest objects — works natively in Groovy
     * without needing Java lambdas.
     */
    private void registerProgrammaticRecurringJobs() {
        log.info("Registering programmatic recurring jobs...")

        jobRequestScheduler.scheduleRecurrently(
            "nightly-audit-cleanup", "0 3 * * *",
            new CleanupJobRequest('audit-logs')
        )
        jobRequestScheduler.scheduleRecurrently(
            "weekly-order-cleanup", "0 4 * * SUN",
            new CleanupJobRequest('cancelled-orders')
        )

        log.info("Programmatic recurring jobs registered")
    }

    def destroy = {
    }
}
