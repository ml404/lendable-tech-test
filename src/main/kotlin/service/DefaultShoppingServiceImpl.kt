package service

import kotlinx.coroutines.*
import offers.FileOfferLoader
import offers.Offer
import offers.OfferConfigParser
import offers.URLOfferLoader
import org.jetbrains.annotations.VisibleForTesting
import pokos.Cart

class DefaultShoppingServiceImpl(
    private val cart: Cart,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val offerConfigParser: OfferConfigParser = OfferConfigParser(
        listOf(
            URLOfferLoader("https://exampleInternalUrl.com/config"),
            FileOfferLoader("offers-config.json")
        )
    )
) : ShoppingService {

    private val offers: MutableList<Offer> = mutableListOf()
    private val refreshIntervalMillis = 3600000L // 1 hour

    // Coroutine scope
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    init {
        coroutineScope.launch {
            loadOffers() // Initial load
            startPeriodicRefresh() // Start periodic refresh
        }
    }

    private fun startPeriodicRefresh() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    loadOffers()
                } catch (e: Exception) {
                    println("Error refreshing offers: ${e.message}")
                }
                delay(refreshIntervalMillis)
            }
        }
    }

    @VisibleForTesting
    suspend fun loadOffers() {
        val newOffers = offerConfigParser.loadOffers().distinctBy { it.name }.toMutableList()
        synchronized(offers) {
            offers.clear()
            offers.addAll(newOffers)
        }
    }

    override fun addOffer(offer: Offer) {
        offers.add(offer)
    }

    override fun generateReceipt(): String {
        val receipt = StringBuilder()
        receipt.append("Receipt:\n")
        val total = calculateTotal(receipt)
        receipt.append("Total: £${"%.2f".format(total)}\n")
        return receipt.toString()
    }

    private fun calculateTotal(receipt: StringBuilder): Double {
        var total = 0.0
        for ((product, quantity) in cart.items) {
            val subtotal = product.price * quantity
            receipt.append("${product.name} x$quantity: £${"%.2f".format(subtotal)}\n")

            val applicableOffers = offers.filter { it.isApplicable(product) }
            applicableOffers.forEach { offer ->
                val discount = offer.apply(product, quantity)
                if (discount > 0) {
                    receipt.append(
                        "Offer '${offer.name}' applied to product (${product.name}): -£${"%.2f".format(discount)}\n"
                    )
                }
                total += (subtotal - discount)
            }

            if (applicableOffers.isEmpty()) {
                total += subtotal
            }
        }
        return total
    }
}
