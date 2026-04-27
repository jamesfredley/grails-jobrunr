package example.grails

class Order {

    String orderNumber
    String customerEmail
    String status = 'PENDING'
    BigDecimal totalAmount
    Date dateCreated
    Date lastUpdated

    static mapping = {
        table 'customer_order'
    }

    static constraints = {
        orderNumber blank: false, unique: true, maxSize: 50
        customerEmail blank: false, email: true, maxSize: 255
        status inList: ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']
        totalAmount min: 0.0 as BigDecimal
    }

    String toString() {
        "Order ${orderNumber} (${status})"
    }
}
