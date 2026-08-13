package vn.hocdantranh.guzheng.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DanThanhColorScheme = lightColorScheme(
    primary = VintageColors.sageDark,
    onPrimary = VintageColors.paperWarm,
    primaryContainer = VintageColors.sageSoft,
    onPrimaryContainer = VintageColors.inkDark,
    secondary = VintageColors.wood,
    onSecondary = VintageColors.paperWarm,
    secondaryContainer = VintageColors.woodSoft,
    onSecondaryContainer = VintageColors.inkDark,
    tertiary = VintageColors.plum,
    onTertiary = VintageColors.paperWarm,
    background = VintageColors.paper,
    onBackground = VintageColors.inkDark,
    surface = VintageColors.paperWarm,
    onSurface = VintageColors.inkDark,
    surfaceVariant = VintageColors.paperDeep,
    onSurfaceVariant = VintageColors.inkSoft,
    outline = VintageColors.inkMuted,
    error = VintageColors.sealRed,
    onError = VintageColors.paperWarm
)

@Composable
fun DanThanhTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DanThanhColorScheme,
        typography = DanThanhTypography,
        content = content
    )
}
