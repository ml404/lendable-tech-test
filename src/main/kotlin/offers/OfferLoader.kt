package offers

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import helper.GsonJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import offers.OfferConfigParser.Companion.mapConfigs
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

interface OfferLoader {
    suspend fun loadOffers(): List<Offer>
}

class URLOfferLoader(private val urlString: String) : OfferLoader {
    private val jsonParser = GsonJsonParser(Gson())
    override suspend fun loadOffers(): List<Offer> = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        return@withContext connection.inputStream.use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                val offerConfigs: List<OfferConfig> =
                    jsonParser.fromJson(reader, object : TypeToken<List<OfferConfig>>() {})
                offerConfigs.mapConfigs()
            }
        }
    }
}

class FileOfferLoader(private val filePath: String) : OfferLoader {
    private val jsonParser = GsonJsonParser(Gson())
    override suspend fun loadOffers(): List<Offer> = withContext(Dispatchers.IO) {
        val classLoader = Thread.currentThread().contextClassLoader
        val resourceStream = classLoader.getResourceAsStream(filePath)
            ?: throw IllegalArgumentException("Configuration file not found in classpath: $filePath")

        return@withContext InputStreamReader(resourceStream).use { reader ->
            val offerConfigs: List<OfferConfig> =
                jsonParser.fromJson(reader, object : TypeToken<List<OfferConfig>>() {})
            offerConfigs.mapConfigs()
        }
    }
}