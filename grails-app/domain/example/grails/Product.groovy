package example.grails

class Product {

    String name
    String sku
    BigDecimal price
    Integer stockQuantity
    Date dateCreated
    Date lastUpdated

    static constraints = {
        name blank: false, maxSize: 255
        sku blank: false, unique: true, maxSize: 50
        price min: 0.0 as BigDecimal
        stockQuantity min: 0
    }

    String toString() {
        "${name} (${sku})"
    }
}
