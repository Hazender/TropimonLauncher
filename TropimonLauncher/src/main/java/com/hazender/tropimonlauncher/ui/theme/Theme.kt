/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.hazender.tropimonlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.hazender.tropimonlauncher.viewmodel.BackgroundViewModel
import com.hazender.tropimonlauncher.viewmodel.LocalBackgroundViewModel

// Définition directe et unique du thème de l'application.
private val tropimonColorScheme = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = scrim,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    inversePrimary = inversePrimary,
    surfaceDim = surfaceDim,
    surfaceBright = surfaceBright,
    surfaceContainerLowest = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    surfaceTint = primary,
    primaryFixed = primaryContainer,
    primaryFixedDim = primaryContainer,
    onPrimaryFixed = onPrimaryContainer,
    onPrimaryFixedVariant = onPrimaryContainer,
    secondaryFixed = secondaryContainer,
    secondaryFixedDim = secondaryContainer,
    onSecondaryFixed = onSecondaryContainer,
    onSecondaryFixedVariant = onSecondaryContainer,
    tertiaryFixed = tertiaryContainer,
    tertiaryFixedDim = tertiaryContainer,
    onTertiaryFixed = onTertiaryContainer,
    onTertiaryFixedVariant = onTertiaryContainer
)

@Composable
fun ZalithLauncherTheme(
    backgroundViewModel: BackgroundViewModel? = null,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalBackgroundViewModel provides backgroundViewModel) {
        MaterialTheme(
            colorScheme = tropimonColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
object TropimonTheme {
    /**
     * Définit les couleurs par défaut pour les NavigationRailItem de l'application,
     * en utilisant le style "inversé" (fond sombre, contenu clair).
     */
    @Composable
    fun navigationRailItemColors(): NavigationRailItemColors =
        NavigationRailItemDefaults.colors(
            // Le fond de la sélection devient SOMBRE
            indicatorColor = MaterialTheme.colorScheme.outlineVariant,

            // L'icône et le texte deviennent CLAIRS (bleu-cyan)
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,

        )
}