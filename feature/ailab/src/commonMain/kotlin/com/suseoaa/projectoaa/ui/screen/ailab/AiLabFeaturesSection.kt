package com.suseoaa.projectoaa.ui.screen.ailab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// AI 功能入口列表。

@Composable
internal fun AiFeaturesSection(
    isModelAvailable: Boolean,
    onNavigateToAcademicAnalysis: () -> Unit,
    onNavigateToAiChat: () -> Unit
) {
    SectionLabel(text = "AI 功能")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AiFeatureRow(
                icon = Icons.Default.Summarize,
                title = "调课通知摘要",
                subtitle = if (isModelAvailable) "模型已就绪，将在拉取调课通知时自动摘要" else "下载模型后启用 · 长文本一键提炼关键信息",
                isEnabled = isModelAvailable,
                onClick = null // 纯展示，功能在通知页自动生效
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            AiFeatureRow(
                icon = Icons.Default.Analytics,
                title = "学业规划分析",
                subtitle = if (isModelAvailable) "分析毕业差距与目标绩点路径（含精确计算工具）" else "下载模型后启用 · 结合成绩与培养方案深度分析",
                isEnabled = isModelAvailable,
                onClick = {
                    if (isModelAvailable) {
                        onNavigateToAcademicAnalysis()
                    }
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            AiFeatureRow(
                icon = Icons.Default.AutoAwesome,
                title = "本地自由对话",
                subtitle = if (isModelAvailable) "开启思维链深思模式的纯离线自由对话" else "下载模型后启用真 AI 对话",
                isEnabled = isModelAvailable,
                onClick = {
                    if (isModelAvailable) {
                        onNavigateToAiChat()
                    }
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            AiFeatureRow(
                icon = Icons.Default.Search,
                title = "智能数据查询",
                subtitle = if (isModelAvailable) "用自然语言搜索所有历史数据" else "需下载模型 · 仅限高级模型",
                isEnabled = isModelAvailable,
                onClick = {
                    if (isModelAvailable) {
                        onNavigateToAiChat()
                    }
                }
            )
        }
    }

    if (!isModelAvailable) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "📝  下载并载入模型后，以上功能将自动解锁。模型仅在设备本地运行，数据绝不离开您的手机。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
internal fun AiFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onClick: (() -> Unit)?
) {
    val contentAlpha = if (isEnabled) 1f else 0.55f

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = (if (isEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = contentAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    lineHeight = 16.sp
                )
            }
            if (!isEnabled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "未启用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (onClick != null && isEnabled) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) { content() }
    } else {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 工具组件
// ─────────────────────────────────────────────────────────────────────────────
