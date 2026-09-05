package com.suseoaa.projectoaa.ui.screen.ailab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.ailab.ModelDownloadState
import com.suseoaa.projectoaa.util.ModelRecommendation
import com.suseoaa.projectoaa.util.ModelRecommendationLevel
import com.suseoaa.projectoaa.util.toReadableStorage
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.window.DialogProperties
import com.suseoaa.projectoaa.util.LocalModelFile

// 端侧模型的推荐、下载与管理。

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelRecommendationSection(
    recommendation: ModelRecommendation,
    selectedModel: com.suseoaa.projectoaa.util.AiModelMetadata?,
    availableModels: List<com.suseoaa.projectoaa.util.AiModelMetadata>,
    downloadState: ModelDownloadState,
    hasUpdateAvailable: Boolean,
    onSelectModel: (String) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    SectionLabel(text = "推荐模型")

    val isSupported = recommendation.level != ModelRecommendationLevel.NOT_RECOMMENDED

    val gradientColors = when (recommendation.level) {
        ModelRecommendationLevel.E4B_RECOMMENDED -> listOf(
            Color(0xFF1A237E), Color(0xFF283593)
        )
        ModelRecommendationLevel.E2B_RECOMMENDED -> listOf(
            Color(0xFF00695C), Color(0xFF00796B)
        )
        ModelRecommendationLevel.NOT_RECOMMENDED -> listOf(
            Color(0xFF37474F), Color(0xFF455A64)
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // 标题行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.menuAnchor()
                            ) {
                                Text(
                                    text = selectedModel?.name ?: recommendation.modelName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Model",
                                    tint = Color.White
                                )
                            }
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.name) },
                                        onClick = { 
                                            onSelectModel(model.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = selectedModel?.sizeDesc ?: recommendation.modelSizeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // 适配徽章
                    val (badgeColor, badgeText) = when (recommendation.level) {
                        ModelRecommendationLevel.E4B_RECOMMENDED -> Color(0xFF69F0AE) to "完整体验"
                        ModelRecommendationLevel.E2B_RECOMMENDED -> Color(0xFF40C4FF) to "推荐"
                        ModelRecommendationLevel.NOT_RECOMMENDED -> Color(0xFFFF5252) to "不支持"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 推荐原因
                // 推荐原因或警告
                val reasonText = if (selectedModel != null && selectedModel.recommendedLevel != recommendation.level) {
                    "⚠️ 警告：您选择的模型非系统推荐模型，强行运行可能导致内存溢出或闪退。"
                } else {
                    recommendation.reason
                }
                
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedModel != null && selectedModel.recommendedLevel != recommendation.level) Color(0xFFFFD740) else Color.White.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )

                // 下载状态
                if (isSupported) {
                    Spacer(modifier = Modifier.height(16.dp))
                    when (downloadState) {
                        is ModelDownloadState.Idle -> {
                            Surface(
                                onClick = onDownload,
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "开始下载（${selectedModel?.sizeDesc ?: recommendation.modelSizeDesc}）",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        is ModelDownloadState.NotOnWifi -> {
                            Text(
                                text = "⚠️ 请连接 Wi-Fi 后再下载，避免消耗流量",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD740)
                            )
                        }
                        is ModelDownloadState.Downloading -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "正在下载… ${downloadState.speedStr}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${(downloadState.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { downloadState.progress },
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.25f),
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${downloadState.downloadedBytes.toReadableStorage()} / ${downloadState.totalBytes.toReadableStorage()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                    Surface(
                                        onClick = onCancelDownload,
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFF5252).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "取消下载",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFF5252)
                                        )
                                    }
                                }
                            }
                        }
                        is ModelDownloadState.Downloaded -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF69F0AE))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "模型已就绪，AI 功能已启用",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF69F0AE)
                                    )
                                }
                                
                                if (hasUpdateAvailable) {
                                    Surface(
                                        onClick = onDownload,
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2196F3).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "官方有更新 (重新下载)",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64B5F6)
                                        )
                                    }
                                }
                            }
                        }
                        is ModelDownloadState.Error -> {
                            Text(
                                text = "下载失败：${downloadState.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF5252)
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 分区 3：AI 功能入口列表
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ModelManagerDialog(
    localModels: List<LocalModelFile>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "本地模型管理",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (localModels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无下载的本地模型文件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(localModels) { file ->
                            val sizeGb = file.sizeBytes.toDouble() / (1024 * 1024 * 1024)
                            val sizeStr = ((sizeGb * 100).toInt() / 100.0).toString() + " GB"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = sizeStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(file.name) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}
