package app.fasdeq.vpn.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.fasdeq.vpn.R
import app.fasdeq.vpn.Server
import app.fasdeq.vpn.Status

@Composable
fun PowerButton(status: Status, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val on = status == Status.On
    val busy = status == Status.Connecting || status == Status.Stopping
    val container by animateColorAsState(
        if (on) scheme.primary else scheme.secondaryContainer,
        tween(450),
        label = "container"
    )
    val content by animateColorAsState(
        if (on) scheme.onPrimary else scheme.onSecondaryContainer,
        tween(450),
        label = "content"
    )
    val corner by animateFloatAsState(
        if (on || busy) 84f else 56f,
        spring(dampingRatio = 0.55f, stiffness = 180f),
        label = "corner"
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
    val ringColor = scheme.primary

    Box(Modifier.size(248.dp), contentAlignment = Alignment.Center) {
        if (on) {
            listOf(first, second).forEach { progress ->
                Box(
                    Modifier
                        .size(168.dp)
                        .graphicsLayer {
                            val s = 1f + 0.45f * progress.value
                            scaleX = s
                            scaleY = s
                            alpha = (1f - progress.value) * 0.35f
                        }
                        .background(ringColor, CircleShape)
                )
            }
        }
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(206.dp),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round
            )
        }
        Surface(
            onClick = onClick,
            enabled = !busy,
            shape = RoundedCornerShape(corner.dp),
            color = container,
            contentColor = content,
            shadowElevation = if (on) 10.dp else 0.dp,
            modifier = Modifier.size(168.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_power),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp)
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
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
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
        shape = RoundedCornerShape(28.dp),
        color = container,
        contentColor = onContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
