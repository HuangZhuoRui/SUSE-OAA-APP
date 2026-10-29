package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.presentation.recruitment.ChoiceSlot
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentUiState
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationForm
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationOptions
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Choice
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.TermPhase
import com.suseoaa.projectoaa.ui.component.common.OptionDropdown
import com.suseoaa.projectoaa.util.pickImageForAvatar

// 「我的申请」Tab：周期信息、录取结果、申请表。

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApplicationTab(
    uiState: RecruitmentUiState,
    onRefresh: () -> Unit,
    onSelectTerm: (Term) -> Unit,
    onUpdateForm: ((ApplicationForm) -> ApplicationForm) -> Unit,
    onUploadAvatar: (ByteArray) -> Unit,
    onSelectDepartment: (ChoiceSlot, Department) -> Unit,
    onSelectRole: (ChoiceSlot, Role) -> Unit,
    onClearChoice: (ChoiceSlot) -> Unit,
    onSubmit: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.terms.isEmpty()) {
        Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading && uiState.terms.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading || uiState.isSubmitting || uiState.isUploadingAvatar) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            val term = uiState.selectedTerm
            if (term == null) {
                EmptyHint(text = "暂无招新或换届安排", onRefresh = onRefresh)
            } else {
                TermContent(
                    term = term,
                    uiState = uiState,
                    onSelectTerm = onSelectTerm,
                    onUpdateForm = onUpdateForm,
                    onUploadAvatar = onUploadAvatar,
                    onSelectDepartment = onSelectDepartment,
                    onSelectRole = onSelectRole,
                    onClearChoice = onClearChoice,
                    onSubmit = onSubmit,
                    onWithdraw = onWithdraw
                )
            }
        }
    }
}

@Composable
private fun TermContent(
    term: Term,
    uiState: RecruitmentUiState,
    onSelectTerm: (Term) -> Unit,
    onUpdateForm: ((ApplicationForm) -> ApplicationForm) -> Unit,
    onUploadAvatar: (ByteArray) -> Unit,
    onSelectDepartment: (ChoiceSlot, Department) -> Unit,
    onSelectRole: (ChoiceSlot, Role) -> Unit,
    onClearChoice: (ChoiceSlot) -> Unit,
    onSubmit: () -> Unit,
    onWithdraw: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TermHeaderCard(
            term = term,
            phase = uiState.phase,
            terms = uiState.terms,
            onSelectTerm = onSelectTerm
        )

        val application = uiState.myApplication
        if (application != null && (application.decision.isNotBlank() || uiState.phase == TermPhase.Querying)) {
            ResultCard(
                decision = application.decision,
                decisionRemark = application.decisionRemark,
                result = application.result,
                uiState = uiState
            )
        }

        when {
            uiState.canEdit -> ApplicationFormCard(
                uiState = uiState,
                isEditable = true,
                onUpdateForm = onUpdateForm,
                onUploadAvatar = onUploadAvatar,
                onSelectDepartment = onSelectDepartment,
                onSelectRole = onSelectRole,
                onClearChoice = onClearChoice,
                onSubmit = onSubmit,
                onWithdraw = onWithdraw
            )
            application != null -> ApplicationFormCard(
                uiState = uiState,
                isEditable = false,
                onUpdateForm = {},
                onUploadAvatar = {},
                onSelectDepartment = { _, _ -> },
                onSelectRole = { _, _ -> },
                onClearChoice = {},
                onSubmit = {},
                onWithdraw = {}
            )
            else -> Text(
                text = if (uiState.phase == TermPhase.NotStarted) "填写期尚未开始，请留意开始时间" else "你没有参加本轮${term.type}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
internal fun EmptyHint(text: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onRefresh) { Text("刷新") }
    }
}

internal fun TermPhase?.label(): String = when (this) {
    TermPhase.NotStarted -> "尚未开始"
    TermPhase.Editing -> "填写中"
    TermPhase.Reviewing -> "面试中"
    TermPhase.Querying -> "结果公示中"
    TermPhase.Ended -> "已结束"
    null -> "未知"
}

internal fun Term.displayTitle(): String = title.ifBlank { "$year 年$type" }

@Composable
private fun TermHeaderCard(
    term: Term,
    phase: TermPhase?,
    terms: List<Term>,
    onSelectTerm: (Term) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${term.displayTitle()} · ${phase.label()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "填写时间：${term.editPeriod.startAt.take(10)} 至 ${term.editPeriod.endAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "结果公示：${term.queryPeriod.startAt.take(10)} 至 ${term.queryPeriod.endAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (terms.size > 1) {
                OptionDropdown(
                    label = "切换周期",
                    options = terms,
                    selected = term,
                    onSelect = onSelectTerm,
                    optionLabel = { it.displayTitle() }
                )
            }
        }
    }
}

@Composable
private fun ResultCard(decision: String, decisionRemark: String, result: Choice?, uiState: RecruitmentUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("录取结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(decision.ifBlank { "暂无结论" }, style = MaterialTheme.typography.bodyLarge)
            if (result != null && result.isComplete) {
                Text(
                    "${uiState.departmentName(result.departmentId)} · ${uiState.roleName(result.roleId)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            if (decisionRemark.isNotBlank()) {
                Text(
                    "备注：$decisionRemark",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ApplicationFormCard(
    uiState: RecruitmentUiState,
    isEditable: Boolean,
    onUpdateForm: ((ApplicationForm) -> ApplicationForm) -> Unit,
    onUploadAvatar: (ByteArray) -> Unit,
    onSelectDepartment: (ChoiceSlot, Department) -> Unit,
    onSelectRole: (ChoiceSlot, Role) -> Unit,
    onClearChoice: (ChoiceSlot) -> Unit,
    onSubmit: () -> Unit,
    onWithdraw: () -> Unit
) {
    val form = uiState.form
    val hasExisting = uiState.myApplication != null
    var confirmWithdraw by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }

    if (showPhotoPicker) {
        pickImageForAvatar { imageData ->
            if (imageData != null) {
                onUploadAvatar(imageData)
            }
            showPhotoPicker = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = when {
                    !isEditable -> "我的申请表"
                    hasExisting -> "修改我的申请表"
                    else -> "填写申请表"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            ApplicationPhotoSection(
                uiState = uiState,
                isEditable = isEditable,
                onPickPhoto = { showPhotoPicker = true }
            )

            FormField("学院", form.college, isEditable) { v -> onUpdateForm { it.copy(college = v) } }
            FormField("专业班级（如 计科241）", form.majorClass, isEditable) { v -> onUpdateForm { it.copy(majorClass = v) } }
            OptionDropdown(
                label = "性别",
                options = ApplicationOptions.genders,
                selected = form.gender.ifBlank { null },
                onSelect = { v -> onUpdateForm { it.copy(gender = v) } },
                optionLabel = { it },
                enabled = isEditable
            )
            FormField("手机号", form.phone, isEditable, KeyboardType.Phone) { v -> onUpdateForm { it.copy(phone = v) } }
            FormField("QQ", form.qq, isEditable, KeyboardType.Number) { v -> onUpdateForm { it.copy(qq = v) } }
            OptionDropdown(
                label = "政治面貌",
                options = ApplicationOptions.politicalStatuses,
                selected = form.politicalStatus.ifBlank { null },
                onSelect = { v -> onUpdateForm { it.copy(politicalStatus = v) } },
                optionLabel = { it },
                enabled = isEditable
            )
            FormField("出生年月（如 2005-09）", form.birthDate, isEditable) { v -> onUpdateForm { it.copy(birthDate = v) } }

            ChoiceSelector(
                title = "第一志愿",
                choice = form.firstChoice,
                departments = uiState.departmentsFor(ChoiceSlot.First),
                roles = uiState.rolesFor(ChoiceSlot.First),
                uiState = uiState,
                isEditable = isEditable,
                onSelectDepartment = { onSelectDepartment(ChoiceSlot.First, it) },
                onSelectRole = { onSelectRole(ChoiceSlot.First, it) },
                onClear = { onClearChoice(ChoiceSlot.First) }
            )
            ChoiceSelector(
                title = "第二志愿",
                choice = form.secondChoice,
                departments = uiState.departmentsFor(ChoiceSlot.Second),
                roles = uiState.rolesFor(ChoiceSlot.Second),
                uiState = uiState,
                isEditable = isEditable,
                onSelectDepartment = { onSelectDepartment(ChoiceSlot.Second, it) },
                onSelectRole = { onSelectRole(ChoiceSlot.Second, it) },
                onClear = { onClearChoice(ChoiceSlot.Second) }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("是否服从调剂", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (form.allowAdjust) "服从调剂" else "不服从调剂",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = form.allowAdjust,
                    onCheckedChange = { checked -> onUpdateForm { it.copy(allowAdjust = checked) } },
                    enabled = isEditable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            FormField("个人经历（在校经历、项目经历等）", form.resume, isEditable, minLines = 4) { v ->
                onUpdateForm { it.copy(resume = v) }
            }
            FormField("申请理由", form.reason, isEditable, minLines = 4) { v -> onUpdateForm { it.copy(reason = v) } }

            if (isEditable) {
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting && !uiState.isUploadingAvatar,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (hasExisting) "保存修改" else "提交申请", style = MaterialTheme.typography.titleMedium)
                }
                if (hasExisting) {
                    TextButton(
                        onClick = { confirmWithdraw = true },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("撤回申请", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmWithdraw) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { confirmWithdraw = false },
            title = { Text("撤回申请") },
            text = { Text("撤回后申请表将被删除，填写期内可以重新提交。确定撤回吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmWithdraw = false
                    onWithdraw()
                }) { Text("撤回", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmWithdraw = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun ApplicationPhotoSection(
    uiState: RecruitmentUiState,
    isEditable: Boolean,
    onPickPhoto: () -> Unit
) {
    val form = uiState.form
    val previewModel: Any? = uiState.localAvatarBytes ?: form.avatarUrl.ifBlank { null }
    val hasPhoto = form.avatarUri.isNotBlank() || previewModel != null
    val canPick = isEditable && !uiState.isUploadingAvatar && !uiState.isSubmitting
    val frameShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 92.dp)
                .clip(frameShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                .border(
                    width = 1.dp,
                    color = if (hasPhoto) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    },
                    shape = frameShape
                )
                .then(if (canPick) Modifier.clickable(onClick = onPickPhoto) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (previewModel != null) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = "申请表个人照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isEditable) Icons.Default.AddAPhoto else Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (hasPhoto) "已上传" else if (isEditable) "必填" else "无照片",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (uiState.isUploadingAvatar) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            uiState.currentUser?.person?.let { person ->
                val gradeText = person.resolvedGrade?.let { " · ${it}级" }.orEmpty()
                Text(
                    text = "${person.name}（${person.studentId}$gradeText）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = when {
                    uiState.isUploadingAvatar -> "正在上传个人照片..."
                    !isEditable && hasPhoto -> "已提交申请表个人照片"
                    !isEditable -> "未上传个人照片"
                    hasPhoto -> "个人照片已上传，点击可重新选择"
                    else -> "请上传个人近期照片（必填，支持 jpg / png / webp，不超过 4MB）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isEditable) {
                OutlinedButton(
                    onClick = onPickPhoto,
                    enabled = canPick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = if (hasPhoto) "更换照片" else "上传照片",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceSelector(
    title: String,
    choice: Choice,
    departments: List<Department>,
    roles: List<Role>,
    uiState: RecruitmentUiState,
    isEditable: Boolean,
    onSelectDepartment: (Department) -> Unit,
    onSelectRole: (Role) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (isEditable && (choice.departmentId > 0 || choice.roleId > 0)) {
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("重置", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionDropdown(
                label = "部门",
                options = departments,
                selected = (departments + uiState.departments).firstOrNull { it.id == choice.departmentId },
                onSelect = onSelectDepartment,
                optionLabel = { it.name },
                placeholder = "请选择",
                enabled = isEditable,
                modifier = Modifier.weight(1f)
            )
            OptionDropdown(
                label = "职位",
                options = roles,
                selected = (roles + uiState.roles).firstOrNull { it.id == choice.roleId },
                onSelect = onSelectRole,
                optionLabel = { it.name },
                placeholder = "请选择",
                enabled = isEditable,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun FormField(
    label: String,
    value: String,
    isEditable: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = !isEditable,
        singleLine = minLines == 1,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    )
}
