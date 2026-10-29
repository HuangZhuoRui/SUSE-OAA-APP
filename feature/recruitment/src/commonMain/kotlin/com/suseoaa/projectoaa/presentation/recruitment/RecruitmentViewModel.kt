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
    val isUploadingAvatar: Boolean = false,
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
    /** 本次新选并上传成功的本地图片字节，用于界面即时无闪烁预览 */
    val localAvatarBytes: ByteArray? = null,
    /** 按已选职位过滤出的可申请部门；null 表示未按职位过滤，展示全部启用部门 */
    val firstChoiceDepartments: List<Department>? = null,
    val secondChoiceDepartments: List<Department>? = null,
    /** 按已选部门过滤出的可申请职位；null 表示未按部门过滤，展示全部启用职位 */
    val firstChoiceRoles: List<Role>? = null,
    val secondChoiceRoles: List<Role>? = null
) {
    val canEdit: Boolean get() = phase == TermPhase.Editing
    val canReview: Boolean get() = OaaPermission.canReviewApplications(currentUser)
    val canManageTerms: Boolean get() = OaaPermission.canManageTerms(currentUser)

    fun departmentsFor(slot: ChoiceSlot): List<Department> =
        when (slot) {
            ChoiceSlot.First -> firstChoiceDepartments
            ChoiceSlot.Second -> secondChoiceDepartments
        } ?: departments.filter { it.isActive }

    fun rolesFor(slot: ChoiceSlot): List<Role> =
        when (slot) {
            ChoiceSlot.First -> firstChoiceRoles
            ChoiceSlot.Second -> secondChoiceRoles
        } ?: roles.filter { it.isActive }

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
    /** 职位 ID -> 可申请的部门，避免来回切换时重复请求 */
    private val fillableDepartmentsCache = mutableMapOf<Int, List<Department>>()
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
                localAvatarBytes = null,
                firstChoiceDepartments = null,
                secondChoiceDepartments = null,
                firstChoiceRoles = null,
                secondChoiceRoles = null
            )
        }
        application?.let {
            loadRolesFor(ChoiceSlot.First, it.firstChoice.departmentId, keepValidSelection = true)
            loadDepartmentsFor(ChoiceSlot.First, it.firstChoice.roleId, keepValidSelection = true)
            loadRolesFor(ChoiceSlot.Second, it.secondChoice.departmentId, keepValidSelection = true)
            loadDepartmentsFor(ChoiceSlot.Second, it.secondChoice.roleId, keepValidSelection = true)
        }
    }

    fun updateForm(updater: (ApplicationForm) -> ApplicationForm) {
        _uiState.update { it.copy(form = updater(it.form)) }
    }

    /** 上传申请表个人照片（POST /upload/image，scene = application），成功后回填 URI 与预览地址。 */
    fun uploadAvatar(imageData: ByteArray) {
        if (!_uiState.value.canEdit) {
            _uiState.update { it.copy(errorMessage = "当前不在填写时间内，无法上传照片") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, errorMessage = null) }
            recruitmentRepository.uploadApplicationAvatar(imageData)
                .onSuccess { stored ->
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            localAvatarBytes = imageData,
                            form = it.form.copy(
                                avatarUri = stored.uri,
                                avatarUrl = stored.url
                            ),
                            successMessage = "照片上传成功"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            errorMessage = "照片上传失败：${error.message ?: "请稍后重试"}"
                        )
                    }
                }
        }
    }

    /** 选部门后联动拉取该部门可申请的职位（GET /application/role）；若原已选职位不兼容则自动清空职位。 */
    fun selectDepartment(slot: ChoiceSlot, department: Department) {
        updateChoice(slot) { it.copy(departmentId = department.id) }
        loadRolesFor(slot, department.id, keepValidSelection = true)
    }

    /** 选职位后联动拉取该职位可申请的部门（GET /application/department）；若原已选部门不兼容则自动清空部门。 */
    fun selectRole(slot: ChoiceSlot, role: Role) {
        updateChoice(slot) { it.copy(roleId = role.id) }
        loadDepartmentsFor(slot, role.id, keepValidSelection = true)
    }

    /** 清空某个志愿的已选项与双向过滤条件，方便重新按部门或职位任意顺序选择。 */
    fun clearChoice(slot: ChoiceSlot) {
        updateChoice(slot) { Choice() }
        _uiState.update {
            when (slot) {
                ChoiceSlot.First -> it.copy(firstChoiceDepartments = null, firstChoiceRoles = null)
                ChoiceSlot.Second -> it.copy(secondChoiceDepartments = null, secondChoiceRoles = null)
            }
        }
    }

    fun submitApplication() {
        val state = _uiState.value
        val term = state.selectedTerm
        if (term == null || !state.canEdit) {
            _uiState.update { it.copy(errorMessage = "当前不在填写时间内，无法提交或修改") }
            return
        }
        if (state.isUploadingAvatar) {
            _uiState.update { it.copy(errorMessage = "照片正在上传中，请稍候") }
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

    private fun loadRolesFor(slot: ChoiceSlot, departmentId: Int, keepValidSelection: Boolean = false) {
        if (departmentId <= 0) return
        viewModelScope.launch {
            val roles = fillableRolesCache[departmentId]
                ?: recruitmentRepository.getFillableRoles(departmentId).getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.also { fillableRolesCache[departmentId] = it }
                // 接口拿不到时退回全部启用的职位，由后端在提交时校验
                ?: _uiState.value.roles.filter { it.isActive }
            _uiState.update { state ->
                val updatedState = when (slot) {
                    ChoiceSlot.First -> state.copy(firstChoiceRoles = roles)
                    ChoiceSlot.Second -> state.copy(secondChoiceRoles = roles)
                }
                val currentChoice = when (slot) {
                    ChoiceSlot.First -> updatedState.form.firstChoice
                    ChoiceSlot.Second -> updatedState.form.secondChoice
                }
                if (currentChoice.roleId > 0 && (!keepValidSelection || roles.none { r -> r.id == currentChoice.roleId })) {
                    val resetChoice = currentChoice.copy(roleId = 0)
                    val newForm = when (slot) {
                        ChoiceSlot.First -> updatedState.form.copy(firstChoice = resetChoice)
                        ChoiceSlot.Second -> updatedState.form.copy(secondChoice = resetChoice)
                    }
                    updatedState.copy(form = newForm)
                } else {
                    updatedState
                }
            }
        }
    }

    private fun loadDepartmentsFor(slot: ChoiceSlot, roleId: Int, keepValidSelection: Boolean = false) {
        if (roleId <= 0) return
        viewModelScope.launch {
            val departments = fillableDepartmentsCache[roleId]
                ?: recruitmentRepository.getFillableDepartments(roleId).getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.also { fillableDepartmentsCache[roleId] = it }
                ?: _uiState.value.departments.filter { it.isActive }
            _uiState.update { state ->
                val updatedState = when (slot) {
                    ChoiceSlot.First -> state.copy(firstChoiceDepartments = departments)
                    ChoiceSlot.Second -> state.copy(secondChoiceDepartments = departments)
                }
                val currentChoice = when (slot) {
                    ChoiceSlot.First -> updatedState.form.firstChoice
                    ChoiceSlot.Second -> updatedState.form.secondChoice
                }
                if (currentChoice.departmentId > 0 && (!keepValidSelection || departments.none { d -> d.id == currentChoice.departmentId })) {
                    val resetChoice = currentChoice.copy(departmentId = 0)
                    val newForm = when (slot) {
                        ChoiceSlot.First -> updatedState.form.copy(firstChoice = resetChoice)
                        ChoiceSlot.Second -> updatedState.form.copy(secondChoice = resetChoice)
                    }
                    updatedState.copy(form = newForm)
                } else {
                    updatedState
                }
            }
        }
    }

    private fun validate(form: ApplicationForm): String? {
        if (form.avatarUri.isBlank()) return "请上传申请表个人照片"
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
