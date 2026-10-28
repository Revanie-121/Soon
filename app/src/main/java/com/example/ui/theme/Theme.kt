package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SonoraDarkColorScheme = darkColorScheme(
    primary = SonoraViolet,
    onPrimary = Color.White,
    primaryContainer = SonoraVioletContainer,
    onPrimaryContainer = SonoraVioletLight,
    secondary = SonoraCyan,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = SonoraCyanContainer,
    onSecondaryContainer = SonoraCyanLight,
    tertiary = StatusCompleted,
    onTertiary = Color.White,
    background = SonoraBackground,
    onBackground = TextPrimary,
    surface = SonoraSurface,
    onSurface = TextPrimary,
    surfaceVariant = SonoraSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SonoraBorder,
    outlineVariant = SonoraBorderLight,
    error = StatusFailed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to true as specified in prompt: "Dark mode sebagai tema utama"
    dynamicColor: Boolean = false, // Keep Sonora's signature violet/cyan brand identity consistent
    content: @Composable () -> Unit
) {
    val colorScheme = SonoraDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SonoraBackground.toArgb()
                window.navigationBarColor = SonoraSurface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
