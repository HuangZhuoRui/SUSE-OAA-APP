package com.suseoaa.projectoaa.ui.screen.usermanagement

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.usermanagement.UserManagementUiState
import com.suseoaa.projectoaa.presentation.usermanagement.UserManagementViewModel
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.ui.component.common.OptionDropdown
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

/** 下拉框里「不限」这一项。 */
private const val ANY = ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var editingUser by remember { mutableStateOf<UserListItem?>(null) }
    var deletingUser by remember { mutableStateOf<UserListItem?>(null) }
    var showBatchEditDialog by remember { mutableStateOf(false) }

    val hasEditableUsers = remember(uiState.users, uiState.currentUser, uiState.roles) {
        uiState.users.any { viewModel.canEditUser(it) }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            ToastManager.showToast(it)
            viewModel.clearMessage()
        }
    }

    AdaptivePageScaffold(
        sharedTransitionKey = "user_management_feature",
        title = "权利的游戏",
        onBack = onNavigateBack,
        actions = {
            if (hasEditableUsers || uiState.isBatchMode) {
                TextButton(onClick = viewModel::toggleBatchMode) {
                    Text(if (uiState.isBatchMode) "完成" else "批量调整")
                }
            }
        },
        compactContent = { modifier ->
            Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
                var isFilterExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { isFilterExpanded = !isFilterExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("筛选条件", style = MaterialTheme.typography.titleMedium)
                            Icon(
                                imageVector = if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isFilterExpanded) "收起筛选" else "展开筛选"
                            )
                        }
                        if (isFilterExpanded) {
                            FilterFields(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
                UserList(
                    uiState = uiState,
                    viewModel = viewModel,
                    onEditClick = { editingUser = it },
                    onDeleteClick = { deletingUser = it },
                    onBatchEditClick = { showBatchEditDialog = true }
                )
            }
        },
        tabletContent = { modifier ->
            // 平板布局：左侧筛选栏，右侧结果列表
            Row(
                modifier = modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(0.3f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "筛选条件",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        FilterFields(uiState = uiState, viewModel = viewModel)
                    }
                }
                Box(modifier = Modifier.weight(0.7f).fillMaxSize()) {
                    UserList(
                        uiState = uiState,
                        viewModel = viewModel,
                        onEditClick = { editingUser = it },
                        onDeleteClick = { deletingUser = it },
                        onBatchEditClick = { showBatchEditDialog = true }
                    )
                }
            }
        }
    )

    editingUser?.let { user ->
        EditUserDialog(
            user = user,
            departments = uiState.departments.filter { it.isActive },
            roles = viewModel.assignableRoles(),
            onDismiss = { editingUser = null },
            onConfirm = { departmentId, roleId ->
                viewModel.updateUser(user, departmentId, roleId)
                editingUser = null
            }
        )
    }

    if (showBatchEditDialog) {
        BatchEditUsersDialog(
            selectedCount = uiState.selectedUserIds.size,
            departments = uiState.departments.filter { it.isActive },
            roles = viewModel.assignableRoles(),
            onDismiss = { showBatchEditDialog = false },
            onConfirm = { departmentId, roleId ->
                viewModel.batchUpdateSelected(departmentId, roleId)
                showBatchEditDialog = false
            }
        )
    }

    deletingUser?.let { user ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { deletingUser = null },
            title = { Text("删除成员") },
            text = { Text("确定要删除 ${user.name.ifBlank { user.username }}（${user.studentId}）吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(user)
                        deletingUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingUser = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun FilterFields(
    uiState: UserManagementUiState,
    viewModel: UserManagementViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = uiState.filterKeyword,
            onValueChange = { viewModel.updateFilters(keyword = it) },
            label = { Text("姓名 / 学号 / 用户名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OptionDropdown(
            label = "部门",
            options = listOf(ANY) + uiState.departments.map { it.name },
            selected = uiState.filterDepartment,
            onSelect = { viewModel.updateFilters(department = it) },
            optionLabel = { it.ifEmpty { "全部部门" } }
        )
        OptionDropdown(
            label = "职位",
            options = listOf(ANY) + uiState.roles.map { it.name },
            selected = uiState.filterRole,
            onSelect = { viewModel.updateFilters(role = it) },
            optionLabel = { it.ifEmpty { "全部职位" } }
        )
        OutlinedTextField(
            value = uiState.filterGrade,
            onValueChange = { input ->
                viewModel.updateFilters(grade = input.filter { it.isDigit() }.take(4))
            },
            label = { Text("年级（如 2024）") },
            placeholder = { Text("全部年级") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "显示全部匹配成员",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = uiState.showAll,
                onCheckedChange = { viewModel.toggleShowAll() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
        Button(
            onClick = viewModel::search,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("查询")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserList(
    uiState: UserManagementUiState,
    viewModel: UserManagementViewModel,
    onEditClick: (UserListItem) -> Unit,
    onDeleteClick: (UserListItem) -> Unit,
    onBatchEditClick: () -> Unit
) {
    if (uiState.isLoading && uiState.users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.isUpdating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (uiState.isBatchMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选 ${uiState.selectedUserIds.size} 人",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::selectAllEditable) { Text("全选") }
                    if (uiState.selectedUserIds.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearSelection) { Text("清空") }
                    }
                    Button(
                        onClick = onBatchEditClick,
                        enabled = uiState.selectedUserIds.isNotEmpty() && !uiState.isUpdating
                    ) {
                        Text("调整")
                    }
                }
            }
        }
        Text(
            text = "共 ${uiState.total} 人",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.users.isNotEmpty(),
            onRefresh = viewModel::search,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.users, key = { it.userId }) { user ->
                    val canEdit = viewModel.canEditUser(user)
                    UserCard(
                        user = user,
                        canEdit = canEdit,
                        canDelete = viewModel.canDeleteUser(user),
                        isBatchMode = uiState.isBatchMode,
                        isSelected = user.userId in uiState.selectedUserIds,
                        onToggleSelect = { viewModel.toggleSelectUser(user) },
                        onEditClick = { onEditClick(user) },
                        onDeleteClick = { onDeleteClick(user) }
                    )
                }
                if (uiState.hasMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                TextButton(onClick = viewModel::loadMore) { Text("加载更多") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(
    user: UserListItem,
    canEdit: Boolean,
    canDelete: Boolean,
    isBatchMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(
                if (isBatchMode && canEdit) Modifier.clickable(onClick = onToggleSelect) else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = if (canEdit) ({ onToggleSelect() }) else null,
                    enabled = canEdit,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifEmpty { "未命名" },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "学号: ${user.studentId}　用户名: ${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val gradePrefix = user.resolvedGrade?.let { "年级: ${it}级　" }.orEmpty()
                Text(
                    text = "${gradePrefix}部门: ${user.department}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "职位: ${user.role}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!isBatchMode) {
                if (canEdit) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (canDelete) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditUserDialog(
    user: UserListItem,
    departments: List<Department>,
    roles: List<Role>,
    onDismiss: () -> Unit,
    onConfirm: (departmentId: Int, roleId: Int) -> Unit
) {
    var department by remember(user) { mutableStateOf(departments.firstOrNull { it.name == user.department }) }
    var role by remember(user) { mutableStateOf(roles.firstOrNull { it.name == user.role }) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("调整 ${user.name.ifBlank { user.username }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionDropdown(
                    label = "部门",
                    options = departments,
                    selected = department,
                    onSelect = { department = it },
                    optionLabel = { it.name }
                )
                OptionDropdown(
                    label = "职位",
                    options = roles,
                    selected = role,
                    onSelect = { role = it },
                    optionLabel = { "${it.name}（等级 ${it.level}）" }
                )
                if (roles.isEmpty()) {
                    Text(
                        "没有可分配的职位：只能分配比自己等级低的职位",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            val selectedDepartment = department
            val selectedRole = role
            Button(
                enabled = selectedDepartment != null && selectedRole != null,
                onClick = {
                    if (selectedDepartment != null && selectedRole != null) {
                        onConfirm(selectedDepartment.id, selectedRole.id)
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun BatchEditUsersDialog(
    selectedCount: Int,
    departments: List<Department>,
    roles: List<Role>,
    onDismiss: () -> Unit,
    onConfirm: (departmentId: Int, roleId: Int) -> Unit
) {
    var department by remember { mutableStateOf<Department?>(null) }
    var role by remember { mutableStateOf<Role?>(null) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("批量调整 $selectedCount 名成员") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "所选成员将统一调整为以下部门和职位：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OptionDropdown(
                    label = "目标部门",
                    options = departments,
                    selected = department,
                    onSelect = { department = it },
                    optionLabel = { it.name }
                )
                OptionDropdown(
                    label = "目标职位",
                    options = roles,
                    selected = role,
                    onSelect = { role = it },
                    optionLabel = { "${it.name}（等级 ${it.level}）" }
                )
                if (roles.isEmpty()) {
                    Text(
                        "没有可分配的职位：只能分配比自己等级低的职位",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            val selectedDepartment = department
            val selectedRole = role
            Button(
                enabled = selectedDepartment != null && selectedRole != null,
                onClick = {
                    if (selectedDepartment != null && selectedRole != null) {
                        onConfirm(selectedDepartment.id, selectedRole.id)
                    }
                }
            ) { Text("确认调整") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
