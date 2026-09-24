package com.suseoaa.projectoaa.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.announcement.Announcement
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementStatus
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.permission.OaaPermission
import com.suseoaa.projectoaa.shared.domain.repository.AnnouncementRepository
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 部门公告详情与编辑页的状态。
 */
data class AnnouncementUiState(
    val departmentName: String = "",
    val currentUser: CurrentUser? = null,
    val canManage: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** 当前生效的公告（正常只有一条） */
    val active: List<Announcement> = emptyList(),
    val history: List<Announcement> = emptyList(),
    /** 草稿只有有管理权限的人才会加载 */
    val drafts: List<Announcement> = emptyList(),
    val isSubmitting: Boolean = false,
    val message: String? = null,
    // ========== 编辑器 ==========
    /** 正在编辑的公告；null 表示新建 */
    val editing: Announcement? = null,
    val editTitle: String = "",
    val editContent: String = "",
    val isEditorReady: Boolean = false,
    /** 保存成功后置 true，编辑页据此返回 */
    val isSaved: Boolean = false
)

/**
 * 部门公告：查看当前公告与历史，管理员 / 部门负责人可以写草稿、发布、删除。
 */
class AnnouncementViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val personRepository: PersonRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnnouncementUiState())
    val uiState: StateFlow<AnnouncementUiState> = _uiState.asStateFlow()

    fun load(departmentName: String) {
        _uiState.update { it.copy(departmentName = departmentName, isLoading = true, error = null) }
        viewModelScope.launch {
            val user = _uiState.value.currentUser ?: personRepository.getCurrentUser().getOrNull()
            val canManage = OaaPermission.canManageAnnouncements(user, departmentName)
            _uiState.update { it.copy(currentUser = user, canManage = canManage) }

            val activeDeferred = async { announcementRepository.getAnnouncements(AnnouncementStatus.Active) }
            val historyDeferred = async { announcementRepository.getAnnouncements(AnnouncementStatus.History) }
            val draftsDeferred = if (canManage) {
                async { announcementRepository.getAnnouncements(AnnouncementStatus.Draft) }
            } else null

            val active = activeDeferred.await()
            val history = historyDeferred.await()
            // 草稿拿不到（比如后端判定无权限）不影响查看公告
            val drafts = draftsDeferred?.await()?.getOrNull().orEmpty()

            fun List<Announcement>.ofDepartment() = filter { it.departmentName == departmentName }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    active = active.getOrNull().orEmpty().ofDepartment(),
                    history = history.getOrNull().orEmpty().ofDepartment()
                        .sortedByDescending { item -> item.publishedAt.orEmpty() },
                    drafts = drafts.ofDepartment().sortedByDescending { item -> item.updatedAt },
                    error = active.exceptionOrNull()?.message
                )
            }
        }
    }

    fun publish(announcement: Announcement) = runAction("已发布「${announcement.title}」") {
        announcementRepository.publishAnnouncement(announcement.id)
    }

    fun delete(announcement: Announcement) = runAction("已删除「${announcement.title}」") {
        announcementRepository.deleteAnnouncement(announcement.id)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // ==================== 编辑器 ====================

    /** [announcementId] 为 null 时新建。 */
    fun prepareEditor(departmentName: String, announcementId: Int?) {
        _uiState.update {
            it.copy(departmentName = departmentName, isEditorReady = false, isSaved = false, error = null)
        }
        if (announcementId == null) {
            _uiState.update { it.copy(editing = null, editTitle = "", editContent = "", isEditorReady = true) }
            return
        }
        viewModelScope.launch {
            // 列表接口的结构文档写得最清楚，先从里面找，找不到再走单条查询
            val fromList = announcementRepository.getAnnouncements().getOrNull()
                ?.firstOrNull { it.id == announcementId }
            val announcement = fromList ?: announcementRepository.getAnnouncement(announcementId).getOrElse { error ->
                _uiState.update { it.copy(error = error.message ?: "公告加载失败") }
                return@launch
            }
            _uiState.update {
                it.copy(
                    editing = announcement,
                    editTitle = announcement.title,
                    editContent = announcement.content,
                    isEditorReady = true
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(editContent = content) }
    }

    /**
     * 保存编辑内容。[publish] 为 true 时保存后立即发布，否则新建的存为草稿、已有的只更新内容。
     */
    fun save(publish: Boolean) {
        val state = _uiState.value
        val title = state.editTitle.trim()
        val content = state.editContent
        if (title.isBlank() || content.isBlank()) {
            _uiState.update { it.copy(message = "标题和正文都不能为空") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val editing = state.editing
            val result = if (editing == null) {
                val department = organizationRepository.getDepartments().getOrNull()
                    ?.firstOrNull { it.name == state.departmentName }
                if (department == null) {
                    appFailure<Unit>(AppError.Business(userMessage = "找不到部门「${state.departmentName}」"))
                } else if (publish) {
                    announcementRepository.createAndPublish(department.id, department.name, title, content)
                } else {
                    announcementRepository.createDraft(department.id, title, content)
                }
            } else {
                val updated = announcementRepository.updateAnnouncement(editing.id, title, content)
                if (updated.isSuccess && publish && !editing.isActive) {
                    announcementRepository.publishAnnouncement(editing.id)
                } else {
                    updated
                }
            }
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSaved = true,
                            message = if (publish) "公告已发布" else "已保存"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false, message = error.message ?: "保存失败") }
                }
        }
    }

    private fun runAction(successMessage: String, action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            action()
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, message = successMessage) }
                    load(_uiState.value.departmentName)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false, message = error.message ?: "操作失败") }
                }
        }
    }
}
