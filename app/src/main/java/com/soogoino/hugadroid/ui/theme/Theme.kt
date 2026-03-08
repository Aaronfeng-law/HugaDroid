package com.soogoino.hugadroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HugaLightColorScheme = lightColorScheme(
    primary = HugaPrimary,
    onPrimary = HugaOnPrimary,
    primaryContainer = HugaPrimaryContainer,
    onPrimaryContainer = HugaOnPrimaryContainer,
    secondary = HugaSecondary,
    onSecondary = HugaOnSecondary,
    secondaryContainer = HugaSecondaryContainer,
    onSecondaryContainer = HugaOnSecondaryContainer,
    tertiary = HugaTertiary,
    onTertiary = HugaOnTertiary,
    tertiaryContainer = HugaTertiaryContainer,
    onTertiaryContainer = HugaOnTertiaryContainer,
    error = HugaError,
    onError = HugaOnError,
    errorContainer = HugaErrorContainer,
    onErrorContainer = HugaOnErrorContainer,
    background = HugaBackground,
    onBackground = HugaOnBackground,
    surface = HugaSurface,
    onSurface = HugaOnSurface,
    surfaceVariant = HugaSurfaceVariant,
    onSurfaceVariant = HugaOnSurfaceVariant,
    outline = HugaOutline,
    outlineVariant = HugaOutlineVariant,
)

private val HugaDarkColorScheme = darkColorScheme(
    primary = HugaPrimaryDark,
    onPrimary = HugaOnPrimaryDark,
    primaryContainer = HugaPrimaryContainerDark,
    onPrimaryContainer = HugaOnPrimaryContainerDark,
    secondary = HugaSecondaryDark,
    onSecondary = HugaOnSecondaryDark,
    secondaryContainer = HugaSecondaryContainerDark,
    onSecondaryContainer = HugaOnSecondaryContainerDark,
    background = HugaBackgroundDark,
    onBackground = HugaOnBackgroundDark,
    surface = HugaSurfaceDark,
    onSurface = HugaOnSurfaceDark,
    surfaceVariant = HugaSurfaceVariantDark,
    onSurfaceVariant = HugaOnSurfaceVariantDark,
    outline = HugaOutlineDark,
)

@Composable
fun HugaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Android 12+ wallpaper-based colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> HugaDarkColorScheme
        else -> HugaLightColorScheme
    }

    // Make status bar transparent and edge-to-edge
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HugaTypography,
        content = content
    )
}
