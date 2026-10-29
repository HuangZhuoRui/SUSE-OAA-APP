package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentReviewUiState
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Application
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewDecision
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.ui.component.common.OptionDropdown

// 「面试审核」Tab：按周期、部门查看申请，录入面试结论。

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewTab(
    uiState: RecruitmentReviewUiState,
    onSelectTerm: (Term) -> Unit,
    onSelectDepartment: (Department?) -> Unit,
    onRefresh: () -> Unit,
    onSaveDecision: (Application, InterviewDecision) -> Unit,
    modifier: Modifier = Modifier
) {
    var deciding by remember { mutableStateOf<Application?>(null) }
    var previewingPhotoApp by remember { mutableStateOf<Application?>(null) }
    // 文档没保证列表里一定有申请 ID，展开状态按位置记
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading || uiState.isSaving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OptionDropdown(
                label = "周期",
                options = uiState.terms,
                selected = uiState.selectedTerm,
                onSelect = onSelectTerm,
                optionLabel = { it.displayTitle() },
                modifier = Modifier.weight(1f)
            )
            // null 这一项表示「我所在的部门」，由后端决定
            OptionDropdown(
                label = "部门",
                options = listOf<Department?>(null) + uiState.departments,
                selected = uiState.departmentFilter,
                onSelect = onSelectDepartment,
                optionLabel = { it?.name ?: "我所在的部门" },
                placeholder = "我所在的部门",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }

        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.applications.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!uiState.isLoading && uiState.applications.isEmpty()) {
                EmptyHint(
                    text = if (uiState.selectedTerm == null) "暂无招新或换届周期" else "当前条件下没有申请",
                    onRefresh = onRefresh
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.applications) { index, application ->
                        val expanded = expandedIndex == index
                        ApplicationReviewCard(
                            application = application,
                            uiState = uiState,
                            expanded = expanded,
                            onToggle = { expandedIndex = if (expanded) null else index },
                            onPreviewPhoto = {
                                if (application.avatar.url.isNotBlank()) {
                                    previewingPhotoApp = application
                                }
                            },
                            onDecide = { deciding = application }
                        )
                    }
                }
            }
        }
    }

    previewingPhotoApp?.let { app ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { previewingPhotoApp = null },
            title = { Text("${app.applicantTitle()} · 个人照片") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = app.avatar.url,
                        contentDescription = "申请人个人照片",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 380.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewingPhotoApp = null }) {
                    Text("关闭")
                }
            }
        )
    }

    deciding?.let { application ->
        DecisionDialog(
            application = application,
            uiState = uiState,
            onDismiss = { deciding = null },
            onConfirm = { decision ->
                onSaveDecision(application, decision)
                deciding = null
            }
        )
    }
}

@Composable
private fun ApplicationReviewCard(
    application: Application,
    uiState: RecruitmentReviewUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPreviewPhoto: () -> Unit,
    onDecide: () -> Unit
) {
    val result = uiState.results[application.resolvedId]
    val decision = result?.decision?.ifBlank { null } ?: application.decision.ifBlank { null }
    val avatarUrl = application.avatar.url
    val photoShape = RoundedCornerShape(10.dp)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable(onClick = onToggle)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 60.dp)
                        .clip(photoShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, photoShape)
                        .then(if (avatarUrl.isNotBlank()) Modifier.clickable(onClick = onPreviewPhoto) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "${application.applicantTitle()}照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = application.applicantTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "一志愿：${uiState.departmentName(application.firstChoice.departmentId)} · " +
                            uiState.roleName(application.firstChoice.roleId),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "二志愿：${uiState.departmentName(application.secondChoice.departmentId)} · " +
                            uiState.roleName(application.secondChoice.roleId),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                val nonAdmission = decision != null && isNonAdmissionDecision(decision)
                val chipContainerColor = when {
                    decision == null -> MaterialTheme.colorScheme.surfaceVariant
                    nonAdmission -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val chipLabelColor = when {
                    decision == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    nonAdmission -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
                AssistChip(
                    onClick = onDecide,
                    label = { Text(decision ?: "待录入") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = chipContainerColor,
                        labelColor = chipLabelColor
                    ),
                    border = null
                )
            }
            Text(
                if (application.allowAdjust) "服从调剂" else "不服从调剂",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (result != null && result.isComplete()) {
                Text(
                    "结论去向：${uiState.departmentName(result.resultDepartmentId)} · ${uiState.roleName(result.resultRoleId)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                DetailLine("学院", application.college)
                DetailLine("专业班级", application.majorClass)
                DetailLine("性别", application.gender)
                DetailLine("手机号", application.phone)
                DetailLine("QQ", application.qq)
                DetailLine("政治面貌", application.politicalStatus)
                DetailLine("出生年月", application.birthDate)
                DetailLine("个人经历", application.resume)
                DetailLine("申请理由", application.reason)
                val note = result?.remark?.ifBlank { null } ?: application.decisionRemark.ifBlank { null }
                if (!note.isNullOrBlank()) DetailLine("面试备注", note)
                if (!result?.operatorName.isNullOrBlank()) {
                    val timeSuffix = result.updatedAt.take(16).replace('T', ' ').takeIf { it.isNotBlank() }
                        ?.let { " · $it" }.orEmpty()
                    DetailLine("录入信息", "${result.operatorName}$timeSuffix")
                }
                Button(onClick = onDecide, modifier = Modifier.align(Alignment.End)) {
                    Text(if (decision == null) "录入结论" else "修改结论")
                }
            }
        }
    }
}

private fun com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewResult.isComplete() =
    resultDepartmentId > 0 && resultRoleId > 0

private fun Application.applicantTitle(): String {
    val who = name.ifBlank { username }.ifBlank { "申请 #$resolvedId" }
    return if (studentId.isBlank()) who else "$who（$studentId）"
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun isNonAdmissionDecision(decision: String): Boolean {
    if (decision.isBlank()) return false
    return listOf("未通过", "不通过", "淘汰", "拒绝", "不录取", "待定").any { it in decision }
}

@Composable
private fun DecisionDialog(
    application: Application,
    uiState: RecruitmentReviewUiState,
    onDismiss: () -> Unit,
    onConfirm: (InterviewDecision) -> Unit
) {
    val existing = uiState.results[application.resolvedId]
    var decision by remember { mutableStateOf(existing?.decision.orEmpty()) }
    // 默认去向为第一志愿；若已有结论为未通过/待定则保持 0
    var departmentId by remember {
        mutableStateOf(
            when {
                existing != null && isNonAdmissionDecision(existing.decision) -> 0
                existing != null && existing.resultDepartmentId > 0 -> existing.resultDepartmentId
                else -> application.firstChoice.departmentId
            }
        )
    }
    var roleId by remember {
        mutableStateOf(
            when {
                existing != null && isNonAdmissionDecision(existing.decision) -> 0
                existing != null && existing.resultRoleId > 0 -> existing.resultRoleId
                else -> application.firstChoice.roleId
            }
        )
    }
    var remark by remember {
        mutableStateOf(existing?.remark?.ifBlank { null } ?: application.decisionRemark)
    }
    val nonAdmission = isNonAdmissionDecision(decision)

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("面试结论") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(application.applicantTitle(), style = MaterialTheme.typography.bodyMedium)
                if (uiState.decisions.isNotEmpty()) {
                    OptionDropdown(
                        label = "结论",
                        options = uiState.decisions,
                        selected = decision.ifBlank { null },
                        onSelect = {
                            decision = it
                            if (isNonAdmissionDecision(it)) {
                                departmentId = 0
                                roleId = 0
                            } else {
                                // 后端的标准结论是「录取第一志愿」「录取第二志愿」这类，选中时顺带把去向切过去
                                val choice = when {
                                    "第一志愿" in it -> application.firstChoice
                                    "第二志愿" in it -> application.secondChoice
                                    departmentId <= 0 || roleId <= 0 -> application.firstChoice
                                    else -> null
                                }
                                if (choice != null) {
                                    departmentId = choice.departmentId
                                    roleId = choice.roleId
                                }
                            }
                        },
                        optionLabel = { it }
                    )
                } else {
                    // 取不到标准取值时退回手动输入，后端会校验
                    OutlinedTextField(
                        value = decision,
                        onValueChange = { decision = it },
                        label = { Text("结论") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!nonAdmission) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                departmentId = application.firstChoice.departmentId
                                roleId = application.firstChoice.roleId
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("第一志愿") }
                        OutlinedButton(
                            onClick = {
                                departmentId = application.secondChoice.departmentId
                                roleId = application.secondChoice.roleId
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("第二志愿") }
                    }
                    OptionDropdown(
                        label = "录取部门",
                        options = uiState.departments,
                        selected = uiState.departments.firstOrNull { it.id == departmentId },
                        onSelect = { departmentId = it.id },
                        optionLabel = { it.name }
                    )
                    OptionDropdown(
                        label = "录取职位",
                        options = uiState.roles.filter { it.isActive },
                        selected = uiState.roles.firstOrNull { it.id == roleId },
                        onSelect = { roleId = it.id },
                        optionLabel = { it.name }
                    )
                }
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = decision.isNotBlank() && (nonAdmission || (departmentId > 0 && roleId > 0)),
                onClick = {
                    onConfirm(
                        InterviewDecision(
                            decision = decision.trim(),
                            resultDepartmentId = if (nonAdmission) 0 else departmentId,
                            resultRoleId = if (nonAdmission) 0 else roleId,
                            remark = remark.trim()
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
