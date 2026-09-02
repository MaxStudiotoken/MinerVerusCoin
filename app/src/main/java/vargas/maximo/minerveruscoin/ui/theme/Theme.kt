package vargas.maximo.minerveruscoin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VrscBlue,
    onPrimary = VrscWhite,
    secondary = VrscGreen,
    onSecondary = VrscWhite,
    tertiary = VrscRed,
    onTertiary = VrscWhite,
    background = VrscBlack,
    onBackground = VrscWhite,
    surface = VrscPanel,
    onSurface = VrscWhite,
    surfaceVariant = VrscPanelAlt,
    onSurfaceVariant = VrscLightGrey,
    outline = VrscGrey
)

private val LightColorScheme = lightColorScheme(
    primary = VrscBlue,
    onPrimary = VrscWhite,
    secondary = VrscGreen,
    onSecondary = VrscWhite,
    tertiary = VrscRed,
    onTertiary = VrscWhite,
    background = VrscWhite,
    onBackground = VrscBlack,
    surface = Color(0xFFF4F7FB),
    onSurface = VrscBlack,
    surfaceVariant = Color(0xFFE3EAF7),
    onSurfaceVariant = VrscBlueDeep,
    outline = VrscGrey
)

@Composable
fun MinerVerusCoinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
