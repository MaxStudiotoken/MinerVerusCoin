package vargas.maximo.minerveruscoin

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MarketRepository {

    suspend fun fetchVrscPrice(): Result<PriceData> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MinerVerusCoin/1.0")
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    error("El mercado devolvio HTTP $responseCode.")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val payload = JSONObject(body).optJSONObject("verus-coin")
                    ?: error("La respuesta del mercado no contiene el activo VRSC.")

                PriceData(
                    price = payload.optDouble("usd", 0.0),
                    change24h = payload.optDouble("usd_24h_change", 0.0),
                    marketCap = payload.optDouble("usd_market_cap", 0.0),
                    lastUpdatedAt = payload.optLong(
                        "last_updated_at",
                        System.currentTimeMillis() / 1000L
                    )
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val ENDPOINT =
            "https://api.coingecko.com/api/v3/simple/price?ids=verus-coin&vs_currencies=usd&include_market_cap=true&include_24hr_change=true&include_last_updated_at=true"
    }
}
