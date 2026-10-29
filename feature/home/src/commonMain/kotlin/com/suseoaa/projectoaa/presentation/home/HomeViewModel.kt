package com.suseoaa.projectoaa.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.domain.model.announcement.Announcement
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementStatus
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.DepartmentTypes
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.repository.AnnouncementRepository
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页界面状态
 */
data class HomeUiState(
    /**
     * 是否登录了协会账号；null 表示还没读到登录态。
     * 游客（false）只显示登录引导，不请求部门和公告。
     */
    val isSignedIn: Boolean? = null,
    val currentUser: CurrentUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** 首页卡片：协会节点排在最前，其余按接口顺序 */
    val departments: List<Department> = emptyList(),
    /** 各部门当前生效的公告，键为部门名称 */
    val activeAnnouncements: Map<String, Announcement> = emptyMap(),
    /** 公告至少成功加载过一次；之前卡片显示「加载中」，之后没有公告的显示「暂无公告」 */
    val hasLoadedAnnouncements: Boolean = false
)

/**
 * 首页 ViewModel：部门卡片与各部门当前公告。
 *
 * 部门以前是写死的 5 个，现在来自 /department/list，后台增删部门首页自动跟着变。
 */
class HomeViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val personRepository: PersonRepository,
    private val organizationRepository: OrganizationRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.tokenFlow
                .map { !it.isNullOrEmpty() }
                .distinctUntilChanged()
                .collect { signedIn ->
                    _uiState.update { if (signedIn) it.copy(isSignedIn = true) else HomeUiState(isSignedIn = false) }
                    if (signedIn) refreshHomeSummary(showLoading = false)
                }
        }
    }

    fun refreshHomeSummary(showLoading: Boolean = true) {
        if (_uiState.value.isSignedIn != true) return
        viewModelScope.launch {
            personRepository.getCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
        refreshDepartmentCards(showLoading)
    }

    private fun refreshDepartmentCards(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            val departmentsDeferred = async { organizationRepository.getDepartments(forceRefresh = showLoading) }
            val announcementsDeferred = async { announcementRepository.getAnnouncements(AnnouncementStatus.Active) }
            val departments = departmentsDeferred.await()
            val announcements = announcementsDeferred.await()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    departments = departments.getOrNull()?.let(::orderForHome) ?: state.departments,
                    activeAnnouncements = announcements.getOrNull()?.let(::latestByDepartment)
                        ?: state.activeAnnouncements,
                    hasLoadedAnnouncements = state.hasLoadedAnnouncements || announcements.isSuccess,
                    errorMessage = (departments.exceptionOrNull() ?: announcements.exceptionOrNull())?.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun orderForHome(departments: List<Department>): List<Department> =
        departments.filter { it.isActive }
            .sortedBy { if (it.type == DepartmentTypes.ASSOCIATION) 0 else 1 }

    private fun latestByDepartment(announcements: List<Announcement>): Map<String, Announcement> =
        announcements.groupBy { it.departmentName }
            .mapValues { (_, list) -> list.maxBy { it.publishedAt.orEmpty() } }
}
