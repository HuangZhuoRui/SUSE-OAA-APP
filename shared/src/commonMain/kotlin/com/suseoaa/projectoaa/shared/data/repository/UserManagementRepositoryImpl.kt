package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.domain.model.person.DeleteUserRequest
import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchFailure
import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserPage
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery
import com.suseoaa.projectoaa.shared.domain.repository.UserManagementRepository

/**
 * 成员管理仓库
 */
class UserManagementRepositoryImpl(
    private val userApi: OaaUserApi
) : UserManagementRepository {

    override suspend fun listUsers(query: UserQuery): Result<UserPage> =
        userApi.list(query).map { it ?: UserPage() }

    override suspend fun batchUpdate(items: List<UserBatchItem>): Result<List<UserBatchFailure>> =
        userApi.batchUpdate(items).map { it.orEmpty() }

    override suspend fun deleteUser(userId: Int): Result<Unit> =
        userApi.delete(DeleteUserRequest(userId))
}
