/*
 * Tropimon Launcher
 * Copyright (C) 2025 Hazender
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hazender.tropimonlauncher.R
import com.hazender.tropimonlauncher.game.server.MinecraftServerStatus
import com.hazender.tropimonlauncher.game.server.ServerStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Cache global pour le statut du serveur
 * Persiste pendant toute la durée de vie de l'application
 */
object ServerStatusCache {
    var cachedStatus: ServerStatus? by mutableStateOf(null)
        private set

    fun updateStatus(newStatus: ServerStatus?) {
        if (newStatus != null) {
            cachedStatus = newStatus
        }
    }
}

/**
 * Indicateur de statut du serveur Minecraft
 * Récupère automatiquement toutes les infos depuis androidConfig.json via CloudflareR2Service
 * Affiche le nombre de joueurs en ligne avec animation
 * Conserve la dernière valeur récupérée avec succès pour toute la durée de l'app
 * Actualise immédiatement au retour au premier plan
 */
@Composable
fun ServerStatusIndicator(
    modifier: Modifier = Modifier,
    updateInterval: Long = 30_000L
) {
    // Utilise le cache global qui persiste même si le composable est détruit
    val cachedServerStatus = ServerStatusCache.cachedStatus

    // Trigger pour forcer une mise à jour immédiate
    var refreshTrigger by remember { mutableStateOf(0) }

    // Détection du retour au premier plan
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // L'app revient au premier plan, on force une mise à jour
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Ping périodique
    LaunchedEffect(updateInterval, refreshTrigger) {
        while (isActive) {
            val newStatus = MinecraftServerStatus.pingServer()
            ServerStatusCache.updateStatus(newStatus)
            delay(updateInterval)
        }
    }

    // Animation de pulsation douce pendant le chargement initial uniquement
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
        cachedServerStatus == null -> "..." // Aucune donnée jamais récupérée
        else -> (cachedServerStatus.takeIf { it.online }?.playerCount ?: 0).toString()
    }

    // Couleur : animation seulement si on n'a jamais eu de données
    val textColor = if (cachedServerStatus == null) {
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
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier.alpha(if (cachedServerStatus == null) pulseAlpha else 1f)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = stringResource(R.string.main_tropimon_serverstatus),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.alpha(if (cachedServerStatus == null) pulseAlpha else 1f)
        )
    }
}