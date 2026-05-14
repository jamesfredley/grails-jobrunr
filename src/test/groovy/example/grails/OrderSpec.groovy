package example.grails

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class OrderSpec extends Specification implements DomainUnitTest<Order> {

    void "valid order passes validation"() {
        when:
        domain.orderNumber = 'ORD-0001'
        domain.customerEmail = 'alice@example.com'
        domain.totalAmount = 19.99 as BigDecimal

        then:
        domain.validate()
    }

    void "status constraint rejects values outside the allowed list"() {
        when:
        domain.orderNumber = 'ORD-0002'
        domain.customerEmail = 'bob@example.com'
        domain.totalAmount = 1.00 as BigDecimal
        domain.status = 'WAT'

        then:
        !domain.validate(['status'])
        domain.errors['status'].code == 'not.inList'
    }

    void "status defaults to PENDING"() {
        expect:
        new Order().status == 'PENDING'
    }

    void "customerEmail must be a syntactically valid email"() {
        when:
        domain.orderNumber = 'ORD-0003'
        domain.customerEmail = 'not-an-email'
        domain.totalAmount = 1.00 as BigDecimal

        then:
        !domain.validate(['customerEmail'])
        domain.errors['customerEmail'].code == 'email.invalid'
    }

    void "totalAmount cannot be negative"() {
        when:
        domain.orderNumber = 'ORD-0004'
        domain.customerEmail = 'carol@example.com'
        domain.totalAmount = -1.00 as BigDecimal

        then:
        !domain.validate(['totalAmount'])
        domain.errors['totalAmount'].code == 'min.notmet'
    }
}
