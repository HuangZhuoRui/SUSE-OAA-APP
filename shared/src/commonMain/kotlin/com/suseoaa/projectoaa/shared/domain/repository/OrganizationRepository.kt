package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role

/**
 * OrganizationRepository 的契约。
 *
 * 部门与职位几乎不变，却被首页、成员管理、公告、招新等多处用来做下拉选项和
 * 名称 / ID 互查，所以实现里做了内存缓存；[forceRefresh] 用于管理页修改后刷新。
 */
interface OrganizationRepository {
    suspend fun getDepartments(forceRefresh: Boolean = false): Result<List<Department>>

    suspend fun getRoles(forceRefresh: Boolean = false): Result<List<Role>>

    suspend fun createDepartment(name: String, type: String): Result<Unit>

    suspend fun updateDepartment(id: Int, name: String, type: String, isActive: Boolean): Result<Unit>

    suspend fun createRole(name: String, level: Int, type: String): Result<Unit>

    suspend fun updateRole(id: Int, name: String, level: Int, type: String, isActive: Boolean): Result<Unit>
}
