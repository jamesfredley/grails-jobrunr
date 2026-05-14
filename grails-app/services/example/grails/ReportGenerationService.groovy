package example.grails

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.annotations.Recurring

/**
 * Demonstrates @Recurring annotation with both CRON and interval-based schedules.
 *
 * Note: `static lazyInit = false` ensures the bean is eagerly initialized so that
 * RecurringJobPostProcessor (a Spring BeanPostProcessor) sees the @Recurring
 * annotations at startup. Grails services default to lazy init = true, which would
 * defer creation until first use - by which time the post-processor has already
 * finished scanning.
 *
 * Not annotated with @GrailsCompileStatic because it uses Hibernate's createCriteria
 * projections DSL, which is not statically compilable.
 */
@Slf4j
@Transactional(readOnly = true)
class ReportGenerationService {

    static lazyInit = false

    @Recurring(id = 'daily-sales-report', cron = '0 2 * * *')
    @Job(name = 'Generate daily sales report')
    void generateDailySalesReport() {
        log.info('=== Generating Daily Sales Report ===')

        int totalOrders = Order.count()
        int pendingOrders = Order.countByStatus('PENDING')
        int shippedOrders = Order.countByStatus('SHIPPED')
        int deliveredOrders = Order.countByStatus('DELIVERED')

        BigDecimal totalRevenue = (Order.createCriteria().get {
            projections {
                sum('totalAmount')
            }
        } as BigDecimal) ?: 0.0G

        log.info('Report Summary:')
        log.info('  Total Orders:     {}', totalOrders)
        log.info('  Pending:          {}', pendingOrders)
        log.info('  Shipped:          {}', shippedOrders)
        log.info('  Delivered:        {}', deliveredOrders)
        log.info('  Total Revenue:    ${}', totalRevenue)
        log.info('=== Daily Sales Report Complete ===')
    }

    @Recurring(id = 'inventory-snapshot', interval = 'PT6H')
    @Job(name = 'Generate inventory snapshot')
    void generateInventorySnapshot() {
        log.info('=== Generating Inventory Snapshot ===')

        List<Product> lowStock = Product.findAllByStockQuantityLessThan(10)
        int totalProducts = Product.count()
        Long totalStock = (Product.createCriteria().get {
            projections {
                sum('stockQuantity')
            }
        } as Long) ?: 0L

        log.info('Inventory Snapshot:')
        log.info('  Total Products:   {}', totalProducts)
        log.info('  Total Stock:      {}', totalStock)
        log.info('  Low Stock Items:  {}', lowStock.size())
        lowStock.each { Product product ->
            log.warn('  LOW STOCK: {} - {} units remaining', product.name, product.stockQuantity)
        }
        log.info('=== Inventory Snapshot Complete ===')
    }
}
