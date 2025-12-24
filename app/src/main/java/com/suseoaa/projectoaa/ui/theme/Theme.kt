package com.suseoaa.projectoaa.ui.theme

import android.app.Activity
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 暗色模式
private val DarkColorScheme = darkColorScheme(
    primary = NightBlue,               // 亮蓝
    onPrimary = Color.White,
    primaryContainer = NightContainer,
    onPrimaryContainer = Color.White,

    // BottomBar 选中态
    secondary = NightBlue,
    secondaryContainer = NightContainer, // 深蓝背景
    onSecondaryContainer = Color.White,  // 白色图标 (高亮)

    background = NightBackground,      // 纯黑
    onBackground = Color.White,
    surface = NightSurface,            // 深灰卡片
    onSurface = Color.White,

    // 未选中状态
    surfaceVariant = NightSurface,
    onSurfaceVariant = Color(0xFF8E8E93), // 浅灰图标

    outline = Color(0xFF2C2C2E)
)

// 亮色模式
private val LightColorScheme = lightColorScheme(
    // 主色组 核心交互元素 (按钮等)
    primary = ElectricBlue,            // 电光蓝 (#007AFF)
    onPrimary = Color.White,
    primaryContainer = SoftBlueWait,   // 浅蓝容器
    onPrimaryContainer = ElectricBlue,

    // 次要色组 BottomBar 选中状态依赖这里！
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = SoftBlueWait, // 选中背景：浅云蓝 (#E3F2FD)
    onSecondaryContainer = ElectricBlue, // 选中图标：电光蓝

    // 背景与表面
    background = OxygenBackground,     // App 背景：云朵灰白 (#F2F4F6)
    onBackground = InkBlack,           // 文字：墨黑
    surface = OxygenWhite,             // 卡片/BottomBar：纯白 (#FFFFFF)
    onSurface = InkBlack,              // 卡片文字

    // 未选中状态
    surfaceVariant = OxygenBackground,
    onSurfaceVariant = InkGrey,        // 未选中图标：柔和灰 (#70767E)

    // 边框
    outline = OutlineSoft,             // 极淡边框
    outlineVariant = Color(0xFFC7C7CC) // 稍深的边框
)

@Composable
fun ProjectOAATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 强制关闭动态取色，保证风格统一
    dynamicColor: Boolean = false,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏颜色与背景融合
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}