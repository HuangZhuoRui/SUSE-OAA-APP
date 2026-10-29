package com.suseoaa.projectoaa.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== 青蟹（iOS / Oxygen 风格）主题配色方案 ====================

// 亮色模式核心色
val OxygenWhite = Color(0xFFFFFFFF)        // 纯净白：卡片、弹窗、顶栏
val OxygenBackground = Color(0xFFF2F4F7)   // 云朵灰蓝白：通透底色
val ElectricBlue = Color(0xFF007AFF)       // 电光蓝：核心品牌交互主色
val SoftBlueWait = Color(0xFFE3F2FD)       // 浅云蓝：选中状态容器背景
val OceanSkyBlue = Color(0xFF0284C7)       // 晴空蓝：次级强调色
val QingxieCyan = Color(0xFF0097A7)        // 青蟹青：第三强调色（与启动页渐变呼应）
val SoftCyanContainer = Color(0xFFE0F7FA)  // 浅青容器：第三强调色背景
val OnSoftCyanContainer = Color(0xFF00606A)

// 文字与边框颜色（亮色）
val InkBlack = Color(0xFF191C1E)           // 主要文字
val InkGrey = Color(0xFF68707A)            // 次要文字（未选中图标/说明文字）
val OutlineSoft = Color(0xFFE2E8F0)        // 柔和分割线与浅边框
val OutlineMuted = Color(0xFF94A3B8)       // 表单未聚焦描边/弱提示灰蓝

// 亮色表面层级（冷调云白阶梯，彻底告别 MD3 默认薰衣草紫）
val LightSurfaceVariant = Color(0xFFEAF0F6)
val LightSurfaceDim = Color(0xFFDCE4EE)
val LightSurfaceBright = Color(0xFFFFFFFF)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF8FAFC)
val LightSurfaceContainer = Color(0xFFF1F5F9)
val LightSurfaceContainerHigh = Color(0xFFE9EFF6)
val LightSurfaceContainerHighest = Color(0xFFE1E8F0)

// 暗色模式核心色
val NightBackground = Color(0xFF000000)    // 纯黑背景
val NightSurface = Color(0xFF1C1C1E)       // 浮层深灰卡片
val NightBlue = Color(0xFF0A84FF)          // 夜间亮蓝主色
val NightContainer = Color(0xFF002F58)     // 夜间选中深蓝容器
val NightSkyBlue = Color(0xFF38BDF8)       // 夜间次级亮天蓝
val NightCyan = Color(0xFF4DD0E1)          // 夜间青蟹亮青
val NightCyanContainer = Color(0xFF083942) // 夜间深青容器
val OnNightCyanContainer = Color(0xFFB2EBF2)

// 暗色表面层级（冷调深灰阶梯，彻底告别 MD3 紫灰暗色底）
val NightSurfaceVariant = Color(0xFF282C34)
val NightOutline = Color(0xFF636973)
val NightOutlineVariant = Color(0xFF2C3038)
val NightSurfaceDim = Color(0xFF0E1013)
val NightSurfaceBright = Color(0xFF363B46)
val NightSurfaceContainerLowest = Color(0xFF0A0B0E)
val NightSurfaceContainerLow = Color(0xFF16181C)
val NightSurfaceContainer = Color(0xFF1E2026)
val NightSurfaceContainerHigh = Color(0xFF262A32)
val NightSurfaceContainerHighest = Color(0xFF303540)

// 功能语义色
val AlertRed = Color(0xFFFF3B30)           // iOS 风格亮红
val NightAlertRed = Color(0xFFFF6961)      // 夜间柔和亮红

// ==================== Material 3 ColorScheme 映射 ====================

// Light Theme Colors
val md_theme_light_primary = ElectricBlue
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = SoftBlueWait
val md_theme_light_onPrimaryContainer = ElectricBlue
val md_theme_light_secondary = OceanSkyBlue
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = SoftBlueWait
val md_theme_light_onSecondaryContainer = ElectricBlue
val md_theme_light_tertiary = QingxieCyan
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = SoftCyanContainer
val md_theme_light_onTertiaryContainer = OnSoftCyanContainer
val md_theme_light_error = AlertRed
val md_theme_light_errorContainer = Color(0xFFFFECEA)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFFB3261E)
val md_theme_light_background = OxygenBackground
val md_theme_light_onBackground = InkBlack
val md_theme_light_surface = OxygenWhite
val md_theme_light_onSurface = InkBlack
val md_theme_light_surfaceVariant = LightSurfaceVariant
val md_theme_light_onSurfaceVariant = InkGrey
val md_theme_light_outline = OutlineMuted
val md_theme_light_outlineVariant = OutlineSoft
val md_theme_light_inverseOnSurface = Color(0xFFF4F7FA)
val md_theme_light_inverseSurface = Color(0xFF1E242B)
val md_theme_light_inversePrimary = NightBlue

// Dark Theme Colors
val md_theme_dark_primary = NightBlue
val md_theme_dark_onPrimary = Color(0xFFFFFFFF)
val md_theme_dark_primaryContainer = NightContainer
val md_theme_dark_onPrimaryContainer = Color(0xFFD8ECFF)
val md_theme_dark_secondary = NightSkyBlue
val md_theme_dark_onSecondary = Color(0xFF002B42)
val md_theme_dark_secondaryContainer = NightContainer
val md_theme_dark_onSecondaryContainer = Color(0xFFFFFFFF)
val md_theme_dark_tertiary = NightCyan
val md_theme_dark_onTertiary = Color(0xFF00363D)
val md_theme_dark_tertiaryContainer = NightCyanContainer
val md_theme_dark_onTertiaryContainer = OnNightCyanContainer
val md_theme_dark_error = NightAlertRed
val md_theme_dark_errorContainer = Color(0xFF421414)
val md_theme_dark_onError = Color(0xFFFFFFFF)
val md_theme_dark_onErrorContainer = Color(0xFFFFCDC8)
val md_theme_dark_background = NightBackground
val md_theme_dark_onBackground = Color(0xFFFFFFFF)
val md_theme_dark_surface = NightSurface
val md_theme_dark_onSurface = Color(0xFFFFFFFF)
val md_theme_dark_surfaceVariant = NightSurfaceVariant
val md_theme_dark_onSurfaceVariant = Color(0xFF9499A3)
val md_theme_dark_outline = NightOutline
val md_theme_dark_outlineVariant = NightOutlineVariant
val md_theme_dark_inverseOnSurface = Color(0xFF16181C)
val md_theme_dark_inverseSurface = Color(0xFFE8EDF3)
val md_theme_dark_inversePrimary = ElectricBlue
