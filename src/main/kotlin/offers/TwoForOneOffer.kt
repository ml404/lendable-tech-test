package offers

import pokos.Product

data class TwoForOneOffer(val applicableProductNames: List<String>) : Offer {

    override val name = "2-for-1"

    private val normalisedApplicableTo = applicableProductNames.map { it.lowercase() }

    override fun isApplicable(product: Product): Boolean {
        // Check if the product name matches any normalized applicableTo
        val productName = product.name.lowercase()
        return normalisedApplicableTo.contains(productName)
    }

    override fun apply(product: Product, quantity: Int): Double {
        // Calculate discount for 2-for-1 offer
        val discountQuantity = quantity / 2
        return discountQuantity * product.price
    }
}