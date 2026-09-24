package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.domain.model.announcement.Announcement
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementIdRequest
import com.suseoaa.projectoaa.shared.domain.model.announcement.CreateAnnouncementRequest
import com.suseoaa.projectoaa.shared.domain.model.announcement.UpdateAnnouncementRequest
import kotlinx.serialization.json.JsonElement

/** `/announcement` 下的接口。 */
class OaaAnnouncementApi(private val requester: OaaRequester) {

    /** 可见范围由后端按当前用户的等级和部门过滤；[status] 为 null 时返回全部可见公告。 */
    suspend fun list(status: String?, withContent: Boolean): Result<List<Announcement>?> =
        requester.get(
            "/announcement/list",
            "获取公告失败",
            params = mapOf("status" to status, "content" to withContent)
        )

    /** 文档里单条查询返回的是数组，也可能改成对象，交给 Repository 兼容处理。 */
    suspend fun get(announcementId: Int): Result<JsonElement?> =
        requester.get("/announcement/get", "获取公告失败", params = mapOf("announcement_id" to announcementId))

    suspend fun create(request: CreateAnnouncementRequest): Result<Unit> =
        requester.post("/announcement/create", request, "创建公告失败")

    suspend fun update(request: UpdateAnnouncementRequest): Result<Unit> =
        requester.post("/announcement/update", request, "更新公告失败")

    suspend fun push(request: AnnouncementIdRequest): Result<Unit> =
        requester.post("/announcement/push", request, "发布公告失败")

    suspend fun delete(request: AnnouncementIdRequest): Result<Unit> =
        requester.post("/announcement/delete", request, "删除公告失败")
}
