package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WeatherSkyBlueDark,
    onPrimary = WeatherOnPrimaryDark,
    primaryContainer = WeatherPrimaryContainerDark,
    onPrimaryContainer = WeatherOnPrimaryContainerDark,
    secondary = WeatherSecondaryDark,
    background = WeatherBackgroundDark,
    surface = WeatherSurfaceDark,
    surfaceVariant = WeatherSurfaceVariantDark,
    onBackground = WeatherOnBackgroundDark,
    onSurface = WeatherOnSurfaceDark,
    onSurfaceVariant = WeatherOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = WeatherSkyBlueLight,
    onPrimary = WeatherOnPrimaryLight,
    primaryContainer = WeatherPrimaryContainerLight,
    onPrimaryContainer = WeatherOnPrimaryContainerLight,
    secondary = WeatherSecondaryLight,
    background = WeatherBackgroundLight,
    surface = WeatherSurfaceLight,
    surfaceVariant = WeatherSurfaceVariantLight,
    onBackground = WeatherOnBackgroundLight,
    onSurface = WeatherOnSurfaceLight,
    onSurfaceVariant = WeatherOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep weather identity consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
