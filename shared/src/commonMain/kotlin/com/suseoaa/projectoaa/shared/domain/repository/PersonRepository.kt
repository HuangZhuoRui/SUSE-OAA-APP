package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData

/**
 * PersonRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface PersonRepository {
    /** 注销后端的本设备会话并清空本地数据；后端调用失败也会清本地。 */
    suspend fun logout()

    suspend fun getPersonInfo(): Result<PersonData>

    /** 当前用户及其职位等级，用于决定界面上显示哪些管理入口。 */
    suspend fun getCurrentUser(): Result<CurrentUser>

    /** 修改用户名与邮箱，姓名不可修改。 */
    suspend fun updateProfile(username: String, email: String): Result<String>

    /** 上传头像并设为当前头像。 */
    suspend fun uploadAvatar(imageData: ByteArray): Result<String>
}
