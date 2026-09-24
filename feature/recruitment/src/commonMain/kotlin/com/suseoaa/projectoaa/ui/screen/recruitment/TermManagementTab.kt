package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.recruitment.TermDraft
import com.suseoaa.projectoaa.presentation.recruitment.TermManagementUiState
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.TermTypes
import com.suseoaa.projectoaa.ui.component.common.OptionDropdown

// 「周期管理」Tab：新建 / 修改 / 删除周期，为周期指定面试官。

/** 正在编辑的周期；existing 为 null 表示新建。 */
private data class TermEditing(val existing: Term?)

@Composable
internal fun TermManagementTab(
    uiState: TermManagementUiState,
    onSaveTerm: (Term?, TermDraft) -> Unit,
    onDeleteTerm: (Term) -> Unit,
    onSearchUsers: (String) -> Unit,
    onAddInterviewers: (Term, List<UserListItem>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<TermEditing?>(null) }
    var deleting by remember { mutableStateOf<Term?>(null) }
    var addingInterviewersTo by remember { mutableStateOf<Term?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading || uiState.isSaving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Button(onClick = { editing = TermEditing(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("新建周期", modifier = Modifier.padding(start = 4.dp))
                }
            }
            items(uiState.terms) { term ->
                TermCard(
                    term = term,
                    onEdit = { editing = TermEditing(term) },
                    onDelete = { deleting = term },
                    onAddInterviewers = { addingInterviewersTo = term }
                )
            }
        }
    }

    editing?.let { current ->
        TermDialog(
            existing = current.existing,
            onDismiss = { editing = null },
            onConfirm = { draft ->
                onSaveTerm(current.existing, draft)
                editing = null
            }
        )
    }

    deleting?.let { term ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除周期") },
            text = { Text("确定删除「${term.displayTitle()}」吗？该周期下的申请可能一并失效。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTerm(term)
                        deleting = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }

    addingInterviewersTo?.let { term ->
        InterviewerDialog(
            term = term,
            uiState = uiState,
            onSearch = onSearchUsers,
            onDismiss = { addingInterviewersTo = null },
            onConfirm = { users, remark ->
                onAddInterviewers(term, users, remark)
                addingInterviewersTo = null
            }
        )
    }
}

@Composable
private fun TermCard(term: Term, onEdit: () -> Unit, onDelete: () -> Unit, onAddInterviewers: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(term.displayTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${term.year} · ${term.type}", style = MaterialTheme.typography.bodySmall)
            Text(
                "填写：${term.editPeriod.startAt.take(10)} ~ ${term.editPeriod.endAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "公示：${term.queryPeriod.startAt.take(10)} ~ ${term.queryPeriod.endAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (term.isExecuted) {
                Text(
                    "已执行录取${term.executedAt?.let { "（${it.take(10)}）" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onAddInterviewers) { Text("添加面试官") }
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun TermDialog(existing: Term?, onDismiss: () -> Unit, onConfirm: (TermDraft) -> Unit) {
    var draft by remember { mutableStateOf(existing?.let { TermDraft.from(it) } ?: TermDraft(type = TermTypes.RECRUITMENT)) }
    val isNew = existing == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "新建周期" else "编辑周期") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 更新接口不接受年份和类型，编辑时只读
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.year,
                        onValueChange = { v -> draft = draft.copy(year = v.filter { it.isDigit() }.take(4)) },
                        label = { Text("年份") },
                        enabled = isNew,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OptionDropdown(
                        label = "类型",
                        options = TermTypes.all,
                        selected = draft.type.ifBlank { null },
                        onSelect = { draft = draft.copy(type = it) },
                        optionLabel = { it },
                        enabled = isNew,
                        modifier = Modifier.weight(1f)
                    )
                }
                DialogField("标题", draft.title) { draft = draft.copy(title = it) }
                Text("日期格式：2026-09-01", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogField("填写开始", draft.editStart, Modifier.weight(1f)) { draft = draft.copy(editStart = it) }
                    DialogField("填写结束", draft.editEnd, Modifier.weight(1f)) { draft = draft.copy(editEnd = it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogField("公示开始", draft.queryStart, Modifier.weight(1f)) { draft = draft.copy(queryStart = it) }
                    DialogField("公示结束", draft.queryEnd, Modifier.weight(1f)) { draft = draft.copy(queryEnd = it) }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(draft) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DialogField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun InterviewerDialog(
    term: Term,
    uiState: TermManagementUiState,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (List<UserListItem>, String) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Map<Int, UserListItem>>(emptyMap()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("为「${term.displayTitle()}」添加面试官") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("搜索成员") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { onSearch(keyword) }) { Text("搜索") }
                }
                if (uiState.isSearchingUsers) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                    // 已选的成员即使不在本次搜索结果里也保留在列表顶部
                    val shown = (selected.values + uiState.userResults).distinctBy { it.userId }
                    items(shown, key = { it.userId }) { user ->
                        val checked = user.userId in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (checked) selected - user.userId else selected + (user.userId to user)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text("${user.name}（${user.department} · ${user.role}）")
                        }
                    }
                }
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注（留空则用姓名）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "目前后端没有面试官列表接口，添加后暂时无法在 App 内查看或移除。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(enabled = selected.isNotEmpty(), onClick = { onConfirm(selected.values.toList(), remark.trim()) }) {
                Text("添加 ${selected.size} 人")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
