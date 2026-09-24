package com.suseoaa.projectoaa.ui.screen.usermanagement

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        compactContent = { modifier ->
            Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
                var isFilterExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).animateContentSize(),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    onDeleteClick = { deletingUser = it }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                        onDeleteClick = { deletingUser = it }
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

    deletingUser?.let { user ->
        AlertDialog(
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

@Composable
private fun UserList(
    uiState: UserManagementUiState,
    viewModel: UserManagementViewModel,
    onEditClick: (UserListItem) -> Unit,
    onDeleteClick: (UserListItem) -> Unit
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.isUpdating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = "共 ${uiState.total} 人",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.users, key = { it.userId }) { user ->
                UserCard(
                    user = user,
                    canEdit = viewModel.canEditUser(user),
                    canDelete = viewModel.canDeleteUser(user),
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

@Composable
fun UserCard(
    user: UserListItem,
    canEdit: Boolean,
    canDelete: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                Text(
                    text = "部门: ${user.department}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "职位: ${user.role}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
        containerColor = MaterialTheme.colorScheme.background,
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
