package com.suseoaa.projectoaa.presentation.login

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.store.AppSettingsStore
import com.suseoaa.projectoaa.shared.domain.repository.OaaAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 登录界面状态
 */
@Immutable
data class LoginUiState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginNotice: String? = null,
    val isLoginSuccess: Boolean = false,
    /** 选了「暂不登录」，游客标记已写入，可以进主页了 */
    val isGuestEntered: Boolean = false
)

/**
 * 登录 ViewModel
 *
 * 会话的保存（token、refresh token、用户 ID、学号）都在 [OaaAuthRepository.login] 里完成，
 * 这里不再接触 SessionStore，也不再把明文密码存下来。
 */
class LoginViewModel(
    private val authRepository: OaaAuthRepository,
    private val appSettingsStore: AppSettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateAccount(account: String) {
        // 账号可以是邮箱，长度放宽到 64
        if (account.length <= 64) {
            _uiState.update { it.copy(account = account, errorMessage = null) }
        }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val currentState = _uiState.value
        val cleanAccount = currentState.account.trim()
        val cleanPassword = currentState.password.trim()

        if (cleanAccount.isBlank() || cleanPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "账号或密码不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.login(cleanAccount, cleanPassword)
                .onSuccess { person ->
                    // 登录后清掉游客标记，退出登录时才会回到登录页
                    appSettingsStore.saveGuestMode(false)
                    val notice = if (person.isDeleting) {
                        val deadline = person.scheduledDeleteAt?.takeIf { it.isNotBlank() }?.let { "（截止 $it）" }.orEmpty()
                        "当前账号处于注销冷静期$deadline，可前往「个人」页撤销注销"
                    } else null
                    _uiState.update { it.copy(isLoading = false, loginNotice = notice, isLoginSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "登录失败")
                    }
                }
        }
    }

    /**
     * 不登录直接使用。课表、教务、物品价值计算等功能不依赖协会账号，
     * App Store 审核要求这些功能不能被登录挡住（Guideline 5.1.1(v)）。
     */
    fun continueAsGuest() {
        viewModelScope.launch {
            appSettingsStore.saveGuestMode(true)
            _uiState.update { it.copy(isGuestEntered = true) }
        }
    }

    fun resetGuestEntered() {
        _uiState.update { it.copy(isGuestEntered = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetLoginSuccess() {
        _uiState.update { it.copy(isLoginSuccess = false, loginNotice = null) }
    }
}
