package com.suseoaa.projectoaa.presentation.changePassword

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.presentation.forgetpassword.validatePasswordChange
import com.suseoaa.projectoaa.shared.domain.repository.OaaAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 修改密码界面状态
 */
@Immutable
data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)

/**
 * 修改密码 ViewModel：已登录，凭旧密码修改。忘记旧密码的走「忘记密码」页。
 */
class ChangePasswordViewModel(
    private val authRepository: OaaAuthRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun updateOldPassword(password: String) {
        _uiState.update { it.copy(oldPassword = password, errorMessage = null) }
    }

    fun updateNewPassword(password: String) {
        _uiState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun updateConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun changePassword() {
        val currentState = _uiState.value
        val validationError = if (currentState.oldPassword.isBlank()) {
            "请输入当前密码"
        } else {
            validatePasswordChange(currentState.newPassword, currentState.confirmPassword)
                ?: "新密码不能与当前密码相同".takeIf { currentState.newPassword == currentState.oldPassword }
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.updatePassword(currentState.oldPassword, currentState.newPassword)
                .onSuccess { msg ->
                    // 修改密码后强制登出，用新密码重新登录
                    personRepository.logout()
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, successMessage = msg) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "修改失败") }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
