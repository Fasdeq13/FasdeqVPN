package app.fasdeq.vpn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.fasdeq.vpn.PingResult
import app.fasdeq.vpn.Ping
import app.fasdeq.vpn.R
import app.fasdeq.vpn.Server
import app.fasdeq.vpn.ServiceCheck
import app.fasdeq.vpn.Status
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val TAU = (2.0 * PI).toFloat()

object LaunchStyles {
    val names = listOf("Печенька", "Пульс", "Орбита", "Волна", "Неон")
}

internal fun DrawScope.drawLaunch(
    style: Int,
    phase: Float,
    amp: Float,
    orbit: Float,
    aura: Float,
    level: Float,
    burst: Float,
    body: Color,
    idle: Color,
    content: Color,
    ring: Color,
    accent: Color,
    base: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val center = Offset(cx, cy)
    val act = maxOf(orbit, aura)
    if (burst < 1f) {
        drawCircle(
            ring.copy(alpha = (1f - burst) * 0.55f),
            radius = base * (1f + 1.25f * burst),
            center = center,
            style = Stroke(width = (16f * (1f - burst) + 2f).dp.toPx())
        )
        drawCircle(
            ring.copy(alpha = (1f - burst) * 0.25f),
            radius = base * (1f + 0.7f * burst),
            center = center,
            style = Stroke(width = (8f * (1f - burst) + 1f).dp.toPx())
        )
    }
    when (style) {
        1 -> {
            for (k in 0..3) {
                val t = (phase * (0.12f + 0.18f * orbit) + k * 0.25f) % 1f
                drawCircle(
                    ring.copy(alpha = (1f - t) * (0.12f + 0.3f * act)),
                    radius = base * (1.02f + 0.95f * t),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            drawCircle(body, radius = base * (1f + 0.025f * sin(phase * 3f) * orbit), center = center)
        }
        2 -> {
            for (r in 0..2) {
                val radius = base * (1.28f + 0.27f * r)
                drawCircle(
                    ring.copy(alpha = 0.10f + 0.22f * act),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5f.dp.toPx())
                )
                val dir = if (r % 2 == 0) 1f else -1f
                val count = 2 + r
                for (i in 0 until count) {
                    val a = dir * phase * (1.1f + 0.4f * r) * (0.6f + 0.8f * orbit) + i * (TAU / count)
                    drawCircle(
                        ring.copy(alpha = 0.35f + 0.65f * act),
                        radius = (4f + r).dp.toPx(),
                        center = Offset(cx + radius * cos(a), cy + radius * sin(a))
                    )
                }
            }
            drawCircle(body, radius = base, center = center)
        }
        3 -> {
            drawCircle(idle, radius = base, center = center)
            val circle = Path()
            circle.addOval(Rect(center, base))
            clipPath(circle) {
                val top = cy + base - 2f * base * level
                val w = size.width
                val waveAmp = base * 0.07f * (0.4f + 0.6f * (1f - level * 0.5f))
                if (level > 0.01f) {
                    for (layer in 0..1) {
                        val path = Path()
                        path.moveTo(0f, size.height)
                        var x = 0f
                        while (x <= w) {
                            val shift = if (layer == 0) 1.7f else 0f
                            path.lineTo(x, top + waveAmp * sin(x / base * 3.2f + phase * 2f + shift))
                            x += 6f
                        }
                        path.lineTo(w, size.height)
                        path.close()
                        drawPath(path, ring.copy(alpha = if (layer == 0) 0.45f else 0.95f))
                    }
                }
            }
        }
        4 -> {
            drawCircle(body, radius = base, center = center)
            val strength = 0.25f + 0.75f * act
            val spin = phase * 57.2958f * (0.5f + orbit * 2f)
            val widths = floatArrayOf(18f, 10f, 4f)
            val alphas = floatArrayOf(0.10f, 0.20f, 0.90f)
            for (g in 0..2) {
                val a = alphas[g] * strength
                rotate(spin, center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, ring.copy(alpha = a), accent.copy(alpha = a), Color.Transparent),
                            center
                        ),
                        radius = base * 1.2f,
                        center = center,
                        style = Stroke(width = widths[g].dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
        else -> {
            if (aura > 0.01f) {
                for (k in 0..1) {
                    val t = (phase * 0.18f + k * 0.5f) % 1f
                    drawCircle(
                        ring.copy(alpha = (1f - t) * 0.22f * aura),
                        radius = base * (1.06f + 0.6f * t),
                        center = center
                    )
                }
            }
            if (orbit > 0.01f) {
                for (i in 0 until 8) {
                    val a = phase * 1.6f + i * (TAU / 8f)
                    val r = base * (1.3f + 0.1f * sin(phase * 2.2f + i)) * (0.7f + 0.3f * orbit)
                    val dot = (3.5f + 2.5f * abs(sin(phase * 3f + i * 1.3f))).dp.toPx()
                    drawCircle(
                        ring.copy(alpha = 0.85f * orbit),
                        radius = dot,
                        center = Offset(cx + r * cos(a), cy + r * sin(a))
                    )
                }
            }
            val path = Path()
            val steps = 140
            for (i in 0..steps) {
                val th = i * TAU / steps
                val rr = base * (1f + amp * cos(8f * th + phase))
                val x = cx + rr * cos(th)
                val y = cy + rr * sin(th)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, body)
            if (orbit > 0.01f) {
                val rad = base * 0.78f
                drawArc(
                    color = content.copy(alpha = orbit * 0.9f),
                    startAngle = phase * 57.2958f * 1.2f,
                    sweepAngle = 90f + 60f * sin(phase * 2f),
                    useCenter = false,
                    topLeft = Offset(cx - rad, cy - rad),
                    size = Size(rad * 2f, rad * 2f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun PowerButton(status: Status, style: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val on = status == Status.On
    val connecting = status == Status.Connecting
    val busy = connecting || status == Status.Stopping

    val container by animateColorAsState(
        if (on) scheme.primary else scheme.secondaryContainer,
        tween(Motion.d(700)),
        label = "container"
    )
    val content by animateColorAsState(
        if (on) scheme.onPrimary else scheme.onSecondaryContainer,
        tween(Motion.d(700)),
        label = "content"
    )
    val amp by animateFloatAsState(
        when {
            connecting -> 0.09f
            status == Status.Stopping -> 0.06f
            on -> 0.05f
            else -> 0f
        },
        spring(dampingRatio = 0.4f, stiffness = Motion.k(50f)),
        label = "amp"
    )
    val speed by animateFloatAsState(
        when {
            connecting -> 2.8f
            status == Status.Stopping -> 3.4f
            on -> 0.35f
            else -> 0.12f
        },
        tween(Motion.d(1000)),
        label = "speed"
    )
    val orbit by animateFloatAsState(if (connecting) 1f else 0f, tween(Motion.d(600)), label = "orbit")
    val aura by animateFloatAsState(if (on) 1f else 0f, tween(Motion.d(900)), label = "aura")
    val level by animateFloatAsState(
        when {
            on -> 1f
            connecting -> 0.55f
            else -> 0f
        },
        tween(Motion.d(1400)),
        label = "level"
    )
    val iconScale by animateFloatAsState(
        if (busy) 0.78f else 1f,
        spring(dampingRatio = 0.35f, stiffness = Motion.k(180f)),
        label = "iconScale"
    )
    val rest by animateFloatAsState(
        if (on) 1.04f else 0.96f,
        spring(dampingRatio = 0.4f, stiffness = Motion.k(90f)),
        label = "rest"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(dampingRatio = 0.35f, stiffness = Motion.k(380f)),
        label = "press"
    )
    val burst = remember { Animatable(1f) }
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { t ->
                if (last != 0L) {
                    phase = (phase + (t - last) / 1_000_000_000f * speed / Motion.scale) % (TAU * 50f)
                }
                last = t
            }
        }
    }
    LaunchedEffect(on) {
        if (on) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            burst.snapTo(0f)
            burst.animateTo(1f, tween(Motion.d(1200), easing = FastOutSlowInEasing))
        }
    }

    val ring = scheme.primary
    val accent = scheme.tertiary
    val idle = scheme.secondaryContainer
    Box(
        Modifier
            .size(256.dp)
            .graphicsLayer {
                val s = rest * press
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawLaunch(
                style = style,
                phase = phase,
                amp = amp,
                orbit = orbit,
                aura = aura,
                level = level,
                burst = burst.value,
                body = container,
                idle = idle,
                content = content,
                ring = ring,
                accent = accent,
                base = size.minDimension * 0.31f
            )
        }
        Box(
            Modifier
                .size(176.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = !busy
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_power),
                contentDescription = null,
                tint = if (style == 3 && !on) scheme.primary else content,
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        rotationZ = if (connecting) sin(phase * 2f) * 12f else 0f
                    }
            )
        }
    }
}

@Composable
fun LaunchPreview(style: Int, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "preview")
    val phase by transition.animateFloat(
        0f,
        TAU * 4f,
        infiniteRepeatable(tween(Motion.d(8000), easing = LinearEasing), RepeatMode.Restart),
        label = "previewPhase"
    )
    val level by transition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(Motion.d(3200), easing = LinearEasing), RepeatMode.Restart),
        label = "previewLevel"
    )
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerHighest,
        tween(Motion.d(300)),
        label = "previewBg"
    )
    val radius by animateDpAsState(
        if (selected) 32.dp else 22.dp,
        spring(dampingRatio = 0.55f, stiffness = Motion.k(300f)),
        label = "previewRadius"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(radius),
        color = container
    ) {
        Canvas(Modifier.size(92.dp)) {
            drawLaunch(
                style = style,
                phase = phase,
                amp = 0.09f,
                orbit = 1f,
                aura = 0f,
                level = level,
                burst = 1f,
                body = scheme.primary,
                idle = scheme.secondaryContainer,
                content = scheme.onPrimary,
                ring = scheme.primary,
                accent = scheme.tertiary,
                base = size.minDimension * 0.22f
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(painterResource(icon), null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun ConnectWave(status: Status) {
    val progress = remember { Animatable(1f) }
    var last by remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (status == Status.On && last != Status.On) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(Motion.d(1500), easing = FastOutSlowInEasing))
        }
        last = status
    }
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        if (p < 1f) {
            val center = Offset(size.width / 2f, 260.dp.toPx())
            val maxR = hypot(size.width, size.height)
            drawCircle(
                color.copy(alpha = (1f - p) * 0.24f),
                radius = maxR * p,
                center = center,
                style = Stroke(width = 110.dp.toPx())
            )
            drawCircle(
                color.copy(alpha = (1f - p) * 0.10f),
                radius = maxR * p * 0.75f,
                center = center
            )
        }
    }
}

@Composable
fun StatPill(icon: Int?, text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(painterResource(icon), null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
fun PingPill(result: PingResult?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val best = result?.best() ?: Int.MAX_VALUE
    val idle = result == null || result.busy
    val container by animateColorAsState(
        when {
            idle -> scheme.surfaceContainerHighest
            best == Int.MAX_VALUE -> scheme.errorContainer
            best < 150 -> scheme.tertiaryContainer
            best < 400 -> scheme.secondaryContainer
            else -> scheme.errorContainer
        },
        tween(Motion.d(400)),
        label = "pingBg"
    )
    val content by animateColorAsState(
        when {
            idle -> scheme.onSurfaceVariant
            best == Int.MAX_VALUE -> scheme.onErrorContainer
            best < 150 -> scheme.onTertiaryContainer
            best < 400 -> scheme.onSecondaryContainer
            else -> scheme.onErrorContainer
        },
        tween(Motion.d(400)),
        label = "pingFg"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
        modifier = Modifier.animateContentSize(spring(dampingRatio = 0.6f, stiffness = Motion.k(400f)))
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (result != null && result.busy) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = content)
            } else if (result == null) {
                Icon(painterResource(R.drawable.ic_bolt), null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Пинг", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    result.tcp?.let { Text(pingText("TCP", it), style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                    result.udp?.let { Text(pingText("UDP", it), style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                    result.icmp?.let { Text(pingText("ICMP", it), style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                }
            }
        }
    }
}

private fun pingText(label: String, value: Int): String = when {
    value > 0 -> "$label $value мс"
    value == Ping.SILENT -> "$label нет ответа"
    else -> "$label ×"
}

@Composable
fun ServerCard(
    server: Server,
    selected: Boolean,
    active: Boolean,
    ping: PingResult?,
    check: ServiceCheck?,
    bestMask: Int,
    checked: Boolean?,
    onPing: () -> Unit,
    enterDelay: Long,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(enterDelay)
        appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Motion.k(220f)))
    }
    val marked = checked == true
    val radius by animateDpAsState(
        if (selected || marked) 36.dp else 24.dp,
        spring(dampingRatio = 0.55f, stiffness = Motion.k(300f)),
        label = "cardRadius"
    )
    val container by animateColorAsState(
        when {
            marked -> scheme.secondaryContainer
            selected -> scheme.primaryContainer
            else -> scheme.surfaceContainerHigh
        },
        tween(Motion.d(300)),
        label = "card"
    )
    val onContainer by animateColorAsState(
        when {
            marked -> scheme.onSecondaryContainer
            selected -> scheme.onPrimaryContainer
            else -> scheme.onSurface
        },
        tween(Motion.d(300)),
        label = "cardText"
    )
    val badge by animateColorAsState(
        if (marked || (selected && checked == null)) scheme.primary else scheme.surfaceContainerHighest,
        tween(Motion.d(300)),
        label = "badge"
    )
    val badgeContent by animateColorAsState(
        if (marked || (selected && checked == null)) scheme.onPrimary else scheme.onSurfaceVariant,
        tween(Motion.d(300)),
        label = "badgeContent"
    )
    val badgeShape by animateDpAsState(
        if (marked || selected) 24.dp else 16.dp,
        spring(dampingRatio = 0.5f, stiffness = Motion.k(300f)),
        label = "badgeShape"
    )
    val lift by animateFloatAsState(
        if (marked) 0.96f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Motion.k(400f)),
        label = "lift"
    )
    Surface(
        shape = RoundedCornerShape(radius),
        color = container,
        contentColor = onContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 40.dp.toPx()
                scaleX = lift
                scaleY = lift
            }
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(badgeShape))
                    .background(badge),
                contentAlignment = Alignment.Center
            ) {
                val showCheck = marked || (selected && checked == null)
                Icon(
                    painter = painterResource(if (showCheck) R.drawable.ic_check else R.drawable.ic_dns),
                    contentDescription = null,
                    tint = badgeContent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    server.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (active) "Подключено" else server.endpoint.ifEmpty { "Сервер" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) scheme.primary else onContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (check != null) {
                    Spacer(Modifier.size(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (i in 0 until 3) {
                            val v = check.ms.getOrNull(i)
                            ServiceLogo(i, v, check.busy && v == null, (bestMask and (1 shl i)) != 0, 20.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            PingPill(ping, onPing)
        }
    }
}

@Composable
fun GroupHeader(
    name: String,
    count: Int,
    collapsed: Boolean,
    checked: Boolean?,
    canRefresh: Boolean,
    refreshing: Boolean,
    onPing: () -> Unit,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    enterDelay: Long,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(enterDelay)
        appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Motion.k(220f)))
    }
    val rotation by animateFloatAsState(
        if (collapsed) -90f else 0f,
        spring(dampingRatio = 0.55f, stiffness = Motion.k(300f)),
        label = "chevron"
    )
    val tint by animateColorAsState(
        if (checked == true) scheme.primary else scheme.onSurfaceVariant,
        tween(Motion.d(250)),
        label = "headerTint"
    )
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 6.dp)
            .graphicsLayer {
                alpha = appear.value
                translationX = (1f - appear.value) * -30.dp.toPx()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (checked == true) R.drawable.ic_check else R.drawable.ic_expand),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { rotationZ = if (checked == null) rotation else 0f }
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name.ifBlank { "Без группы" },
            style = MaterialTheme.typography.titleSmall,
            color = scheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Surface(shape = CircleShape, color = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        if (refreshing) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
        }
        IconButton(onClick = onPing) {
            Icon(painterResource(R.drawable.ic_bolt), "Проверить пинг группы", Modifier.size(20.dp))
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(painterResource(R.drawable.ic_more), "Действия", Modifier.size(22.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (canRefresh) {
                    DropdownMenuItem(
                        text = { Text("Обновить подписку") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_refresh), null, Modifier.size(20.dp)) },
                        onClick = {
                            menu = false
                            onRefresh()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Проверить пинг") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_bolt), null, Modifier.size(20.dp)) },
                    onClick = {
                        menu = false
                        onPing()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Очистить содержимое") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_select_all), null, Modifier.size(20.dp)) },
                    onClick = {
                        menu = false
                        onClear()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Удалить группу", color = scheme.error) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(20.dp), tint = scheme.error)
                    },
                    onClick = {
                        menu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyServers(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EmptyIllustration()
        Text("Серверов пока нет", style = MaterialTheme.typography.titleLarge)
        Text(
            "Вставьте ссылку, подписку или импортируйте настройки из Happ, v2rayNG и v2rayN",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(8.dp))
        androidx.compose.material3.Button(onClick = onAdd) {
            Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Добавить")
        }
    }
}

@Composable
fun BurnInGuard(enabled: Boolean, content: @Composable () -> Unit) {
    val steps = remember { listOf(0 to 0, 6 to 4, -6 to 6, -4 to -5, 5 to -6) }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(enabled) {
        if (enabled) {
            while (true) {
                delay(60_000)
                index = (index + 1) % steps.size
            }
        } else {
            index = 0
        }
    }
    val x by animateDpAsState(steps[index].first.dp, tween(Motion.d(4000)), label = "burnX")
    val y by animateDpAsState(steps[index].second.dp, tween(Motion.d(4000)), label = "burnY")
    Box(Modifier.fillMaxSize().offset(x, y)) { content() }
}
