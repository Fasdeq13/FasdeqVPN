package app.fasdeq.vpn.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val IL_TAU = (2.0 * PI).toFloat()

private fun DrawScope.sparkle(x: Float, y: Float, r: Float, color: androidx.compose.ui.graphics.Color) {
    val p = Path()
    p.moveTo(x, y - r)
    p.quadraticBezierTo(x, y, x + r, y)
    p.quadraticBezierTo(x, y, x, y + r)
    p.quadraticBezierTo(x, y, x - r, y)
    p.quadraticBezierTo(x, y, x, y - r)
    p.close()
    drawPath(p, color)
}

private fun DrawScope.blob(cx: Float, cy: Float, base: Float, lobes: Float, amp: Float, offset: Float, color: androidx.compose.ui.graphics.Color) {
    val path = Path()
    val steps = 120
    for (i in 0..steps) {
        val th = i * IL_TAU / steps
        val rr = base * (1f + amp * cos(lobes * th + offset))
        val x = cx + rr * cos(th)
        val y = cy + rr * sin(th)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
fun EmptyIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "empty")
    val t by transition.animateFloat(
        0f,
        IL_TAU,
        infiniteRepeatable(tween(Motion.d(7000), easing = LinearEasing), RepeatMode.Restart),
        label = "emptyT"
    )
    val bob by transition.animateFloat(
        -5f,
        5f,
        infiniteRepeatable(tween(Motion.d(2400)), RepeatMode.Reverse),
        label = "emptyBob"
    )
    val back = scheme.tertiaryContainer
    val rack = scheme.surfaceContainerHighest
    val rackEdge = scheme.outlineVariant
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary
    val shieldFill = scheme.primaryContainer
    val onShield = scheme.onPrimaryContainer
    Canvas(modifier.size(200.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        blob(cx, cy, w * 0.36f, 7f, 0.06f, t, back)
        blob(cx, cy, w * 0.28f, 5f, 0.05f, -t * 1.5f, back.copy(alpha = 0.55f))
        val boxW = w * 0.46f
        val boxH = h * 0.15f
        val left = cx - boxW / 2f - w * 0.06f
        for (i in 0..2) {
            val top = cy - h * 0.2f + i * (boxH + h * 0.04f)
            drawRoundRect(rack, Offset(left, top), Size(boxW, boxH), CornerRadius(boxH * 0.4f))
            drawRoundRect(rackEdge, Offset(left, top), Size(boxW, boxH), CornerRadius(boxH * 0.4f), style = Stroke(1.5f.dp.toPx()))
            val ledY = top + boxH / 2f
            for (j in 0..2) {
                val a = 0.35f + 0.65f * abs(sin(t * 2f + i * 1.3f + j * 0.9f))
                val c = when (j) {
                    0 -> primary
                    1 -> secondary
                    else -> tertiary
                }
                drawCircle(c.copy(alpha = a), radius = boxH * 0.11f, center = Offset(left + boxH * 0.45f + j * boxH * 0.3f, ledY))
            }
            drawRoundRect(
                rackEdge,
                Offset(left + boxW * 0.55f, ledY - boxH * 0.06f),
                Size(boxW * 0.35f, boxH * 0.12f),
                CornerRadius(boxH * 0.06f)
            )
        }
        val sx = cx + w * 0.24f
        val sy = cy - h * 0.2f + bob.dp.toPx()
        val sw = w * 0.17f
        val sh = h * 0.22f
        val shield = Path()
        shield.moveTo(sx, sy - sh)
        shield.lineTo(sx + sw, sy - sh * 0.55f)
        shield.lineTo(sx + sw, sy + sh * 0.05f)
        shield.cubicTo(sx + sw, sy + sh * 0.6f, sx + sw * 0.5f, sy + sh * 0.9f, sx, sy + sh)
        shield.cubicTo(sx - sw * 0.5f, sy + sh * 0.9f, sx - sw, sy + sh * 0.6f, sx - sw, sy + sh * 0.05f)
        shield.lineTo(sx - sw, sy - sh * 0.55f)
        shield.close()
        drawPath(shield, shieldFill)
        drawPath(shield, primary, style = Stroke(2.5f.dp.toPx()))
        val check = Path()
        check.moveTo(sx - sw * 0.45f, sy + sh * 0.02f)
        check.lineTo(sx - sw * 0.1f, sy + sh * 0.35f)
        check.lineTo(sx + sw * 0.5f, sy - sh * 0.3f)
        drawPath(check, onShield, style = Stroke(3.5f.dp.toPx(), cap = StrokeCap.Round))
        for (k in 0..3) {
            val a = 0.25f + 0.75f * abs(sin(t * 2f + k * 1.6f))
            val px = cx + w * (0.34f * cos(k * 1.7f + 0.4f))
            val py = cy + h * (0.34f * sin(k * 2.1f + 0.2f))
            sparkle(px, py, (4f + 3f * a).dp.toPx(), primary.copy(alpha = a))
        }
    }
}

@Composable
fun HeroBackdrop(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "backdrop")
    val t by transition.animateFloat(
        0f,
        IL_TAU,
        infiniteRepeatable(tween(Motion.d(26000), easing = LinearEasing), RepeatMode.Restart),
        label = "backdropT"
    )
    val c1 = scheme.primaryContainer
    val c2 = scheme.tertiaryContainer
    val c3 = scheme.secondaryContainer
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        blob(w * (0.15f + 0.05f * sin(t)), h * (0.35f + 0.05f * cos(t)), w * 0.22f, 5f, 0.08f, t * 2f, c1.copy(alpha = 0.45f))
        blob(w * (0.88f + 0.04f * cos(t)), h * (0.55f + 0.06f * sin(t * 2f)), w * 0.26f, 6f, 0.07f, -t * 2f, c2.copy(alpha = 0.40f))
        blob(w * (0.5f + 0.08f * sin(t * 3f)), h * (0.98f + 0.03f * cos(t * 2f)), w * 0.3f, 4f, 0.09f, t, c3.copy(alpha = 0.35f))
    }
}

@Composable
fun SheetHeader(icon: Int, title: String, subtitle: String? = null) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "sheetHeader")
    val spin by transition.animateFloat(
        0f,
        IL_TAU,
        infiniteRepeatable(tween(Motion.d(10000), easing = LinearEasing), RepeatMode.Restart),
        label = "sheetSpin"
    )
    val primary = scheme.primaryContainer
    val accent = scheme.tertiaryContainer
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                blob(cx, cy, size.minDimension * 0.4f, 6f, 0.07f, -spin * 1.3f, accent)
                blob(cx, cy, size.minDimension * 0.36f, 8f, 0.06f, spin, primary)
            }
            Icon(
                painterResource(icon),
                null,
                Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = sin(spin) * 6f },
                tint = scheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}
