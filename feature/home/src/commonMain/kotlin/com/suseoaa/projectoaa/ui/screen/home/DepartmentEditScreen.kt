package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.home.AnnouncementViewModel
import com.suseoaa.projectoaa.ui.component.common.SharedTransitionPageContainer
import com.suseoaa.projectoaa.util.ToastManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * 公告编辑器：新建或修改一条公告。
 *
 * - 新建：可以「存草稿」或直接「发布」；
 * - 修改当前生效的公告：只有「保存」，改完立即生效；
 * - 修改草稿 / 历史公告：「保存」只改内容，「发布」会设为该部门当前公告。
 *
 * @param announcementId 为 null 时新建
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentEditScreen(
    departmentName: String,
    announcementId: Int?,
    onBack: () -> Unit,
    viewModel: AnnouncementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val containerScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var editorValue by remember(departmentName, announcementId) { mutableStateOf(TextFieldValue("")) }
    var hasAppliedInitialContent by remember(departmentName, announcementId) { mutableStateOf(false) }
    var editorHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(departmentName, announcementId) {
        viewModel.prepareEditor(departmentName, announcementId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            ToastManager.showToast(it)
            viewModel.clearMessage()
        }
    }

    // 编辑器就绪后把已有正文灌进输入框，光标放到末尾
    LaunchedEffect(uiState.isEditorReady) {
        if (uiState.isEditorReady && !hasAppliedInitialContent) {
            editorValue = TextFieldValue(
                text = uiState.editContent,
                selection = TextRange(uiState.editContent.length)
            )
            hasAppliedInitialContent = true
        }
    }

    LaunchedEffect(editorValue.selection, editorHasFocus) {
        if (editorHasFocus) {
            delay(40)
            bringIntoViewRequester.bringIntoView()
        }
    }

    val isEditingActive = uiState.editing?.isActive == true
    val canSubmit = !uiState.isSubmitting && uiState.isEditorReady &&
        uiState.editTitle.isNotBlank() && uiState.editContent.isNotBlank()

    SharedTransitionPageContainer(
        transitionKey = "department_edit_$departmentName"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
                Text(
                    text = if (announcementId == null) "新建$departmentName 公告" else "编辑公告",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                TextButton(enabled = canSubmit, onClick = { viewModel.save(publish = false) }) {
                    Text(
                        when {
                            isEditingActive -> "保存"
                            announcementId == null -> "存草稿"
                            else -> "保存"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isEditingActive) {
                    TextButton(enabled = canSubmit, onClick = { viewModel.save(publish = true) }) {
                        Text("发布", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                when {
                    uiState.error != null && !uiState.isEditorReady -> {
                        ErrorContent(
                            error = uiState.error ?: "加载失败",
                            onRetry = { viewModel.prepareEditor(departmentName, announcementId) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    !uiState.isEditorReady -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    else -> {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (uiState.isSubmitting) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(containerScrollState)
                                        .imePadding()
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.editTitle,
                                        onValueChange = viewModel::onTitleChange,
                                        label = { Text("标题") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    TextField(
                                        value = editorValue,
                                        onValueChange = { value ->
                                            editorValue = value
                                            viewModel.onContentChange(value.text)
                                            if (editorHasFocus) {
                                                scope.launch {
                                                    delay(30)
                                                    bringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 420.dp)
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .onFocusChanged { state -> editorHasFocus = state.isFocused },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        placeholder = {
                                            Text(
                                                "在此输入 Markdown 正文...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                        minLines = 18,
                                        maxLines = Int.MAX_VALUE
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
