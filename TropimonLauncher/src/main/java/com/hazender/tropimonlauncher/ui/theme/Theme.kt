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

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
            typography = RighteousTypography,
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
            indicatorColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),

            // L'icône et le texte deviennent CLAIRS (bleu-cyan)
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,

        )

    @Composable
    fun filterChipColors(): SelectableChipColors {
        val colors = MaterialTheme.colorScheme

        return SelectableChipColors(
            containerColor               = colors.surfaceBright.copy(alpha = 0.65f),
            labelColor                   = colors.onSurfaceVariant,
            leadingIconColor             = colors.onSurfaceVariant,
            trailingIconColor            = colors.onSurfaceVariant,

            disabledContainerColor       = Color.Transparent,
            disabledLabelColor           = colors.onSurface.copy(alpha = 0.38f),
            disabledLeadingIconColor     = colors.onSurface.copy(alpha = 0.38f),
            disabledTrailingIconColor    = colors.onSurface.copy(alpha = 0.38f),

            selectedContainerColor       = colors.surfaceBright.copy(alpha = 0.65f),
            disabledSelectedContainerColor = colors.primary.copy(alpha = 0.12f),

            selectedLabelColor           = colors.primary,
            selectedLeadingIconColor     = colors.primary,
            selectedTrailingIconColor    = colors.primary,
        )
    }

    @Composable
    fun checkChipBorder(
        selected: Boolean,
        enabled: Boolean = true
    ): BorderStroke {
        val colors = MaterialTheme.colorScheme

        if (!enabled) {
            return BorderStroke(
                width = 1.dp,
                color = colors.outline.copy(alpha = 0.12f)
            )
        }

        return BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.primary else colors.outline
        )
    }
}