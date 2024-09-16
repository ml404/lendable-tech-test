package helper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

interface JsonParser {
    suspend fun <T> fromJson(reader: InputStreamReader, typeToken: TypeToken<T>): T
}

class GsonJsonParser(private val gson: Gson) : JsonParser {
    override suspend fun <T> fromJson(reader: InputStreamReader, typeToken: TypeToken<T>): T {
        return gson.fromJson(reader, typeToken.type)
    }
}