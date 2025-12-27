package com.suseoaa.projectoaa.feature.course

import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// === 1. 颜色配置 (莫兰迪色系 - 更加现代柔和) ===
val CourseColors = listOf(
    Color(0xFF5C6BC0), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFF26A69A),
    Color(0xFFFFCA28), Color(0xFF9CCC65), Color(0xFF7E57C2), Color(0xFF29B6F6),
    Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFF78909C), Color(0xFF8D6E63)
)

// === 2. 时间轴配置 ===
enum class SlotType { CLASS, BREAK_SMALL, BREAK_LUNCH, BREAK_DINNER }

data class TimeSlot(
    val name: String,      // 显示的名字 "1", "午休"
    val startTime: String, // "08:30"
    val endTime: String,   // "09:15"
    val type: SlotType,
    val weight: Float      // 高度权重 (1.0 = 标准课高度, 0.5 = 半高)
)

// 2025年新作息配置
val DailySchedule2025 = listOf(
    TimeSlot("1", "08:30", "09:15", SlotType.CLASS, 1.2f),
    TimeSlot("2", "09:20", "10:05", SlotType.CLASS, 1.2f),
    TimeSlot("", "", "", SlotType.BREAK_SMALL, 0.2f), // 小课间
    TimeSlot("3", "10:25", "11:10", SlotType.CLASS, 1.2f),
    TimeSlot("4", "11:15", "12:00", SlotType.CLASS, 1.2f),
    TimeSlot("午休", "12:00", "14:00", SlotType.BREAK_LUNCH, 1.0f), // 午休留大一点
    TimeSlot("5", "14:00", "14:45", SlotType.CLASS, 1.2f),
    TimeSlot("6", "14:50", "15:35", SlotType.CLASS, 1.2f),
    TimeSlot("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlot("7", "15:55", "16:40", SlotType.CLASS, 1.2f),
    TimeSlot("8", "16:45", "17:30", SlotType.CLASS, 1.2f),
    TimeSlot("晚饭", "17:30", "19:00", SlotType.BREAK_DINNER, 0.8f),
    TimeSlot("9", "19:00", "19:45", SlotType.CLASS, 1.2f),
    TimeSlot("10", "19:50", "20:35", SlotType.CLASS, 1.2f),
    TimeSlot("11", "20:40", "21:25", SlotType.CLASS, 1.2f)
)

// UI 尺寸常量
val TimeAxisWidth = 40.dp
val HeaderHeight = 40.dp
val BaseUnitHeight = 50.dp // 基础单位高度 (weight=1.0 时的高度)