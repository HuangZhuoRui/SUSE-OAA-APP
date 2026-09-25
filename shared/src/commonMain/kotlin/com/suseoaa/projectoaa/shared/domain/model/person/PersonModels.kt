package com.suseoaa.projectoaa.shared.domain.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 对象存储里的文件：[uri] 是存储路径（提交给后端用），[url] 是带签名的临时下载地址（展示用）。
 */
@Serializable
data class StoredFile(
    @SerialName("uri") val uri: String = "",
    @SerialName("url") val url: String = ""
)

/** GET /user/me */
@Serializable
data class PersonData(
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("avatar") val avatar: StoredFile = StoredFile(),
    @SerialName("name") val name: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("role") val role: String = "",
    // 文档没写，但线上接口返回了职位等级；老版本后端没有时为 null
    @SerialName("role_level") val roleLevel: Int? = null
) {
    val avatarUrl: String get() = avatar.url
}

/**
 * 当前登录用户及其权限等级。
 *
 * 等级优先取 /user/me 的 role_level，没有时再拿 /role/list 按名称对出来；都对不上按 0 处理。
 * 界面上只隐藏入口，真正的权限仍以后端校验为准。
 */
data class CurrentUser(
    val person: PersonData,
    val level: Int,
    val departmentId: Int?,
    val roleId: Int?
)

/** POST /user/me/update，三个字段都是必填，不改的也要原样带上。 */
@Serializable
data class UpdateMeRequest(
    @SerialName("username") val username: String,
    @SerialName("email") val email: String,
    @SerialName("avatar") val avatar: String
)
