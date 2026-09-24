package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaAnnouncementApi
import com.suseoaa.projectoaa.shared.data.remote.api.decodeListLenient
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.announcement.Announcement
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementIdRequest
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementStatus
import com.suseoaa.projectoaa.shared.domain.model.announcement.CreateAnnouncementRequest
import com.suseoaa.projectoaa.shared.domain.model.announcement.UpdateAnnouncementRequest
import com.suseoaa.projectoaa.shared.domain.repository.AnnouncementRepository
import kotlinx.serialization.json.Json

/**
 * 公告信息仓库
 */
class AnnouncementRepositoryImpl(
    private val api: OaaAnnouncementApi,
    private val json: Json
) : AnnouncementRepository {

    override suspend fun getAnnouncements(status: AnnouncementStatus?): Result<List<Announcement>> =
        api.list(status = status?.value, withContent = true).map { it.orEmpty() }

    override suspend fun getAnnouncement(id: Int): Result<Announcement> {
        val element = api.get(id).getOrElse { return Result.failure(it) }
        val announcement = runCatching { json.decodeListLenient<Announcement>(element).firstOrNull() }.getOrNull()
            ?: return appFailure(AppError.Business(userMessage = "公告不存在或已被删除"))
        return Result.success(announcement)
    }

    override suspend fun createDraft(departmentId: Int, title: String, content: String): Result<Unit> =
        api.create(CreateAnnouncementRequest(departmentId = departmentId, title = title, content = content))

    override suspend fun createAndPublish(
        departmentId: Int,
        departmentName: String,
        title: String,
        content: String
    ): Result<Unit> {
        createDraft(departmentId, title, content).getOrElse { return Result.failure(it) }

        val created = api.list(status = AnnouncementStatus.Draft.value, withContent = false)
            .getOrNull()
            .orEmpty()
            .filter { it.departmentName == departmentName && it.title == title }
            .maxByOrNull { it.id }
            ?: return appFailure(
                AppError.Business(userMessage = "公告已保存为草稿，但未能自动发布，请在草稿中手动发布")
            )
        return publishAnnouncement(created.id)
    }

    override suspend fun updateAnnouncement(id: Int, title: String, content: String): Result<Unit> =
        api.update(UpdateAnnouncementRequest(announcementId = id, title = title, content = content))

    override suspend fun publishAnnouncement(id: Int): Result<Unit> =
        api.push(AnnouncementIdRequest(id))

    override suspend fun deleteAnnouncement(id: Int): Result<Unit> =
        api.delete(AnnouncementIdRequest(id))
}
