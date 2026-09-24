package com.suseoaa.projectoaa.presentation.recruitment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery
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
    /** 添加面试官时的成员搜索结果 */
    val userResults: List<UserListItem> = emptyList(),
    val isSearchingUsers: Boolean = false
)

/**
 * 招新换届 —— 管理员视角：维护周期、指定面试官。后端要求 level >= 80。
 */
class TermManagementViewModel(
    private val recruitmentRepository: RecruitmentRepository,
    private val userManagementRepository: UserManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TermManagementUiState())
    val uiState: StateFlow<TermManagementUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            recruitmentRepository.getTerms()
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

    fun searchUsers(keyword: String) {
        if (keyword.isBlank()) {
            _uiState.update { it.copy(userResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUsers = true) }
            val result = userManagementRepository.listUsers(UserQuery(keyword = keyword.trim(), pageSize = 20))
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
        runAction("已添加 ${users.size} 名面试官") {
            recruitmentRepository.createInterviewers(
                termId = term.resolvedId,
                interviewers = users.map { InterviewerEntry(userId = it.userId, remark = remark.ifBlank { it.name }) }
            )
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
