package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.domain.model.org.CreateDepartmentRequest
import com.suseoaa.projectoaa.shared.domain.model.org.CreateRoleRequest
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.org.UpdateDepartmentRequest
import com.suseoaa.projectoaa.shared.domain.model.org.UpdateRoleRequest

/** `/department` 与 `/role` 下的接口：组织架构。 */
class OaaOrganizationApi(private val requester: OaaRequester) {

    suspend fun departments(): Result<List<Department>?> =
        requester.get("/department/list", "获取部门列表失败")

    suspend fun createDepartment(request: CreateDepartmentRequest): Result<Unit> =
        requester.post("/department/create", request, "新建部门失败")

    suspend fun updateDepartment(request: UpdateDepartmentRequest): Result<Unit> =
        requester.post("/department/update", request, "更新部门失败")

    suspend fun roles(): Result<List<Role>?> =
        requester.get("/role/list", "获取职位列表失败")

    suspend fun createRole(request: CreateRoleRequest): Result<Unit> =
        requester.post("/role/create", request, "新建职位失败")

    suspend fun updateRole(request: UpdateRoleRequest): Result<Unit> =
        requester.post("/role/update", request, "更新职位失败")
}
