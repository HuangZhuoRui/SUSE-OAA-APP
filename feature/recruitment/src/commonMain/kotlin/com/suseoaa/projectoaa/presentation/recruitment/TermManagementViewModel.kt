package com.suseoaa.projectoaa.presentation.recruitment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Interviewer
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewerEntry
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Period
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.parsePeriodDate
import com.suseoaa.projectoaa.shared.domain.repository.RecruitmentRepository
import com.suseoaa.projectoaa.shared.domain.repository.UserManagementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 周期编辑表单，日期均为 yyyy-MM-dd 文本。 */
data class TermDraft(
    val year: String = "",
    val type: String = "",
    val title: String = "",
    val editStart: String = "",
    val editEnd: String = "",
    val queryStart: String = "",
    val queryEnd: String = ""
) {
    companion object {
        fun from(term: Term) = TermDraft(
            year = term.year.toString(),
            type = term.type,
            title = term.title,
            editStart = term.editPeriod.startAt.take(10),
            editEnd = term.editPeriod.endAt.take(10),
            queryStart = term.queryPeriod.startAt.take(10),
            queryEnd = term.queryPeriod.endAt.take(10)
        )
    }
}

data class TermManagementUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val terms: List<Term> = emptyList(),
    /** 周期列表筛选条件（空串表示不限） */
    val filterYear: String = "",
    val filterType: String = "",
    /** 当前选中周期下已指派的面试官列表（GET /interviewer/list） */
    val interviewers: List<Interviewer> = emptyList(),
    val isLoadingInterviewers: Boolean = false,
    /** 添加面试官时的成员搜索结果 */
    val userResults: List<UserListItem> = emptyList(),
    val isSearchingUsers: Boolean = false
)

/**
 * 招新换届 —— 管理员视角：维护周期、指定 / 修改 / 移除面试官。后端要求 level >= 80。
 */
class TermManagementViewModel(
    private val recruitmentRepository: RecruitmentRepository,
    private val userManagementRepository: UserManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TermManagementUiState())
    val uiState: StateFlow<TermManagementUiState> = _uiState.asStateFlow()

    private var initialized = false
    /** 通过 is_all=true 预拉取的全部可见成员缓存，用于快速筛选面试官候选人 */
    private var allMembersCache: List<UserListItem> = emptyList()

    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        reload()
    }

    fun updateFilters(year: String? = null, type: String? = null, autoReload: Boolean = false) {
        _uiState.update {
            it.copy(
                filterYear = year ?: it.filterYear,
                filterType = type ?: it.filterType
            )
        }
        if (autoReload) reload()
    }

    fun reload() {
        val state = _uiState.value
        val yearFilter = state.filterYear.trim().let { raw ->
            if (raw.isEmpty()) null else raw.toIntOrNull() ?: run {
                _uiState.update { it.copy(message = "请输入有效的四位年份") }
                return
            }
        }
        val typeFilter = state.filterType.ifBlank { null }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            recruitmentRepository.getTerms(year = yearFilter, type = typeFilter)
                .onSuccess { terms ->
                    _uiState.update {
                        it.copy(isLoading = false, terms = terms.sortedByDescending { t -> t.editPeriod.startAt })
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, message = error.message ?: "获取周期失败") }
                }
        }
    }

    /** [existing] 为 null 时新建；更新接口不允许改年份和类型。 */
    fun saveTerm(existing: Term?, draft: TermDraft) {
        validate(draft, isNew = existing == null)?.let { error ->
            _uiState.update { it.copy(message = error) }
            return
        }
        val editPeriod = Period(draft.editStart.trim(), draft.editEnd.trim())
        val queryPeriod = Period(draft.queryStart.trim(), draft.queryEnd.trim())
        runAction(if (existing == null) "周期已创建" else "周期已更新") {
            if (existing == null) {
                recruitmentRepository.createTerm(
                    year = draft.year.trim().toInt(),
                    type = draft.type,
                    title = draft.title.trim(),
                    editPeriod = editPeriod,
                    queryPeriod = queryPeriod
                )
            } else {
                recruitmentRepository.updateTerm(existing.resolvedId, draft.title.trim(), editPeriod, queryPeriod)
            }
        }
    }

    fun deleteTerm(term: Term) = runAction("已删除「${term.title}」") {
        recruitmentRepository.deleteTerm(term.resolvedId)
    }

    /** 打开某周期的面试官管理弹窗时加载当前面试官列表，并利用 is_all=true 预加载可选成员。 */
    fun openInterviewerManager(term: Term) {
        loadInterviewers(term.resolvedId)
        if (allMembersCache.isEmpty()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSearchingUsers = true) }
                val result = userManagementRepository.listUsers(UserQuery(isAll = true))
                val members = result.getOrNull()?.list?.distinctBy { it.userId }.orEmpty()
                allMembersCache = members
                _uiState.update {
                    it.copy(
                        isSearchingUsers = false,
                        userResults = members
                    )
                }
            }
        } else {
            _uiState.update { it.copy(userResults = allMembersCache) }
        }
    }

    fun loadInterviewers(termId: Int) {
        if (termId <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInterviewers = true) }
            recruitmentRepository.getInterviewers(termId)
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoadingInterviewers = false, interviewers = list) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingInterviewers = false,
                            interviewers = emptyList(),
                            message = error.message ?: "获取面试官列表失败"
                        )
                    }
                }
        }
    }

    fun searchUsers(keyword: String) {
        val clean = keyword.trim()
        if (allMembersCache.isNotEmpty()) {
            val filtered = if (clean.isEmpty()) {
                allMembersCache
            } else {
                allMembersCache.filter { user ->
                    user.name.contains(clean, ignoreCase = true) ||
                        user.username.contains(clean, ignoreCase = true) ||
                        user.studentId.contains(clean, ignoreCase = true) ||
                        user.department.contains(clean, ignoreCase = true) ||
                        user.role.contains(clean, ignoreCase = true) ||
                        user.resolvedGrade?.toString()?.contains(clean) == true
                }
            }
            _uiState.update { it.copy(userResults = filtered) }
            if (filtered.isNotEmpty() || clean.isEmpty()) return
        }
        if (clean.isEmpty()) {
            _uiState.update { it.copy(userResults = allMembersCache) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUsers = true) }
            val result = userManagementRepository.listUsers(UserQuery(keyword = clean, isAll = true))
            _uiState.update {
                it.copy(
                    isSearchingUsers = false,
                    userResults = result.getOrNull()?.list.orEmpty(),
                    message = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun addInterviewers(term: Term, users: List<UserListItem>, remark: String) {
        if (users.isEmpty()) {
            _uiState.update { it.copy(message = "请至少选择一名面试官") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            recruitmentRepository.createInterviewers(
                termId = term.resolvedId,
                interviewers = users.map { InterviewerEntry(userId = it.userId, remark = remark.ifBlank { it.name }) }
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false, message = "已添加 ${users.size} 名面试官") }
                loadInterviewers(term.resolvedId)
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, message = error.message ?: "添加面试官失败") }
            }
        }
    }

    fun updateInterviewerRemark(term: Term, interviewer: Interviewer, remark: String) {
        val id = interviewer.resolvedId
        if (id == 0) {
            _uiState.update { it.copy(message = "后端未返回面试官编号，无法修改") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            recruitmentRepository.updateInterviewer(interviewerId = id, remark = remark.trim())
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, message = "已更新「${interviewer.displayName}」备注") }
                    loadInterviewers(term.resolvedId)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, message = error.message ?: "更新面试官失败") }
                }
        }
    }

    fun removeInterviewer(term: Term, interviewer: Interviewer) {
        val id = interviewer.resolvedId
        if (id == 0) {
            _uiState.update { it.copy(message = "后端未返回面试官编号，无法移除") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            recruitmentRepository.deleteInterviewer(interviewerId = id)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, message = "已移除面试官「${interviewer.displayName}」") }
                    loadInterviewers(term.resolvedId)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, message = error.message ?: "移除面试官失败") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun runAction(successMessage: String, action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            action()
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, message = successMessage) }
                    reload()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, message = error.message ?: "操作失败") }
                }
        }
    }

    private fun validate(draft: TermDraft, isNew: Boolean): String? {
        if (isNew) {
            if (draft.year.trim().toIntOrNull() == null) return "请填写年份"
            if (draft.type.isBlank()) return "请选择周期类型"
        }
        if (draft.title.isBlank()) return "请填写标题"
        val dates = listOf(draft.editStart, draft.editEnd, draft.queryStart, draft.queryEnd).map { parsePeriodDate(it) }
        if (dates.any { it == null }) return "日期格式应为 2026-09-01"
        val (editStart, editEnd, queryStart, queryEnd) = dates.map { it!! }
        if (editStart > editEnd) return "填写期的结束日期不能早于开始日期"
        if (queryStart > queryEnd) return "公示期的结束日期不能早于开始日期"
        if (queryStart <= editEnd) return "公示期应在填写期结束之后开始"
        return null
    }
}
