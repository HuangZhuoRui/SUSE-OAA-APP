package com.suseoaa.projectoaa.presentation.person

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.ServerHealthStatus
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.suseoaa.projectoaa.shared.data.local.store.AppearanceStore
import com.suseoaa.projectoaa.shared.data.local.store.AppSettingsStore
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
data class PersonUiState(
    val isLoading: Boolean = false,
    val userInfo: PersonData? = null,
    /** 是否登录了协会账号；null 表示还没读到。游客模式下为 false，页面只展示登录入口和本地设置 */
    val isSignedIn: Boolean? = null,
    val isLoggedOut: Boolean = false,
    val isSendingDeleteCode: Boolean = false,
    /** 注销验证码已发出，可以输入验证码了 */
    val isDeleteCodeSent: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isSendingCancelDeleteCode: Boolean = false,
    val isCancelDeleteCodeSent: Boolean = false,
    val isCancelingDelete: Boolean = false,
    val isCancelDeleteSucceeded: Boolean = false,
    val isCheckingServerHealth: Boolean = false,
    val serverHealthStatus: ServerHealthStatus? = null,
    val serverHealthError: String? = null,
    val message: String? = null,
    val isCheckinUnlocked: Boolean = false,  // 652签到功能是否已解锁
    val isDynamicColorEnabled: Boolean = false,
    val dynamicPaletteLightColorHex: String? = null,
    val dynamicPaletteDarkColorHex: String? = null,
    val defaultStartTab: Int = 0,  // 默认起始页（0=首页, 1=课程, 2=教务信息, 3=个人）
    val isPredictiveBackEnabled: Boolean = true,
    val isLiquidGlassTabbarEnabled: Boolean = false,
    val liquidGlassTabbarStyle: Int = 1
)

class PersonViewModel(
    private val personRepository: PersonRepository,
    private val appearanceStore: AppearanceStore,
    private val appSettingsStore: AppSettingsStore,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonUiState())
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        loadCheckinUnlockStatus()
        loadDynamicColorStatus()
        loadDynamicPaletteColors()
        loadDefaultStartTab()
        loadPredictiveBackEnabled()
        loadLiquidGlassTabbarEnabled()
        loadLiquidGlassTabbarStyle()
    }

    private fun loadDynamicColorStatus() {
        viewModelScope.launch {
            appearanceStore.dynamicColorEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isDynamicColorEnabled = enabled) }
            }
        }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            val currentState = _uiState.value.isDynamicColorEnabled
            appearanceStore.saveDynamicColorEnabled(!currentState)
        }
    }

    private fun loadDynamicPaletteColors() {
        viewModelScope.launch {
            appearanceStore.dynamicColorPaletteLightFlow.collect { colorHex ->
                _uiState.update { it.copy(dynamicPaletteLightColorHex = colorHex) }
            }
        }

        viewModelScope.launch {
            appearanceStore.dynamicColorPaletteDarkFlow.collect { colorHex ->
                _uiState.update { it.copy(dynamicPaletteDarkColorHex = colorHex) }
            }
        }
    }

    fun setDynamicPaletteColors(lightColorHex: String?, darkColorHex: String?) {
        viewModelScope.launch {
            appearanceStore.saveDynamicColorPalettes(lightColorHex, darkColorHex)
        }
    }

    /**
     * 加载652签到功能解锁状态
     */
    private fun loadCheckinUnlockStatus() {
        viewModelScope.launch {
            appSettingsStore.checkinUnlockedFlow.collect { unlocked ->
                _uiState.update { it.copy(isCheckinUnlocked = unlocked) }
            }
        }
    }

    /**
     * 解锁652签到功能（永久保存）
     */
    fun unlockCheckinFeature() {
        viewModelScope.launch {
            appSettingsStore.unlockCheckinFeature()
            _uiState.update { it.copy(isCheckinUnlocked = true) }
        }
    }

    private fun loadDefaultStartTab() {
        viewModelScope.launch {
            appSettingsStore.defaultStartTabFlow.collect { tab ->
                _uiState.update { it.copy(defaultStartTab = tab) }
            }
        }
    }

    fun saveDefaultStartTab(tabIndex: Int) {
        viewModelScope.launch {
            appSettingsStore.saveDefaultStartTab(tabIndex)
            _uiState.update { it.copy(defaultStartTab = tabIndex) }
        }
    }

    private fun loadPredictiveBackEnabled() {
        viewModelScope.launch {
            appSettingsStore.predictiveBackEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isPredictiveBackEnabled = enabled) }
            }
        }
    }

    fun togglePredictiveBackEnabled() {
        viewModelScope.launch {
            val currentState = _uiState.value.isPredictiveBackEnabled
            appSettingsStore.savePredictiveBackEnabled(!currentState)
        }
    }

    private fun loadLiquidGlassTabbarEnabled() {
        viewModelScope.launch {
            appSettingsStore.liquidGlassTabbarEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isLiquidGlassTabbarEnabled = enabled) }
            }
        }
    }

    fun toggleLiquidGlassTabbarEnabled() {
        viewModelScope.launch {
            val currentState = _uiState.value.isLiquidGlassTabbarEnabled
            appSettingsStore.saveLiquidGlassTabbarEnabled(!currentState)
        }
    }

    private fun loadLiquidGlassTabbarStyle() {
        viewModelScope.launch {
            appSettingsStore.liquidGlassTabbarStyleFlow.collect { style ->
                _uiState.update { it.copy(liquidGlassTabbarStyle = style) }
            }
        }
    }

    fun setLiquidGlassTabbarStyle(style: Int) {
        viewModelScope.launch {
            appSettingsStore.saveLiquidGlassTabbarStyle(style)
        }
    }

    /** 登录态变化时刷新：登录了就拉个人信息，游客不发请求，免得弹出「未携带 Token」。 */
    private fun observeSession() {
        viewModelScope.launch {
            sessionStore.tokenFlow
                .map { !it.isNullOrEmpty() }
                .distinctUntilChanged()
                .collect { signedIn ->
                    _uiState.update {
                        it.copy(
                            isSignedIn = signedIn,
                            isLoggedOut = if (signedIn) false else it.isLoggedOut,
                            userInfo = it.userInfo.takeIf { signedIn }
                        )
                    }
                    if (signedIn) loadUserInfo()
                }
        }
    }

    fun loadUserInfo() {
        if (_uiState.value.isSignedIn != true) return
        viewModelScope.launch {
            if (_uiState.value.userInfo == null) {
                _uiState.update { it.copy(isLoading = true) }
            }

            val result = personRepository.getPersonInfo()

            result.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userInfo = data
                    )
                }
            }

            result.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = e.message
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            personRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    /** 注销第一步：向绑定邮箱发验证码。 */
    fun sendDeleteAccountCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingDeleteCode = true) }
            personRepository.sendDeleteAccountCode()
                .onSuccess {
                    _uiState.update {
                        it.copy(isSendingDeleteCode = false, isDeleteCodeSent = true, message = "验证码已发送，请查收邮箱")
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSendingDeleteCode = false, message = e.message ?: "验证码发送失败") }
                }
        }
    }

    /** 关掉注销弹窗时重置，下次打开从发送验证码开始。 */
    fun resetDeleteAccountFlow() {
        _uiState.update { it.copy(isDeleteCodeSent = false) }
    }

    /** 注销第二步：凭验证码注销。成功后本地会话已清空，按退出登录处理。 */
    fun deleteAccount(code: String) {
        if (code.isBlank()) {
            _uiState.update { it.copy(message = "请输入邮箱验证码") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            personRepository.deleteAccount(code.trim())
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            isDeleteCodeSent = false,
                            isLoggedOut = true,
                            message = "注销申请已提交"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isDeletingAccount = false, message = e.message ?: "注销失败，请稍后重试")
                    }
                }
        }
    }

    /** 撤销冷静期第一步：向绑定邮箱发送 cancel_delete 验证码。 */
    fun sendCancelDeleteCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCancelDeleteCode = true) }
            personRepository.sendCancelDeleteCode()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSendingCancelDeleteCode = false,
                            isCancelDeleteCodeSent = true,
                            message = "验证码已发送，请查收邮箱"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSendingCancelDeleteCode = false, message = e.message ?: "验证码发送失败")
                    }
                }
        }
    }

    fun resetCancelDeleteFlow() {
        _uiState.update {
            it.copy(
                isCancelDeleteCodeSent = false,
                isCancelDeleteSucceeded = false
            )
        }
    }

    /** 撤销冷静期第二步：提交验证码恢复账号。 */
    fun cancelDeleteAccount(code: String) {
        if (code.isBlank()) {
            _uiState.update { it.copy(message = "请输入邮箱验证码") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelingDelete = true) }
            personRepository.cancelDeleteAccount(code.trim())
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isCancelingDelete = false,
                            isCancelDeleteCodeSent = false,
                            isCancelDeleteSucceeded = true,
                            userInfo = it.userInfo?.copy(isDeleting = false, scheduledDeleteAt = null),
                            message = "已撤销注销，账号已恢复正常"
                        )
                    }
                    loadUserInfo()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isCancelingDelete = false, message = e.message ?: "撤销注销失败，请稍后重试")
                    }
                }
        }
    }

    /** 基础设施探针检测（GET /livez 与 GET /healthz）。 */
    fun checkServerHealth() {
        if (_uiState.value.isCheckingServerHealth) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingServerHealth = true, serverHealthError = null) }
            personRepository.checkServerHealth()
                .onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            isCheckingServerHealth = false,
                            serverHealthStatus = status,
                            serverHealthError = null,
                            message = status.message
                        )
                    }
                }
                .onFailure { e ->
                    val err = e.message ?: "无法连接到协会服务器"
                    _uiState.update {
                        it.copy(
                            isCheckingServerHealth = false,
                            serverHealthStatus = null,
                            serverHealthError = err,
                            message = err
                        )
                    }
                }
        }
    }

    fun updateInfo(username: String, email: String, gradeText: String = "") {
        val cleanUsername = username.trim()
        val cleanEmail = email.trim()
        val cleanGrade = gradeText.trim()
        if (cleanUsername.isBlank() || cleanEmail.isBlank()) {
            _uiState.update { it.copy(message = "填写的内容不能为空") }
            return
        }
        val parsedGrade: Int? = if (cleanGrade.isEmpty()) {
            null
        } else {
            val g = cleanGrade.toIntOrNull()
            if (cleanGrade.length != 4 || g == null || g <= 0) {
                _uiState.update { it.copy(message = "请输入有效的四位数字年级（如 2024）") }
                return
            }
            g
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = personRepository.updateProfile(cleanUsername, cleanEmail, parsedGrade)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "信息更新成功"
                    )
                }
                // 重新加载用户信息
                loadUserInfo()
            }

            result.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = e.message
                    )
                }
            }
        }
    }

    fun uploadAvatar(imageData: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在上传头像...") }

            val result = personRepository.uploadAvatar(imageData)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "头像更新成功"
                    )
                }
                // 重新加载用户信息
                loadUserInfo()
            }

            result.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "头像上传失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
