package vargas.maximo.minerveruscoin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import vargas.maximo.minerveruscoin.ui.theme.MinerVerusCoinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinerVerusCoinTheme(darkTheme = true) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    VerusCoinMinerApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun VerusCoinMinerApp(
    modifier: Modifier = Modifier,
    viewModel: MinerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val canStartMining = uiState.poolAddress.isNotBlank() && uiState.minerAddress.isNotBlank()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroCard(uiState = uiState)
        }
        item {
            ConfigurationCard(
                uiState = uiState,
                onWalletChange = viewModel::updateMinerAddress,
                onPoolChange = viewModel::updatePoolAddress,
                onWorkerChange = viewModel::updateWorkerName,
                onFarmApiUrlChange = viewModel::updateFarmApiUrl,
                onFarmApiKeyChange = viewModel::updateFarmApiKey,
                onCpuLoadChange = viewModel::updateCpuLoad
            )
        }
        item {
            ActionButtons(
                uiState = uiState,
                canStartMining = canStartMining,
                onToggleMining = viewModel::toggleMining,
                onRefresh = viewModel::refreshData
            )
        }
        item {
            PerformanceCard(uiState = uiState)
        }
        item {
            MarketCard(uiState = uiState)
        }
        item {
            ActivityCard(uiState = uiState)
        }
    }
}

@Composable
private fun HeroCard(uiState: MinerUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Verus Miner Control",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Dashboard VRSC inspirado en apps de mineria: pool, wallet, worker y rendimiento.",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroBadge(
                        modifier = Modifier.weight(1f),
                        label = "Estado",
                        value = if (uiState.isMining) "Activo" else "Listo"
                    )
                    HeroBadge(
                        modifier = Modifier.weight(1f),
                        label = "Worker",
                        value = uiState.workerName
                    )
                    HeroBadge(
                        modifier = Modifier.weight(1f),
                        label = "Wallet",
                        value = shortenValue(uiState.minerAddress.ifBlank { "Sin configurar" })
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBadge(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConfigurationCard(
    uiState: MinerUiState,
    onWalletChange: (String) -> Unit,
    onPoolChange: (String) -> Unit,
    onWorkerChange: (String) -> Unit,
    onFarmApiUrlChange: (String) -> Unit,
    onFarmApiKeyChange: (String) -> Unit,
    onCpuLoadChange: (Int) -> Unit
) {
    DashboardCard {
        SectionHeader(
            title = "Configuracion del worker",
            subtitle = "Ajusta el pool, tu wallet VRSC y la intensidad antes de iniciar."
        )

        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = uiState.minerAddress,
            onValueChange = onWalletChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isMining,
            singleLine = true,
            label = { Text("Wallet VRSC") },
            placeholder = { Text("R... o tu direccion de minado") },
            colors = textFieldColors
        )

        OutlinedTextField(
            value = uiState.poolAddress,
            onValueChange = onPoolChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isMining,
            singleLine = true,
            label = { Text("Pool / Stratum") },
            placeholder = { Text("stratum+tcp://pool:9999") },
            colors = textFieldColors
        )

        OutlinedTextField(
            value = uiState.workerName,
            onValueChange = onWorkerChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isMining,
            singleLine = true,
            label = { Text("Nombre del worker") },
            colors = textFieldColors
        )

        OutlinedTextField(
            value = uiState.farmApiUrl,
            onValueChange = onFarmApiUrlChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isMining,
            singleLine = true,
            label = { Text("Farm API (opcional)") },
            placeholder = { Text("https://api.tu-dominio.com") },
            colors = textFieldColors
        )

        OutlinedTextField(
            value = uiState.farmApiKey,
            onValueChange = onFarmApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isMining,
            singleLine = true,
            label = { Text("Clave Farm API") },
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors
        )

        Text(
            text = "${uiState.farmSyncLabel}. La clave no se guarda en el dispositivo.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Carga estimada",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${uiState.cpuLoadPercent}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = uiState.cpuLoadPercent.toFloat(),
                onValueChange = { onCpuLoadChange(it.roundToInt()) },
                enabled = !uiState.isMining,
                valueRange = 25f..100f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            LinearProgressIndicator(
                progress = uiState.cpuLoadPercent / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtons(
    uiState: MinerUiState,
    canStartMining: Boolean,
    onToggleMining: () -> Unit,
    onRefresh: () -> Unit
) {
    DashboardCard {
        SectionHeader(
            title = "Control",
            subtitle = uiState.statusLabel
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onToggleMining,
                modifier = Modifier.weight(1f),
                enabled = uiState.isMining || canStartMining,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isMining) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                ),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = if (uiState.isMining) "Detener mineria" else "Iniciar mineria",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.width(128.dp),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = if (uiState.isRefreshingMarket) "Cargando" else "Actualizar",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!uiState.isMining && !canStartMining) {
            Text(
                text = "Completa wallet y pool para habilitar el inicio del worker.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PerformanceCard(uiState: MinerUiState) {
    DashboardCard {
        SectionHeader(
            title = "Sesion de minado",
            subtitle = "Metricas en vivo del worker y del rendimiento estimado."
        )

        MetricRow(
            left = MetricSpec("Hash actual", formatHashRate(uiState.hashRate)),
            right = MetricSpec("Hash medio", formatHashRate(uiState.averageHashRate))
        )
        MetricRow(
            left = MetricSpec("Shares OK", uiState.acceptedShares.toString()),
            right = MetricSpec("Shares rechazados", uiState.rejectedShares.toString())
        )
        MetricRow(
            left = MetricSpec("Bloques", uiState.blocksFound.toString()),
            right = MetricSpec("Uptime", uiState.uptimeLabel)
        )
        MetricRow(
            left = MetricSpec("Ultima share", uiState.lastShareAt),
            right = MetricSpec("Rendimiento", "${uiState.cpuLoadPercent}%")
        )
    }
}

@Composable
private fun MarketCard(uiState: MinerUiState) {
    DashboardCard {
        SectionHeader(
            title = "Mercado VRSC",
            subtitle = "Precio real y rentabilidad diaria estimada."
        )

        if (uiState.priceData == null && uiState.isRefreshingMarket) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            val priceData = uiState.priceData
            val changeColor = if ((priceData?.change24h ?: 0.0) >= 0.0) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.tertiary
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = priceData?.let { formatUsd(it.price) } ?: "--",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Precio actual",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = changeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = priceData?.let { formatSignedPercent(it.change24h) } ?: "--",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = changeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            MetricRow(
                left = MetricSpec(
                    title = "Market cap",
                    value = priceData?.let { formatCompactUsd(it.marketCap) } ?: "--"
                ),
                right = MetricSpec(
                    title = "VRSC / dia",
                    value = formatVrsc(uiState.estimatedDailyReward)
                )
            )
            MetricRow(
                left = MetricSpec(
                    title = "USD / dia",
                    value = formatUsd(uiState.estimatedDailyUsd)
                ),
                right = MetricSpec(
                    title = "Actualizado",
                    value = priceData?.let { formatEpochSeconds(it.lastUpdatedAt) } ?: "--"
                )
            )
        }

        if (uiState.marketError != null) {
            Text(
                text = uiState.marketError,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 12.sp
            )
        }

        Text(
            text = "Fuente de mercado: CoinGecko.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ActivityCard(uiState: MinerUiState) {
    DashboardCard {
        SectionHeader(
            title = "Actividad reciente",
            subtitle = "Ultimos eventos del worker y del mercado."
        )

        if (uiState.logs.isEmpty()) {
            Text(
                text = "Todavia no hay eventos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.logs.asReversed().take(8).forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                    ) {
                        Text(
                            text = entry,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

private data class MetricSpec(
    val title: String,
    val value: String
)

@Composable
private fun MetricRow(
    left: MetricSpec,
    right: MetricSpec
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            title = left.title,
            value = left.value
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = right.title,
            value = right.value
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatHashRate(value: Double): String {
    return when {
        value >= 1000 -> String.format(Locale.US, "%.2f MH/s", value / 1000.0)
        value > 0 -> String.format(Locale.US, "%.2f kH/s", value)
        else -> "0.00 kH/s"
    }
}

private fun formatVrsc(value: Double): String =
    String.format(Locale.US, "%.4f", value)

private fun formatSignedPercent(value: Double): String =
    String.format(Locale.US, "%+.2f%%", value)

private fun formatUsd(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        minimumFractionDigits = if (value in 0.0..0.9999) 4 else 2
        maximumFractionDigits = if (value in 0.0..0.9999) 4 else 2
    }
    return formatter.format(value)
}

private fun formatCompactUsd(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format(Locale.US, "$%.2fB", value / 1_000_000_000.0)
        value >= 1_000_000 -> String.format(Locale.US, "$%.2fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.US, "$%.2fK", value / 1_000.0)
        else -> formatUsd(value)
    }
}

private fun formatEpochSeconds(epochSeconds: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(epochSeconds * 1000L))
}

private fun shortenValue(value: String, startChars: Int = 8, endChars: Int = 6): String {
    if (value.length <= startChars + endChars + 3) {
        return value
    }
    return "${value.take(startChars)}...${value.takeLast(endChars)}"
}
