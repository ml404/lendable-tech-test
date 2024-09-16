package offers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging

data class OfferConfig(
    val type: String,
    val applicableTo: List<String>? = null,
    val discountPercentage: Double? = null
)

class OfferConfigParser(
    private val loaders : List<OfferLoader>
) {
    private val logger = KotlinLogging.logger {}

    suspend fun loadOffers(): List<Offer> = coroutineScope {
        // Launch all loaders concurrently
        val deferredOffers = loaders.map { loader ->
            async(Dispatchers.IO) {
                try {
                    loader.loadOffers()
                } catch (e: Exception) {
                    logger.info { "Error loading offers from: ${e.message}" }
                    emptyList()
                }
            }
        }

        // Await all results
        val results = deferredOffers.awaitAll()

        // Flatten the list of lists
        results.flatten()
    }

    companion object {
        fun List<OfferConfig>.mapConfigs(): List<Offer> =
            this.mapNotNull { config ->
                when (config.type) {
                    "TwoForOneOffer" -> config.applicableTo?.let { TwoForOneOffer(it) }
                    else -> null
                }
            }
    }
}
