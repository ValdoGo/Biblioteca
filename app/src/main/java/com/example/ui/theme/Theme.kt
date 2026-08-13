package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NaturalDarkPrimary,
    onPrimary = NaturalDarkOnPrimary,
    primaryContainer = NaturalDarkPrimaryContainer,
    onPrimaryContainer = NaturalDarkOnPrimaryContainer,
    secondary = NaturalDarkSecondary,
    onSecondary = NaturalDarkOnSecondary,
    secondaryContainer = NaturalDarkSecondaryContainer,
    onSecondaryContainer = NaturalDarkOnSecondaryContainer,
    tertiary = NaturalDarkTertiary,
    onTertiary = NaturalDarkOnTertiary,
    tertiaryContainer = NaturalDarkTertiaryContainer,
    background = NaturalDarkBackground,
    onBackground = NaturalDarkOnBackground,
    surface = NaturalDarkSurface,
    onSurface = NaturalDarkOnSurface,
    surfaceVariant = NaturalDarkSurfaceVariant,
    onSurfaceVariant = NaturalDarkOnSurfaceVariant,
    outline = NaturalDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalPrimary,
    onPrimary = NaturalOnPrimary,
    primaryContainer = NaturalPrimaryContainer,
    onPrimaryContainer = NaturalOnPrimaryContainer,
    secondary = NaturalSecondary,
    onSecondary = NaturalOnSecondary,
    secondaryContainer = NaturalSecondaryContainer,
    onSecondaryContainer = NaturalOnSecondaryContainer,
    tertiary = NaturalTertiary,
    onTertiary = NaturalOnTertiary,
    tertiaryContainer = NaturalTertiaryContainer,
    background = NaturalBackground,
    onBackground = NaturalOnBackground,
    surface = NaturalSurface,
    onSurface = NaturalOnSurface,
    surfaceVariant = NaturalSurfaceVariant,
    onSurfaceVariant = NaturalOnSurfaceVariant,
    outline = NaturalOutline,
    outlineVariant = NaturalOutlineVariant
)

@Composable
fun BibliotecaDigitalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

