package offers

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import offers.OfferConfigParser.Companion.mapConfigs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfferConfigParserTest {

    private val mockLoader1 = mockk<OfferLoader>()
    private val mockLoader2 = mockk<OfferLoader>()

    @Test
    fun `loadOffers should return combined offers from all loaders`() = runTest {
        // Arrange
        val offer1 = OfferConfig("TwoForOneOffer", listOf("ProductA"))

        coEvery { mockLoader1.loadOffers() } returns listOf(offer1).mapConfigs()

        val parser = OfferConfigParser(listOf(mockLoader1, mockLoader2))

        // Act
        val result = parser.loadOffers()

        // Assert
        assertTrue(listOf(offer1).mapConfigs().containsAll(result))
    }

    @Test
    fun `loadOffers should handle errors gracefully`() = runTest {
        // Arrange
        val offer1 = OfferConfig("TwoForOneOffer", listOf("ProductA"))

        coEvery { mockLoader1.loadOffers() } returns listOf(offer1).mapConfigs()
        coEvery { mockLoader2.loadOffers() } throws RuntimeException("Loader error")

        val parser = OfferConfigParser(listOf(mockLoader1, mockLoader2))

        // Act
        val result = parser.loadOffers()

        // Assert
        assertEquals(listOf(offer1).mapConfigs(), result)
    }

    @Test
    fun `mapConfigs should map configurations to offers correctly`() {
        // Arrange
        val configs = listOf(
            OfferConfig("TwoForOneOffer", listOf("ProductA")),
            OfferConfig("SomeOtherOffer", listOf("ProductB"))
        )

        // Act
        val result = configs.mapConfigs()

        // Assert
        assertEquals(listOf(TwoForOneOffer(listOf("ProductA"))), result)
    }
}
