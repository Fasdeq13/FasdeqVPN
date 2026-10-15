package app.fasdeq.vpn.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.fasdeq.vpn.AppState
import app.fasdeq.vpn.Services

private val DiscordColor = Color(0xFF5865F2)
private val TelegramColor = Color(0xFF2AABEE)
private val YouTubeColor = Color(0xFFFF0000)

private fun DrawScope.drawService(index: Int, alpha: Float) {
    val s = this.size.minDimension
    val white = Color.White.copy(alpha = alpha)
    when (index) {
        0 -> {
            drawRoundRect(
                DiscordColor.copy(alpha = alpha),
                Offset.Zero,
                Size(s, s),
                CornerRadius(s * 0.28f)
            )
            drawRoundRect(
                white,
                Offset(s * 0.2f, s * 0.28f),
                Size(s * 0.6f, s * 0.42f),
                CornerRadius(s * 0.2f)
            )
            drawCircle(white, s * 0.07f, Offset(s * 0.3f, s * 0.72f))
            drawCircle(white, s * 0.07f, Offset(s * 0.7f, s * 0.72f))
            val eye = DiscordColor.copy(alpha = alpha)
            drawRoundRect(
                eye,
                Offset(s * 0.34f, s * 0.43f),
                Size(s * 0.08f, s * 0.13f),
                CornerRadius(s * 0.04f)
            )
            drawRoundRect(
                eye,
                Offset(s * 0.58f, s * 0.43f),
                Size(s * 0.08f, s * 0.13f),
                CornerRadius(s * 0.04f)
            )
        }
        1 -> {
            drawCircle(TelegramColor.copy(alpha = alpha), s / 2f, Offset(s / 2f, s / 2f))
            val plane = Path()
            plane.moveTo(s * 0.22f, s * 0.50f)
            plane.lineTo(s * 0.78f, s * 0.27f)
            plane.lineTo(s * 0.66f, s * 0.74f)
            plane.lineTo(s * 0.48f, s * 0.60f)
            plane.lineTo(s * 0.38f, s * 0.70f)
            plane.lineTo(s * 0.39f, s * 0.56f)
            plane.close()
            drawPath(plane, white)
            val fold = Path()
            fold.moveTo(s * 0.39f, s * 0.56f)
            fold.lineTo(s * 0.70f, s * 0.36f)
            fold.lineTo(s * 0.48f, s * 0.60f)
            fold.close()
            drawPath(fold, TelegramColor.copy(alpha = alpha * 0.35f))
        }
        else -> {
            drawRoundRect(
                YouTubeColor.copy(alpha = alpha),
                Offset(0f, s * 0.14f),
                Size(s, s * 0.72f),
                CornerRadius(s * 0.22f)
            )
            val play = Path()
            play.moveTo(s * 0.42f, s * 0.34f)
            play.lineTo(s * 0.42f, s * 0.66f)
            play.lineTo(s * 0.68f, s * 0.50f)
            play.close()
            drawPath(play, white)
        }
    }
}

@Composable
fun ServiceLogo(index: Int, value: Int?, busy: Boolean, best: Boolean, size: Dp = 22.dp) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "logo")
    val pulse by transition.animateFloat(
        0.35f,
        0.9f,
        infiniteRepeatable(tween(Motion.d(700)), RepeatMode.Reverse),
        label = "logoPulse"
    )
    val failed = value != null && value < 0
    val alpha = when {
        failed -> 0.3f
        value == null && busy -> pulse
        value == null -> 0.4f
        else -> 1f
    }
    val ring = scheme.primary
    val error = scheme.error
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        if (best) {
            drawCircle(
                ring.copy(alpha = 0.25f),
                radius = s * 0.78f,
                center = Offset(s / 2f, s / 2f)
            )
            drawCircle(
                ring,
                radius = s * 0.72f,
                center = Offset(s / 2f, s / 2f),
                style = Stroke(width = 1.5f.dp.toPx())
            )
        }
        drawService(index, alpha)
        if (failed) {
            drawLine(
                error,
                Offset(s * 0.12f, s * 0.88f),
                Offset(s * 0.88f, s * 0.12f),
                strokeWidth = s * 0.1f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ServiceStrip(state: AppState, bestIds: List<String?>) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in Services.names.indices) {
            val bestId = bestIds.getOrNull(i)
            val server = state.servers.firstOrNull { it.id == bestId }
            val ms = state.checks[bestId]?.ms?.getOrNull(i)
            Surface(
                onClick = { if (bestId != null) state.select(bestId) },
                shape = RoundedCornerShape(22.dp),
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ServiceLogo(i, ms ?: if (state.checkingServices) null else -1, state.checkingServices, false, 28.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            server?.name ?: "—",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (ms != null && ms > 0) "$ms мс" else if (state.checkingServices) "…" else "нет",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSecondaryContainer.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
