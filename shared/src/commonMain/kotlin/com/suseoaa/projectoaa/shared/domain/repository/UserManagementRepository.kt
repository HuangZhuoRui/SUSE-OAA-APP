package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchFailure
import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserPage
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery

/**
 * UserManagementRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface UserManagementRepository {
    suspend fun listUsers(query: UserQuery): Result<UserPage>

    /** 批量调整部门与职位；返回未能修改的条目，全部成功时为空。 */
    suspend fun batchUpdate(items: List<UserBatchItem>): Result<List<UserBatchFailure>>

    suspend fun deleteUser(userId: Int): Result<Unit>
}
