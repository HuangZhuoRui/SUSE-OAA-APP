package com.suseoaa.projectoaa.ui.screen.organization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.organization.OrganizationViewModel
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.DepartmentTypes
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.ui.component.common.OptionDropdown
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

/** 正在编辑的对象：新建时 existing 为 null。 */
private sealed interface OrgEditing {
    data class DepartmentEdit(val existing: Department?) : OrgEditing
    data class RoleEdit(val existing: Role?) : OrgEditing
}

/**
 * 组织架构管理：部门与职位。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrganizationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<OrgEditing?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            ToastManager.showToast(it)
            viewModel.clearMessage()
        }
    }

    AdaptivePageScaffold(
        title = "组织架构",
        onBack = onNavigateBack,
        sharedTransitionKey = "organization_feature",
        actions = {
            IconButton(onClick = {
                editing = if (selectedTab == 0) OrgEditing.DepartmentEdit(null) else OrgEditing.RoleEdit(null)
            }) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "新建部门" else "新建职位")
            }
        }
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("部门") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("职位") })
            }
            if (uiState.isLoading || uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedTab == 0) {
                    items(uiState.departments, key = { "d${it.id}" }) { department ->
                        OrgRow(
                            title = department.name,
                            subtitle = "类型：${department.type}",
                            isActive = department.isActive,
                            onEdit = { editing = OrgEditing.DepartmentEdit(department) }
                        )
                    }
                } else {
                    items(uiState.roles, key = { "r${it.id}" }) { role ->
                        OrgRow(
                            title = role.name,
                            subtitle = "等级 ${role.level} · 类型：${role.type}",
                            isActive = role.isActive,
                            onEdit = { editing = OrgEditing.RoleEdit(role) }
                        )
                    }
                }
            }
        }
    }

    when (val current = editing) {
        is OrgEditing.DepartmentEdit -> DepartmentDialog(
            existing = current.existing,
            onDismiss = { editing = null },
            onConfirm = { name, type, isActive ->
                viewModel.saveDepartment(current.existing, name, type, isActive)
                editing = null
            }
        )
        is OrgEditing.RoleEdit -> RoleDialog(
            existing = current.existing,
            onDismiss = { editing = null },
            onConfirm = { name, level, type, isActive ->
                viewModel.saveRole(current.existing, name, level, type, isActive)
                editing = null
            }
        )
        null -> Unit
    }
}

@Composable
private fun OrgRow(title: String, subtitle: String, isActive: Boolean, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isActive) {
                AssistChip(onClick = {}, enabled = false, label = { Text("已停用") })
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ActiveSwitch(isActive: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("启用", modifier = Modifier.weight(1f))
        Switch(checked = isActive, onCheckedChange = onChange)
    }
}

@Composable
private fun DepartmentDialog(
    existing: Department?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember { mutableStateOf(existing?.type?.ifBlank { null } ?: DepartmentTypes.DEPARTMENT) }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "新建部门" else "编辑部门") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OptionDropdown(
                    label = "类型",
                    options = DepartmentTypes.all,
                    selected = type,
                    onSelect = { type = it },
                    optionLabel = { it }
                )
                // 新建接口不接受启用状态，只有编辑时才显示
                if (existing != null) ActiveSwitch(isActive) { isActive = it }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, type, isActive) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RoleDialog(
    existing: Role?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, level: String, type: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var level by remember { mutableStateOf(existing?.level?.toString().orEmpty()) }
    var type by remember { mutableStateOf(existing?.type?.ifBlank { null } ?: DepartmentTypes.DEPARTMENT) }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "新建职位" else "编辑职位") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = level,
                    onValueChange = { input -> level = input.filter { it.isDigit() }.take(3) },
                    label = { Text("等级") },
                    supportingText = { Text("30 起可管理本部门，80 起可管理全协会") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OptionDropdown(
                    label = "类型",
                    options = DepartmentTypes.all,
                    selected = type,
                    onSelect = { type = it },
                    optionLabel = { it }
                )
                if (existing != null) ActiveSwitch(isActive) { isActive = it }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, level, type, isActive) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
