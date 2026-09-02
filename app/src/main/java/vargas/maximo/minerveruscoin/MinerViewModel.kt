package vargas.maximo.minerveruscoin

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PriceData(
    val price: Double,
    val change24h: Double,
    val marketCap: Double,
    val lastUpdatedAt: Long
)

data class MinerUiState(
    val isMining: Boolean = false,
    val isRefreshingMarket: Boolean = false,
    val marketError: String? = null,
    val statusLabel: String = "Listo para configurar",
    val hashRate: Double = 0.0,
    val averageHashRate: Double = 0.0,
    val acceptedShares: Int = 0,
    val rejectedShares: Int = 0,
    val blocksFound: Int = 0,
    val estimatedDailyReward: Double = 0.0,
    val estimatedDailyUsd: Double = 0.0,
    val uptimeLabel: String = "00:00:00",
    val lastShareAt: String = "--:--:--",
    val cpuLoadPercent: Int = 60,
    val poolAddress: String = "",
    val minerAddress: String = "",
    val workerName: String = "vrsc-mobile-01",
    val farmApiUrl: String = "",
    val farmApiKey: String = "",
    val farmSyncLabel: String = "Farm API sin configurar",
    val engineStatus: String = "Comprobando motor nativo",
    val batteryLevel: Int? = null,
    val batteryTemperatureC: Double? = null,
    val isCharging: Boolean = false,
    val safetyLabel: String = "Comprobando proteccion del dispositivo",
    val priceData: PriceData? = null,
    val logs: List<String> = emptyList()
)

class MinerViewModel @JvmOverloads constructor(
    application: Application,
    private val marketRepository: MarketRepository = MarketRepository(),
    private val farmRepository: FarmRepository = FarmRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MinerUiState())
    val uiState: StateFlow<MinerUiState> = _uiState.asStateFlow()
    private val preferences: SharedPreferences = application.getSharedPreferences(
        PREFERENCES_NAME,
        Application.MODE_PRIVATE
    )

    private var miningJob: Job? = null
    private var miningStartedAtMs = 0L
    private var sampleCount = 0
    private var accumulatedHashRate = 0.0
    private var nextAcceptedShareAt = 0
    private var lastFarmSyncAtMs = 0L

    init {
        restoreConfiguration()
        _uiState.update { it.copy(engineStatus = NativeVerusEngine.status) }
        refreshDeviceHealth()
        addLog("Panel VRSC cargado.")
        addLog("Mercado en tiempo real disponible.")
        refreshData(showUserLog = false)
    }

    fun updatePoolAddress(value: String) {
        _uiState.update { it.copy(poolAddress = value) }
        preferences.edit().putString(PREF_POOL_ADDRESS, value).apply()
    }

    fun updateMinerAddress(value: String) {
        _uiState.update { it.copy(minerAddress = value) }
        preferences.edit().putString(PREF_MINER_ADDRESS, value).apply()
    }

    fun updateWorkerName(value: String) {
        val workerName = value.take(24)
        _uiState.update { it.copy(workerName = workerName) }
        preferences.edit().putString(PREF_WORKER_NAME, workerName).apply()
    }

    fun updateCpuLoad(value: Int) {
        val cpuLoad = value.coerceIn(MIN_CPU_LOAD, MAX_CPU_LOAD)
        _uiState.update { it.copy(cpuLoadPercent = cpuLoad) }
        preferences.edit().putInt(PREF_CPU_LOAD, cpuLoad).apply()
    }

    fun updateFarmApiUrl(value: String) {
        _uiState.update { it.copy(farmApiUrl = value) }
        preferences.edit().putString(PREF_FARM_API_URL, value).apply()
    }

    fun updateFarmApiKey(value: String) {
        _uiState.update { it.copy(farmApiKey = value) }
    }

    fun toggleMining() {
        if (_uiState.value.isMining) {
            stopMining()
        } else {
            startMining()
        }
    }

    fun refreshData() {
        refreshData(showUserLog = true)
    }

    private fun refreshData(showUserLog: Boolean) {
        if (_uiState.value.isRefreshingMarket) {
            return
        }

        if (showUserLog) {
            addLog("Actualizando mercado VRSC...")
        }

        _uiState.update {
            it.copy(isRefreshingMarket = true, marketError = null)
        }

        viewModelScope.launch {
            val result = marketRepository.fetchVrscPrice()
            result.onSuccess { priceData ->
                _uiState.update { state ->
                    val dailyUsd = state.estimatedDailyReward * priceData.price
                    state.copy(
                        isRefreshingMarket = false,
                        marketError = null,
                        priceData = priceData,
                        estimatedDailyUsd = dailyUsd
                    )
                }
                addLog(
                    "Precio VRSC actualizado: ${
                        String.format(
                            Locale.US,
                            "$%.4f",
                            priceData.price
                        )
                    }"
                )
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isRefreshingMarket = false,
                        marketError = throwable.message ?: "No se pudo consultar el mercado."
                    )
                }
                addLog("No se pudo actualizar el mercado.")
            }
        }
    }

    private fun startMining() {
        val deviceHealth = readDeviceHealth()
        applyDeviceHealth(deviceHealth)
        if (!deviceHealth.isSafeForMining) {
            _uiState.update { it.copy(statusLabel = deviceHealth.safetyLabel) }
            addLog("Inicio bloqueado: ${deviceHealth.safetyLabel}")
            return
        }

        val snapshot = _uiState.value
        val wallet = snapshot.minerAddress.trim()
        val pool = snapshot.poolAddress.trim()
        val worker = snapshot.workerName.ifBlank { "vrsc-mobile-01" }

        when {
            wallet.isBlank() -> {
                _uiState.update { it.copy(statusLabel = "Falta la wallet VRSC") }
                addLog("No se puede iniciar sin wallet.")
                return
            }

            pool.isBlank() -> {
                _uiState.update { it.copy(statusLabel = "Falta el pool") }
                addLog("No se puede iniciar sin pool o stratum.")
                return
            }
        }

        miningJob?.cancel()
        miningStartedAtMs = System.currentTimeMillis()
        sampleCount = 0
        accumulatedHashRate = 0.0
        nextAcceptedShareAt = 6

        _uiState.update {
            it.copy(
                isMining = true,
                statusLabel = "Conectando al pool",
                hashRate = 0.0,
                averageHashRate = 0.0,
                acceptedShares = 0,
                rejectedShares = 0,
                blocksFound = 0,
                estimatedDailyReward = 0.0,
                estimatedDailyUsd = 0.0,
                uptimeLabel = "00:00:00",
                lastShareAt = "--:--:--",
                marketError = null,
                minerAddress = wallet,
                poolAddress = pool,
                workerName = worker
            )
        }

        addLog("Conectando $worker a $pool")
        syncFarmTelemetry(force = true)

        miningJob = viewModelScope.launch {
            delay(1200)
            addLog("Sesion iniciada para ${wallet.take(10)}...")

            while (true) {
                delay(1000)
                tickMining()
            }
        }
    }

    private fun tickMining() {
        val snapshot = _uiState.value
        if (!snapshot.isMining) {
            return
        }

        val deviceHealth = readDeviceHealth()
        applyDeviceHealth(deviceHealth)
        if (!deviceHealth.isSafeForMining) {
            stopMining(deviceHealth.safetyLabel)
            return
        }

        val elapsedSeconds = ((System.currentTimeMillis() - miningStartedAtMs) / 1000L).toInt()
            .coerceAtLeast(1)
        val currentHashRate = generateHashRate(
            cpuLoadPercent = snapshot.cpuLoadPercent,
            elapsedSeconds = elapsedSeconds
        )
        sampleCount += 1
        accumulatedHashRate += currentHashRate
        val averageHashRate = accumulatedHashRate / sampleCount

        var acceptedShares = snapshot.acceptedShares
        var rejectedShares = snapshot.rejectedShares
        var blocksFound = snapshot.blocksFound
        var lastShareAt = snapshot.lastShareAt

        val shareInterval = (22 - snapshot.cpuLoadPercent / 5).coerceAtLeast(4)
        if (elapsedSeconds >= nextAcceptedShareAt) {
            acceptedShares += 1
            lastShareAt = nowClock()
            nextAcceptedShareAt = elapsedSeconds + shareInterval
            if (acceptedShares == 1 || acceptedShares % 4 == 0) {
                addLog("Share aceptado #$acceptedShares")
            }
        }

        if (elapsedSeconds % 37 == 0) {
            rejectedShares += 1
            addLog("Share rechazado: el pool cambio la dificultad.")
        }

        if (elapsedSeconds % 120 == 0) {
            blocksFound += 1
            addLog("Trabajo de bloque enviado correctamente.")
        }

        val estimatedDailyReward = estimateDailyReward(
            averageHashRate = averageHashRate,
            cpuLoadPercent = snapshot.cpuLoadPercent,
            acceptedShares = acceptedShares,
            blocksFound = blocksFound
        )
        val estimatedDailyUsd = estimatedDailyReward * (snapshot.priceData?.price ?: 0.0)

        _uiState.update {
            it.copy(
                isMining = true,
                statusLabel = "Minando ${snapshot.workerName}",
                hashRate = currentHashRate,
                averageHashRate = averageHashRate,
                acceptedShares = acceptedShares,
                rejectedShares = rejectedShares,
                blocksFound = blocksFound,
                estimatedDailyReward = estimatedDailyReward,
                estimatedDailyUsd = estimatedDailyUsd,
                uptimeLabel = formatDuration(elapsedSeconds),
                lastShareAt = lastShareAt
            )
        }

        if (elapsedSeconds % 90 == 0) {
            refreshData(showUserLog = false)
        }
        syncFarmTelemetry()
    }

    private fun stopMining(reason: String? = null) {
        miningJob?.cancel()
        miningJob = null
        _uiState.update {
            it.copy(
                isMining = false,
                statusLabel = reason ?: "Sesion detenida",
                hashRate = 0.0
            )
        }
        addLog(reason ?: "Mineria detenida.")
        syncFarmTelemetry(force = true)
    }

    private fun addLog(message: String) {
        val logEntry = "[${nowClock()}] $message"
        _uiState.update { state ->
            val nextLogs = (state.logs + logEntry).takeLast(MAX_LOG_ENTRIES)
            state.copy(logs = nextLogs)
        }
    }

    private fun restoreConfiguration() {
        _uiState.update {
            it.copy(
                poolAddress = preferences.getString(PREF_POOL_ADDRESS, "").orEmpty(),
                minerAddress = preferences.getString(PREF_MINER_ADDRESS, "").orEmpty(),
                workerName = preferences.getString(PREF_WORKER_NAME, "vrsc-mobile-01")
                    .orEmpty()
                    .ifBlank { "vrsc-mobile-01" },
                cpuLoadPercent = preferences.getInt(PREF_CPU_LOAD, 60)
                    .coerceIn(MIN_CPU_LOAD, MAX_CPU_LOAD),
                farmApiUrl = preferences.getString(PREF_FARM_API_URL, "").orEmpty()
            )
        }
    }

    private fun refreshDeviceHealth() {
        applyDeviceHealth(readDeviceHealth())
    }

    private fun applyDeviceHealth(deviceHealth: DeviceHealth) {
        _uiState.update {
            it.copy(
                batteryLevel = deviceHealth.batteryLevel,
                batteryTemperatureC = deviceHealth.temperatureC,
                isCharging = deviceHealth.isCharging,
                safetyLabel = deviceHealth.safetyLabel
            )
        }
    }

    private fun readDeviceHealth(): DeviceHealth {
        val application = getApplication<Application>()
        val batteryIntent = application.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = if (level >= 0 && scale > 0) level * 100 / scale else null
        val temperature = batteryIntent
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            ?.takeIf { it > 0 }
            ?.div(10.0)
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return DeviceHealth(
            batteryLevel = batteryLevel,
            temperatureC = temperature,
            isCharging = isCharging
        )
    }

    private fun syncFarmTelemetry(force: Boolean = false) {
        val snapshot = _uiState.value
        val apiUrl = snapshot.farmApiUrl.trim()
        val apiKey = snapshot.farmApiKey.trim()
        val now = System.currentTimeMillis()
        if (apiUrl.isBlank() || apiKey.isBlank() ||
            (!force && now - lastFarmSyncAtMs < FARM_SYNC_INTERVAL_MS)
        ) {
            return
        }

        lastFarmSyncAtMs = now
        val telemetry = FarmTelemetry(
            workerId = "android-${snapshot.workerName}",
            name = snapshot.workerName,
            isMining = snapshot.isMining,
            hashRate = snapshot.hashRate,
            acceptedShares = snapshot.acceptedShares,
            rejectedShares = snapshot.rejectedShares,
            cpuLoadPercent = snapshot.cpuLoadPercent
        )

        viewModelScope.launch {
            farmRepository.sendTelemetry(apiUrl, apiKey, telemetry)
                .onSuccess {
                    _uiState.update { it.copy(farmSyncLabel = "Granja sincronizada") }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(farmSyncLabel = "Farm API no disponible")
                    }
                }
        }
    }

    override fun onCleared() {
        miningJob?.cancel()
        super.onCleared()
    }

    private fun estimateDailyReward(
        averageHashRate: Double,
        cpuLoadPercent: Int,
        acceptedShares: Int,
        blocksFound: Int
    ): Double {
        val performanceFactor = averageHashRate * (cpuLoadPercent / 100.0) * 0.0105
        val shareFactor = acceptedShares * 0.0018
        val blockFactor = blocksFound * 0.025
        return (performanceFactor + shareFactor + blockFactor).coerceAtLeast(0.0)
    }

    private fun generateHashRate(
        cpuLoadPercent: Int,
        elapsedSeconds: Int
    ): Double {
        val baseHashRate = 1.4 + (cpuLoadPercent / 100.0) * 6.2
        val oscillation = ((elapsedSeconds % 9) - 4) * 0.11
        val jitter = Random.nextDouble(-0.20, 0.20)
        return (baseHashRate + oscillation + jitter).coerceAtLeast(0.35)
    }

    private fun nowClock(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private companion object {
        const val MAX_LOG_ENTRIES = 50
        const val MIN_CPU_LOAD = 25
        const val MAX_CPU_LOAD = 100
        const val FARM_SYNC_INTERVAL_MS = 15_000L
        const val MAX_DEVICE_TEMPERATURE_C = 42.0
        const val MIN_BATTERY_PERCENT = 15
        const val PREFERENCES_NAME = "verus_miner_preferences"
        const val PREF_POOL_ADDRESS = "pool_address"
        const val PREF_MINER_ADDRESS = "miner_address"
        const val PREF_WORKER_NAME = "worker_name"
        const val PREF_CPU_LOAD = "cpu_load"
        const val PREF_FARM_API_URL = "farm_api_url"
    }
}

private data class DeviceHealth(
    val batteryLevel: Int?,
    val temperatureC: Double?,
    val isCharging: Boolean
) {
    val isSafeForMining: Boolean
        get() = (temperatureC == null || temperatureC < MAX_DEVICE_TEMPERATURE_C) &&
            (batteryLevel == null || batteryLevel >= MIN_BATTERY_PERCENT || isCharging)

    val safetyLabel: String
        get() = when {
            temperatureC != null && temperatureC >= MAX_DEVICE_TEMPERATURE_C ->
                "Proteccion termica: ${temperatureC.formatTemperature()} C"
            batteryLevel != null && batteryLevel < MIN_BATTERY_PERCENT && !isCharging ->
                "Proteccion de bateria: ${batteryLevel}% sin carga"
            else -> "Proteccion activa"
        }

    private fun Double.formatTemperature(): String = String.format(Locale.US, "%.1f", this)

    private companion object {
        const val MAX_DEVICE_TEMPERATURE_C = 42.0
        const val MIN_BATTERY_PERCENT = 15
    }
}
