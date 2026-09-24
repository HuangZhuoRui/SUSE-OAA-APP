package com.suseoaa.projectoaa.presentation.recruitment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Application
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationForm
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Choice
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.TermPhase
import com.suseoaa.projectoaa.shared.domain.model.recruitment.phaseOn
import com.suseoaa.projectoaa.shared.domain.model.recruitment.pickCurrent
import com.suseoaa.projectoaa.shared.domain.permission.OaaPermission
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository
import com.suseoaa.projectoaa.shared.domain.repository.RecruitmentRepository
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** 申请表里的两个志愿。 */
enum class ChoiceSlot { First, Second }

data class RecruitmentUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUser: CurrentUser? = null,
    val terms: List<Term> = emptyList(),
    val selectedTerm: Term? = null,
    val phase: TermPhase? = null,
    val departments: List<Department> = emptyList(),
    val roles: List<Role> = emptyList(),
    /** 当前周期里我已提交的申请，没有则为 null */
    val myApplication: Application? = null,
    val form: ApplicationForm = ApplicationForm(),
    /** 两个志愿各自所选部门下可申请的职位 */
    val firstChoiceRoles: List<Role> = emptyList(),
    val secondChoiceRoles: List<Role> = emptyList()
) {
    val canEdit: Boolean get() = phase == TermPhase.Editing
    val canReview: Boolean get() = OaaPermission.canReviewApplications(currentUser)
    val canManageTerms: Boolean get() = OaaPermission.canManageTerms(currentUser)

    fun departmentName(id: Int): String = departments.firstOrNull { it.id == id }?.name ?: "未知部门"
    fun roleName(id: Int): String = roles.firstOrNull { it.id == id }?.name ?: "未知职位"
}

/**
 * 招新换届 —— 申请人视角：选周期、填写 / 修改申请表、查看录取结果。
 */
class RecruitmentViewModel(
    private val recruitmentRepository: RecruitmentRepository,
    private val personRepository: PersonRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecruitmentUiState())
    val uiState: StateFlow<RecruitmentUiState> = _uiState.asStateFlow()

    /** 部门 ID -> 可申请的职位，避免来回切换时重复请求 */
    private val fillableRolesCache = mutableMapOf<Int, List<Role>>()
    private var myApplications: List<Application> = emptyList()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val user = personRepository.getCurrentUser().getOrNull()
            val departments = organizationRepository.getDepartments().getOrDefault(emptyList())
            val roles = organizationRepository.getRoles().getOrDefault(emptyList())
            _uiState.update { it.copy(currentUser = user, departments = departments, roles = roles) }

            val terms = recruitmentRepository.getTerms().getOrElse { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "获取招新周期失败") }
                return@launch
            }
            // 没提交过申请时后端可能返回错误，按「没有申请」处理
            myApplications = recruitmentRepository.getMyApplications().getOrDefault(emptyList())

            val previous = _uiState.value.selectedTerm?.resolvedId
            val selected = terms.firstOrNull { it.resolvedId == previous } ?: terms.pickCurrent(today())
            _uiState.update { it.copy(isLoading = false, terms = terms) }
            selectTerm(selected)
        }
    }

    fun selectTerm(term: Term?) {
        val application = term?.let { t -> myApplications.firstOrNull { it.termId == t.resolvedId } }
        _uiState.update {
            it.copy(
                selectedTerm = term,
                phase = term?.phaseOn(today()),
                myApplication = application,
                form = application?.toForm() ?: ApplicationForm(),
                firstChoiceRoles = emptyList(),
                secondChoiceRoles = emptyList()
            )
        }
        application?.let {
            loadRolesFor(ChoiceSlot.First, it.firstChoice.departmentId)
            loadRolesFor(ChoiceSlot.Second, it.secondChoice.departmentId)
        }
    }

    fun updateForm(updater: (ApplicationForm) -> ApplicationForm) {
        _uiState.update { it.copy(form = updater(it.form)) }
    }

    /** 换部门时清空已选职位，因为不同部门可申请的职位不一样。 */
    fun selectDepartment(slot: ChoiceSlot, department: Department) {
        updateChoice(slot) { Choice(departmentId = department.id, roleId = 0) }
        loadRolesFor(slot, department.id)
    }

    fun selectRole(slot: ChoiceSlot, role: Role) {
        updateChoice(slot) { it.copy(roleId = role.id) }
    }

    fun submitApplication() {
        val state = _uiState.value
        val term = state.selectedTerm
        if (term == null || !state.canEdit) {
            _uiState.update { it.copy(errorMessage = "当前不在填写时间内，无法提交或修改") }
            return
        }
        validate(state.form)?.let { error ->
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val existing = state.myApplication
            val result = if (existing == null) {
                recruitmentRepository.createApplication(term.resolvedId, state.form)
            } else {
                recruitmentRepository.updateApplication(term.resolvedId, existing, state.form)
            }
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(isSubmitting = false, successMessage = if (existing == null) "提交成功" else "修改已保存")
                    }
                    loadInitialData()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "提交失败") }
                }
        }
    }

    fun deleteApplication() {
        val application = _uiState.value.myApplication ?: return
        if (application.resolvedId == 0) {
            _uiState.update { it.copy(errorMessage = "后端未返回申请编号，暂时无法删除") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            recruitmentRepository.deleteApplication(application.resolvedId)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "申请已撤回") }
                    loadInitialData()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "删除失败") }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun updateChoice(slot: ChoiceSlot, transform: (Choice) -> Choice) {
        updateForm { form ->
            when (slot) {
                ChoiceSlot.First -> form.copy(firstChoice = transform(form.firstChoice))
                ChoiceSlot.Second -> form.copy(secondChoice = transform(form.secondChoice))
            }
        }
    }

    private fun loadRolesFor(slot: ChoiceSlot, departmentId: Int) {
        if (departmentId <= 0) return
        viewModelScope.launch {
            val roles = fillableRolesCache[departmentId]
                ?: recruitmentRepository.getFillableRoles(departmentId).getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.also { fillableRolesCache[departmentId] = it }
                // 接口拿不到时退回全部启用的职位，由后端在提交时校验
                ?: _uiState.value.roles.filter { it.isActive }
            _uiState.update {
                when (slot) {
                    ChoiceSlot.First -> it.copy(firstChoiceRoles = roles)
                    ChoiceSlot.Second -> it.copy(secondChoiceRoles = roles)
                }
            }
        }
    }

    private fun validate(form: ApplicationForm): String? {
        val required = listOf(
            "学院" to form.college,
            "专业班级" to form.majorClass,
            "性别" to form.gender,
            "手机号" to form.phone,
            "QQ" to form.qq,
            "政治面貌" to form.politicalStatus,
            "出生年月" to form.birthDate,
            "个人经历" to form.resume,
            "申请理由" to form.reason
        )
        required.firstOrNull { it.second.isBlank() }?.let { return "${it.first}不能为空" }
        if (!PHONE_REGEX.matches(form.phone.trim())) return "请输入 11 位手机号"
        if (!QQ_REGEX.matches(form.qq.trim())) return "QQ 号格式不正确"
        if (!BIRTH_REGEX.matches(form.birthDate.trim())) return "出生年月格式应为 2005-09"
        if (!form.firstChoice.isComplete) return "请选择第一志愿的部门和职位"
        if (!form.secondChoice.isComplete) return "请选择第二志愿的部门和职位"
        return null
    }

    private fun today(): LocalDate =
        OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private companion object {
        val PHONE_REGEX = Regex("^1\\d{10}$")
        val QQ_REGEX = Regex("^\\d{5,12}$")
        val BIRTH_REGEX = Regex("^\\d{4}-\\d{2}(-\\d{2})?$")
    }
}
