package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF064E3B),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldLight,
    secondary = AmberSecondary,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = ExpenseRed,
    onTertiary = Color(0xFF451A1A),
    tertiaryContainer = ExpenseRedContainer,
    onTertiaryContainer = ExpenseRedLight,
    background = DarkBg,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = AmberDark,
    onSecondary = Color.White,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = AmberOnContainerLight,
    tertiary = ExpenseRedDark,
    onTertiary = Color.White,
    tertiaryContainer = ExpenseRedLight,
    onTertiaryContainer = ExpenseRedOnContainerLight,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

@Composable
fun CatatKasTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // disabled by default so custom human aesthetic shines
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
