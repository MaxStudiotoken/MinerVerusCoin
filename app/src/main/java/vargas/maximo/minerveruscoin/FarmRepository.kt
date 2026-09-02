package vargas.maximo.minerveruscoin

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class FarmTelemetry(
    val workerId: String,
    val name: String,
    val isMining: Boolean,
    val hashRate: Double,
    val acceptedShares: Int,
    val rejectedShares: Int,
    val cpuLoadPercent: Int
)

class FarmRepository {

    suspend fun sendTelemetry(
        apiBaseUrl: String,
        apiKey: String,
        telemetry: FarmTelemetry
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiBaseUrl.startsWith("https://")) {
                "La URL de Farm API debe comenzar con https://."
            }

            val endpoint = "${apiBaseUrl.trimEnd('/')}/api/farm/telemetry"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Farm-Key", apiKey)
            }

            try {
                val body = JSONObject()
                    .put("workerId", telemetry.workerId)
                    .put("name", telemetry.name)
                    .put("type", "android")
                    .put("isMining", telemetry.isMining)
                    .put("hashrate", telemetry.hashRate)
                    .put("acceptedShares", telemetry.acceptedShares)
                    .put("rejectedShares", telemetry.rejectedShares)
                    .put("cpuLoadPercent", telemetry.cpuLoadPercent)
                    .toString()

                connection.outputStream.bufferedWriter().use { it.write(body) }
                if (connection.responseCode !in 200..299) {
                    error("Farm API devolvio HTTP ${connection.responseCode}.")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
