package pokos

class Cart {
    val items: MutableMap<Product, Int> = mutableMapOf()

    fun addItem(product: Product, quantity: Int = 1) {
        items[product] = items.getOrDefault(product, 0) + quantity
    }

    fun removeItem(product: Product, quantity: Int = 1) {
        val currentQuantity = items[product] ?: return
        if (currentQuantity <= quantity) {
            items.remove(product)
        } else {
            items[product] = currentQuantity - quantity
        }
    }

}