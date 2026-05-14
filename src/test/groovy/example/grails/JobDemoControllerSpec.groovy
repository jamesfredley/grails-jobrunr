package example.grails

import example.grails.jobrunr.CleanupJobRequest
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.jobrunr.scheduling.JobRequestScheduler
import spock.lang.Specification

class JobDemoControllerSpec extends Specification
        implements ControllerUnitTest<JobDemoController>, DataTest {

    JobRequestScheduler jobRequestScheduler = Mock()

    Class[] getDomainClassesToMock() {
        [Order, Product, AuditLog] as Class[]
    }

    def setup() {
        controller.jobRequestScheduler = jobRequestScheduler
    }

    void "all state-changing actions reject GET (allowedMethods)"() {
        when: 'each mutating action is invoked over GET'
        request.method = 'GET'
        controller."${action}"()

        then: 'response is 405 Method Not Allowed'
        response.status == 405

        and: 'no job is enqueued'
        0 * jobRequestScheduler._

        where:
        action << ['fireAndForget', 'fireAndForgetWithRetry', 'scheduleDelayed',
                   'importProducts', 'bulkSync', 'triggerCleanup']
    }

    void "triggerCleanup enqueues a CleanupJobRequest('audit-logs')"() {
        given:
        request.method = 'POST'

        when:
        controller.triggerCleanup()

        then:
        1 * jobRequestScheduler.enqueue({ CleanupJobRequest req -> req.type == 'audit-logs' })
        response.redirectedUrl != null
        flash.message?.contains('Cleanup job enqueued')
    }

    void "bulkSync with no products flashes a message and does not enqueue"() {
        given:
        request.method = 'POST'
        assert Product.count() == 0

        when:
        controller.bulkSync()

        then:
        0 * jobRequestScheduler.enqueue(_)
        flash.message == 'No products found to sync.'
    }

    void "scheduleDelayed with no orders does not throw NoSuchElementException"() {
        given: 'no Order rows exist'
        request.method = 'POST'
        assert Order.count() == 0

        when: 'the regression bug being fixed: Order.list(max:1).first() used to throw'
        controller.scheduleDelayed()

        then: 'controller handles empty result gracefully'
        noExceptionThrown()
        0 * jobRequestScheduler.schedule(_, _)
        flash.message == 'No orders found.'
    }

    void "fireAndForget with no PENDING orders flashes a message and does not enqueue"() {
        given:
        request.method = 'POST'
        new Order(orderNumber: 'ORD-99', customerEmail: 'x@example.com',
                  totalAmount: 1.00G, status: 'DELIVERED').save(failOnError: true)

        when:
        controller.fireAndForget()

        then:
        0 * jobRequestScheduler.enqueue(_)
        flash.message?.startsWith('No pending orders found')
    }
}
