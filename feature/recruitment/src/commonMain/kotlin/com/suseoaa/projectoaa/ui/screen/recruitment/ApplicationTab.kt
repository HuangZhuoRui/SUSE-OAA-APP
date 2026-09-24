package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

// 「我的申请」Tab：周期信息、录取结果、申请表。

@Composable
internal fun ApplicationTab(
    uiState: RecruitmentUiState,
    onRefresh: () -> Unit,
    onSelectTerm: (Term) -> Unit,
    onUpdateForm: ((ApplicationForm) -> ApplicationForm) -> Unit,
    onSelectDepartment: (ChoiceSlot, Department) -> Unit,
    onSelectRole: (ChoiceSlot, Role) -> Unit,
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

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.isLoading || uiState.isSubmitting) {
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
                onSelectDepartment = onSelectDepartment,
                onSelectRole = onSelectRole,
                onSubmit = onSubmit,
                onWithdraw = onWithdraw
            )
        }
    }
}

@Composable
private fun TermContent(
    term: Term,
    uiState: RecruitmentUiState,
    onSelectTerm: (Term) -> Unit,
    onUpdateForm: ((ApplicationForm) -> ApplicationForm) -> Unit,
    onSelectDepartment: (ChoiceSlot, Department) -> Unit,
    onSelectRole: (ChoiceSlot, Role) -> Unit,
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
                result = application.result,
                uiState = uiState
            )
        }

        when {
            uiState.canEdit -> ApplicationFormCard(
                uiState = uiState,
                isEditable = true,
                onUpdateForm = onUpdateForm,
                onSelectDepartment = onSelectDepartment,
                onSelectRole = onSelectRole,
                onSubmit = onSubmit,
                onWithdraw = onWithdraw
            )
            application != null -> ApplicationFormCard(
                uiState = uiState,
                isEditable = false,
                onUpdateForm = {},
                onSelectDepartment = { _, _ -> },
                onSelectRole = { _, _ -> },
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
                Icon(Icons.Default.Info, contentDescription = null)
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
private fun ResultCard(decision: String, result: Choice?, uiState: RecruitmentUiState) {
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
        }
    }
}

@Composable
private fun ApplicationFormCard(
    uiState: RecruitmentUiState,
    isEditable: Boolean,
    onUpdateForm: ((ApplicationForm) -> ApplicationForm) -> Unit,
    onSelectDepartment: (ChoiceSlot, Department) -> Unit,
    onSelectRole: (ChoiceSlot, Role) -> Unit,
    onSubmit: () -> Unit,
    onWithdraw: () -> Unit
) {
    val form = uiState.form
    val hasExisting = uiState.myApplication != null
    val departments = uiState.departments.filter { it.isActive }
    var confirmWithdraw by remember { mutableStateOf(false) }

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
            uiState.currentUser?.person?.let { person ->
                Text(
                    "${person.name}（${person.studentId}）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                departments = departments,
                roles = uiState.firstChoiceRoles,
                uiState = uiState,
                isEditable = isEditable,
                onSelectDepartment = { onSelectDepartment(ChoiceSlot.First, it) },
                onSelectRole = { onSelectRole(ChoiceSlot.First, it) }
            )
            ChoiceSelector(
                title = "第二志愿",
                choice = form.secondChoice,
                departments = departments,
                roles = uiState.secondChoiceRoles,
                uiState = uiState,
                isEditable = isEditable,
                onSelectDepartment = { onSelectDepartment(ChoiceSlot.Second, it) },
                onSelectRole = { onSelectRole(ChoiceSlot.Second, it) }
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
                    enabled = isEditable
                )
            }

            FormField("个人经历（在校经历、项目经历等）", form.resume, isEditable, minLines = 4) { v ->
                onUpdateForm { it.copy(resume = v) }
            }
            FormField("申请理由", form.reason, isEditable, minLines = 4) { v -> onUpdateForm { it.copy(reason = v) } }

            if (isEditable) {
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting,
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
private fun ChoiceSelector(
    title: String,
    choice: Choice,
    departments: List<Department>,
    roles: List<Role>,
    uiState: RecruitmentUiState,
    isEditable: Boolean,
    onSelectDepartment: (Department) -> Unit,
    onSelectRole: (Role) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionDropdown(
                label = "部门",
                options = departments,
                selected = uiState.departments.firstOrNull { it.id == choice.departmentId },
                onSelect = onSelectDepartment,
                optionLabel = { it.name },
                enabled = isEditable,
                modifier = Modifier.weight(1f)
            )
            OptionDropdown(
                label = "职位",
                options = roles,
                selected = (roles + uiState.roles).firstOrNull { it.id == choice.roleId },
                onSelect = onSelectRole,
                optionLabel = { it.name },
                placeholder = if (choice.departmentId > 0) "请选择" else "先选部门",
                enabled = isEditable && choice.departmentId > 0,
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
