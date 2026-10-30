package com.suseoaa.projectoaa.presentation

import com.suseoaa.projectoaa.presentation.register.RegisterViewModel
import com.suseoaa.projectoaa.presentation.usermanagement.UserManagementViewModel
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.*
import com.suseoaa.projectoaa.shared.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountGraduationTest {
    private val dispatcher = StandardTestDispatcher()
    private val member = UserListItem(userId = 2, name = "成员", department = "技术部", role = "会员", roleLevel = 10)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun registrationRequiresValidYearAndPassesItToRepository() = runTest(dispatcher) {
        val grades = mutableListOf<Int>()
        val repo = object : OaaRegisterRepository {
            override suspend fun register(studentId: String, name: String, username: String, password: String, email: String, grade: Int): Result<String> {
                grades += grade
                return Result.success("注册成功")
            }
        }
        val vm = RegisterViewModel(repo)
        vm.updateStudentId("24101010330")
        vm.updateRealName("张三")
        vm.updateUserName("zhangsan")
        vm.updateEmail("z@x.com")
        vm.updatePassword("password")
        vm.updateConfirmPassword("password")
        for (year in listOf("", "2000", "1999")) {
            vm.updateGrade(year)
            vm.register()
            advanceUntilIdle()
            assertEquals(EnrollmentGrade.ERROR_MESSAGE, vm.uiState.value.errorMessage)
        }
        assertTrue(grades.isEmpty())
        vm.updateGrade("2024")
        vm.register()
        advanceUntilIdle()
        assertEquals(listOf(2024), grades)
        assertTrue(vm.uiState.value.isRegisterSuccess)
    }

    @Test
    fun departmentManagerCannotChangeGradeButCanChangeDepartmentAndRole() = runTest(dispatcher) {
        val repo = Members(listOf(member))
        val vm = UserManagementViewModel(Person(50), repo, Organization())
        advanceUntilIdle()
        assertFalse(vm.canEditGrade)
        vm.updateUser(member, 1, 1, grade = 2024)
        advanceUntilIdle()
        assertTrue(repo.updates.isEmpty())
        assertTrue(vm.uiState.value.message.orEmpty().contains("副会长"))
        vm.updateUser(member, 1, 1)
        advanceUntilIdle()
        assertEquals(null, repo.updates.single().single().grade)
    }

    @Test
    fun adminCanCorrectGradeAndBatchUpdatePreservesExplicitFalseFilter() = runTest(dispatcher) {
        val repo = Members(listOf(member))
        val vm = UserManagementViewModel(Person(80), repo, Organization())
        advanceUntilIdle()
        vm.updateGraduationFilter(false)
        vm.search()
        advanceUntilIdle()
        assertEquals(false, repo.queries.last().isGraduated)
        vm.toggleSelectUser(member)
        vm.batchUpdateSelected(1, 1, grade = 2024)
        advanceUntilIdle()
        assertEquals(2024, repo.updates.single().single().grade)
        assertTrue(vm.uiState.value.selectedUserIds.isEmpty())
    }

    @Test
    fun graduatePromotionIsRejectedEvenWhenGradeCorrectionIsRequested() = runTest(dispatcher) {
        val graduate = member.copy(isGraduated = true)
        val repo = Members(listOf(graduate))
        val vm = UserManagementViewModel(Person(80), repo, Organization())
        advanceUntilIdle()
        assertEquals(listOf(1), vm.assignableRoles(listOf(graduate)).map { it.id })
        vm.toggleSelectUser(graduate)
        vm.batchUpdateSelected(1, 2, grade = 2026)
        advanceUntilIdle()
        assertTrue(repo.updates.isEmpty())
        assertEquals("已毕业成员不可任命在任职务", vm.uiState.value.message)
        vm.updateUser(graduate, 1, 1, grade = 2000)
        advanceUntilIdle()
        assertTrue(repo.updates.isEmpty())
        assertEquals(EnrollmentGrade.ERROR_MESSAGE, vm.uiState.value.message)
    }

    private class Members(private val users: List<UserListItem>) : UserManagementRepository {
        val queries = mutableListOf<UserQuery>()
        val updates = mutableListOf<List<UserBatchItem>>()
        override suspend fun listUsers(query: UserQuery): Result<UserPage> {
            queries += query
            return Result.success(UserPage(users.size, users))
        }
        override suspend fun batchUpdate(items: List<UserBatchItem>): Result<List<UserBatchFailure>> {
            updates += items
            return Result.success(emptyList())
        }
        override suspend fun deleteUser(userId: Int): Result<Unit> = error("unused")
    }

    private class Organization : OrganizationRepository {
        override suspend fun getDepartments(forceRefresh: Boolean) = Result.success(listOf(Department(1, "技术部")))
        override suspend fun getRoles(forceRefresh: Boolean) = Result.success(listOf(Role(1, "会员", 10), Role(2, "部长", 30)))
        override suspend fun createDepartment(name: String, type: String): Result<Unit> = error("unused")
        override suspend fun updateDepartment(id: Int, name: String, type: String, isActive: Boolean): Result<Unit> = error("unused")
        override suspend fun createRole(name: String, level: Int, type: String): Result<Unit> = error("unused")
        override suspend fun updateRole(id: Int, name: String, level: Int, type: String, isActive: Boolean): Result<Unit> = error("unused")
    }

    private class Person(private val level: Int) : PersonRepository {
        override suspend fun getCurrentUser() = Result.success(CurrentUser(PersonData(userId = 1), level, 1, 1))
        override suspend fun logout(): Unit = error("unused")
        override suspend fun getPersonInfo(): Result<PersonData> = error("unused")
        override suspend fun updateProfile(username: String, email: String, grade: Int?): Result<String> = error("unused")
        override suspend fun uploadAvatar(imageData: ByteArray): Result<String> = error("unused")
        override suspend fun uploadFile(bytes: ByteArray, fileName: String, mimeType: String, scene: String): Result<StoredFile> = error("unused")
        override suspend fun sendDeleteAccountCode(): Result<Unit> = error("unused")
        override suspend fun deleteAccount(code: String): Result<Unit> = error("unused")
        override suspend fun sendCancelDeleteCode(): Result<Unit> = error("unused")
        override suspend fun cancelDeleteAccount(code: String): Result<Unit> = error("unused")
        override suspend fun checkServerHealth(): Result<ServerHealthStatus> = error("unused")
    }
}
