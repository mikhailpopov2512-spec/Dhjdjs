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
    primary = NightPrimary,
    onPrimary = NightBackground,
    primaryContainer = NightContainer,
    onPrimaryContainer = NightTextPrimary,
    secondary = NightSecondary,
    onSecondary = NightBackground,
    tertiary = NightTertiary,
    background = NightBackground,
    onBackground = NightTextPrimary,
    surface = NightSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightContainer,
    onSurfaceVariant = NightTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = BurgundyPrimary,
    onPrimary = PaperBackground,
    primaryContainer = PaperContainer,
    onPrimaryContainer = InkTextPrimary,
    secondary = BurgundySecondary,
    onSecondary = PaperBackground,
    tertiary = BurgundyTertiary,
    background = PaperBackground,
    onBackground = InkTextPrimary,
    surface = PaperSurface,
    onSurface = InkTextPrimary,
    surfaceVariant = PaperContainer,
    onSurfaceVariant = InkTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false by default to showcase our gorgeous custom Ozhegov design!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
