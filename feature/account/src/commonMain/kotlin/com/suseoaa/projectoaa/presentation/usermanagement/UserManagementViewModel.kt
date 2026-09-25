package com.suseoaa.projectoaa.presentation.usermanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery
import com.suseoaa.projectoaa.shared.domain.permission.OaaPermission
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository
import com.suseoaa.projectoaa.shared.domain.repository.UserManagementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val currentUser: CurrentUser? = null,
    val departments: List<Department> = emptyList(),
    val roles: List<Role> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val users: List<UserListItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    // 过滤条件：部门、职位按名称筛选，空串表示不限
    val filterKeyword: String = "",
    val filterDepartment: String = "",
    val filterRole: String = "",
    // 更新状态
    val isUpdating: Boolean = false,
    val message: String? = null
) {
    val hasMore: Boolean get() = users.size < total
}

class UserManagementViewModel(
    private val personRepository: PersonRepository,
    private val userManagementRepository: UserManagementRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val departments = organizationRepository.getDepartments().getOrDefault(emptyList())
            val roles = organizationRepository.getRoles().getOrDefault(emptyList())
            val currentUser = personRepository.getCurrentUser().getOrNull()
            _uiState.update { it.copy(currentUser = currentUser, departments = departments, roles = roles) }
            search()
        }
    }

    fun updateFilters(keyword: String? = null, department: String? = null, role: String? = null) {
        _uiState.update {
            it.copy(
                filterKeyword = keyword ?: it.filterKeyword,
                filterDepartment = department ?: it.filterDepartment,
                filterRole = role ?: it.filterRole
            )
        }
    }

    /** 按当前筛选条件从第一页重新查询。 */
    fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            userManagementRepository.listUsers(state.toQuery(page = 1))
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = sortByLevel(page.list.distinctBy { user -> user.userId }),
                            total = page.total,
                            page = 1
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, message = error.message ?: "获取成员列表失败") }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = state.page + 1
            userManagementRepository.listUsers(state.toQuery(page = nextPage))
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            users = sortByLevel((it.users + page.list).distinctBy { user -> user.userId }),
                            total = page.total,
                            page = nextPage
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingMore = false, message = error.message ?: "加载失败") }
                }
        }
    }

    /** 优先用列表返回的 role_level；都拿不到时按最高等级处理，避免误改。 */
    fun levelOf(user: UserListItem): Int =
        user.roleLevel ?: _uiState.value.roles.firstOrNull { it.name == user.role }?.level ?: Int.MAX_VALUE

    fun canEditUser(user: UserListItem): Boolean =
        user.userId != _uiState.value.currentUser?.person?.userId &&
            OaaPermission.canEditUser(_uiState.value.currentUser, levelOf(user))

    fun canDeleteUser(user: UserListItem): Boolean =
        OaaPermission.isAdmin(_uiState.value.currentUser) && canEditUser(user)

    /** 可以分配给别人的职位：只能是比自己等级低的。 */
    fun assignableRoles(): List<Role> {
        val myLevel = _uiState.value.currentUser?.level ?: 0
        return _uiState.value.roles.filter { it.isActive && it.level < myLevel }
    }

    fun updateUser(user: UserListItem, departmentId: Int, roleId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            userManagementRepository.batchUpdate(
                listOf(UserBatchItem(userId = user.userId, departmentId = departmentId, roleId = roleId))
            ).onSuccess { failures ->
                val message = if (failures.isEmpty()) {
                    "已更新 ${user.name.ifBlank { user.username }}"
                } else {
                    failures.joinToString("\n") { "${it.name.ifBlank { it.username }}：${it.errorMessage}" }
                }
                _uiState.update { it.copy(isUpdating = false, message = message) }
                search()
            }.onFailure { error ->
                _uiState.update { it.copy(isUpdating = false, message = "更新失败：${error.message}") }
            }
        }
    }

    fun deleteUser(user: UserListItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            userManagementRepository.deleteUser(user.userId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isUpdating = false,
                            users = state.users.filterNot { it.userId == user.userId },
                            total = (state.total - 1).coerceAtLeast(0),
                            message = "已删除 ${user.name.ifBlank { user.username }}"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isUpdating = false, message = "删除失败：${error.message}") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun UserManagementUiState.toQuery(page: Int) = UserQuery(
        keyword = filterKeyword.trim(),
        department = filterDepartment,
        role = filterRole,
        page = page,
        pageSize = PAGE_SIZE
    )

    private fun sortByLevel(users: List<UserListItem>): List<UserListItem> {
        val levels = _uiState.value.roles.associate { it.name to it.level }
        return users.sortedByDescending { it.roleLevel ?: levels[it.role] ?: -1 }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
