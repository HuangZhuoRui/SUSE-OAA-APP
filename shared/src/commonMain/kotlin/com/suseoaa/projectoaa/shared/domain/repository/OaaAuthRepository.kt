package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.person.PersonData

/**
 * OaaAuthRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface OaaAuthRepository {
    /**
     * 登录并建立会话：保存 token 与 refresh token，再拉取 /user/me 记下用户 ID 和学号。
     * [account] 可以是用户名、邮箱或学号。
     */
    suspend fun login(account: String, password: String): Result<PersonData>

    /** 向账号绑定的邮箱发送重置密码验证码。 */
    suspend fun sendResetPasswordCode(account: String): Result<String>

    /** 用邮箱验证码重置密码（忘记密码）。 */
    suspend fun resetPassword(account: String, code: String, newPassword: String): Result<String>

    /** 已登录状态下用旧密码修改密码。 */
    suspend fun updatePassword(oldPassword: String, newPassword: String): Result<String>
}
