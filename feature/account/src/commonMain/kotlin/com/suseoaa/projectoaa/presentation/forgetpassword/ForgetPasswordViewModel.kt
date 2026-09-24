package com.suseoaa.projectoaa.presentation.forgetpassword

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.repository.OaaAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 忘记密码界面状态
 */
@Immutable
data class ForgetPasswordUiState(
    val account: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val emailCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)

/**
 * 忘记密码 ViewModel：邮箱验证码 + 新密码。
 */
class ForgetPasswordViewModel(
    private val authRepository: OaaAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgetPasswordUiState())
    val uiState: StateFlow<ForgetPasswordUiState> = _uiState.asStateFlow()

    fun confirmAccount(account: String) {
        _uiState.update { it.copy(account = account, errorMessage = null) }
    }

    fun updateNewPassword(password: String) {
        _uiState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun updateConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun updateEmailCode(emailCode: String) {
        _uiState.update { it.copy(emailCode = emailCode, errorMessage = null) }
    }

    fun changePassword() {
        val currentState = _uiState.value
        val validationError = validatePasswordChange(
            newPassword = currentState.newPassword,
            confirmPassword = currentState.confirmPassword
        ) ?: when {
            currentState.account.isBlank() -> "请输入账号"
            currentState.emailCode.isBlank() -> "请输入邮箱验证码"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.resetPassword(
                account = currentState.account.trim(),
                code = currentState.emailCode.trim(),
                newPassword = currentState.newPassword
            ).onSuccess { msg ->
                _uiState.update { it.copy(isLoading = false, isSuccess = true, successMessage = msg) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "重置失败") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /** 获取邮箱验证码 */
    fun getEmailCode() {
        val account = _uiState.value.account.trim()
        if (account.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入账号") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            authRepository.sendResetPasswordCode(account)
                .onSuccess { msg -> _uiState.update { it.copy(successMessage = msg) } }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "发送失败") } }
        }
    }
}

/** 新密码的本地校验，忘记密码与修改密码共用。 */
internal fun validatePasswordChange(newPassword: String, confirmPassword: String): String? = when {
    newPassword.isBlank() -> "请输入新密码"
    newPassword.length < 6 -> "新密码长度至少6位"
    newPassword != confirmPassword -> "两次密码输入不一致"
    else -> null
}
