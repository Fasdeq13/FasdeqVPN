package app.fasdeq.vpn.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

object Accents {
    val names = listOf(
        "Индиго", "Синий", "Бирюзовый", "Зелёный", "Лайм",
        "Янтарный", "Оранжевый", "Красный", "Розовый", "Небесный",
        "Мятный", "Жёлтый", "Коралловый", "Малиновый", "Пурпурный", "Фиолетовый"
    )
    val hues = floatArrayOf(
        250f, 215f, 175f, 135f, 80f,
        45f, 22f, 355f, 320f, 200f,
        155f, 58f, 8f, 338f, 290f, 270f
    )
    val styles = listOf("Стандарт", "Приглушённая", "Насыщенная")
    val factors = floatArrayOf(1f, 0.5f, 1.3f)
    fun swatch(index: Int, style: Int): Color =
        hsl(hues[index], (0.65f * factors[style.coerceIn(0, factors.size - 1)]).coerceIn(0f, 1f), 0.52f)
}

fun hsl(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    return Color(ColorUtils.HSLToColor(floatArrayOf(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))
}

private fun lightScheme(h: Float, k: Float): ColorScheme {
    val c = { hh: Float, ss: Float, ll: Float -> hsl(hh, (ss * k).coerceIn(0f, 1f), ll) }
    return lightColorScheme(
    primary = c(h, 0.62f, 0.42f),
    onPrimary = Color.White,
    primaryContainer = c(h, 0.90f, 0.89f),
    onPrimaryContainer = c(h, 0.80f, 0.14f),
    inversePrimary = c(h, 0.90f, 0.75f),
    secondary = c(h, 0.28f, 0.40f),
    onSecondary = Color.White,
    secondaryContainer = c(h, 0.45f, 0.88f),
    onSecondaryContainer = c(h, 0.50f, 0.14f),
    tertiary = c(h + 55f, 0.40f, 0.40f),
    onTertiary = Color.White,
    tertiaryContainer = c(h + 55f, 0.60f, 0.89f),
    onTertiaryContainer = c(h + 55f, 0.60f, 0.14f),
    background = c(h, 0.40f, 0.98f),
    onBackground = c(h, 0.20f, 0.10f),
    surface = c(h, 0.40f, 0.98f),
    onSurface = c(h, 0.20f, 0.10f),
    surfaceVariant = c(h, 0.30f, 0.90f),
    onSurfaceVariant = c(h, 0.18f, 0.30f),
    surfaceTint = c(h, 0.62f, 0.42f),
    inverseSurface = c(h, 0.12f, 0.20f),
    inverseOnSurface = c(h, 0.30f, 0.95f),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = c(h, 0.10f, 0.46f),
    outlineVariant = c(h, 0.20f, 0.80f),
    scrim = Color.Black,
    surfaceBright = c(h, 0.40f, 0.98f),
    surfaceDim = c(h, 0.20f, 0.86f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = c(h, 0.45f, 0.96f),
    surfaceContainer = c(h, 0.38f, 0.94f),
    surfaceContainerHigh = c(h, 0.32f, 0.92f),
    surfaceContainerHighest = c(h, 0.28f, 0.90f)
)
}

private fun darkScheme(h: Float, k: Float): ColorScheme {
    val c = { hh: Float, ss: Float, ll: Float -> hsl(hh, (ss * k).coerceIn(0f, 1f), ll) }
    return darkColorScheme(
    primary = c(h, 0.90f, 0.78f),
    onPrimary = c(h, 0.80f, 0.20f),
    primaryContainer = c(h, 0.60f, 0.30f),
    onPrimaryContainer = c(h, 0.90f, 0.90f),
    inversePrimary = c(h, 0.62f, 0.42f),
    secondary = c(h, 0.40f, 0.78f),
    onSecondary = c(h, 0.40f, 0.20f),
    secondaryContainer = c(h, 0.28f, 0.28f),
    onSecondaryContainer = c(h, 0.45f, 0.90f),
    tertiary = c(h + 55f, 0.50f, 0.78f),
    onTertiary = c(h + 55f, 0.50f, 0.20f),
    tertiaryContainer = c(h + 55f, 0.35f, 0.28f),
    onTertiaryContainer = c(h + 55f, 0.60f, 0.90f),
    background = c(h, 0.14f, 0.09f),
    onBackground = c(h, 0.20f, 0.90f),
    surface = c(h, 0.14f, 0.09f),
    onSurface = c(h, 0.20f, 0.90f),
    surfaceVariant = c(h, 0.12f, 0.26f),
    onSurfaceVariant = c(h, 0.20f, 0.80f),
    surfaceTint = c(h, 0.90f, 0.78f),
    inverseSurface = c(h, 0.20f, 0.90f),
    inverseOnSurface = c(h, 0.14f, 0.18f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = c(h, 0.10f, 0.58f),
    outlineVariant = c(h, 0.12f, 0.30f),
    scrim = Color.Black,
    surfaceBright = c(h, 0.12f, 0.24f),
    surfaceDim = c(h, 0.14f, 0.09f),
    surfaceContainerLowest = c(h, 0.14f, 0.06f),
    surfaceContainerLow = c(h, 0.13f, 0.12f),
    surfaceContainer = c(h, 0.12f, 0.15f),
    surfaceContainerHigh = c(h, 0.12f, 0.19f),
    surfaceContainerHighest = c(h, 0.11f, 0.23f)
)
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

private val AppTypography = Typography().let {
    it.copy(
        headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = it.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

private fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = lerp(surfaceContainerLow, Color.Black, 0.6f),
    surfaceContainer = lerp(surfaceContainer, Color.Black, 0.5f),
    surfaceContainerHigh = lerp(surfaceContainerHigh, Color.Black, 0.35f),
    surfaceContainerHighest = lerp(surfaceContainerHighest, Color.Black, 0.2f)
)

@Composable
fun FasdeqTheme(
    mode: Int,
    dynamic: Boolean,
    accent: Int,
    palette: Int,
    amoled: Boolean,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val base = if (dynamic && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val hue = Accents.hues[accent.coerceIn(0, Accents.hues.size - 1)]
        val k = Accents.factors[palette.coerceIn(0, Accents.factors.size - 1)]
        if (dark) darkScheme(hue, k) else lightScheme(hue, k)
    }
    val scheme = if (dark && amoled) base.pureBlack() else base
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
