import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import offers.Offer
import offers.OfferConfigParser
import offers.OfferLoader
import offers.TwoForOneOffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pokos.Cart
import pokos.Product
import service.DefaultShoppingServiceImpl

class ShoppingServiceTest {

    private lateinit var shoppingService: DefaultShoppingServiceImpl
    private lateinit var cart: Cart
    private val product = Product("Cornflakes", 2.5)
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    // Mock OfferLoader for testing
    private class MockOfferLoader(private val offers: List<Offer>) : OfferLoader {

        override suspend fun loadOffers(): List<Offer> {
            return offers
        }
    }

    @BeforeEach
    fun setUp() {
        cart = Cart()
    }

    @AfterEach
    fun tearDown() {
        // Clean up resources if needed
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test adding items to the cart`() = runTest {
        shoppingService = DefaultShoppingServiceImpl(
            cart,
            testDispatcher,
            OfferConfigParser(listOf(MockOfferLoader(emptyList())))
        )

        advanceUntilIdle()
        cart.addItem(product, 3)
        val receipt = shoppingService.generateReceipt()
        assertTrue(receipt.contains("Cornflakes x3"))
    }

    @Test
    fun `test removing items to the cart`() = runTest {
        shoppingService = DefaultShoppingServiceImpl(
            cart,
            testDispatcher,
            OfferConfigParser(listOf(MockOfferLoader(emptyList())))
        )

        cart.addItem(product, 3)
        cart.removeItem(product, 2)
        val receipt = shoppingService.generateReceipt()
        assertTrue(receipt.contains("Cornflakes x1"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test applying 2-for-1 offer`() = runTest {
        val twoForOneOffer = TwoForOneOffer(listOf("Cornflakes"))
        shoppingService = DefaultShoppingServiceImpl(
            cart,
            testDispatcher,
            OfferConfigParser(listOf(MockOfferLoader(listOf(twoForOneOffer))))
        )


        cart.addItem(product, 4)
        shoppingService.loadOffers()
        advanceUntilIdle()  // Ensure all coroutines have completed

        val receipt = shoppingService.generateReceipt()

        // Extracting parts of the receipt for more granular assertions
        val lines = receipt.split("\n")
        assertTrue(lines.any { it.contains("Cornflakes x4: £10.00") }, "Receipt should contain 'Cornflakes x4: £10.00'")
        assertTrue(
            lines.any { it.contains("Offer '2-for-1' applied to product (Cornflakes): -£5.00") },
            "Receipt should contain 'Offer '2-for-1' applied to product (Cornflakes): -£5.00'"
        )
        assertTrue(lines.any { it.contains("Total: £5.00") }, "Receipt should contain 'Total: £5.00'")
    }
}
