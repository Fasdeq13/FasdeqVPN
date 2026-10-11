package app.fasdeq.vpn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import app.fasdeq.vpn.R
import app.fasdeq.vpn.Server
import app.fasdeq.vpn.Status
import kotlinx.coroutines.delay

@Composable
fun PowerButton(status: Status, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val on = status == Status.On
    val busy = status == Status.Connecting || status == Status.Stopping
    val container by animateColorAsState(
        if (on) scheme.primary else scheme.secondaryContainer,
        tween(500),
        label = "container"
    )
    val content by animateColorAsState(
        if (on) scheme.onPrimary else scheme.onSecondaryContainer,
        tween(500),
        label = "content"
    )
    val corner by animateFloatAsState(
        if (on) 84f else if (busy) 72f else 56f,
        spring(dampingRatio = 0.5f, stiffness = 160f),
        label = "corner"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "press"
    )
    val rest by animateFloatAsState(
        if (on) 1f else 0.95f,
        spring(dampingRatio = 0.5f, stiffness = 120f),
        label = "rest"
    )
    val busyAmount by animateFloatAsState(if (busy) 1f else 0f, tween(350), label = "busyAmount")
    val ringAmount by animateFloatAsState(if (on) 1f else 0f, tween(700), label = "ringAmount")
    val iconScale by animateFloatAsState(
        if (busy) 0.8f else 1f,
        spring(dampingRatio = 0.4f, stiffness = 220f),
        label = "iconScale"
    )
    val transition = rememberInfiniteTransition(label = "pulse")
    val first = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "first"
    )
    val second = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = LinearEasing),
            RepeatMode.Restart,
            StartOffset(1300)
        ),
        label = "second"
    )
    val breath = transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "breath"
    )
    val ringColor = scheme.primary

    Box(Modifier.size(248.dp), contentAlignment = Alignment.Center) {
        if (ringAmount > 0.01f) {
            listOf(first, second).forEach { progress ->
                Box(
                    Modifier
                        .size(168.dp)
                        .graphicsLayer {
                            val s = 1f + 0.45f * progress.value
                            scaleX = s
                            scaleY = s
                            alpha = (1f - progress.value) * 0.35f * ringAmount
                        }
                        .background(ringColor, CircleShape)
                )
            }
        }
        AnimatedVisibility(
            visible = busy,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
            exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.8f)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(206.dp),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round
            )
        }
        Surface(
            onClick = onClick,
            enabled = !busy,
            interactionSource = interaction,
            shape = RoundedCornerShape(corner.dp),
            color = container,
            contentColor = content,
            shadowElevation = if (on) 10.dp else 0.dp,
            modifier = Modifier
                .size(168.dp)
                .graphicsLayer {
                    val s = lerp(rest, breath.value, busyAmount) * press
                    scaleX = s
                    scaleY = s
                }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_power),
                    contentDescription = null,
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
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
fun ServerCard(
    server: Server,
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "cardPress"
    )
    val radius by animateDpAsState(
        if (selected) 36.dp else 24.dp,
        spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "cardRadius"
    )
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
        tween(300),
        label = "card"
    )
    val onContainer by animateColorAsState(
        if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        tween(300),
        label = "cardText"
    )
    val badge by animateColorAsState(
        if (selected) scheme.primary else scheme.surfaceContainerHighest,
        tween(300),
        label = "badge"
    )
    val badgeContent by animateColorAsState(
        if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        tween(300),
        label = "badgeContent"
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(radius),
        color = container,
        contentColor = onContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(if (selected) 24.dp else 16.dp))
                    .background(badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (selected) R.drawable.ic_check else R.drawable.ic_dns),
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
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Удалить",
                    modifier = Modifier.size(22.dp)
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
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dns),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.size(4.dp))
        Text("Серверов пока нет", style = MaterialTheme.typography.titleLarge)
        Text(
            "Вставьте ссылку или конфигурацию из буфера обмена, и всё готово",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
    val x by animateDpAsState(steps[index].first.dp, tween(4000), label = "burnX")
    val y by animateDpAsState(steps[index].second.dp, tween(4000), label = "burnY")
    Box(Modifier.fillMaxSize().offset(x, y)) { content() }
}
