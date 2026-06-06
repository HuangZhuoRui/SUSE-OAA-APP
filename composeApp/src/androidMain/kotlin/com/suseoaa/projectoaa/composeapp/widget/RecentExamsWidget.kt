@file:SuppressLint("RestrictedApi")
package com.suseoaa.projectoaa.composeapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.appwidget.cornerRadius
import kotlin.math.abs
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import com.suseoaa.projectoaa.shared.data.repository.ExamCacheEntity

class RecentExamsWidget : GlanceAppWidget() {

    enum class ExamTheme(val bgHex: Long, val textHex: Long, val titleHex: Long) {
        RED(0xFFFEE2E2, 0xFFDC2626, 0xFF7F1D1D),
        BLUE(0xFFE0F2FE, 0xFF0284C7, 0xFF0C4A6E),
        GREEN(0xFFDCFCE7, 0xFF16A34A, 0xFF14532D),
        ORANGE(0xFFFFEDD5, 0xFFEA580C, 0xFF7C2D12),
        PURPLE(0xFFF3E8FF, 0xFF9333EA, 0xFF581C87)
    }

    private fun getExamTheme(name: String): ExamTheme {
        val themes = ExamTheme.entries.toTypedArray()
        val index = abs(name.hashCode()) % themes.size
        return themes[index]
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var errorMsg: String? = null
        var recentExams: List<ExamCacheEntity>? = null
        try {
            recentExams = WidgetDataFetcher.getUpcomingExams()
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = e.stackTraceToString()
        }

        provideContent {
            val bgSurface = DayNightColorProvider(day = Color.White, night = Color(0xFF1F2937))
            val textPrimary = DayNightColorProvider(day = Color.Black, night = Color.White)
            val textSecondary = DayNightColorProvider(day = Color.DarkGray, night = Color.LightGray)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("app://suseoaa/exams")).apply {
                setPackage(context.packageName)
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgSurface)
                    .cornerRadius(12.dp)
                    .clickable(actionStartActivity(intent))
                    .padding(12.dp)
            ) {
                if (errorMsg != null) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "加载失败: ${errorMsg.take(50)}",
                            style = TextStyle(color = ColorProvider(Color.Red), fontSize = 10.sp)
                        )
                    }
                } else if (recentExams.isNullOrEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "近期无考试，好好休息！",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Text(
                            text = "近期考试",
                            style = TextStyle(
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.padding(bottom = 8.dp)
                        )
                        
                        recentExams.forEachIndexed { index, exam ->
                            val theme = getExamTheme(exam.courseName)
                            
                            @SuppressLint("RestrictedApi")
                            val badgeBg = DayNightColorProvider(day = Color(theme.bgHex), night = Color(theme.bgHex).copy(alpha = 0.2f))
                            
                            @SuppressLint("RestrictedApi")
                            val badgeTitle = DayNightColorProvider(day = Color(theme.titleHex), night = Color(theme.textHex))

                            Row(
                                modifier = GlanceModifier.fillMaxWidth().padding(bottom = if (index < recentExams.size - 1) 8.dp else 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Badge
                                Column(
                                    modifier = GlanceModifier
                                        .background(badgeBg)
                                        .cornerRadius(8.dp)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .width(48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "考试",
                                        style = TextStyle(
                                            color = badgeTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = GlanceModifier.width(12.dp))

                                // Right: Details
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = exam.courseName,
                                        style = TextStyle(
                                            color = textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                    Text(
                                        text = "${exam.time} | ${exam.location}",
                                        style = TextStyle(
                                            color = textSecondary,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }
}
