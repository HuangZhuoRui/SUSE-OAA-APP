package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaOrganizationApi
import com.suseoaa.projectoaa.shared.domain.model.org.CreateDepartmentRequest
import com.suseoaa.projectoaa.shared.domain.model.org.CreateRoleRequest
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.org.UpdateDepartmentRequest
import com.suseoaa.projectoaa.shared.domain.model.org.UpdateRoleRequest
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 部门与职位仓库，带进程内缓存。
 */
class OrganizationRepositoryImpl(
    private val api: OaaOrganizationApi
) : OrganizationRepository {

    private val mutex = Mutex()
    private var departments: List<Department>? = null
    private var roles: List<Role>? = null

    override suspend fun getDepartments(forceRefresh: Boolean): Result<List<Department>> = mutex.withLock {
        departments?.takeUnless { forceRefresh }?.let { return@withLock Result.success(it) }
        api.departments().map { it.orEmpty() }.onSuccess { departments = it }
    }

    override suspend fun getRoles(forceRefresh: Boolean): Result<List<Role>> = mutex.withLock {
        roles?.takeUnless { forceRefresh }?.let { return@withLock Result.success(it) }
        api.roles().map { it.orEmpty() }.onSuccess { roles = it }
    }

    override suspend fun createDepartment(name: String, type: String): Result<Unit> =
        api.createDepartment(CreateDepartmentRequest(name = name, type = type))
            .onSuccess { invalidateDepartments() }

    override suspend fun updateDepartment(id: Int, name: String, type: String, isActive: Boolean): Result<Unit> =
        api.updateDepartment(UpdateDepartmentRequest(departmentId = id, name = name, type = type, isActive = isActive))
            .onSuccess { invalidateDepartments() }

    override suspend fun createRole(name: String, level: Int, type: String): Result<Unit> =
        api.createRole(CreateRoleRequest(name = name, level = level, type = type))
            .onSuccess { invalidateRoles() }

    override suspend fun updateRole(id: Int, name: String, level: Int, type: String, isActive: Boolean): Result<Unit> =
        api.updateRole(UpdateRoleRequest(roleId = id, name = name, level = level, type = type, isActive = isActive))
            .onSuccess { invalidateRoles() }

    private suspend fun invalidateDepartments() = mutex.withLock { departments = null }

    private suspend fun invalidateRoles() = mutex.withLock { roles = null }
}
