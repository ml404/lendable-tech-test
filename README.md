# Shopping Service Design and Implementation - README

This `README` file provides an overview of the design choices and implementation details for the
`DefaultShoppingServiceImpl`, `OfferConfigParser`, and related components. The primary goal is to manage shopping
offers, load them asynchronously, apply them to a shopping cart, and ensure the service stays updated with periodic
refreshes. We'll cover the key design decisions for handling concurrency, offer management, testing, and more.

---

## Project Overview

The `DefaultShoppingServiceImpl` is designed to handle shopping carts and apply applicable offers to products. It
interfaces with loaders (e.g., from a URL or file) to retrieve offers and applies them in real-time during receipt
generation.

Key classes and features:

- **`OfferConfigParser`:** Parses offers from multiple loaders (URL, File, etc.) concurrently and applies them to the
  shopping cart.
- **`DefaultShoppingServiceImpl`:** Manages the cart and applies offers during checkout, with a periodic refresh of
  offers.
- **Concurrency & Asynchronous Design:** Efficient use of Kotlin Coroutines to handle asynchronous offer loading and
  periodic refreshing.
- **Testability:** The service is easily testable with custom mocks and coroutine testing utilities.

---

## Design Choices

### 1. **Asynchronous Offer Loading**

The core design decision is to **load offers asynchronously** from multiple loaders. This allows the system to scale
better and handle offers from different sources (e.g., URLs, files) in a non-blocking way.

- **Offer Loaders:** We use `OfferLoader` interfaces that represent different sources (e.g., `URLOfferLoader`,
  `FileOfferLoader`), all of which are suspend functions to support asynchronous loading.

```kotlin
class OfferConfigParser(
    private val loaders: List<OfferLoader>
) {
    suspend fun loadOffers(): List<Offer> = coroutineScope {
        val deferredOffers = loaders.map { loader ->
            async(Dispatchers.IO) {
                try {
                    loader.loadOffers()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
        deferredOffers.awaitAll().flatten()
    }
}
```

### 2. Managing Offers with Synchronization

Once offers are loaded, we store them in a mutable list (offers) within DefaultShoppingServiceImpl. To ensure thread
safety, we synchronize access to the list.

```kotlin

@VisibleForTesting
suspend fun loadOffers() = withContext(dispatcher) {
    val newOffers = offerConfigParser.loadOffers().distinctBy { it.name }.toMutableList()
    synchronized(offers) {
        offers.clear()
        offers.addAll(newOffers)
    }
}
```

The synchronised block ensures that concurrent reads/writes to the offers list are safe.

### 3. Non-Blocking Initialization with Coroutines

Initially, the service loads offers and refreshes them periodically. I wanted to ensure that the initial offer load
completes before starting the periodic refresh without blocking the entire thread.

We achieved this by using coroutines in the init block:

```kotlin
init {
// Non-blocking initialization: Await the initial load before starting periodic refresh
    serviceScope.launch {
        loadOffers()
        startPeriodicRefresh()
    }
}
```

Non-Blocking Initialization: The serviceScope.launch ensures the initialisation isn't blocked, but the initial offer
loading is awaited. Once the offers are loaded, the periodic refresh begins.

### 4. Periodic Offer Refresh

A key feature of the service is to periodically refresh the offers. This is managed by launching a coroutine in a
background scope (serviceScope) that keeps reloading offers at fixed intervals.

```kotlin
private fun startPeriodicRefresh() {
    serviceScope.launch {
        while (isActive) {
            try {
                loadOffers()
            } catch (e: Exception) {
                // Handle exception
            }
            delay(refreshIntervalMillis)  // Wait for the next refresh
        }
    }
}
```

This design choice ensures the offers stay updated without blocking or interfering with other operations.

### 5. Testing and Coroutine Test Dispatchers

In the unit tests, we use the StandardTestDispatcher to control the execution of coroutines deterministically, and
advanceUntilIdle() to allow tests to await completion of suspended functions.

```kotlin

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
    advanceUntilIdle()  // Ensure all coroutines have completed

    val receipt = shoppingService.generateReceipt()

    assertTrue(receipt.contains("Total: £5.00"))
}
```

advanceUntilIdle() ensures that all coroutines have completed before making assertions. 
We use mock implementations of OfferLoader to simulate offer loading during tests.

# Future Considerations:

- **Error Handling Improvements:** We currently log errors in offer loading and refresh. With more time I could introduce retries or circuit breakers for failed loaders.
- **Scalability:** The design is flexible for adding more offer loaders or handling larger lists of offers by leveraging more advanced coroutine patterns like Flow for streaming data rather than my cruder setup.