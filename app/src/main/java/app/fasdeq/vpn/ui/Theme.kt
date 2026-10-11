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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

object Accents {
    val names = listOf(
        "Индиго", "Синий", "Бирюзовый", "Зелёный", "Лайм",
        "Янтарный", "Оранжевый", "Красный", "Розовый"
    )
    val hues = floatArrayOf(250f, 215f, 175f, 135f, 80f, 45f, 22f, 355f, 320f)
    fun swatch(index: Int): Color = hsl(hues[index], 0.65f, 0.52f)
}

fun hsl(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    return Color(ColorUtils.HSLToColor(floatArrayOf(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))
}

private fun lightScheme(h: Float): ColorScheme = lightColorScheme(
    primary = hsl(h, 0.62f, 0.42f),
    onPrimary = Color.White,
    primaryContainer = hsl(h, 0.90f, 0.89f),
    onPrimaryContainer = hsl(h, 0.80f, 0.14f),
    inversePrimary = hsl(h, 0.90f, 0.75f),
    secondary = hsl(h, 0.28f, 0.40f),
    onSecondary = Color.White,
    secondaryContainer = hsl(h, 0.45f, 0.88f),
    onSecondaryContainer = hsl(h, 0.50f, 0.14f),
    tertiary = hsl(h + 55f, 0.40f, 0.40f),
    onTertiary = Color.White,
    tertiaryContainer = hsl(h + 55f, 0.60f, 0.89f),
    onTertiaryContainer = hsl(h + 55f, 0.60f, 0.14f),
    background = hsl(h, 0.40f, 0.98f),
    onBackground = hsl(h, 0.20f, 0.10f),
    surface = hsl(h, 0.40f, 0.98f),
    onSurface = hsl(h, 0.20f, 0.10f),
    surfaceVariant = hsl(h, 0.30f, 0.90f),
    onSurfaceVariant = hsl(h, 0.18f, 0.30f),
    surfaceTint = hsl(h, 0.62f, 0.42f),
    inverseSurface = hsl(h, 0.12f, 0.20f),
    inverseOnSurface = hsl(h, 0.30f, 0.95f),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = hsl(h, 0.10f, 0.46f),
    outlineVariant = hsl(h, 0.20f, 0.80f),
    scrim = Color.Black,
    surfaceBright = hsl(h, 0.40f, 0.98f),
    surfaceDim = hsl(h, 0.20f, 0.86f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = hsl(h, 0.45f, 0.96f),
    surfaceContainer = hsl(h, 0.38f, 0.94f),
    surfaceContainerHigh = hsl(h, 0.32f, 0.92f),
    surfaceContainerHighest = hsl(h, 0.28f, 0.90f)
)

private fun darkScheme(h: Float): ColorScheme = darkColorScheme(
    primary = hsl(h, 0.90f, 0.78f),
    onPrimary = hsl(h, 0.80f, 0.20f),
    primaryContainer = hsl(h, 0.60f, 0.30f),
    onPrimaryContainer = hsl(h, 0.90f, 0.90f),
    inversePrimary = hsl(h, 0.62f, 0.42f),
    secondary = hsl(h, 0.40f, 0.78f),
    onSecondary = hsl(h, 0.40f, 0.20f),
    secondaryContainer = hsl(h, 0.28f, 0.28f),
    onSecondaryContainer = hsl(h, 0.45f, 0.90f),
    tertiary = hsl(h + 55f, 0.50f, 0.78f),
    onTertiary = hsl(h + 55f, 0.50f, 0.20f),
    tertiaryContainer = hsl(h + 55f, 0.35f, 0.28f),
    onTertiaryContainer = hsl(h + 55f, 0.60f, 0.90f),
    background = hsl(h, 0.14f, 0.09f),
    onBackground = hsl(h, 0.20f, 0.90f),
    surface = hsl(h, 0.14f, 0.09f),
    onSurface = hsl(h, 0.20f, 0.90f),
    surfaceVariant = hsl(h, 0.12f, 0.26f),
    onSurfaceVariant = hsl(h, 0.20f, 0.80f),
    surfaceTint = hsl(h, 0.90f, 0.78f),
    inverseSurface = hsl(h, 0.20f, 0.90f),
    inverseOnSurface = hsl(h, 0.14f, 0.18f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = hsl(h, 0.10f, 0.58f),
    outlineVariant = hsl(h, 0.12f, 0.30f),
    scrim = Color.Black,
    surfaceBright = hsl(h, 0.12f, 0.24f),
    surfaceDim = hsl(h, 0.14f, 0.09f),
    surfaceContainerLowest = hsl(h, 0.14f, 0.06f),
    surfaceContainerLow = hsl(h, 0.13f, 0.12f),
    surfaceContainer = hsl(h, 0.12f, 0.15f),
    surfaceContainerHigh = hsl(h, 0.12f, 0.19f),
    surfaceContainerHighest = hsl(h, 0.11f, 0.23f)
)

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

@Composable
fun FasdeqTheme(mode: Int, dynamic: Boolean, accent: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = if (dynamic && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val hue = Accents.hues[accent.coerceIn(0, Accents.hues.size - 1)]
        if (dark) darkScheme(hue) else lightScheme(hue)
    }
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
