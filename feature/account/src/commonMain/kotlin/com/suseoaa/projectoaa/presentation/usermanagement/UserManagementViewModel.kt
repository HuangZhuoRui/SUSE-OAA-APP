package com.suseoaa.projectoaa.presentation.usermanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.EnrollmentGrade
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
    // 过滤条件：部门、职位按名称筛选，年级为四位年份，空串表示不限
    val filterKeyword: String = "",
    val filterDepartment: String = "",
    val filterRole: String = "",
    val filterGrade: String = "",
    /** null 不限，false 在校，true 已毕业。 */
    val filterGraduated: Boolean? = null,
    /** 是否启用 is_all=true 不分页拉取全部匹配成员 */
    val showAll: Boolean = false,
    // 批量选择模式（对应 POST /user/batch 一次提交多名成员变更）
    val isBatchMode: Boolean = false,
    val selectedUserIds: Set<Int> = emptySet(),
    // 更新状态
    val isUpdating: Boolean = false,
    val message: String? = null
) {
    val hasMore: Boolean get() = !showAll && users.size < total
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

    fun updateFilters(
        keyword: String? = null,
        department: String? = null,
        role: String? = null,
        grade: String? = null
    ) {
        _uiState.update {
            it.copy(
                filterKeyword = keyword ?: it.filterKeyword,
                filterDepartment = department ?: it.filterDepartment,
                filterRole = role ?: it.filterRole,
                filterGrade = grade ?: it.filterGrade
            )
        }
    }

    fun toggleShowAll() {
        _uiState.update { it.copy(showAll = !it.showAll) }
        search()
    }

    fun toggleBatchMode() {
        _uiState.update {
            val next = !it.isBatchMode
            it.copy(
                isBatchMode = next,
                selectedUserIds = if (next) it.selectedUserIds else emptySet()
            )
        }
    }

    fun toggleSelectUser(user: UserListItem) {
        if (!canEditUser(user)) return
        _uiState.update { state ->
            val updated = if (user.userId in state.selectedUserIds) {
                state.selectedUserIds - user.userId
            } else {
                state.selectedUserIds + user.userId
            }
            state.copy(selectedUserIds = updated)
        }
    }

    fun selectAllEditable() {
        val editableIds = _uiState.value.users.filter { canEditUser(it) }.map { it.userId }.toSet()
        _uiState.update { it.copy(selectedUserIds = editableIds) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedUserIds = emptySet()) }
    }

    /** 按当前筛选条件从第一页重新查询。 */
    fun search() {
        val currentGrade = _uiState.value.filterGrade.trim()
        if (currentGrade.isNotEmpty() && EnrollmentGrade.parse(currentGrade) == null) {
            _uiState.update { it.copy(message = EnrollmentGrade.ERROR_MESSAGE) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            userManagementRepository.listUsers(state.toQuery(page = 1))
                .onSuccess { page ->
                    val deduped = sortByLevel(page.list.distinctBy { user -> user.userId })
                    val visibleIds = deduped.map { it.userId }.toSet()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = deduped,
                            total = if (it.showAll) maxOf(page.total, deduped.size) else page.total,
                            page = 1,
                            selectedUserIds = it.selectedUserIds.intersect(visibleIds)
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
    fun assignableRoles(targets: List<UserListItem> = emptyList()): List<Role> {
        val myLevel = _uiState.value.currentUser?.level ?: 0
        return _uiState.value.roles.filter { role ->
            role.isActive && role.level < myLevel && targets.all { OaaPermission.canAssignRole(it, role) }
        }
    }

    val canEditGrade: Boolean get() = OaaPermission.canEditGrades(_uiState.value.currentUser)

    fun updateGraduationFilter(isGraduated: Boolean?) {
        _uiState.update { it.copy(filterGraduated = isGraduated) }
    }

    fun updateUser(user: UserListItem, departmentId: Int, roleId: Int, grade: Int? = null) {
        if (_uiState.value.isUpdating) return
        validateUpdate(listOf(user), departmentId, roleId, grade)?.let { error ->
            _uiState.update { it.copy(message = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            userManagementRepository.batchUpdate(
                listOf(UserBatchItem(userId = user.userId, departmentId = departmentId, roleId = roleId, grade = grade))
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

    /** 一次提交多名已选成员的部门与职位变更（POST /user/batch）。 */
    fun batchUpdateSelected(departmentId: Int, roleId: Int, grade: Int? = null) {
        if (_uiState.value.isUpdating) return
        val ids = _uiState.value.selectedUserIds.toList()
        if (ids.isEmpty()) {
            _uiState.update { it.copy(message = "请先选择要调整的成员") }
            return
        }
        val targets = _uiState.value.users.filter { it.userId in ids }
        if (targets.size != ids.size) {
            _uiState.update { it.copy(message = "成员列表已变化，请重新选择") }
            return
        }
        validateUpdate(targets, departmentId, roleId, grade)?.let { error ->
            _uiState.update { it.copy(message = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            val items = ids.map { UserBatchItem(userId = it, departmentId = departmentId, roleId = roleId, grade = grade) }
            userManagementRepository.batchUpdate(items)
                .onSuccess { failures ->
                    if (failures.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isUpdating = false,
                                isBatchMode = false,
                                selectedUserIds = emptySet(),
                                message = "已批量更新 ${items.size} 名成员"
                            )
                        }
                    } else {
                        val successCount = (items.size - failures.size).coerceAtLeast(0)
                        val detail = failures.joinToString("\n") {
                            "${it.name.ifBlank { it.username }.ifBlank { it.studentId }}：${it.errorMessage}"
                        }
                        _uiState.update {
                            it.copy(
                                isUpdating = false,
                                message = "成功 $successCount 人，失败 ${failures.size} 人：\n$detail"
                            )
                        }
                    }
                    search()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isUpdating = false, message = "批量更新失败：${error.message}") }
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
                            selectedUserIds = state.selectedUserIds - user.userId,
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
        grade = EnrollmentGrade.parse(filterGrade),
        page = page,
        pageSize = PAGE_SIZE,
        isAll = true.takeIf { showAll },
        isGraduated = filterGraduated
    )

    private fun validateUpdate(targets: List<UserListItem>, departmentId: Int, roleId: Int, grade: Int?): String? {
        if (targets.any { !canEditUser(it) }) return "权限不足：只能调整等级比自己低的其他成员"
        if (grade != null) {
            if (!canEditGrade) return "权限不足：只有副会长及以上管理才可修改年级"
            if (!EnrollmentGrade.isValid(grade)) return EnrollmentGrade.ERROR_MESSAGE
        }
        if (_uiState.value.departments.none { it.id == departmentId && it.isActive }) return "请选择有效的部门"
        val role = _uiState.value.roles.firstOrNull { it.id == roleId } ?: return "请选择有效的职位"
        if (targets.any { !OaaPermission.canAssignRole(it, role) }) return "已毕业成员不可任命在任职务"
        if (role !in assignableRoles(targets)) return "权限不足：只能分配比自己等级低的有效职位"
        return null
    }

    private fun sortByLevel(users: List<UserListItem>): List<UserListItem> {
        val levels = _uiState.value.roles.associate { it.name to it.level }
        return users.sortedByDescending { it.roleLevel ?: levels[it.role] ?: -1 }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
