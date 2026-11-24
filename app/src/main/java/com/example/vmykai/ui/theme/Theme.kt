package com.example.vmykai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Palette.IconColor,
    // `primary` (amber) is the same brand hue in both themes, so its `on*` ink stays the dark
    // text color in both schemes too - using TextNight here would put near-white text on amber.
    onPrimary = Palette.TextDay,
    primaryContainer = Palette.IconBgNight,
    onPrimaryContainer = Palette.IconColor,
    secondary = Palette.PowerOn,
    onSecondary = Color.White,
    secondaryContainer = Palette.StatusOnBgNight,
    onSecondaryContainer = Palette.StatusOnInkNight,
    tertiary = Palette.PowerOff,
    tertiaryContainer = Palette.StatusOffBgNight,
    onTertiaryContainer = Palette.StatusOffInkNight,
    background = Palette.BgNight,
    onBackground = Palette.TextNight,
    surface = Palette.SurfaceNight,
    onSurface = Palette.TextNight,
    surfaceVariant = Palette.SurfaceNight,
    onSurfaceVariant = Palette.SecondaryNight,
    outline = Palette.DividerNight,
    outlineVariant = Palette.DividerNight,
    error = Palette.UnseenDotNight,
)

private val LightColorScheme = lightColorScheme(
    primary = Palette.IconColor,
    onPrimary = Palette.TextDay,
    primaryContainer = Palette.IconBgDay,
    onPrimaryContainer = Palette.IconColor,
    secondary = Palette.PowerOn,
    onSecondary = Color.White,
    secondaryContainer = Palette.StatusOnBgDay,
    onSecondaryContainer = Palette.StatusOnInkDay,
    tertiary = Palette.PowerOff,
    tertiaryContainer = Palette.StatusOffBgDay,
    onTertiaryContainer = Palette.StatusOffInkDay,
    background = Palette.BgDay,
    onBackground = Palette.TextDay,
    surface = Palette.SurfaceDay,
    onSurface = Palette.TextDay,
    surfaceVariant = Palette.SurfaceDay,
    onSurfaceVariant = Palette.SecondaryDay,
    outline = Palette.DividerDay,
    outlineVariant = Palette.DividerDay,
    error = Palette.UnseenDotDay,
)

private val AppShapesM3 = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = AppShapes.StatusCard,
    large = AppShapes.Card,
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The brand palette is authoritative app-wide: Material You dynamic color is intentionally not
 * supported here, so the app doesn't visually fork between "branded" components (status card,
 * ribbon, widgets, which read [Palette] directly) and wallpaper-tinted generic chrome.
 */
@Composable
fun VmykaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapesM3,
        content = content
    )
}
