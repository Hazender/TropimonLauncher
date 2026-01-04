package com.hazender.tropimonlauncher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hazender.tropimonlauncher.R
import com.hazender.tropimonlauncher.game.server.MinecraftServerStatus
import com.hazender.tropimonlauncher.game.server.ServerStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Indicateur de statut du serveur Minecraft
 * Récupère automatiquement toutes les infos depuis androidConfig.json via CloudflareR2Service
 * Affiche le nombre de joueurs en ligne avec animation
 */
@Composable
fun ServerStatusIndicator(
    modifier: Modifier = Modifier,
    updateInterval: Long = 30_000L
) {
    var serverStatus by remember { mutableStateOf<ServerStatus?>(null) }
    var isChecking by remember { mutableStateOf(true) }

    // Ping périodique
    LaunchedEffect(updateInterval) {
        while (isActive) {
            isChecking = true
            serverStatus = MinecraftServerStatus.pingServer()
            isChecking = false
            delay(updateInterval)
        }
    }

    // Animation de pulsation douce pendant le chargement initial
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val playerCountText = when {
        isChecking && serverStatus == null -> "..."
        else -> (serverStatus?.takeIf { it.online }?.playerCount ?: 0).toString()
    }

    // Toujours la même couleur primaire, même si offline
    val textColor = if (isChecking && serverStatus == null) {
        LocalContentColor.current.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Nombre de joueurs
        Text(
            text = playerCountText,
            style = MaterialTheme.typography.titleMedium, // Même taille que le texte à côté
            color = textColor,
            modifier = Modifier.alpha(if (isChecking && serverStatus == null) pulseAlpha else 1f)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = stringResource(R.string.main_tropimon_serverstatus),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.alpha(if (isChecking && serverStatus == null) pulseAlpha else 1f)
        )
    }
}