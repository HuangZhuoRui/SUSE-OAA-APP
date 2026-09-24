package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.remote.oaaDevice
import com.suseoaa.projectoaa.shared.domain.model.login.LogoutRequest
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.UpdateMeRequest
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository

/**
 * 用户个人信息仓库
 */
class PersonRepositoryImpl(
    private val userApi: OaaUserApi,
    private val authApi: OaaAuthApi,
    private val organizationRepository: OrganizationRepository,
    private val userDataCleaner: UserDataCleaner
) : PersonRepository {

    override suspend fun logout() {
        // 后端注销失败（断网、token 已过期）不影响本地退出
        authApi.logout(LogoutRequest(device = oaaDevice))
        userDataCleaner.clearSession()
    }

    override suspend fun getPersonInfo(): Result<PersonData> = userApi.me()

    override suspend fun getCurrentUser(): Result<CurrentUser> {
        val person = userApi.me().getOrElse { return Result.failure(it) }
        // 职位 / 部门列表拿不到时降级为最低权限，不影响页面本身的加载
        val role = organizationRepository.getRoles().getOrNull()?.firstOrNull { it.name == person.role }
        val department = organizationRepository.getDepartments().getOrNull()
            ?.firstOrNull { it.name == person.department }
        return Result.success(
            CurrentUser(
                person = person,
                level = role?.level ?: 0,
                departmentId = department?.id,
                roleId = role?.id
            )
        )
    }

    override suspend fun updateProfile(username: String, email: String): Result<String> {
        // 更新接口三个字段都必填，头像沿用当前的
        val current = userApi.me().getOrElse { return Result.failure(it) }
        return userApi.updateMe(UpdateMeRequest(username = username, email = email, avatar = current.avatar.uri))
            .map { "信息更新成功" }
    }

    override suspend fun uploadAvatar(imageData: ByteArray): Result<String> {
        val uploaded = userApi.uploadImage(
            scene = AVATAR_SCENE,
            bytes = imageData,
            fileName = "avatar.jpg",
            mimeType = "image/jpeg"
        ).getOrElse { return Result.failure(it) }
        val current = userApi.me().getOrElse { return Result.failure(it) }
        return userApi.updateMe(
            UpdateMeRequest(username = current.username, email = current.email, avatar = uploaded.uri)
        ).map { "头像更新成功" }
    }

    private companion object {
        const val AVATAR_SCENE = "avatar"
    }
}
