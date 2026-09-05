package com.suseoaa.projectoaa.ui.screen.ailab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.ailab.AiLabViewModel
import com.suseoaa.projectoaa.presentation.ailab.ModelDownloadState
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Folder

// AI 实验室主页：设备能力、模型推荐与功能入口三段。

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLabScreen(
    onBack: () -> Unit,
    onNavigateToAcademicAnalysis: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    viewModel: AiLabViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val showTokenDialog by viewModel.showTokenDialog.collectAsState()
    var showModelManagerDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadLocalModels()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            ToastManager.showToast(msg)
            viewModel.clearError()
        }
    }
    
    if (showTokenDialog) {
        var inputUsername by remember { mutableStateOf("") }
        var inputKey by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissTokenDialog() },
            title = { Text("需要 Kaggle API 凭证", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "为了获取最高精度和原生的模型资源，我们已切换至 Kaggle 官方源。请在 Kaggle 个人设置中生成 API Key (kaggle.json)，并在此填入。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = { Text("Kaggle Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("Kaggle API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.submitKaggleAuthAndDownload(inputUsername.trim(), inputKey.trim()) }
                ) {
                    Text("保存并下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTokenDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("推理设置", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("选择底层计算引擎后端。GPU 提供更高性能，但在部分设备上可能存在兼容性问题。")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setPreferGpu(true)
                            com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.setPreferGpu(true)
                        }
                    ) {
                        RadioButton(
                            selected = uiState.preferGpu,
                            onClick = { 
                                viewModel.setPreferGpu(true)
                                com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.setPreferGpu(true)
                            }
                        )
                        Text("GPU 性能模式 (推荐)")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setPreferGpu(false)
                            com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.setPreferGpu(false)
                        }
                    ) {
                        RadioButton(
                            selected = !uiState.preferGpu,
                            onClick = { 
                                viewModel.setPreferGpu(false)
                                com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.setPreferGpu(false)
                            }
                        )
                        Text("CPU 兼容模式")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "AI 实验室",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "推理设置",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                viewModel.loadLocalModels()
                showModelManagerDialog = true
            }) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "模型管理",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 分区 1：设备能力检测 ────────────────────────────────────
            item {
                DeviceCapabilitySection(
                    isLoading = uiState.isLoadingDeviceInfo,
                    deviceInfo = uiState.deviceInfo
                )
            }

            // ── 分区 2：模型推荐 ──────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = !uiState.isLoadingDeviceInfo,
                    enter = fadeIn(tween(400)) + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    uiState.recommendation?.let { rec ->
                        ModelRecommendationSection(
                            recommendation = rec,
                            selectedModel = uiState.selectedModel,
                            availableModels = uiState.availableModels,
                            downloadState = uiState.downloadState,
                            hasUpdateAvailable = uiState.hasUpdateAvailable,
                            onSelectModel = { viewModel.selectModel(it) },
                            onDownload = { viewModel.startDownload() },
                            onCancelDownload = { viewModel.cancelDownload() }
                        )
                    }
                }
            }

            // ── 分区 3：AI 功能入口 ───────────────────────────────────
            item {
                AiFeaturesSection(
                    isModelAvailable = uiState.downloadState is ModelDownloadState.Downloaded,
                    onNavigateToAcademicAnalysis = onNavigateToAcademicAnalysis,
                    onNavigateToAiChat = onNavigateToAiChat
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showModelManagerDialog) {
        ModelManagerDialog(
            localModels = uiState.localModels,
            onDismiss = { showModelManagerDialog = false },
            onDelete = { fileName -> viewModel.deleteLocalModel(fileName) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 分区 1：设备能力检测面板
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
