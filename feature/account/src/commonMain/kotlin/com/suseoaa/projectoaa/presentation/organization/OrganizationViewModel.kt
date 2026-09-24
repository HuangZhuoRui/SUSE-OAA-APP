package com.suseoaa.projectoaa.presentation.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrganizationUiState(
    val departments: List<Department> = emptyList(),
    val roles: List<Role> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)

/**
 * 组织架构维护：部门与职位的新建、修改、停用。后端要求 level >= 80。
 */
class OrganizationViewModel(
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizationUiState())
    val uiState: StateFlow<OrganizationUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val departments = organizationRepository.getDepartments(forceRefresh = true)
            val roles = organizationRepository.getRoles(forceRefresh = true)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    departments = departments.getOrDefault(it.departments),
                    roles = roles.getOrDefault(it.roles).sortedByDescending { role -> role.level },
                    message = (departments.exceptionOrNull() ?: roles.exceptionOrNull())?.message
                )
            }
        }
    }

    fun saveDepartment(existing: Department?, name: String, type: String, isActive: Boolean) {
        if (name.isBlank()) {
            _uiState.update { it.copy(message = "部门名称不能为空") }
            return
        }
        save(if (existing == null) "部门已创建" else "部门已更新") {
            if (existing == null) {
                organizationRepository.createDepartment(name.trim(), type)
            } else {
                organizationRepository.updateDepartment(existing.id, name.trim(), type, isActive)
            }
        }
    }

    fun saveRole(existing: Role?, name: String, levelText: String, type: String, isActive: Boolean) {
        val level = levelText.trim().toIntOrNull()
        val error = when {
            name.isBlank() -> "职位名称不能为空"
            level == null || level < 0 -> "等级必须是非负整数"
            else -> null
        }
        if (error != null || level == null) {
            _uiState.update { it.copy(message = error) }
            return
        }
        save(if (existing == null) "职位已创建" else "职位已更新") {
            if (existing == null) {
                organizationRepository.createRole(name.trim(), level, type)
            } else {
                organizationRepository.updateRole(existing.id, name.trim(), level, type, isActive)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun save(successMessage: String, action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            action()
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, message = successMessage) }
                    refresh()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, message = error.message ?: "保存失败") }
                }
        }
    }
}
