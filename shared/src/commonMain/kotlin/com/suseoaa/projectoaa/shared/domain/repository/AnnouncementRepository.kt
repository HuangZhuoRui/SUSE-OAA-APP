package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.announcement.Announcement
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementStatus

/**
 * AnnouncementRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface AnnouncementRepository {
    /** 当前用户可见的公告（含正文）；[status] 为 null 时不按状态筛选。 */
    suspend fun getAnnouncements(status: AnnouncementStatus? = null): Result<List<Announcement>>

    suspend fun getAnnouncement(id: Int): Result<Announcement>

    /** 创建草稿。 */
    suspend fun createDraft(departmentId: Int, title: String, content: String): Result<Unit>

    /**
     * 创建并立即发布。创建接口不返回 ID，实现里会回查草稿列表找到刚建的那条再发布。
     */
    suspend fun createAndPublish(
        departmentId: Int,
        departmentName: String,
        title: String,
        content: String
    ): Result<Unit>

    suspend fun updateAnnouncement(id: Int, title: String, content: String): Result<Unit>

    /** 发布：设为该部门当前生效的公告，原来生效的那条转入历史。 */
    suspend fun publishAnnouncement(id: Int): Result<Unit>

    suspend fun deleteAnnouncement(id: Int): Result<Unit>
}
