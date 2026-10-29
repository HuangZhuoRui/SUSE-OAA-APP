package com.suseoaa.projectoaa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// 亮色模式 - 青蟹主题配色（全量显式指定，不回落任何 MD3 默认紫粉取色）
private val LightColorScheme = lightColorScheme(
    // 主色组 核心交互元素 (按钮等)
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    inversePrimary = md_theme_light_inversePrimary,

    // 次要色组 BottomBar 选中状态依赖这里！
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer, // 选中背景：浅云蓝 (#E3F2FD)
    onSecondaryContainer = md_theme_light_onSecondaryContainer, // 选中图标：电光蓝

    // 第三强调色（青蟹青，与启动页渐变协调）
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,

    // 背景与表面阶梯
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    surfaceTint = md_theme_light_primary,
    surfaceBright = LightSurfaceBright,
    surfaceDim = LightSurfaceDim,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,

    // 反色与遮罩
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    scrim = Color(0xFF000000),

    // 错误色
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,

    // 边框
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,

    // Fixed 色阶
    primaryFixed = SoftBlueWait,
    primaryFixedDim = Color(0xFFBDD9FF),
    onPrimaryFixed = Color(0xFF002B5C),
    onPrimaryFixedVariant = ElectricBlue,
    secondaryFixed = SoftBlueWait,
    secondaryFixedDim = Color(0xFFBAE6FD),
    onSecondaryFixed = Color(0xFF002B42),
    onSecondaryFixedVariant = OceanSkyBlue,
    tertiaryFixed = SoftCyanContainer,
    tertiaryFixedDim = Color(0xFF80DEEA),
    onTertiaryFixed = Color(0xFF00363D),
    onTertiaryFixedVariant = QingxieCyan,
)

// 暗色模式 - 青蟹主题配色（全量显式指定，不回落任何 MD3 默认紫灰取色）
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    inversePrimary = md_theme_dark_inversePrimary,

    // BottomBar 选中态
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer, // 深蓝背景
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,  // 白色图标 (高亮)

    // 第三强调色（夜间青蟹亮青）
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,

    // 背景与表面阶梯
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    surfaceTint = md_theme_dark_primary,
    surfaceBright = NightSurfaceBright,
    surfaceDim = NightSurfaceDim,
    surfaceContainerLowest = NightSurfaceContainerLowest,
    surfaceContainerLow = NightSurfaceContainerLow,
    surfaceContainer = NightSurfaceContainer,
    surfaceContainerHigh = NightSurfaceContainerHigh,
    surfaceContainerHighest = NightSurfaceContainerHighest,

    // 反色与遮罩
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    scrim = Color(0xFF000000),

    // 错误色
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,

    // 边框
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,

    // Fixed 色阶
    primaryFixed = SoftBlueWait,
    primaryFixedDim = Color(0xFFBDD9FF),
    onPrimaryFixed = Color(0xFF002B5C),
    onPrimaryFixedVariant = ElectricBlue,
    secondaryFixed = SoftBlueWait,
    secondaryFixedDim = Color(0xFFBAE6FD),
    onSecondaryFixed = Color(0xFF002B42),
    onSecondaryFixedVariant = OceanSkyBlue,
    tertiaryFixed = SoftCyanContainer,
    tertiaryFixedDim = Color(0xFF80DEEA),
    onTertiaryFixed = Color(0xFF00363D),
    onTertiaryFixedVariant = QingxieCyan,
)

// 字体排版
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ProjectOAATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    dynamicPaletteLightColorHex: String? = null,
    dynamicPaletteDarkColorHex: String? = null,
    content: @Composable () -> Unit
) {
    val paletteHex = if (darkTheme) dynamicPaletteDarkColorHex else dynamicPaletteLightColorHex
    val customAccent = paletteHex?.takeIf { dynamicColor }?.toColorOrNull()

    val colorScheme = if (customAccent != null) {
        val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
        baseScheme.withPaletteAccent(customAccent, darkTheme)
    } else {
        platformColorScheme(
            darkTheme = darkTheme,
            dynamicColor = dynamicColor,
            defaultLightScheme = LightColorScheme,
            defaultDarkScheme = DarkColorScheme
        )
    }

    CompositionLocalProvider(LocalTonalElevationEnabled provides false) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

private fun ColorScheme.withPaletteAccent(accent: Color, darkTheme: Boolean): ColorScheme {
    val container = if (darkTheme) blendArgb(accent, Color.Black, 0.62f) else blendArgb(accent, Color.White, 0.84f)
    val onContainer = if (darkTheme) blendArgb(accent, Color.White, 0.72f) else blendArgb(accent, Color.Black, 0.25f)
    val secondary = if (darkTheme) blendArgb(accent, Color.White, 0.16f) else blendArgb(accent, Color.Black, 0.08f)
    val tertiary = if (darkTheme) blendArgb(accent, Color.White, 0.28f) else blendArgb(accent, Color.Black, 0.16f)
    val tertiaryContainer = if (darkTheme) blendArgb(tertiary, Color.Black, 0.65f) else blendArgb(tertiary, Color.White, 0.85f)
    val onTertiaryContainer = if (darkTheme) blendArgb(tertiary, Color.White, 0.70f) else blendArgb(tertiary, Color.Black, 0.30f)

    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val onSecondary = if (secondary.luminance() > 0.5f) Color.Black else Color.White
    val onTertiary = if (tertiary.luminance() > 0.5f) Color.Black else Color.White

    return copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = container,
        onSecondaryContainer = if (darkTheme) Color.White else accent,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        surfaceTint = accent,
        surfaceVariant = blendArgb(surfaceVariant, accent, if (darkTheme) 0.08f else 0.05f),
        surfaceContainerLow = blendArgb(surfaceContainerLow, accent, if (darkTheme) 0.04f else 0.02f),
        surfaceContainer = blendArgb(surfaceContainer, accent, if (darkTheme) 0.06f else 0.04f),
        surfaceContainerHigh = blendArgb(surfaceContainerHigh, accent, if (darkTheme) 0.08f else 0.05f),
        surfaceContainerHighest = blendArgb(surfaceContainerHighest, accent, if (darkTheme) 0.10f else 0.06f),
        primaryFixed = container,
        primaryFixedDim = blendArgb(container, accent, 0.2f),
        onPrimaryFixed = blendArgb(accent, Color.Black, 0.45f),
        onPrimaryFixedVariant = accent,
        secondaryFixed = container,
        secondaryFixedDim = blendArgb(container, secondary, 0.2f),
        onSecondaryFixed = blendArgb(secondary, Color.Black, 0.45f),
        onSecondaryFixedVariant = secondary,
        tertiaryFixed = tertiaryContainer,
        tertiaryFixedDim = blendArgb(tertiaryContainer, tertiary, 0.2f),
        onTertiaryFixed = blendArgb(tertiary, Color.Black, 0.45f),
        onTertiaryFixedVariant = tertiary,
    )
}

private fun String.toColorOrNull(): Color? {
    val cleaned = trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
    val argbInt = try {
        when (cleaned.length) {
            6 -> (0xFF000000 or cleaned.toLong(16)).toInt()
            8 -> cleaned.toLong(16).toInt()
            else -> return null
        }
    } catch (_: Exception) {
        return null
    }
    return Color(argbInt)
}

private fun blendArgb(from: Color, to: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    val fromArgb = from.toArgb()
    val toArgb = to.toArgb()

    val fa = (fromArgb ushr 24) and 0xFF
    val fr = (fromArgb ushr 16) and 0xFF
    val fg = (fromArgb ushr 8) and 0xFF
    val fb = fromArgb and 0xFF

    val ta = (toArgb ushr 24) and 0xFF
    val tr = (toArgb ushr 16) and 0xFF
    val tg = (toArgb ushr 8) and 0xFF
    val tb = toArgb and 0xFF

    val a = (fa + (ta - fa) * t).roundToInt().coerceIn(0, 255)
    val r = (fr + (tr - fr) * t).roundToInt().coerceIn(0, 255)
    val g = (fg + (tg - fg) * t).roundToInt().coerceIn(0, 255)
    val b = (fb + (tb - fb) * t).roundToInt().coerceIn(0, 255)

    val result = (a shl 24) or (r shl 16) or (g shl 8) or b
    return Color(result)
}
