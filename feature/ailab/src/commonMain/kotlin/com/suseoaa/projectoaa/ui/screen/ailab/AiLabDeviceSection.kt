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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.util.DeviceInfo
import com.suseoaa.projectoaa.util.toReadableStorage
import com.suseoaa.projectoaa.util.format
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items

// 设备端侧推理能力检测与硬件参数展示。

@Composable
internal fun DeviceCapabilitySection(
    isLoading: Boolean,
    deviceInfo: DeviceInfo?
) {
    SectionLabel(text = "设备能力检测")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "正在检测设备能力…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (deviceInfo != null) {
            val totalRamGb = deviceInfo.totalRam / (1024f * 1024f * 1024f)
            val availRamGb = deviceInfo.availableRam / (1024f * 1024f * 1024f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // RAM 使用率进度条
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "内存 (RAM)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "可用 ${availRamGb.format(1)}GB / 共 ${totalRamGb.format(1)}GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val usedFraction = if (totalRamGb > 0) (1f - availRamGb / totalRamGb).coerceIn(0f, 1f) else 0f
                    val ramBarColor = when {
                        usedFraction > 0.85f -> Color(0xFFFF3B30)
                        usedFraction > 0.65f -> Color(0xFFFF9500)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = ramBarColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // 参数网格
                val items = buildHardwareItems(deviceInfo)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { item ->
                                HardwareParamChip(
                                    label = item.first,
                                    value = item.second,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 如果这行只有 1 个，补一个空 weight
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "无法读取设备信息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

internal fun buildHardwareItems(info: DeviceInfo): List<Pair<String, String>> {
    val totalRamGb = info.totalRam / (1024f * 1024f * 1024f)
    return buildList {
        add("CPU 型号" to info.cpuModel.take(28))
        if (info.socModel.isNotBlank()) add("SoC 型号" to info.socModel.take(28))
        add("GPU 渲染器" to info.gpuRenderer.take(28))
        add("NPU 支持" to if (info.hasNpu) info.npuDescription.take(24) else "未检测到")
        add("总内存" to "${totalRamGb.format(1)} GB")
        add("总存储" to info.totalStorage.toReadableStorage())
        add("可用存储" to info.availableStorage.toReadableStorage())
        add("系统版本" to info.osVersion)
    }
}

@Composable
internal fun HardwareParamChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 分区 2：模型推荐与下载
// ─────────────────────────────────────────────────────────────────────────────
