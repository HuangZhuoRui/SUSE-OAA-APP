package com.suseoaa.projectoaa.shared.domain.model.announcement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /announcement/list 的 status 参数。 */
enum class AnnouncementStatus(val value: String) {
    /** 各部门当前生效的公告 */
    Active("active"),
    /** 被新公告替换下来的历史公告 */
    History("history"),
    /** 未发布的草稿，只有有权限的人看得到 */
    Draft("draft")
}

@Serializable
data class Announcement(
    @SerialName("announcement_id") val id: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("content") val content: String = "",
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("department_name") val departmentName: String = "",
    @SerialName("publisher_id") val publisherId: Int = 0,
    @SerialName("publisher_name") val publisherName: String = "",
    @SerialName("publisher_role") val publisherRole: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    /** Go 后端对未设置的时间可能返回零值 0001-01-01，同样视为未发布。 */
    val isPublished: Boolean
        get() = !publishedAt.isNullOrBlank() && !publishedAt.startsWith("0001-")
}

@Serializable
data class CreateAnnouncementRequest(
    @SerialName("department_id") val departmentId: Int,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String
)

@Serializable
data class UpdateAnnouncementRequest(
    @SerialName("announcement_id") val announcementId: Int,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String
)

/** push / delete 共用的请求体。 */
@Serializable
data class AnnouncementIdRequest(
    @SerialName("announcement_id") val announcementId: Int
)
