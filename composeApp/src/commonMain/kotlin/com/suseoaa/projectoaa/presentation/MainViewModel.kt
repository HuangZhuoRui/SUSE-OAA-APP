package com.suseoaa.projectoaa.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.store.BackgroundPageIds
import com.suseoaa.projectoaa.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner
import com.suseoaa.projectoaa.shared.data.remote.network.OaaSessionEvents
import com.suseoaa.projectoaa.shared.data.local.store.AppearanceStore
import com.suseoaa.projectoaa.shared.data.local.store.CredentialStore
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.data.local.store.AppSettingsStore

/**
 * 主 ViewModel - 管理应用级状态
 */
class MainViewModel(
    private val appearanceStore: AppearanceStore,
    private val credentialStore: CredentialStore,
    private val sessionStore: SessionStore,
    private val appSettingsStore: AppSettingsStore,
    private val userDataCleaner: UserDataCleaner,
    sessionEvents: OaaSessionEvents
) : ViewModel() {

    private val _selectedMainTab = MutableStateFlow(0)
    val selectedMainTab: StateFlow<Int> = _selectedMainTab.asStateFlow()

    private val _homeFeatureDrawerExpanded = MutableStateFlow(false)
    val homeFeatureDrawerExpanded: StateFlow<Boolean> = _homeFeatureDrawerExpanded.asStateFlow()

    private val _academicFeatureDrawerExpanded = MutableStateFlow(false)
    val academicFeatureDrawerExpanded: StateFlow<Boolean> = _academicFeatureDrawerExpanded.asStateFlow()

    /** 升级迁移做完之前不决定起始页，否则旧会话会先把人带进主页再被踢出来。 */
    private val sessionMigrated = MutableStateFlow(false)

    /**
     * refresh token 也失效、会话已被清空时发出，应用壳层据此跳回登录页。
     * token 的续期本身在网络层自动完成，这里只处理续不上的情况。
     */
    val sessionExpired: SharedFlow<Unit> = sessionEvents.expired

    init {
        // 启动时读取默认起始页并应用
        viewModelScope.launch {
            val startTab = appSettingsStore.defaultStartTabFlow.first()
            if (startTab != 0) {
                _selectedMainTab.value = startTab
            }
        }

        viewModelScope.launch {
            // v1 为了定期重新登录把明文密码存在本地；v2 用 refresh token 续期，不再需要，直接删掉
            credentialStore.clear()
            // v1 的会话只有 access token，v2 后端不认，清掉让用户重新登录
            if (sessionStore.isLegacySession()) {
                userDataCleaner.clearSession()
            }
            sessionMigrated.value = true
        }
    }

    fun updateSelectedMainTab(index: Int) {
        _selectedMainTab.value = index
    }

    fun updateHomeFeatureDrawerExpanded(expanded: Boolean) {
        _homeFeatureDrawerExpanded.value = expanded
    }

    fun updateAcademicFeatureDrawerExpanded(expanded: Boolean) {
        _academicFeatureDrawerExpanded.value = expanded
    }

    /**
     * 启动目标页面 - 根据软件账号 Token 是否存在决定
     * 使用 tokenFlow (JWT Token) 而不是 currentStudentId (教务系统学号)
     * 初始值为 null，表示正在加载，防止登录页闪烁
     */
    val startDestination: StateFlow<String?> = combine(sessionStore.tokenFlow, sessionMigrated) { token, migrated ->
            when {
                !migrated -> null
                token.isNullOrEmpty() -> Screen.Login.route
                else -> Screen.Main.route
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null  // 初始值为 null，表示正在加载
        )

    val dynamicColorEnabled: StateFlow<Boolean> = appearanceStore.dynamicColorEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val dynamicPaletteLightColorHex: StateFlow<String?> = appearanceStore.dynamicColorPaletteLightFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val dynamicPaletteDarkColorHex: StateFlow<String?> = appearanceStore.dynamicColorPaletteDarkFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val appBackgroundImages: StateFlow<Map<String, String?>> = appearanceStore.appBackgroundImagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = mapOf(
                BackgroundPageIds.DEFAULT to null,
                BackgroundPageIds.HOME to null,
                BackgroundPageIds.COURSE to null,
                BackgroundPageIds.ACADEMIC to null,
                BackgroundPageIds.PERSON to null,
            )
        )

    // 默认起始页（0=首页, 1=课程, 2=教务信息, 3=个人）
    val defaultStartTab: StateFlow<Int> = appSettingsStore.defaultStartTabFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    val isLiquidGlassTabbarEnabled: StateFlow<Boolean> = appSettingsStore.liquidGlassTabbarEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val liquidGlassTabbarStyle: StateFlow<Int> = appSettingsStore.liquidGlassTabbarStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 1
        )

    fun saveDefaultStartTab(tabIndex: Int) {
        viewModelScope.launch {
            appSettingsStore.saveDefaultStartTab(tabIndex)
        }
    }
}
