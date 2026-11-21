package app.caravan.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** The weather at the end of a ride, from Open-Meteo (no key): "rain", "snow", "fog" or "" for none or no network. */
object Weather {
    suspend fun now(lat: Double, lon: Double): String = withTimeoutOrNull(6000) {
        withContext(Dispatchers.IO) {
            runCatching {
                val url = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=weather_code", lat, lon)
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 4000; c.readTimeout = 4000
                c.setRequestProperty("User-Agent", "Caravan (app.caravan)")
                val code = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current").getInt("weather_code")
                when (code) {
                    45, 48 -> "fog"
                    in 51..67, in 80..82, in 95..99 -> "rain"
                    in 71..77, 85, 86 -> "snow"
                    else -> ""
                }
            }.getOrDefault("")
        }
    } ?: ""

    fun title(w: String) = when (w) { "rain" -> tr("дождь", "rain"); "snow" -> tr("снег", "snow"); "fog" -> tr("туман", "fog"); else -> "" }
}
