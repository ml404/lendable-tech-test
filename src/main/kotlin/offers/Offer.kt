package offers

import pokos.Product

interface Offer {
    val name: String
    fun isApplicable(product: Product): Boolean
    fun apply(product: Product, quantity: Int): Double
}